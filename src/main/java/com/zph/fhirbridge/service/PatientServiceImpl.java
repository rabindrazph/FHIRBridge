package com.zph.fhirbridge.service;

import ca.uhn.fhir.context.FhirContext;
import com.zph.fhirbridge.dto.PatientDTO;
import com.zph.fhirbridge.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of {@link PatientService}.
 * Uses in-memory storage (ConcurrentHashMap) for simplicity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final FhirContext fhirContext;
    private final Map<String, Patient> patientStore = new ConcurrentHashMap<>();

    @Override
    public PatientDTO createPatient(PatientDTO dto) {
        Patient patient = new Patient();
        String id = UUID.randomUUID().toString();
        patient.setId(id);

        // Name
        patient.addName()
                .setFamily(dto.getFamilyName())
                .addGiven(dto.getGivenName());

        // Gender
        if (dto.getGender() != null) {
            patient.setGender(Enumerations.AdministrativeGender.fromCode(dto.getGender().toLowerCase()));
        }

        // Birth date
        if (dto.getBirthDate() != null) {
            patient.setBirthDateElement(new DateType(dto.getBirthDate()));
        }

        // Telecom
        if (dto.getPhone() != null) {
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.PHONE)
                    .setValue(dto.getPhone())
                    .setUse(ContactPoint.ContactPointUse.HOME);
        }
        if (dto.getEmail() != null) {
            patient.addTelecom()
                    .setSystem(ContactPoint.ContactPointSystem.EMAIL)
                    .setValue(dto.getEmail());
        }

        // Address
        if (dto.getCity() != null || dto.getAddressLine() != null) {
            Address address = patient.addAddress();
            if (dto.getAddressLine() != null) address.addLine(dto.getAddressLine());
            if (dto.getCity() != null) address.setCity(dto.getCity());
            if (dto.getState() != null) address.setState(dto.getState());
            if (dto.getPostalCode() != null) address.setPostalCode(dto.getPostalCode());
            if (dto.getCountry() != null) address.setCountry(dto.getCountry());
        }

        patientStore.put(id, patient);
        log.info("Created Patient with ID: {}", id);

        dto.setId(id);
        return dto;
    }

    @Override
    public PatientDTO getPatient(String id) {
        Patient patient = patientStore.get(id);
        if (patient == null) {
            throw new ResourceNotFoundException("Patient not found with ID: " + id);
        }
        return toDTO(patient);
    }

    @Override
    public List<PatientDTO> getAllPatients() {
        return patientStore.values().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public PatientDTO updatePatient(String id, PatientDTO dto) {
        if (!patientStore.containsKey(id)) {
            throw new ResourceNotFoundException("Patient not found with ID: " + id);
        }
        patientStore.remove(id);
        dto.setId(null);
        PatientDTO created = createPatient(dto);
        // Re-store with original ID
        Patient patient = patientStore.remove(created.getId());
        patient.setId(id);
        patientStore.put(id, patient);
        created.setId(id);
        return created;
    }

    @Override
    public void deletePatient(String id) {
        if (patientStore.remove(id) == null) {
            throw new ResourceNotFoundException("Patient not found with ID: " + id);
        }
        log.info("Deleted Patient with ID: {}", id);
    }

    @Override
    public String toFhirJson(String id) {
        Patient patient = patientStore.get(id);
        if (patient == null) {
            throw new ResourceNotFoundException("Patient not found with ID: " + id);
        }
        return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(patient);
    }

    /**
     * Convert Patient resource to DTO.
     */
    private PatientDTO toDTO(Patient patient) {
        PatientDTO.PatientDTOBuilder builder = PatientDTO.builder()
                .id(patient.getIdElement().getIdPart());

        if (patient.hasName()) {
            HumanName name = patient.getNameFirstRep();
            builder.familyName(name.getFamily());
            if (name.hasGiven()) {
                builder.givenName(name.getGivenAsSingleString());
            }
        }

        if (patient.hasGender()) {
            builder.gender(patient.getGender().toCode());
        }

        if (patient.hasBirthDate()) {
            builder.birthDate(patient.getBirthDateElement().getValueAsString());
        }

        for (ContactPoint telecom : patient.getTelecom()) {
            if (telecom.getSystem() == ContactPoint.ContactPointSystem.PHONE) {
                builder.phone(telecom.getValue());
            } else if (telecom.getSystem() == ContactPoint.ContactPointSystem.EMAIL) {
                builder.email(telecom.getValue());
            }
        }

        if (patient.hasAddress()) {
            Address addr = patient.getAddressFirstRep();
            if (addr.hasLine()) builder.addressLine(addr.getLine().get(0).getValue());
            builder.city(addr.getCity())
                    .state(addr.getState())
                    .postalCode(addr.getPostalCode())
                    .country(addr.getCountry());
        }

        return builder.build();
    }
}
