package com.covoiturage.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entité représentant un utilisateur de la plateforme de covoiturage.
 * Peut être un passager, un chauffeur, ou les deux.
 */
public class Utilisateur {

    // ── Énumérations internes ─────────────────────────────────────────────────

    /** Rôle de l'utilisateur dans la plateforme */
    public enum Role {
        PASSAGER, CHAUFFEUR, ADMIN
    }

    /** Statut du compte */
    public enum StatutCompte {
        ACTIF, SUSPENDU, BLOQUE, EN_ATTENTE_VALIDATION
    }

    // ── Champs privés ─────────────────────────────────────────────────────────

    private int    id;
    private String nom;
    private String prenom;
    private String email;
    private String motDePasseHash;   // toujours stocké hashé — jamais en clair
    private String telephone;
    private Role          role;
    private StatutCompte  statutCompte;
    private double        noteMoyenne;   // de 0.0 à 5.0
    private int           nombreAvis;
    private LocalDateTime dateInscription;
    private LocalDateTime derniereConnexion;

    /** Réservations passées (copie défensive à chaque accès) */
    private final List<Reservation> reservations = new ArrayList<>();

    /** Trajets proposés en tant que chauffeur */
    private final List<Trajet> trajetsProposés = new ArrayList<>();

    // ── Constructeur complet ──────────────────────────────────────────────────

    /**
     * Constructeur principal utilisé lors de la création d'un compte.
     *
     * @param nom           Nom de famille
     * @param prenom        Prénom
     * @param email         Adresse email (identifiant unique)
     * @param motDePasseHash Hash bcrypt du mot de passe
     * @param telephone     Numéro de téléphone (pour SMS)
     * @param role          Rôle initial (PASSAGER, CHAUFFEUR, ADMIN)
     */
    public Utilisateur(String nom, String prenom, String email,
                       String motDePasseHash, String telephone, Role role) {
        this.nom            = Objects.requireNonNull(nom,     "Le nom ne peut pas être null");
        this.prenom         = Objects.requireNonNull(prenom,  "Le prénom ne peut pas être null");
        this.email          = Objects.requireNonNull(email,   "L'email ne peut pas être null");
        this.motDePasseHash = Objects.requireNonNull(motDePasseHash, "Le hash du mot de passe ne peut pas être null");
        this.telephone      = telephone;
        this.role           = Objects.requireNonNull(role, "Le rôle ne peut pas être null");
        this.statutCompte   = StatutCompte.EN_ATTENTE_VALIDATION;
        this.noteMoyenne    = 0.0;
        this.nombreAvis     = 0;
        this.dateInscription = LocalDateTime.now();
    }

    /** Constructeur par défaut nécessaire pour JDBC (ResultSet → objet) */
    public Utilisateur() {
        this.statutCompte    = StatutCompte.EN_ATTENTE_VALIDATION;
        this.dateInscription = LocalDateTime.now();
    }

    // ── Méthodes métier ───────────────────────────────────────────────────────

    /**
     * Ajoute un avis et recalcule la note moyenne.
     *
     * @param note Note entre 1 et 5
     * @throws IllegalArgumentException si la note est hors intervalle
     */
    public void ajouterAvis(int note) {
        if (note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit être comprise entre 1 et 5, reçu : " + note);
        }
        // Recalcul incrémental de la moyenne
        this.noteMoyenne = (this.noteMoyenne * this.nombreAvis + note) / (this.nombreAvis + 1);
        this.nombreAvis++;
    }

    /**
     * Vérifie si le compte est actif (non suspendu ni bloqué).
     */
    public boolean estActif() {
        return StatutCompte.ACTIF.equals(this.statutCompte);
    }

    /**
     * Ajoute une réservation à l'historique du passager.
     * Copie défensive : on ne stocke pas la référence brute.
     */
    public void ajouterReservation(Reservation reservation) {
        Objects.requireNonNull(reservation, "La réservation ne peut pas être null");
        this.reservations.add(reservation);
    }

    /**
     * Ajoute un trajet proposé par ce chauffeur.
     */
    public void ajouterTrajetProposé(Trajet trajet) {
        Objects.requireNonNull(trajet, "Le trajet ne peut pas être null");
        this.trajetsProposés.add(trajet);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = Objects.requireNonNull(nom); }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = Objects.requireNonNull(prenom); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = Objects.requireNonNull(email); }

    public String getMotDePasseHash() { return motDePasseHash; }
    public void setMotDePasseHash(String motDePasseHash) {
        this.motDePasseHash = Objects.requireNonNull(motDePasseHash);
    }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = Objects.requireNonNull(role); }

    public StatutCompte getStatutCompte() { return statutCompte; }
    public void setStatutCompte(StatutCompte statutCompte) {
        this.statutCompte = Objects.requireNonNull(statutCompte);
    }

    public double getNoteMoyenne() { return noteMoyenne; }
    public void setNoteMoyenne(double noteMoyenne) { this.noteMoyenne = noteMoyenne; }

    public int getNombreAvis() { return nombreAvis; }
    public void setNombreAvis(int nombreAvis) { this.nombreAvis = nombreAvis; }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public LocalDateTime getDerniereConnexion() { return derniereConnexion; }
    public void setDerniereConnexion(LocalDateTime derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    /** Retourne une copie défensive de la liste des réservations */
    public List<Reservation> getReservations() {
        return new ArrayList<>(this.reservations);
    }

    /** Retourne une copie défensive de la liste des trajets proposés */
    public List<Trajet> getTrajetsProposés() {
        return new ArrayList<>(this.trajetsProposés);
    }

    // ── equals / hashCode / toString ─────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Utilisateur)) return false;
        Utilisateur that = (Utilisateur) o;
        return id == that.id && Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    @Override
    public String toString() {
        return "Utilisateur{" +
               "id=" + id +
               ", nom='" + nom + '\'' +
               ", prenom='" + prenom + '\'' +
               ", email='" + email + '\'' +
               ", role=" + role +
               ", statutCompte=" + statutCompte +
               ", noteMoyenne=" + String.format("%.2f", noteMoyenne) +
               ", nombreAvis=" + nombreAvis +
               '}';
    }
}
