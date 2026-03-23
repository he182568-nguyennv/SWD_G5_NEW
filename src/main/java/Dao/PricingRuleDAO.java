package Dao;


import Model.PricingRule;
import Utils.DBConnection;

import java.sql.*;
import java.util.*;

public class PricingRuleDAO {

    private PricingRule map(ResultSet rs) throws SQLException {
        PricingRule r = new PricingRule();
        r.setRuleId(rs.getInt("rule_id"));
        r.setLotId(rs.getInt("lot_id"));
        r.setTypeId(rs.getInt("type_id"));
        r.setFeeType(rs.getString("fee_type"));
        r.setPricePerBlock(rs.getDouble("price_per_block"));
        r.setBlockMinutes(rs.getInt("block_minutes"));
        r.setMaxDailyFee(rs.getDouble("max_daily_fee"));
        r.setNightFee(rs.getInt("is_night_fee") == 1);
        r.setActive(rs.getInt("is_active") == 1);
        return r;
    }

    public List<PricingRule> findAll() throws SQLException {
        List<PricingRule> list = new ArrayList<>();
        String sql = "SELECT pr.*, pl.lot_name, vt.type_name FROM pricing_rules pr " +
                     "JOIN parking_lots pl ON pr.lot_id=pl.lot_id " +
                     "JOIN vehicle_types vt ON pr.type_id=vt.type_id ORDER BY pr.lot_id, pr.type_id";
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                PricingRule r = map(rs);
                list.add(r);
            }
        }
        return list;
    }

    public int insert(PricingRule r) throws SQLException {
        String sql = "INSERT INTO pricing_rules(lot_id,type_id,fee_type,price_per_block,block_minutes,max_daily_fee,is_night_fee,is_active) VALUES(?,?,?,?,?,?,?,1)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,r.getLotId()); ps.setInt(2,r.getTypeId()); ps.setString(3,r.getFeeType());
            ps.setDouble(4,r.getPricePerBlock()); ps.setInt(5,r.getBlockMinutes());
            ps.setDouble(6,r.getMaxDailyFee()); ps.setInt(7,r.isNightFee()?1:0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) { return rs.next() ? rs.getInt(1) : -1; }
        }
    }

    public boolean update(PricingRule r) throws SQLException {
        String sql = "UPDATE pricing_rules SET fee_type=?,price_per_block=?,block_minutes=?,max_daily_fee=?,is_night_fee=? WHERE rule_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1,r.getFeeType()); ps.setDouble(2,r.getPricePerBlock());
            ps.setInt(3,r.getBlockMinutes()); ps.setDouble(4,r.getMaxDailyFee());
            ps.setInt(5,r.isNightFee()?1:0); ps.setInt(6,r.getRuleId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean toggleActive(int ruleId) throws SQLException {
        String sql = "UPDATE pricing_rules SET is_active = 1 - is_active WHERE rule_id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, ruleId); return ps.executeUpdate() > 0;
        }
    }

    public PricingRule findApplicable(int lotId, int typeId, String feeType) throws SQLException {
        String sql = "SELECT * FROM pricing_rules WHERE lot_id=? AND type_id=? AND fee_type=? AND is_active=1 LIMIT 1";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1,lotId); ps.setInt(2,typeId); ps.setString(3,feeType);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? map(rs) : null; }
        }
    }
}
