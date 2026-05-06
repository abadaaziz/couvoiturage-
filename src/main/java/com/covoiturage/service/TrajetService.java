package com.covoiturage.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.covoiturage.dao.ReservationDAO;
import com.covoiturage.dao.TrajetDAO;
import com.covoiturage.exception.PaiementEcheException;
import com.covoiturage.exception.ReservationInvalideException;
import com.covoiturage.exception.TrajetCompletException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Admin;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Reservation.StatutReservation;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.Trajet.StatutTrajet;
import com.covoiturage.model.Utilisateur;

/**
 * Service de gestion des trajets de covoiturage.
 */
public class TrajetService {

    private final TrajetDAO trajetDAO;
    private final ReservationDAO reservationDAO;
    private final PaiementService paiementService;
    private final NotificationService notificationService;
    private final InAppNotificationService inAppNotificationService;

    public TrajetService() {
        this(new TrajetDAO(), new ReservationDAO(), new PaiementService(),
            new NotificationService(), new InAppNotificationService());
    }

    public TrajetService(TrajetDAO trajetDAO, ReservationDAO reservationDAO,
                         NotificationService notificationService,
                         InAppNotificationService inAppNotificationService) {
        this(trajetDAO, reservationDAO, new PaiementService(), notificationService,
            inAppNotificationService);
    }

    public TrajetService(TrajetDAO trajetDAO, ReservationDAO reservationDAO,
                         PaiementService paiementService,
                         NotificationService notificationService,
                         InAppNotificationService inAppNotificationService) {
        this.trajetDAO = trajetDAO;
        this.reservationDAO = reservationDAO;
        this.paiementService = paiementService;
        this.notificationService = notificationService;
        this.inAppNotificationService = inAppNotificationService;
    }

    public TrajetService(TrajetDAO trajetDAO, NotificationService notificationService) {
        this(trajetDAO, new ReservationDAO(), notificationService, new InAppNotificationService());
    }

    public Trajet proposerTrajet(Utilisateur chauffeur, String villeDepart, String villeArrivee,
                                 LocalDateTime dateHeureDepart, int placesTotal,
                                 double prixParPlace, String descriptionVehicule)
            throws UtilisateurSuspenduException {
        if (!chauffeur.estActif()) {
            throw new UtilisateurSuspenduException(chauffeur.getEmail());
        }
        // Vérification par type réel — plus robuste qu'une comparaison de chaînes
        if (!(chauffeur instanceof Chauffeur) && !(chauffeur instanceof Admin)) {
            throw new IllegalArgumentException(
                "Seul un chauffeur peut proposer un trajet. Type reçu : "
                + chauffeur.getClass().getSimpleName());
        }
        if (dateHeureDepart.isBefore(LocalDateTime.now().plusMinutes(30))) {
            throw new IllegalArgumentException(
                "Le depart doit etre prevu au moins 30 minutes dans le futur.");
        }
        if (placesTotal < 1 || placesTotal > 8) {
            throw new IllegalArgumentException(
                "Le nombre de places doit etre compris entre 1 et 8.");
        }
        if (prixParPlace < 0 || prixParPlace > 500) {
            throw new IllegalArgumentException(
                "Le prix par place doit etre compris entre 0 et 500 DT.");
        }

        try {
            Trajet trajet = new Trajet(villeDepart, villeArrivee, dateHeureDepart,
                placesTotal, prixParPlace, (Chauffeur) chauffeur, descriptionVehicule);
            Trajet trajetPersiste = trajetDAO.inserer(trajet);

            notificationService.notifierEmail(
                chauffeur.getEmail(),
                "Trajet propose avec succes",
                "Votre trajet " + villeDepart + " -> " + villeArrivee +
                    " du " + dateHeureDepart + " a bien ete enregistre (ref. #" +
                    trajetPersiste.getId() + ")."
            );

            inAppNotificationService.notifierUtilisateur(
                chauffeur.getId(),
                "TRAJET",
                "Trajet propose",
                "Votre trajet #" + trajetPersiste.getId() + " est publie."
            );

            return trajetPersiste;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la creation du trajet : " + e.getMessage(), e);
        }
    }

    public void cloreTrajet(int trajetId, int chauffeurId) {
        try {
            Optional<Trajet> opt = trajetDAO.trouverParId(trajetId);
            if (opt.isEmpty()) {
                throw new IllegalArgumentException("Trajet #" + trajetId + " introuvable.");
            }
            Trajet trajet = opt.get();

            if (trajet.getChauffeur().getId() != chauffeurId) {
                throw new IllegalArgumentException("Seul le chauffeur du trajet peut le clore.");
            }
            if (trajet.getStatut() == StatutTrajet.ANNULE) {
                throw new IllegalStateException("Impossible de clore un trajet annule.");
            }

            trajetDAO.mettreAJourStatut(trajetId, StatutTrajet.TERMINE);
            System.out.println("[TrajetService] Trajet #" + trajetId + " cloture.");

            inAppNotificationService.notifierUtilisateur(
                chauffeurId,
                "TRAJET",
                "Trajet termine",
                "Votre trajet #" + trajetId + " est termine."
            );
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la cloture du trajet : " + e.getMessage(), e);
        }
    }

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
                    "Le trajet #" + trajetId + " n'est pas ouvert aux reservations (statut : " +
                        trajet.getStatut() + ").");
            }

            trajet.reserverPlace();
            trajetDAO.mettreAJourPlaces(trajetId, trajet.getPlacesDisponibles(), trajet.getStatut());

            return trajet;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'ajout du passager : " + e.getMessage(), e);
        }
    }

    public Trajet retirerPassager(int trajetId) throws ReservationInvalideException {
        try {
            Optional<Trajet> opt = trajetDAO.trouverParId(trajetId);
            if (opt.isEmpty()) {
                throw new ReservationInvalideException("Trajet #" + trajetId + " introuvable.");
            }
            Trajet trajet = opt.get();

            trajet.libererPlace();
            trajetDAO.mettreAJourPlaces(trajetId, trajet.getPlacesDisponibles(), trajet.getStatut());

            return trajet;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du retrait du passager : " + e.getMessage(), e);
        }
    }

    /**
     * Annule un trajet par le chauffeur.
     *
     * Les reservations confirmees sont remboursees a 120% du montant paye.
     * Les 20% supplementaires sont retournes comme penalite chauffeur.
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
                throw new IllegalStateException("Le trajet est deja annule.");
            }

            List<Reservation> reservations = reservationDAO.trouverParTrajet(trajetId);
            double penalite = traiterReservationsAnnuleesParChauffeur(reservations);

            if (penalite > 0) {
                System.out.println("[TrajetService] Penalite chauffeur #" + chauffeurId +
                    " : " + penalite + " DT");
            }

            trajetDAO.mettreAJourStatut(trajetId, StatutTrajet.ANNULE);

            notificationService.notifierEmail(
                "passagers@notification.sys",
                "Trajet annule",
                "Le trajet #" + trajetId + " a ete annule par le chauffeur."
            );

            inAppNotificationService.notifierUtilisateur(
                chauffeurId,
                "TRAJET",
                "Trajet annule",
                "Votre trajet #" + trajetId + " a ete annule."
            );

            notifierPassagersTrajetAnnule(trajetId, reservations);

            return penalite;
        } catch (PaiementEcheException e) {
            throw new RuntimeException("Erreur lors du remboursement des passagers : " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'annulation du trajet : " + e.getMessage(), e);
        }
    }

    private double traiterReservationsAnnuleesParChauffeur(List<Reservation> reservations)
            throws PaiementEcheException, SQLException {
        double penalite = 0.0;

        for (Reservation reservation : reservations) {
            if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
                double compensation = reservation.getMontantTotal() * 0.20;
                double montantRembourse = reservation.getMontantTotal() + compensation;

                paiementService.rembourser(reservation.getReferenceTransaction(), montantRembourse);

                reservation.setDateAnnulation(LocalDateTime.now());
                reservation.setMontantRembourse(montantRembourse);
                reservation.setStatut(StatutReservation.REMBOURSEE);
                reservationDAO.mettreAJourStatutAnnulation(reservation);

                penalite += compensation;
            } else if (reservation.getStatut() == StatutReservation.EN_ATTENTE) {
                reservation.setDateAnnulation(LocalDateTime.now());
                reservation.setMontantRembourse(0.0);
                reservation.setStatut(StatutReservation.ANNULEE);
                reservationDAO.mettreAJourStatutAnnulation(reservation);
            }
        }

        return penalite;
    }

    private void notifierPassagersTrajetAnnule(int trajetId, List<Reservation> reservations) {
        Set<Integer> passagersNotifies = new HashSet<>();
        for (Reservation reservation : reservations) {
            int passagerId = reservation.getPassager().getId();
            if (passagersNotifies.add(passagerId)) {
                inAppNotificationService.notifierUtilisateur(
                    passagerId,
                    "TRAJET",
                    "Trajet annule",
                    "Le trajet #" + trajetId + " a ete annule par le chauffeur."
                );
            }
        }
    }

    public List<Trajet> rechercherTrajets(String villeDepart, String villeArrivee,
                                          String date, int placesMin) {
        try {
            return trajetDAO.rechercher(villeDepart, villeArrivee, date, placesMin);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de trajets : " + e.getMessage(), e);
        }
    }

    public List<Trajet> rechercherTrajetsFlexible(String villeDepart, String villeArrivee,
                                                 String date, Integer placesMin) {
        try {
            return trajetDAO.rechercherFlexible(villeDepart, villeArrivee, date, placesMin);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de trajets : " + e.getMessage(), e);
        }
    }

    public List<Trajet> listerTrajetsDisponibles() {
        try {
            return trajetDAO.trouverDisponibles();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation des trajets : " + e.getMessage(), e);
        }
    }

    public List<Trajet> listerTrajetsParChauffeur(int chauffeurId) {
        try {
            return trajetDAO.trouverParChauffeur(chauffeurId);
        } catch (SQLException e) {
            throw new RuntimeException(
                "Erreur lors de la recuperation des trajets du chauffeur : " + e.getMessage(), e);
        }
    }

    public Optional<Trajet> trouverParId(int id) {
        try {
            return trajetDAO.trouverParId(id);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recuperation du trajet : " + e.getMessage(), e);
        }
    }
}
