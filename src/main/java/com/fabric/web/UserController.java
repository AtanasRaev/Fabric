package com.fabric.web;

import com.fabric.config.JwtTokenProvider;
import com.fabric.database.dto.clothes.ClothingPageDTO;
import com.fabric.database.dto.user.*;
import com.fabric.service.PasswordResetService;
import com.fabric.service.RefreshTokenService;
import com.fabric.service.UserService;
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
@RequestMapping("/users")
@Validated
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final PasswordResetService passwordResetService;

    public UserController(UserService userService,
                          JwtTokenProvider jwtTokenProvider,
                          AuthenticationManager authenticationManager,
                          RefreshTokenService refreshTokenService,
                          PasswordResetService passwordResetService) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(HttpServletRequest request) {
        UserDTO userDTO = this.userService.validateUser(request);
        return ResponseEntity.ok(this.userService.getUserProfile(userDTO.getEmail()));
    }

    @PutMapping("/edit")
    public ResponseEntity<?> editProfile(@RequestBody @Valid UserEitDTO userEditDTO, HttpServletRequest request) {
        if (!this.userService.editUser(userEditDTO, request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Error"));
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "User edited successfully!"
        ));
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

        this.refreshTokenService.saveNewToken(
                jwtTokenProvider.getJtiFromJwt(refreshToken),
                registrationDTO.getEmail(),
                jwtTokenProvider.getExpirationDate(refreshToken)
        );

        return ResponseEntity.ok(new UserResponseDTO("success", "Registration successful", accessToken, refreshToken, this.userService.getUserRoles(accessToken)));
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

        this.refreshTokenService.saveNewToken(
                jwtTokenProvider.getJtiFromJwt(refreshToken),
                loginRequest.getEmail(),
                jwtTokenProvider.getExpirationDate(refreshToken)
        );

        return ResponseEntity.ok(new UserResponseDTO("success", "Login successful", accessToken, refreshToken, this.userService.getUserRoles(accessToken)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordDTO forgotPasswordDTO) {
        this.passwordResetService.createPasswordResetToken(forgotPasswordDTO.getEmail());
        return ResponseEntity.ok("A reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordDTO resetPasswordDTO) {
        if (!this.passwordResetService.validatePasswordResetToken(resetPasswordDTO.getToken())) {
            return ResponseEntity.badRequest().body("Invalid or expired token.");
        }
        boolean reset = this.passwordResetService.resetPassword(resetPasswordDTO.getToken(), resetPasswordDTO.getPassword());
        return reset ? ResponseEntity.ok("Your password has been updated successfully.") : ResponseEntity.badRequest().body("Password reset failed.");
    }

    @PutMapping("/wishlist")
    public ResponseEntity<?> addToWishlist(@RequestBody @Valid ClothingPageDTO clothingPageDTO,
                                           HttpServletRequest request) {
        if (!this.userService.addToWishList(clothingPageDTO, request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Clothing item not found"));
        }

        return ResponseEntity.ok(Map.of("message", "Item successfully added to wishlist"));
    }

    @GetMapping("/wishlist")
    public ResponseEntity<?> getWishlist(HttpServletRequest request) {
        return ResponseEntity.ok(this.userService.getFavorites(request));
    }

    @PutMapping("/wishlist/{id}")
    public ResponseEntity<?> removeFromWishlist(@PathVariable Long id,
                                                HttpServletRequest request) {
        this.userService.removeFromWishlist(id, request);
        return ResponseEntity.ok(Map.of("message", "Item successfully removed from wishlist"));
    }
}