package Model;

public class VehicleType {
    private int typeId;
    private String typeName;

    public VehicleType(int typeId, String typeName) {
        this.typeId = typeId;
        this.typeName = typeName;
    }

    public VehicleType() {
    }

    public int getTypeId() {
        return typeId;
    }

    public void setTypeId(int typeId) {
        this.typeId = typeId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }
}
