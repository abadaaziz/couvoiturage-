package com.covoiturage.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import com.covoiturage.dao.ReservationDAO;
import com.covoiturage.dao.TrajetDAO;
import com.covoiturage.exception.PaiementEcheException;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Reservation.StatutReservation;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.Trajet.StatutTrajet;

class TrajetServiceTest {

    @Test
    void annulerTrajetChauffeurRembourseReservationConfirmeeAvecVingtPourcentEnPlus()
            throws Exception {
        Chauffeur chauffeur = chauffeur(10);
        Passager passager = passager(20);

        Trajet trajet = new Trajet();
        trajet.setId(7);
        trajet.setChauffeur(chauffeur);
        trajet.setStatut(StatutTrajet.OUVERT);
        trajet.setDateHeureDepart(LocalDateTime.now().plusHours(3));
        trajet.setPrixParPlace(50.0);

        Reservation reservation = new Reservation();
        reservation.setId(99);
        reservation.setTrajet(trajet);
        reservation.setPassager(passager);
        reservation.setNombrePlaces(1);
        reservation.setMontantTotal(50.0);
        reservation.setStatut(StatutReservation.CONFIRMEE);
        reservation.setReferenceTransaction("AUTH-123");

        FakeTrajetDAO trajetDAO = new FakeTrajetDAO(trajet);
        FakeReservationDAO reservationDAO = new FakeReservationDAO(List.of(reservation));
        RecordingPaiementService paiementService = new RecordingPaiementService();

        TrajetService service = new TrajetService(
            trajetDAO,
            reservationDAO,
            paiementService,
            new NoopNotificationService(),
            new NoopInAppNotificationService()
        );

        double penalite = service.annulerTrajetChauffeur(7, 10);

        assertEquals(10.0, penalite, 0.001);
        assertEquals(60.0, reservation.getMontantRembourse(), 0.001);
        assertEquals(StatutReservation.REMBOURSEE, reservation.getStatut());
        assertNotNull(reservation.getDateAnnulation());
        assertEquals(StatutTrajet.ANNULE, trajetDAO.statutMisAJour);
        assertEquals("AUTH-123", paiementService.referenceRemboursee);
        assertEquals(60.0, paiementService.montantRembourse, 0.001);
    }

    private static Chauffeur chauffeur(int id) {
        Chauffeur utilisateur = new Chauffeur();
        utilisateur.setId(id);
        utilisateur.setNom("Nom" + id);
        utilisateur.setPrenom("Prenom" + id);
        utilisateur.setEmail("user" + id + "@example.com");
        return utilisateur;
    }

    private static Passager passager(int id) {
        Passager utilisateur = new Passager();
        utilisateur.setId(id);
        utilisateur.setNom("Nom" + id);
        utilisateur.setPrenom("Prenom" + id);
        utilisateur.setEmail("user" + id + "@example.com");
        return utilisateur;
    }

    private static final class FakeTrajetDAO extends TrajetDAO {
        private final Trajet trajet;
        private StatutTrajet statutMisAJour;

        private FakeTrajetDAO(Trajet trajet) {
            this.trajet = trajet;
        }

        @Override
        public Optional<Trajet> trouverParId(int id) {
            return Optional.of(trajet);
        }

        @Override
        public void mettreAJourStatut(int trajetId, StatutTrajet statut) {
            this.statutMisAJour = statut;
        }
    }

    private static final class FakeReservationDAO extends ReservationDAO {
        private final List<Reservation> reservations;

        private FakeReservationDAO(List<Reservation> reservations) {
            this.reservations = reservations;
        }

        @Override
        public List<Reservation> trouverParTrajet(int trajetId) throws SQLException {
            return reservations;
        }

        @Override
        public void mettreAJourStatutAnnulation(Reservation reservation) {

        }
    }

    private static final class RecordingPaiementService extends PaiementService {
        private String referenceRemboursee;
        private double montantRembourse;

        @Override
        public void rembourser(String referenceTransaction, double montantARembourser)
                throws PaiementEcheException {
            this.referenceRemboursee = referenceTransaction;
            this.montantRembourse = montantARembourser;
        }
    }

    private static final class NoopNotificationService extends NotificationService {
        @Override
        public void notifierEmail(String destinataire, String sujet, String corps) {

        }
    }

    private static final class NoopInAppNotificationService extends InAppNotificationService {
        @Override
        public void notifierUtilisateur(int utilisateurId, String type, String titre, String message) {

        }
    }
}
