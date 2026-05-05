package com.covoiturage.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

import com.covoiturage.exception.PaiementEcheException;
import com.covoiturage.exception.ReservationInvalideException;
import com.covoiturage.exception.TrajetCompletException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Paiement.MethodePaiement;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.model.Utilisateur.Role;
import com.covoiturage.service.ReservationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet gérant les réservations de places.
 *
 * <ul>
 *   <li>POST /reservation/creer   → créer une réservation</li>
 *   <li>POST /reservation/annuler → annuler une réservation</li>
 *   <li>GET  /reservation/mes       → lister les réservations du passager connecté</li>
 *   <li>GET  /reservation/chauffeur → lister les réservations liées aux trajets du chauffeur connecté</li>
 *   <li>GET  /reservation/eligibles → lister les reservations eligibles a la notation</li>
 *   <li>POST /reservation/confirmer → confirmer une réservation (chauffeur)</li>
 *   <li>POST /reservation/supprimer → supprimer une réservation passée (passager)</li>
 *   <li>POST /reservation/noter → noter le chauffeur (passager)</li>
 * </ul>
 */
@WebServlet(urlPatterns = {
    "/reservation/creer", "/reservation/annuler", "/reservation/mes",
    "/reservation/chauffeur", "/reservation/confirmer", "/reservation/supprimer",
    "/reservation/eligibles",
    "/reservation/noter"
})
public class ReservationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private ReservationService reservationService;

    @Override
    public void init() throws ServletException {
        this.reservationService = new ReservationService();
    }

    // ── GET ───────────────────────────────────────────────────────────────────

    /** Retourne les réservations de l'utilisateur connecté */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String accept = request.getHeader("Accept");
        boolean requeteJson = (accept != null && accept.contains("application/json")) ||
                              "json".equalsIgnoreCase(request.getParameter("format"));

        String chemin = request.getServletPath();

        if ("/reservation/chauffeur".equals(chemin)) {
            traiterListeChauffeur(request, response, requeteJson);
            return;
        }
        if ("/reservation/eligibles".equals(chemin)) {
            traiterEligiblesNotation(request, response, requeteJson);
            return;
        }

        Utilisateur passager = getUtilisateurConnecte(request);
        if (passager == null) {
            if (requeteJson) {
                envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Vous devez etre connecte.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        if (!requeteJson) {
            request.getRequestDispatcher("/views/reservations.html").forward(request, response);
            return;
        }

        try {
            List<Reservation> reservations = reservationService.listerReservationsPassager(passager.getId());
            getServletContext().log("[DEBUG] Retrieved " + reservations.size() + " reservations for user " + passager.getId());
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.print(reservationListToJson(reservations));
            out.flush();
        } catch (Exception e) {
            getServletContext().log("Erreur chargement reservations passager " + passager.getId(), e);
            envoyerErreurJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Erreur serveur: " + e.getMessage());
        }
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();

        switch (chemin) {
            case "/reservation/creer"   -> traiterCreation(request, response);
            case "/reservation/annuler" -> traiterAnnulation(request, response);
            case "/reservation/confirmer" -> traiterConfirmation(request, response);
            case "/reservation/supprimer" -> traiterSuppression(request, response);
            case "/reservation/noter" -> traiterNotation(request, response);
            default -> envoyerErreurJson(response, HttpServletResponse.SC_NOT_FOUND,
                "Route non reconnue.");
        }
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    /**
     * Crée une nouvelle réservation pour l'utilisateur connecté.
     * Paramètres attendus : trajetId, nombrePlaces, methodePaiement
     */
    private void traiterCreation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Utilisateur passager = getUtilisateurConnecte(request);
        if (passager == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez être connecté pour effectuer une réservation.");
            return;
        }

        String trajetIdStr     = request.getParameter("trajetId");
        String nombrePlacesStr = request.getParameter("nombrePlaces");
        String methodeStr      = request.getParameter("methodePaiement");

        if (estNullOuVide(trajetIdStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "L'identifiant du trajet est obligatoire.");
            return;
        }

        try {
            int trajetId     = Integer.parseInt(trajetIdStr);
            int nombrePlaces = nombrePlacesStr != null ? Integer.parseInt(nombrePlacesStr) : 1;
            MethodePaiement methode = methodeStr != null
                ? MethodePaiement.valueOf(methodeStr.toUpperCase())
                : MethodePaiement.CARTE_BANCAIRE;

            Reservation reservation = reservationService.creerReservation(
                passager, trajetId, nombrePlaces, methode
            );

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().print(reservationToJson(reservation));

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Paramètres numériques invalides.");
        } catch (IllegalArgumentException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Méthode de paiement invalide : " + methodeStr);
        } catch (TrajetCompletException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_CONFLICT,
                "Le trajet est complet, aucune place disponible.");
        } catch (ReservationInvalideException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (PaiementEcheException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_PAYMENT_REQUIRED,
                "Paiement refusé : " + e.getMessage());
        } catch (UtilisateurSuspenduException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        }
    }

    /**
     * Annule une réservation existante.
     * Paramètre attendu : reservationId
     */
    private void traiterAnnulation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Utilisateur passager = getUtilisateurConnecte(request);
        if (passager == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez être connecté.");
            return;
        }

        String reservationIdStr = request.getParameter("reservationId");
        if (estNullOuVide(reservationIdStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "L'identifiant de la réservation est obligatoire.");
            return;
        }

        try {
            int    reservationId  = Integer.parseInt(reservationIdStr);
            double montantRembourse = reservationService.annulerReservation(reservationId, passager.getId());

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"succes\":true,\"montantRembourse\":" + montantRembourse +
                ",\"message\":\"Réservation annulée. Remboursement de " +
                String.format("%.2f", montantRembourse) + "€ en cours.\"}");

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de réservation invalide.");
        } catch (ReservationInvalideException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (PaiementEcheException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "Erreur lors du remboursement : " + e.getMessage());
        }
    }

    private void traiterConfirmation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Utilisateur chauffeur = getChauffeurConnecte(request);
        if (chauffeur == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_FORBIDDEN,
                "Action reservee aux chauffeurs.");
            return;
        }

        String reservationIdStr = request.getParameter("reservationId");
        if (estNullOuVide(reservationIdStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "L'identifiant de la reservation est obligatoire.");
            return;
        }

        try {
            int reservationId = Integer.parseInt(reservationIdStr);
            reservationService.confirmerReservation(reservationId, chauffeur.getId());

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"succes\":true,\"message\":\"Reservation confirmee.\"}");

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de reservation invalide.");
        } catch (ReservationInvalideException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (PaiementEcheException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_PAYMENT_REQUIRED,
                "Capture du paiement echouee : " + e.getMessage());
        }
    }

    /**
     * Supprime une reservation si le trajet est passe.
     * Parametre attendu : reservationId
     */
    private void traiterSuppression(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Utilisateur passager = getUtilisateurConnecte(request);
        if (passager == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez etre connecte.");
            return;
        }

        String reservationIdStr = request.getParameter("reservationId");
        if (estNullOuVide(reservationIdStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "L'identifiant de la reservation est obligatoire.");
            return;
        }

        try {
            int reservationId = Integer.parseInt(reservationIdStr);
            reservationService.supprimerReservationSiTerminee(reservationId, passager.getId());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"succes\":true,\"message\":\"Reservation supprimee.\"}");
        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de reservation invalide.");
        } catch (ReservationInvalideException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * Liste des reservations eligibles a la notation pour un chauffeur.
     */
    private void traiterEligiblesNotation(HttpServletRequest request, HttpServletResponse response,
                                          boolean requeteJson) throws IOException {
        Utilisateur passager = getUtilisateurConnecte(request);
        if (passager == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez etre connecte.");
            return;
        }

        String chauffeurIdStr = request.getParameter("chauffeurId");
        if (estNullOuVide(chauffeurIdStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "chauffeurId obligatoire.");
            return;
        }

        try {
            int chauffeurId = Integer.parseInt(chauffeurIdStr);
            List<Reservation> reservations = reservationService.listerEligiblesNotation(passager.getId(), chauffeurId);

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print(reservationEligiblesToJson(reservations));

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant chauffeur invalide.");
        }
    }

    /**
     * Notation du chauffeur par le passager.
     * Parametres attendus : reservationId, note
     */
    private void traiterNotation(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Utilisateur passager = getUtilisateurConnecte(request);
        if (passager == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez etre connecte.");
            return;
        }

        String reservationIdStr = request.getParameter("reservationId");
        String noteStr = request.getParameter("note");

        if (estNullOuVide(reservationIdStr) || estNullOuVide(noteStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Reservation et note obligatoires.");
            return;
        }

        try {
            int reservationId = Integer.parseInt(reservationIdStr);
            int note = Integer.parseInt(noteStr);

            reservationService.noterChauffeur(reservationId, passager.getId(), note);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"succes\":true,\"message\":\"Merci pour votre note.\"}");

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Parametres invalides.");
        } catch (ReservationInvalideException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void traiterListeChauffeur(HttpServletRequest request, HttpServletResponse response,
                                       boolean requeteJson) throws IOException, ServletException {
        Utilisateur chauffeur = getChauffeurConnecte(request);
        if (chauffeur == null) {
            if (requeteJson) {
                envoyerErreurJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "Action reservee aux chauffeurs.");
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        if (!requeteJson) {
            response.sendRedirect(request.getContextPath() + "/trajets/mes");
            return;
        }

        List<Reservation> reservations = reservationService.listerReservationsChauffeur(chauffeur.getId());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().print(reservationListToJson(reservations));
    }

    // ── Sérialisation JSON manuelle ───────────────────────────────────────────

    private String reservationListToJson(List<Reservation> reservations) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < reservations.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(reservationToJson(reservations.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String reservationToJson(Reservation r) {
        String passagerNom = "";
        String passagerPrenom = "";
        String passagerEmail = "";
        String chauffeurNom = "";
        String chauffeurPrenom = "";
        String chauffeurEmail = "";
        String chauffeurTelephone = "";
        double chauffeurNote = 0.0;
        if (r.getPassager() != null) {
            passagerNom = r.getPassager().getNom();
            passagerPrenom = r.getPassager().getPrenom();
            passagerEmail = r.getPassager().getEmail();
        }
        if (r.getTrajet() != null && r.getTrajet().getChauffeur() != null) {
            chauffeurNom = r.getTrajet().getChauffeur().getNom();
            chauffeurPrenom = r.getTrajet().getChauffeur().getPrenom();
            chauffeurEmail = r.getTrajet().getChauffeur().getEmail();
            chauffeurTelephone = r.getTrajet().getChauffeur().getTelephone();
            chauffeurNote = r.getTrajet().getChauffeur().getNoteMoyenne();
        }

        return "{" +
               "\"id\":" + r.getId() + "," +
               "\"trajetId\":" + r.getTrajet().getId() + "," +
               "\"villeDepart\":\"" + echapper(r.getTrajet().getVilleDepart()) + "\"," +
               "\"villeArrivee\":\"" + echapper(r.getTrajet().getVilleArrivee()) + "\"," +
               "\"dateHeureDepart\":\"" + r.getTrajet().getDateHeureDepart() + "\"," +
               "\"chauffeur\":{" +
                   "\"id\":" + (r.getTrajet().getChauffeur() != null ? r.getTrajet().getChauffeur().getId() : 0) + "," +
                   "\"nom\":\"" + echapper(chauffeurNom) + "\"," +
                   "\"prenom\":\"" + echapper(chauffeurPrenom) + "\"," +
                   "\"email\":\"" + echapper(chauffeurEmail) + "\"," +
                   "\"telephone\":\"" + echapper(chauffeurTelephone) + "\"," +
                   "\"note\":" + String.format(Locale.US, "%.2f", chauffeurNote) +
               "}," +
               "\"passagerNom\":\"" + echapper(passagerNom) + "\"," +
               "\"passagerPrenom\":\"" + echapper(passagerPrenom) + "\"," +
               "\"passagerEmail\":\"" + echapper(passagerEmail) + "\"," +
               "\"nombrePlaces\":" + r.getNombrePlaces() + "," +
               "\"montantTotal\":" + String.format(Locale.US, "%.2f", r.getMontantTotal()) + "," +
               "\"statut\":\"" + r.getStatut() + "\"," +
               "\"dateReservation\":\"" + r.getDateReservation() + "\"," +
               "\"montantRembourse\":" + String.format(Locale.US, "%.2f", r.getMontantRembourse()) + "," +
               "\"notePassager\":" + (r.getNotePassager() == null ? "null" : r.getNotePassager()) +
               "}";
    }

    private String reservationEligiblesToJson(List<Reservation> reservations) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < reservations.size(); i++) {
            Reservation r = reservations.get(i);
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"id\":").append(r.getId()).append(",")
              .append("\"villeDepart\":\"").append(echapper(r.getTrajet().getVilleDepart())).append("\",")
              .append("\"villeArrivee\":\"").append(echapper(r.getTrajet().getVilleArrivee())).append("\",")
              .append("\"dateHeureDepart\":\"").append(r.getTrajet().getDateHeureDepart()).append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String echapper(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
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

    private Utilisateur getChauffeurConnecte(HttpServletRequest request) {
        Utilisateur u = getUtilisateurConnecte(request);
        if (u == null) return null;
        if (u.getRole() != Role.CHAUFFEUR && u.getRole() != Role.ADMIN) return null;
        return u;
    }

    private boolean estNullOuVide(String v) {
        return v == null || v.isBlank();
    }
}
