package com.covoiturage.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.covoiturage.exception.AuthenticationException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Admin;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.service.AuthService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@WebServlet(urlPatterns = {"/login", "/logout"})
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    
    public static final String SESSION_UTILISATEUR = "utilisateurConnecte";

    private AuthService authService;

    @Override
    public void init() throws ServletException {

        this.authService = new AuthService();
    }



    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();

        if ("/logout".equals(chemin)) {

            traiterDeconnexion(request, response);
        } else {

            traiterConnexion(request, response);
        }
    }



    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();

        if ("/logout".equals(chemin)) {

            traiterDeconnexion(request, response);
        } else {

            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute(SESSION_UTILISATEUR) != null) {
                response.sendRedirect(request.getContextPath() + "/trajets");
                return;
            }

            request.getRequestDispatcher("/views/login.html").forward(request, response);
        }
    }



        private void traiterConnexion(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String email      = request.getParameter("email");
        String motDePasse = request.getParameter("motDePasse");


        if (estNullOuVide(email) || estNullOuVide(motDePasse)) {
            redirigerAvecMessage(request, response, "erreur",
                "Veuillez renseigner votre email et votre mot de passe.");
            return;
        }

        try {

            Utilisateur utilisateur = authService.authentifier(email.trim(), motDePasse);


            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_UTILISATEUR, utilisateur);
            session.setMaxInactiveInterval(30 * 60);


            if (utilisateur instanceof Admin) {
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

            Utilisateur utilisateur = (Utilisateur) session.getAttribute(SESSION_UTILISATEUR);
            if (utilisateur != null) {
                authService.deconnecter(utilisateur.getId());
            }
            session.invalidate();
        }


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
