package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.DepartmentDTO;
import com.example.intranet_back_stage.model.Department;
import com.example.intranet_back_stage.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class DepartmentService {

    private DepartmentRepository departmentRepository;

    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        Department department = new Department();
        department.setName(dto.getName());
        departmentRepository.save(department);
        dto.setId(department.getId());
        return dto;
    }

    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findAll().stream()
                .map(dept -> new DepartmentDTO(dept.getId(), dept.getName()))
                .collect(Collectors.toList());
    }

    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));
        return new DepartmentDTO(department.getId(), department.getName());
    }

    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        department.setName(dto.getName());
        departmentRepository.save(department);
        return new DepartmentDTO(department.getId(), department.getName());
    }

    public void deleteDepartment(Long id) {
        departmentRepository.deleteById(id);
    }
}
