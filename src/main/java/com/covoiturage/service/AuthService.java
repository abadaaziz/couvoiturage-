package com.covoiturage.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.covoiturage.dao.UtilisateurDAO;
import com.covoiturage.exception.AuthenticationException;
import com.covoiturage.exception.AuthenticationException.Raison;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Admin;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.model.Utilisateur.StatutCompte;
import com.covoiturage.util.PasswordUtils;

/**
 * Service d'authentification et de gestion des comptes.
 * <p>
 * Responsabilité unique (SRP) : tout ce qui concerne l'identité utilisateur.
 * </p>
 */
public class AuthService {

    /** Taille minimale du mot de passe */
    private static final int LONGUEUR_MIN_MDP = 8;
    private static final int MAX_TENTATIVES_CONNEXION = 5;

    private final UtilisateurDAO utilisateurDAO;

    public AuthService() {
        this.utilisateurDAO = new UtilisateurDAO();
    }

    /** Constructeur pour injection de dépendance (tests unitaires) */
    public AuthService(UtilisateurDAO utilisateurDAO) {
        this.utilisateurDAO = utilisateurDAO;
    }

    // ── Méthodes publiques ────────────────────────────────────────────────────

    /**
     * Crée un nouveau compte utilisateur.
     * <ol>
     *   <li>Vérifie que l'email n'est pas déjà utilisé</li>
     *   <li>Valide la robustesse du mot de passe</li>
     *   <li>Hache le mot de passe avant persistance</li>
     *   <li>Enregistre le compte avec statut EN_ATTENTE_VALIDATION</li>
     * </ol>
     *
     * @param nom           Nom de famille
     * @param prenom        Prénom
     * @param email         Adresse email (doit être unique)
     * @param motDePasse    Mot de passe en clair (sera haché)
     * @param telephone     Numéro de téléphone
    * @param role          Rôle choisi (PASSAGER ou CHAUFFEUR)
     * @return Utilisateur créé avec son id généré
     * @throws IllegalArgumentException si les données sont invalides
     * @throws RuntimeException si l'email est déjà pris ou erreur SQL
     */
    public Utilisateur creerCompte(String nom, String prenom, String email,
                                   String motDePasse, String telephone, String role) {
        // ── Validations ──────────────────────────────────────────────────────
        validerEmail(email);
        validerMotDePasse(motDePasse);
        validerNomPrenom(nom, prenom);

        try {
            // Vérification unicité de l'email
            if (utilisateurDAO.emailExiste(email)) {
                throw new IllegalArgumentException(
                    "Un compte avec l'adresse email '" + email + "' existe déjà.");
            }

            // Hachage du mot de passe (ne jamais stocker en clair)
            String hashMdp = PasswordUtils.hacher(motDePasse);

            // Création de l'entité avec le bon type métier
            Utilisateur utilisateur = creerUtilisateurSelonRole(nom, prenom, email, hashMdp, telephone, role);
            utilisateur.setStatutCompte(StatutCompte.ACTIF); // activation directe pour la démo
            // En production : StatutCompte.EN_ATTENTE_VALIDATION + envoi email confirmation

            // Persistance
            return utilisateurDAO.inserer(utilisateur);

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du compte : " + e.getMessage(), e);
        }
    }

    /**
     * Authentifie un utilisateur par email et mot de passe.
     *
     * @param email      Adresse email
     * @param motDePasse Mot de passe en clair saisi
     * @return Utilisateur authentifié
     * @throws AuthenticationException   si les identifiants sont incorrects ou le compte invalide
     * @throws UtilisateurSuspenduException si le compte est suspendu/bloqué
     */
    public Utilisateur authentifier(String email, String motDePasse)
            throws AuthenticationException, UtilisateurSuspenduException {
        try {
            // Recherche par email
            Optional<Utilisateur> optUtilisateur = utilisateurDAO.trouverParEmail(email);
            if (optUtilisateur.isEmpty()) {
                throw new AuthenticationException(Raison.EMAIL_INCONNU,
                    "Aucun compte trouvé pour l'adresse email : " + email);
            }

            Utilisateur utilisateur = optUtilisateur.get();

            // Vérification du statut du compte
            if (utilisateur.getStatutCompte() == StatutCompte.BLOQUE) {
                throw new AuthenticationException(Raison.COMPTE_BLOQUE,
                    "Votre compte a été définitivement bloqué. Contactez l'administration.");
            }
            if (utilisateur.getStatutCompte() == StatutCompte.SUSPENDU) {
                throw new UtilisateurSuspenduException(email);
            }
            if (utilisateur.getStatutCompte() == StatutCompte.EN_ATTENTE_VALIDATION) {
                throw new AuthenticationException(Raison.COMPTE_NON_VALIDE,
                    "Votre compte n'a pas encore été validé. Vérifiez votre email.");
            }

            // Vérification du mot de passe
            if (!PasswordUtils.verifier(motDePasse, utilisateur.getMotDePasseHash())) {
                int tentatives = utilisateurDAO.incrementerTentativesConnexionEchouees(utilisateur.getId());
                int tentativesRestantes = MAX_TENTATIVES_CONNEXION - tentatives;
                if (tentatives >= MAX_TENTATIVES_CONNEXION) {
                    utilisateurDAO.mettreAJourStatut(utilisateur.getId(), StatutCompte.SUSPENDU);
                    throw new AuthenticationException(Raison.COMPTE_BLOQUE,
                        "Trop de tentatives de connexion. Votre compte est suspendu.");
                }
                throw new AuthenticationException(Raison.MOT_DE_PASSE_INCORRECT,
                    "Mot de passe incorrect. Il vous reste " + tentativesRestantes +
                    " tentative(s) avant suspension.");
            }

            // Mise à jour de la dernière connexion
            LocalDateTime maintenant = LocalDateTime.now();
            utilisateur.setDerniereConnexion(maintenant);
            utilisateurDAO.mettreAJourDerniereConnexion(utilisateur.getId(), maintenant);
            utilisateurDAO.reinitialiserTentativesConnexionEchouees(utilisateur.getId());
            utilisateur.setTentativesConnexionEchouees(0);

            return utilisateur;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur base de données lors de l'authentification : " + e.getMessage(), e);
        }
    }

    /**
     * Invalide la session de l'utilisateur.
     * La gestion effective de la session HTTP est faite dans le servlet.
     *
     * @param utilisateurId Identifiant de l'utilisateur à déconnecter
     */
    public void deconnecter(int utilisateurId) {
        // Ici on pourrait invalider un token ou logguer la déconnexion
        System.out.println("[AuthService] Déconnexion de l'utilisateur #" + utilisateurId);
    }

    /**
     * Suspend temporairement un compte utilisateur.
     *
     * @param utilisateurId Identifiant de l'utilisateur à suspendre
     * @param adminId       Identifiant de l'administrateur qui effectue l'action
     * @throws RuntimeException si l'utilisateur n'existe pas ou erreur SQL
     */
    public void suspendreCompte(int utilisateurId, int adminId) {
        try {
            Optional<Utilisateur> opt = utilisateurDAO.trouverParId(utilisateurId);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Utilisateur #" + utilisateurId + " introuvable.");
            }
            utilisateurDAO.mettreAJourStatut(utilisateurId, StatutCompte.SUSPENDU);
            System.out.println("[AuthService] Compte #" + utilisateurId +
                               " suspendu par l'admin #" + adminId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suspension du compte : " + e.getMessage(), e);
        }
    }

    /**
     * Bloque définitivement un compte utilisateur (action admin irréversible).
     *
     * @param utilisateurId Identifiant de l'utilisateur à bloquer
     * @param adminId       Identifiant de l'administrateur
     */
    public void bloquerUtilisateur(int utilisateurId, int adminId) {
        try {
            Optional<Utilisateur> opt = utilisateurDAO.trouverParId(utilisateurId);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Utilisateur #" + utilisateurId + " introuvable.");
            }
            utilisateurDAO.mettreAJourStatut(utilisateurId, StatutCompte.BLOQUE);
            System.out.println("[AuthService] Compte #" + utilisateurId +
                               " bloqué définitivement par l'admin #" + adminId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du blocage du compte : " + e.getMessage(), e);
        }
    }

    /**
     * Réactive un compte suspendu ou bloqué.
     *
     * @param utilisateurId Identifiant de l'utilisateur à réactiver
     * @param adminId       Identifiant de l'administrateur
     */
    public void reactiverCompte(int utilisateurId, int adminId) {
        try {
            Optional<Utilisateur> opt = utilisateurDAO.trouverParId(utilisateurId);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Utilisateur #" + utilisateurId + " introuvable.");
            }
            utilisateurDAO.mettreAJourStatut(utilisateurId, StatutCompte.ACTIF);
            utilisateurDAO.reinitialiserTentativesConnexionEchouees(utilisateurId);
            System.out.println("[AuthService] Compte #" + utilisateurId +
                               " réactivé par l'admin #" + adminId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la réactivation du compte : " + e.getMessage(), e);
        }
    }

    /**
     * Retourne la liste de tous les utilisateurs (usage admin uniquement).
     */
    public List<Utilisateur> listerTousLesUtilisateurs() {
        try {
            return utilisateurDAO.trouverTous();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des utilisateurs : " + e.getMessage(), e);
        }
    }

    /**
     * Retourne la liste des utilisateurs bloqués (usage admin uniquement).
     */
    public List<Utilisateur> listerUtilisateursBloques() {
        try {
            return utilisateurDAO.trouverParStatut(StatutCompte.BLOQUE);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des comptes bloqués : " + e.getMessage(), e);
        }
    }

    /**
     * Recherche un utilisateur par son id.
     */
    public Optional<Utilisateur> trouverParId(int id) {
        try {
            return utilisateurDAO.trouverParId(id);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de l'utilisateur : " + e.getMessage(), e);
        }
    }

    // ── Validations privées ───────────────────────────────────────────────────

    private void validerEmail(String email) {
        if (email == null || !email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Adresse email invalide : " + email);
        }
    }

    private void validerMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.length() < LONGUEUR_MIN_MDP) {
            throw new IllegalArgumentException(
                "Le mot de passe doit contenir au moins " + LONGUEUR_MIN_MDP + " caractères.");
        }
    }

    private void validerNomPrenom(String nom, String prenom) {
        if (nom == null || nom.isBlank()) {
            throw new IllegalArgumentException("Le nom est obligatoire.");
        }
        if (prenom == null || prenom.isBlank()) {
            throw new IllegalArgumentException("Le prénom est obligatoire.");
        }
    }

    private Utilisateur creerUtilisateurSelonRole(String nom, String prenom, String email,
                                                  String hashMdp, String telephone, String role) {
        return switch (role.toUpperCase()) {
            case "ADMIN" -> new Admin(nom, prenom, email, hashMdp, telephone);
            case "CHAUFFEUR" -> new Chauffeur(nom, prenom, email, hashMdp, telephone);
            case "PASSAGER" -> new Passager(nom, prenom, email, hashMdp, telephone);
            default -> throw new IllegalArgumentException("Role invalide : " + role);
        };
    }
}
