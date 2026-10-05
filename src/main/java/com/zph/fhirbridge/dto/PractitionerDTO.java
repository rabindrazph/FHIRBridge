package com.zph.fhirbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Practitioner resource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PractitionerDTO {
    private String id;
    private String familyName;
    private String givenName;
    private String gender;
    private String birthDate;
    private String phone;
    private String email;
    private String qualification;
    private String qualificationSystem;
}
