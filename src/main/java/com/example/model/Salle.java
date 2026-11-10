package com.example.model;

import javax.persistence.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "salles")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Salle {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private int version;
    @Column(nullable = false, unique = true, length = 100)
    private String nom;
    @Column(nullable = false)
    private int capacite;
    @Column(nullable = false, length = 80)
    private String batiment;
    @Column(nullable = false)
    private int etage;
    @Column(length = 255)
    private String description;
    @Column(nullable = false)
    private boolean videoprojecteur;
    @Column(nullable = false)
    private boolean accessiblePmr;
    @Column(nullable = false, precision = 10, scale = 2)
    private java.math.BigDecimal tarifHoraire;
    public Long getId() { return id; }
    public int getVersion() { return version; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public int getCapacite() { return capacite; }
    public void setCapacite(int capacite) { this.capacite = capacite; }
    public String getBatiment() { return batiment; }
    public void setBatiment(String batiment) { this.batiment = batiment; }
    public int getEtage() { return etage; }
    public void setEtage(int etage) { this.etage = etage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean getVideoprojecteur() { return videoprojecteur; }
    public void setVideoprojecteur(boolean videoprojecteur) { this.videoprojecteur = videoprojecteur; }
    public boolean getAccessiblePmr() { return accessiblePmr; }
    public void setAccessiblePmr(boolean accessiblePmr) { this.accessiblePmr = accessiblePmr; }
    public java.math.BigDecimal getTarifHoraire() { return tarifHoraire; }
    public void setTarifHoraire(java.math.BigDecimal tarifHoraire) { this.tarifHoraire = tarifHoraire; }
}
