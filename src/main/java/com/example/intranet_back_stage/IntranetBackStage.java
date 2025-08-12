package com.example.intranet_back_stage;

import com.example.intranet_back_stage.model.Role;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.RoleRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.UUID;


@SpringBootApplication
public class IntranetBackStage{

//    @Autowired
//    private UserRepository userRepository;
//    @Override
//    public void run(String... args) throws Exception {
//        // Create roles if they don't exist
//        Role adminRole = createRoleIfNotExist("ADMIN");
//        Role managerRole = createRoleIfNotExist("MANAGER");
//        Role hrRole = createRoleIfNotExist("HR");
//        Role userRole = createRoleIfNotExist("USER");
//
//        // Create users if not exist
//        createUserIfNotExist("admin", "admin123", adminRole);
//        createUserIfNotExist("manager", "manager123", managerRole);
//        createUserIfNotExist("hr", "hr123456", hrRole);
//        createUserIfNotExist("user", "user123", userRole);
//    }
//
//    private Role createRoleIfNotExist(String roleName) {
//        return roleRepository.findByName(roleName).orElseGet(() -> {
//            Role role = new Role();
//            role.setName(roleName);
//            roleRepository.save(role);
//            System.out.println("✅ Created role: " + roleName);
//            return role;
//        });
//    }
//
//    private void createUserIfNotExist(String username, String password, Role role) {
//        if (userRepository.findByUsername(username).isEmpty()) {
//            User user = new User();
//
//            String code;
//            do {
//                code = "EMP-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
//            } while (userRepository.existsByEmployeeCode(code));
//
//            user.setEmployeeCode(code); // ✅ IMPORTANT: set the employeeCode
//            user.setUsername(username);
//            user.setPassword(passwordEncoder.encode(password));
//            user.setRole(role);
//
//            // Set default fields to avoid nulls
//            user.setFirstname("System");
//            user.setLastname(username.toUpperCase());
//            user.setEmail(username + "@oncf.ma");
//            user.setSalaire(BigDecimal.valueOf(0.00));
//
//            userRepository.save(user);
//            System.out.println("✅ Created user: " + username + " with role: " + role.getName());
//        } else {
//            System.out.println("ℹ️ User '" + username + "' already exists.");
//        }
//    }
//
//
//
//    @Autowired
//    private RoleRepository roleRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
    public static void main(String[] args) {
        SpringApplication.run(IntranetBackStage.class, args);
    }
}
