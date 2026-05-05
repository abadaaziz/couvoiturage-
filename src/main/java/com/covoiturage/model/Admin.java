package com.covoiturage.model;

/**
 * Sous-classe de Utilisateur représentant un administrateur.
 */
public class Admin extends Utilisateur {

    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
    }

    public Admin(String nom, String prenom, String email, String motDePasseHash, String telephone) {
        super(nom, prenom, email, motDePasseHash, telephone);
    }

    @Override
    public String getRole() {
        return "ADMIN";
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id=" + getId() +
                ", nom='" + getNom() + '\'' +
                ", prenom='" + getPrenom() + '\'' +
                ", email='" + getEmail() + '\'' +
                '}';
    }
}
