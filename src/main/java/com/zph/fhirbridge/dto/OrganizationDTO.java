package com.zph.fhirbridge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for Organization resource.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDTO {
    private String id;
    private String name;
    private String type;
    private String phone;
    private String email;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
