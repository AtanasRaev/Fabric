package bg.tshirt.web;

import bg.tshirt.config.JwtTokenProvider;
import bg.tshirt.database.dto.UserDTO;
import bg.tshirt.database.dto.UserLoginDTO;
import bg.tshirt.database.dto.UserRegistrationDTO;
import bg.tshirt.database.dto.UserResponseDTO;
import bg.tshirt.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public UserController(UserService userService,
                          JwtTokenProvider jwtTokenProvider,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        String token = this.jwtTokenProvider.getJwtFromRequest(request);

        String currentFingerprint = jwtTokenProvider.generateDeviceFingerprint(request);

        if (token == null || !this.jwtTokenProvider.validateToken(token, currentFingerprint) || !this.jwtTokenProvider.isTokenTypeValid(token, "access")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid or expired token"));
        }

        String userEmail = this.jwtTokenProvider.getEmailFromJwt(token);
        UserDTO user = this.userService.findByEmail(userEmail);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        Map<String, Object> profile = Map.of(
                "email", user.getEmail(),
                "address", user.getAddress()
        );

        return ResponseEntity.ok(profile);
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid UserRegistrationDTO registrationDTO, HttpServletRequest request) {
        this.userService.registerUser(registrationDTO);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registrationDTO.getEmail(),
                        registrationDTO.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String currentFingerprint = jwtTokenProvider.generateDeviceFingerprint(request);
        String accessToken = jwtTokenProvider.generateAccessToken(authentication, currentFingerprint);
        String refreshToken = jwtTokenProvider.generateRefreshToken(registrationDTO.getEmail(), currentFingerprint);

        return ResponseEntity.ok(new UserResponseDTO("success", "Registration successful", accessToken, refreshToken));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid UserLoginDTO loginRequest, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        String currentFingerprint = jwtTokenProvider.generateDeviceFingerprint(request);

        String accessToken = jwtTokenProvider.generateAccessToken(authentication, currentFingerprint);
        String refreshToken = jwtTokenProvider.generateRefreshToken(loginRequest.getEmail(), currentFingerprint);

        return ResponseEntity.ok(new UserResponseDTO("success", "Login successful", accessToken, refreshToken));
    }
}