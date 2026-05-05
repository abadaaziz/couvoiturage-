package com.covoiturage.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.covoiturage.model.Utilisateur;
import com.covoiturage.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet gérant l'inscription de nouveaux utilisateurs.
 *
 * <ul>
 *   <li>GET  /inscription → affichage du formulaire d'inscription</li>
 *   <li>POST /inscription → traitement et création du compte</li>
 * </ul>
 */
@WebServlet("/inscription")
public class InscriptionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
    }

    // ── GET /inscription ──────────────────────────────────────────────────────

    /** Affiche le formulaire d'inscription */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // Si déjà connecté, rediriger vers les trajets
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginServlet.SESSION_UTILISATEUR) != null) {
            response.sendRedirect(request.getContextPath() + "/trajets");
            return;
        }

        request.getRequestDispatcher("/views/inscription.html").forward(request, response);
    }

    // ── POST /inscription ─────────────────────────────────────────────────────

    /**
     * Traite la soumission du formulaire d'inscription.
     * Crée le compte et connecte automatiquement le nouvel utilisateur.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // ── Récupération des paramètres ───────────────────────────────────────
        String nom         = request.getParameter("nom");
        String prenom      = request.getParameter("prenom");
        String email       = request.getParameter("email");
        String motDePasse  = request.getParameter("motDePasse");
        String confirmation = request.getParameter("confirmationMotDePasse");
        String telephone   = request.getParameter("telephone");
        String roleStr     = request.getParameter("role");

        // ── Validation côté serveur ───────────────────────────────────────────
        if (estNullOuVide(nom) || estNullOuVide(prenom) || estNullOuVide(email) ||
            estNullOuVide(motDePasse) || estNullOuVide(roleStr)) {
            redirigerAvecErreur(request, response,
                "Tous les champs obligatoires doivent etre renseignes.");
            return;
        }

        // Vérification de la confirmation du mot de passe
        if (!motDePasse.equals(confirmation)) {
            redirigerAvecErreur(request, response, "Les mots de passe ne correspondent pas.");
            return;
        }

        // Validation du rôle
        String role;
        try {
            role = roleStr.toUpperCase();
            if ("ADMIN".equals(role)) {
                // Impossible de s'inscrire directement en tant qu'ADMIN
                redirigerAvecErreur(request, response, "Role non autorise a l'inscription.");
                return;
            }
        } catch (IllegalArgumentException e) {
            redirigerAvecErreur(request, response, "Role invalide selectionne.");
            return;
        }

        try {
            // ── Création du compte ────────────────────────────────────────────
            Utilisateur nouvelUtilisateur = authService.creerCompte(
                nom.trim(), prenom.trim(), email.trim().toLowerCase(),
                motDePasse, telephone, role
            );

            // ── Connexion automatique après inscription ────────────────────────
            HttpSession session = request.getSession(true);
            session.setAttribute(LoginServlet.SESSION_UTILISATEUR, nouvelUtilisateur);
            session.setMaxInactiveInterval(30 * 60);

            // ── Redirection vers les trajets ───────────────────────────────────
            request.setAttribute("succes", "Compte créé avec succès ! Bienvenue, " + prenom + " !");
            response.sendRedirect(request.getContextPath() + "/trajets");

        } catch (IllegalArgumentException e) {
            // Erreur de validation métier (email déjà pris, mdp trop court, etc.)
            redirigerAvecErreur(request, response, e.getMessage());
        }
    }

    private void redirigerAvecErreur(HttpServletRequest request, HttpServletResponse response,
                                     String message) throws IOException {
        String msgEncode = URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/inscription?erreur=" + msgEncode);
    }

    private boolean estNullOuVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
