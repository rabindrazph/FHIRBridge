//package com.zph.fhirbridge.controller;
//
//import com.zph.fhirbridge.dto.PractitionerDTO;
//import com.zph.fhirbridge.service.PractitionerService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.List;
//
///**
// * REST Controller for FHIR Practitioner resource operations.
// */
//@RestController
//@RequestMapping("/api/practitioners")
//@RequiredArgsConstructor
//public class PractitionerController {
//
//    private final PractitionerService practitionerService;
//
//    @PostMapping
//    public ResponseEntity<PractitionerDTO> createPractitioner(@RequestBody PractitionerDTO dto) {
//        return ResponseEntity.status(HttpStatus.CREATED).body(practitionerService.createPractitioner(dto));
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<PractitionerDTO> getPractitioner(@PathVariable String id) {
//        return ResponseEntity.ok(practitionerService.getPractitioner(id));
//    }
//
//    @GetMapping
//    public ResponseEntity<List<PractitionerDTO>> getAllPractitioners() {
//        return ResponseEntity.ok(practitionerService.getAllPractitioners());
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<PractitionerDTO> updatePractitioner(@PathVariable String id, @RequestBody PractitionerDTO dto) {
//        return ResponseEntity.ok(practitionerService.updatePractitioner(id, dto));
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deletePractitioner(@PathVariable String id) {
//        practitionerService.deletePractitioner(id);
//        return ResponseEntity.noContent().build();
//    }
//
//    @GetMapping(value = "/{id}/fhir", produces = MediaType.APPLICATION_JSON_VALUE)
//    public ResponseEntity<String> getPractitionerFhirJson(@PathVariable String id) {
//        return ResponseEntity.ok(practitionerService.toFhirJson(id));
//    }
//}
