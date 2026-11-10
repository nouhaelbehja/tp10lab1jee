package com.example;

import com.example.model.*;
import com.example.service.ReservationService;
import com.example.util.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {
    private EntityManagerFactory emf;
    private EntityManager em;
    private ReservationService service;
    private Long userId, salleId;
    private final LocalDateTime debut = LocalDateTime.now().plusDays(5).withNano(0);

    @BeforeEach void preparer() throws Exception {
        String url = "jdbc:h2:mem:test_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        new MigrationExecutor(url, "sa", "").migrate();
        // Un second passage doit ignorer les migrations deja appliquees.
        new MigrationExecutor(url, "sa", "").migrate();
        emf = Persistence.createEntityManagerFactory("ReservationPU", Map.of("javax.persistence.jdbc.url", url));
        em = emf.createEntityManager();
        service = new ReservationService(em);
        Transactions.execute(em, () -> {
            Utilisateur u = new Utilisateur(); u.setNom("Test"); u.setEmail("test@example.com"); u.setDepartement("Informatique"); em.persist(u);
            Salle s = new Salle(); s.setNom("Salle Test"); s.setCapacite(20); s.setBatiment("Bloc A"); s.setTarifHoraire(new BigDecimal("120.00")); em.persist(s);
            userId = u.getId(); salleId = s.getId(); return null;
        });
        em.clear();
    }
    @AfterEach void fermer() {
        if (em != null && em.isOpen()) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); em.close(); }
        if (emf != null) emf.close();
    }
    private Reservation creer(LocalDateTime d, LocalDateTime f) {
        return service.creer(userId, salleId, d, f, "Reunion projet", 12);
    }
    @Test void creerModifierSupprimer() {
        Reservation r = creer(debut, debut.plusHours(1));
        assertNotNull(r.getId());
        service.modifier(r.getId(), debut.plusDays(1), debut.plusDays(1).plusHours(2), "Atelier", 18);
        em.clear();
        assertEquals("Atelier", em.find(Reservation.class, r.getId()).getMotif());
        service.supprimer(r.getId()); em.clear();
        assertNull(em.find(Reservation.class, r.getId()));
    }
    @Test void refuserChevauchementEtAccepterCreneauAdjacent() {
        creer(debut, debut.plusHours(2));
        assertThrows(IllegalArgumentException.class, () -> creer(debut.plusHours(1), debut.plusHours(3)));
        em.clear();
        assertNotNull(creer(debut.plusHours(2), debut.plusHours(3)).getId());
    }
    @Test void refuserDatesEtCapaciteInvalides() {
        assertThrows(IllegalArgumentException.class, () -> creer(debut, debut));
        em.clear();
        assertThrows(IllegalArgumentException.class, () -> creer(debut.minusDays(10), debut.minusDays(9)));
        em.clear();
        assertThrows(IllegalArgumentException.class, () -> service.creer(userId, salleId, debut, debut.plusHours(1), "Reunion", 21));
        em.clear();
        assertEquals(0L, em.createQuery("select count(r) from Reservation r", Long.class).getSingleResult());
    }
    @Test void annulationLibereLeCreneau() {
        Reservation r = creer(debut, debut.plusHours(1));
        service.annuler(r.getId());
        assertNotNull(creer(debut, debut.plusHours(1)).getId());
    }
    @Test void jpqlEtCriteriaRetournentLesMemesReservations() {
        Reservation r = creer(debut, debut.plusHours(1)); em.clear();
        assertEquals(List.of(r.getId()), service.rechercherJpql("Salle Test", debut.minusDays(1)).stream().map(Reservation::getId).toList());
        assertEquals(List.of(r.getId()), service.rechercherCriteria("Salle Test", debut.minusDays(1)).stream().map(Reservation::getId).toList());
    }
    @Test void cachePartageEntreDeuxEntityManagers() {
        em.clear(); emf.getCache().evictAll();
        var stats = emf.unwrap(SessionFactory.class).getStatistics(); stats.clear();
        EntityManager premier = emf.createEntityManager();
        try { assertNotNull(premier.find(Salle.class, salleId)); } finally { premier.close(); }
        long hits = stats.getSecondLevelCacheHitCount();
        long statements = stats.getPrepareStatementCount();
        EntityManager second = emf.createEntityManager();
        try { assertNotNull(second.find(Salle.class, salleId)); } finally { second.close(); }
        assertTrue(stats.getSecondLevelCacheHitCount() > hits);
        assertEquals(statements, stats.getPrepareStatementCount());
    }
    @Test void verrouillageOptimisteRefuseUneVersionObsolete() {
        EntityManager autre = emf.createEntityManager();
        try {
            em.getTransaction().begin(); autre.getTransaction().begin();
            Utilisateur premier = em.find(Utilisateur.class, userId);
            Utilisateur obsolete = autre.find(Utilisateur.class, userId);
            premier.setNom("Premiere modification"); em.getTransaction().commit();
            obsolete.setNom("Modification obsolete");
            RuntimeException erreur = assertThrows(RuntimeException.class, () -> autre.getTransaction().commit());
            Throwable cause = erreur;
            while (!(cause instanceof OptimisticLockException) && cause.getCause() != null) cause = cause.getCause();
            assertInstanceOf(OptimisticLockException.class, cause);
        } finally {
            if (autre.getTransaction().isActive()) autre.getTransaction().rollback(); autre.close();
        }
    }
    @Test void emailDoitEtreUnique() {
        assertThrows(PersistenceException.class, () -> Transactions.execute(em, () -> {
            Utilisateur u = new Utilisateur(); u.setNom("Doublon"); u.setEmail("test@example.com"); u.setDepartement("Gestion");
            em.persist(u); em.flush(); return null;
        }));
    }
    @Test void initialisationNeDupliquePasLesDonnees() {
        Transactions.execute(em, () -> { em.createQuery("delete from Utilisateur").executeUpdate(); em.createQuery("delete from Salle").executeUpdate(); return null; });
        em.clear();
        new DataInitializer(em).initData(); new DataInitializer(em).initData();
        assertEquals(3L, em.createQuery("select count(u) from Utilisateur u", Long.class).getSingleResult());
        assertEquals(3L, em.createQuery("select count(s) from Salle s", Long.class).getSingleResult());
        assertEquals(3L, em.createQuery("select count(r) from Reservation r", Long.class).getSingleResult());
    }
}
