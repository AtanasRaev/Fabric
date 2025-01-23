package bg.tshirt.service.impl;

import bg.tshirt.config.JwtTokenProvider;
import bg.tshirt.database.dto.UserDTO;
import bg.tshirt.database.dto.UserRegistrationDTO;
import bg.tshirt.database.entity.User;
import bg.tshirt.database.entity.enums.Role;
import bg.tshirt.database.repository.UserRepository;
import bg.tshirt.exceptions.EmailAlreadyInUseException;
import bg.tshirt.exceptions.ForbiddenException;
import bg.tshirt.exceptions.NotFoundException;
import bg.tshirt.exceptions.UnauthorizedException;
import bg.tshirt.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final static int ADMINS_COUNT = 2;

    public UserServiceImpl(UserRepository userRepository,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void registerUser(UserRegistrationDTO registrationDTO) {
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new EmailAlreadyInUseException("Email is already in use");
        }

        Set<Role> roles = determineRoles();

        User user = new User(
                registrationDTO.getEmail(),
                this.passwordEncoder.encode(registrationDTO.getPassword()),
                registrationDTO.getAddress(),
                roles
        );

        userRepository.save(user);
    }

    @Override
    public UserDTO findByEmail(String email) {
        return this.userRepository.findByEmail(email)
                .map(user -> new UserDTO(user.getEmail(), user.getAddress()))
                .orElse(null);
    }

    @Override
    public UserDTO validateAdmin(HttpServletRequest request) {
        String token = extractToken(request);
        validateAdminRole(token);
        return findUser(token);
    }

    @Override
    public UserDTO validateUser(HttpServletRequest request) {
        String token = extractToken(request);
        return findUser(token);
    }

    private void validateAdminRole(String token) {
        List<?> rolesFromJwt = this.jwtTokenProvider.getRolesFromJwt(token);
        if (rolesFromJwt == null || rolesFromJwt.isEmpty()) {
            throw new ForbiddenException("Access denied. No roles found in the token.");
        }
        if (!rolesFromJwt.contains("ADMIN")) {
            throw new ForbiddenException("Access denied. Admin privileges required.");
        }
    }

    private UserDTO findUser(String token) {
        String email = jwtTokenProvider.getEmailFromJwt(token);
        if (email == null || email.isEmpty()) {
            throw new UnauthorizedException("Email not found in token.");
        }

        UserDTO user = findByEmail(email);
        if (user == null) {
            throw new NotFoundException("User not found in the system.");
        }
        return user;
    }

    private String extractToken(HttpServletRequest request) {
        String token = this.jwtTokenProvider.getJwtFromRequest(request);
        if (!this.jwtTokenProvider.isValidToken(token, request)) {
            throw new UnauthorizedException("Invalid or expired token.");
        }
        return token;
    }

    private Set<Role> determineRoles() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.USER);
        if (userRepository.count() < ADMINS_COUNT) {
            roles.add(Role.ADMIN);
        }
        return roles;
    }
}
