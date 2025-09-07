// src/main/java/com/example/intranet_back_stage/model/User.java
package com.example.intranet_back_stage.model;

import com.example.intranet_back_stage.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "users")
public class User implements UserDetails {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, updatable = false)
    private String employeeCode;

    private String username;
    private String password;
    private String firstname;
    private String lastname;
    private String email;

    @Column(precision = 10, scale = 2)
    private BigDecimal salaire;

    /* NEW FIELDS */
    @Column(name = "hire_date")
    private LocalDate hireDate;        // date d’embauche

    @Column(name = "cin", length = 20, unique = true)
    @Size(max = 20)
    private String cin;                // CIN (often 1–2 letters + digits)

    @Column(name = "phone_number", length = 20)
    @Size(max = 20)
    private String phoneNumber;        // e.g. +212 6xx xx xx xx

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.OFFLINE;

    @Column
    private LocalDateTime lastSeen;

    @PrePersist
    public void onCreate() {
        if (status == null) status = UserStatus.OFFLINE;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_permissions",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    public User(Long id, String username, String password, Role role, Set<Permission> permissions) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
        }
        authorities.addAll(
                permissions.stream()
                        .map(p -> new SimpleGrantedAuthority(p.getName()))
                        .collect(Collectors.toSet())
        );
        return authorities;
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
