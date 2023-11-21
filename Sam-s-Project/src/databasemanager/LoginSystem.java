package databasemanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

import product.java.Product;

public class LoginSystem {
    // Static variable to hold the currently logged in user
    private static User currentUser = null;
    private static User newUser;
    private static List<Store> stores;
    Scanner scanner = new Scanner(System.in);
    private static PurchaseOrderRequestManager orderRequestManager;

    // Initialization (perhaps in a static block or static method)
    public static void initialize() {
        orderRequestManager = new PurchaseOrderRequestManager(scanner);
    }

    public static void main(String[] args) {
        DatabaseManager dbManager = null;
        Scanner scanner = null;
        try {
            dbManager = new DatabaseManager();
            System.out.println("Connected to the database successfully.");

            scanner = new Scanner(System.in);
            StoreDAO storeDAO = new StoreDAO(dbManager.getConnection());

            // Corrected instantiation of PurchaseOrderRequests
            purchaseOrderRequests orderRequestManager = new purchaseOrderRequests(scanner);

            while (true) {
                // Display the appropriate menu based on user login status
                if (currentUser == null) {
                    displayMainMenu();
                } else {
                    displayUserMenu(scanner);
                }
                // Read the user's choice
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character

                // Handle the user's choice
                if (currentUser == null) {
                    handleMainMenuChoice(scanner, choice);
                } else {
                    handleUserMenuChoice(scanner, choice, storeDAO);
                }
            }
        } catch (SQLException e) {
            System.out.println("Failed to connect to the database: " + e.getMessage());
            e.printStackTrace();
        } catch (RuntimeException e) {
            System.out.println("An unexpected runtime error occurred: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close resources in the finally block
            if (scanner != null) {
                scanner.close();
            }
            if (dbManager != null) {
                try {
                    dbManager.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Additional methods (displayMainMenu, displayUserMenu, handleMainMenuChoice,
    // handleUserMenuChoice) ...

    // Do not close the scanner as it is tied to System.in and will be closed by the
    // JVM upon application exit

    // Displays the main menu options to the user
    private static void displayMainMenu() {
        System.out.println("\nWelcome to Sam's Mart inventory system! Please select a number below.");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Sign Out");
        if (currentUser != null && currentUser.getRole() == UserRole.ADMIN) {
            System.out.println("4. Change User Role");
        }
        System.out.println("5. Exit");
        System.out.print("Choose an option: ");
    }

    // Handles the main menu choices
    private static void handleMainMenuChoice(Scanner scanner, int choice) throws Exception {
        switch (choice) {
            case 1:
                loginUser(scanner);
                break;
            case 2:
                registerUser(scanner);
                break;
            case 3:
                signOut();
                break;
            case 4:
                if (currentUser != null && currentUser.getRole() == UserRole.ADMIN) {
                    changeUserRole(scanner);
                } else {
                    System.out.println("Access denied. Only admins can change user roles.");
                }
                break;
            case 5:
                System.out.println("Goodbye!");
                exitApplication(scanner); // Close the scanner and exit the application
            default:
                System.out.println("Invalid option.");
                break;
        }
    }

    // Handle Admin User Menu
    public static void handleAdminMenuChoice(Scanner scanner, int choice) throws SQLException {
        try (DatabaseManager dbManager = new DatabaseManager()) {
            switch (choice) {
                case 1: // Delete a user
                    System.out.print("Enter the email of the user to delete: ");
                    String emailToDelete = scanner.nextLine();
                    dbManager.deleteUserByEmail(emailToDelete);
                    break;
                case 2: // Assign Manager role
                    System.out.print("Enter the email of the user to assign as Manager: ");
                    String emailToPromote = scanner.nextLine();
                    dbManager.assignManagerRole(emailToPromote);
                    break;
                // ... other admin choices ...
            }
        }
    }

    // Method for admins and managers to Add/delete products
    private static void addOrDeleteProducts(Scanner scanner) throws Exception {
        // Check if the user is logged in and has the right role
        if (currentUser == null
                || (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.MANAGER)) {
            System.out.println("You must be an admin or a manager to modify products.");
            return;
        }

        System.out.println("Select an option:");
        System.out.println("1. Add Product");
        System.out.println("2. Delete Product");
        System.out.print("Your choice: ");

        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline

        switch (choice) {
            case 1:
                addProduct(scanner);
                break;
            case 2:
                deleteProduct(scanner);
                break;
            case 3:
                return; // This will exit the method and return to the previous menu
            default:
                System.out.println("Invalid choice. Please try again.");
                break;
        }
    }

    // Method for users to update inventory
    private static void updateInventory(Scanner scanner) throws Exception {
        // Only logged-in users can update inventory
        if (currentUser == null) {
            System.out.println("Please log in to update inventory.");
            return;
        }

        System.out.println("Update Inventory:");
        System.out.println("1. Update Product Quantity");
        System.out.println("2. Return to User Menu");

        System.out.print("Choose an option: ");
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline

        switch (choice) {
            case 1:
                updateProductQuantity(scanner);
                break;
            case 2:
                // Simply break out of this switch to return to the user menu
                break;
            default:
                System.out.println("Invalid option. Please try again.");
                break;
        }
    }

    private static void updateProductQuantity(Scanner scanner) throws SQLException {
        System.out.print("Enter Product ID to update quantity: ");
        int productId = scanner.nextInt();
        scanner.nextLine(); // consume newline

        System.out.print("Enter new quantity: ");
        int newQuantity = scanner.nextInt();
        scanner.nextLine(); // consume newline

        try (DatabaseManager dbManager = new DatabaseManager()) {
            dbManager.updateProductQuantity(productId, newQuantity);
            System.out.println("Product quantity updated successfully.");
        } catch (SQLException e) {
            System.out.println("A database error occurred while updating product quantity.");
            e.printStackTrace();
        }
    }

    // Method to handle user login
    private static void loginUser(Scanner scanner) throws ClassNotFoundException {
        System.out.print("Enter your email: ");
        String email = scanner.nextLine();

        System.out.print("Enter your password (phone number): ");
        String phoneNumber = scanner.nextLine(); // Password is the phone number

        // Log the email and phone number for debugging purposes
        System.out.println("Attempting to log in with email: " + email + " and phone number: " + phoneNumber);

        try (DatabaseManager dbManager = new DatabaseManager()) {
            User user = dbManager.authenticateUser(email, phoneNumber); // Use phoneNumber as the password
            if (user != null) {
                currentUser = user;
                System.out.println("Login successful! Welcome, " + user.getFirstName() + " " + user.getLastName());
                // You may want to display the user menu or perform other actions here
            } else {
                System.out.println("Login failed: Invalid credentials.");
            }
        } catch (SQLException e) {
            // Debugging log
            System.out.println("A database error occurred during login: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            // Debugging log
            System.out.println("An unexpected error occurred during login. Please contact support.");
            e.printStackTrace();
        }
    }

    // Method to handle user registration
    private static void registerUser(Scanner scanner) {
        String firstName = "";
        while (firstName.isEmpty()) {
            System.out.print("Enter your first name: ");
            firstName = scanner.nextLine().trim();
            if (firstName.isEmpty()) {
                System.out.println("First name cannot be empty. Please enter your first name.");
            }
        }
        System.out.print("Enter your last name: ");
        String lastName = scanner.nextLine();

        System.out.print("Enter your phone number: ");
        String phoneNumber = scanner.nextLine();

        // Check if phone number is valid
        if (!isValidPhoneNumber(phoneNumber)) {
            System.out.println("Invalid phone number. Must be 10 digits.");
            return;
        }

        System.out.print("Enter your email: ");
        String email = scanner.nextLine();

        if (!isValidEmail(email)) {
            System.out.println("Invalid email. Please enter a valid email address.");
            return;
        }
        UserRole role = UserRole.EMPLOYEE;
        // Create a new User object with the collected details
        final User newUser = new User(0, firstName, lastName, phoneNumber, email, email, null);

        try (DatabaseManager dbManager = new DatabaseManager()) {
            if (!dbManager.userExists(email)) {
                dbManager.addUser(newUser);
                System.out.println("Registration successful!");
            } else {
                System.out.println("An account with this email already exists. Please log in.");
            }
        } catch (SQLException e) {
            System.out.println("A database error occurred during registration.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("An unexpected error occurred during registration.");
            e.printStackTrace();
        }

        // Check if email contains an '@' symbol
        if (!isValidEmail(email)) {
            System.out.println("Invalid email. Please enter a valid email address.");
        } else {
            {
            }
            try (DatabaseManager dbManager = new DatabaseManager()) {
                if (!dbManager.userExists(email)) {
                    dbManager.addUser(newUser);
                    System.out.println("Registration successful!");
                } else {
                    System.out.println("An account with this email already exists. Please log in.");
                }
            } catch (SQLException e) {
                System.out.println("A database error occurred during registration.");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("An unexpected error occurred during registration.");
                e.printStackTrace();
            }
        }
    }

    // Helper method to validate phone number
    private static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber.matches("\\d{10}");
    }

    // Helper method to validate email
    private static boolean isValidEmail(String email) {
        return email.contains("@");
    }

    // Method to handle user sign-out
    private static void signOut() {
        if (currentUser != null) {
            System.out.println(
                    currentUser.getFirstName() + " " + currentUser.getLastName() + " has signed out. Thank you!");
            currentUser = null; // This ensures the user is signed out
        } else {
            System.out.println("No user is currently logged in.");
        }
    }

    // Method to change the role of a user
    private static void changeUserRole(Scanner scanner) throws Exception {
        System.out.print("Enter the email of the user you want to change the role for: ");
        String email = scanner.nextLine();

        System.out.println("Choose a role:");
        System.out.println("1. ADMIN");
        System.out.println("2. MANAGER");
        System.out.println("3. EMPLOYEE");
        System.out.print("Enter your choice: ");

        UserRole newRole;
        try {
            int roleChoice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            newRole = getRoleFromChoice(roleChoice);
        } catch (InputMismatchException ime) {
            System.out.println("Please enter a valid number.");
            scanner.nextLine(); // consume the wrong input
            return;
        }

        if (newRole == null) {
            System.out.println("Invalid role choice.");
            return;
        }

        try (DatabaseManager dbManager = new DatabaseManager()) {
            if (dbManager.userExists(email)) {
                dbManager.updateUserRole(email, newRole);
                System.out.println("Role updated successfully!");
            } else {
                System.out.println("No user found with the given email.");
            }
        } catch (SQLException e) {
            System.out.println("A database error occurred while updating roles.");
            e.printStackTrace();
        }
    }

    // Helper method to convert choice to UserRole
    private static UserRole getRoleFromChoice(int roleChoice) {
        switch (roleChoice) {
            case 1:
                return UserRole.ADMIN;
            case 2:
                return UserRole.MANAGER;
            case 3:
                return UserRole.EMPLOYEE;
            default:
                return null;
        }
    }

    // Method to display the user menu after successful login
    private static void displayUserMenu(Scanner scanner) throws Exception {
        while (currentUser != null) { // Keep displaying the menu until the user signs out
            System.out.println("User Menu:");
            System.out.println("1. View Products");
            System.out.println("2. View Stores");
            System.out.println("3. Add/Delete Products");
            System.out.println("4. Update Inventory");
            System.out.println("5. Purchase order requests");
            System.out.println("6. View My Details");
            System.out.println("7. Check Expired Items");
            System.out.println("8. View Markdown Items");
            System.out.println("9. Sign Out");

            // Show additional options for Admins and Managers
            if (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.MANAGER) {
                System.out.println("10. Role Management");
                System.out.println("11. View All Users");
            }

            System.out.println("12. Exit Application");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // consume newline

            switch (choice) {
                case 1:
                    viewProducts();
                    break;
                case 2:
                    viewAllStores();
                    if (currentUser == null)
                        ; {
                }
                    break;
                case 3:
                    addOrDeleteProducts(scanner);
                    break;
                case 4:
                    updateInventory(scanner);
                    break;
                case 5:
                    orderRequestManager.processOrderRequest();
                    break;
                case 6:
                    viewMyDetails();
                    break;
                case 7:
                    checkExpiredItems();
                    break;
                case 8:
                    displayMarkdownProducts();
                    break;
                case 9:
                    signOut();
                    break;
                case 10:
                    if (currentUser != null && currentUser.getRole() == UserRole.ADMIN) {
                        manageRoles(scanner);
                    } else {
                        System.out.println("Access denied. Only admins can manage roles.");
                    }
                    break;
                case 11:
                    if (currentUser != null
                            && (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.MANAGER)) {
                        viewAllUsers();
                    } else {
                        System.out.println("Access denied. Only admins and managers can view all users.");
                    }
                    break;
                case 12:
                    System.out.println("Exiting application...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
                    break;
            }
        }
    }

    // View products
    {
    }

    private static void viewProducts() {
        try (DatabaseManager dbManager = new DatabaseManager()) {
            List<Product> products = dbManager.getAllProducts(); // Assuming this method exists and returns all products

            if (products == null || products.isEmpty()) {
                System.out.println("No products available.");
            } else {
                // Print table header with Product ID
                System.out.printf("%-20s %-20s %-20s %-20s %-20s %n", "Product ID", "Name", "Quantity", "Price",
                        "category");
                System.out.println(
                        "------------------------------------------------------------------------------------------------");
                for (Product product : products) {
                    // Assuming Product class has getters for the necessary fields
                    System.out.printf("%-20d %-20s %-20d %-20.2f %-20s %n",
                            product.getProductID(),
                            product.getProductName(),
                            product.getQuantity(),
                            product.getPrice(),
                            product.getCategory().toString()); // Assuming getExpiryDate() returns a LocalDate
                }
            }
        } catch (SQLException e) {
            System.out.println("Failed to retrieve products: " + e.getMessage());
            e.printStackTrace();
        }
        // The try-with-resources statement will auto close dbManager, so no need for a
        // finally block to close it.
    }

    // View all stores with green text
    // View all stores
    private static void viewAllStores() throws SQLException {
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            System.out.println("Access denied. This feature is only available to admins.");
            return;
        }

        try (DatabaseManager dbManager = new DatabaseManager()) {
            List<Store> stores = dbManager.getAllStores();
            if (stores.isEmpty()) {
                System.out.println("No stores found.");
            } else {
                System.out.println("Stores List:");
                for (Store store : stores) {
                    System.out.printf(
                            "ID: %d, Name: %s, Address: %s, City: %s, State: %s, ZIP: %d, Phone: %s, Type: %s, Opening Date: %s%n",
                            store.getStoreId(),
                            store.getStoreName(),
                            store.getAddress(),
                            store.getCity(),
                            store.getState(),
                            store.getZip(),
                            store.getPhone(),
                            store.getStoreType(),
                            store.getOpeningDate());
                }
            }
        }
    }

    // View all users
    private static void viewAllUsers() throws SQLException {
        if (currentUser == null
                || (currentUser.getRole() != UserRole.ADMIN && currentUser.getRole() != UserRole.MANAGER)) {
            System.out.println("Access denied. This feature is only available to admins and managers.");
            return;
        }

        try (DatabaseManager dbManager = new DatabaseManager()) {
            List<User> users = dbManager.getAllUsers();
            if (users.isEmpty()) {
                System.out.println("No users found.");
            } else {
                System.out.println("Users List:");
                for (User user : users) {
                    System.out.printf("ID: %d, Name: %s %s, Email: %s, Phone: %s, Role: %s%n",
                            user.getUserId(),
                            user.getFirstName(),
                            user.getLastName(),
                            user.getEmail(),
                            user.getPhoneNumber(),
                            user.getRole());

                }
            }
        }
    }

    private static void manageRoles(Scanner scanner) {
        // Check if the current user is an admin before proceeding
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            System.out.println("Access denied. Only admins can manage roles.");
            return;
        }

        System.out.println("Role Management:");
        System.out.println("1. Grant Manager Role");
        // ... any other role management options can be added here
        System.out.println("2. Return to User Menu");

        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline

        switch (choice) {
            case 1:
                grantManagerRole(scanner);
                break;
            case 2:
                // Simply break out of this switch to return to the user menu
                break;
            default:
                System.out.println("Invalid option. Please try again.");
                break;
        }
    }

    private static void grantManagerRole(Scanner scanner) {
        // Check if the current user is an admin before proceeding
        if (currentUser == null || currentUser.getRole() != UserRole.ADMIN) {
            System.out.println("Access denied. Only admins can grant roles.");
            return;
        }

        System.out.print("Enter the email of the user to grant Manager role to: ");
        String email = scanner.nextLine();
        // Assume dbManager is your database manager instance or however you interact
        // with your database
        try (DatabaseManager dbManager = new DatabaseManager()) {
            User user = dbManager.getUserByEmail(email);
            if (user != null && !user.getRole().equals(UserRole.MANAGER)) {
                dbManager.updateUserRole(email, UserRole.MANAGER); // Corrected line
                System.out.println("Manager role granted to user with email: " + email);
            } else {
                System.out.println("User not found or already a manager.");
            }
        } catch (SQLException e) {
            System.out.println("A database error occurred. Please try again later.");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    private static void viewMyDetails() {
        if (currentUser == null) {
            System.out.println("No user is currently logged in.");
            return;
        }

        // ANSI escape code for blue text
        String ANSI_BLUE = "\u001B[34m";
        // ANSI escape code to reset text color
        String ANSI_RESET = "\u001B[0m";

        System.out.println(ANSI_BLUE + "User Details:" + ANSI_RESET);
        System.out.println(ANSI_BLUE + "First Name: " + currentUser.getFirstName() + ANSI_RESET);
        System.out.println(ANSI_BLUE + "Last Name: " + currentUser.getLastName() + ANSI_RESET);
        System.out.println(ANSI_BLUE + "Email: " + currentUser.getEmail() + ANSI_RESET);
        System.out.println(ANSI_BLUE + "Role: " + currentUser.getRole() + ANSI_RESET);
        // You can add more details if needed
    }

    // Helper method to prompt for a date
    private static LocalDate promptForDate(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        if (!input.isEmpty()) {
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }
        return null;
    }

    // Helper method to prompt for an integer
    private static int promptForInt(Scanner scanner, String errorMessage) {
        while (true) {
            try {
                return scanner.nextInt();
            } catch (InputMismatchException e) {
                System.out.println(errorMessage);
                scanner.next(); // Consume the invalid input
            }
        }
    }

    // Helper method to prompt for a double
    private static double promptForDouble(Scanner scanner, String errorMessage) {
        while (true) {
            try {
                return scanner.nextDouble();
            } catch (InputMismatchException e) {
                System.out.println(errorMessage);
                scanner.next(); // Consume the invalid input
            }
        }
    }

    // Method to add products
    public static void addProduct(Scanner scanner) {
        System.out.println("Adding a new product...");

        // Prompt user for product details
        System.out.print("Enter product name: ");
        String productName = scanner.nextLine();

        LocalDate expirationDate = promptForDate(scanner,
                "Enter expiration date (YYYY-MM-DD) or press Enter if none: ");
        LocalDate markdownDate = promptForDate(scanner, "Enter markdown date (YYYY-MM-DD) or press Enter if none: ");

        System.out.print("Enter quantity: ");
        int quantity = promptForInt(scanner, "Invalid input for quantity.");
        scanner.nextLine(); // Consume the newline after integer input

        System.out.print("Enter manufacturer: ");
        String manufacturer = scanner.nextLine();

        System.out.print("Enter brand: ");
        String brand = scanner.nextLine();

        System.out.print("Enter category: ");
        String category = scanner.nextLine();

        System.out.print("Enter price: ");
        double price = promptForDouble(scanner, "Invalid input for price.");
        scanner.nextLine(); // Consume the newline after double input

        Product product = new Product(productName, expirationDate, markdownDate, quantity, manufacturer, brand, price,
                category);

        // Insert the product into the database
        try (DatabaseManager dbManager = new DatabaseManager()) {
            try (Connection connection = dbManager.getConnection()) {
                connection.setAutoCommit(false);

                String insertSQL = "INSERT INTO product (productName, expirationDate, markdownDate, quantity, manufacturer, brand, price, category) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setString(1, product.getProductName());
                    stmt.setDate(2, expirationDate != null ? Date.valueOf(expirationDate) : null);
                    stmt.setDate(3, markdownDate != null ? Date.valueOf(markdownDate) : null);
                    stmt.setInt(4, product.getQuantity());
                    stmt.setString(5, product.getManufacturer());
                    stmt.setString(6, product.getBrand());
                    stmt.setDouble(7, product.getPrice());
                    stmt.setString(8, product.getCategory());

                    int affectedRows = stmt.executeUpdate();
                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                product.setProductID(generatedKeys.getInt(1));
                                System.out.println("Product added successfully with ID: " + product.getProductID());
                            }
                        }
                    }
                    connection.commit(); // Commit the transaction
                } catch (SQLException e) {
                    connection.rollback(); // Rollback the transaction in case of an error
                    System.err.println("SQL error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Implement the handleUserMenuChoice method to handle the user's menu selection
    private static void handleUserMenuChoice(Scanner scanner, int choice, StoreDAO storeDAO) throws Exception {
        switch (choice) {
            case 1:
                // View products
                break;
            case 2:// View stores
                List<Store> stores = storeDAO.getAllStores();
                displayStores(stores);
                break;
            case 3:
                // Add/Delete Products
                addOrDeleteProducts(scanner);
                break;
            case 4:
                // Update Inventory
                updateInventory(scanner);
                break;
            // Add cases for other menu options as needed
            default:
                System.out.println("Invalid option. Please try again.");
                break;
        }
    }

    // ...

    private static void displayStores(List<Store> stores) {
    }

    // Method to delete product
    private static void deleteProduct(Scanner scanner) throws Exception {
        System.out.print("Enter Product ID to delete: ");
        int productID = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        try (DatabaseManager dbManager = new DatabaseManager()) {
            dbManager.deleteProduct(productID);
            System.out.println("Product deleted successfully.");
        } catch (SQLException e) {
            System.out.println("A database error occurred while deleting the product.");
            e.printStackTrace();
        }
    }

    private static void checkExpiredItems() throws SQLException {
        DatabaseManager dbManager = null;
        try {
            dbManager = new DatabaseManager();
            List<Product> expiredProducts = dbManager.getExpiredProducts();

            if (expiredProducts.isEmpty()) {
                System.out.println("No expired products.");
            } else {
                System.out.println("Expired Products:");
                for (Product product : expiredProducts) {
                    System.out.printf("\u001B[31m Product ID: %d, Name: %s, Expiry Date: %s \u001B[0m%n",
                            product.getProductID(),
                            product.getProductName(),
                            product.getExpirationDate());
                }
            }
        } finally {
            if (dbManager != null) {
                try {
                    dbManager.close();
                } catch (SQLException e) {
                    System.out.println("Failed to close database connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public static void displayMarkdownProducts() throws SQLException {
        DatabaseManager dbManager = null;
        try {
            dbManager = new DatabaseManager();
            List<Product> markdownProducts = dbManager.getMarkdownProducts(); // Corrected this line

            if (markdownProducts == null || markdownProducts.isEmpty()) {
                System.out.println("There are no markdown products at this time.");
            } else {
                System.out.println("\u001B[33mMarkdown Products:\u001B[0m");
                for (Product product : markdownProducts) {
                    System.out.printf(
                            "\u001B[33mProduct ID: %d, Name: %s, Expiry Date: %s, Markdown Date: %s\u001B[0m\n",
                            product.getProductID(),
                            product.getProductName(),
                            product.getExpirationDate(),
                            product.getMarkdownDate());
                }
            }
        } finally {
            if (dbManager != null) {
                try {
                    dbManager.close();
                } catch (SQLException e) {
                    System.out.println("Failed to close database connection: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    private static void exitApplication(Scanner scanner) {
        System.out.println("Exiting the application...");
        scanner.close(); // Close the scanner
        System.exit(0); // Exit the program
    }

    {
    }
}