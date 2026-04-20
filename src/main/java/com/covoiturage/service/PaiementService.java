package com.covoiturage.service;

import com.covoiturage.dao.PaiementDAO;
import com.covoiturage.exception.PaiementEcheException;
import com.covoiturage.model.Paiement;
import com.covoiturage.model.Paiement.MethodePaiement;
import com.covoiturage.model.Paiement.StatutPaiement;
import com.covoiturage.model.Reservation;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Service de gestion des paiements.
 * <p>
 * Responsabilité unique (SRP) : toutes les opérations financières.
 * Suit le cycle : AUTORISATION → CAPTURE → (REMBOURSEMENT si annulation).
 * </p>
 * <strong>Note :</strong> En production, ce service appellerait l'API d'un prestataire
 * (Stripe, PayPal, etc.). Ici, la logique est simulée en Java pur.
 */
public class PaiementService {

    private final PaiementDAO paiementDAO;

    public PaiementService() {
        this.paiementDAO = new PaiementDAO();
    }

    public PaiementService(PaiementDAO paiementDAO) {
        this.paiementDAO = paiementDAO;
    }

    // ── Méthodes publiques ────────────────────────────────────────────────────

    /**
     * Autorise un paiement : bloque les fonds immédiatement.
     * La capture interviendra à la confirmation du chauffeur.
     *
     * @param reservation Réservation associée au paiement
     * @param montant     Montant à bloquer en euros
     * @param methode     Méthode de paiement
     * @return Référence unique de la transaction (pour la capture ultérieure)
     * @throws PaiementEcheException si l'autorisation échoue
     */
    public String autoriser(Reservation reservation, double montant, MethodePaiement methode)
            throws PaiementEcheException {
        if (montant <= 0) {
            throw new PaiementEcheException("Le montant à autoriser doit être positif : " + montant);
        }

        // Génération d'une référence unique (en prod : appel API prestataire)
        String referenceExterne = "AUTH-" + UUID.randomUUID().toString().toUpperCase().substring(0, 12);

        try {
            // Simulation de l'autorisation (toujours réussie ici)
            // En production : appel HTTP vers Stripe/PayPal et vérification du retour
            Paiement paiement = new Paiement(reservation, montant, methode, referenceExterne);
            paiementDAO.inserer(paiement);

            System.out.println("[PaiementService] Autorisation OK — réf. " + referenceExterne +
                               " — montant " + montant + "€");
            return referenceExterne;

        } catch (SQLException e) {
            throw new PaiementEcheException(
                "Erreur lors de l'autorisation du paiement : " + e.getMessage(), e);
        }
    }

    /**
     * Capture un paiement préalablement autorisé (débit réel du client).
     * Appelée lors de la confirmation de la réservation par le chauffeur.
     *
     * @param referenceTransaction Référence retournée par {@link #autoriser}
     * @throws PaiementEcheException si la capture échoue ou si la référence est inconnue
     */
    public void capturer(String referenceTransaction) throws PaiementEcheException {
        if (referenceTransaction == null || referenceTransaction.isBlank()) {
            throw new PaiementEcheException("Référence de transaction invalide ou nulle.");
        }

        try {
            // Recherche du paiement par référence
            // Note : en production, on rechercherait directement par referenceExterne
            // Ici on simule la capture
            System.out.println("[PaiementService] Capture du paiement — réf. " + referenceTransaction);

            // En production : appel API prestataire pour capturer les fonds bloqués
            // Mise à jour en base via le DAO (recherche par référence puis update)
            // Simulation : on log uniquement
            System.out.println("[PaiementService] Capture OK — réf. " + referenceTransaction);

        } catch (Exception e) {
            throw new PaiementEcheException(
                "Erreur lors de la capture du paiement " + referenceTransaction + " : " + e.getMessage(),
                referenceTransaction);
        }
    }

    /**
     * Rembourse un paiement (total ou partiel).
     * Appelée lors d'une annulation de réservation.
     *
     * @param referenceTransaction  Référence de la transaction originale
     * @param montantARembourser    Montant à rembourser (≤ montant initial)
     * @throws PaiementEcheException si le remboursement échoue
     */
    public void rembourser(String referenceTransaction, double montantARembourser)
            throws PaiementEcheException {
        if (montantARembourser < 0) {
            throw new PaiementEcheException("Le montant de remboursement ne peut pas être négatif.");
        }
        if (montantARembourser == 0) {
            System.out.println("[PaiementService] Remboursement nul — aucune action.");
            return;
        }

        try {
            // En production : appel API prestataire pour émettre le remboursement
            System.out.println("[PaiementService] Remboursement de " + montantARembourser +
                               "€ pour transaction " + referenceTransaction);

            // Simulation : succès garanti
            System.out.println("[PaiementService] Remboursement OK — " +
                               String.format("%.2f", montantARembourser) + "€ restitués.");

        } catch (Exception e) {
            throw new PaiementEcheException(
                "Erreur lors du remboursement pour " + referenceTransaction + " : " + e.getMessage(),
                referenceTransaction);
        }
    }

    /**
     * Effectue un paiement direct (autorisation + capture immédiate).
     * Usage : paiement sans confirmation chauffeur requise.
     *
     * @param reservation Réservation à payer
     * @param montant     Montant total
     * @param methode     Méthode de paiement
     * @return Référence de la transaction
     * @throws PaiementEcheException si le paiement échoue
     */
    public String payer(Reservation reservation, double montant, MethodePaiement methode)
            throws PaiementEcheException {
        // Autorisation immédiate
        String reference = autoriser(reservation, montant, methode);
        // Capture immédiate (pas d'attente de confirmation)
        capturer(reference);
        return reference;
    }
}
