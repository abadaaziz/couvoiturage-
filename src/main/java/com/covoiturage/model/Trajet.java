package com.covoiturage.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entité représentant un trajet de covoiturage proposé par un chauffeur.
 * <p>
 * Règle métier clé : dès que la dernière place disponible est réservée,
 * le statut passe automatiquement à {@link StatutTrajet#COMPLET}.
 * </p>
 */
public class Trajet implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Énumération du statut ─────────────────────────────────────────────────

    public enum StatutTrajet {
        OUVERT,    // places disponibles
        COMPLET,   // plus de place
        ANNULE,    // annulé par le chauffeur
        TERMINE    // trajet effectué
    }

    // ── Champs privés ─────────────────────────────────────────────────────────

    private int    id;
    private String villeDepart;
    private String villeArrivee;
    private LocalDateTime dateHeureDepart;
    private int           placesTotal;
    private int           placesDisponibles;
    private double        prixParPlace;     // en euros
    private StatutTrajet  statut;
    private Chauffeur     chauffeur;
    private String        descriptionVehicule;
    private LocalDateTime dateCreation;

    /** Liste des réservations liées à ce trajet (copie défensive à chaque accès) */
    private final List<Reservation> reservations = new ArrayList<>();

    // ── Constructeur complet ──────────────────────────────────────────────────

    /**
     * @param villeDepart        Ville de départ
     * @param villeArrivee       Ville d'arrivée
     * @param dateHeureDepart    Date et heure du départ
     * @param placesTotal        Nombre total de places proposées
     * @param prixParPlace       Prix par place en euros
     * @param chauffeur          Chauffeur qui propose le trajet
     * @param descriptionVehicule Description du véhicule (marque, couleur, immat.)
     */
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
        this.placesDisponibles    = placesTotal;  // toutes les places disponibles au départ
        this.prixParPlace         = prixParPlace;
        this.chauffeur            = Objects.requireNonNull(chauffeur, "Le chauffeur est obligatoire");
        this.descriptionVehicule  = descriptionVehicule;
        this.statut               = StatutTrajet.OUVERT;
        this.dateCreation         = LocalDateTime.now();
    }

    /** Constructeur par défaut pour JDBC */
    public Trajet() {
        this.statut        = StatutTrajet.OUVERT;
        this.dateCreation  = LocalDateTime.now();
    }

    // ── Méthodes métier ───────────────────────────────────────────────────────

    /**
     * Réserve une place sur ce trajet et met à jour le statut automatiquement.
     * <p>
     * Règle : si {@code placesDisponibles} tombe à 0, le statut passe à COMPLET.
     * </p>
     *
     * @throws IllegalStateException si le trajet n'est pas OUVERT ou s'il est déjà complet
     */
    public void reserverPlace() {
        if (this.statut != StatutTrajet.OUVERT) {
            throw new IllegalStateException(
                "Impossible de réserver : le trajet est en statut " + this.statut);
        }
        if (this.placesDisponibles <= 0) {
            throw new IllegalStateException("Aucune place disponible sur ce trajet");
        }
        this.placesDisponibles--;

        // ── Règle métier : passage automatique à COMPLET ──────────────────────
        if (this.placesDisponibles == 0) {
            this.statut = StatutTrajet.COMPLET;
        }
    }

    /**
     * Libère une place (suite à une annulation de réservation).
     * Si le trajet était COMPLET, il repasse à OUVERT.
     *
     * @throws IllegalStateException si le trajet est ANNULE ou TERMINE
     */
    public void libererPlace() {
        if (this.statut == StatutTrajet.ANNULE || this.statut == StatutTrajet.TERMINE) {
            throw new IllegalStateException(
                "Impossible de libérer une place : le trajet est " + this.statut);
        }
        if (this.placesDisponibles >= this.placesTotal) {
            throw new IllegalStateException("Incohérence : plus de places disponibles que de places totales");
        }
        this.placesDisponibles++;

        // Repasse à OUVERT si le trajet était COMPLET
        if (this.statut == StatutTrajet.COMPLET) {
            this.statut = StatutTrajet.OUVERT;
        }
    }

    /**
     * Calcule le nombre de passagers ayant effectivement confirmé leur réservation.
     *
     * @return Nombre de réservations confirmées
     */
    public int getNombreReservationsConfirmees() {
        return (int) reservations.stream()
            .filter(r -> r.getStatut() == Reservation.StatutReservation.CONFIRMEE)
            .count();
    }

    /**
     * Indique si le chauffeur peut annuler ce trajet sans pénalité.
     * Il peut annuler sans pénalité uniquement s'il n'y a aucune réservation confirmée.
     */
    public boolean peutAnnulerSansPenalite() {
        return getNombreReservationsConfirmees() == 0;
    }

    /**
     * Ajoute une réservation à la liste interne du trajet.
     */
    public void ajouterReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "La réservation ne peut pas être null");
        this.reservations.add(reservation);
    }

    /**
     * Retourne le nombre de minutes restantes avant le départ.
     * Valeur négative si le départ est déjà passé.
     */
    public long minutesAvantDepart() {
        return java.time.Duration.between(LocalDateTime.now(), this.dateHeureDepart).toMinutes();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

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

    /** Retourne une copie défensive de la liste des réservations */
    public List<Reservation> getReservations() {
        return new ArrayList<>(this.reservations);
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

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
               ", prix=" + prixParPlace + "€" +
               ", statut=" + statut +
               '}';
    }
}
