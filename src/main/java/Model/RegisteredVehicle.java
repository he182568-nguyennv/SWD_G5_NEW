package Model;

public class RegisteredVehicle {
    private int vehicleId, userId, typeId;
    private String plateNumber;
    private boolean isActive;

    public RegisteredVehicle() {
    }

    public RegisteredVehicle(int vehicleId, int userId, int typeId, String plateNumber, boolean isActive) {
        this.vehicleId = vehicleId;
        this.userId = userId;
        this.typeId = typeId;
        this.plateNumber = plateNumber;
        this.isActive = isActive;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
