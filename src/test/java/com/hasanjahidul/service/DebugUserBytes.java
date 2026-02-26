package com.hasanjahidul.service;

import com.hasanjahidul.protocol.ZKTecoCommand;
import com.hasanjahidul.protocol.ZKTecoProtocol;
import com.hasanjahidul.util.PacketUtil;

/**
 * Debug tool to dump raw user data bytes
 */
public class DebugUserBytes {

    public static void main(String[] args) {
        String deviceIp = "172.22.22.37";
        ZKTecoProtocol protocol = new ZKTecoProtocol(deviceIp);
        
        try {
            if (!protocol.connect()) {
                System.out.println("Failed to connect");
                return;
            }
            
            System.out.println("Connected successfully");
            
            protocol.disableDevice();
            
            byte[] response = protocol.sendCommand(ZKTecoCommand.CMD_USER_TEMP_RRQ, new byte[]{ZKTecoCommand.FCT_USER});
            
            int[] header = PacketUtil.parseHeader(response);
            if (header == null || header[0] != ZKTecoCommand.CMD_PREPARE_DATA) {
                System.out.println("No data prepared");
                return;
            }
            
            byte[] allData = protocol.receiveDataPackets();
            System.out.println("Total data size: " + allData.length);
            
            // Skip first 11 bytes
            if (allData.length > 11) {
                byte[] userData = new byte[allData.length - 11];
                System.arraycopy(allData, 11, userData, 0, userData.length);
                
                System.out.println("User data size: " + userData.length);
                System.out.println("Number of 72-byte records: " + (userData.length / 72));
                
                // Dump first record in hex
                if (userData.length >= 72) {
                    System.out.println("\nFirst user record (72 bytes) in hex:");
                    for (int i = 0; i < 72; i++) {
                        System.out.printf("%02X ", userData[i] & 0xFF);
                        if ((i + 1) % 16 == 0) System.out.println();
                    }
                    System.out.println();
                    
                    // Parse it manually
                    System.out.println("\nManual parsing:");
                    System.out.println("Byte 0: " + (userData[0] & 0xFF));
                    System.out.println("Byte 1 (u1): " + (userData[1] & 0xFF));
                    System.out.println("Byte 2 (u2): " + (userData[2] & 0xFF));
                    System.out.println("UID: " + ((userData[1] & 0xFF) + ((userData[2] & 0xFF) * 256)));
                    System.out.println("Byte 3 (role): " + (userData[3] & 0xFF));
                    
                    // Password (bytes 4-11)
                    System.out.print("Password bytes (4-11): ");
                    for (int i = 4; i < 12; i++) {
                        System.out.printf("%02X ", userData[i] & 0xFF);
                    }
                    System.out.println();
                    
                    // Name (bytes 12-35)
                    System.out.print("Name bytes (12-35): ");
                    StringBuilder name = new StringBuilder();
                    for (int i = 12; i < 36; i++) {
                        byte b = userData[i];
                        if (b == 0) break;
                        name.append((char) b);
                    }
                    System.out.println("= \"" + name + "\"");
                    
                    // Card (bytes 36-39)
                    System.out.print("Card bytes (36-39): ");
                    for (int i = 36; i < 40; i++) {
                        System.out.printf("%02X ", userData[i] & 0xFF);
                    }
                    System.out.println();
                    
                    // User ID (bytes 49-57)
                    System.out.print("UserID bytes (49-57): ");
                    StringBuilder userId = new StringBuilder();
                    for (int i = 49; i < 58; i++) {
                        byte b = userData[i];
                        if (b == 0) break;
                        userId.append((char) b);
                    }
                    System.out.println("= \"" + userId + "\"");
                }
            }
            
            protocol.enableDevice();
            protocol.disconnect();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
