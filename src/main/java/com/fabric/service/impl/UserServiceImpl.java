package com.fabric.service.impl;

import com.fabric.config.JwtTokenProvider;
import com.fabric.database.dto.clothes.ClothingPageDTO;
import com.fabric.database.dto.clothes.ClothingWishlistDTO;
import com.fabric.database.dto.user.UserDTO;
import com.fabric.database.dto.user.UserEitDTO;
import com.fabric.database.dto.user.UserProfileDTO;
import com.fabric.database.dto.user.UserRegistrationDTO;
import com.fabric.database.entity.Clothing;
import com.fabric.database.entity.User;
import com.fabric.database.entity.enums.Role;
import com.fabric.database.repository.UserRepository;
import com.fabric.exceptions.EmailAlreadyInUseException;
import com.fabric.exceptions.ForbiddenException;
import com.fabric.exceptions.NotFoundException;
import com.fabric.exceptions.UnauthorizedException;
import com.fabric.service.ClothingService;
import com.fabric.service.UserService;
import com.fabric.utils.PhoneNumberUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final PhoneNumberUtils phoneNumberUtils;
    private final ClothingService clothingService;
    private final static int ADMINS_COUNT = 2;
    private static final int MODERATOR_COUNT = 2;

    public UserServiceImpl(UserRepository userRepository,
                           JwtTokenProvider jwtTokenProvider,
                           PasswordEncoder passwordEncoder,
                           ModelMapper modelMapper,
                           PhoneNumberUtils phoneNumberUtils,
                           ClothingService clothingService) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.phoneNumberUtils = phoneNumberUtils;
        this.clothingService = clothingService;
    }

    @Override
    @Transactional
    public void registerUser(UserRegistrationDTO registrationDTO) {
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new EmailAlreadyInUseException("Email is already in use");
        }

        this.phoneNumberUtils.validateBulgarianPhoneNumber(registrationDTO.getPhoneNumber());

        Set<Role> roles = determineRoles();

        User user = new User(
                registrationDTO.getEmail(),
                this.passwordEncoder.encode(registrationDTO.getPassword()),
                registrationDTO.getFirstName(),
                registrationDTO.getLastName(),
                this.phoneNumberUtils.formatPhoneNumber(registrationDTO.getPhoneNumber()),
                registrationDTO.getCity(),
                registrationDTO.getRegion(),
                registrationDTO.getAddress(),
                roles
        );

        saveUser(user);
    }

    @Override
    public UserDTO findByEmail(String email) {
        return this.userRepository.findByEmail(email)
                .map(user -> new UserDTO(user.getEmail(), user.getAddress(), user.getRoles()))
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

    @Override
    @Transactional
    @Cacheable(value = "userProfile", key = "#email")
    public UserProfileDTO getUserProfile(String email) {
       return userRepository.findByEmail(email)
                .map(this::mapToUserProfileDTO)
                .orElse(null);
    }

    @Override
    public List<String> getUserRoles(String accessToken) {
        return this.jwtTokenProvider.getRoles(accessToken);
    }

    @Override
    public boolean resetUserPassword(UserDTO userDTO) {
        Optional<User> optional = this.userRepository.findByEmail(userDTO.getEmail());

        if (optional.isEmpty()) {
            return false;
        }
        optional.get().setPassword(this.passwordEncoder.encode(userDTO.getPassword()));
        this.userRepository.save(optional.get());

        return true;
    }

    @Override
    @CacheEvict(value = "userProfile", allEntries = true)
    public boolean editUser(UserEitDTO userEditDTO, HttpServletRequest request) {
        UserDTO userDTO = this.validateUser(request);

        Optional<User> optionalUser = this.userRepository.findByEmail(userDTO.getEmail());
        if (optionalUser.isEmpty()) {
            return false;
        }

        User user = optionalUser.get();

        this.phoneNumberUtils.validateBulgarianPhoneNumber(userEditDTO.getPhoneNumber());

        if (!user.getEmail().equals(userEditDTO.getEmail()) && this.userRepository.existsByEmail(userEditDTO.getEmail())) {
            throw new EmailAlreadyInUseException("Email is already in use");
        }

        user.setFirstName(userEditDTO.getFirstName());
        user.setLastName(userEditDTO.getLastName());
        user.setCity(userEditDTO.getCity());
        user.setRegion(userEditDTO.getRegion());
        user.setAddress(userEditDTO.getAddress());
        user.setEmail(userEditDTO.getEmail());
        user.setPhoneNumber(userEditDTO.getPhoneNumber());

        saveUser(user);
        return true;
    }

    @Transactional
    @Override
    public boolean addToWishList(ClothingPageDTO clothingPageDTO, HttpServletRequest request) {
        User user = getUser(request);
        Clothing clothingEntityById = this.clothingService.getClothingEntityById(clothingPageDTO.getId());
        if (clothingEntityById == null) {
            return false;
        }

        List<Clothing> list = user.getFavorites()
                .stream()
                .filter(c -> c.getId() == clothingPageDTO.getId())
                .toList();

        if (list.isEmpty()) {
            user.addFavorite(clothingEntityById);
            this.userRepository.save(user);
        }

        return true;
    }

    @Override
    @Transactional
    public List<ClothingWishlistDTO> getFavorites(HttpServletRequest request) {
        User user = getUser(request);

        return user.getFavorites()
                .stream()
                .map(c -> {
                    ClothingWishlistDTO dto = this.modelMapper.map(c, ClothingWishlistDTO.class);
                    if (c.getDiscountPrice() != null) {
                        dto.setPrice(c.getDiscountPrice());
                    }

                    c.getImages().forEach(i -> {
                        if (i.getPublicId().contains("_F")) {
                            dto.setImage(i.getPath());
                        }
                    });

                    return dto;
                })
                .toList();
    }

    @Override
    @Transactional
    public void removeFromWishlist(Long clothingId, HttpServletRequest request) {
        User user = getUser(request);
        user.removeFavorite(clothingId);
    }

    private User getUser(HttpServletRequest request) {
        UserDTO userDTO = validateUser(request);
        Optional<User> optional = this.userRepository.findByEmail(userDTO.getEmail());

        if (optional.isEmpty()) {
            throw new NotFoundException("User not found");
        }

        return optional.get();
    }

    private void saveUser(User user) {
        this.userRepository.save(user);
    }

    private UserProfileDTO mapToUserProfileDTO(User user) {
        return modelMapper.map(user, UserProfileDTO.class);
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

        if (userRepository.count() < MODERATOR_COUNT) {
            roles.add(Role.MODERATOR);
        }

        if (userRepository.count() < ADMINS_COUNT) {
            roles.add(Role.ADMIN);
        }

        return roles;
    }
}
