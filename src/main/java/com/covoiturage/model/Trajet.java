package com.covoiturage.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Trajet implements Serializable {

    private static final long serialVersionUID = 1L;



    public enum StatutTrajet {
        OUVERT,
        COMPLET,
        ANNULE,
        TERMINE
    }



    private int    id;
    private String villeDepart;
    private String villeArrivee;
    private LocalDateTime dateHeureDepart;
    private int           placesTotal;
    private int           placesDisponibles;
    private double        prixParPlace;
    private StatutTrajet  statut;
    private Chauffeur     chauffeur;
    private String        descriptionVehicule;
    private LocalDateTime dateCreation;

    
    private final List<Reservation> reservations = new ArrayList<>();



    
    public Trajet(String villeDepart, String villeArrivee,
                  LocalDateTime dateHeureDepart, int placesTotal,
                  double prixParPlace, Chauffeur chauffeur,
                  String descriptionVehicule) {

        if (placesTotal <= 0) {
            throw new IllegalArgumentException("Le nombre de places doit être supérieur à 0");
        }
        if (prixParPlace < 0) {
            throw new IllegalArgumentException("Le prix par place ne peut pas être négatif");
        }
        this.villeDepart          = Objects.requireNonNull(villeDepart, "La ville de départ est obligatoire");
        this.villeArrivee         = Objects.requireNonNull(villeArrivee, "La ville d'arrivée est obligatoire");
        this.dateHeureDepart      = Objects.requireNonNull(dateHeureDepart, "La date de départ est obligatoire");
        this.placesTotal          = placesTotal;
        this.placesDisponibles    = placesTotal;
        this.prixParPlace         = prixParPlace;
        this.chauffeur            = Objects.requireNonNull(chauffeur, "Le chauffeur est obligatoire");
        this.descriptionVehicule  = descriptionVehicule;
        this.statut               = StatutTrajet.OUVERT;
        this.dateCreation         = LocalDateTime.now();
    }

    
    public Trajet() {
        this.statut        = StatutTrajet.OUVERT;
        this.dateCreation  = LocalDateTime.now();
    }



    
    public void reserverPlace() {
        if (this.statut != StatutTrajet.OUVERT) {
            throw new IllegalStateException(
                "Impossible de réserver : le trajet est en statut " + this.statut);
        }
        if (this.placesDisponibles <= 0) {
            throw new IllegalStateException("Aucune place disponible sur ce trajet");
        }
        this.placesDisponibles--;


        if (this.placesDisponibles == 0) {
            this.statut = StatutTrajet.COMPLET;
        }
    }

    
    public void libererPlace() {
        if (this.statut == StatutTrajet.ANNULE || this.statut == StatutTrajet.TERMINE) {
            throw new IllegalStateException(
                "Impossible de libérer une place : le trajet est " + this.statut);
        }
        if (this.placesDisponibles >= this.placesTotal) {
            throw new IllegalStateException("Incohérence : plus de places disponibles que de places totales");
        }
        this.placesDisponibles++;


        if (this.statut == StatutTrajet.COMPLET) {
            this.statut = StatutTrajet.OUVERT;
        }
    }

    
    public int getNombreReservationsConfirmees() {
        return (int) reservations.stream()
            .filter(r -> r.getStatut() == Reservation.StatutReservation.CONFIRMEE)
            .count();
    }

    
    public boolean peutAnnulerSansPenalite() {
        return getNombreReservationsConfirmees() == 0;
    }

    
    public void ajouterReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "La réservation ne peut pas être null");
        this.reservations.add(reservation);
    }

    
    public long minutesAvantDepart() {
        return java.time.Duration.between(LocalDateTime.now(), this.dateHeureDepart).toMinutes();
    }



    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getVilleDepart() { return villeDepart; }
    public void setVilleDepart(String villeDepart) {
        this.villeDepart = Objects.requireNonNull(villeDepart);
    }

    public String getVilleArrivee() { return villeArrivee; }
    public void setVilleArrivee(String villeArrivee) {
        this.villeArrivee = Objects.requireNonNull(villeArrivee);
    }

    public LocalDateTime getDateHeureDepart() { return dateHeureDepart; }
    public void setDateHeureDepart(LocalDateTime dateHeureDepart) {
        this.dateHeureDepart = Objects.requireNonNull(dateHeureDepart);
    }

    public int getPlacesTotal() { return placesTotal; }
    public void setPlacesTotal(int placesTotal) {
        if (placesTotal <= 0) throw new IllegalArgumentException("Places total doit être > 0");
        this.placesTotal = placesTotal;
    }

    public int getPlacesDisponibles() { return placesDisponibles; }
    public void setPlacesDisponibles(int placesDisponibles) {
        this.placesDisponibles = placesDisponibles;
    }

    public double getPrixParPlace() { return prixParPlace; }
    public void setPrixParPlace(double prixParPlace) {
        if (prixParPlace < 0) throw new IllegalArgumentException("Le prix ne peut pas être négatif");
        this.prixParPlace = prixParPlace;
    }

    public StatutTrajet getStatut() { return statut; }
    public void setStatut(StatutTrajet statut) { this.statut = Objects.requireNonNull(statut); }

    public Chauffeur getChauffeur() { return chauffeur; }
    public void setChauffeur(Chauffeur chauffeur) {
        this.chauffeur = Objects.requireNonNull(chauffeur);
    }

    public String getDescriptionVehicule() { return descriptionVehicule; }
    public void setDescriptionVehicule(String descriptionVehicule) {
        this.descriptionVehicule = descriptionVehicule;
    }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    
    public List<Reservation> getReservations() {
        return new ArrayList<>(this.reservations);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trajet)) return false;
        Trajet trajet = (Trajet) o;
        return id == trajet.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Trajet{" +
               "id=" + id +
               ", " + villeDepart + " → " + villeArrivee +
               ", départ=" + dateHeureDepart +
               ", places=" + placesDisponibles + "/" + placesTotal +
               ", prix=" + prixParPlace + " DT" +
               ", statut=" + statut +
               '}';
    }
}
