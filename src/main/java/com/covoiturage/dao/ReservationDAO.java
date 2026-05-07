package com.covoiturage.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Reservation.StatutReservation;
import com.covoiturage.model.Trajet;
import com.covoiturage.util.DatabaseConnection;


public class ReservationDAO {

    private static final String SQL_INSERT =
        "INSERT INTO reservations (trajet_id, passager_id, nombre_places, montant_total, " +
        "statut, date_reservation, reference_transaction) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
        "SELECT r.*, " +
        "t.ville_depart, t.ville_arrivee, t.date_heure_depart, t.prix_par_place, t.chauffeur_id, " +
        "u.nom AS passager_nom, u.prenom AS passager_prenom, u.email AS passager_email, " +
        "c.nom AS chauffeur_nom, c.prenom AS chauffeur_prenom, c.email AS chauffeur_email, " +
        "c.telephone AS chauffeur_telephone, c.note_moyenne AS chauffeur_note " +
        "FROM reservations r " +
        "JOIN trajets t ON r.trajet_id = t.id " +
        "JOIN utilisateurs u ON r.passager_id = u.id " +
        "JOIN utilisateurs c ON t.chauffeur_id = c.id " +
        "WHERE r.id = ?";

    private static final String SQL_SELECT_BY_PASSAGER =
        "SELECT r.*, " +
        "t.ville_depart, t.ville_arrivee, t.date_heure_depart, t.prix_par_place, t.chauffeur_id, " +
        "u.nom AS passager_nom, u.prenom AS passager_prenom, u.email AS passager_email, " +
        "c.nom AS chauffeur_nom, c.prenom AS chauffeur_prenom, c.email AS chauffeur_email, " +
        "c.telephone AS chauffeur_telephone, c.note_moyenne AS chauffeur_note " +
        "FROM reservations r " +
        "JOIN trajets t ON r.trajet_id = t.id " +
        "JOIN utilisateurs u ON r.passager_id = u.id " +
        "JOIN utilisateurs c ON t.chauffeur_id = c.id " +
        "WHERE r.passager_id = ? ORDER BY r.date_reservation DESC";

    private static final String SQL_SELECT_BY_TRAJET =
        "SELECT r.*, " +
        "t.ville_depart, t.ville_arrivee, t.date_heure_depart, t.prix_par_place, t.chauffeur_id, " +
        "u.nom AS passager_nom, u.prenom AS passager_prenom, u.email AS passager_email, " +
        "c.nom AS chauffeur_nom, c.prenom AS chauffeur_prenom, c.email AS chauffeur_email, " +
        "c.telephone AS chauffeur_telephone, c.note_moyenne AS chauffeur_note " +
        "FROM reservations r " +
        "JOIN trajets t ON r.trajet_id = t.id " +
        "JOIN utilisateurs u ON r.passager_id = u.id " +
        "JOIN utilisateurs c ON t.chauffeur_id = c.id " +
        "WHERE r.trajet_id = ? ORDER BY r.date_reservation ASC";

    private static final String SQL_SELECT_BY_CHAUFFEUR =
        "SELECT r.*, " +
        "t.ville_depart, t.ville_arrivee, t.date_heure_depart, t.prix_par_place, t.chauffeur_id, " +
        "u.nom AS passager_nom, u.prenom AS passager_prenom, u.email AS passager_email, " +
        "c.nom AS chauffeur_nom, c.prenom AS chauffeur_prenom, c.email AS chauffeur_email, " +
        "c.telephone AS chauffeur_telephone, c.note_moyenne AS chauffeur_note " +
        "FROM reservations r " +
        "JOIN trajets t ON r.trajet_id = t.id " +
        "JOIN utilisateurs u ON r.passager_id = u.id " +
        "JOIN utilisateurs c ON t.chauffeur_id = c.id " +
        "WHERE t.chauffeur_id = ? ORDER BY r.date_reservation DESC";

    private static final String SQL_UPDATE_STATUT =
        "UPDATE reservations SET statut=?, date_annulation=?, montant_rembourse=? WHERE id=?";

    private static final String SQL_UPDATE_CONFIRMATION =
        "UPDATE reservations SET statut='CONFIRMEE' WHERE id=?";

    private static final String SQL_UPDATE_REFERENCE_TRANSACTION =
        "UPDATE reservations SET reference_transaction=? WHERE id=?";

    private static final String SQL_UPDATE_NOTE_PASSAGER =
        "UPDATE reservations SET note_passager=? WHERE id=?";

    private static final String SQL_COUNT_ACTIVES_BY_TRAJET =
        "SELECT COUNT(*) FROM reservations WHERE trajet_id=? AND statut IN ('EN_ATTENTE','CONFIRMEE')";

    private static final String SQL_DELETE_PAST_BY_PASSAGER =
        "DELETE FROM reservations WHERE id = ? AND passager_id = ? " +
        "AND trajet_id IN (SELECT id FROM trajets WHERE date_heure_depart < CURRENT_TIMESTAMP)";

    private static final String SQL_SELECT_ELIGIBLE_RATING =
        "SELECT r.id, t.ville_depart, t.ville_arrivee, t.date_heure_depart " +
        "FROM reservations r " +
        "JOIN trajets t ON r.trajet_id = t.id " +
        "WHERE r.passager_id = ? AND t.chauffeur_id = ? " +
        "AND r.statut = 'CONFIRMEE' " +
        "AND (r.note_passager IS NULL) " +
        "ORDER BY t.date_heure_depart DESC";



    public Reservation inserer(Reservation reservation) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, reservation.getTrajet().getId());
            ps.setInt(2, reservation.getPassager().getId());
            ps.setInt(3, reservation.getNombrePlaces());
            ps.setDouble(4, reservation.getMontantTotal());
            ps.setString(5, reservation.getStatut().name());
            ps.setTimestamp(6, Timestamp.valueOf(reservation.getDateReservation()));
            ps.setString(7, reservation.getReferenceTransaction());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) reservation.setId(rs.getInt(1));
            }
        }
        return reservation;
    }

    public Optional<Reservation> trouverParId(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapperResultSet(rs));
            }
        }
        return Optional.empty();
    }

    public List<Reservation> trouverParPassager(int passagerId) throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_PASSAGER)) {

            ps.setInt(1, passagerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapperResultSet(rs));
            }
        }
        return liste;
    }

    public List<Reservation> trouverParTrajet(int trajetId) throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_TRAJET)) {

            ps.setInt(1, trajetId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapperResultSet(rs));
            }
        }
        return liste;
    }

    public List<Reservation> trouverParChauffeur(int chauffeurId) throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_CHAUFFEUR)) {

            ps.setInt(1, chauffeurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapperResultSet(rs));
            }
        }
        return liste;
    }

    
    public void mettreAJourStatutAnnulation(Reservation reservation) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUT)) {

            ps.setString(1, reservation.getStatut().name());
            ps.setTimestamp(2, reservation.getDateAnnulation() != null
                ? Timestamp.valueOf(reservation.getDateAnnulation()) : null);
            ps.setDouble(3, reservation.getMontantRembourse());
            ps.setInt(4, reservation.getId());
            ps.executeUpdate();
        }
    }

    
    public void confirmer(int reservationId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_CONFIRMATION)) {

            ps.setInt(1, reservationId);
            ps.executeUpdate();
        }
    }

    
    public void mettreAJourReferenceTransaction(int reservationId, String referenceTransaction)
            throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_REFERENCE_TRANSACTION)) {

            ps.setString(1, referenceTransaction);
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        }
    }

    
    public void mettreAJourNotePassager(int reservationId, int note) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_NOTE_PASSAGER)) {

            ps.setInt(1, note);
            ps.setInt(2, reservationId);
            ps.executeUpdate();
        }
    }

    
    public int compterReservationsActives(int trajetId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_COUNT_ACTIVES_BY_TRAJET)) {

            ps.setInt(1, trajetId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    
    public boolean supprimerReservationPasse(int reservationId, int passagerId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {

                boolean eligible = false;
                try (PreparedStatement check = conn.prepareStatement(
                        "SELECT COUNT(*) FROM reservations r " +
                        "JOIN trajets t ON r.trajet_id = t.id " +
                        "WHERE r.id = ? AND r.passager_id = ? AND t.date_heure_depart < CURRENT_TIMESTAMP")) {
                    check.setInt(1, reservationId);
                    check.setInt(2, passagerId);
                    try (ResultSet rs = check.executeQuery()) {
                        eligible = rs.next() && rs.getInt(1) > 0;
                    }
                }
                if (!eligible) {
                    conn.rollback();
                    return false;
                }


                try (PreparedStatement delPay = conn.prepareStatement(
                        "DELETE FROM paiements WHERE reservation_id = ?")) {
                    delPay.setInt(1, reservationId);
                    delPay.executeUpdate();
                }


                try (PreparedStatement delRes = conn.prepareStatement(
                        "DELETE FROM reservations WHERE id = ? AND passager_id = ?")) {
                    delRes.setInt(1, reservationId);
                    delRes.setInt(2, passagerId);
                    int rows = delRes.executeUpdate();
                    conn.commit();
                    return rows > 0;
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    
    public List<Reservation> trouverEligiblesNotation(int passagerId, int chauffeurId) throws SQLException {
        List<Reservation> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ELIGIBLE_RATING)) {

            ps.setInt(1, passagerId);
            ps.setInt(2, chauffeurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Reservation r = new Reservation();
                    r.setId(rs.getInt("id"));
                    Trajet t = new Trajet();
                    t.setVilleDepart(rs.getString("ville_depart"));
                    t.setVilleArrivee(rs.getString("ville_arrivee"));
                    t.setDateHeureDepart(rs.getTimestamp("date_heure_depart").toLocalDateTime());
                    r.setTrajet(t);
                    liste.add(r);
                }
            }
        }
        return liste;
    }



    private Reservation mapperResultSet(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));


        Trajet trajet = new Trajet();
        trajet.setId(rs.getInt("trajet_id"));
        trajet.setVilleDepart(rs.getString("ville_depart"));
        trajet.setVilleArrivee(rs.getString("ville_arrivee"));
        trajet.setDateHeureDepart(rs.getTimestamp("date_heure_depart").toLocalDateTime());
        trajet.setPrixParPlace(rs.getDouble("prix_par_place"));
        Chauffeur chauffeur = new Chauffeur();
        chauffeur.setId(rs.getInt("chauffeur_id"));
        chauffeur.setNom(rs.getString("chauffeur_nom"));
        chauffeur.setPrenom(rs.getString("chauffeur_prenom"));
        chauffeur.setEmail(rs.getString("chauffeur_email"));
        chauffeur.setTelephone(rs.getString("chauffeur_telephone"));
        chauffeur.setNoteMoyenne(rs.getDouble("chauffeur_note"));
        trajet.setChauffeur(chauffeur);
        r.setTrajet(trajet);


        Passager passager = new Passager();
        passager.setId(rs.getInt("passager_id"));
        passager.setNom(rs.getString("passager_nom"));
        passager.setPrenom(rs.getString("passager_prenom"));
        passager.setEmail(rs.getString("passager_email"));
        r.setPassager(passager);

        r.setNombrePlaces(rs.getInt("nombre_places"));
        r.setMontantTotal(rs.getDouble("montant_total"));
        r.setStatut(StatutReservation.valueOf(rs.getString("statut")));
        r.setReferenceTransaction(rs.getString("reference_transaction"));

        int notePassager = rs.getInt("note_passager");
        if (rs.wasNull()) {
            r.setNotePassager(null);
        } else {
            r.setNotePassager(notePassager);
        }

        Timestamp tsRes = rs.getTimestamp("date_reservation");
        if (tsRes != null) r.setDateReservation(tsRes.toLocalDateTime());

        Timestamp tsAnn = rs.getTimestamp("date_annulation");
        if (tsAnn != null) r.setDateAnnulation(tsAnn.toLocalDateTime());

        r.setMontantRembourse(rs.getDouble("montant_rembourse"));
        return r;
    }
}
