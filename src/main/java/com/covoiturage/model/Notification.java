package com.covoiturage.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Notification interne (in-app) pour un utilisateur.
 */
public class Notification implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int utilisateurId;
    private String type;
    private String titre;
    private String message;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLecture;

    public Notification() {
        this.dateCreation = LocalDateTime.now();
    }

    public Notification(int utilisateurId, String type, String titre, String message) {
        this.utilisateurId = utilisateurId;
        this.type = Objects.requireNonNull(type, "type");
        this.titre = Objects.requireNonNull(titre, "titre");
        this.message = Objects.requireNonNull(message, "message");
        this.dateCreation = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(int utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = Objects.requireNonNull(type); }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = Objects.requireNonNull(titre); }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = Objects.requireNonNull(message); }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public LocalDateTime getDateLecture() { return dateLecture; }
    public void setDateLecture(LocalDateTime dateLecture) { this.dateLecture = dateLecture; }

    public boolean estLue() {
        return dateLecture != null;
    }
}
