package Model;

public class MembershipPlan {
    private int planId, durationDays;
    private String name;
    private double price, discountPct;
    private boolean isActive;

    public MembershipPlan() {
    }

    public MembershipPlan(int planId, int durationDays, String name, double price, double discountPct, boolean isActive) {
        this.planId = planId;
        this.durationDays = durationDays;
        this.name = name;
        this.price = price;
        this.discountPct = discountPct;
        this.isActive = isActive;
    }

    public int getPlanId() {
        return planId;
    }

    public void setPlanId(int planId) {
        this.planId = planId;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(int durationDays) {
        this.durationDays = durationDays;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscountPct() {
        return discountPct;
    }

    public void setDiscountPct(double discountPct) {
        this.discountPct = discountPct;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
