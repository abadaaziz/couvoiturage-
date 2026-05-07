package com.covoiturage.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Chauffeur extends Utilisateur {

    private static final long serialVersionUID = 1L;

    public Chauffeur() {
        super();
    }

    public Chauffeur(String nom, String prenom, String email, String motDePasseHash, String telephone) {
        super(nom, prenom, email, motDePasseHash, telephone);
    }

    @Override
    public String getRole() {
        return "CHAUFFEUR";
    }

    private final List<Trajet> trajetsProposes = new ArrayList<>();

    
    private double noteMoyenne = 0.0;

    
    private int nombreAvis = 0;



    public double getNoteMoyenne() { return noteMoyenne; }
    public void setNoteMoyenne(double noteMoyenne) { this.noteMoyenne = noteMoyenne; }

    public int getNombreAvis() { return nombreAvis; }
    public void setNombreAvis(int nombreAvis) { this.nombreAvis = nombreAvis; }

    
    public void ajouterAvis(int note) {
        if (note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 1 et 5, reçu : " + note);
        }
        this.noteMoyenne = (this.noteMoyenne * this.nombreAvis + note) / (this.nombreAvis + 1);
        this.nombreAvis++;
    }

    
    public void ajouterTrajetProposé(Trajet trajet) {
        Objects.requireNonNull(trajet, "Le trajet ne peut pas être null");
        this.trajetsProposes.add(trajet);
    }

    
    public List<Trajet> getTrajetsProposés() {
        return new ArrayList<>(this.trajetsProposes);
    }

    @Override
    public String toString() {
        return "Chauffeur{" +
                "id=" + getId() +
                ", nom='" + getNom() + '\'' +
                ", prenom='" + getPrenom() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", noteMoyenne=" + getNoteMoyenne() +
                '}';
    }
}
