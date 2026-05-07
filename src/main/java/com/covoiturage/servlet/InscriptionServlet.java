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


@WebServlet("/inscription")
public class InscriptionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private AuthService authService;

    @Override
    public void init() throws ServletException {
        this.authService = new AuthService();
    }



    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {


        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(LoginServlet.SESSION_UTILISATEUR) != null) {
            response.sendRedirect(request.getContextPath() + "/trajets");
            return;
        }

        request.getRequestDispatcher("/views/inscription.html").forward(request, response);
    }



    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {


        String nom         = request.getParameter("nom");
        String prenom      = request.getParameter("prenom");
        String email       = request.getParameter("email");
        String motDePasse  = request.getParameter("motDePasse");
        String confirmation = request.getParameter("confirmationMotDePasse");
        String telephone   = request.getParameter("telephone");
        String roleStr     = request.getParameter("role");


        if (estNullOuVide(nom) || estNullOuVide(prenom) || estNullOuVide(email) ||
            estNullOuVide(motDePasse) || estNullOuVide(roleStr)) {
            redirigerAvecErreur(request, response,
                "Tous les champs obligatoires doivent etre renseignes.");
            return;
        }


        if (!motDePasse.equals(confirmation)) {
            redirigerAvecErreur(request, response, "Les mots de passe ne correspondent pas.");
            return;
        }


        String role;
        try {
            role = roleStr.toUpperCase();
            if ("ADMIN".equals(role)) {

                redirigerAvecErreur(request, response, "Role non autorise a l'inscription.");
                return;
            }
        } catch (IllegalArgumentException e) {
            redirigerAvecErreur(request, response, "Role invalide selectionne.");
            return;
        }

        try {

            Utilisateur nouvelUtilisateur = authService.creerCompte(
                nom.trim(), prenom.trim(), email.trim().toLowerCase(),
                motDePasse, telephone, role
            );


            HttpSession session = request.getSession(true);
            session.setAttribute(LoginServlet.SESSION_UTILISATEUR, nouvelUtilisateur);
            session.setMaxInactiveInterval(30 * 60);


            request.setAttribute("succes", "Compte créé avec succès ! Bienvenue, " + prenom + " !");
            response.sendRedirect(request.getContextPath() + "/trajets");

        } catch (IllegalArgumentException e) {

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
