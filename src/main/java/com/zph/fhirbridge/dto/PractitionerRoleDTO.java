package com.zph.fhirbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for PractitionerRole resource.
 * Links a Practitioner to an Organization with a specific role and specialty.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PractitionerRoleDTO {
    private String id;
    private String practitionerId;
    private String practitionerDisplay;
    private String organizationId;
    private String organizationDisplay;
    private String roleCode;
    private String roleDisplay;
    private String specialtyCode;
    private String specialtyDisplay;
    private boolean active;
}
