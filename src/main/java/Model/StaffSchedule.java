package Model;

public class StaffSchedule {
    private int scheduleId, staffId, lotId;
    private String workDate, shiftStart, shiftEnd, status;

    public StaffSchedule() {
    }

    public StaffSchedule(int scheduleId, int staffId, int lotId, String workDate, String shiftStart, String shiftEnd, String status) {
        this.scheduleId = scheduleId;
        this.staffId = staffId;
        this.lotId = lotId;
        this.workDate = workDate;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
        this.status = status;
    }

    public int getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(int scheduleId) {
        this.scheduleId = scheduleId;
    }

    public int getStaffId() {
        return staffId;
    }

    public void setStaffId(int staffId) {
        this.staffId = staffId;
    }

    public int getLotId() {
        return lotId;
    }

    public void setLotId(int lotId) {
        this.lotId = lotId;
    }

    public String getWorkDate() {
        return workDate;
    }

    public void setWorkDate(String workDate) {
        this.workDate = workDate;
    }

    public String getShiftStart() {
        return shiftStart;
    }

    public void setShiftStart(String shiftStart) {
        this.shiftStart = shiftStart;
    }

    public String getShiftEnd() {
        return shiftEnd;
    }

    public void setShiftEnd(String shiftEnd) {
        this.shiftEnd = shiftEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
