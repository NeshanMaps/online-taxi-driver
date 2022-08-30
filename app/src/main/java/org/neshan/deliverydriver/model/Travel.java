package org.neshan.deliverydriver.model;

public class Travel extends BaseModel {

    private long price;
    private double sourceLat;
    private double sourceLng;
    private double destinationLat;
    private double destinationLng;
    private String sourceAddress;
    private String destinationAddress;
    private Driver driver;
    private Consignment consignment;
    private User user;

    public long getPrice() {
        return price;
    }

    public Travel setPrice(long price) {
        this.price = price;
        return this;
    }

    public double getSourceLat() {
        return sourceLat;
    }

    public Travel setSourceLat(double sourceLat) {
        this.sourceLat = sourceLat;
        return this;
    }

    public double getSourceLng() {
        return sourceLng;
    }

    public Travel setSourceLng(double sourceLng) {
        this.sourceLng = sourceLng;
        return this;
    }

    public double getDestinationLat() {
        return destinationLat;
    }

    public Travel setDestinationLat(double destinationLat) {
        this.destinationLat = destinationLat;
        return this;
    }

    public double getDestinationLng() {
        return destinationLng;
    }

    public Travel setDestinationLng(double destinationLng) {
        this.destinationLng = destinationLng;
        return this;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public Travel setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
        return this;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public Travel setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
        return this;
    }

    public Driver getDriver() {
        return driver;
    }

    public Travel setDriver(Driver driver) {
        this.driver = driver;
        return this;
    }

    public Consignment getConsignment() {
        return consignment;
    }

    public Travel setConsignment(Consignment consignment) {
        this.consignment = consignment;
        return this;
    }

    public User getUser() {
        return user;
    }

    public Travel setUser(User user) {
        this.user = user;
        return this;
    }
}
