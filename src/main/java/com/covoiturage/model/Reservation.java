package com.covoiturage.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;



    public enum StatutReservation {
        EN_ATTENTE,
        CONFIRMEE,
        ANNULEE,
        REMBOURSEE
    }



    
    private static final long SEUIL_REMBOURSEMENT_TOTAL_HEURES = 24;

    
    private static final double TAUX_REMBOURSEMENT_PARTIEL = 0.50;



    private int              id;
    private Trajet           trajet;
    private Passager         passager;
    private int              nombrePlaces;
    private double           montantTotal;
    private StatutReservation statut;
    private LocalDateTime    dateReservation;
    private LocalDateTime    dateAnnulation;
    private double           montantRembourse;
    private String           referenceTransaction;
    private Integer          notePassager;



    
    public Reservation(Trajet trajet, Passager passager, int nombrePlaces) {
        if (nombrePlaces <= 0) {
            throw new IllegalArgumentException("Le nombre de places doit être supérieur à 0");
        }
        this.trajet        = Objects.requireNonNull(trajet,   "Le trajet est obligatoire");
        this.passager      = Objects.requireNonNull(passager, "Le passager est obligatoire");
        this.nombrePlaces  = nombrePlaces;
        this.montantTotal  = trajet.getPrixParPlace() * nombrePlaces;
        this.statut        = StatutReservation.EN_ATTENTE;
        this.dateReservation = LocalDateTime.now();
        this.montantRembourse = 0.0;
    }

    
    public Reservation() {
        this.statut        = StatutReservation.EN_ATTENTE;
        this.dateReservation = LocalDateTime.now();
    }



    
    public double calculerMontantRemboursement() {
        if (trajet == null) return 0.0;


        long heuresAvantDepart = java.time.Duration
            .between(LocalDateTime.now(), trajet.getDateHeureDepart())
            .toHours();

        if (heuresAvantDepart > SEUIL_REMBOURSEMENT_TOTAL_HEURES) {

            return this.montantTotal;
        } else {

            return this.montantTotal * TAUX_REMBOURSEMENT_PARTIEL;
        }
    }

    
    public void annuler() {
        if (this.statut == StatutReservation.ANNULEE ||
            this.statut == StatutReservation.REMBOURSEE) {
            throw new IllegalStateException(
                "La réservation " + id + " est déjà en statut " + this.statut);
        }
        this.montantRembourse = calculerMontantRemboursement();
        this.dateAnnulation   = LocalDateTime.now();
        this.statut           = StatutReservation.ANNULEE;
    }

    
    public void marquerCommeRemboursee() {
        if (this.statut != StatutReservation.ANNULEE) {
            throw new IllegalStateException(
                "Seule une réservation annulée peut être marquée comme remboursée");
        }
        this.statut = StatutReservation.REMBOURSEE;
    }

    
    public void confirmer() {
        if (this.statut != StatutReservation.EN_ATTENTE) {
            throw new IllegalStateException(
                "Seule une réservation en attente peut être confirmée");
        }
        this.statut = StatutReservation.CONFIRMEE;
    }



    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Trajet getTrajet() { return trajet; }
    public void setTrajet(Trajet trajet) { this.trajet = Objects.requireNonNull(trajet); }

    public Passager getPassager() { return passager; }
    public void setPassager(Passager passager) {
        this.passager = Objects.requireNonNull(passager);
    }

    public int getNombrePlaces() { return nombrePlaces; }
    public void setNombrePlaces(int nombrePlaces) {
        if (nombrePlaces <= 0) throw new IllegalArgumentException("Nombre de places > 0 requis");
        this.nombrePlaces = nombrePlaces;
    }

    public double getMontantTotal() { return montantTotal; }
    public void setMontantTotal(double montantTotal) { this.montantTotal = montantTotal; }

    public StatutReservation getStatut() { return statut; }
    public void setStatut(StatutReservation statut) { this.statut = Objects.requireNonNull(statut); }

    public LocalDateTime getDateReservation() { return dateReservation; }
    public void setDateReservation(LocalDateTime dateReservation) {
        this.dateReservation = dateReservation;
    }

    public LocalDateTime getDateAnnulation() { return dateAnnulation; }
    public void setDateAnnulation(LocalDateTime dateAnnulation) {
        this.dateAnnulation = dateAnnulation;
    }

    public double getMontantRembourse() { return montantRembourse; }
    public void setMontantRembourse(double montantRembourse) {
        this.montantRembourse = montantRembourse;
    }

    public Integer getNotePassager() { return notePassager; }
    public void setNotePassager(Integer notePassager) {
        this.notePassager = notePassager;
    }

    public String getReferenceTransaction() { return referenceTransaction; }
    public void setReferenceTransaction(String referenceTransaction) {
        this.referenceTransaction = referenceTransaction;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Reservation)) return false;
        Reservation that = (Reservation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reservation{" +
               "id=" + id +
               ", trajetId=" + (trajet != null ? trajet.getId() : "null") +
               ", passager=" + (passager != null ? passager.getEmail() : "null") +
               ", nombrePlaces=" + nombrePlaces +
               ", montantTotal=" + montantTotal + " DT" +
               ", statut=" + statut +
               ", dateReservation=" + dateReservation +
               '}';
    }
}
