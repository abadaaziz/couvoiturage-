package com.covoiturage.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.covoiturage.model.Notification;
import com.covoiturage.util.DatabaseConnection;


public class NotificationDAO {

    private static final String SQL_INSERT =
        "INSERT INTO notifications (utilisateur_id, type, titre, message, date_creation) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_USER =
        "SELECT * FROM notifications WHERE utilisateur_id = ? " +
        "ORDER BY date_creation DESC";

    private static final String SQL_MARK_READ =
        "UPDATE notifications SET date_lecture = CURRENT_TIMESTAMP " +
        "WHERE id = ? AND utilisateur_id = ?";

    private static final String SQL_MARK_ALL_READ =
        "UPDATE notifications SET date_lecture = CURRENT_TIMESTAMP " +
        "WHERE utilisateur_id = ? AND date_lecture IS NULL";

    private static final String SQL_DELETE_BY_ID =
        "DELETE FROM notifications WHERE id = ? AND utilisateur_id = ?";

    public Notification inserer(Notification notification) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, notification.getUtilisateurId());
            ps.setString(2, notification.getType());
            ps.setString(3, notification.getTitre());
            ps.setString(4, notification.getMessage());
            ps.setTimestamp(5, Timestamp.valueOf(notification.getDateCreation()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) notification.setId(rs.getInt(1));
            }
        }
        return notification;
    }

    public List<Notification> listerParUtilisateur(int utilisateurId) throws SQLException {
        List<Notification> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_USER)) {

            ps.setInt(1, utilisateurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapperResultSet(rs));
                }
            }
        }
        return liste;
    }

    public void marquerCommeLu(int notificationId, int utilisateurId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_MARK_READ)) {

            ps.setInt(1, notificationId);
            ps.setInt(2, utilisateurId);
            ps.executeUpdate();
        }
    }

    public void marquerTousLus(int utilisateurId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_MARK_ALL_READ)) {

            ps.setInt(1, utilisateurId);
            ps.executeUpdate();
        }
    }

    public boolean supprimer(int notificationId, int utilisateurId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE_BY_ID)) {

            ps.setInt(1, notificationId);
            ps.setInt(2, utilisateurId);
            return ps.executeUpdate() > 0;
        }
    }

    private Notification mapperResultSet(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getInt("id"));
        n.setUtilisateurId(rs.getInt("utilisateur_id"));
        n.setType(rs.getString("type"));
        n.setTitre(rs.getString("titre"));
        n.setMessage(rs.getString("message"));

        Timestamp created = rs.getTimestamp("date_creation");
        if (created != null) n.setDateCreation(created.toLocalDateTime());

        Timestamp read = rs.getTimestamp("date_lecture");
        if (read != null) n.setDateLecture(read.toLocalDateTime());

        return n;
    }
}
