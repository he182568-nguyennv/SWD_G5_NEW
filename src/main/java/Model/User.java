package Model;

public class User {
    private String id;
    private String fullName;
    private int gender;
    private int age;
    private String address;
    private String account;
    private String password;
    private Role roleId;

    public User(String id, String fullName, int gender, int age, String address, String account, String password, Role roleId) {
        this.id = id;
        this.fullName = fullName;
        this.gender = gender;
        this.age = age;
        this.address = address;
        this.account = account;
        this.password = password;
        this.roleId = roleId;
    }

    public User(String id, String fullName, int gender, int age, String address, String account, String password) {
        this.id = id;
        this.fullName = fullName;
        this.gender = gender;
        this.age = age;
        this.address = address;
        this.account = account;
        this.password = password;
    }

    public Role getRoleId() {
        return roleId;
    }

    public void setRoleId(Role roleId) {
        this.roleId = roleId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

