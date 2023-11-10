package databasemanager;

import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

public class purchaseOrderRequests implements PurchaseOrderRequestManager {
    private Scanner scanner;

    public purchaseOrderRequests(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public void processOrderRequest() {
        System.out.println("Enter the item number: ");
        int itemNumber = scanner.nextInt();

        System.out.println("Enter the quantity: ");
        int quantity = scanner.nextInt();

        String trackingNumber = generateRandomTrackingNumber();
        System.out.println("Your order has been placed. Tracking number: " + trackingNumber);
        // Further logic to process the order can be added here
    }

    private String generateRandomTrackingNumber() {
        return String.format("%09d", ThreadLocalRandom.current().nextInt(1000000000));
    }
}
