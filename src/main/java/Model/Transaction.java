package Model;

public class Transaction {
    private int transId, sessionId, userId, membershipId, ruleId;
    private double amount, discountAmount;
    private String paymentMethod, feeType, paymentStatus, createdAt;

    public Transaction() {
    }

    public Transaction(int transId, int sessionId, int userId, int membershipId, int ruleId, double amount, double discountAmount, String paymentMethod, String feeType, String paymentStatus, String createdAt) {
        this.transId = transId;
        this.sessionId = sessionId;
        this.userId = userId;
        this.membershipId = membershipId;
        this.ruleId = ruleId;
        this.amount = amount;
        this.discountAmount = discountAmount;
        this.paymentMethod = paymentMethod;
        this.feeType = feeType;
        this.paymentStatus = paymentStatus;
        this.createdAt = createdAt;
    }

    public int getTransId() {
        return transId;
    }

    public void setTransId(int transId) {
        this.transId = transId;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(int membershipId) {
        this.membershipId = membershipId;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
