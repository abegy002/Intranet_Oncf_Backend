package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.model.Role;
import com.example.intranet_back_stage.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class RoleService {

   private RoleRepository roleRepo;

   public List<Role> findAll() { return roleRepo.findAll(); }

    public void createRole(String name) {
        Role role = new Role();
        role.setName(name);
        roleRepo.save(role);
    }

    public void updateRole(Long id, String newName) {
        Role role = roleRepo.findById(id).orElseThrow();
       role.setName(newName);
        roleRepo.save(role);
    }

    public void deleteRole(Long id) {
        roleRepo.deleteById(id);
   }
}
