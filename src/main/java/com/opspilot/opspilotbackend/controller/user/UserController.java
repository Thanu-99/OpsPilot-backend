package com.opspilot.opspilotbackend.controller.user;
import jakarta.validation.Valid;
import com.opspilot.opspilotbackend.dto.UserRequestDto;
import com.opspilot.opspilotbackend.dto.UserResponseDto;
import com.opspilot.opspilotbackend.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto createUser(
            @Valid @RequestBody UserRequestDto request) {

        return userService.createUser(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponseDto> getAllUsers() {

        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto getUserById(
            @PathVariable Long id) {

        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDto updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequestDto request) {

        return userService.updateUser(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(
            @PathVariable Long id) {

        userService.deleteUser(id);

        return "User deleted successfully";
    }
}