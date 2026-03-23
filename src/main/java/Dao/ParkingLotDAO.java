package Dao;


import Model.ParkingLot;
import Utils.DBConnection;

import java.sql.*;
import java.util.*;

public class ParkingLotDAO {

    private ParkingLot map(ResultSet rs) throws SQLException {
        ParkingLot l = new ParkingLot();
        l.setLotId(rs.getInt("lot_id"));
        l.setLotName(rs.getString("lot_name"));
        l.setAddress(rs.getString("address"));
        l.setCapacity(rs.getInt("capacity"));
        l.setCurrentCount(rs.getInt("current_count"));
        return l;
    }

    public List<ParkingLot> findAll() throws SQLException {
        List<ParkingLot> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM parking_lots ORDER BY lot_id")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public ParkingLot findById(int id) throws SQLException {
        String sql = "SELECT * FROM parking_lots WHERE lot_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public int insert(ParkingLot l) throws SQLException {
        String sql = "INSERT INTO parking_lots(lot_name,address,capacity,current_count) VALUES(?,?,?,0)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, l.getLotName()); ps.setString(2, l.getAddress()); ps.setInt(3, l.getCapacity());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public boolean update(ParkingLot l) throws SQLException {
        String sql = "UPDATE parking_lots SET lot_name=?,address=?,capacity=? WHERE lot_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, l.getLotName()); ps.setString(2, l.getAddress());
            ps.setInt(3, l.getCapacity()); ps.setInt(4, l.getLotId());
            return ps.executeUpdate() > 0;
        }
    }
}
