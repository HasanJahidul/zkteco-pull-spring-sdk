package com.hasanjahidul;

import com.hasanjahidul.model.AttendanceRecord;
import com.hasanjahidul.model.DeviceInfo;
import com.hasanjahidul.service.ZKTecoDeviceService;

import java.util.List;

/**
 * Manual test class for testing with TWO ZKTeco devices
 * Run this directly to test connectivity with devices at:
 * - 172.22.22.37
 * - 172.22.22.38
 */
public class MultiDeviceTest {

    public static void main(String[] args) {
        String[] deviceIps = {"172.22.22.37", "172.22.22.38"};
        int port = 4370;
        
        System.out.println("=".repeat(70));
        System.out.println("Testing Multiple ZKTeco Devices");
        System.out.println("=".repeat(70));
        
        for (String deviceIp : deviceIps) {
            System.out.println("\n\n" + "=".repeat(70));
            System.out.println("TESTING DEVICE: " + deviceIp + ":" + port);
            System.out.println("=".repeat(70));
            testDevice(deviceIp, port);
        }
        
        System.out.println("\n\n" + "=".repeat(70));
        System.out.println("All devices tested!");
        System.out.println("=".repeat(70));
    }
    
    private static void testDevice(String deviceIp, int port) {
        try (ZKTecoDeviceService service = new ZKTecoDeviceService(deviceIp, port)) {
            
            // Test 1: Connect to device
            System.out.println("\n[TEST 1] Connecting to device " + deviceIp + "...");
            boolean connected = service.connect();
            if (connected) {
                System.out.println("✓ Successfully connected to device!");
            } else {
                System.out.println("✗ Failed to connect to device");
                System.out.println("  Skipping remaining tests for this device.\n");
                return;
            }
            
            // Test 2: Get device info
            System.out.println("\n[TEST 2] Getting device information...");
            try {
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
            } catch (Exception e) {
                System.out.println("✗ Error getting device info: " + e.getMessage());
            }
            
            // Test 3: Get attendance records
            System.out.println("\n[TEST 3] Fetching attendance records...");
            try {
                List<AttendanceRecord> records = service.getAttendance();
                if (records != null) {
                    System.out.println("✓ Found " + records.size() + " attendance records");
                    
                    if (!records.isEmpty()) {
                        System.out.println("\nShowing first 5 records:");
                        int count = Math.min(5, records.size());
                        for (int i = 0; i < count; i++) {
                            AttendanceRecord record = records.get(i);
                            System.out.println("  " + (i + 1) + ". User ID: " + record.getUserId() + 
                                             ", Time: " + record.getTimestamp() + 
                                             ", State: " + record.getState() + 
                                             ", Type: " + record.getType());
                        }
                        
                        if (records.size() > 5) {
                            System.out.println("  ... and " + (records.size() - 5) + " more records");
                        }
                    } else {
                        System.out.println("  No attendance records found on this device");
                    }
                } else {
                    System.out.println("✗ Failed to get attendance records");
                }
            } catch (Exception e) {
                System.out.println("✗ Error getting attendance: " + e.getMessage());
            }
            
            // Test 4: Disconnect
            System.out.println("\n[TEST 4] Disconnecting from device...");
            service.disconnect();
            System.out.println("✓ Disconnected from device");
            
        } catch (Exception e) {
            System.err.println("\n✗ Error occurred during testing device " + deviceIp + ":");
            System.err.println("  " + e.getMessage());
            e.printStackTrace();
        }
    }
}
