package com.covoiturage.exception;

/**
 * Lancée lorsqu'une tentative d'authentification échoue
 * (email inconnu, mot de passe incorrect, compte non validé, etc.).
 */
public class AuthenticationException extends Exception {

    public enum Raison {
        EMAIL_INCONNU,
        MOT_DE_PASSE_INCORRECT,
        COMPTE_NON_VALIDE,
        COMPTE_BLOQUE,
        SESSION_EXPIREE
    }

    private final Raison raison;

    public AuthenticationException(Raison raison, String message) {
        super(message);
        this.raison = raison;
    }

    public AuthenticationException(String message) {
        super(message);
        this.raison = null;
    }

    public Raison getRaison() {
        return raison;
    }
}
