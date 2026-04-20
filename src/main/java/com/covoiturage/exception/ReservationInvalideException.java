package com.covoiturage.exception;

/**
 * Lancée lorsqu'une opération sur une réservation est invalide
 * (ex: réserver sur un trajet annulé, dépasser le nombre de places, etc.).
 */
public class ReservationInvalideException extends Exception {

    public ReservationInvalideException(String message) {
        super(message);
    }

    public ReservationInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
