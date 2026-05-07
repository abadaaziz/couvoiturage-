package com.covoiturage.servlet;

import java.io.IOException;
import java.sql.SQLException;

import com.covoiturage.dao.AppRatingDAO;
import com.covoiturage.model.Utilisateur;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@WebServlet(urlPatterns = {"/app/notation"})
public class AppRatingServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private AppRatingDAO appRatingDAO;

    @Override
    public void init() throws ServletException {
        this.appRatingDAO = new AppRatingDAO();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur utilisateur = getUtilisateurConnecte(request);
        if (utilisateur == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez etre connecte.");
            return;
        }

        String noteStr = request.getParameter("note");
        if (noteStr == null || noteStr.isBlank()) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "La note est obligatoire.");
            return;
        }

        try {
            int note = Integer.parseInt(noteStr);
            if (note < 1 || note > 5) {
                envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    "La note doit etre comprise entre 1 et 5.");
                return;
            }

            appRatingDAO.enregistrerNote(utilisateur.getId(), note);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"succes\":true,\"message\":\"Merci pour votre note.\"}");

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Note invalide.");
        } catch (SQLException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Erreur serveur : " + e.getMessage());
        }
    }

    private void envoyerErreurJson(HttpServletResponse response, int statut, String message)
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statut);
        String msg = message != null ? message.replace("\"", "'") : "";
        response.getWriter().print("{\"erreur\":\"" + msg + "\"}");
    }

    private Utilisateur getUtilisateurConnecte(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (Utilisateur) session.getAttribute(LoginServlet.SESSION_UTILISATEUR);
    }
}