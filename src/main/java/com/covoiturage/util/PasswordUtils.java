package com.covoiturage.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitaire de hachage des mots de passe.
 * <p>
 * Utilise SHA-256 + sel aléatoire (16 octets).
 * Format stocké : BASE64(sel) + ":" + BASE64(hash(sel + motDePasse))
 * </p>
 * <strong>Note :</strong> En production, préférer bcrypt (via une bibliothèque externe).
 * Ici nous respectons la contrainte Java pur sans dépendances.
 */
public final class PasswordUtils {

    private static final String ALGORITHM  = "SHA-256";
    private static final int    SEL_OCTETS = 16;

    /** Constructeur privé — classe utilitaire, non instanciable */
    private PasswordUtils() { }

    /**
     * Hache un mot de passe avec un sel aléatoire.
     *
     * @param motDePasse Mot de passe en clair
     * @return Chaîne "sel:hash" encodée en Base64
     */
    public static String hacher(String motDePasse) {
        try {
            // Génération d'un sel aléatoire
            SecureRandom random = new SecureRandom();
            byte[] sel = new byte[SEL_OCTETS];
            random.nextBytes(sel);

            // Hachage SHA-256 : sel + motDePasse
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(sel);
            byte[] hash = digest.digest(motDePasse.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Encodage Base64 pour le stockage
            String selBase64  = Base64.getEncoder().encodeToString(sel);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);
            return selBase64 + ":" + hashBase64;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme de hachage indisponible : " + ALGORITHM, e);
        }
    }

    /**
     * Vérifie si un mot de passe en clair correspond au hash stocké.
     *
     * @param motDePasse Mot de passe saisi par l'utilisateur
     * @param hashStocke Hash au format "sel:hash" issu de {@link #hacher(String)}
     * @return {@code true} si les mots de passe correspondent
     */
    public static boolean verifier(String motDePasse, String hashStocke) {
        try {
            if (hashStocke == null || hashStocke.isBlank()) {
                return false;
            }

            // Format actuel: BASE64(sel):BASE64(hash)
            String[] parties   = hashStocke.split(":");
            if (parties.length == 2) {
                byte[] sel         = Base64.getDecoder().decode(parties[0]);
                byte[] hashAttendu = Base64.getDecoder().decode(parties[1]);

                MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
                digest.update(sel);
                byte[] hashCalcule = digest.digest(
                    motDePasse.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                // Comparaison constante en temps pour éviter les attaques timing
                return MessageDigest.isEqual(hashCalcule, hashAttendu);
            }

            // Format legacy: SHA-256 hex du mot de passe sans sel
            MessageDigest legacyDigest = MessageDigest.getInstance(ALGORITHM);
            byte[] legacyHash = legacyDigest.digest(
                motDePasse.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String legacyHex = bytesVersHex(legacyHash);

            return MessageDigest.isEqual(
                legacyHex.toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                hashStocke.trim().toLowerCase().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );

        } catch (Exception e) {
            return false;
        }
    }

    private static String bytesVersHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
