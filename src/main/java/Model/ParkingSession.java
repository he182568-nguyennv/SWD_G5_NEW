package Model;

public class ParkingSession {
    private int sessionId, vehicleId, lotId, cardId, staffCheckinId, staffCheckoutId, membershipId;
    private String checkinTime, checkoutTime, vehicleImgIn, vehicleImgOut, plateNumber, status;

    public ParkingSession() {
    }

    public ParkingSession(int sessionId, int vehicleId, int lotId, int cardId, int staffCheckinId, int staffCheckoutId, int membershipId, String checkinTime, String checkoutTime, String vehicleImgIn, String vehicleImgOut, String plateNumber, String status) {
        this.sessionId = sessionId;
        this.vehicleId = vehicleId;
        this.lotId = lotId;
        this.cardId = cardId;
        this.staffCheckinId = staffCheckinId;
        this.staffCheckoutId = staffCheckoutId;
        this.membershipId = membershipId;
        this.checkinTime = checkinTime;
        this.checkoutTime = checkoutTime;
        this.vehicleImgIn = vehicleImgIn;
        this.vehicleImgOut = vehicleImgOut;
        this.plateNumber = plateNumber;
        this.status = status;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getLotId() {
        return lotId;
    }

    public void setLotId(int lotId) {
        this.lotId = lotId;
    }

    public int getCardId() {
        return cardId;
    }

    public void setCardId(int cardId) {
        this.cardId = cardId;
    }

    public int getStaffCheckinId() {
        return staffCheckinId;
    }

    public void setStaffCheckinId(int staffCheckinId) {
        this.staffCheckinId = staffCheckinId;
    }

    public int getStaffCheckoutId() {
        return staffCheckoutId;
    }

    public void setStaffCheckoutId(int staffCheckoutId) {
        this.staffCheckoutId = staffCheckoutId;
    }

    public int getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(int membershipId) {
        this.membershipId = membershipId;
    }

    public String getCheckinTime() {
        return checkinTime;
    }

    public void setCheckinTime(String checkinTime) {
        this.checkinTime = checkinTime;
    }

    public String getCheckoutTime() {
        return checkoutTime;
    }

    public void setCheckoutTime(String checkoutTime) {
        this.checkoutTime = checkoutTime;
    }

    public String getVehicleImgIn() {
        return vehicleImgIn;
    }

    public void setVehicleImgIn(String vehicleImgIn) {
        this.vehicleImgIn = vehicleImgIn;
    }

    public String getVehicleImgOut() {
        return vehicleImgOut;
    }

    public void setVehicleImgOut(String vehicleImgOut) {
        this.vehicleImgOut = vehicleImgOut;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
