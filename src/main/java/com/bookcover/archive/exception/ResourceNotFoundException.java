package com.bookcover.archive.exception;

import java.util.UUID;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final UUID resourceId;
    private final String fieldName;
    private final String fieldValue;

    public ResourceNotFoundException(String resourceType, UUID resourceId) {
        super(resourceType + " not found with id: " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.fieldName = "id";
        this.fieldValue = resourceId != null ? resourceId.toString() : null;
    }

    public ResourceNotFoundException(String resourceType, String fieldName, String fieldValue) {
        super(resourceType + " not found with " + fieldName + ": " + fieldValue);
        this.resourceType = resourceType;
        this.resourceId = null;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getFieldValue() {
        return fieldValue;
    }
}
