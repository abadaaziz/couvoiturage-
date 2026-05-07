package com.covoiturage.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.covoiturage.model.Admin;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.model.Utilisateur.StatutCompte;
import com.covoiturage.util.DatabaseConnection;
import java.sql.Types;


public class UtilisateurDAO {



    private static final String SQL_INSERT =
        "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, telephone, role, statut_compte, note_moyenne, nombre_avis, date_inscription, tentatives_connexion_echouees) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
        "SELECT * FROM utilisateurs WHERE id = ?";

    private static final String SQL_SELECT_BY_EMAIL =
        "SELECT * FROM utilisateurs WHERE email = ?";

    private static final String SQL_SELECT_ALL =
        "SELECT * FROM utilisateurs ORDER BY date_inscription DESC";

    private static final String SQL_SELECT_BY_STATUT =
        "SELECT * FROM utilisateurs WHERE statut_compte = ? ORDER BY date_inscription DESC";

    private static final String SQL_UPDATE =
        "UPDATE utilisateurs SET nom=?, prenom=?, email=?, telephone=?, role=?, statut_compte=?, " +
        "note_moyenne=?, nombre_avis=?, derniere_connexion=?, tentatives_connexion_echouees=? WHERE id=?";

    private static final String SQL_UPDATE_STATUT =
        "UPDATE utilisateurs SET statut_compte=? WHERE id=?";

    private static final String SQL_UPDATE_CONNEXION =
        "UPDATE utilisateurs SET derniere_connexion=? WHERE id=?";

    private static final String SQL_INCREMENT_TENTATIVES_CONNEXION =
        "UPDATE utilisateurs SET tentatives_connexion_echouees = tentatives_connexion_echouees + 1 WHERE id=?";

    private static final String SQL_RESET_TENTATIVES_CONNEXION =
        "UPDATE utilisateurs SET tentatives_connexion_echouees = 0 WHERE id=?";

    private static final String SQL_SELECT_TENTATIVES_CONNEXION =
        "SELECT tentatives_connexion_echouees FROM utilisateurs WHERE id=?";

    private static final String SQL_DELETE =
        "DELETE FROM utilisateurs WHERE id=?";

    private static final String SQL_EXISTS_EMAIL =
        "SELECT COUNT(*) FROM utilisateurs WHERE email=?";



    
    public Utilisateur inserer(Utilisateur utilisateur) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            remplirStatement(ps, utilisateur);
            ps.executeUpdate();


            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    utilisateur.setId(rs.getInt(1));
                }
            }
        }
        return utilisateur;
    }

    
    public Optional<Utilisateur> trouverParId(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapperResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    
    public Optional<Utilisateur> trouverParEmail(String email) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_EMAIL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapperResultSet(rs));
                }
            }
        }
        return Optional.empty();
    }

    
    public List<Utilisateur> trouverTous() throws SQLException {
        List<Utilisateur> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_ALL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                liste.add(mapperResultSet(rs));
            }
        }
        return liste;
    }

    
    public List<Utilisateur> trouverParStatut(StatutCompte statut) throws SQLException {
        List<Utilisateur> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_STATUT)) {

            ps.setString(1, statut.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(mapperResultSet(rs));
                }
            }
        }
        return liste;
    }

    
    public void mettreAJour(Utilisateur utilisateur) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, utilisateur.getNom());
            ps.setString(2, utilisateur.getPrenom());
            ps.setString(3, utilisateur.getEmail());
            ps.setString(4, utilisateur.getTelephone());
            ps.setString(5, utilisateur.getRole());
            ps.setString(6, utilisateur.getStatutCompte().name());

            if (utilisateur instanceof Chauffeur c) {
                ps.setDouble(7, c.getNoteMoyenne());
                ps.setInt(8, c.getNombreAvis());
            } else {
                ps.setNull(7, Types.DOUBLE);
                ps.setNull(8, Types.INTEGER);
            }
            ps.setTimestamp(9, utilisateur.getDerniereConnexion() != null
                ? Timestamp.valueOf(utilisateur.getDerniereConnexion()) : null);
            ps.setInt(10, utilisateur.getTentativesConnexionEchouees());
            ps.setInt(11, utilisateur.getId());
            ps.executeUpdate();
        }
    }

    
    public void mettreAJourStatut(int utilisateurId, StatutCompte statut) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUT)) {

            ps.setString(1, statut.name());
            ps.setInt(2, utilisateurId);
            ps.executeUpdate();
        }
    }

    
    public void mettreAJourDerniereConnexion(int utilisateurId, LocalDateTime dateConnexion)
            throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_CONNEXION)) {

            ps.setTimestamp(1, Timestamp.valueOf(dateConnexion));
            ps.setInt(2, utilisateurId);
            ps.executeUpdate();
        }
    }

    
    public int incrementerTentativesConnexionEchouees(int utilisateurId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement(SQL_INCREMENT_TENTATIVES_CONNEXION)) {
                ps.setInt(1, utilisateurId);
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(SQL_SELECT_TENTATIVES_CONNEXION)) {
                ps.setInt(1, utilisateurId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("tentatives_connexion_echouees");
                    }
                }
            }
        }
        throw new SQLException("Utilisateur #" + utilisateurId + " introuvable.");
    }

    
    public void reinitialiserTentativesConnexionEchouees(int utilisateurId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_RESET_TENTATIVES_CONNEXION)) {

            ps.setInt(1, utilisateurId);
            ps.executeUpdate();
        }
    }

    
    public void supprimer(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    
    public boolean emailExiste(String email) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_EMAIL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }



    
    private void remplirStatement(PreparedStatement ps, Utilisateur u) throws SQLException {
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasseHash());
        ps.setString(5, u.getTelephone());
        ps.setString(6, u.getRole());
        ps.setString(7, u.getStatutCompte().name());

        if (u instanceof Chauffeur c) {
            ps.setDouble(8, c.getNoteMoyenne());
            ps.setInt(9, c.getNombreAvis());
        } else {
            ps.setNull(8, Types.DOUBLE);
            ps.setNull(9, Types.INTEGER);
        }
        ps.setTimestamp(10, u.getDateInscription() != null
            ? Timestamp.valueOf(u.getDateInscription()) : Timestamp.valueOf(LocalDateTime.now()));
        ps.setInt(11, u.getTentativesConnexionEchouees());
    }

    
    private Utilisateur mapperResultSet(ResultSet rs) throws SQLException {
        Utilisateur u = creerInstanceSelonRole(rs.getString("role"));
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasseHash(rs.getString("mot_de_passe_hash"));
        u.setTelephone(rs.getString("telephone"));
        u.setStatutCompte(StatutCompte.valueOf(rs.getString("statut_compte")));

        if (u instanceof Chauffeur c) {
            c.setNoteMoyenne(rs.getDouble("note_moyenne"));
            c.setNombreAvis(rs.getInt("nombre_avis"));
        }

        Timestamp tsInscription = rs.getTimestamp("date_inscription");
        if (tsInscription != null) u.setDateInscription(tsInscription.toLocalDateTime());

        Timestamp tsConnexion = rs.getTimestamp("derniere_connexion");
        if (tsConnexion != null) u.setDerniereConnexion(tsConnexion.toLocalDateTime());
        u.setTentativesConnexionEchouees(rs.getInt("tentatives_connexion_echouees"));

        return u;
    }

    private Utilisateur creerInstanceSelonRole(String role) throws SQLException {
        if (role == null || role.isBlank()) {
            throw new SQLException("Rôle utilisateur absent en base.");
        }

        return switch (role.toUpperCase()) {
            case "ADMIN" -> new Admin();
            case "CHAUFFEUR" -> new Chauffeur();
            case "PASSAGER" -> new Passager();
            default -> throw new SQLException("Rôle utilisateur inconnu : " + role);
        };
    }
}
