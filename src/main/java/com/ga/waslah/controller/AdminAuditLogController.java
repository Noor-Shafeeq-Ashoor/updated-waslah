
package com.ga.waslah.controller;

import com.ga.waslah.model.AuditLog;
import com.ga.waslah.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users & Admin")
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AdminAuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AdminAuditLogController(
            AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Operation(
            summary = "List audit-log entries (admin)",
            description = "Paginated and sortable audit trail (actor, action, target, timestamp). Query parameters: page, size, sort (e.g. timestamp,desc). ADMIN only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of audit-log entries"),
            @ApiResponse(responseCode = "401", description = "Missing, invalid or expired JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not allowed (wrong role or not the owner)")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<AuditLog> getAuditLogs(
            @PageableDefault(size = 10, sort = "timestamp")
            Pageable pageable) {

        return auditLogRepository.findAll(pageable);
    }
}
