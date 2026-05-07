package com.covoiturage.exception;


public class TrajetCompletException extends Exception {

    private final int trajetId;

    public TrajetCompletException(int trajetId) {
        super("Le trajet #" + trajetId + " est complet, aucune place disponible.");
        this.trajetId = trajetId;
    }

    public TrajetCompletException(int trajetId, String message) {
        super(message);
        this.trajetId = trajetId;
    }

    public int getTrajetId() {
        return trajetId;
    }
}
