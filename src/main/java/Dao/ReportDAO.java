package Dao;



import Model.Report;
import Utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReportDAO {

    private Report mapRow(ResultSet rs) throws SQLException {
        Report r = new Report();
        r.setReportId(rs.getInt("report_id"));
        r.setReporterId(rs.getInt("reporter_id"));
        r.setVehicleId(rs.getInt("vehicle_id"));
        r.setSessionId(rs.getInt("session_id"));
        r.setReporterName(rs.getString("reporter_name"));
        r.setReporterPhone(rs.getString("reporter_phone"));
        r.setReportType(rs.getString("report_type"));
        r.setNotes(rs.getString("notes"));
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getString("created_at"));
        return r;
    }

    public int insert(Report r) throws SQLException {
        String sql = "INSERT INTO reports(reporter_id,vehicle_id,session_id,reporter_name,reporter_phone,report_type,notes,status) VALUES(?,?,?,?,?,?,?,'pending')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getReporterId());
            ps.setInt(2, r.getVehicleId());
            ps.setInt(3, r.getSessionId());
            ps.setString(4, r.getReporterName());
            ps.setString(5, r.getReporterPhone());
            ps.setString(6, r.getReportType());
            ps.setString(7, r.getNotes());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public List<Report> findPending() throws SQLException {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE status='pending' ORDER BY created_at DESC";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public List<Report> findByReporter(int reporterId) throws SQLException {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports WHERE reporter_id=? ORDER BY created_at DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reporterId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean approve(int reportId, int managerId, String decision, String note) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            String upd = "UPDATE reports SET status=? WHERE report_id=?";
            try (PreparedStatement ps = c.prepareStatement(upd)) {
                ps.setString(1, decision.equals("approved") ? "approved" : "rejected");
                ps.setInt(2, reportId);
                ps.executeUpdate();
            }
            String ins = "INSERT INTO report_approvals(report_id,manager_id,decision,note) VALUES(?,?,?,?)";
            try (PreparedStatement ps = c.prepareStatement(ins)) {
                ps.setInt(1, reportId);
                ps.setInt(2, managerId);
                ps.setString(3, decision);
                ps.setString(4, note);
                ps.executeUpdate();
            }
            c.commit();
            return true;
        }
    }

    public int countByStatus(String status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM reports WHERE status=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public List<Report> findRecent(int limit) throws SQLException {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports ORDER BY created_at DESC LIMIT ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Report> findAll() throws SQLException {
        List<Report> list = new ArrayList<>();
        String sql = "SELECT * FROM reports ORDER BY created_at DESC";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }
}
