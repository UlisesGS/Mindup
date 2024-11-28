package com.mindup.core.controllers;

import com.mindup.core.dtos.PasswordReset.PasswordResetDTO;
import com.mindup.core.dtos.PasswordReset.PasswordResetRequestDTO;
import com.mindup.core.dtos.User.*;
import com.mindup.core.entities.EmailVerification;
import com.mindup.core.entities.User;
import com.mindup.core.repositories.UserRepository;
import com.mindup.core.services.EmailVerificationService;
import com.mindup.core.services.UserService;
import com.mindup.core.validations.UserValidation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/core")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final EmailVerificationService emailVerificationService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody @Valid UserRegisterDTO userRegisterDTO) {
        UserDTO userDTO = userService.registerUser(userRegisterDTO);
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseLoginDto> loginUser(@RequestBody @Valid UserLoginDTO loginDTO) {

        ResponseLoginDto responseLoginDto = userService.authenticateUser(loginDTO.getEmail(), loginDTO.getPassword());

        if (responseLoginDto != null) {
            // Obtener el usuario autenticado
            Optional<User> userOptional = userRepository.findByEmail(loginDTO.getEmail());

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                EmailVerification emailVerification = emailVerificationService.findByUser(user);

                if (emailVerification != null && emailVerification.isVerified()) {
                    return ResponseEntity.ok(responseLoginDto);
                } else {
                    return ResponseEntity.status(403).body(new ResponseLoginDto(null, null, "Account not verified. Please verify your email first."));
                }
            } else {
                return ResponseEntity.status(404).body(new ResponseLoginDto(null, null, "User  not found."));
            }
        } else {
            return ResponseEntity.status(401).body(new ResponseLoginDto(null, null, "Invalid credentials."));
        }
    }

    @GetMapping("/user/profile")
    public ResponseEntity<UserDTO> getUserProfile(@RequestParam String email) {
        return userService.findUserByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}/change-password")
    public ResponseEntity<String> changePassword(
            @PathVariable String userId,
            @RequestBody ChangePasswordDTO changePasswordDTO) {
        try {
            userService.changePassword(userId, changePasswordDTO.getCurrentPassword(), changePasswordDTO.getNewPassword());
            return ResponseEntity.ok("Password updated successfully.");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PutMapping("/user/preferences")
    public ResponseEntity<UserDTO> updateUserPreferences(@RequestParam String email, @RequestBody @Valid String preferences) {
        return userService.findUserByEmail(email)
                .map(user -> {
                    user.setPreferences(preferences);
                    userService.updateUser(user);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/user/logout")
    public ResponseEntity<String> logoutUser(HttpServletRequest request) {
        return ResponseEntity.ok("User logged out successfully.");
    }

    @DeleteMapping("/user/delete-account")
    public ResponseEntity<String> deleteUserAccount(@RequestParam String email) {
        userService.deleteUserAccount(email);
        return ResponseEntity.ok("User account deleted successfully.");
    }

    @PostMapping("/user/{userId}/profile-image/update")
    public ResponseEntity<String> updateProfileImage(
            @PathVariable String userId,
            @RequestBody @Valid ProfileImageDTO profileImageDTO) {
        userService.updateProfileImage(userId, profileImageDTO);
        return ResponseEntity.ok("Profile image updated successfully.");
    }

    @DeleteMapping("/user/{userId}/profile-image/delete")
    public ResponseEntity<String> deleteProfileImage(@PathVariable String userId) {
        userService.deleteProfileImage(userId);
        return ResponseEntity.ok("Profile image deleted successfully.");
    }

    @PutMapping("/user/availability/{professionalId}")
    public ResponseEntity<UserDTO> toggleAvailability(@PathVariable String professionalId) throws IOException {
        UserDTO user = userService.toggleAvailability(professionalId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/user/{userId}/profile")
    public ResponseEntity<UserProfileDTO> getUserProfileById(@PathVariable String userId) {
        UserProfileDTO userProfile = userService.getUserProfile(userId);
        return ResponseEntity.ok(userProfile);
    }

    @PutMapping("/user/{userId}/profile")
    public ResponseEntity<Void> updateUserProfile(
            @PathVariable String userId,
            @Valid @RequestBody UserProfileDTO userProfileDTO) {
        UserValidation.validateUserProfile(userProfileDTO);
        userService.updateUserProfile(userId, userProfileDTO.getName(), userProfileDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/professional/{id}")
    public ResponseEntity<Boolean> findProfessionalByUserIdAndRole(@PathVariable String id) {
        userService.findProfessionalByUserIdAndRole(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/patient/{id}")
    public ResponseEntity<Boolean> findPatientByUserIdAndRole(@PathVariable String id) {
        userService.findPatientByUserIdAndRole(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/requestPwReset")
    public ResponseEntity<String> requestPasswordReset(@RequestBody @Valid PasswordResetRequestDTO requestDTO) {
        ResponseEntity<String> response = userService.requestPasswordReset(requestDTO.getEmail());
        return response;
    }

    @PostMapping("/resetPW")
    public ResponseEntity<String> resetPassword(@RequestBody @Valid PasswordResetDTO resetDTO) {
        return userService.resetPassword(resetDTO.getToken(), resetDTO.getNewPassword());
    }
}
