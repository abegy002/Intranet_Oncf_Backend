package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.Department;
import com.example.intranet_back_stage.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Optional<Department> findByName(String name);
    List<Department> findAll();
    boolean existsByName(String name);
    List<Department> findByNameIn(List<String> names);
}
