package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.JobDTO;
import com.example.intranet_back_stage.model.Department;
import com.example.intranet_back_stage.model.Job;
import com.example.intranet_back_stage.repository.DepartmentRepository;
import com.example.intranet_back_stage.repository.JobRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class JobService {

    private final DepartmentRepository departmentRepo;
    private final JobRepository jobRepo;

    public JobDTO createJob(JobDTO jobDTO) {
        Department department = departmentRepo.findById(jobDTO.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        Job job = new Job();
        job.setTitle(jobDTO.getTitle());
        job.setDepartment(department);

        jobRepo.save(job);
        jobDTO.setId(job.getId());
        return jobDTO;
    }

    public List<JobDTO> getAllJobs() {
        return jobRepo.findAll().stream()
                .map(job -> new JobDTO(job.getId(), job.getTitle(), job.getDepartment().getId()))
                .collect(Collectors.toList());
    }

    public JobDTO getJobById(Long id) {
        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));
        return new JobDTO(job.getId(), job.getTitle(), job.getDepartment().getId());
    }

    public JobDTO updateJob(Long id, JobDTO jobDTO) {
        Job job = jobRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Job not found"));

        Department department = departmentRepo.findById(jobDTO.getDepartmentId())
                .orElseThrow(() -> new RuntimeException("Department not found"));

        job.setTitle(jobDTO.getTitle());
        job.setDepartment(department);
        jobRepo.save(job);

        return new JobDTO(job.getId(), job.getTitle(), job.getDepartment().getId());
    }

    public void deleteJob(Long id) {
        jobRepo.deleteById(id);
    }
}
