package com.bookcover.archive.exception;

import java.util.UUID;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final UUID resourceId;

    public ResourceNotFoundException(String resourceType, UUID resourceId) {
        super(resourceType + " not found with id: " + resourceId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }
}
