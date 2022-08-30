package org.neshan.deliverydriver.model;

public class User extends BaseModel{
    private String phoneNumber;

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public User setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }
}
