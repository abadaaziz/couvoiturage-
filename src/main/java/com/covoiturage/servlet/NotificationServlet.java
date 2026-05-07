package com.covoiturage.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.covoiturage.model.Notification;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.service.InAppNotificationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@WebServlet(urlPatterns = {
    "/notifications", "/notifications/mes", "/notifications/lu", "/notifications/lu-tout",
    "/notifications/supprimer"
})
public class NotificationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private InAppNotificationService notificationService;

    @Override
    public void init() throws ServletException {
        this.notificationService = new InAppNotificationService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();
        boolean requeteJson = isJsonRequest(request);

        Utilisateur utilisateur = getUtilisateurConnecte(request);
        if (utilisateur == null) {
            if (requeteJson) {
                envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Vous devez etre connecte.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        if ("/notifications".equals(chemin)) {
            request.getRequestDispatcher("/views/notifications.html").forward(request, response);
            return;
        }

        if ("/notifications/mes".equals(chemin)) {
            List<Notification> notifications =
                notificationService.listerPourUtilisateur(utilisateur.getId());
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(notificationListToJson(notifications));
            out.flush();
            return;
        }

        envoyerErreurJson(response, HttpServletResponse.SC_NOT_FOUND, "Route non reconnue.");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();
        Utilisateur utilisateur = getUtilisateurConnecte(request);
        if (utilisateur == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez etre connecte.");
            return;
        }

        switch (chemin) {
            case "/notifications/lu" -> traiterMarquerLu(request, response, utilisateur.getId());
            case "/notifications/lu-tout" -> traiterMarquerTousLus(response, utilisateur.getId());
            case "/notifications/supprimer" -> traiterSuppression(request, response, utilisateur.getId());
            default -> envoyerErreurJson(response, HttpServletResponse.SC_NOT_FOUND,
                "Route non reconnue.");
        }
    }

    private void traiterMarquerLu(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isBlank()) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de notification manquant.");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            notificationService.marquerCommeLu(id, userId);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"succes\":true}");
        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de notification invalide.");
        }
    }

    private void traiterMarquerTousLus(HttpServletResponse response, int userId)
            throws IOException {
        notificationService.marquerTousLus(userId);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print("{\"succes\":true}");
    }

    private void traiterSuppression(HttpServletRequest request, HttpServletResponse response, int userId)
            throws IOException {
        String idStr = request.getParameter("id");
        if (idStr == null || idStr.isBlank()) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de notification manquant.");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            boolean deleted = notificationService.supprimer(id, userId);
            if (!deleted) {
                envoyerErreurJson(response, HttpServletResponse.SC_NOT_FOUND,
                    "Notification introuvable.");
                return;
            }
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"succes\":true}");
        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de notification invalide.");
        }
    }

    private String notificationListToJson(List<Notification> notifications) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < notifications.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(notificationToJson(notifications.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String notificationToJson(Notification n) {
        String dateLecture = n.getDateLecture() != null ? n.getDateLecture().toString() : "";
        return "{" +
               "\"id\":" + n.getId() + "," +
               "\"type\":\"" + echapper(n.getType()) + "\"," +
               "\"titre\":\"" + echapper(n.getTitre()) + "\"," +
               "\"message\":\"" + echapper(n.getMessage()) + "\"," +
               "\"dateCreation\":\"" + n.getDateCreation() + "\"," +
               "\"dateLecture\":\"" + dateLecture + "\"" +
               "}";
    }

    private String echapper(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return (accept != null && accept.contains("application/json")) ||
               "json".equalsIgnoreCase(request.getParameter("format"));
    }

    private void envoyerErreurJson(HttpServletResponse response, int statut, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statut);
        response.getWriter().print("{\"erreur\":\"" + echapper(message) + "\"}");
    }

    private Utilisateur getUtilisateurConnecte(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (Utilisateur) session.getAttribute(LoginServlet.SESSION_UTILISATEUR);
    }
}
