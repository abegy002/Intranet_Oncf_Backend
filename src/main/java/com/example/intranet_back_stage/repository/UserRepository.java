package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.enums.UserStatus;
import com.example.intranet_back_stage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Convenience (JpaRepository already provides findById, but keep if you use it explicitly)
    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    @Query("select u from User u where u.role.name = :roleName")
    List<User> findAllByRoleName(@Param("roleName") String roleName);

    List<User> findAll();

    Boolean existsByEmployeeCode(String code);

    Optional<User> findByEmployeeCode(String employeeCode);

    /** Return pairs of (id, username) for a list of user IDs */
    @Query("select u.id, u.username from User u where u.id in :ids")
    List<Object[]> findIdAndUsernameByIdIn(@Param("ids") Collection<Long> ids);

    /** Return pairs of (id, username) for users having the given single role name */
    @Query("select u.id, u.username from User u join u.role r where r.name = :roleName")
    List<Object[]> findIdAndUsernameByRole(@Param("roleName") String roleName);

    /** Return pairs of (id, username) for users having ANY of the given role names */
    @Query("select u.id, u.username from User u join u.role r where r.name in :roleNames")
    List<Object[]> findIdAndUsernameByRoles(@Param("roleNames") Collection<String> roleNames);

    List<User> findByLastSeenIsNullOrLastSeenBefore(LocalDateTime before);

    List<User> findByLastSeenAfter(LocalDateTime after);

    List<User> findByStatus(UserStatus status);
}
