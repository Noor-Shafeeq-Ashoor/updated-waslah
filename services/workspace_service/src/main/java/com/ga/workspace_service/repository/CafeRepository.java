
package com.ga.workspace_service.repository;

import com.ga.workspace_service.model.Cafe;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;


public interface CafeRepository extends
        JpaRepository<Cafe, Long>,
        JpaSpecificationExecutor<Cafe> {


    List<Cafe> findByManagerUserId(Long managerUserId);
}
