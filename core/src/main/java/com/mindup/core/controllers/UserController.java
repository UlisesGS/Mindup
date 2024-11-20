package com.mindup.core.controllers;

import com.mindup.core.dtos.User.ResponseLoginDto;
import com.mindup.core.dtos.User.UserDTO;
import com.mindup.core.dtos.User.UserLoginDTO;
import com.mindup.core.dtos.User.UserRegisterDTO;
import com.mindup.core.entities.EmailVerification;
import com.mindup.core.entities.User;
import com.mindup.core.repositories.UserRepository;
import com.mindup.core.services.EmailVerificationService;
import com.mindup.core.services.UserService;
import com.mindup.core.validations.UserValidation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

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
    public ResponseEntity<ResponseLoginDto> loginUser (@RequestBody @Valid UserLoginDTO loginDTO) {
        UserValidation.validateLoginData(loginDTO);

        ResponseLoginDto responseLoginDto = userService.authenticateUser (loginDTO.getEmail(), loginDTO.getPassword());

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

    @PutMapping("/user/password")
    public ResponseEntity<String> changePassword(@RequestParam String email, @RequestBody @Valid String newPassword) {
        userService.changePassword(email, newPassword);
        return ResponseEntity.ok("Password changed successfully");
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
}
