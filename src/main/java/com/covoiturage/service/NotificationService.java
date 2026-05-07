package com.covoiturage.service;


public class NotificationService {



    private static final String EXPEDITEUR_EMAIL = "noreply@covoiturageapp.fr";
    private static final String NOM_APP          = "Same Trip";



    
    public void notifierEmail(String destinataire, String sujet, String corps) {
        if (destinataire == null || destinataire.isBlank()) {
            System.err.println("[NotificationService] Email impossible : destinataire null ou vide.");
            return;
        }
        if (sujet == null || corps == null) {
            System.err.println("[NotificationService] Email impossible : sujet ou corps null.");
            return;
        }


        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("[EMAIL] De       : " + EXPEDITEUR_EMAIL);
        System.out.println("[EMAIL] À        : " + destinataire);
        System.out.println("[EMAIL] Sujet    : " + sujet);
        System.out.println("[EMAIL] Message  : " + corps);
        System.out.println("══════════════════════════════════════════════════════");

        
    }

    
    public void notifierSMS(String numeroTelephone, String message) {
        if (numeroTelephone == null || numeroTelephone.isBlank()) {
            System.err.println("[NotificationService] SMS impossible : numéro null ou vide.");
            return;
        }
        if (message == null || message.isBlank()) {
            System.err.println("[NotificationService] SMS impossible : message null ou vide.");
            return;
        }


        String messageSMS = message.length() > 160 ? message.substring(0, 157) + "..." : message;


        System.out.println("──────────────────────────────────────────────────────");
        System.out.println("[SMS] Expéditeur : " + NOM_APP);
        System.out.println("[SMS] Destinataire : " + numeroTelephone);
        System.out.println("[SMS] Message      : " + messageSMS);
        System.out.println("──────────────────────────────────────────────────────");

        
    }

    
    public void notifier(String email, String telephone, String sujet, String message) {
        notifierEmail(email, sujet, message);
        notifierSMS(telephone, message);
    }
}
