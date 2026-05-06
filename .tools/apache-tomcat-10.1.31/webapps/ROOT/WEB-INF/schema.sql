-- ============================================================
-- Script de création de la base de données covoiturage
-- SGBD : MySQL 8.x / MariaDB 10.x
-- Encodage : UTF-8 (utf8mb4 pour les emojis)
-- ============================================================

CREATE DATABASE IF NOT EXISTS covoiturage
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE covoiturage;

-- ── Table des utilisateurs ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS utilisateurs (
    id                  INT          AUTO_INCREMENT PRIMARY KEY,
    nom                 VARCHAR(100) NOT NULL,
    prenom              VARCHAR(100) NOT NULL,
    email               VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe_hash   VARCHAR(512) NOT NULL,            -- format "sel:hash"
    telephone           VARCHAR(20),
    role                ENUM('PASSAGER','CHAUFFEUR','ADMIN') NOT NULL DEFAULT 'PASSAGER',
    statut_compte       ENUM('ACTIF','SUSPENDU','BLOQUE','EN_ATTENTE_VALIDATION')
                        NOT NULL DEFAULT 'EN_ATTENTE_VALIDATION',
    tentatives_connexion_echouees INT NOT NULL DEFAULT 0,
    note_moyenne        DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    nombre_avis         INT          NOT NULL DEFAULT 0,
    date_inscription    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    derniere_connexion  DATETIME,
    INDEX idx_email (email),
    INDEX idx_statut (statut_compte),
    INDEX idx_role   (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Table des trajets ─────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS trajets (
    id                   INT           AUTO_INCREMENT PRIMARY KEY,
    ville_depart         VARCHAR(100)  NOT NULL,
    ville_arrivee        VARCHAR(100)  NOT NULL,
    date_heure_depart    DATETIME      NOT NULL,
    places_total         TINYINT       NOT NULL CHECK (places_total BETWEEN 1 AND 8),
    places_disponibles   TINYINT       NOT NULL CHECK (places_disponibles >= 0),
    prix_par_place       DECIMAL(8,2)  NOT NULL CHECK (prix_par_place >= 0),
    statut               ENUM('OUVERT','COMPLET','ANNULE','TERMINE') NOT NULL DEFAULT 'OUVERT',
    chauffeur_id         INT           NOT NULL,
    description_vehicule VARCHAR(500),
    date_creation        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_trajet_chauffeur
        FOREIGN KEY (chauffeur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,

    INDEX idx_ville_depart  (ville_depart),
    INDEX idx_ville_arrivee (ville_arrivee),
    INDEX idx_date_depart   (date_heure_depart),
    INDEX idx_statut_trajet (statut),
    INDEX idx_chauffeur     (chauffeur_id),

    -- Cohérence : places disponibles ne peut pas dépasser le total
    CONSTRAINT chk_places CHECK (places_disponibles <= places_total)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Table des réservations ────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS reservations (
    id                    INT           AUTO_INCREMENT PRIMARY KEY,
    trajet_id             INT           NOT NULL,
    passager_id           INT           NOT NULL,
    nombre_places         TINYINT       NOT NULL DEFAULT 1 CHECK (nombre_places >= 1),
    montant_total         DECIMAL(10,2) NOT NULL CHECK (montant_total >= 0),
    montant_rembourse     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    statut                ENUM('EN_ATTENTE','CONFIRMEE','ANNULEE','REMBOURSEE')
                          NOT NULL DEFAULT 'EN_ATTENTE',
    date_reservation      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_annulation       DATETIME,
    reference_transaction VARCHAR(100),
    note_passager         TINYINT       NULL CHECK (note_passager BETWEEN 1 AND 5),

    CONSTRAINT fk_reservation_trajet
        FOREIGN KEY (trajet_id)   REFERENCES trajets(id)      ON DELETE CASCADE,
    CONSTRAINT fk_reservation_passager
        FOREIGN KEY (passager_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,

    -- Un passager ne peut avoir qu'une seule réservation active par trajet
    UNIQUE KEY uk_passager_trajet_actif (passager_id, trajet_id),

    INDEX idx_trajet_id   (trajet_id),
    INDEX idx_passager_id (passager_id),
    INDEX idx_statut_res  (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Table des notes de l'application ─────────────────────────────────────

CREATE TABLE IF NOT EXISTS app_ratings (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    note           TINYINT NOT NULL CHECK (note BETWEEN 1 AND 5),
    date_creation  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_ratings_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    UNIQUE KEY uk_app_ratings_user (utilisateur_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Table des paiements ───────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS paiements (
    id                    INT           AUTO_INCREMENT PRIMARY KEY,
    reservation_id        INT           NOT NULL,
    montant               DECIMAL(10,2) NOT NULL CHECK (montant > 0),
    montant_rembourse     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    statut                ENUM('AUTORISE','CAPTURE','REMBOURSE','ECHOUE','ANNULE')
                          NOT NULL DEFAULT 'AUTORISE',
    methode               ENUM('CARTE_BANCAIRE','PAYPAL','VIREMENT') NOT NULL DEFAULT 'CARTE_BANCAIRE',
    reference_externe     VARCHAR(100)  UNIQUE,           -- référence du prestataire de paiement
    date_autorisation     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_capture          DATETIME,
    date_remboursement    DATETIME,
    message_erreur        TEXT,

    CONSTRAINT fk_paiement_reservation
        FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE CASCADE,

    INDEX idx_reservation_paiement (reservation_id),
    INDEX idx_reference_ext        (reference_externe),
    INDEX idx_statut_paiement      (statut)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Table des notifications ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS notifications (
    id               INT           AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id   INT           NOT NULL,
    type             VARCHAR(30)   NOT NULL,
    titre            VARCHAR(200)  NOT NULL,
    message          VARCHAR(1000) NOT NULL,
    date_creation    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_lecture     DATETIME,

    CONSTRAINT fk_notification_utilisateur
        FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,

    INDEX idx_notification_user (utilisateur_id),
    INDEX idx_notification_date (date_creation)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Données initiales (admin par défaut) ──────────────────────────────────────
-- Mot de passe : Admin1234! (à changer en production !)
-- Hash généré avec SHA-256 + sel (classe PasswordUtils)

INSERT IGNORE INTO utilisateurs
    (nom, prenom, email, mot_de_passe_hash, telephone, role, statut_compte)
VALUES
    ('Admin', 'Système', 'admin@covoiturageapp.fr',
     '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
     '+33600000000', 'ADMIN', 'ACTIF');

-- ── Vue pratique : trajets disponibles ───────────────────────────────────────

CREATE OR REPLACE VIEW v_trajets_disponibles AS
    SELECT
        t.id,
        t.ville_depart,
        t.ville_arrivee,
        t.date_heure_depart,
        t.places_disponibles,
        t.prix_par_place,
        t.statut,
        t.description_vehicule,
        u.nom          AS chauffeur_nom,
        u.prenom       AS chauffeur_prenom,
        u.note_moyenne AS chauffeur_note
    FROM trajets t
    JOIN utilisateurs u ON t.chauffeur_id = u.id
    WHERE t.statut IN ('OUVERT', 'COMPLET')
      AND t.date_heure_depart > NOW()
    ORDER BY t.date_heure_depart ASC;
