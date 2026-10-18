
package com.ga.waslah.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedBy
    @Column(name = "audit_created_by")
    private Long auditCreatedBy;

    @LastModifiedBy
    @Column(name = "audit_updated_by")
    private Long auditUpdatedBy;

    public Long getAuditCreatedBy() {
        return auditCreatedBy;
    }

    public Long getAuditUpdatedBy() {
        return auditUpdatedBy;
    }
}
