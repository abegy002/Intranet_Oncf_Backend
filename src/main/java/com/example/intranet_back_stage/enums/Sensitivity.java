// src/main/java/.../model/enums/Sensitivity.java
package com.example.intranet_back_stage.enums;

public enum Sensitivity {
    INTERNAL,      // visible to all authenticated users
    SENSITIVE,     // visible to roles: HR or ADMIN (and owner)
    CONFIDENTIAL   // visible to role: ADMIN (and owner)
}
