package com.covoiturage.servlet;

import java.io.IOException;

import com.covoiturage.model.Utilisateur;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Expose un endpoint JSON minimal pour connaitre l'utilisateur connecté.
 */
@WebServlet("/session/me")
public class SessionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        Utilisateur u = null;
        if (session != null) {
            u = (Utilisateur) session.getAttribute(LoginServlet.SESSION_UTILISATEUR);
        }

        response.setContentType("application/json;charset=UTF-8");

        if (u == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().print("{\"connecte\":false}");
            return;
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().print("{" +
            "\"connecte\":true," +
            "\"id\":" + u.getId() + "," +
            "\"nom\":\"" + echapper(u.getNom()) + "\"," +
            "\"prenom\":\"" + echapper(u.getPrenom()) + "\"," +
            "\"role\":\"" + u.getRole() + "\"" +
            "}");
    }

    private String echapper(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}