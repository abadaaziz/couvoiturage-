package com.covoiturage.exception;


public class UtilisateurSuspenduException extends Exception {

    private final String email;

    public UtilisateurSuspenduException(String email) {
        super("Le compte de l'utilisateur '" + email + "' est suspendu ou bloqué. " +
              "Contactez l'administration pour plus d'informations.");
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}
