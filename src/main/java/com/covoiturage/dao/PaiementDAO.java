package com.covoiturage.dao;

import com.covoiturage.model.Paiement;
import com.covoiturage.model.Paiement.MethodePaiement;
import com.covoiturage.model.Paiement.StatutPaiement;
import com.covoiturage.model.Reservation;
import com.covoiturage.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour l'entité {@link Paiement}. JDBC pur.
 */
public class PaiementDAO {

    private static final String SQL_INSERT =
        "INSERT INTO paiements (reservation_id, montant, montant_rembourse, statut, methode, " +
        "reference_externe, date_autorisation) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
        "SELECT * FROM paiements WHERE id = ?";

    private static final String SQL_SELECT_BY_RESERVATION =
        "SELECT * FROM paiements WHERE reservation_id = ? ORDER BY date_autorisation DESC";

    private static final String SQL_UPDATE_STATUT =
        "UPDATE paiements SET statut=?, date_capture=?, date_remboursement=?, " +
        "montant_rembourse=?, message_erreur=? WHERE id=?";

    // ── Méthodes CRUD ─────────────────────────────────────────────────────────

    public Paiement inserer(Paiement paiement) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, paiement.getReservation().getId());
            ps.setDouble(2, paiement.getMontant());
            ps.setDouble(3, paiement.getMontantRembourse());
            ps.setString(4, paiement.getStatut().name());
            ps.setString(5, paiement.getMethode().name());
            ps.setString(6, paiement.getReferenceExterne());
            ps.setTimestamp(7, paiement.getDateAutorisation() != null
                ? Timestamp.valueOf(paiement.getDateAutorisation()) : new Timestamp(System.currentTimeMillis()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) paiement.setId(rs.getInt(1));
            }
        }
        return paiement;
    }

    public Optional<Paiement> trouverParId(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapperResultSet(rs));
            }
        }
        return Optional.empty();
    }

    public List<Paiement> trouverParReservation(int reservationId) throws SQLException {
        List<Paiement> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_RESERVATION)) {

            ps.setInt(1, reservationId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapperResultSet(rs));
            }
        }
        return liste;
    }

    /**
     * Met à jour le statut et les dates associées d'un paiement.
     */
    public void mettreAJourStatut(Paiement paiement) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUT)) {

            ps.setString(1, paiement.getStatut().name());
            ps.setTimestamp(2, paiement.getDateCapture() != null
                ? Timestamp.valueOf(paiement.getDateCapture()) : null);
            ps.setTimestamp(3, paiement.getDateRemboursement() != null
                ? Timestamp.valueOf(paiement.getDateRemboursement()) : null);
            ps.setDouble(4, paiement.getMontantRembourse());
            ps.setString(5, paiement.getMessageErreur());
            ps.setInt(6, paiement.getId());
            ps.executeUpdate();
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private Paiement mapperResultSet(ResultSet rs) throws SQLException {
        Paiement p = new Paiement();
        p.setId(rs.getInt("id"));

        // Association minimale à la réservation (hydratation par l'id uniquement)
        Reservation reservation = new Reservation();
        reservation.setId(rs.getInt("reservation_id"));
        p.setReservation(reservation);

        p.setMontant(rs.getDouble("montant"));
        p.setMontantRembourse(rs.getDouble("montant_rembourse"));
        p.setStatut(StatutPaiement.valueOf(rs.getString("statut")));
        p.setMethode(MethodePaiement.valueOf(rs.getString("methode")));
        p.setReferenceExterne(rs.getString("reference_externe"));

        Timestamp tsAuto = rs.getTimestamp("date_autorisation");
        if (tsAuto != null) p.setDateAutorisation(tsAuto.toLocalDateTime());

        Timestamp tsCapt = rs.getTimestamp("date_capture");
        if (tsCapt != null) p.setDateCapture(tsCapt.toLocalDateTime());

        Timestamp tsRemb = rs.getTimestamp("date_remboursement");
        if (tsRemb != null) p.setDateRemboursement(tsRemb.toLocalDateTime());

        p.setMessageErreur(rs.getString("message_erreur"));
        return p;
    }
}
