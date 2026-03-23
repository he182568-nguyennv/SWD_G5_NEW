package Dao;


import Model.StaffSchedule;
import Utils.DBConnection;

import java.sql.*;
import java.util.*;

public class StaffScheduleDAO {

    private StaffSchedule map(ResultSet rs) throws SQLException {
        StaffSchedule s = new StaffSchedule();
        s.setScheduleId(rs.getInt("schedule_id"));
        s.setStaffId(rs.getInt("staff_id"));
        s.setLotId(rs.getInt("lot_id"));
        s.setWorkDate(rs.getString("work_date"));
        s.setShiftStart(rs.getString("shift_start"));
        s.setShiftEnd(rs.getString("shift_end"));
        s.setStatus(rs.getString("status"));
        return s;
    }

    public List<StaffSchedule> findAll() throws SQLException {
        List<StaffSchedule> list = new ArrayList<>();
        String sql = "SELECT * FROM staff_schedules ORDER BY work_date DESC, staff_id";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<StaffSchedule> findByStaff(int staffId) throws SQLException {
        List<StaffSchedule> list = new ArrayList<>();
        String sql = "SELECT * FROM staff_schedules WHERE staff_id=? ORDER BY work_date DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public int insert(StaffSchedule s) throws SQLException {
        String sql = "INSERT INTO staff_schedules(staff_id,lot_id,work_date,shift_start,shift_end,status) VALUES(?,?,?,?,?,'scheduled')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,s.getStaffId()); ps.setInt(2,s.getLotId());
            ps.setString(3,s.getWorkDate()); ps.setString(4,s.getShiftStart()); ps.setString(5,s.getShiftEnd());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public boolean updateStatus(int scheduleId, String status) throws SQLException {
        String sql = "UPDATE staff_schedules SET status=? WHERE schedule_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status); ps.setInt(2, scheduleId);
            return ps.executeUpdate() > 0;
        }
    }
}
