package com.hasanjahidul.service;

import com.hasanjahidul.model.UserInfo;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Simple test focused on user retrieval
 */
public class UserRetrievalTest {

    private static final String DEVICE_IP = "172.22.22.37";

    @Test
    public void testGetUsers() {
        System.out.println("\n========================================");
        System.out.println("USER RETRIEVAL TEST");
        System.out.println("========================================\n");

        ZKTecoDeviceService service = null;
        try {
            service = new ZKTecoDeviceService(DEVICE_IP);
            boolean connected = service.connect();
            
            if (!connected) {
                System.out.println("✗ Failed to connect to device");
                return;
            }
            
            System.out.println("✓ Connected to device: " + DEVICE_IP);
            
            List<UserInfo> users = service.getUsers();
            
            System.out.println("✓ Total Users Retrieved: " + users.size());
            
            if (!users.isEmpty()) {
                System.out.println("\nFirst 10 users:");
                int count = Math.min(10, users.size());
                for (int i = 0; i < count; i++) {
                    UserInfo user = users.get(i);
                    System.out.printf("  [%d] UID: %d, UserID: %s, Name: %s, Role: %d, Card: %d%n",
                            i + 1,
                            user.getUid(),
                            user.getUserId(),
                            user.getName(),
                            user.getRole(),
                            user.getCardno());
                }
                
                if (users.size() > 10) {
                    System.out.println("  ... and " + (users.size() - 10) + " more users");
                }
            } else {
                System.out.println("⚠ No users found on device");
            }
            
        } catch (Exception e) {
            System.out.println("✗ Error retrieving users: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (service != null) {
                service.disconnect();
                System.out.println("\n✓ Disconnected from device");
            }
        }
    }
}
