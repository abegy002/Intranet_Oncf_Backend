package com.example.intranet_back_stage.controller;

        import com.example.intranet_back_stage.dto.PermissionDTO;
        import com.example.intranet_back_stage.service.PermissionService;
        import lombok.RequiredArgsConstructor;
        import org.springframework.http.ResponseEntity;
        import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        return ResponseEntity.ok(permissionService.getAllPermissions());
    }

    @GetMapping("/{id}")
    public PermissionDTO getPermission(@PathVariable Long id) {
        return permissionService.getPermissionById(id);
    }

    @PostMapping("/create")
    public ResponseEntity<PermissionDTO> createPermission(@RequestBody PermissionDTO permissionDTO) {
        return ResponseEntity.ok(permissionService.createPermission(permissionDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionDTO> updatePermission(@PathVariable Long id,
                                                          @RequestBody PermissionDTO permissionDTO) {
        return ResponseEntity.ok(permissionService.updatePermission(id, permissionDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
    }
}
