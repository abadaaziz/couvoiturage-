package com.covoiturage.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;


public class Paiement implements Serializable {

    private static final long serialVersionUID = 1L;



    public enum StatutPaiement {
        AUTORISE,
        CAPTURE,
        REMBOURSE,
        ECHOUE,
        ANNULE
    }

    public enum MethodePaiement {
        CARTE_BANCAIRE,
        PAYPAL,
        VIREMENT
    }



    private int              id;
    private Reservation      reservation;
    private double           montant;
    private double           montantRembourse;
    private StatutPaiement   statut;
    private MethodePaiement  methode;
    private String           referenceExterne;
    private LocalDateTime    dateAutorisation;
    private LocalDateTime    dateCapture;
    private LocalDateTime    dateRemboursement;
    private String           messageErreur;



    
    public Paiement(Reservation reservation, double montant,
                    MethodePaiement methode, String referenceExterne) {
        if (montant <= 0) {
            throw new IllegalArgumentException("Le montant doit être positif, reçu : " + montant);
        }
        this.reservation      = Objects.requireNonNull(reservation, "La réservation est obligatoire");
        this.montant          = montant;
        this.methode          = Objects.requireNonNull(methode, "La méthode de paiement est obligatoire");
        this.referenceExterne = referenceExterne;
        this.statut           = StatutPaiement.AUTORISE;
        this.dateAutorisation = LocalDateTime.now();
        this.montantRembourse = 0.0;
    }

    
    public Paiement() { }



    
    public void capturer() {
        if (this.statut != StatutPaiement.AUTORISE) {
            throw new IllegalStateException(
                "Impossible de capturer : le paiement est en statut " + this.statut);
        }
        this.statut      = StatutPaiement.CAPTURE;
        this.dateCapture = LocalDateTime.now();
    }

    
    public void rembourser(double montantARembourser) {
        if (this.statut != StatutPaiement.CAPTURE && this.statut != StatutPaiement.AUTORISE) {
            throw new IllegalStateException(
                "Impossible de rembourser : le paiement est en statut " + this.statut);
        }
        if (montantARembourser <= 0 || montantARembourser > this.montant) {
            throw new IllegalArgumentException(
                "Montant de remboursement invalide : " + montantARembourser +
                " (max=" + this.montant + ")");
        }
        this.montantRembourse   += montantARembourser;
        this.statut              = StatutPaiement.REMBOURSE;
        this.dateRemboursement   = LocalDateTime.now();
    }

    
    public void annulerAutorisation() {
        if (this.statut != StatutPaiement.AUTORISE) {
            throw new IllegalStateException(
                "Seule une autorisation peut être annulée, statut actuel : " + this.statut);
        }
        this.statut = StatutPaiement.ANNULE;
    }

    
    public void marquerCommeEchoue(String messageErreur) {
        this.statut        = StatutPaiement.ECHOUE;
        this.messageErreur = messageErreur;
    }



    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Reservation getReservation() { return reservation; }
    public void setReservation(Reservation reservation) {
        this.reservation = Objects.requireNonNull(reservation);
    }

    public double getMontant() { return montant; }
    public void setMontant(double montant) { this.montant = montant; }

    public double getMontantRembourse() { return montantRembourse; }
    public void setMontantRembourse(double montantRembourse) {
        this.montantRembourse = montantRembourse;
    }

    public StatutPaiement getStatut() { return statut; }
    public void setStatut(StatutPaiement statut) { this.statut = Objects.requireNonNull(statut); }

    public MethodePaiement getMethode() { return methode; }
    public void setMethode(MethodePaiement methode) { this.methode = methode; }

    public String getReferenceExterne() { return referenceExterne; }
    public void setReferenceExterne(String referenceExterne) {
        this.referenceExterne = referenceExterne;
    }

    public LocalDateTime getDateAutorisation() { return dateAutorisation; }
    public void setDateAutorisation(LocalDateTime dateAutorisation) {
        this.dateAutorisation = dateAutorisation;
    }

    public LocalDateTime getDateCapture() { return dateCapture; }
    public void setDateCapture(LocalDateTime dateCapture) { this.dateCapture = dateCapture; }

    public LocalDateTime getDateRemboursement() { return dateRemboursement; }
    public void setDateRemboursement(LocalDateTime dateRemboursement) {
        this.dateRemboursement = dateRemboursement;
    }

    public String getMessageErreur() { return messageErreur; }
    public void setMessageErreur(String messageErreur) { this.messageErreur = messageErreur; }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paiement)) return false;
        Paiement paiement = (Paiement) o;
        return id == paiement.id && Objects.equals(referenceExterne, paiement.referenceExterne);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, referenceExterne);
    }

    @Override
    public String toString() {
        return "Paiement{" +
               "id=" + id +
               ", montant=" + montant + " DT" +
               ", montantRembourse=" + montantRembourse + " DT" +
               ", statut=" + statut +
               ", methode=" + methode +
               ", reference='" + referenceExterne + '\'' +
               ", dateAutorisation=" + dateAutorisation +
               '}';
    }
}
