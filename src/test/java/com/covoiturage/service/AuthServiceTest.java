package com.covoiturage.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.covoiturage.dao.UtilisateurDAO;
import com.covoiturage.exception.AuthenticationException;
import com.covoiturage.exception.UtilisateurSuspenduException;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.model.Utilisateur.StatutCompte;
import com.covoiturage.util.PasswordUtils;

class AuthServiceTest {

    @Test
    void cinqMotsDePasseIncorrectsSuspendentLeCompte() throws Exception {
        Passager utilisateur = utilisateurActif();
        FakeUtilisateurDAO utilisateurDAO = new FakeUtilisateurDAO(utilisateur);
        AuthService service = new AuthService(utilisateurDAO);

        for (int i = 1; i <= 4; i++) {
            assertThrows(AuthenticationException.class,
                () -> service.authentifier(utilisateur.getEmail(), "mauvais-mdp"));
            assertEquals(i, utilisateur.getTentativesConnexionEchouees());
            assertEquals(StatutCompte.ACTIF, utilisateur.getStatutCompte());
        }

        AuthenticationException erreur = assertThrows(AuthenticationException.class,
            () -> service.authentifier(utilisateur.getEmail(), "mauvais-mdp"));

        assertEquals(AuthenticationException.Raison.COMPTE_BLOQUE, erreur.getRaison());
        assertEquals(5, utilisateur.getTentativesConnexionEchouees());
        assertEquals(StatutCompte.SUSPENDU, utilisateur.getStatutCompte());
        assertThrows(UtilisateurSuspenduException.class,
            () -> service.authentifier(utilisateur.getEmail(), "motdepasse"));
    }

    @Test
    void connexionReussieReinitialiseLesTentatives() throws Exception {
        Passager utilisateur = utilisateurActif();
        utilisateur.setTentativesConnexionEchouees(3);
        FakeUtilisateurDAO utilisateurDAO = new FakeUtilisateurDAO(utilisateur);
        AuthService service = new AuthService(utilisateurDAO);

        Utilisateur connecte = service.authentifier(utilisateur.getEmail(), "motdepasse");

        assertEquals(utilisateur, connecte);
        assertEquals(0, utilisateur.getTentativesConnexionEchouees());
        assertEquals(StatutCompte.ACTIF, utilisateur.getStatutCompte());
    }

    private static Passager utilisateurActif() {
        Passager utilisateur = new Passager();
        utilisateur.setId(42);
        utilisateur.setNom("Test");
        utilisateur.setPrenom("User");
        utilisateur.setEmail("user@example.com");
        utilisateur.setTelephone("0600000000");
        utilisateur.setMotDePasseHash(PasswordUtils.hacher("motdepasse"));
        utilisateur.setStatutCompte(StatutCompte.ACTIF);
        return utilisateur;
    }

    private static final class FakeUtilisateurDAO extends UtilisateurDAO {
        private final Utilisateur utilisateur;

        private FakeUtilisateurDAO(Utilisateur utilisateur) {
            this.utilisateur = utilisateur;
        }

        @Override
        public Optional<Utilisateur> trouverParEmail(String email) {
            return utilisateur.getEmail().equals(email)
                ? Optional.of(utilisateur)
                : Optional.empty();
        }

        @Override
        public int incrementerTentativesConnexionEchouees(int utilisateurId) {
            utilisateur.setTentativesConnexionEchouees(
                utilisateur.getTentativesConnexionEchouees() + 1);
            return utilisateur.getTentativesConnexionEchouees();
        }

        @Override
        public void reinitialiserTentativesConnexionEchouees(int utilisateurId) {
            utilisateur.setTentativesConnexionEchouees(0);
        }

        @Override
        public void mettreAJourStatut(int utilisateurId, StatutCompte statut) {
            utilisateur.setStatutCompte(statut);
        }

        @Override
        public void mettreAJourDerniereConnexion(int utilisateurId, LocalDateTime dateConnexion)
                throws SQLException {
            utilisateur.setDerniereConnexion(dateConnexion);
        }
    }
}
