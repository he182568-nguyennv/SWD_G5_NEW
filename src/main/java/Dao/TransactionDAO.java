package Dao;

import Model.Transaction;
import Utils.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionDAO {

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setTransId(rs.getInt("trans_id"));
        t.setSessionId(rs.getInt("session_id"));
        t.setUserId(rs.getInt("user_id"));
        t.setMembershipId(rs.getInt("membership_id"));
        t.setRuleId(rs.getInt("rule_id"));
        t.setAmount(rs.getDouble("amount"));
        t.setDiscountAmount(rs.getDouble("discount_amount"));
        t.setPaymentMethod(rs.getString("payment_method"));
        t.setFeeType(rs.getString("fee_type"));
        t.setPaymentStatus(rs.getString("payment_status"));
        t.setCreatedAt(rs.getString("created_at"));
        return t;
    }

    public int insert(Transaction t) throws SQLException {
        String sql = "INSERT INTO transactions(session_id,user_id,membership_id,rule_id,amount,discount_amount,payment_method,fee_type,payment_status) VALUES(?,?,?,?,?,?,?,?,'pending')";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getSessionId());
            ps.setInt(2, t.getUserId());
            ps.setInt(3, t.getMembershipId());
            ps.setInt(4, t.getRuleId());
            ps.setDouble(5, t.getAmount());
            ps.setDouble(6, t.getDiscountAmount());
            ps.setString(7, t.getPaymentMethod());
            ps.setString(8, t.getFeeType());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public boolean markPaid(int transId, String method) throws SQLException {
        String sql = "UPDATE transactions SET payment_status='paid',payment_method=? WHERE trans_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, method);
            ps.setInt(2, transId);
            return ps.executeUpdate() > 0;
        }
    }

    public double revenueByLot(int lotId, String from, String to) throws SQLException {
        String sql = "SELECT COALESCE(SUM(t.amount),0) FROM transactions t " +
                "JOIN parking_sessions s ON t.session_id=s.session_id " +
                "WHERE s.lot_id=? AND t.payment_status='paid' AND t.created_at BETWEEN ? AND ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, lotId);
            ps.setString(2, from);
            ps.setString(3, to);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        }
    }

    public List<Transaction> findAll() throws SQLException {
        List<Transaction> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM transactions ORDER BY created_at DESC")) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }
}