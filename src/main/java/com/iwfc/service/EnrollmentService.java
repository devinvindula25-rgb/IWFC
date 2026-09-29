package com.iwfc.service;

import com.iwfc.exception.DuplicateDataException;
import com.iwfc.exception.InvalidBookingException;
import com.iwfc.model.ChangeRequestStatus;
import com.iwfc.model.Enrollment;
import com.iwfc.model.InstructorChange;
import com.iwfc.repository.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing member fitness plan enrolments and instructor changes.
 */
public class EnrollmentService {
    private final Repository<Enrollment> enrollmentRepo;
    private final Repository<InstructorChange> changeRequestRepo;

    public EnrollmentService() {
        this.enrollmentRepo = new Repository<>();
        this.changeRequestRepo = new Repository<>();
    }

    public Enrollment enrollMember(String memberId, String planId, String instructorId, int durationWeeks) throws DuplicateDataException {
        // Check if member is already enrolled in this exact plan
        boolean alreadyEnrolled = enrollmentRepo.findAll().stream()
            .anyMatch(e -> e.getMemberId().equals(memberId) && e.getPlanId().equals(planId) && e.getExpirationDate().isAfter(LocalDate.now()));
            
        if (alreadyEnrolled) {
            throw new DuplicateDataException("Member is already actively enrolled in plan " + planId);
        }

        String id = "ENR" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        Enrollment enrollment = new Enrollment(id, memberId, planId, instructorId, LocalDate.now(), LocalDate.now().plusWeeks(durationWeeks));
        
        enrollmentRepo.save(enrollment);
        return enrollment;
    }

    public List<Enrollment> getEnrollmentsForMember(String memberId) {
        return enrollmentRepo.findAll().stream()
            .filter(e -> e.getMemberId().equals(memberId))
            .collect(Collectors.toList());
    }

    public Enrollment getEnrollmentById(String id) {
        return enrollmentRepo.findById(id).orElse(null);
    }

    public void updateEnrollment(Enrollment enrollment) {
        enrollmentRepo.update(enrollment);
    }

    public List<Enrollment> getEnrollmentsForInstructor(String instructorId) {
        return enrollmentRepo.findAll().stream()
            .filter(e -> instructorId.equals(e.getInstructorId()))
            .collect(Collectors.toList());
    }

    public InstructorChange requestInstructorChange(String enrollmentId, String requestedInstructorId, String reason) throws InvalidBookingException, DuplicateDataException {
        Enrollment enrollment = enrollmentRepo.findById(enrollmentId).orElse(null);
        if (enrollment == null) {
            throw new InvalidBookingException("Enrollment not found.");
        }
        if (requestedInstructorId.equals(enrollment.getInstructorId())) {
            throw new InvalidBookingException("Member is already assigned to this instructor.");
        }

        String id = "REQ" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        InstructorChange req = new InstructorChange(id, enrollmentId, requestedInstructorId, reason, LocalDateTime.now(), ChangeRequestStatus.PENDING);
        
        changeRequestRepo.save(req);
        return req;
    }

    public void approveInstructorChange(String requestId) throws InvalidBookingException {
        InstructorChange req = changeRequestRepo.findById(requestId).orElse(null);
        if (req == null || req.getStatus() != ChangeRequestStatus.PENDING) {
            throw new InvalidBookingException("Invalid or already processed request.");
        }

        Enrollment enrollment = enrollmentRepo.findById(req.getEnrollmentId()).orElse(null);
        if (enrollment != null) {
            enrollment.setInstructorId(req.getRequestedInstructorId());
            enrollmentRepo.update(enrollment);
        }
        
        req.setStatus(ChangeRequestStatus.APPROVED);
        changeRequestRepo.update(req);
    }

    public void rejectInstructorChange(String requestId) throws InvalidBookingException {
        InstructorChange req = changeRequestRepo.findById(requestId).orElse(null);
        if (req == null || req.getStatus() != ChangeRequestStatus.PENDING) {
            throw new InvalidBookingException("Invalid or already processed request.");
        }

        req.setStatus(ChangeRequestStatus.REJECTED);
        changeRequestRepo.update(req);
    }
    
    public List<InstructorChange> getPendingChangeRequests() {
        return changeRequestRepo.findAll().stream()
            .filter(r -> r.getStatus() == ChangeRequestStatus.PENDING)
            .collect(Collectors.toList());
    }
}
