package Dao;


import Model.RegisteredVehicle;
import Utils.DBConnection;

import java.sql.*;
import java.util.*;

public class VehicleDAO {

    private RegisteredVehicle map(ResultSet rs) throws SQLException {
        RegisteredVehicle v = new RegisteredVehicle();
        v.setVehicleId(rs.getInt("vehicle_id"));
        v.setUserId(rs.getInt("user_id"));
        v.setTypeId(rs.getInt("type_id"));
        v.setPlateNumber(rs.getString("plate_number"));
        v.setActive(rs.getInt("is_active") == 1);
        return v;
    }

    public List<RegisteredVehicle> findByUser(int userId) throws SQLException {
        List<RegisteredVehicle> list = new ArrayList<>();
        String sql = "SELECT * FROM registered_vehicles WHERE user_id = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public RegisteredVehicle findByPlate(String plate) throws SQLException {
        String sql = "SELECT * FROM registered_vehicles WHERE plate_number = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, plate.toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(RegisteredVehicle v) throws SQLException {
        String sql = "INSERT INTO registered_vehicles(user_id,type_id,plate_number,is_active) VALUES(?,?,?,1)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, v.getUserId());
            ps.setInt(2, v.getTypeId());
            ps.setString(3, v.getPlateNumber().toUpperCase());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public boolean toggleActive(int vehicleId, int userId) throws SQLException {
        String sql = "UPDATE registered_vehicles SET is_active = 1 - is_active WHERE vehicle_id=? AND user_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vehicleId); ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int vehicleId, int userId) throws SQLException {
        String sql = "UPDATE registered_vehicles SET is_active=0 WHERE vehicle_id=? AND user_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vehicleId); ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }
}
