package com.ga.workspace_service.controller;

import com.ga.workspace_service.security.CurrentUser;
import com.ga.workspace_service.security.JwtPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WhoAmlController {

    private final CurrentUser currentUser;

    @GetMapping("/api/career/whoami")
    public JwtPrincipal whoAmI() {
        return currentUser.get();
    }
}
