package com.example.service;

import com.example.model.*;
import com.example.util.Transactions;
import javax.persistence.*;
import javax.persistence.criteria.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.*;

public class ReservationService {
    private static final Logger LOG = Logger.getLogger(ReservationService.class.getName());
    private final EntityManager em;
    public ReservationService(EntityManager em) { this.em = em; }

    public Reservation creer(Long utilisateurId, Long salleId, LocalDateTime debut,
                              LocalDateTime fin, String motif, int participants) {
        return journaliser(() -> Transactions.execute(em, () -> {
            Salle salle = em.find(Salle.class, salleId, LockModeType.PESSIMISTIC_WRITE);
            Utilisateur utilisateur = em.find(Utilisateur.class, utilisateurId);
            if (salle == null || utilisateur == null) throw new IllegalArgumentException("Salle ou utilisateur introuvable");
            Reservation r = new Reservation();
            r.setSalle(salle); r.setUtilisateur(utilisateur); r.setStatut(StatutReservation.CONFIRMEE);
            verifier(r, debut, fin, motif, participants);
            r.setDateDebut(debut); r.setDateFin(fin); r.setMotif(motif); r.setNombreParticipants(participants);
            em.persist(r);
            return r;
        }));
    }
    public void modifier(Long id, LocalDateTime debut, LocalDateTime fin, String motif, int participants) {
        journaliser(() -> Transactions.execute(em, () -> {
            Reservation r = trouver(id);
            em.lock(r.getSalle(), LockModeType.PESSIMISTIC_WRITE);
            if (r.getStatut() == StatutReservation.ANNULEE) throw new IllegalArgumentException("Reservation annulee");
            verifier(r, debut, fin, motif, participants);
            r.setDateDebut(debut); r.setDateFin(fin); r.setMotif(motif); r.setNombreParticipants(participants);
            return null;
        }));
    }
    public void annuler(Long id) {
        journaliser(() -> Transactions.execute(em, () -> { trouver(id).setStatut(StatutReservation.ANNULEE); return null; }));
    }
    public void supprimer(Long id) {
        journaliser(() -> Transactions.execute(em, () -> { em.remove(trouver(id)); return null; }));
    }
    private Reservation trouver(Long id) {
        Reservation r = em.find(Reservation.class, id);
        if (r == null) throw new IllegalArgumentException("Reservation introuvable");
        return r;
    }
    private void verifier(Reservation r, LocalDateTime debut, LocalDateTime fin, String motif, int participants) {
        if (debut == null || fin == null || !fin.isAfter(debut) || !debut.isAfter(LocalDateTime.now()))
            throw new IllegalArgumentException("Le creneau doit etre futur et de duree positive");
        if (motif == null || motif.isBlank() || motif.length() > 150)
            throw new IllegalArgumentException("Motif obligatoire, 150 caracteres maximum");
        if (participants <= 0 || participants > r.getSalle().getCapacite())
            throw new IllegalArgumentException("Nombre de participants incompatible avec la capacite");
        Long conflits = em.createQuery("select count(r) from Reservation r where r.salle = :salle "
                + "and r.statut = :statut and r.dateDebut < :fin and r.dateFin > :debut "
                + "and (:id is null or r.id <> :id)", Long.class)
            .setParameter("salle", r.getSalle()).setParameter("statut", StatutReservation.CONFIRMEE)
            .setParameter("debut", debut).setParameter("fin", fin).setParameter("id", r.getId()).getSingleResult();
        if (conflits > 0) throw new IllegalArgumentException("La salle est deja reservee sur ce creneau");
    }
    public List<Reservation> rechercherJpql(String nomSalle, LocalDateTime apres) {
        return em.createQuery("select r from Reservation r join fetch r.salle join fetch r.utilisateur "
                + "where r.salle.nom = :nom and r.dateDebut > :date order by r.dateDebut", Reservation.class)
            .setParameter("nom", nomSalle).setParameter("date", apres)
            .setHint("org.hibernate.cacheable", true).getResultList();
    }
    public List<Reservation> rechercherCriteria(String nomSalle, LocalDateTime apres) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Reservation> q = cb.createQuery(Reservation.class);
        Root<Reservation> r = q.from(Reservation.class);
        r.fetch("salle"); r.fetch("utilisateur");
        q.select(r).where(cb.and(cb.equal(r.get("salle").get("nom"), nomSalle),
            cb.greaterThan(r.<LocalDateTime>get("dateDebut"), apres))).orderBy(cb.asc(r.get("dateDebut")));
        return em.createQuery(q).getResultList();
    }
    private <T> T journaliser(java.util.function.Supplier<T> action) {
        try { T result = action.get(); LOG.info("Operation reservation reussie"); return result; }
        catch (RuntimeException e) { LOG.log(Level.SEVERE, "Operation reservation refusee", e); throw e; }
    }
}
