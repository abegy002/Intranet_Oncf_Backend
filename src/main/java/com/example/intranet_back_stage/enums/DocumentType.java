// DocumentType.java
package com.example.intranet_back_stage.enums;

public enum DocumentType {
    PDF("application/pdf"),
    WORD("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    WORD_LEGACY("application/msword"),
    EXCEL("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    EXCEL_LEGACY("application/vnd.ms-excel"),
    POWERPOINT("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    POWERPOINT_LEGACY("application/vnd.ms-powerpoint"),
    TEXT("text/plain"),
    IMAGE("image/*"),
    OTHER("application/octet-stream");

    private final String mimeType;

    DocumentType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getMimeType() {
        return mimeType;
    }

    public static DocumentType fromMimeType(String mimeType) {
        for (DocumentType type : values()) {
            if (type.getMimeType().equals(mimeType) ||
                    (type == IMAGE && mimeType.startsWith("image/"))) {
                return type;
            }
        }
        return OTHER;
    }
}
        