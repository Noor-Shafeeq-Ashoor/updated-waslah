package com.ga.identity_service.controller;


import com.ga.identity_service.dto.InternalUserSummary;
import com.ga.identity_service.dto.RoleChangeRequest;
import com.ga.identity_service.exception.ResourceNotFoundException;
import com.ga.identity_service.model.User;
import com.ga.identity_service.repository.UserRepository;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Hidden
@Slf4j
@RestController
@RequestMapping("/api/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public List<InternalUserSummary> summary(@RequestParam List<Long> ids) {
        return userRepository.findAllById(ids).stream()
                .map(this::toSummary)
                .toList();
    }

    @PutMapping("/{id}/role")
    @Transactional
    public InternalUserSummary changeRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleChangeRequest request
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setRole(request.role());
        log.info("Role of user {} changed to {} by an internal service call", id, request.role());
        return toSummary(userRepository.save(user));
    }

    private InternalUserSummary toSummary(User user) {
        return new InternalUserSummary(user.getId(), user.getUsername(), user.getRole().name());
    }
}

