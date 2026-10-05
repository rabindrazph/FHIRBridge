package com.zph.fhirbridge.service;

import com.zph.fhirbridge.dto.PatientDTO;

import java.util.List;

/**
 * Service for managing FHIR Patient resources.
 */
public interface PatientService {

    /**
     * Create a new Patient from DTO.
     */
    PatientDTO createPatient(PatientDTO dto);

    /**
     * Get a Patient by ID, returned as DTO.
     */
    PatientDTO getPatient(String id);

    /**
     * Get all Patients.
     */
    List<PatientDTO> getAllPatients();

    /**
     * Update an existing Patient.
     */
    PatientDTO updatePatient(String id, PatientDTO dto);

    /**
     * Delete a Patient.
     */
    void deletePatient(String id);

    /**
     * Serialize a Patient to FHIR JSON.
     */
    String toFhirJson(String id);
}
