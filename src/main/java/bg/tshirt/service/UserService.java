package bg.tshirt.service;

import bg.tshirt.database.dto.user.UserDTO;
import bg.tshirt.database.dto.user.UserProfileDTO;
import bg.tshirt.database.dto.user.UserRegistrationDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface UserService {
    void registerUser(UserRegistrationDTO registrationDTO);

    UserDTO findByEmail(String email);

    UserDTO validateAdmin(HttpServletRequest request);

    UserDTO validateUser(HttpServletRequest request);

    UserProfileDTO getUserProfile(HttpServletRequest request);

    List<String> getUserRoles(String accessToken);

    boolean resetUserPassword(UserDTO userDTO);
}
