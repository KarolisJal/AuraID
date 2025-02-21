package com.aura.auraid.dto;

import com.aura.auraid.model.AccessRequestStatus;
import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class AccessRequestDTO {
    private Long id;
    
    @NotNull(message = "Resource ID is required")
    private Long resourceId;
    
    @NotNull(message = "Permission ID is required")
    private Long permissionId;
    
    @Size(max = 500, message = "Justification must not exceed 500 characters")
    private String justification;
    
    private AccessRequestStatus status;
    private String approverComment;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    
    // Additional fields for response
    private String resourceName;
    private String permissionName;
    private String requesterName;
    private String approverName;
} 