package Model;

public class ParkingLot {
    private int lotId, capacity, currentCount;
    private String lotName, address;

    public ParkingLot() {
    }

    public ParkingLot(int lotId, int capacity, int currentCount, String lotName, String address) {
        this.lotId = lotId;
        this.capacity = capacity;
        this.currentCount = currentCount;
        this.lotName = lotName;
        this.address = address;
    }

    public int getLotId() {
        return lotId;
    }

    public void setLotId(int lotId) {
        this.lotId = lotId;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(int currentCount) {
        this.currentCount = currentCount;
    }

    public String getLotName() {
        return lotName;
    }

    public void setLotName(String lotName) {
        this.lotName = lotName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
