package com.covoiturage.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entité représentant un utilisateur de la plateforme de covoiturage.
 * Classe mère partagée par les profils métier.
 */
public abstract class Utilisateur implements Serializable {

    private static final long serialVersionUID = 1L;

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
    private StatutCompte  statutCompte;
    private LocalDateTime dateInscription;
    private LocalDateTime derniereConnexion;
    private int tentativesConnexionEchouees;

    // ── Constructeur complet ──────────────────────────────────────────────────

    /**
     * Constructeur principal utilisé lors de la création d'un compte.
     *
     * @param nom           Nom de famille
     * @param prenom        Prénom
     * @param email         Adresse email (identifiant unique)
     * @param motDePasseHash Hash bcrypt du mot de passe
     * @param telephone     Numéro de téléphone (pour SMS)
     */
    public Utilisateur(String nom, String prenom, String email,
                       String motDePasseHash, String telephone) {
        this.nom            = Objects.requireNonNull(nom,     "Le nom ne peut pas être null");
        this.prenom         = Objects.requireNonNull(prenom,  "Le prénom ne peut pas être null");
        this.email          = Objects.requireNonNull(email,   "L'email ne peut pas être null");
        this.motDePasseHash = Objects.requireNonNull(motDePasseHash, "Le hash du mot de passe ne peut pas être null");
        this.telephone      = telephone;
        this.statutCompte   = StatutCompte.EN_ATTENTE_VALIDATION;
        this.dateInscription = LocalDateTime.now();
    }

    /** Constructeur par défaut nécessaire pour JDBC (ResultSet → objet) */
    public Utilisateur() {
        this.statutCompte    = StatutCompte.EN_ATTENTE_VALIDATION;
        this.dateInscription = LocalDateTime.now();
    }

    /**
     * Vérifie si le compte est actif (non suspendu ni bloqué).
     */
    public boolean estActif() {
        return StatutCompte.ACTIF.equals(this.statutCompte);
    }

    /** Rôle métier porté par la sous-classe concrète. */
    public abstract String getRole();

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

    public StatutCompte getStatutCompte() { return statutCompte; }
    public void setStatutCompte(StatutCompte statutCompte) {
        this.statutCompte = Objects.requireNonNull(statutCompte);
    }

    public LocalDateTime getDateInscription() { return dateInscription; }
    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public LocalDateTime getDerniereConnexion() { return derniereConnexion; }
    public void setDerniereConnexion(LocalDateTime derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    public int getTentativesConnexionEchouees() { return tentativesConnexionEchouees; }
    public void setTentativesConnexionEchouees(int tentativesConnexionEchouees) {
        this.tentativesConnexionEchouees = Math.max(0, tentativesConnexionEchouees);
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
               ", role=" + getRole() +
               ", statutCompte=" + statutCompte +
               ", tentativesConnexionEchouees=" + tentativesConnexionEchouees +
               '}';
    }
}
