package com.covoiturage.dao;

import com.covoiturage.model.Utilisateur;
import com.covoiturage.model.Utilisateur.Role;
import com.covoiturage.model.Utilisateur.StatutCompte;
import com.covoiturage.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO (Data Access Object) pour l'entité {@link Utilisateur}.
 * Toutes les requêtes utilisent des PreparedStatement pour prévenir les injections SQL.
 */
public class UtilisateurDAO {

    // ── Requêtes SQL ──────────────────────────────────────────────────────────

    private static final String SQL_INSERT =
        "INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe_hash, telephone, role, statut_compte, note_moyenne, nombre_avis, date_inscription) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
        "SELECT * FROM utilisateurs WHERE id = ?";

    private static final String SQL_SELECT_BY_EMAIL =
        "SELECT * FROM utilisateurs WHERE email = ?";

    private static final String SQL_SELECT_ALL =
        "SELECT * FROM utilisateurs ORDER BY date_inscription DESC";

    private static final String SQL_UPDATE =
        "UPDATE utilisateurs SET nom=?, prenom=?, email=?, telephone=?, role=?, statut_compte=?, " +
        "note_moyenne=?, nombre_avis=?, derniere_connexion=? WHERE id=?";

    private static final String SQL_UPDATE_STATUT =
        "UPDATE utilisateurs SET statut_compte=? WHERE id=?";

    private static final String SQL_UPDATE_CONNEXION =
        "UPDATE utilisateurs SET derniere_connexion=? WHERE id=?";

    private static final String SQL_DELETE =
        "DELETE FROM utilisateurs WHERE id=?";

    private static final String SQL_EXISTS_EMAIL =
        "SELECT COUNT(*) FROM utilisateurs WHERE email=?";

    // ── Méthodes CRUD ─────────────────────────────────────────────────────────

    /**
     * Insère un nouvel utilisateur en base et met à jour son id généré.
     *
     * @param utilisateur Utilisateur à persister (id ignoré en entrée)
     * @return Utilisateur avec l'id généré par la base
     * @throws SQLException en cas d'erreur base de données
     */
    public Utilisateur inserer(Utilisateur utilisateur) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            remplirStatement(ps, utilisateur);
            ps.executeUpdate();

            // Récupération de l'id auto-généré
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    utilisateur.setId(rs.getInt(1));
                }
            }
        }
        return utilisateur;
    }

    /**
     * Recherche un utilisateur par son identifiant technique.
     *
     * @param id Identifiant de l'utilisateur
     * @return Optional vide si non trouvé
     */
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

    /**
     * Recherche un utilisateur par son adresse email (identifiant métier unique).
     *
     * @param email Adresse email
     * @return Optional vide si non trouvé
     */
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

    /**
     * Retourne tous les utilisateurs (usage admin).
     */
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

    /**
     * Met à jour toutes les informations d'un utilisateur existant.
     *
     * @param utilisateur Utilisateur avec les nouvelles valeurs (id requis)
     * @throws SQLException en cas d'erreur base de données
     */
    public void mettreAJour(Utilisateur utilisateur) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE)) {

            ps.setString(1, utilisateur.getNom());
            ps.setString(2, utilisateur.getPrenom());
            ps.setString(3, utilisateur.getEmail());
            ps.setString(4, utilisateur.getTelephone());
            ps.setString(5, utilisateur.getRole().name());
            ps.setString(6, utilisateur.getStatutCompte().name());
            ps.setDouble(7, utilisateur.getNoteMoyenne());
            ps.setInt(8, utilisateur.getNombreAvis());
            ps.setTimestamp(9, utilisateur.getDerniereConnexion() != null
                ? Timestamp.valueOf(utilisateur.getDerniereConnexion()) : null);
            ps.setInt(10, utilisateur.getId());
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour uniquement le statut du compte (suspension, blocage, etc.).
     */
    public void mettreAJourStatut(int utilisateurId, StatutCompte statut) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUT)) {

            ps.setString(1, statut.name());
            ps.setInt(2, utilisateurId);
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour la date de dernière connexion.
     */
    public void mettreAJourDerniereConnexion(int utilisateurId, LocalDateTime dateConnexion)
            throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_CONNEXION)) {

            ps.setTimestamp(1, Timestamp.valueOf(dateConnexion));
            ps.setInt(2, utilisateurId);
            ps.executeUpdate();
        }
    }

    /**
     * Supprime un utilisateur de la base de données.
     */
    public void supprimer(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_DELETE)) {

            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Vérifie si un email est déjà enregistré en base.
     */
    public boolean emailExiste(String email) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_EXISTS_EMAIL)) {

            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ── Méthodes privées ──────────────────────────────────────────────────────

    /**
     * Renseigne les paramètres du PreparedStatement d'insertion.
     */
    private void remplirStatement(PreparedStatement ps, Utilisateur u) throws SQLException {
        ps.setString(1, u.getNom());
        ps.setString(2, u.getPrenom());
        ps.setString(3, u.getEmail());
        ps.setString(4, u.getMotDePasseHash());
        ps.setString(5, u.getTelephone());
        ps.setString(6, u.getRole().name());
        ps.setString(7, u.getStatutCompte().name());
        ps.setDouble(8, u.getNoteMoyenne());
        ps.setInt(9, u.getNombreAvis());
        ps.setTimestamp(10, u.getDateInscription() != null
            ? Timestamp.valueOf(u.getDateInscription()) : Timestamp.valueOf(LocalDateTime.now()));
    }

    /**
     * Mappe un ResultSet vers un objet {@link Utilisateur}.
     */
    private Utilisateur mapperResultSet(ResultSet rs) throws SQLException {
        Utilisateur u = new Utilisateur();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setPrenom(rs.getString("prenom"));
        u.setEmail(rs.getString("email"));
        u.setMotDePasseHash(rs.getString("mot_de_passe_hash"));
        u.setTelephone(rs.getString("telephone"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setStatutCompte(StatutCompte.valueOf(rs.getString("statut_compte")));
        u.setNoteMoyenne(rs.getDouble("note_moyenne"));
        u.setNombreAvis(rs.getInt("nombre_avis"));

        Timestamp tsInscription = rs.getTimestamp("date_inscription");
        if (tsInscription != null) u.setDateInscription(tsInscription.toLocalDateTime());

        Timestamp tsConnexion = rs.getTimestamp("derniere_connexion");
        if (tsConnexion != null) u.setDerniereConnexion(tsConnexion.toLocalDateTime());

        return u;
    }
}
