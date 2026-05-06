package com.covoiturage.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

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
                    tentatives_connexion_echouees INT NOT NULL DEFAULT 0,
                    note_moyenne         DOUBLE       NOT NULL DEFAULT 0.0,
                    nombre_avis          INT          NOT NULL DEFAULT 0,
                    date_inscription     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    derniere_connexion   DATETIME
                )
            """);
            stmt.execute("""
                ALTER TABLE utilisateurs
                ADD COLUMN IF NOT EXISTS tentatives_connexion_echouees INT NOT NULL DEFAULT 0
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
                    note_passager         INT,
                    CONSTRAINT fk_reservation_trajet   FOREIGN KEY (trajet_id)   REFERENCES trajets(id),
                    CONSTRAINT fk_reservation_passager FOREIGN KEY (passager_id) REFERENCES utilisateurs(id)
                )
            """);

            try {
                stmt.execute("ALTER TABLE reservations ADD COLUMN IF NOT EXISTS note_passager INT");
            } catch (SQLException e) {
                // Ignore if not supported
            }

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

            // ── Table notifications ────────────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notifications (
                    id               INT AUTO_INCREMENT PRIMARY KEY,
                    utilisateur_id   INT NOT NULL,
                    type             VARCHAR(30)  NOT NULL,
                    titre            VARCHAR(200) NOT NULL,
                    message          VARCHAR(1000) NOT NULL,
                    date_creation    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    date_lecture     DATETIME,
                    CONSTRAINT fk_notification_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
                )
            """);

            // ── Table notes application ───────────────────────────────────
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_ratings (
                    id             INT AUTO_INCREMENT PRIMARY KEY,
                    utilisateur_id INT NOT NULL,
                    note           INT NOT NULL,
                    date_creation  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_app_ratings_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id),
                    CONSTRAINT uk_app_ratings_user UNIQUE (utilisateur_id)
                )
            """);

            // ── Compte admin par défaut ─────────────────────────────────────
            // Email: admin@covoitapp.com  |  Mot de passe: admin123
            // Hash SHA-256 (legacy) de "admin123"
            stmt.execute("""
                MERGE INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, telephone, role, statut_compte)
                KEY (email)
                VALUES ('Admin', 'Same Trip', 'admin@covoitapp.com',
                        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
                        '0600000000', 'ADMIN', 'ACTIF')
            """);

            // Nettoyage des anciens comptes de demo Jean/Sophie sans creer de trajets.
            supprimerUtilisateursDemoJeanEtSophie(stmt);

            System.out.println("[DatabaseInitializer] Base de données prête ✓");

        } catch (SQLException e) {
            System.err.println("[DatabaseInitializer] ERREUR : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Supprime les comptes de demo Jean/Sophie et toutes leurs donnees liees.
     */
    private void supprimerUtilisateursDemoJeanEtSophie(Statement stmt) throws SQLException {
        stmt.execute("""
            DELETE FROM paiements WHERE reservation_id IN (
                SELECT id FROM reservations
                WHERE passager_id IN (
                    SELECT id FROM utilisateurs
                    WHERE email IN ('jean.dupont@demo.com', 'sophie.martin@demo.com')
                )
                OR trajet_id IN (
                    SELECT id FROM trajets
                    WHERE chauffeur_id IN (
                        SELECT id FROM utilisateurs
                        WHERE email IN ('jean.dupont@demo.com', 'sophie.martin@demo.com')
                    )
                )
            )
        """);
        stmt.execute("""
            DELETE FROM reservations
            WHERE passager_id IN (
                SELECT id FROM utilisateurs
                WHERE email IN ('jean.dupont@demo.com', 'sophie.martin@demo.com')
            )
            OR trajet_id IN (
                SELECT id FROM trajets
                WHERE chauffeur_id IN (
                    SELECT id FROM utilisateurs
                    WHERE email IN ('jean.dupont@demo.com', 'sophie.martin@demo.com')
                )
            )
        """);
        stmt.execute("""
            DELETE FROM notifications WHERE utilisateur_id IN (
                SELECT id FROM utilisateurs
                WHERE email IN ('jean.dupont@demo.com', 'sophie.martin@demo.com')
            )
        """);
        stmt.execute("""
            DELETE FROM app_ratings WHERE utilisateur_id IN (
                SELECT id FROM utilisateurs
                WHERE email IN ('jean.dupont@demo.com', 'sophie.martin@demo.com')
            )
        """);
        stmt.execute("""
            DELETE FROM trajets WHERE chauffeur_id IN (
                SELECT id FROM utilisateurs
                WHERE email IN ('jean.dupont@demo.com', 'sophie.martin@demo.com')
            )
        """);
        stmt.execute("""
            DELETE FROM utilisateurs
            WHERE email IN ('jean.dupont@demo.com', 'sophie.martin@demo.com')
        """);
        System.out.println("[DatabaseInitializer] Comptes de demo Jean/Sophie supprimes.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[DatabaseInitializer] Arrêt de l'application.");
    }
}
