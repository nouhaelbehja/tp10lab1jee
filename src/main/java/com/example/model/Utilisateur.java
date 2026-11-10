package com.example.model;

import javax.persistence.*;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "utilisateurs")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Utilisateur {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private int version;
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    @Column(nullable = false, length = 100)
    private String nom;
    @Column(length = 30)
    private String telephone;
    @Column(nullable = false, length = 80)
    private String departement;
    public Long getId() { return id; }
    public int getVersion() { return version; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }
}
