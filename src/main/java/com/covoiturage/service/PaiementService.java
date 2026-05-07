package com.covoiturage.service;

import com.covoiturage.dao.PaiementDAO;
import com.covoiturage.exception.PaiementEcheException;
import com.covoiturage.model.Paiement;
import com.covoiturage.model.Paiement.MethodePaiement;
import com.covoiturage.model.Paiement.StatutPaiement;
import com.covoiturage.model.Reservation;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;


public class PaiementService {

    private final PaiementDAO paiementDAO;

    public PaiementService() {
        this.paiementDAO = new PaiementDAO();
    }

    public PaiementService(PaiementDAO paiementDAO) {
        this.paiementDAO = paiementDAO;
    }



    
    public String autoriser(Reservation reservation, double montant, MethodePaiement methode)
            throws PaiementEcheException {
        if (montant <= 0) {
            throw new PaiementEcheException("Le montant à autoriser doit être positif : " + montant);
        }


        String referenceExterne = "AUTH-" + UUID.randomUUID().toString().toUpperCase().substring(0, 12);

        try {


            Paiement paiement = new Paiement(reservation, montant, methode, referenceExterne);
            paiementDAO.inserer(paiement);

            System.out.println("[PaiementService] Autorisation OK — réf. " + referenceExterne +
                               " — montant " + montant + " DT");
            return referenceExterne;

        } catch (SQLException e) {
            throw new PaiementEcheException(
                "Erreur lors de l'autorisation du paiement : " + e.getMessage(), e);
        }
    }

    
    public void capturer(String referenceTransaction) throws PaiementEcheException {
        if (referenceTransaction == null || referenceTransaction.isBlank()) {
            throw new PaiementEcheException("Référence de transaction invalide ou nulle.");
        }

        try {



            System.out.println("[PaiementService] Capture du paiement — réf. " + referenceTransaction);




            System.out.println("[PaiementService] Capture OK — réf. " + referenceTransaction);

        } catch (Exception e) {
            throw new PaiementEcheException(
                "Erreur lors de la capture du paiement " + referenceTransaction + " : " + e.getMessage(),
                referenceTransaction);
        }
    }

    
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
            persisterRemboursement(referenceTransaction, montantARembourser);

            System.out.println("[PaiementService] Remboursement de " + montantARembourser +
                               " DT pour transaction " + referenceTransaction);


            System.out.println("[PaiementService] Remboursement OK — " +
                               String.format("%.2f", montantARembourser) + " DT restitués.");

        } catch (Exception e) {
            throw new PaiementEcheException(
                "Erreur lors du remboursement pour " + referenceTransaction + " : " + e.getMessage(),
                referenceTransaction);
        }
    }

    
    private void persisterRemboursement(String referenceTransaction, double montantARembourser)
            throws SQLException {
        if (referenceTransaction == null || referenceTransaction.isBlank()) {
            return;
        }

        var paiement = paiementDAO.trouverParReferenceExterne(referenceTransaction);
        if (paiement.isEmpty()) {
            return;
        }

        Paiement p = paiement.get();
        p.setStatut(StatutPaiement.REMBOURSE);
        p.setMontantRembourse(montantARembourser);
        p.setDateRemboursement(LocalDateTime.now());
        paiementDAO.mettreAJourStatut(p);
    }

    public String payer(Reservation reservation, double montant, MethodePaiement methode)
            throws PaiementEcheException {

        String reference = autoriser(reservation, montant, methode);

        capturer(reference);
        return reference;
    }
}
