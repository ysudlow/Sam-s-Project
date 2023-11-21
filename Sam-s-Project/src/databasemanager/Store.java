package databasemanager;

import java.time.LocalDate;

public class Store {
    private static final String store_name = null;
    private int storeId;
    private String storeName;
    private String address;
    private String city;
    private String state;
    private int zip;
    private String phone;
    private StoreType storeType; // Assuming StoreType is an enum you've defined
    private LocalDate openingDate;

    // Constructor
    public Store(int storeId, String storeName, String address, String city,
            String state, int zip, String phone, StoreType storeType,
            LocalDate localDate) {
        this.storeId = storeId;
        this.storeName = storeName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.phone = phone;
        this.storeType = storeType;
        this.openingDate = localDate;
    }

    // Getter methods
    public int getStoreId() {
        return storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public int getZip() {
        return zip;
    }

    public String getPhone() {
        return phone;
    }

    public StoreType getStoreType() {
        return storeType;
    }

    public LocalDate getOpeningDate() {
        return openingDate;
    }

    @Override
    public String toString() {
        return "Store{" +
                "store_id=" + storeId +
                ", store_name='" + store_name + '\'' +
                ", address='" + address + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zip=" + zip +
                ", phone='" + phone + '\'' +
                ", store_type=" + storeType +
                ", opening_date='" + openingDate + '\'' +
                '}';
    }
}
