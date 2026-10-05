package com.zph.fhirbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for Patient resource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientDTO {
    private String id;
    private String familyName;
    private String givenName;
    private String birthDate;
    private String gender;
    private String phone;
    private String email;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
