package com.covoiturage.servlet;

import com.covoiturage.exception.PaiementEcheException;
import com.covoiturage.exception.ReservationInvalideException;
import com.covoiturage.exception.TrajetCompletException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Paiement.MethodePaiement;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.service.ReservationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Servlet gérant les réservations de places.
 *
 * <ul>
 *   <li>POST /reservation/creer   → créer une réservation</li>
 *   <li>POST /reservation/annuler → annuler une réservation</li>
 *   <li>GET  /reservation/mes     → lister les réservations de l'utilisateur connecté</li>
 * </ul>
 */
@WebServlet(urlPatterns = {"/reservation/creer", "/reservation/annuler", "/reservation/mes"})
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

        Utilisateur passager = getUtilisateurConnecte(request);
        if (passager == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez être connecté.");
            return;
        }

        List<Reservation> reservations = reservationService.listerReservationsPassager(passager.getId());

        response.setContentType("application/json;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.print(reservationListToJson(reservations));
        out.flush();
    }

    // ── POST ──────────────────────────────────────────────────────────────────

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();

        switch (chemin) {
            case "/reservation/creer"   -> traiterCreation(request, response);
            case "/reservation/annuler" -> traiterAnnulation(request, response);
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
        return "{" +
               "\"id\":" + r.getId() + "," +
               "\"trajetId\":" + r.getTrajet().getId() + "," +
               "\"villeDepart\":\"" + echapper(r.getTrajet().getVilleDepart()) + "\"," +
               "\"villeArrivee\":\"" + echapper(r.getTrajet().getVilleArrivee()) + "\"," +
               "\"dateHeureDepart\":\"" + r.getTrajet().getDateHeureDepart() + "\"," +
               "\"nombrePlaces\":" + r.getNombrePlaces() + "," +
               "\"montantTotal\":" + r.getMontantTotal() + "," +
               "\"statut\":\"" + r.getStatut() + "\"," +
               "\"dateReservation\":\"" + r.getDateReservation() + "\"," +
               "\"montantRembourse\":" + r.getMontantRembourse() +
               "}";
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

    private boolean estNullOuVide(String v) {
        return v == null || v.isBlank();
    }
}
