package com.covoiturage.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entité représentant une réservation d'une place sur un trajet.
 * <p>
 * Règles métier :
 * <ul>
 *   <li>Annulation passager : remboursement total si > 24h avant départ, partiel sinon</li>
 *   <li>Le paiement est autorisé immédiatement à la création de la réservation</li>
 *   <li>La capture du paiement s'effectue à l'acceptation du chauffeur</li>
 * </ul>
 * </p>
 */
public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Énumération du statut ─────────────────────────────────────────────────

    public enum StatutReservation {
        EN_ATTENTE,   // réservation créée, en attente d'acceptation chauffeur
        CONFIRMEE,    // acceptée par le chauffeur, paiement capturé
        ANNULEE,      // annulée (passager ou système)
        REMBOURSEE    // annulée et remboursée
    }

    // ── Constantes métier ─────────────────────────────────────────────────────

    /** Seuil en heures pour le remboursement total passager */
    private static final long SEUIL_REMBOURSEMENT_TOTAL_HEURES = 24;

    /** Pourcentage de remboursement partiel (0.5 = 50 %) */
    private static final double TAUX_REMBOURSEMENT_PARTIEL = 0.50;

    // ── Champs privés ─────────────────────────────────────────────────────────

    private int              id;
    private Trajet           trajet;
    private Utilisateur      passager;
    private int              nombrePlaces;        // places réservées (1 par défaut)
    private double           montantTotal;        // prix total = prixParPlace × nombrePlaces
    private StatutReservation statut;
    private LocalDateTime    dateReservation;
    private LocalDateTime    dateAnnulation;
    private double           montantRembourse;    // calculé lors de l'annulation
    private String           referenceTransaction; // référence de l'autorisation de paiement
    private Integer          notePassager;        // note du chauffeur (1-5)

    // ── Constructeur complet ──────────────────────────────────────────────────

    /**
     * @param trajet       Trajet concerné
     * @param passager     Passager qui effectue la réservation
     * @param nombrePlaces Nombre de places réservées (minimum 1)
     */
    public Reservation(Trajet trajet, Utilisateur passager, int nombrePlaces) {
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

    /** Constructeur par défaut pour JDBC */
    public Reservation() {
        this.statut        = StatutReservation.EN_ATTENTE;
        this.dateReservation = LocalDateTime.now();
    }

    // ── Méthodes métier ───────────────────────────────────────────────────────

    /**
     * Calcule le montant à rembourser selon les règles métier :
     * <ul>
     *   <li>Si l'annulation intervient plus de 24h avant le départ → remboursement total</li>
     *   <li>Sinon → remboursement partiel à 50 %</li>
     * </ul>
     *
     * @return Montant à rembourser en euros
     */
    public double calculerMontantRemboursement() {
        if (trajet == null) return 0.0;

        // Nombre d'heures restantes avant le départ
        long heuresAvantDepart = java.time.Duration
            .between(LocalDateTime.now(), trajet.getDateHeureDepart())
            .toHours();

        if (heuresAvantDepart > SEUIL_REMBOURSEMENT_TOTAL_HEURES) {
            // Remboursement total
            return this.montantTotal;
        } else {
            // Remboursement partiel (50%)
            return this.montantTotal * TAUX_REMBOURSEMENT_PARTIEL;
        }
    }

    /**
     * Annule la réservation et calcule le montant à rembourser.
     * Met à jour les champs dateAnnulation et montantRembourse.
     *
     * @throws IllegalStateException si la réservation est déjà annulée ou remboursée
     */
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

    /**
     * Marque la réservation comme REMBOURSEE après le traitement du remboursement.
     */
    public void marquerCommeRemboursee() {
        if (this.statut != StatutReservation.ANNULEE) {
            throw new IllegalStateException(
                "Seule une réservation annulée peut être marquée comme remboursée");
        }
        this.statut = StatutReservation.REMBOURSEE;
    }

    /**
     * Confirme la réservation (capture du paiement).
     *
     * @throws IllegalStateException si la réservation n'est pas EN_ATTENTE
     */
    public void confirmer() {
        if (this.statut != StatutReservation.EN_ATTENTE) {
            throw new IllegalStateException(
                "Seule une réservation en attente peut être confirmée");
        }
        this.statut = StatutReservation.CONFIRMEE;
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Trajet getTrajet() { return trajet; }
    public void setTrajet(Trajet trajet) { this.trajet = Objects.requireNonNull(trajet); }

    public Utilisateur getPassager() { return passager; }
    public void setPassager(Utilisateur passager) {
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

    // ── equals / hashCode / toString ─────────────────────────────────────────

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
               ", montantTotal=" + montantTotal + "€" +
               ", statut=" + statut +
               ", dateReservation=" + dateReservation +
               '}';
    }
}
