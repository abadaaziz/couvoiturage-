package com.covoiturage.service;

import java.sql.SQLException;
import java.util.List;

import com.covoiturage.dao.NotificationDAO;
import com.covoiturage.model.Notification;

/**
 * Service pour les notifications in-app (stockage en base).
 */
public class InAppNotificationService {

    private final NotificationDAO notificationDAO;

    public InAppNotificationService() {
        this.notificationDAO = new NotificationDAO();
    }

    public InAppNotificationService(NotificationDAO notificationDAO) {
        this.notificationDAO = notificationDAO;
    }

    public void notifierUtilisateur(int utilisateurId, String type, String titre, String message) {
        try {
            Notification notif = new Notification(utilisateurId, type, titre, message);
            notificationDAO.inserer(notif);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la creation de la notification : " + e.getMessage(), e);
        }
    }

    public List<Notification> listerPourUtilisateur(int utilisateurId) {
        try {
            return notificationDAO.listerParUtilisateur(utilisateurId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la lecture des notifications : " + e.getMessage(), e);
        }
    }

    public void marquerCommeLu(int notificationId, int utilisateurId) {
        try {
            notificationDAO.marquerCommeLu(notificationId, utilisateurId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du marquage de la notification : " + e.getMessage(), e);
        }
    }

    public void marquerTousLus(int utilisateurId) {
        try {
            notificationDAO.marquerTousLus(utilisateurId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors du marquage des notifications : " + e.getMessage(), e);
        }
    }

    public boolean supprimer(int notificationId, int utilisateurId) {
        try {
            return notificationDAO.supprimer(notificationId, utilisateurId);
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression de la notification : " + e.getMessage(), e);
        }
    }
}
