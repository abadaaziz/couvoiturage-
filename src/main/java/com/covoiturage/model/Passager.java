package com.covoiturage.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Sous-classe de Utilisateur représentant un passager.
 */
public class Passager extends Utilisateur {

    private static final long serialVersionUID = 1L;

    public Passager() {
        super();
    }

    public Passager(String nom, String prenom, String email, String motDePasseHash, String telephone) {
        super(nom, prenom, email, motDePasseHash, telephone);
    }

    @Override
    public String getRole() {
        return "PASSAGER";
    }

    private final List<Reservation> reservations = new ArrayList<>();

    /**
     * Ajoute une réservation à l'historique du passager.
     */
    public void ajouterReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "La réservation ne peut pas être null");
        this.reservations.add(reservation);
    }

    /**
     * Retourne une copie défensive des réservations du passager.
     */
    public List<Reservation> getReservations() {
        return new ArrayList<>(this.reservations);
    }

    @Override
    public String toString() {
        return "Passager{" +
                "id=" + getId() +
                ", nom='" + getNom() + '\'' +
                ", prenom='" + getPrenom() + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }
}
