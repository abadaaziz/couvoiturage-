package com.covoiturage.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.covoiturage.model.Utilisateur;
import com.covoiturage.model.Utilisateur.Role;
import com.covoiturage.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet d'administration (réservée aux utilisateurs avec le rôle ADMIN).
 *
 * <ul>
 *   <li>GET  /admin/users         → liste de tous les utilisateurs (JSON)</li>
 *   <li>POST /admin/suspendre     → suspension d'un compte</li>
 *   <li>POST /admin/bloquer       → blocage définitif d'un compte</li>
 * </ul>
 */
@WebServlet(urlPatterns = {"/admin/users", "/admin/suspendre", "/admin/bloquer"})
public class AdminServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
    }

    // ── GET /admin/users ──────────────────────────────────────────────────────

    /** Retourne la liste de tous les utilisateurs en JSON */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accept = request.getHeader("Accept");
        boolean requeteJson = (accept != null && accept.contains("application/json")) ||
                              "json".equalsIgnoreCase(request.getParameter("format"));

        // Vérification des droits admin
        Utilisateur admin = getAdmin(request);
        if (admin == null) {
            if (requeteJson) {
                envoyerErreurJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "Acces reserve aux administrateurs.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        if (!requeteJson) {
            request.getRequestDispatcher("/views/admin.html").forward(request, response);
            return;
        }

        List<Utilisateur> utilisateurs = authService.listerTousLesUtilisateurs();

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(utilisateurListToJson(utilisateurs));
        out.flush();
    }

    // ── POST /admin/suspendre & /admin/bloquer ────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur admin = getAdmin(request);
        if (admin == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_FORBIDDEN,
                "Accès réservé aux administrateurs.");
            return;
        }

        String chemin           = request.getServletPath();
        String utilisateurIdStr = request.getParameter("utilisateurId");

        if (estNullOuVide(utilisateurIdStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "L'identifiant utilisateur est obligatoire.");
            return;
        }

        try {
            int utilisateurId = Integer.parseInt(utilisateurIdStr);

            switch (chemin) {
                case "/admin/suspendre" -> {
                    authService.suspendreCompte(utilisateurId, admin.getId());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().print(
                        "{\"succes\":true,\"message\":\"Compte #" + utilisateurId + " suspendu.\"}"
                    );
                }
                case "/admin/bloquer" -> {
                    authService.bloquerUtilisateur(utilisateurId, admin.getId());
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().print(
                        "{\"succes\":true,\"message\":\"Compte #" + utilisateurId + " bloqué définitivement.\"}"
                    );
                }
                default -> envoyerErreurJson(response, HttpServletResponse.SC_NOT_FOUND,
                    "Route admin non reconnue : " + chemin);
            }

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant utilisateur invalide.");
        } catch (IllegalArgumentException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    // ── Sérialisation JSON manuelle ───────────────────────────────────────────

    private String utilisateurListToJson(List<Utilisateur> utilisateurs) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < utilisateurs.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(utilisateurToJson(utilisateurs.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String utilisateurToJson(Utilisateur u) {
        return "{" +
               "\"id\":" + u.getId() + "," +
               "\"nom\":\"" + echapper(u.getNom()) + "\"," +
               "\"prenom\":\"" + echapper(u.getPrenom()) + "\"," +
               "\"email\":\"" + echapper(u.getEmail()) + "\"," +
               "\"telephone\":\"" + echapper(u.getTelephone()) + "\"," +
               "\"role\":\"" + u.getRole() + "\"," +
               "\"statutCompte\":\"" + u.getStatutCompte() + "\"," +
               "\"noteMoyenne\":" + String.format("%.2f", u.getNoteMoyenne()) + "," +
               "\"nombreAvis\":" + u.getNombreAvis() + "," +
               "\"dateInscription\":\"" + u.getDateInscription() + "\"" +
               "}";
    }

    /**
     * Vérifie si l'utilisateur connecté est un ADMIN.
     * @return L'utilisateur ADMIN ou null si non autorisé
     */
    private Utilisateur getAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Utilisateur u = (Utilisateur) session.getAttribute(LoginServlet.SESSION_UTILISATEUR);
        if (u == null || u.getRole() != Role.ADMIN) return null;
        return u;
    }

    private void envoyerErreurJson(HttpServletResponse response, int statut, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statut);
        response.getWriter().print("{\"erreur\":\"" + echapper(message) + "\"}");
    }

    private String echapper(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private boolean estNullOuVide(String v) {
        return v == null || v.isBlank();
    }
}
