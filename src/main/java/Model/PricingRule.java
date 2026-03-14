package Model;

public class PricingRule {
    private int ruleId, lotId, typeId, blockMinutes;
    private String feeType;
    private double pricePerBlock, maxDailyFee;
    private boolean isNightFee, isActive;

    public PricingRule() {
    }

    public PricingRule(int ruleId, int lotId, int typeId, int blockMinutes, String feeType, double pricePerBlock, double maxDailyFee, boolean isNightFee, boolean isActive) {
        this.ruleId = ruleId;
        this.lotId = lotId;
        this.typeId = typeId;
        this.blockMinutes = blockMinutes;
        this.feeType = feeType;
        this.pricePerBlock = pricePerBlock;
        this.maxDailyFee = maxDailyFee;
        this.isNightFee = isNightFee;
        this.isActive = isActive;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public int getLotId() {
        return lotId;
    }

    public void setLotId(int lotId) {
        this.lotId = lotId;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public int getBlockMinutes() {
        return blockMinutes;
    }

    public void setBlockMinutes(int blockMinutes) {
        this.blockMinutes = blockMinutes;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public double getPricePerBlock() {
        return pricePerBlock;
    }

    public void setPricePerBlock(double pricePerBlock) {
        this.pricePerBlock = pricePerBlock;
    }

    public double getMaxDailyFee() {
        return maxDailyFee;
    }

    public void setMaxDailyFee(double maxDailyFee) {
        this.maxDailyFee = maxDailyFee;
    }

    public boolean isNightFee() {
        return isNightFee;
    }

    public void setNightFee(boolean nightFee) {
        isNightFee = nightFee;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
