package com.campusconnect.campusconnect.repository;

import com.campusconnect.campusconnect.Event;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @Query(value = "SELECT * FROM events WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Event> findByIdWithLock(@Param("id") Long id);

    List<Event> findAllByOrderByEventDateAscStartTimeAsc();

    List<Event> findAllByOrderByIdDesc();

    List<Event> findTop10ByOrderByIdDesc();

    List<Event> findByEventDateGreaterThanEqualOrderByEventDateAscStartTimeAsc(LocalDate date);

    List<Event> findByEventDateGreaterThanEqualAndStatusNotOrderByEventDateAscStartTimeAsc(LocalDate date, com.campusconnect.campusconnect.model.EventStatus status);

    @Query("SELECT e FROM Event e WHERE " +
           "(:category IS NULL OR :category = '' OR :category = 'All' OR e.category = :category) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY e.eventDate ASC, e.startTime ASC")
    List<Event> searchEvents(@Param("keyword") String keyword, @Param("category") String category);

    @Query("SELECT e FROM Event e WHERE " +
           "(:status IS NULL OR e.status = :status) AND " +
           "(:category IS NULL OR :category = '' OR :category = 'All' OR e.category = :category) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY e.id DESC")
    List<Event> searchEventsWithStatus(@Param("keyword") String keyword,
                                       @Param("category") String category,
                                       @Param("status") com.campusconnect.campusconnect.model.EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.eventDate >= :today AND e.status != com.campusconnect.campusconnect.model.EventStatus.CANCELLED AND " +
           "(:category IS NULL OR :category = '' OR :category = 'All' OR e.category = :category) AND " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           " LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY e.eventDate ASC, e.startTime ASC")
    List<Event> searchUpcomingEvents(@Param("keyword") String keyword, @Param("category") String category, @Param("today") LocalDate today);

    long countByEventDateGreaterThanEqual(LocalDate date);

    long countByEventDateGreaterThanEqualAndStatusNot(LocalDate date, com.campusconnect.campusconnect.model.EventStatus status);

    long countByStatus(com.campusconnect.campusconnect.model.EventStatus status);

    @Query("SELECT DISTINCT e.category FROM Event e")
    List<String> findDistinctCategories();

    @Query("SELECT e FROM Event e LEFT JOIN EventRegistration r ON r.event = e AND r.status = com.campusconnect.campusconnect.model.RegistrationStatus.CONFIRMED " +
           "GROUP BY e ORDER BY COUNT(r) DESC")
    List<Event> findMostPopularEvents(Pageable pageable);
}
