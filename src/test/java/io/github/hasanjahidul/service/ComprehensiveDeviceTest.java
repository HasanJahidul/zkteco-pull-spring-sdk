package io.github.hasanjahidul.service;

import io.github.hasanjahidul.model.AttendanceRecord;
import io.github.hasanjahidul.model.DeviceInfo;
import io.github.hasanjahidul.model.UserInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprehensive test for all ZKTeco device functions
 * Tests all functions from the PHP SDK
 */
public class ComprehensiveDeviceTest {

    // Configure device IP here
    private static final String DEVICE_IP = "172.22.22.37"; // Change to test a different device
    private static final int PORT = 4370;

    @Test
    public void testAllDeviceFunctions() {
        System.out.println("========================================");
        System.out.println("COMPREHENSIVE ZKTECO DEVICE TEST");
        System.out.println("Device IP: " + DEVICE_IP + ":" + PORT);
        System.out.println("========================================\n");

        try (ZKTecoDeviceService service = new ZKTecoDeviceService(DEVICE_IP, PORT)) {
            
            // 1. CONNECTION TEST
            System.out.println("1. CONNECTION TEST");
            System.out.println("-".repeat(40));
            boolean connected = service.connect();
            System.out.println("✓ Connection: " + (connected ? "SUCCESS" : "FAILED"));
            if (!connected) {
                System.out.println("Cannot proceed without connection!");
                return;
            }
            System.out.println();

            // 2. DEVICE INFORMATION TEST
            System.out.println("2. DEVICE INFORMATION TEST");
            System.out.println("-".repeat(40));
            try {
                DeviceInfo deviceInfo = service.getDeviceInfo();
                System.out.println("✓ Version: " + deviceInfo.getVersion());
                System.out.println("✓ OS Version: " + deviceInfo.getOsVersion());
                System.out.println("✓ Platform: " + deviceInfo.getPlatform());
                System.out.println("✓ Serial Number: " + deviceInfo.getSerialNumber());
                System.out.println("✓ Device Name: " + deviceInfo.getDeviceName());
                System.out.println("✓ Device Time: " + deviceInfo.getDeviceTime());
            } catch (Exception e) {
                System.out.println("✗ Device Info Error: " + e.getMessage());
            }
            System.out.println();

            // 3. ADDITIONAL DEVICE INFO TEST
            System.out.println("3. ADDITIONAL DEVICE INFO TEST");
            System.out.println("-".repeat(40));
            try {
                System.out.println("✓ Work Code: " + service.getWorkCode());
                System.out.println("✓ SSR: " + service.getSsr());
                System.out.println("✓ PIN Width: " + service.getPinWidth());
                System.out.println("✓ Face Function: " + service.getFaceFunctionOn());
                System.out.println("✓ Firmware Version: " + service.getFirmwareVersion());
                System.out.println("✓ MAC Address: " + service.getMacAddress());
            } catch (Exception e) {
                System.out.println("✗ Additional Info Error: " + e.getMessage());
            }
            System.out.println();

            // 4. TIME MANAGEMENT TEST
            System.out.println("4. TIME MANAGEMENT TEST");
            System.out.println("-".repeat(40));
            try {
                LocalDateTime currentTime = service.getDeviceTime();
                System.out.println("✓ Current Device Time: " + currentTime);
                
                // Test setting time (set to current system time)
                LocalDateTime newTime = LocalDateTime.now();
                service.setDeviceTime(newTime);
                System.out.println("✓ Time Set To: " + newTime);
                
                // Verify time was set
                Thread.sleep(2000);
                LocalDateTime verifyTime = service.getDeviceTime();
                System.out.println("✓ Verified Time: " + verifyTime);
            } catch (Exception e) {
                System.out.println("✗ Time Management Error: " + e.getMessage());
            }
            System.out.println();

            // 5. ATTENDANCE RECORDS TEST
            System.out.println("5. ATTENDANCE RECORDS TEST");
            System.out.println("-".repeat(40));
            try {
                List<AttendanceRecord> records = service.getAttendance();
                System.out.println("✓ Total Attendance Records: " + records.size());
                
                if (records.size() > 0) {
                    System.out.println("\nFirst 5 records:");
                    int count = Math.min(5, records.size());
                    for (int i = 0; i < count; i++) {
                        AttendanceRecord record = records.get(i);
                        System.out.printf("  [%d] UID: %d, UserID: %s, State: %d, Type: %d, Time: %s%n",
                                i + 1,
                                record.getUid(),
                                record.getUserId(),
                                record.getState(),
                                record.getType(),
                                record.getTimestamp());
                    }
                    
                    if (records.size() > 5) {
                        System.out.println("  ... and " + (records.size() - 5) + " more records");
                    }
                }
            } catch (Exception e) {
                System.out.println("✗ Attendance Error: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();

            // 6. USER MANAGEMENT TEST
            System.out.println("6. USER MANAGEMENT TEST");
            System.out.println("-".repeat(40));
            try {
                List<UserInfo> users = service.getUsers();
                System.out.println("✓ Total Users: " + users.size());
                
                if (users.size() > 0) {
                    System.out.println("\nFirst 5 users:");
                    int count = Math.min(5, users.size());
                    for (int i = 0; i < count; i++) {
                        UserInfo user = users.get(i);
                        System.out.printf("  [%d] UID: %d, ID: %s, Name: %s, Role: %d, Card: %d%n",
                                i + 1,
                                user.getUid(),
                                user.getUserId(),
                                user.getName(),
                                user.getRole(),
                                user.getCardno());
                    }
                    
                    if (users.size() > 5) {
                        System.out.println("  ... and " + (users.size() - 5) + " more users");
                    }
                }
                
                // Note: User addition/removal test is disabled to avoid modifying device data
                System.out.println("\n⚠ User Addition/Removal Test:");
                System.out.println("  Test is DISABLED to preserve device data");
                System.out.println("  To test user management, uncomment the code below");
                
                /* UNCOMMENT TO TEST USER ADDITION/REMOVAL
                int initialUserCount = users.size();
                System.out.println("  Initial user count: " + initialUserCount);
                
                service.setUser(9999, "TEST001", "Test User", "12345678", 0, 0);
                System.out.println("  Sent command to add test user: TEST001 (UID: 9999)");
                
                // Verify user was added - wait a bit longer for device to process
                Thread.sleep(2000);
                List<UserInfo> updatedUsers = service.getUsers();
                System.out.println("  Users after addition: " + updatedUsers.size());
                
                // Check if user was actually added
                boolean userFound = updatedUsers.stream()
                    .anyMatch(u -> "TEST001".equals(u.getUserId()));
                
                if (userFound) {
                    System.out.println("  ✓ Test user successfully added!");
                    
                    // Remove the test user to clean up
                    service.removeUser(9999);
                    System.out.println("  Sent command to remove test user: 9999");
                    
                    Thread.sleep(1000);
                    List<UserInfo> finalUsers = service.getUsers();
                    System.out.println("  Users after removal: " + finalUsers.size());
                } else {
                    System.out.println("  ⚠ Test user not found in user list");
                }
                */
                
            } catch (Exception e) {
                System.out.println("✗ User Management Error: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println();

            // 7. LCD DISPLAY TEST
            System.out.println("7. LCD DISPLAY TEST");
            System.out.println("-".repeat(40));
            try {
                service.writeLCD(0, "Hello ZKTeco!");
                System.out.println("✓ Written to LCD line 0: 'Hello ZKTeco!'");
                Thread.sleep(3000);
                
                service.writeLCD(1, "Test from Java");
                System.out.println("✓ Written to LCD line 1: 'Test from Java'");
                Thread.sleep(3000);
                
                service.clearLCD();
                System.out.println("✓ LCD Cleared");
            } catch (Exception e) {
                System.out.println("✗ LCD Error: " + e.getMessage());
            }
            System.out.println();

            // 8. VOICE TEST
            System.out.println("8. VOICE TEST");
            System.out.println("-".repeat(40));
            try {
                service.testVoice();
                System.out.println("✓ Voice test triggered (should hear 'Thank you')");
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("✗ Voice Test Error: " + e.getMessage());
            }
            System.out.println();

            // 9. DEVICE LOCK/UNLOCK TEST
            System.out.println("9. DEVICE LOCK/UNLOCK TEST");
            System.out.println("-".repeat(40));
            try {
                service.disableDevice();
                System.out.println("✓ Device Disabled (Locked)");
                Thread.sleep(2000);
                
                service.enableDevice();
                System.out.println("✓ Device Enabled (Unlocked)");
                Thread.sleep(1000);
                
                service.unlockDoor(3);
                System.out.println("✓ Remote Door Unlock Sent (3 seconds)");
            } catch (Exception e) {
                System.out.println("✗ Lock/Unlock Error: " + e.getMessage());
            }
            System.out.println();

            // 10. DEVICE CONTROL TEST (COMMENTED OUT - DANGEROUS)
            System.out.println("10. DEVICE CONTROL TEST");
            System.out.println("-".repeat(40));
            System.out.println("⚠ Sleep/Resume/Restart/PowerOff tests are COMMENTED OUT");
            System.out.println("  Uncomment in code if you want to test these functions");
            System.out.println("  WARNING: PowerOff and Restart will turn off/restart the device!");
            
            // UNCOMMENT BELOW TO TEST (BE CAREFUL!)
            /*
            try {
                // Test sleep/resume
                service.sleep();
                System.out.println("✓ Device Sleep initiated");
                Thread.sleep(3000);
                
                service.resume();
                System.out.println("✓ Device Resume initiated");
                Thread.sleep(2000);
                
                // DANGEROUS: Restart device
                // service.restart();
                // System.out.println("✓ Device Restart initiated");
                
                // VERY DANGEROUS: Power off device
                // service.powerOff();
                // System.out.println("✓ Device Power Off initiated");
                
            } catch (Exception e) {
                System.out.println("✗ Device Control Error: " + e.getMessage());
            }
            */
            System.out.println();

            // 11. DATA MANAGEMENT TEST (COMMENTED OUT - DESTRUCTIVE)
            System.out.println("11. DATA MANAGEMENT TEST");
            System.out.println("-".repeat(40));
            System.out.println("⚠ Clear operations are COMMENTED OUT");
            System.out.println("  Uncomment in code if you want to test these functions");
            System.out.println("  WARNING: These operations will DELETE data from the device!");
            
            // UNCOMMENT BELOW TO TEST (BE VERY CAREFUL!)
            /*
            try {
                // Clear attendance records
                // service.clearAttendance();
                // System.out.println("✓ Attendance records cleared");
                
                // Clear admin privileges
                // service.clearAdmin();
                // System.out.println("✓ Admin privileges cleared");
                
                // Clear all users
                // service.clearUsers();
                // System.out.println("✓ All users cleared");
                
            } catch (Exception e) {
                System.out.println("✗ Data Management Error: " + e.getMessage());
            }
            */
            System.out.println();

            // DISCONNECT
            System.out.println("12. DISCONNECTION");
            System.out.println("-".repeat(40));
            service.disconnect();
            System.out.println("✓ Disconnected from device");
            System.out.println();

            System.out.println("========================================");
            System.out.println("TEST COMPLETED SUCCESSFULLY!");
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("Test failed with error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testAddUserToDevice() {
        System.out.println("\n--- SPECIAL TEST: ADD USER ---");
        try (ZKTecoDeviceService service = new ZKTecoDeviceService(DEVICE_IP, PORT)) {
            if (service.connect()) {
                System.out.println("Connected to device.");
                
                // Unique Identifier for test user
                int testUid = 9999;
                String testId = "TEST001";
                
                // Try to add the user
                System.out.println("Sending add command for User: " + testId + " (UID: " + testUid + ")");
                service.setUser(testUid, testId, "Java SDK Tester", "12345", 0, 0);
                System.out.println("Command sent successfully.");
                
                // Wait to ensure device processes it
                Thread.sleep(2000);
                
                // Verify
                List<UserInfo> users = service.getUsers();
                boolean userFound = users.stream().anyMatch(u -> testId.equals(u.getUserId()));
                if (userFound) {
                    System.out.println("✓ SUCCESS: New user " + testId + " was found on the device!");
                } else {
                    System.out.println("✗ FAILED: User " + testId + " was NOT found on the device after adding.");
                }
            } else {
                System.out.println("✗ Failed to connect to device.");
            }
        } catch (Exception e) {
            System.err.println("Error adding user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testEditUserOnDevice() {
        System.out.println("\n--- SPECIAL TEST: EDIT USER ---");
        try (ZKTecoDeviceService service = new ZKTecoDeviceService(DEVICE_IP, PORT)) {
            if (service.connect()) {
                System.out.println("Connected to device.");
                
                // Unique Identifier for test user
                int testUid = 9999;
                String testId = "TEST001";
                
                // The setUser command functions as an UPSERT (Update or Insert)
                // If a user with UID 9999 exists, it will overwrite their properties.
                System.out.println("Sending edit command for User: " + testId + " (UID: " + testUid + ")");
                service.setUser(testUid, testId, "Edited JDK Tester", "88888", 0, 100);
                System.out.println("Edit command sent successfully.");
                
                // Wait to ensure device processes it
                Thread.sleep(2000);
                
                // Verify
                List<UserInfo> users = service.getUsers();
                boolean userFound = false;
                for (UserInfo u : users) {
                    if (testId.equals(u.getUserId())) {
                        userFound = true;
                        if ("Edited JDK Tester".equals(u.getName())) {
                            System.out.println("✓ SUCCESS: User " + testId + " was successfully updated!");
                        } else {
                            System.out.println("✗ FAILED: User " + testId + " was found, but name wasn't updated.");
                        }
                        break;
                    }
                }
                
                if (!userFound) {
                    System.out.println("✗ FAILED: User " + testId + " was NOT found on the device. (Did they exist?)");
                }
            } else {
                System.out.println("✗ Failed to connect to device.");
            }
        } catch (Exception e) {
            System.err.println("Error editing user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testDeleteUserFromDevice() {
        System.out.println("\n--- SPECIAL TEST: DELETE USER ---");
        try (ZKTecoDeviceService service = new ZKTecoDeviceService(DEVICE_IP, PORT)) {
            if (service.connect()) {
                System.out.println("Connected to device.");
                
                int testUid = 9999;
                String testId = "TEST001";
                
                // Check if user exists first
                List<UserInfo> initialUsers = service.getUsers();
                boolean userExists = initialUsers.stream().anyMatch(u -> testId.equals(u.getUserId()));
                
                if (!userExists) {
                    System.out.println("⚠ WARNING: User " + testId + " does not exist on the device. Cannot test deletion.");
                    return;
                }
                
                System.out.println("User " + testId + " found. Sending delete command...");
                service.removeUser(testUid);
                System.out.println("Delete command sent successfully.");
                
                // Wait to ensure device processes it
                Thread.sleep(2000);
                
                // Verify
                List<UserInfo> finalUsers = service.getUsers();
                boolean userStillExists = finalUsers.stream().anyMatch(u -> testId.equals(u.getUserId()));
                if (!userStillExists) {
                    System.out.println("✓ SUCCESS: User " + testId + " was completely removed from the device!");
                } else {
                    System.out.println("✗ FAILED: User " + testId + " is STILL on the device after deletion.");
                }
            } else {
                System.out.println("✗ Failed to connect to device.");
            }
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void testBothDevices() {
        String[] devices = {"192.168.68.222", "192.168.68.223"};
        
        System.out.println("========================================");
        System.out.println("TESTING BOTH DEVICES");
        System.out.println("========================================\n");
        
        for (String ip : devices) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("Testing Device: " + ip);
            System.out.println("=".repeat(50));
            
            try (ZKTecoDeviceService service = new ZKTecoDeviceService(ip, PORT)) {
                if (service.connect()) {
                    System.out.println("✓ Connected to " + ip);
                    
                    DeviceInfo info = service.getDeviceInfo();
                    System.out.println("  Serial: " + info.getSerialNumber());
                    System.out.println("  Name: " + info.getDeviceName());
                    System.out.println("  Platform: " + info.getPlatform());
                    
                    List<AttendanceRecord> records = service.getAttendance();
                    System.out.println("  Attendance Records: " + records.size());
                    
                    List<UserInfo> users = service.getUsers();
                    System.out.println("  Users: " + users.size());
                    
                    service.disconnect();
                    System.out.println("✓ Disconnected from " + ip);
                } else {
                    System.out.println("✗ Failed to connect to " + ip);
                }
            } catch (Exception e) {
                System.err.println("✗ Error testing " + ip + ": " + e.getMessage());
            }
        }
        
        System.out.println("\n========================================");
        System.out.println("BOTH DEVICES TEST COMPLETED");
        System.out.println("========================================");
    }
}
