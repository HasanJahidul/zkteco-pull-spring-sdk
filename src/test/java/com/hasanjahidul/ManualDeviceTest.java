package com.hasanjahidul;

import com.hasanjahidul.model.AttendanceRecord;
import com.hasanjahidul.model.DeviceInfo;
import com.hasanjahidul.service.ZKTecoDeviceService;

import java.util.List;

/**
 * Manual test class for testing with actual ZKTeco device
 * Run this directly to test connectivity with your device at 172.22.22.38
 */
public class ManualDeviceTest {

    public static void main(String[] args) {
        // Create service instance with your device IP
        // You can pass the IP as a command line argument or use default
        String deviceIp = args.length > 0 ? args[0] : "172.22.22.37";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 4370;
        
        System.out.println("=".repeat(60));
        System.out.println("Testing ZKTeco Device Connection");
        System.out.println("Device IP: " + deviceIp);
        System.out.println("Port: " + port);
        System.out.println("=".repeat(60));
        
        try (ZKTecoDeviceService service = new ZKTecoDeviceService(deviceIp, port)) {
            // Test 1: Connect to device
            System.out.println("\n[TEST 1] Connecting to device...");
            boolean connected = service.connect();
            if (connected) {
                System.out.println("✓ Successfully connected to device!");
            } else {
                System.out.println("✗ Failed to connect to device");
                return;
            }
            
            // Test 2: Get device info
            System.out.println("\n[TEST 2] Getting device information...");
            DeviceInfo deviceInfo = service.getDeviceInfo();
            if (deviceInfo != null) {
                System.out.println("✓ Device Information Retrieved:");
                System.out.println("  - Serial Number: " + deviceInfo.getSerialNumber());
                System.out.println("  - Version: " + deviceInfo.getVersion());
                System.out.println("  - OS Version: " + deviceInfo.getOsVersion());
                System.out.println("  - Device Name: " + deviceInfo.getDeviceName());
                System.out.println("  - Platform: " + deviceInfo.getPlatform());
                System.out.println("  - Device Time: " + deviceInfo.getDeviceTime());
                System.out.println("  - User Count: " + deviceInfo.getUserCount());
                System.out.println("  - Attendance Count: " + deviceInfo.getAttendanceCount());
                System.out.println("  - Fingerprint Count: " + deviceInfo.getFingerprintCount());
                System.out.println("  - Face Count: " + deviceInfo.getFaceCount());
            } else {
                System.out.println("✗ Failed to get device information");
            }
            
            // Test 3: Get attendance records
            System.out.println("\n[TEST 3] Fetching attendance records...");
            List<AttendanceRecord> records = service.getAttendance();
            if (records != null) {
                System.out.println("✓ Found " + records.size() + " attendance records");
                
                if (!records.isEmpty()) {
                    System.out.println("\nShowing first 10 records:");
                    int count = Math.min(10, records.size());
                    for (int i = 0; i < count; i++) {
                        AttendanceRecord record = records.get(i);
                        System.out.println("  " + (i + 1) + ". User ID: " + record.getUserId() + 
                                         ", Time: " + record.getTimestamp() + 
                                         ", State: " + record.getState() + 
                                         ", Type: " + record.getType());
                    }
                    
                    if (records.size() > 10) {
                        System.out.println("  ... and " + (records.size() - 10) + " more records");
                    }
                }
            } else {
                System.out.println("✗ Failed to get attendance records");
            }
            
            // Test 4: Clear attendance records (commented out for safety)
            // Uncomment only if you want to clear the records
            /*
            System.out.println("\n[TEST 4] Clearing attendance records...");
            boolean cleared = service.clearAttendanceRecords();
            if (cleared) {
                System.out.println("✓ Attendance records cleared successfully");
            } else {
                System.out.println("✗ Failed to clear attendance records");
            }
            */
            
            // Test 5: Disconnect
            System.out.println("\n[TEST 5] Disconnecting from device...");
            service.disconnect();
            System.out.println("✓ Disconnected from device");
            
            System.out.println("\n" + "=".repeat(60));
            System.out.println("All tests completed!");
            System.out.println("=".repeat(60));
            
        } catch (Exception e) {
            System.err.println("\n✗ Error occurred during testing:");
            e.printStackTrace();
        }
    }
}
