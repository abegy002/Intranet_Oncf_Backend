package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByName(String name);
    List<Permission> findAll();
    boolean existsByName(String name);
    List<Permission> findByNameIn(List<String> names);
}
