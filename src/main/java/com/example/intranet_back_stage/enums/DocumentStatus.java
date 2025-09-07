// src/main/java/.../model/enums/DocumentStatus.java
package com.example.intranet_back_stage.enums;

public enum DocumentStatus {
    DRAFT,        // optional: if you later add a "save draft" flow
    IN_REVIEW,    // waiting for HR/ADMIN review
    PUBLISHED,    // approved & published
    REJECTED,     // rejected during review
    WITHDRAWN     // deprecated/retired
}
