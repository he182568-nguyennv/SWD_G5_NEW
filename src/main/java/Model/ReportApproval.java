package Model;

public class ReportApproval {
    private int approvalId, reportId, managerId;
    private String decision, note, reviewedAt;

    public ReportApproval() {
    }

    public ReportApproval(int approvalId, int reportId, int managerId, String decision, String note, String reviewedAt) {
        this.approvalId = approvalId;
        this.reportId = reportId;
        this.managerId = managerId;
        this.decision = decision;
        this.note = note;
        this.reviewedAt = reviewedAt;
    }

    public int getApprovalId() {
        return approvalId;
    }

    public void setApprovalId(int approvalId) {
        this.approvalId = approvalId;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getManagerId() {
        return managerId;
    }

    public void setManagerId(int managerId) {
        this.managerId = managerId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(String reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}
