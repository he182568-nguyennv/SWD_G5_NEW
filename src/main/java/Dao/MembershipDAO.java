package Dao;


import Model.Membership;
import Model.MembershipPlan;
import Utils.DBConnection;

import java.sql.*;
import java.util.*;

public class MembershipDAO {

    private Membership mapMembership(ResultSet rs) throws SQLException {
        Membership m = new Membership();
        m.setMembershipId(rs.getInt("membership_id"));
        m.setUserId(rs.getInt("user_id"));
        m.setPlanId(rs.getInt("plan_id"));
        m.setStartDate(rs.getString("start_date"));
        m.setEndDate(rs.getString("end_date"));
        m.setStatus(rs.getString("status"));
        return m;
    }

    private MembershipPlan mapPlan(ResultSet rs) throws SQLException {
        MembershipPlan p = new MembershipPlan();
        p.setPlanId(rs.getInt("plan_id"));
        p.setName(rs.getString("name"));
        p.setDurationDays(rs.getInt("duration_days"));
        p.setPrice(rs.getDouble("price"));
        p.setDiscountPct(rs.getDouble("discount_pct"));
        p.setActive(rs.getInt("is_active") == 1);
        return p;
    }

    public List<MembershipPlan> findAllPlans() throws SQLException {
        List<MembershipPlan> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM membership_plans WHERE is_active=1 ORDER BY duration_days")) {
            while (rs.next()) list.add(mapPlan(rs));
        }
        return list;
    }

    public Membership findActiveByUser(int userId) throws SQLException {
        String sql = "SELECT * FROM memberships WHERE user_id=? AND status='active' AND end_date >= date('now') LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapMembership(rs) : null; }
        }
    }

    public List<Membership> findByUser(int userId) throws SQLException {
        List<Membership> list = new ArrayList<>();
        String sql = "SELECT * FROM memberships WHERE user_id=? ORDER BY start_date DESC";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapMembership(rs)); }
        }
        return list;
    }

    public int register(int userId, int planId) throws SQLException {
        MembershipPlan plan = getPlanById(planId);
        if (plan == null) return -1;
        String startDate = java.time.LocalDate.now().toString();
        String endDate   = java.time.LocalDate.now().plusDays(plan.getDurationDays()).toString();
        String sql = "INSERT INTO memberships(user_id,plan_id,start_date,end_date,status) VALUES(?,?,?,?,'active')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,userId); ps.setInt(2,planId);
            ps.setString(3,startDate); ps.setString(4,endDate);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public MembershipPlan getPlanById(int planId) throws SQLException {
        String sql = "SELECT * FROM membership_plans WHERE plan_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, planId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? mapPlan(rs) : null; }
        }
    }

    public double getPlanDiscountPct(int planId) throws SQLException {
        MembershipPlan plan = getPlanById(planId);
        return plan != null ? plan.getDiscountPct() : 0;
    }
}
