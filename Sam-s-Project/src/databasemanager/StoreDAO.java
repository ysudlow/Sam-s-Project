package databasemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class StoreDAO {

    private static Connection connection;

    public StoreDAO(Connection connection) {
        this.connection = connection;
    }

    /**
     * @return
     * @throws SQLException
     */
    // method to retrieve all stores
    public List<Store> getAllStores() throws SQLException {
        List<Store> stores = new ArrayList<>();
        String sql = "SELECT * FROM stores";
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {

                Store store = new Store(
                        resultSet.getInt("store_id"),
                        resultSet.getString("store_name"),
                        resultSet.getString("address"),
                        resultSet.getString("city"),
                        resultSet.getString("state"),
                        resultSet.getInt("zip"),
                        resultSet.getString("phone"),
                        StoreType.valueOf(resultSet.getString("store_type").toUpperCase()),
                        resultSet.getDate("opening_date").toLocalDate());
                stores.add(store);
            }
        }
        return stores;
    }

    public void addStore(Store store) throws SQLException {
        String sql = "INSERT INTO stores (store_name, address, city, state, zip, phone, store_type, opening_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Set parameters for the INSERT statement
            stmt.setString(1, store.getStoreName());
            stmt.setString(2, store.getAddress());
            stmt.setString(3, store.getCity());
            stmt.setString(4, store.getState());
            stmt.setInt(5, store.getZip());
            stmt.setString(6, store.getPhone());
            stmt.setString(7, store.getStoreType().toString());
            stmt.setDate(8, java.sql.Date.valueOf(store.getOpeningDate())); // Convert LocalDate to SQL Date
            stmt.executeUpdate();
        }
    }

    public void deleteStore(int storeId) throws SQLException {
        String sql = "DELETE FROM stores WHERE store_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, storeId);
            stmt.executeUpdate();
        }
    }

    public void updateStore(Store store) throws SQLException {
        String sql = "UPDATE stores SET store_name = ?, address = ?, city = ?, state = ?, zip = ?, phone = ?, store_type = ?, opening_date = ? WHERE store_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // Set parameters for the UPDATE statement
            stmt.setString(1, store.getStoreName());
            stmt.setString(2, store.getAddress());
            stmt.setString(3, store.getCity());
            stmt.setString(4, store.getState());
            stmt.setInt(5, store.getZip());
            stmt.setString(6, store.getPhone());
            stmt.setString(7, store.getStoreType().toString());
            stmt.setDate(8, java.sql.Date.valueOf(store.getOpeningDate())); // Convert LocalDate to SQL Date
            stmt.setInt(9, store.getStoreId());
            stmt.executeUpdate();
        }
    }
}