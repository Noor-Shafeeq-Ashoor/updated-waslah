package com.ga.waslah.controller;

import com.ga.waslah.dto.RegisterRequest;
import com.ga.waslah.dto.UserResponseDTO;
import com.ga.waslah.model.User;
import com.ga.waslah.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(
            summary = "Register a new account",
            description = "Always creates an account with the USER role and sends a verification email. The account cannot log in until the email is verified. Limited to 5 requests per minute per IP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid request"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists"),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @SecurityRequirements
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public ResponseEntity<UserResponseDTO> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        User user = userService.register(request);

        UserResponseDTO response = new UserResponseDTO(
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Check that a JWT is accepted",
            description = "Returns a fixed message when the supplied JWT is valid."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT accepted"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT")
    })
    @GetMapping("/test")
    public String test() {
        return "JWT works!";
    }
}