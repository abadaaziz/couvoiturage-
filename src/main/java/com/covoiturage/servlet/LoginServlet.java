package com.covoiturage.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.covoiturage.exception.AuthenticationException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet gérant la connexion et la déconnexion des utilisateurs.
 *
 * <ul>
 *   <li>POST /login  → authentification</li>
 *   <li>POST /logout → déconnexion</li>
 * </ul>
 */
@WebServlet(urlPatterns = {"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Attribut de session contenant l'utilisateur connecté */
    public static final String SESSION_UTILISATEUR = "utilisateurConnecte";

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        // Initialisation du service (peut être injecté via un ServletContext listener)
        this.authService = new AuthService();
    }

    // ── POST /login ───────────────────────────────────────────────────────────

    /**
     * Traite la soumission du formulaire de connexion.
     * En cas de succès, crée une session et redirige vers le tableau de bord.
     * En cas d'échec, redirige vers le formulaire avec un message d'erreur.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();

        if ("/logout".equals(chemin)) {
            // ── Déconnexion ───────────────────────────────────────────────────
            traiterDeconnexion(request, response);
        } else {
            // ── Connexion ─────────────────────────────────────────────────────
            traiterConnexion(request, response);
        }
    }

    // ── GET /login ────────────────────────────────────────────────────────────

    /**
     * Affiche la page de connexion (vue HTML).
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();

        if ("/logout".equals(chemin)) {
            // GET /logout → déconnexion directe puis redirection
            traiterDeconnexion(request, response);
        } else {
            // Vérification si l'utilisateur est déjà connecté
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute(SESSION_UTILISATEUR) != null) {
                response.sendRedirect(request.getContextPath() + "/trajets");
                return;
            }
            // Affichage de la vue login
            request.getRequestDispatcher("/views/login.html").forward(request, response);
        }
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

        private void traiterConnexion(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String email      = request.getParameter("email");
        String motDePasse = request.getParameter("motDePasse");

        // ── Validation basique des paramètres ─────────────────────────────────
        if (estNullOuVide(email) || estNullOuVide(motDePasse)) {
            redirigerAvecMessage(request, response, "erreur",
                "Veuillez renseigner votre email et votre mot de passe.");
            return;
        }

        try {
            // ── Authentification ──────────────────────────────────────────────
            Utilisateur utilisateur = authService.authentifier(email.trim(), motDePasse);

            // ── Création de la session ────────────────────────────────────────
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_UTILISATEUR, utilisateur);
            session.setMaxInactiveInterval(30 * 60); // 30 minutes

            // ── Redirection selon le rôle ─────────────────────────────────────
            if (utilisateur.getRole() == Utilisateur.Role.ADMIN) {
                response.sendRedirect(request.getContextPath() + "/admin/users");
            } else {
                response.sendRedirect(request.getContextPath() + "/trajets");
            }

        } catch (AuthenticationException e) {
            redirigerAvecMessage(request, response, "erreur", e.getMessage());

        } catch (UtilisateurSuspenduException e) {
            redirigerAvecMessage(request, response, "erreur",
                "Votre compte est suspendu. " + e.getMessage());
        } catch (RuntimeException e) {
            redirigerAvecMessage(request, response, "erreur",
                "Service temporairement indisponible. Relancez l'application puis reessayez.");
        }
    }

    private void traiterDeconnexion(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            // Récupération de l'id avant invalidation (pour le log)
            Utilisateur utilisateur = (Utilisateur) session.getAttribute(SESSION_UTILISATEUR);
            if (utilisateur != null) {
                authService.deconnecter(utilisateur.getId());
            }
            session.invalidate();
        }

        // Redirection vers la page de connexion
        redirigerAvecMessage(request, response, "succes", "Vous avez ete deconnecte.");
    }

    private void redirigerAvecMessage(HttpServletRequest request, HttpServletResponse response,
                                      String type, String message) throws IOException {
        String msgEncode = URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/login?" + type + "=" + msgEncode);
    }

    private boolean estNullOuVide(String valeur) {
        return valeur == null || valeur.isBlank();
    }
}
