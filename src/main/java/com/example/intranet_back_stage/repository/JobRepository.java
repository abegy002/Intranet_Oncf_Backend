package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.Job;
import com.example.intranet_back_stage.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findByTitle(String name);
    List<Job> findAll();
    boolean existsByTitle(String name);
    List<Job> findByTitleIn(List<String> names);
}