-- ============================================================
--  Same Trip — Script de création de la base de données
--  Compatible MySQL 8.0+ / MariaDB 10.5+
--  Exécuter une seule fois avant le premier lancement.
-- ============================================================

-- 1. Créer la base si elle n'existe pas encore
CREATE DATABASE IF NOT EXISTS covoiturage
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE covoiturage;

-- ============================================================
-- TABLE : utilisateurs
-- ============================================================
CREATE TABLE IF NOT EXISTS utilisateurs (
    id                   INT          AUTO_INCREMENT PRIMARY KEY,
    nom                  VARCHAR(100) NOT NULL,
    prenom               VARCHAR(100) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    mot_de_passe_hash    VARCHAR(255) NOT NULL,
    telephone            VARCHAR(20),
    role                 ENUM('PASSAGER','CHAUFFEUR','ADMIN') NOT NULL DEFAULT 'PASSAGER',
    statut_compte        ENUM('ACTIF','SUSPENDU','BLOQUE','EN_ATTENTE_VALIDATION') NOT NULL DEFAULT 'EN_ATTENTE_VALIDATION',
    tentatives_connexion_echouees INT NOT NULL DEFAULT 0,
    note_moyenne         DOUBLE       NULL DEFAULT NULL,  -- uniquement pour les chauffeurs
    nombre_avis          INT          NULL DEFAULT NULL,  -- uniquement pour les chauffeurs
    date_inscription     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    derniere_connexion   DATETIME
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE : trajets
-- ============================================================
CREATE TABLE IF NOT EXISTS trajets (
    id                   INT          AUTO_INCREMENT PRIMARY KEY,
    ville_depart         VARCHAR(100) NOT NULL,
    ville_arrivee        VARCHAR(100) NOT NULL,
    date_heure_depart    DATETIME     NOT NULL,
    places_total         INT          NOT NULL,
    places_disponibles   INT          NOT NULL,
    prix_par_place       DOUBLE       NOT NULL,
    statut               ENUM('OUVERT','COMPLET','ANNULE','TERMINE') NOT NULL DEFAULT 'OUVERT',
    chauffeur_id         INT          NOT NULL,
    description_vehicule VARCHAR(255),
    date_creation        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trajet_chauffeur FOREIGN KEY (chauffeur_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE : reservations
-- ============================================================
CREATE TABLE IF NOT EXISTS reservations (
    id                    INT    AUTO_INCREMENT PRIMARY KEY,
    trajet_id             INT    NOT NULL,
    passager_id           INT    NOT NULL,
    nombre_places         INT    NOT NULL DEFAULT 1,
    montant_total         DOUBLE NOT NULL,
    statut                ENUM('EN_ATTENTE','CONFIRMEE','ANNULEE','REMBOURSEE') NOT NULL DEFAULT 'EN_ATTENTE',
    date_reservation      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_annulation       DATETIME,
    montant_rembourse     DOUBLE   NOT NULL DEFAULT 0.0,
    reference_transaction VARCHAR(100),
    note_passager         TINYINT       NULL CHECK (note_passager BETWEEN 1 AND 5),
    CONSTRAINT fk_reservation_trajet  FOREIGN KEY (trajet_id)   REFERENCES trajets(id),
    CONSTRAINT fk_reservation_passager FOREIGN KEY (passager_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Table des notes de l'application ─────────────────────────────────────

CREATE TABLE IF NOT EXISTS app_ratings (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id INT NOT NULL,
    note           TINYINT NOT NULL CHECK (note BETWEEN 1 AND 5),
    date_creation  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_app_ratings_user FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id) ON DELETE CASCADE,
    UNIQUE KEY uk_app_ratings_user (utilisateur_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE : paiements
-- ============================================================
CREATE TABLE IF NOT EXISTS paiements (
    id                   INT    AUTO_INCREMENT PRIMARY KEY,
    reservation_id       INT    NOT NULL,
    montant              DOUBLE NOT NULL,
    montant_rembourse    DOUBLE NOT NULL DEFAULT 0.0,
    statut               ENUM('AUTORISE','CAPTURE','REMBOURSE','ECHOUE','ANNULE') NOT NULL DEFAULT 'AUTORISE',
    methode              ENUM('CARTE_BANCAIRE','PAYPAL','VIREMENT') NOT NULL,
    reference_externe    VARCHAR(100),
    date_autorisation    DATETIME,
    date_capture         DATETIME,
    date_remboursement   DATETIME,
    message_erreur       VARCHAR(500),
    CONSTRAINT fk_paiement_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE : notifications
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id   INT NOT NULL,
    type             VARCHAR(30)  NOT NULL,
    titre            VARCHAR(200) NOT NULL,
    message          VARCHAR(1000) NOT NULL,
    date_creation    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_lecture     DATETIME,
    CONSTRAINT fk_notification_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- Données de test : un compte admin pour commencer
-- Mot de passe : "admin123"  (hash SHA-256 pour demo)
-- ============================================================
INSERT IGNORE INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, telephone, role, statut_compte)
VALUES ('Admin', 'Same Trip', 'admin@covoitapp.com',
    '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9',
        '0600000000', 'ADMIN', 'ACTIF');
