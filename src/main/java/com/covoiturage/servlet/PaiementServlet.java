package com.covoiturage.servlet;

import com.covoiturage.exception.PaiementEcheException;
import com.covoiturage.model.Paiement.MethodePaiement;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.service.PaiementService;
import com.covoiturage.service.ReservationService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

/**
 * Servlet gérant le traitement des paiements.
 *
 * <ul>
 *   <li>POST /paiement → traitement du paiement d'une réservation</li>
 * </ul>
 */
@WebServlet("/paiement")
public class PaiementServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private PaiementService    paiementService;
    private ReservationService reservationService;

    @Override
    public void init() throws ServletException {
        this.paiementService    = new PaiementService();
        this.reservationService = new ReservationService();
    }

    // ── POST /paiement ────────────────────────────────────────────────────────

    /**
     * Traite le paiement d'une réservation.
     * Paramètres attendus : reservationId, methodePaiement
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur utilisateur = getUtilisateurConnecte(request);
        if (utilisateur == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez être connecté pour effectuer un paiement.");
            return;
        }

        String reservationIdStr = request.getParameter("reservationId");
        String methodeStr       = request.getParameter("methodePaiement");

        if (estNullOuVide(reservationIdStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "L'identifiant de réservation est obligatoire.");
            return;
        }

        try {
            int reservationId = Integer.parseInt(reservationIdStr);

            // Récupération de la réservation
            // Note : en production, utiliser un ReservationDAO directement
            // Ici, on délègue à une méthode du service
            Optional<Reservation> opt = reservationService
                .listerReservationsPassager(utilisateur.getId())
                .stream()
                .filter(r -> r.getId() == reservationId)
                .findFirst();

            if (opt.isEmpty()) {
                envoyerErreurJson(response, HttpServletResponse.SC_NOT_FOUND,
                    "Réservation #" + reservationId + " introuvable ou non autorisée.");
                return;
            }

            Reservation reservation = opt.get();

            // Vérification que la réservation appartient bien à l'utilisateur connecté
            if (reservation.getPassager().getId() != utilisateur.getId()) {
                envoyerErreurJson(response, HttpServletResponse.SC_FORBIDDEN,
                    "Vous n'êtes pas autorisé à payer cette réservation.");
                return;
            }

            // Méthode de paiement
            MethodePaiement methode = methodeStr != null
                ? MethodePaiement.valueOf(methodeStr.toUpperCase())
                : MethodePaiement.CARTE_BANCAIRE;

            // Traitement du paiement (autorisation + capture)
            String reference = paiementService.payer(
                reservation, reservation.getMontantTotal(), methode
            );

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().print(
                "{\"succes\":true,\"reference\":\"" + reference + "\"," +
                "\"montant\":" + reservation.getMontantTotal() + "," +
                "\"message\":\"Paiement traité avec succès.\"}"
            );

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de réservation invalide.");
        } catch (IllegalArgumentException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Méthode de paiement invalide : " + methodeStr);
        } catch (PaiementEcheException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_PAYMENT_REQUIRED,
                "Paiement refusé : " + e.getMessage());
        }
    }

    // ── GET /paiement ─────────────────────────────────────────────────────────

    /** Affiche la page de paiement */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Utilisateur utilisateur = getUtilisateurConnecte(request);
        if (utilisateur == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        String reservationIdStr = request.getParameter("reservationId");
        request.setAttribute("reservationId", reservationIdStr);
        request.getRequestDispatcher("/views/paiement.html").forward(request, response);
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

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

    private boolean estNullOuVide(String v) {
        return v == null || v.isBlank();
    }
}
