package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    Boolean existsByEmployeeCode (String code);
    Optional<User> findByEmployeeCode(String employeeCode);

}
