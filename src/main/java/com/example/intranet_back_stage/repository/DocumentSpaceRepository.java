// DocumentSpaceRepository.java
package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.DocumentSpace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentSpaceRepository extends JpaRepository<DocumentSpace, Long> {
    List<DocumentSpace> findByCreatedById(Long userId);
    List<DocumentSpace> findByNameContainingIgnoreCase(String name);
}