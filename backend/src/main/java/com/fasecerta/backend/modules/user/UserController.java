package com.fasecerta.backend.modules.user;

import com.fasecerta.backend.modules.user.UserDtos.RegisterUserRequest;
import com.fasecerta.backend.modules.user.UserDtos.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse response = userService.registerPublicUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}