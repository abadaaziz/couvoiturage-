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


public class AuthService {

    
    private static final int LONGUEUR_MIN_MDP = 8;
    private static final int MAX_TENTATIVES_CONNEXION = 5;

    private final UtilisateurDAO utilisateurDAO;

    public AuthService() {
        this.utilisateurDAO = new UtilisateurDAO();
    }

    
    public AuthService(UtilisateurDAO utilisateurDAO) {
        this.utilisateurDAO = utilisateurDAO;
    }



    
    public Utilisateur creerCompte(String nom, String prenom, String email,
                                   String motDePasse, String telephone, String role) {

        validerEmail(email);
        validerMotDePasse(motDePasse);
        validerNomPrenom(nom, prenom);

        try {

            if (utilisateurDAO.emailExiste(email)) {
                throw new IllegalArgumentException(
                    "Un compte avec l'adresse email '" + email + "' existe déjà.");
            }


            String hashMdp = PasswordUtils.hacher(motDePasse);


            Utilisateur utilisateur = creerUtilisateurSelonRole(nom, prenom, email, hashMdp, telephone, role);
            utilisateur.setStatutCompte(StatutCompte.ACTIF);



            return utilisateurDAO.inserer(utilisateur);

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du compte : " + e.getMessage(), e);
        }
    }

    
    public Utilisateur authentifier(String email, String motDePasse)
            throws AuthenticationException, UtilisateurSuspenduException {
        try {

            Optional<Utilisateur> optUtilisateur = utilisateurDAO.trouverParEmail(email);
            if (optUtilisateur.isEmpty()) {
                throw new AuthenticationException(Raison.EMAIL_INCONNU,
                    "Aucun compte trouvé pour l'adresse email : " + email);
            }

            Utilisateur utilisateur = optUtilisateur.get();


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

    
    public void deconnecter(int utilisateurId) {

        System.out.println("[AuthService] Déconnexion de l'utilisateur #" + utilisateurId);
    }

    
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

    
    public List<Utilisateur> listerTousLesUtilisateurs() {
        try {
            return utilisateurDAO.trouverTous();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des utilisateurs : " + e.getMessage(), e);
        }
    }

    
    public List<Utilisateur> listerUtilisateursBloques() {
        try {
            return utilisateurDAO.trouverParStatut(StatutCompte.BLOQUE);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des comptes bloqués : " + e.getMessage(), e);
        }
    }

    
    public Optional<Utilisateur> trouverParId(int id) {
        try {
            return utilisateurDAO.trouverParId(id);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de l'utilisateur : " + e.getMessage(), e);
        }
    }



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
