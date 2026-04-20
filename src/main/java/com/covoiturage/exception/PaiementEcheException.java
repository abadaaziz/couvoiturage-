package com.covoiturage.exception;

/**
 * Lancée lorsqu'un paiement échoue ou qu'une opération financière est impossible.
 */
public class PaiementEcheException extends Exception {

    private final String referenceTransaction;

    public PaiementEcheException(String message) {
        super(message);
        this.referenceTransaction = null;
    }

    public PaiementEcheException(String message, String referenceTransaction) {
        super(message);
        this.referenceTransaction = referenceTransaction;
    }

    public PaiementEcheException(String message, Throwable cause) {
        super(message, cause);
        this.referenceTransaction = null;
    }

    public String getReferenceTransaction() {
        return referenceTransaction;
    }
}
