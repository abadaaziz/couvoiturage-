package com.covoiturage.exception;


public class ReservationInvalideException extends Exception {

    public ReservationInvalideException(String message) {
        super(message);
    }

    public ReservationInvalideException(String message, Throwable cause) {
        super(message, cause);
    }
}
