-- Enrichissement du modele et index des recherches de disponibilite.
ALTER TABLE salles ADD COLUMN description VARCHAR(255);
CREATE INDEX idx_reservation_creneau ON reservations(salle_id, statut, dateDebut, dateFin);
CREATE INDEX idx_reservation_utilisateur ON reservations(utilisateur_id);
-- email possede deja une contrainte UNIQUE dans V1 : aucun index redondant.
