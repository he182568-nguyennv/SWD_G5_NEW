package Dao;


import Model.ParkingSession;
import Utils.DBConnection;

import java.sql.*;
import java.util.*;

public class ParkingSessionDAO {

    private ParkingSession map(ResultSet rs) throws SQLException {
        ParkingSession s = new ParkingSession();
        s.setSessionId(rs.getInt("session_id"));
        s.setVehicleId(rs.getInt("vehicle_id"));
        s.setLotId(rs.getInt("lot_id"));
        s.setCardId(rs.getInt("card_id"));
        s.setStaffCheckinId(rs.getInt("staff_checkin_id"));
        s.setStaffCheckoutId(rs.getInt("staff_checkout_id"));
        s.setMembershipId(rs.getInt("membership_id"));
        s.setCheckinTime(rs.getString("checkin_time"));
        s.setCheckoutTime(rs.getString("checkout_time"));
        s.setVehicleImgIn(rs.getString("vehicle_img_in"));
        s.setVehicleImgOut(rs.getString("vehicle_img_out"));
        s.setPlateNumber(rs.getString("plate_number"));
        s.setStatus(rs.getString("status"));
        return s;
    }

    public int checkIn(ParkingSession s) throws SQLException {
        String sql = "INSERT INTO parking_sessions(vehicle_id,lot_id,card_id,staff_checkin_id,membership_id,plate_number,vehicle_img_in,status) VALUES(?,?,?,?,?,?,?,'active')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,s.getVehicleId()); ps.setInt(2,s.getLotId()); ps.setInt(3,s.getCardId());
            ps.setInt(4,s.getStaffCheckinId()); ps.setInt(5,s.getMembershipId());
            ps.setString(6,s.getPlateNumber()); ps.setString(7,s.getVehicleImgIn());
            ps.executeUpdate();
            c.createStatement().execute("UPDATE parking_lots SET current_count=current_count+1 WHERE lot_id="+s.getLotId());
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public boolean checkOut(int sessionId, int staffId, String imgOut) throws SQLException {
        String sql = "UPDATE parking_sessions SET checkout_time=datetime('now'),staff_checkout_id=?,vehicle_img_out=?,status='completed' WHERE session_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1,staffId); ps.setString(2,imgOut); ps.setInt(3,sessionId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ResultSet rs2 = c.createStatement().executeQuery("SELECT lot_id FROM parking_sessions WHERE session_id="+sessionId);
                if (rs2.next()) c.createStatement().execute("UPDATE parking_lots SET current_count=MAX(0,current_count-1) WHERE lot_id="+rs2.getInt(1));
            }
            return rows > 0;
        }
    }

    public ParkingSession findById(int id) throws SQLException {
        String sql = "SELECT * FROM parking_sessions WHERE session_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1,id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public ParkingSession findActiveByPlate(String plate) throws SQLException {
        String sql = "SELECT * FROM parking_sessions WHERE plate_number=? AND status='active' LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,plate);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }

    public List<ParkingSession> findByLot(int lotId) throws SQLException {
        List<ParkingSession> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_sessions WHERE lot_id=? ORDER BY checkin_time DESC LIMIT 100";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1,lotId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<ParkingSession> findRecent(int limit) throws SQLException {
        List<ParkingSession> list = new ArrayList<>();
        String sql = "SELECT * FROM parking_sessions ORDER BY checkin_time DESC LIMIT ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1,limit);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public int countByDate(String date) throws SQLException {
        String sql = "SELECT COUNT(*) FROM parking_sessions WHERE date(checkin_time)=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,date);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        }
    }

    public int countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM parking_sessions WHERE status=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }
}
