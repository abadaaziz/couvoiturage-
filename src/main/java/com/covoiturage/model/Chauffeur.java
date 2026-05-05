package com.covoiturage.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sous-classe de Utilisateur représentant un chauffeur.
 */
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

    /** Note moyenne reçue par les passagers (de 0.0 à 5.0). Propre au chauffeur. */
    private double noteMoyenne = 0.0;

    /** Nombre d'avis reçus. Propre au chauffeur. */
    private int nombreAvis = 0;

    // ── Rating (propre au chauffeur) ─────────────────────────────────────────

    public double getNoteMoyenne() { return noteMoyenne; }
    public void setNoteMoyenne(double noteMoyenne) { this.noteMoyenne = noteMoyenne; }

    public int getNombreAvis() { return nombreAvis; }
    public void setNombreAvis(int nombreAvis) { this.nombreAvis = nombreAvis; }

    /**
     * Ajoute un avis et recalcule la note moyenne.
     */
    public void ajouterAvis(int note) {
        if (note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 1 et 5, reçu : " + note);
        }
        this.noteMoyenne = (this.noteMoyenne * this.nombreAvis + note) / (this.nombreAvis + 1);
        this.nombreAvis++;
    }

    /**
     * Ajoute un trajet proposé par ce chauffeur.
     */
    public void ajouterTrajetProposé(Trajet trajet) {
        Objects.requireNonNull(trajet, "Le trajet ne peut pas être null");
        this.trajetsProposes.add(trajet);
    }

    /**
     * Retourne une copie défensive des trajets proposés par le chauffeur.
     */
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
