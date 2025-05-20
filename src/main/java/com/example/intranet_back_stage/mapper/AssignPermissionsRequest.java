package com.example.intranet_back_stage.mapper;

import java.util.Set;

public class AssignPermissionsRequest {
    private Set<Long> permissionIds;

    public Set<Long> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(Set<Long> permissionIds) {
        this.permissionIds = permissionIds;
    }
}

