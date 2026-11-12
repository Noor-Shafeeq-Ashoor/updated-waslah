
package com.ga.identity_service.service;

import com.ga.identity_service.model.AuditLog;
import com.ga.identity_service.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(
            String actor,
            String action,
            String targetType,
            Long targetId) {

        AuditLog auditLog = new AuditLog();
        auditLog.setActor(actor);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);

        auditLogRepository.save(auditLog);
    }
}
