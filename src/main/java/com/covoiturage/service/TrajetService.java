package com.covoiturage.service;

import com.covoiturage.dao.TrajetDAO;
import com.covoiturage.exception.ReservationInvalideException;
import com.covoiturage.exception.TrajetCompletException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.Trajet.StatutTrajet;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.model.Utilisateur.Role;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des trajets de covoiturage.
 * <p>
 * Responsabilité unique (SRP) : création, annulation et gestion des places des trajets.
 * </p>
 */
public class TrajetService {

    private final TrajetDAO    trajetDAO;
    private final NotificationService notificationService;

    public TrajetService() {
        this.trajetDAO           = new TrajetDAO();
        this.notificationService = new NotificationService();
    }

    public TrajetService(TrajetDAO trajetDAO, NotificationService notificationService) {
        this.trajetDAO           = trajetDAO;
        this.notificationService = notificationService;
    }

    // ── Méthodes publiques ────────────────────────────────────────────────────

    /**
     * Propose un nouveau trajet de covoiturage.
     *
     * @param chauffeur          Utilisateur proposant le trajet (doit avoir le rôle CHAUFFEUR)
     * @param villeDepart        Ville de départ
     * @param villeArrivee       Ville d'arrivée
     * @param dateHeureDepart    Date et heure du départ (doit être future)
     * @param placesTotal        Nombre de places offertes (min 1)
     * @param prixParPlace       Prix par place en euros
     * @param descriptionVehicule Description du véhicule
     * @return Trajet créé et persisté
     * @throws UtilisateurSuspenduException si le compte est suspendu
     * @throws IllegalArgumentException si les données sont invalides
     */
    public Trajet proposerTrajet(Utilisateur chauffeur, String villeDepart, String villeArrivee,
                                  LocalDateTime dateHeureDepart, int placesTotal,
                                  double prixParPlace, String descriptionVehicule)
            throws UtilisateurSuspenduException {

        // ── Validations ──────────────────────────────────────────────────────
        if (!chauffeur.estActif()) {
            throw new UtilisateurSuspenduException(chauffeur.getEmail());
        }
        if (chauffeur.getRole() != Role.CHAUFFEUR && chauffeur.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException(
                "Seul un chauffeur peut proposer un trajet. Rôle actuel : " + chauffeur.getRole());
        }
        if (dateHeureDepart.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new IllegalArgumentException(
                "Le départ doit être prévu au moins 30 minutes dans le futur.");
        }
        if (placesTotal < 1 || placesTotal > 8) {
            throw new IllegalArgumentException(
                "Le nombre de places doit être compris entre 1 et 8.");
        }
        if (prixParPlace < 0 || prixParPlace > 500) {
            throw new IllegalArgumentException(
                "Le prix par place doit être compris entre 0 et 500 euros.");
        }

        try {
            Trajet trajet = new Trajet(villeDepart, villeArrivee, dateHeureDepart,
                                       placesTotal, prixParPlace, chauffeur, descriptionVehicule);
            Trajet trajetPersiste = trajetDAO.inserer(trajet);

            // Notification de confirmation au chauffeur
            notificationService.notifierEmail(
                chauffeur.getEmail(),
                "Trajet proposé avec succès",
                "Votre trajet " + villeDepart + " → " + villeArrivee +
                " du " + dateHeureDepart + " a bien été enregistré (réf. #" + trajetPersiste.getId() + ")."
            );

            return trajetPersiste;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du trajet : " + e.getMessage(), e);
        }
    }

    /**
     * Clôture (termine) un trajet après son déroulement effectif.
     *
     * @param trajetId  Identifiant du trajet à clore
     * @param chauffeurId Identifiant du chauffeur demandant la clôture
     */
    public void cloreTrajet(int trajetId, int chauffeurId) {
        try {
            Optional<Trajet> opt = trajetDAO.trouverParId(trajetId);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Trajet #" + trajetId + " introuvable.");
            }
            Trajet trajet = opt.get();

            if (trajet.getChauffeur().getId() != chauffeurId) {
                throw new IllegalArgumentException(
                    "Seul le chauffeur du trajet peut le clore.");
            }
            if (trajet.getStatut() == StatutTrajet.ANNULE) {
                throw new IllegalStateException("Impossible de clore un trajet annulé.");
            }

            trajetDAO.mettreAJourStatut(trajetId, StatutTrajet.TERMINE);
            System.out.println("[TrajetService] Trajet #" + trajetId + " clôturé.");

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la clôture du trajet : " + e.getMessage(), e);
        }
    }

    /**
     * Ajoute un passager (réserve une place) sur un trajet.
     * <strong>Note :</strong> La réservation formelle est gérée par {@link ReservationService}.
     * Cette méthode met à jour les places disponibles et le statut du trajet.
     *
     * @param trajetId Identifiant du trajet
     * @throws TrajetCompletException si plus aucune place n'est disponible
     */
    public Trajet ajouterPassager(int trajetId)
            throws TrajetCompletException, ReservationInvalideException {
        try {
            Optional<Trajet> opt = trajetDAO.trouverParId(trajetId);
            if (opt.isEmpty()) {
                throw new ReservationInvalideException("Trajet #" + trajetId + " introuvable.");
            }
            Trajet trajet = opt.get();

            if (trajet.getStatut() == StatutTrajet.COMPLET) {
                throw new TrajetCompletException(trajetId);
            }
            if (trajet.getStatut() != StatutTrajet.OUVERT) {
                throw new ReservationInvalideException(
                    "Le trajet #" + trajetId + " n'est pas ouvert aux réservations (statut : " +
                    trajet.getStatut() + ").");
            }

            // Mise à jour en mémoire
            trajet.reserverPlace();

            // Persistance atomique (places + statut en une requête)
            trajetDAO.mettreAJourPlaces(trajetId, trajet.getPlacesDisponibles(), trajet.getStatut());

            return trajet;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du passager : " + e.getMessage(), e);
        }
    }

    /**
     * Retire un passager (libère une place) sur un trajet, suite à une annulation.
     *
     * @param trajetId Identifiant du trajet
     */
    public Trajet retirerPassager(int trajetId) throws ReservationInvalideException {
        try {
            Optional<Trajet> opt = trajetDAO.trouverParId(trajetId);
            if (opt.isEmpty()) {
                throw new ReservationInvalideException("Trajet #" + trajetId + " introuvable.");
            }
            Trajet trajet = opt.get();

            // Mise à jour en mémoire
            trajet.libererPlace();

            // Persistance
            trajetDAO.mettreAJourPlaces(trajetId, trajet.getPlacesDisponibles(), trajet.getStatut());

            return trajet;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du retrait du passager : " + e.getMessage(), e);
        }
    }

    /**
     * Annule un trajet (par le chauffeur).
     * <p>
     * Règle métier : si des passagers ont confirmé leur réservation, une pénalité
     * de 20 % du prix de chaque place est appliquée au chauffeur.
     * </p>
     *
     * @param trajetId    Identifiant du trajet
     * @param chauffeurId Identifiant du chauffeur
     * @return Montant de la pénalité (0 si aucune réservation confirmée)
     */
    public double annulerTrajetChauffeur(int trajetId, int chauffeurId)
            throws ReservationInvalideException {
        try {
            Optional<Trajet> opt = trajetDAO.trouverParId(trajetId);
            if (opt.isEmpty()) {
                throw new ReservationInvalideException("Trajet #" + trajetId + " introuvable.");
            }
            Trajet trajet = opt.get();

            if (trajet.getChauffeur().getId() != chauffeurId) {
                throw new IllegalArgumentException("Seul le chauffeur peut annuler ce trajet.");
            }
            if (trajet.getStatut() == StatutTrajet.ANNULE) {
                throw new IllegalStateException("Le trajet est déjà annulé.");
            }

            // Calcul de la pénalité (20% par passager avec réservation confirmée)
            int passagersConfirmes = trajet.getNombreReservationsConfirmees();
            double penalite = 0.0;

            if (passagersConfirmes > 0 && trajet.minutesAvantDepart() < 24 * 60) {
                // Pénalité 20% par passager si moins de 24h avant le départ
                penalite = passagersConfirmes * trajet.getPrixParPlace() * 0.20;
                System.out.println("[TrajetService] Pénalité chauffeur #" + chauffeurId +
                                   " : " + penalite + "€ (" + passagersConfirmes + " passagers)");
            }

            trajetDAO.mettreAJourStatut(trajetId, StatutTrajet.ANNULE);

            // Notification aux passagers
            notificationService.notifierEmail(
                "passagers@notification.sys",
                "Trajet annulé",
                "Le trajet #" + trajetId + " a été annulé par le chauffeur."
            );

            return penalite;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'annulation du trajet : " + e.getMessage(), e);
        }
    }

    /**
     * Recherche des trajets disponibles selon des critères.
     */
    public List<Trajet> rechercherTrajets(String villeDepart, String villeArrivee,
                                           String date, int placesMin) {
        try {
            return trajetDAO.rechercher(villeDepart, villeArrivee, date, placesMin);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de trajets : " + e.getMessage(), e);
        }
    }

    /**
     * Retourne tous les trajets disponibles (ouverts et futurs).
     */
    public List<Trajet> listerTrajetsDisponibles() {
        try {
            return trajetDAO.trouverDisponibles();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des trajets : " + e.getMessage(), e);
        }
    }

    /**
     * Retourne les trajets proposés par un chauffeur.
     */
    public List<Trajet> listerTrajetsParChauffeur(int chauffeurId) {
        try {
            return trajetDAO.trouverParChauffeur(chauffeurId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des trajets du chauffeur : " + e.getMessage(), e);
        }
    }

    /**
     * Recherche un trajet par son id.
     */
    public Optional<Trajet> trouverParId(int id) {
        try {
            return trajetDAO.trouverParId(id);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération du trajet : " + e.getMessage(), e);
        }
    }
}
