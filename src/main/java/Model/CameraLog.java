package Model;

public class CameraLog {
    private int cameraLogId, sessionId;
    private String plateNumber, vehicleImgIn, capturedTime;

    public CameraLog(int cameraLogId, int sessionId, String plateNumber, String vehicleImgIn, String capturedTime) {
        this.cameraLogId = cameraLogId;
        this.sessionId = sessionId;
        this.plateNumber = plateNumber;
        this.vehicleImgIn = vehicleImgIn;
        this.capturedTime = capturedTime;
    }

    public CameraLog() {
    }

    public int getCameraLogId() {
        return cameraLogId;
    }

    public void setCameraLogId(int cameraLogId) {
        this.cameraLogId = cameraLogId;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getVehicleImgIn() {
        return vehicleImgIn;
    }

    public void setVehicleImgIn(String vehicleImgIn) {
        this.vehicleImgIn = vehicleImgIn;
    }

    public String getCapturedTime() {
        return capturedTime;
    }

    public void setCapturedTime(String capturedTime) {
        this.capturedTime = capturedTime;
    }
}
