package com.covoiturage.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entité représentant une transaction de paiement.
 * <p>
 * Règles métier :
 * <ul>
 *   <li>AUTORISATION : fonds bloqués immédiatement lors de la réservation</li>
 *   <li>CAPTURE : montant débité réellement lors de la confirmation du chauffeur</li>
 *   <li>REMBOURSEMENT : restitution partielle ou totale</li>
 * </ul>
 * </p>
 */
public class Paiement {

    // ── Énumérations ──────────────────────────────────────────────────────────

    public enum StatutPaiement {
        AUTORISE,     // fonds bloqués, en attente de confirmation
        CAPTURE,      // paiement effectivement débité
        REMBOURSE,    // remboursé (partiel ou total)
        ECHOUE,       // paiement refusé
        ANNULE        // autorisation annulée (avant capture)
    }

    public enum MethodePaiement {
        CARTE_BANCAIRE,
        PAYPAL,
        VIREMENT
    }

    // ── Champs privés ─────────────────────────────────────────────────────────

    private int              id;
    private Reservation      reservation;
    private double           montant;
    private double           montantRembourse;
    private StatutPaiement   statut;
    private MethodePaiement  methode;
    private String           referenceExterne;   // référence chez le prestataire (ex: Stripe)
    private LocalDateTime    dateAutorisation;
    private LocalDateTime    dateCapture;
    private LocalDateTime    dateRemboursement;
    private String           messageErreur;

    // ── Constructeur ──────────────────────────────────────────────────────────

    /**
     * @param reservation      Réservation associée à ce paiement
     * @param montant          Montant à payer en euros
     * @param methode          Méthode de paiement choisie
     * @param referenceExterne Référence retournée par le prestataire de paiement
     */
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

    /** Constructeur par défaut pour JDBC */
    public Paiement() { }

    // ── Méthodes métier ───────────────────────────────────────────────────────

    /**
     * Capture le paiement (débit réel suite à l'acceptation du chauffeur).
     *
     * @throws IllegalStateException si le statut n'est pas AUTORISE
     */
    public void capturer() {
        if (this.statut != StatutPaiement.AUTORISE) {
            throw new IllegalStateException(
                "Impossible de capturer : le paiement est en statut " + this.statut);
        }
        this.statut      = StatutPaiement.CAPTURE;
        this.dateCapture = LocalDateTime.now();
    }

    /**
     * Effectue un remboursement (partiel ou total).
     *
     * @param montantARembourser Montant à rembourser (doit être ≤ montant capturé)
     * @throws IllegalArgumentException si le montant est invalide
     * @throws IllegalStateException    si le paiement n'est pas capturé
     */
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

    /**
     * Annule l'autorisation (avant capture uniquement).
     *
     * @throws IllegalStateException si le paiement a déjà été capturé
     */
    public void annulerAutorisation() {
        if (this.statut != StatutPaiement.AUTORISE) {
            throw new IllegalStateException(
                "Seule une autorisation peut être annulée, statut actuel : " + this.statut);
        }
        this.statut = StatutPaiement.ANNULE;
    }

    /**
     * Marque le paiement comme échoué avec un message d'erreur.
     */
    public void marquerCommeEchoue(String messageErreur) {
        this.statut        = StatutPaiement.ECHOUE;
        this.messageErreur = messageErreur;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

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

    // ── equals / hashCode / toString ─────────────────────────────────────────

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
               ", montant=" + montant + "€" +
               ", montantRembourse=" + montantRembourse + "€" +
               ", statut=" + statut +
               ", methode=" + methode +
               ", reference='" + referenceExterne + '\'' +
               ", dateAutorisation=" + dateAutorisation +
               '}';
    }
}
