package com.covoiturage.util;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Initialise automatiquement le schéma de la base H2 au démarrage de l'application.
 * Aucune action manuelle n'est requise : les tables sont créées si elles n'existent pas.
 */
@WebListener
public class DatabaseInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[DatabaseInitializer] Initialisation de la base de données H2...");
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {

            // ── Table utilisateurs ──────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS utilisateurs (
                    id                   INT          AUTO_INCREMENT PRIMARY KEY,
                    nom                  VARCHAR(100) NOT NULL,
                    prenom               VARCHAR(100) NOT NULL,
                    email                VARCHAR(255) NOT NULL UNIQUE,
                    mot_de_passe_hash    VARCHAR(255) NOT NULL,
                    telephone            VARCHAR(20),
                    role                 VARCHAR(20)  NOT NULL DEFAULT 'PASSAGER',
                    statut_compte        VARCHAR(40)  NOT NULL DEFAULT 'EN_ATTENTE_VALIDATION',
                    note_moyenne         DOUBLE       NOT NULL DEFAULT 0.0,
                    nombre_avis          INT          NOT NULL DEFAULT 0,
                    date_inscription     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    derniere_connexion   DATETIME
                )
            """);

            // ── Table trajets ───────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS trajets (
                    id                   INT          AUTO_INCREMENT PRIMARY KEY,
                    ville_depart         VARCHAR(100) NOT NULL,
                    ville_arrivee        VARCHAR(100) NOT NULL,
                    date_heure_depart    DATETIME     NOT NULL,
                    places_total         INT          NOT NULL,
                    places_disponibles   INT          NOT NULL,
                    prix_par_place       DOUBLE       NOT NULL,
                    statut               VARCHAR(20)  NOT NULL DEFAULT 'OUVERT',
                    chauffeur_id         INT          NOT NULL,
                    description_vehicule VARCHAR(255),
                    date_creation        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_trajet_chauffeur FOREIGN KEY (chauffeur_id) REFERENCES utilisateurs(id)
                )
            """);

            // ── Table reservations ──────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id                    INT    AUTO_INCREMENT PRIMARY KEY,
                    trajet_id             INT    NOT NULL,
                    passager_id           INT    NOT NULL,
                    nombre_places         INT    NOT NULL DEFAULT 1,
                    montant_total         DOUBLE NOT NULL,
                    statut                VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE',
                    date_reservation      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    date_annulation       DATETIME,
                    montant_rembourse     DOUBLE   NOT NULL DEFAULT 0.0,
                    reference_transaction VARCHAR(100),
                    CONSTRAINT fk_reservation_trajet   FOREIGN KEY (trajet_id)   REFERENCES trajets(id),
                    CONSTRAINT fk_reservation_passager FOREIGN KEY (passager_id) REFERENCES utilisateurs(id)
                )
            """);

            // ── Table paiements ─────────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS paiements (
                    id                   INT    AUTO_INCREMENT PRIMARY KEY,
                    reservation_id       INT    NOT NULL,
                    montant              DOUBLE NOT NULL,
                    montant_rembourse    DOUBLE NOT NULL DEFAULT 0.0,
                    statut               VARCHAR(20) NOT NULL DEFAULT 'AUTORISE',
                    methode              VARCHAR(30) NOT NULL,
                    reference_externe    VARCHAR(100),
                    date_autorisation    DATETIME,
                    date_capture         DATETIME,
                    date_remboursement   DATETIME,
                    message_erreur       VARCHAR(500),
                    CONSTRAINT fk_paiement_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
                )
            """);

            // ── Compte admin par défaut ─────────────────────────────────────
            // Email: admin@covoitapp.com  |  Mot de passe: admin123
            // Hash SHA-256 de "admin123"
            stmt.execute("""
                MERGE INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, telephone, role, statut_compte)
                KEY (email)
                VALUES ('Admin', 'CovoitApp', 'admin@covoitapp.com',
                        'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
                        '0600000000', 'ADMIN', 'ACTIF')
            """);

            // ── Données de démo : quelques trajets ─────────────────────────
            insertDemoDataIfEmpty(stmt);

            System.out.println("[DatabaseInitializer] Base de données prête ✓");

        } catch (SQLException e) {
            System.err.println("[DatabaseInitializer] ERREUR : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Insère des données de démonstration uniquement si la table trajets est vide.
     */
    private void insertDemoDataIfEmpty(Statement stmt) throws SQLException {
        var rs = stmt.executeQuery("SELECT COUNT(*) FROM trajets");
        rs.next();
        if (rs.getInt(1) > 0) return; // données déjà présentes

        // Compte chauffeur de démo
        stmt.execute("""
            MERGE INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, telephone, role, statut_compte, note_moyenne, nombre_avis)
            KEY (email)
            VALUES ('Dupont', 'Jean', 'jean.dupont@demo.com',
                    'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
                    '0612345678', 'CHAUFFEUR', 'ACTIF', 4.5, 12)
        """);

        stmt.execute("""
            MERGE INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, telephone, role, statut_compte, note_moyenne, nombre_avis)
            KEY (email)
            VALUES ('Martin', 'Sophie', 'sophie.martin@demo.com',
                    'a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3',
                    '0698765432', 'CHAUFFEUR', 'ACTIF', 4.8, 25)
        """);

        // Trajets de démo (dates dans le futur)
        stmt.execute("""
            INSERT INTO trajets (ville_depart, ville_arrivee, date_heure_depart,
                                 places_total, places_disponibles, prix_par_place,
                                 statut, chauffeur_id, description_vehicule, date_creation)
            SELECT 'Paris', 'Lyon',
                   DATEADD('DAY', 3, CURRENT_TIMESTAMP),
                   4, 3, 25.0, 'OUVERT',
                   u.id, 'Peugeot 308 Blanche - AB-123-CD', CURRENT_TIMESTAMP
            FROM utilisateurs u WHERE u.email = 'jean.dupont@demo.com'
        """);

        stmt.execute("""
            INSERT INTO trajets (ville_depart, ville_arrivee, date_heure_depart,
                                 places_total, places_disponibles, prix_par_place,
                                 statut, chauffeur_id, description_vehicule, date_creation)
            SELECT 'Marseille', 'Paris',
                   DATEADD('DAY', 5, CURRENT_TIMESTAMP),
                   3, 3, 35.0, 'OUVERT',
                   u.id, 'Renault Clio Grise - EF-456-GH', CURRENT_TIMESTAMP
            FROM utilisateurs u WHERE u.email = 'sophie.martin@demo.com'
        """);

        stmt.execute("""
            INSERT INTO trajets (ville_depart, ville_arrivee, date_heure_depart,
                                 places_total, places_disponibles, prix_par_place,
                                 statut, chauffeur_id, description_vehicule, date_creation)
            SELECT 'Bordeaux', 'Toulouse',
                   DATEADD('DAY', 2, CURRENT_TIMESTAMP),
                   2, 2, 15.0, 'OUVERT',
                   u.id, 'Tesla Model 3 Noire - IJ-789-KL', CURRENT_TIMESTAMP
            FROM utilisateurs u WHERE u.email = 'jean.dupont@demo.com'
        """);

        System.out.println("[DatabaseInitializer] Données de démo insérées ✓");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[DatabaseInitializer] Arrêt de l'application.");
    }
}
