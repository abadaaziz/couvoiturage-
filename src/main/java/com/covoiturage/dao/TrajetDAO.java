package com.covoiturage.dao;

import com.covoiturage.model.Trajet;
import com.covoiturage.model.Trajet.StatutTrajet;
import com.covoiturage.model.Utilisateur;
import com.covoiturage.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour l'entité {@link Trajet}. JDBC pur, aucun ORM.
 */
public class TrajetDAO {

    private static final String SQL_INSERT =
        "INSERT INTO trajets (ville_depart, ville_arrivee, date_heure_depart, places_total, " +
        "places_disponibles, prix_par_place, statut, chauffeur_id, description_vehicule, date_creation) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    private static final String SQL_SELECT_BY_ID =
        "SELECT t.*, u.nom, u.prenom, u.email, u.telephone, u.note_moyenne " +
        "FROM trajets t JOIN utilisateurs u ON t.chauffeur_id = u.id WHERE t.id = ?";

    private static final String SQL_SELECT_DISPONIBLES =
        "SELECT t.*, u.nom, u.prenom, u.email, u.telephone, u.note_moyenne " +
        "FROM trajets t JOIN utilisateurs u ON t.chauffeur_id = u.id " +
        "WHERE t.statut = 'OUVERT' AND t.date_heure_depart > CURRENT_TIMESTAMP " +
        "ORDER BY t.date_heure_depart ASC";

    private static final String SQL_RECHERCHE =
        "SELECT t.*, u.nom, u.prenom, u.email, u.telephone, u.note_moyenne " +
        "FROM trajets t JOIN utilisateurs u ON t.chauffeur_id = u.id " +
        "WHERE t.statut IN ('OUVERT','COMPLET') " +
        "AND t.ville_depart LIKE ? AND t.ville_arrivee LIKE ? " +
        "AND CAST(t.date_heure_depart AS DATE) = ? " +
        "AND t.places_disponibles >= ? " +
        "ORDER BY t.prix_par_place ASC";

    private static final String SQL_SELECT_BY_CHAUFFEUR =
        "SELECT t.*, u.nom, u.prenom, u.email, u.telephone, u.note_moyenne " +
        "FROM trajets t JOIN utilisateurs u ON t.chauffeur_id = u.id " +
        "WHERE t.chauffeur_id = ? ORDER BY t.date_heure_depart DESC";

    private static final String SQL_UPDATE_STATUT =
        "UPDATE trajets SET statut=? WHERE id=?";

    private static final String SQL_UPDATE_PLACES =
        "UPDATE trajets SET places_disponibles=?, statut=? WHERE id=?";

    private static final String SQL_UPDATE_COMPLET =
        "UPDATE trajets SET statut='COMPLET' WHERE id=? AND places_disponibles=0";

    // ── Méthodes CRUD ─────────────────────────────────────────────────────────

    /**
     * Insère un trajet en base et retourne l'objet avec l'id généré.
     */
    public Trajet inserer(Trajet trajet) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERT, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, trajet.getVilleDepart());
            ps.setString(2, trajet.getVilleArrivee());
            ps.setTimestamp(3, Timestamp.valueOf(trajet.getDateHeureDepart()));
            ps.setInt(4, trajet.getPlacesTotal());
            ps.setInt(5, trajet.getPlacesDisponibles());
            ps.setDouble(6, trajet.getPrixParPlace());
            ps.setString(7, trajet.getStatut().name());
            ps.setInt(8, trajet.getChauffeur().getId());
            ps.setString(9, trajet.getDescriptionVehicule());
            ps.setTimestamp(10, Timestamp.valueOf(trajet.getDateCreation() != null
                ? trajet.getDateCreation() : java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) trajet.setId(rs.getInt(1));
            }
        }
        return trajet;
    }

    /**
     * Recherche un trajet par son id.
     */
    public Optional<Trajet> trouverParId(int id) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_ID)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapperResultSet(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Retourne tous les trajets ouverts et futurs.
     */
    public List<Trajet> trouverDisponibles() throws SQLException {
        List<Trajet> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_DISPONIBLES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) liste.add(mapperResultSet(rs));
        }
        return liste;
    }

    /**
     * Recherche des trajets avec filtres (ville départ, arrivée, date, places min).
     *
     * @param villeDepart  Ville de départ (partielle, ex: "Par" → "Paris")
     * @param villeArrivee Ville d'arrivée
     * @param date         Date du trajet (format "yyyy-MM-dd")
     * @param placesMin    Nombre minimum de places requises
     */
    public List<Trajet> rechercher(String villeDepart, String villeArrivee,
                                    String date, int placesMin) throws SQLException {
        List<Trajet> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_RECHERCHE)) {

            ps.setString(1, "%" + villeDepart + "%");
            ps.setString(2, "%" + villeArrivee + "%");
            ps.setString(3, date);
            ps.setInt(4, placesMin);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapperResultSet(rs));
            }
        }
        return liste;
    }

    /**
     * Retourne tous les trajets proposés par un chauffeur donné.
     */
    public List<Trajet> trouverParChauffeur(int chauffeurId) throws SQLException {
        List<Trajet> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_BY_CHAUFFEUR)) {

            ps.setInt(1, chauffeurId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapperResultSet(rs));
            }
        }
        return liste;
    }

    /**
     * Met à jour le statut d'un trajet (ANNULE, TERMINE, etc.).
     */
    public void mettreAJourStatut(int trajetId, StatutTrajet statut) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUT)) {

            ps.setString(1, statut.name());
            ps.setInt(2, trajetId);
            ps.executeUpdate();
        }
    }

    /**
     * Met à jour le nombre de places disponibles et le statut en une seule requête atomique.
     *
     * @param trajetId          Identifiant du trajet
     * @param placesDisponibles Nouveau nombre de places disponibles
     * @param statut            Nouveau statut calculé
     */
    public void mettreAJourPlaces(int trajetId, int placesDisponibles, StatutTrajet statut)
            throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_PLACES)) {

            ps.setInt(1, placesDisponibles);
            ps.setString(2, statut.name());
            ps.setInt(3, trajetId);
            ps.executeUpdate();
        }
    }

    // ── Mapping ResultSet → Trajet ────────────────────────────────────────────

    private Trajet mapperResultSet(ResultSet rs) throws SQLException {
        Trajet t = new Trajet();
        t.setId(rs.getInt("id"));
        t.setVilleDepart(rs.getString("ville_depart"));
        t.setVilleArrivee(rs.getString("ville_arrivee"));
        t.setDateHeureDepart(rs.getTimestamp("date_heure_depart").toLocalDateTime());
        t.setPlacesTotal(rs.getInt("places_total"));
        t.setPlacesDisponibles(rs.getInt("places_disponibles"));
        t.setPrixParPlace(rs.getDouble("prix_par_place"));
        t.setStatut(StatutTrajet.valueOf(rs.getString("statut")));
        t.setDescriptionVehicule(rs.getString("description_vehicule"));
        t.setDateCreation(rs.getTimestamp("date_creation").toLocalDateTime());

        // Hydratation minimale du chauffeur (sans récursion)
        Utilisateur chauffeur = new Utilisateur();
        chauffeur.setId(rs.getInt("chauffeur_id"));
        chauffeur.setNom(rs.getString("nom"));
        chauffeur.setPrenom(rs.getString("prenom"));
        chauffeur.setEmail(rs.getString("email"));
        chauffeur.setTelephone(rs.getString("telephone"));
        chauffeur.setNoteMoyenne(rs.getDouble("note_moyenne"));
        t.setChauffeur(chauffeur);

        return t;
    }
}
