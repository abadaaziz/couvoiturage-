package com.covoiturage.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.covoiturage.util.DatabaseConnection;

/**
 * DAO pour les notes de l'application.
 */
public class AppRatingDAO {

    private static final String SQL_UPDATE =
        "UPDATE app_ratings SET note=?, date_creation=CURRENT_TIMESTAMP WHERE utilisateur_id=?";

    private static final String SQL_INSERT =
        "INSERT INTO app_ratings (utilisateur_id, note, date_creation) VALUES (?, ?, CURRENT_TIMESTAMP)";

    public void enregistrerNote(int utilisateurId, int note) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement psUpdate = conn.prepareStatement(SQL_UPDATE)) {

            psUpdate.setInt(1, note);
            psUpdate.setInt(2, utilisateurId);
            int updated = psUpdate.executeUpdate();

            if (updated == 0) {
                try (PreparedStatement psInsert = conn.prepareStatement(SQL_INSERT)) {
                    psInsert.setInt(1, utilisateurId);
                    psInsert.setInt(2, note);
                    psInsert.executeUpdate();
                }
            }
        }
    }
}