package com.covoiturage.service;

/**
 * Service de notifications multicanal (email et SMS).
 * <p>
 * Responsabilité unique (SRP) : envoi de notifications aux utilisateurs.
 * </p>
 * <strong>Note :</strong> En production, ce service s'interfacerait avec :
 * <ul>
 *   <li>Un serveur SMTP (via javax.mail) pour les emails</li>
 *   <li>Un prestataire SMS (Twilio, Vonage, etc.) via API HTTP</li>
 * </ul>
 * Ici, l'implémentation est simulée (logs console).
 */
public class NotificationService {

    // ── Configuration simulée ─────────────────────────────────────────────────

    private static final String EXPEDITEUR_EMAIL = "noreply@covoiturageapp.fr";
    private static final String NOM_APP          = "CovoitApp";

    // ── Méthodes publiques ────────────────────────────────────────────────────

    /**
     * Envoie une notification par email.
     * <p>
     * En production : utiliser javax.mail (JavaMail) ou jakarta.mail avec un serveur SMTP.
     * </p>
     *
     * @param destinataire Adresse email du destinataire
     * @param sujet        Sujet de l'email
     * @param corps        Corps du message (texte brut ou HTML)
     */
    public void notifierEmail(String destinataire, String sujet, String corps) {
        if (destinataire == null || destinataire.isBlank()) {
            System.err.println("[NotificationService] Email impossible : destinataire null ou vide.");
            return;
        }
        if (sujet == null || corps == null) {
            System.err.println("[NotificationService] Email impossible : sujet ou corps null.");
            return;
        }

        // Simulation de l'envoi (log formaté)
        System.out.println("══════════════════════════════════════════════════════");
        System.out.println("[EMAIL] De       : " + EXPEDITEUR_EMAIL);
        System.out.println("[EMAIL] À        : " + destinataire);
        System.out.println("[EMAIL] Sujet    : " + sujet);
        System.out.println("[EMAIL] Message  : " + corps);
        System.out.println("══════════════════════════════════════════════════════");

        /*
         * Exemple d'implémentation réelle avec javax.mail :
         *
         * Properties props = new Properties();
         * props.put("mail.smtp.host", "smtp.monserveur.fr");
         * props.put("mail.smtp.port", "587");
         * props.put("mail.smtp.auth", "true");
         * props.put("mail.smtp.starttls.enable", "true");
         *
         * Session session = Session.getInstance(props, new Authenticator() {
         *     protected PasswordAuthentication getPasswordAuthentication() {
         *         return new PasswordAuthentication("user", "password");
         *     }
         * });
         * Message message = new MimeMessage(session);
         * message.setFrom(new InternetAddress(EXPEDITEUR_EMAIL));
         * message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
         * message.setSubject(sujet);
         * message.setText(corps);
         * Transport.send(message);
         */
    }

    /**
     * Envoie une notification par SMS.
     * <p>
     * En production : intégration avec l'API Twilio ou équivalent via java.net.http.HttpClient.
     * </p>
     *
     * @param numeroTelephone Numéro de téléphone au format international (+33...)
     * @param message         Texte du SMS (max 160 caractères pour un SMS simple)
     */
    public void notifierSMS(String numeroTelephone, String message) {
        if (numeroTelephone == null || numeroTelephone.isBlank()) {
            System.err.println("[NotificationService] SMS impossible : numéro null ou vide.");
            return;
        }
        if (message == null || message.isBlank()) {
            System.err.println("[NotificationService] SMS impossible : message null ou vide.");
            return;
        }

        // Tronquage si dépassement de 160 caractères
        String messageSMS = message.length() > 160 ? message.substring(0, 157) + "..." : message;

        // Simulation de l'envoi SMS
        System.out.println("──────────────────────────────────────────────────────");
        System.out.println("[SMS] Expéditeur : " + NOM_APP);
        System.out.println("[SMS] Destinataire : " + numeroTelephone);
        System.out.println("[SMS] Message      : " + messageSMS);
        System.out.println("──────────────────────────────────────────────────────");

        /*
         * Exemple d'appel HTTP vers Twilio (Java 11+ avec HttpClient) :
         *
         * String credentials = Base64.getEncoder().encodeToString(
         *     ("ACCOUNT_SID:AUTH_TOKEN").getBytes());
         * String body = "To=" + URLEncoder.encode(numeroTelephone, "UTF-8") +
         *               "&From=+33XXXXXXX&Body=" + URLEncoder.encode(messageSMS, "UTF-8");
         *
         * HttpRequest request = HttpRequest.newBuilder()
         *     .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/SID/Messages.json"))
         *     .header("Authorization", "Basic " + credentials)
         *     .header("Content-Type", "application/x-www-form-urlencoded")
         *     .POST(HttpRequest.BodyPublishers.ofString(body))
         *     .build();
         *
         * HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
         */
    }

    /**
     * Envoie une notification par email ET SMS simultanément.
     *
     * @param email    Adresse email du destinataire
     * @param telephone Numéro de téléphone du destinataire
     * @param sujet    Sujet de l'email
     * @param message  Message (email et SMS auront le même corps ici)
     */
    public void notifier(String email, String telephone, String sujet, String message) {
        notifierEmail(email, sujet, message);
        notifierSMS(telephone, message);
    }
}
