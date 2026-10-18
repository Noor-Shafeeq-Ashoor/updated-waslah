
package com.ga.waslah.service;

import com.ga.waslah.model.AuditLog;
import com.ga.waslah.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

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
