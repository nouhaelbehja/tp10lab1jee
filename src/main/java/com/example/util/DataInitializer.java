package com.example.util;
import com.example.model.*;
import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DataInitializer {
    private final EntityManager em;
    public DataInitializer(EntityManager em) { this.em = em; }
    public void initData() {
        Transactions.execute(em, () -> {
            if (em.createQuery("select count(u) from Utilisateur u", Long.class).getSingleResult() > 0) return null;
            String[] noms = {"Salma Bennani", "Yassine Alami", "Nora Idrissi"};
            for (int i = 0; i < noms.length; i++) {
                Utilisateur u = new Utilisateur(); u.setNom(noms[i]);
                u.setEmail("membre" + (i + 1) + "@example.com"); u.setTelephone("060000000" + i);
                u.setDepartement(i == 0 ? "Informatique" : "Gestion"); em.persist(u);
                Salle s = new Salle(); s.setNom("Salle " + (char) ('A' + i)); s.setCapacite(20 + i * 10);
                s.setBatiment("Bloc " + (i + 1)); s.setEtage(i); s.setAccessiblePmr(true);
                s.setVideoprojecteur(i != 1); s.setTarifHoraire(BigDecimal.valueOf(100 + 50 * i));
                s.setDescription("Salle equipee pour reunions et ateliers"); em.persist(s);
                Reservation r = new Reservation(); r.setUtilisateur(u); r.setSalle(s);
                LocalDateTime debut = LocalDateTime.now().plusDays(i + 1).withHour(10).withMinute(0).withSecond(0).withNano(0);
                r.setDateDebut(debut); r.setDateFin(debut.plusHours(2)); r.setMotif("Atelier " + (i + 1));
                r.setNombreParticipants(10 + i); r.setStatut(StatutReservation.CONFIRMEE); em.persist(r);
            }
            return null;
        });
    }
}
