package com.covoiturage.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import com.covoiturage.exception.ReservationInvalideException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.model.Utilisateur.Role;
import com.covoiturage.service.TrajetService;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Servlet gérant les opérations sur les trajets.
 *
 * <ul>
 *   <li>GET  /trajets           → liste des trajets disponibles (JSON)</li>
 *   <li>GET  /trajets/mes       → page/liste JSON des trajets du chauffeur connecté</li>
 *   <li>POST /trajets/nouveau   → proposition d'un nouveau trajet</li>
 *   <li>POST /trajets/annuler   → annulation d'un trajet par le chauffeur</li>
 * </ul>
 */
@WebServlet(urlPatterns = {"/trajets", "/trajets/mes", "/trajets/nouveau", "/trajets/annuler"})
public class TrajetServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private TrajetService trajetService;

    @Override
    public void init() throws ServletException {
        this.trajetService = new TrajetService();
    }

    // ── GET /trajets ──────────────────────────────────────────────────────────

    /**
     * Retourne la liste des trajets disponibles.
     * Supporte la recherche par paramètres de requête :
     * ?depart=Paris&arrivee=Lyon&date=2025-06-01&places=1
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();

        String accept = request.getHeader("Accept");
        boolean requeteJson = (accept != null && accept.contains("application/json")) ||
                              "json".equalsIgnoreCase(request.getParameter("format"));

        if ("/trajets/mes".equals(chemin)) {
            traiterMesTrajets(request, response, requeteJson);
            return;
        }

        // Navigation navigateur: /trajets doit afficher la page HTML.
        // Les appels AJAX de trajets.js demandent explicitement du JSON.
        if (!requeteJson) {
            request.getRequestDispatcher("/views/trajets.html").forward(request, response);
            return;
        }

        String depart  = request.getParameter("depart");
        String arrivee = request.getParameter("arrivee");
        String date    = request.getParameter("date");
        String placesP = request.getParameter("places");

        try {
            List<Trajet> trajets;

            boolean hasTextFilters = (depart != null && !depart.isBlank()) ||
                                     (arrivee != null && !arrivee.isBlank()) ||
                                     (date != null && !date.isBlank());
            boolean hasPlacesFilter = (placesP != null && !placesP.isBlank() && !"1".equals(placesP));

            if (hasTextFilters || hasPlacesFilter) {
                Integer places = null;
                if (placesP != null && !placesP.isBlank()) {
                    places = Integer.parseInt(placesP);
                }
                trajets = trajetService.rechercherTrajetsFlexible(depart, arrivee, date, places);
            } else {
                // Liste complète des trajets disponibles
                trajets = trajetService.listerTrajetsDisponibles();
            }

            // Réponse JSON
            response.setContentType("application/json;charset=UTF-8");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
            PrintWriter out = response.getWriter();
            out.print(trajetListToJson(trajets));
            out.flush();

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Paramètre 'places' invalide : doit être un entier.");
        }
    }

    private void traiterMesTrajets(HttpServletRequest request, HttpServletResponse response,
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
            request.getRequestDispatcher("/views/mes-trajets.html").forward(request, response);
            return;
        }

        List<Trajet> trajets = trajetService.listerTrajetsParChauffeur(chauffeur.getId());
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.getWriter().print(trajetListToJson(trajets));
    }

    // ── POST /trajets/nouveau ─────────────────────────────────────────────────

    /**
     * Crée un nouveau trajet proposé par un chauffeur.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String chemin = request.getServletPath();

        switch (chemin) {
            case "/trajets/nouveau" -> traiterNouveauTrajet(request, response);
            case "/trajets/annuler" -> traiterAnnulationTrajet(request, response);
            default -> envoyerErreurJson(response, HttpServletResponse.SC_NOT_FOUND,
                "Route POST non reconnue : " + chemin);
        }
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    private void traiterNouveauTrajet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Vérification de la session
        Utilisateur chauffeur = getUtilisateurConnecte(request);
        if (chauffeur == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez être connecté pour proposer un trajet.");
            return;
        }

        // Récupération des paramètres
        String villeDepart          = request.getParameter("villeDepart");
        String villeArrivee         = request.getParameter("villeArrivee");
        String dateHeureDepartStr   = request.getParameter("dateHeureDepart");
        String placesTotalStr       = request.getParameter("placesTotal");
        String prixStr              = request.getParameter("prixParPlace");
        String descriptionVehicule  = request.getParameter("descriptionVehicule");

        // Validation basique
        if (estNullOuVide(villeDepart) || estNullOuVide(villeArrivee) ||
            estNullOuVide(dateHeureDepartStr) || estNullOuVide(placesTotalStr) || estNullOuVide(prixStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Tous les champs obligatoires doivent être renseignés.");
            return;
        }

        try {
            LocalDateTime dateHeureDepart = LocalDateTime.parse(dateHeureDepartStr);
            int           placesTotal     = Integer.parseInt(placesTotalStr);
            double        prixParPlace    = Double.parseDouble(prixStr);

            Trajet trajet = trajetService.proposerTrajet(
                chauffeur, villeDepart, villeArrivee,
                dateHeureDepart, placesTotal, prixParPlace, descriptionVehicule
            );

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.getWriter().print(trajetToJson(trajet));

        } catch (DateTimeParseException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Format de date invalide. Attendu : yyyy-MM-ddTHH:mm");
        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Valeur numérique invalide pour le nombre de places ou le prix.");
        } catch (UtilisateurSuspenduException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (IllegalArgumentException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    private void traiterAnnulationTrajet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Utilisateur chauffeur = getUtilisateurConnecte(request);
        if (chauffeur == null) {
            envoyerErreurJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                "Vous devez être connecté.");
            return;
        }

        String trajetIdStr = request.getParameter("trajetId");
        if (estNullOuVide(trajetIdStr)) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "L'identifiant du trajet est obligatoire.");
            return;
        }

        try {
            int trajetId  = Integer.parseInt(trajetIdStr);
            double penalite = trajetService.annulerTrajetChauffeur(trajetId, chauffeur.getId());

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().print("{\"succes\":true,\"penalite\":" + penalite +
                ",\"message\":\"Trajet annulé. Pénalité appliquée : " +
                String.format("%.2f", penalite) + "€\"}");

        } catch (NumberFormatException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST,
                "Identifiant de trajet invalide.");
        } catch (ReservationInvalideException | IllegalArgumentException | IllegalStateException e) {
            envoyerErreurJson(response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        }
    }

    // ── Sérialisation JSON manuelle (sans bibliothèque externe) ───────────────

    private String trajetListToJson(List<Trajet> trajets) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < trajets.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(trajetToJson(trajets.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    private String trajetToJson(Trajet t) {
        return "{" +
               "\"id\":" + t.getId() + "," +
               "\"villeDepart\":\"" + echapper(t.getVilleDepart()) + "\"," +
               "\"villeArrivee\":\"" + echapper(t.getVilleArrivee()) + "\"," +
               "\"dateHeureDepart\":\"" + t.getDateHeureDepart() + "\"," +
               "\"placesTotal\":" + t.getPlacesTotal() + "," +
               "\"placesDisponibles\":" + t.getPlacesDisponibles() + "," +
               "\"prixParPlace\":" + String.format(Locale.US, "%.2f", t.getPrixParPlace()) + "," +
               "\"statut\":\"" + t.getStatut() + "\"," +
               "\"descriptionVehicule\":\"" + echapper(t.getDescriptionVehicule()) + "\"," +
               "\"chauffeur\":{" +
                   "\"id\":" + t.getChauffeur().getId() + "," +
                   "\"nom\":\"" + echapper(t.getChauffeur().getNom()) + "\"," +
                   "\"prenom\":\"" + echapper(t.getChauffeur().getPrenom()) + "\"," +
                   "\"email\":\"" + echapper(t.getChauffeur().getEmail()) + "\"," +
                   "\"telephone\":\"" + echapper(t.getChauffeur().getTelephone()) + "\"," +
                   "\"note\":" + String.format(Locale.US, "%.2f", t.getChauffeur().getNoteMoyenne()) +
               "}" +
               "}";
    }

    /** Échappe les caractères spéciaux JSON */
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
