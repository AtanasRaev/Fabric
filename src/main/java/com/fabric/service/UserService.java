package com.fabric.service;

import com.fabric.database.dto.clothes.ClothingPageDTO;
import com.fabric.database.dto.clothes.ClothingWishlistDTO;
import com.fabric.database.dto.user.UserDTO;
import com.fabric.database.dto.user.UserEitDTO;
import com.fabric.database.dto.user.UserProfileDTO;
import com.fabric.database.dto.user.UserRegistrationDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface UserService {
    void registerUser(UserRegistrationDTO registrationDTO);

    UserDTO findByEmail(String email);

    UserDTO validateAdmin(HttpServletRequest request);

    UserDTO validateUser(HttpServletRequest request);

    UserProfileDTO getUserProfile(String email);

    List<String> getUserRoles(String accessToken);

    boolean resetUserPassword(UserDTO userDTO);

    boolean editUser(UserEitDTO userEditDTO, HttpServletRequest request);

    boolean addToWishList(ClothingPageDTO clothingPageDTO, HttpServletRequest request);

    List<ClothingWishlistDTO> getFavorites(HttpServletRequest request);

    void removeFromWishlist(Long clothingId, HttpServletRequest request);
}
