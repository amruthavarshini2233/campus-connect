package com.campusconnect.campusconnect.repository;

import com.campusconnect.campusconnect.model.EventRegistration;
import com.campusconnect.campusconnect.model.RegistrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    long countByStatus(RegistrationStatus status);

    boolean existsByEventIdAndStudentEmail(Long eventId, String studentEmail);

    boolean existsByEventIdAndRollNumber(Long eventId, String rollNumber);

    Optional<EventRegistration> findByEventIdAndStudentEmail(Long eventId, String studentEmail);

    Optional<EventRegistration> findByEventIdAndRollNumber(Long eventId, String rollNumber);

    List<EventRegistration> findByStudentEmailAndRollNumberOrderByRegisteredAtDesc(String studentEmail, String rollNumber);

    List<EventRegistration> findByStudentEmailOrderByRegisteredAtDesc(String studentEmail);

    List<EventRegistration> findByEventIdOrderByRegisteredAtDesc(Long eventId);

    List<EventRegistration> findByEventIdAndStatusOrderByRegisteredAtDesc(Long eventId, RegistrationStatus status);

    List<EventRegistration> findTop10ByOrderByRegisteredAtDesc();

    List<EventRegistration> findAllByOrderByRegisteredAtDesc();

    @Query("SELECT r FROM EventRegistration r WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(r.studentName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.rollNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.studentEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.event.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR r.status = :status) " +
           "ORDER BY r.registeredAt DESC")
    List<EventRegistration> searchAllRegistrations(
            @Param("keyword") String keyword,
            @Param("status") RegistrationStatus status
    );

    @Query("SELECT r FROM EventRegistration r WHERE " +
           "(:emailStatus IS NULL OR :emailStatus = '' OR :emailStatus = 'ALL' OR r.confirmationEmailStatus = :emailStatus) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(r.studentName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.studentEmail) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.event.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY r.registeredAt DESC")
    List<EventRegistration> searchByEmailStatus(
            @Param("keyword") String keyword,
            @Param("emailStatus") String emailStatus
    );

    long countByConfirmationEmailStatus(String confirmationEmailStatus);

    @Query("SELECT r FROM EventRegistration r WHERE r.event.id = :eventId AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(r.studentName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.rollNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(r.studentEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:status IS NULL OR r.status = :status) " +
           "ORDER BY r.registeredAt DESC")
    List<EventRegistration> searchParticipants(
            @Param("eventId") Long eventId,
            @Param("keyword") String keyword,
            @Param("status") RegistrationStatus status
    );
}
