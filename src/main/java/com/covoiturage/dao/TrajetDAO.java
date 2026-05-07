package com.covoiturage.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.Trajet.StatutTrajet;
import com.covoiturage.util.DatabaseConnection;


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

    private static final String SQL_SELECT_RECHERCHE_BASE =
        "SELECT t.*, u.nom, u.prenom, u.email, u.telephone, u.note_moyenne " +
        "FROM trajets t JOIN utilisateurs u ON t.chauffeur_id = u.id " +
        "WHERE t.statut IN ('OUVERT','COMPLET') ";

    private static final String SQL_UPDATE_STATUT =
        "UPDATE trajets SET statut=? WHERE id=?";

    private static final String SQL_UPDATE_PLACES =
        "UPDATE trajets SET places_disponibles=?, statut=? WHERE id=?";



    
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

    
    public List<Trajet> trouverDisponibles() throws SQLException {
        List<Trajet> liste = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_SELECT_DISPONIBLES);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) liste.add(mapperResultSet(rs));
        }
        return liste;
    }

    
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

    
    public List<Trajet> rechercherFlexible(String villeDepart, String villeArrivee,
                                           String date, Integer placesMin) throws SQLException {
        List<Trajet> liste = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SQL_SELECT_RECHERCHE_BASE);
        List<Object> params = new ArrayList<>();

        if (villeDepart != null && !villeDepart.isBlank()) {
            sql.append("AND t.ville_depart LIKE ? ");
            params.add("%" + villeDepart + "%");
        }
        if (villeArrivee != null && !villeArrivee.isBlank()) {
            sql.append("AND t.ville_arrivee LIKE ? ");
            params.add("%" + villeArrivee + "%");
        }
        if (date != null && !date.isBlank()) {
            sql.append("AND CAST(t.date_heure_depart AS DATE) = ? ");
            params.add(date);
        }
        if (placesMin != null) {
            sql.append("AND t.places_disponibles >= ? ");
            params.add(placesMin);
        }

        sql.append("ORDER BY t.prix_par_place ASC");

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) liste.add(mapperResultSet(rs));
            }
        }

        return liste;
    }

    
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

    
    public void mettreAJourStatut(int trajetId, StatutTrajet statut) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL_UPDATE_STATUT)) {

            ps.setString(1, statut.name());
            ps.setInt(2, trajetId);
            ps.executeUpdate();
        }
    }

    
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


        Chauffeur chauffeur = new Chauffeur();
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
