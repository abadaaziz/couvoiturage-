package com.covoiturage.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;


public final class PasswordUtils {

    private static final String ALGORITHM  = "SHA-256";
    private static final int    SEL_OCTETS = 16;

    
    private PasswordUtils() { }

    
    public static String hacher(String motDePasse) {
        try {

            SecureRandom random = new SecureRandom();
            byte[] sel = new byte[SEL_OCTETS];
            random.nextBytes(sel);


            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            digest.update(sel);
            byte[] hash = digest.digest(motDePasse.getBytes(java.nio.charset.StandardCharsets.UTF_8));


            String selBase64  = Base64.getEncoder().encodeToString(sel);
            String hashBase64 = Base64.getEncoder().encodeToString(hash);
            return selBase64 + ":" + hashBase64;

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithme de hachage indisponible : " + ALGORITHM, e);
        }
    }

    
    public static boolean verifier(String motDePasse, String hashStocke) {
        try {
            if (hashStocke == null || hashStocke.isBlank()) {
                return false;
            }


            String[] parties   = hashStocke.split(":");
            if (parties.length == 2) {
                byte[] sel         = Base64.getDecoder().decode(parties[0]);
                byte[] hashAttendu = Base64.getDecoder().decode(parties[1]);

                MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
                digest.update(sel);
                byte[] hashCalcule = digest.digest(
                    motDePasse.getBytes(java.nio.charset.StandardCharsets.UTF_8));


                return MessageDigest.isEqual(hashCalcule, hashAttendu);
            }


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
