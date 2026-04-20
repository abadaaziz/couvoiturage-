package com.covoiturage.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestionnaire de connexions JDBC — Singleton.
 * <p>
 * Configure la connexion vers la base de données.
 * Modifiez les constantes ci-dessous selon votre environnement.
 * </p>
 * Aucune dépendance externe : java.sql.* pur.
 */
public final class DatabaseConnection {

    // ── Configuration ─────────────────────────────────────────────────────────

    /**
     * Base H2 embarquée — fichier stocké dans le répertoire de travail courant.
     * MODE=MySQL assure la compatibilité maximale avec le SQL MySQL utilisé dans les DAO.
     * CASE_INSENSITIVE_IDENTIFIERS=TRUE évite les problèmes de casse.
     */
    private static final String JDBC_URL    = "jdbc:h2:file:./covoiturage_db;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1";
    private static final String JDBC_USER   = "sa";
    private static final String JDBC_PASSWORD = "";
    private static final String JDBC_DRIVER = "org.h2.Driver";

    // ── Singleton (thread-safe via initialisation statique) ───────────────────

    private static DatabaseConnection instance;

    /** Constructeur privé — empêche l'instanciation directe */
    private DatabaseConnection() {
        try {
            // Chargement explicite du driver (requis pour certains conteneurs)
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(
                "Driver JDBC introuvable : " + JDBC_DRIVER + " — " + e.getMessage());
        }
    }

    /**
     * Retourne l'instance unique du gestionnaire (double-checked locking).
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    // ── Méthodes publiques ────────────────────────────────────────────────────

    /**
     * Ouvre et retourne une nouvelle connexion JDBC.
     * L'appelant est responsable de la fermeture (try-with-resources recommandé).
     *
     * @return Connexion JDBC active
     * @throws SQLException si la connexion échoue
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
    }

    /**
     * Ferme proprement une connexion (null-safe).
     *
     * @param connection Connexion à fermer (peut être null)
     */
    public static void fermerConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("[DatabaseConnection] Erreur lors de la fermeture : " + e.getMessage());
            }
        }
    }
}
