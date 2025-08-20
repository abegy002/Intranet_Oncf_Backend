// repository/FolderRepository.java
package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {
    Optional<Folder> findByPath(String path);

    List<Folder> findByParentIsNullOrderByNameAsc();
    List<Folder> findByParentIdOrderByNameAsc(Long parentId);

    boolean existsByParentId(Long parentId);
}
