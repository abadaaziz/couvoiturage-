package com.covoiturage.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import com.covoiturage.dao.ReservationDAO;
import com.covoiturage.exception.PaiementEcheException;
import com.covoiturage.exception.ReservationInvalideException;
import com.covoiturage.exception.TrajetCompletException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Paiement.MethodePaiement;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Reservation.StatutReservation;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.Trajet.StatutTrajet;
import com.covoiturage.model.Utilisateur;


public class ReservationService {

    private final ReservationDAO      reservationDAO;
    private final TrajetService       trajetService;
    private final PaiementService     paiementService;
    private final NotificationService notificationService;
    private final InAppNotificationService inAppNotificationService;

    public ReservationService() {
        this.reservationDAO      = new ReservationDAO();
        this.trajetService       = new TrajetService();
        this.paiementService     = new PaiementService();
        this.notificationService = new NotificationService();
        this.inAppNotificationService = new InAppNotificationService();
    }

    
    public ReservationService(ReservationDAO reservationDAO, TrajetService trajetService,
                               PaiementService paiementService, NotificationService notificationService,
                               InAppNotificationService inAppNotificationService) {
        this.reservationDAO      = reservationDAO;
        this.trajetService       = trajetService;
        this.paiementService     = paiementService;
        this.notificationService = notificationService;
        this.inAppNotificationService = inAppNotificationService;
    }

    
    public ReservationService(ReservationDAO reservationDAO, TrajetService trajetService,
                               PaiementService paiementService, NotificationService notificationService) {
        this(reservationDAO, trajetService, paiementService, notificationService,
            new InAppNotificationService());
    }



    
    public Reservation creerReservation(Utilisateur passager, int trajetId,
                                         int nombrePlaces, MethodePaiement methode)
            throws TrajetCompletException, ReservationInvalideException,
                   PaiementEcheException, UtilisateurSuspenduException {


        if (!passager.estActif()) {
            throw new UtilisateurSuspenduException(passager.getEmail());
        }


        Optional<Trajet> optTrajet = trajetService.trouverParId(trajetId);
        if (optTrajet.isEmpty()) {
            throw new ReservationInvalideException("Trajet #" + trajetId + " introuvable.");
        }
        Trajet trajet = optTrajet.get();

        if (trajet.getStatut() == StatutTrajet.COMPLET) {
            throw new TrajetCompletException(trajetId);
        }
        if (trajet.getStatut() != StatutTrajet.OUVERT) {
            throw new ReservationInvalideException(
                "Le trajet #" + trajetId + " n'accepte plus de réservations (statut : " +
                trajet.getStatut() + ").");
        }
        if (nombrePlaces < 1 || nombrePlaces > 8) {
            throw new ReservationInvalideException(
                "Le nombre de places doit être compris entre 1 et 8.");
        }
        if (trajet.getPlacesDisponibles() < nombrePlaces) {
            throw new ReservationInvalideException(
                "Seulement " + trajet.getPlacesDisponibles() +
                " place(s) disponible(s), vous en demandez " + nombrePlaces + ".");
        }
        if (trajet.getChauffeur().getId() == passager.getId()) {
            throw new ReservationInvalideException(
                "Un chauffeur ne peut pas réserver une place sur son propre trajet.");
        }

        try {

            double montant = trajet.getPrixParPlace() * nombrePlaces;


                                            Reservation reservation = new Reservation(trajet, (Passager) passager, nombrePlaces);



            reservationDAO.inserer(reservation);



            String referenceTransaction = paiementService.autoriser(reservation, montant, methode);
            reservation.setReferenceTransaction(referenceTransaction);
            reservationDAO.mettreAJourReferenceTransaction(reservation.getId(), referenceTransaction);


            for (int i = 0; i < nombrePlaces; i++) {
                trajetService.ajouterPassager(trajetId);
            }


            notificationService.notifierEmail(
                passager.getEmail(),
                "Confirmation de votre demande de réservation",
                "Votre demande de réservation #" + reservation.getId() +
                " pour le trajet " + trajet.getVilleDepart() + " → " + trajet.getVilleArrivee() +
                " est en cours de traitement. En attente de confirmation du chauffeur."
            );
            notificationService.notifierSMS(
                passager.getTelephone(),
                "Same Trip : Réservation #" + reservation.getId() + " en attente de confirmation."
            );
            notificationService.notifierEmail(
                trajet.getChauffeur().getEmail(),
                "Nouvelle demande de réservation",
                passager.getPrenom() + " " + passager.getNom() +
                " souhaite réserver " + nombrePlaces + " place(s) sur votre trajet #" + trajetId + "."
            );

            inAppNotificationService.notifierUtilisateur(
                passager.getId(),
                "RESERVATION",
                "Reservation creee",
                "Votre reservation #" + reservation.getId() + " est en attente de confirmation."
            );
            inAppNotificationService.notifierUtilisateur(
                trajet.getChauffeur().getId(),
                "RESERVATION",
                "Nouvelle reservation",
                passager.getPrenom() + " " + passager.getNom() +
                " a reserve " + nombrePlaces + " place(s) sur votre trajet #" + trajetId + "."
            );

            return reservation;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création de la réservation : " + e.getMessage(), e);
        }
    }

    
    public void confirmerReservation(int reservationId, int chauffeurId)
            throws ReservationInvalideException, PaiementEcheException {
        try {
            Optional<Reservation> opt = reservationDAO.trouverParId(reservationId);
            if (opt.isEmpty()) {
                throw new ReservationInvalideException("Réservation #" + reservationId + " introuvable.");
            }
            Reservation reservation = opt.get();


            if (reservation.getTrajet().getChauffeur().getId() != chauffeurId) {
                throw new ReservationInvalideException(
                    "Seul le chauffeur du trajet peut confirmer cette réservation.");
            }
            if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
                throw new ReservationInvalideException(
                    "Seule une réservation EN_ATTENTE peut être confirmée. Statut actuel : " +
                    reservation.getStatut());
            }


            paiementService.capturer(reservation.getReferenceTransaction());


            reservation.confirmer();
            reservationDAO.confirmer(reservationId);


            notificationService.notifierEmail(
                reservation.getPassager().getEmail(),
                "Réservation confirmée !",
                "Votre réservation #" + reservationId + " pour le trajet " +
                reservation.getTrajet().getVilleDepart() + " → " +
                reservation.getTrajet().getVilleArrivee() + " est confirmée. Bon voyage !"
            );

            inAppNotificationService.notifierUtilisateur(
                reservation.getPassager().getId(),
                "RESERVATION",
                "Reservation confirmee",
                "Votre reservation #" + reservationId + " est confirmee par le chauffeur."
            );
            inAppNotificationService.notifierUtilisateur(
                reservation.getTrajet().getChauffeur().getId(),
                "RESERVATION",
                "Reservation confirmee",
                "Vous avez confirme la reservation #" + reservationId + "."
            );

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la confirmation de la réservation : " + e.getMessage(), e);
        }
    }

    
    public double annulerReservation(int reservationId, int passagerId)
            throws ReservationInvalideException, PaiementEcheException {
        try {
            Optional<Reservation> opt = reservationDAO.trouverParId(reservationId);
            if (opt.isEmpty()) {
                throw new ReservationInvalideException("Réservation #" + reservationId + " introuvable.");
            }
            Reservation reservation = opt.get();


            if (reservation.getPassager().getId() != passagerId) {
                throw new ReservationInvalideException(
                    "Vous n'êtes pas autorisé à annuler cette réservation.");
            }
            if (reservation.getStatut() == StatutReservation.ANNULEE ||
                reservation.getStatut() == StatutReservation.REMBOURSEE) {
                throw new ReservationInvalideException(
                    "Cette réservation est déjà annulée ou remboursée.");
            }


            double montantARemb;
            java.time.LocalDateTime depart = reservation.getTrajet().getDateHeureDepart();
            if (depart != null) {
                long heuresAvantDepart = java.time.Duration
                    .between(java.time.LocalDateTime.now(), depart)
                    .toHours();
                montantARemb = heuresAvantDepart > 24
                    ? reservation.getMontantTotal()
                    : reservation.getMontantTotal() * 0.50;
            } else {
                montantARemb = reservation.getMontantTotal() * 0.50;
            }


            reservation.annuler();
            reservationDAO.mettreAJourStatutAnnulation(reservation);


            for (int i = 0; i < reservation.getNombrePlaces(); i++) {
                trajetService.retirerPassager(reservation.getTrajet().getId());
            }


            rembourserReservation(reservationId, montantARemb, reservation.getReferenceTransaction());


            boolean remboursementTotal = montantARemb >= reservation.getMontantTotal();
            notificationService.notifierEmail(
                reservation.getPassager().getEmail(),
                "Réservation annulée",
                "Votre réservation #" + reservationId + " a été annulée. " +
                "Remboursement de " + String.format("%.2f", montantARemb) + " DT " +
                (remboursementTotal ? "(total)" : "(partiel — moins de 24h avant départ)") +
                " en cours de traitement."
            );

            inAppNotificationService.notifierUtilisateur(
                reservation.getPassager().getId(),
                "RESERVATION",
                "Reservation annulee",
                "Votre reservation #" + reservationId + " a ete annulee."
            );
            inAppNotificationService.notifierUtilisateur(
                reservation.getTrajet().getChauffeur().getId(),
                "RESERVATION",
                "Reservation annulee",
                "Le passager a annule la reservation #" + reservationId + "."
            );

            return montantARemb;

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'annulation de la réservation : " + e.getMessage(), e);
        }
    }

    
    public void rembourserReservation(int reservationId, double montantARembourser,
                                       String referenceTransaction) throws PaiementEcheException {
        try {
            paiementService.rembourser(referenceTransaction, montantARembourser);


            Optional<Reservation> opt = reservationDAO.trouverParId(reservationId);
            if (opt.isPresent()) {
                Reservation r = opt.get();
                r.marquerCommeRemboursee();
                r.setMontantRembourse(montantARembourser);
                reservationDAO.mettreAJourStatutAnnulation(r);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du remboursement : " + e.getMessage(), e);
        }
    }

    
    public List<Reservation> listerReservationsPassager(int passagerId) {
        try {
            return reservationDAO.trouverParPassager(passagerId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des réservations : " + e.getMessage(), e);
        }
    }

    
    public List<Reservation> listerReservationsTrajet(int trajetId) {
        try {
            return reservationDAO.trouverParTrajet(trajetId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des réservations : " + e.getMessage(), e);
        }
    }

    
    public List<Reservation> listerReservationsChauffeur(int chauffeurId) {
        try {
            return reservationDAO.trouverParChauffeur(chauffeurId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des réservations chauffeur : " + e.getMessage(), e);
        }
    }

    
    public List<Reservation> listerEligiblesNotation(int passagerId, int chauffeurId) {
        try {
            return reservationDAO.trouverEligiblesNotation(passagerId, chauffeurId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la récupération des réservations notables : " + e.getMessage(), e);
        }
    }

    
    public void supprimerReservationSiTerminee(int reservationId, int passagerId)
            throws ReservationInvalideException {
        try {
            Optional<Reservation> opt = reservationDAO.trouverParId(reservationId);
            if (opt.isEmpty()) {
                throw new ReservationInvalideException("Réservation #" + reservationId + " introuvable.");
            }

            Reservation reservation = opt.get();
            if (reservation.getPassager().getId() != passagerId) {
                throw new ReservationInvalideException("Vous n'etes pas autorise a supprimer cette reservation.");
            }

            if (reservation.getTrajet().getDateHeureDepart() == null ||
                reservation.getTrajet().getDateHeureDepart().isAfter(java.time.LocalDateTime.now())) {
                throw new ReservationInvalideException("La reservation ne peut etre supprimee qu'apres la date du trajet.");
            }

            boolean deleted = reservationDAO.supprimerReservationPasse(reservationId, passagerId);
            if (!deleted) {
                throw new ReservationInvalideException("Impossible de supprimer cette reservation.");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la reservation : " + e.getMessage(), e);
        }
    }

    
    public void noterChauffeur(int reservationId, int passagerId, int note)
            throws ReservationInvalideException {
        if (note < 1 || note > 5) {
            throw new ReservationInvalideException("La note doit etre comprise entre 1 et 5.");
        }

        try {
            Optional<Reservation> opt = reservationDAO.trouverParId(reservationId);
            if (opt.isEmpty()) {
                throw new ReservationInvalideException("Reservation #" + reservationId + " introuvable.");
            }

            Reservation reservation = opt.get();
            if (reservation.getPassager().getId() != passagerId) {
                throw new ReservationInvalideException("Vous n'etes pas autorise a noter cette reservation.");
            }
            if (reservation.getStatut() != StatutReservation.CONFIRMEE) {
                throw new ReservationInvalideException("Seules les reservations confirmees peuvent etre notees.");
            }
            if (reservation.getNotePassager() != null) {
                throw new ReservationInvalideException("Vous avez deja note ce chauffeur.");
            }


            reservationDAO.mettreAJourNotePassager(reservationId, note);


            int chauffeurId = reservation.getTrajet().getChauffeur().getId();
            Optional<Utilisateur> optChauffeur = new com.covoiturage.dao.UtilisateurDAO().trouverParId(chauffeurId);
            if (optChauffeur.isEmpty()) {
                throw new ReservationInvalideException("Chauffeur introuvable.");
            }

            Chauffeur chauffeur = (Chauffeur) optChauffeur.get();
            chauffeur.ajouterAvis(note);
            new com.covoiturage.dao.UtilisateurDAO().mettreAJour(chauffeur);

        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la notation : " + e.getMessage(), e);
        }
    }
}
