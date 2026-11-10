package com.example;
import com.example.model.Salle;
import com.example.service.ReservationService;
import com.example.util.*;
import javax.persistence.*;
import org.hibernate.SessionFactory;
import java.time.LocalDateTime;
import java.util.logging.*;

public class Main {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:h2:file:./data/reservation";
        EntityManagerFactory emf = null;
        try {
            new MigrationExecutor(url, "sa", "").migrate();
            emf = Persistence.createEntityManagerFactory("ReservationPU");
            EntityManager em = emf.createEntityManager();
            Long salleId;
            try {
                new DataInitializer(em).initData();
                salleId = em.createQuery("select s.id from Salle s order by s.id", Long.class).setMaxResults(1).getSingleResult();
                ReservationService service = new ReservationService(em);
                LocalDateTime apres = LocalDateTime.now();
                System.out.println("JPQL : " + service.rechercherJpql("Salle A", apres).size());
                System.out.println("Criteria : " + service.rechercherCriteria("Salle A", apres).size());
            } finally { em.close(); }
            emf.getCache().evictAll();
            emf.unwrap(SessionFactory.class).getStatistics().clear();
            for (int i = 0; i < 2; i++) {
                EntityManager lecture = emf.createEntityManager();
                try { System.out.println("Lecture " + (i + 1) + " : " + lecture.find(Salle.class, salleId).getNom()); }
                finally { lecture.close(); }
            }
            new PerformanceReport(emf).printReport();
        } catch (Exception e) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Echec de l'application", e);
            throw e;
        } finally { if (emf != null) emf.close(); }
    }
}
