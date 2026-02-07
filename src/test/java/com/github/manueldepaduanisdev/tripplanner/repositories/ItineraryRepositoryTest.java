package com.github.manueldepaduanisdev.tripplanner.repositories;

import com.github.manueldepaduanisdev.tripplanner.domain.GeoData;
import com.github.manueldepaduanisdev.tripplanner.domain.Itinerary;
import com.github.manueldepaduanisdev.tripplanner.domain.ItineraryLocation;
import com.github.manueldepaduanisdev.tripplanner.dto.enums.Status;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@DataJpaTest
public class ItineraryRepositoryTest {

    @Autowired
    private ItineraryRepository itineraryRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    private GeoData geoDataStored;

    @BeforeEach
    void setupGeoData() {
        GeoData geodata = GeoData.builder()
                .country("Italia")
                .region("Puglia")
                .province("LE")
                .city("Lecce")
                .latitude(1.0)
                .longitude(2.0)
                .build();

        geoDataStored = testEntityManager.persistAndFlush(geodata);
    }

    @Test
    @DisplayName("CountLocations should count how many locations have to be processed, included itinerary locations of id param")
    void countLocations_ShouldGetNumberOfLocationsCorrectly() {
        // Current itinerary update date
        LocalDateTime now = LocalDateTime.now();

        var itinerarySaved = createItinerary(null, "ITINERARY-ID-PARAM", Status.QUEUED, 2, now);
        createItinerary(null, "OLD-QUEUED", Status.QUEUED, 3, now.minusHours(2));
        createItinerary(null, "OLD-PROCESSING", Status.PROCESSING, 3, now.minusHours(3));
        createItinerary(null, "OLD-COMPLETED", Status.COMPLETED, 1, now.minusHours(5));
        createItinerary(null, "OLD-FAILED", Status.FAILED, 1, now.minusHours(7));
        createItinerary(null, "NEW-QUEUED", Status.QUEUED, 1, now.plusHours(2));

        testEntityManager.flush();
        testEntityManager.clear();
        // Total locations to count: 8

        long countLocations = itineraryRepository.countLocations(itinerarySaved.getId(), now);

        Assertions.assertEquals(8L, countLocations);
    }

    @Test
    @DisplayName("findByIdWithLocationsAndGeoData should find the itinerary with the same id as param id and sessionId as param sessionId and include reference with location and geo data")
    void findByIdWithLocationsAndGeoData_ShouldFindItineraryBySessionIdAndId() {

        Itinerary itinerarySaved = createItinerary(null, "findByIdWithLocationsAndGeoData Test", Status.QUEUED, 5, LocalDateTime.now());

        testEntityManager.flush();
        testEntityManager.clear();

        Optional<Itinerary> result = itineraryRepository.findByIdWithLocationsAndGeoData(itinerarySaved.getSessionId(), itinerarySaved.getId());

        Assertions.assertTrue(result.isPresent());
        Itinerary itinerary = result.get();

        Assertions.assertEquals(itinerarySaved.getSessionId(), itinerary.getSessionId());
        Assertions.assertEquals(itinerarySaved.getId(), itinerary.getId());
        Assertions.assertEquals(5, itinerary.getItineraryLocations().size());

        Assertions.assertTrue(
                itinerary.getItineraryLocations().stream()
                        .allMatch(loc -> loc.getGeoData() != null && loc.getGeoData().getCity().equals("Lecce"))
        );

        boolean isLocationsLoaded = testEntityManager.getEntityManager().getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(itinerary, "itineraryLocations");

        Assertions.assertTrue(isLocationsLoaded);
    }

    @Test
    @DisplayName("findBySessionIdAndStatus should get a list of itineraries processed by same sessionId including reference with location and geo data")
    void findBySessionIdAndStatus_ShouldFindItineraryBySessionId() {
        String sessionId = String.valueOf(UUID.randomUUID());
        createItinerary(sessionId, "findBySessionIdAndStatus", Status.QUEUED, 5, LocalDateTime.now());
        createItinerary(sessionId, "findBySessionIdAndStatus", Status.PROCESSING, 5, LocalDateTime.now());
        createItinerary(sessionId, "findBySessionIdAndStatus", Status.FAILED, 5, LocalDateTime.now());
        createItinerary(sessionId, "findBySessionIdAndStatus", Status.COMPLETED, 5, LocalDateTime.now());
        createItinerary(null, "findBySessionIdAndStatus", Status.QUEUED, 5, LocalDateTime.now());
        createItinerary(null, "findBySessionIdAndStatus", Status.QUEUED, 5, LocalDateTime.now());

        testEntityManager.flush();
        testEntityManager.clear();

        List<Itinerary> list = itineraryRepository.findBySessionIdAndStatus(sessionId, null);

        Assertions.assertFalse(list.isEmpty());
        Assertions.assertEquals(4, list.size());
    }

    @Test
    @DisplayName("findBySessionIdAndStatus should get a list of itineraries processed by status filter including reference with location and geo data")
    void findBySessionIdAndStatus_ShouldFindItineraryByStatusFilter() {
        String sessionIdFilter = String.valueOf(UUID.randomUUID());
        Status statusFilter = Status.QUEUED;

        createItinerary(sessionIdFilter, "findBySessionIdAndStatus", Status.QUEUED, 5, LocalDateTime.now());
        createItinerary(null, "findBySessionIdAndStatus", Status.PROCESSING, 5, LocalDateTime.now());
        createItinerary(sessionIdFilter, "findBySessionIdAndStatus", Status.FAILED, 5, LocalDateTime.now());
        createItinerary(sessionIdFilter, "findBySessionIdAndStatus", Status.COMPLETED, 5, LocalDateTime.now());
        createItinerary(null, "findBySessionIdAndStatus", Status.QUEUED, 5, LocalDateTime.now());
        createItinerary(sessionIdFilter, "findBySessionIdAndStatus", Status.QUEUED, 5, LocalDateTime.now());

        testEntityManager.flush();
        testEntityManager.clear();

        // Should find 2 itineraries
        List<Itinerary> list = itineraryRepository.findBySessionIdAndStatus(sessionIdFilter, statusFilter);

        Assertions.assertFalse(list.isEmpty());
        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.stream().allMatch(l -> l.getSessionId().equals(sessionIdFilter) && l.getStatus() == statusFilter));
        Assertions.assertTrue(list.stream().allMatch(itinerary ->
                !itinerary.getItineraryLocations().isEmpty() &&
                        itinerary.getItineraryLocations().get(0).getGeoData() != null
        ));
    }

    // findFirstBySessionId già testato nei precedenti test

    //TODO: avrei potuto fare i test anche per gli errori, per vedere se ritornava
    // eccezione nel caso in cui non trovasse nulla, ma per questione di tempistiche non l'ho fatto


    // Create and store itinerary
    private Itinerary createItinerary(@Nullable String sessionId, String name, Status status, int locationCount, LocalDateTime updateAt) {
        Itinerary itinerary = Itinerary.builder()
                .status(status)
                .sessionId(sessionId == null ? String.valueOf(UUID.randomUUID()) : sessionId)
                .updatedAt(updateAt)
                .title(name)
                .build();

        List<ItineraryLocation> locations = new ArrayList<>();

        for (int i = 0; i < locationCount; i++) {
            ItineraryLocation location = ItineraryLocation.builder()
                    .orderIndex(i)
                    .geoData(geoDataStored)
                    .currentStop(i == 0)
                    .itinerary(itinerary)
                    .build();

            locations.add(location);
        }

        itinerary.setItineraryLocations(locations);

        Itinerary savedItinerary = testEntityManager.persist(itinerary);

        testEntityManager.flush();
        // This for @UpdateTimestamp annotation on updated_at
        testEntityManager.getEntityManager()
                .createQuery("UPDATE Itinerary i SET i.updatedAt = :date WHERE i.id = :id")
                .setParameter("date", updateAt)
                .setParameter("id", savedItinerary.getId())
                .executeUpdate();

        return savedItinerary;
    }
}