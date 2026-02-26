package com.hasanjahidul.service;

import com.hasanjahidul.exception.ZKTecoException;
import com.hasanjahidul.model.AttendanceRecord;
import com.hasanjahidul.model.DeviceInfo;
import com.hasanjahidul.model.UserInfo;
import com.hasanjahidul.protocol.ZKTecoCommand;
import com.hasanjahidul.protocol.ZKTecoProtocol;
import com.hasanjahidul.util.PacketUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Main service for interacting with ZKTeco devices
 */
@Slf4j
public class ZKTecoDeviceService implements AutoCloseable {

    private final ZKTecoProtocol protocol;

    /**
     * Create ZKTeco device service with default port
     *
     * @param ipAddress IP address of the device
     */
    public ZKTecoDeviceService(String ipAddress) {
        this.protocol = new ZKTecoProtocol(ipAddress);
    }

    /**
     * Create ZKTeco device service with a specific protocol instance (used for testing)
     *
     * @param protocol The ZKTecoProtocol to use
     */
    ZKTecoDeviceService(ZKTecoProtocol protocol) {
        this.protocol = protocol;
    }


    /**
     * Create ZKTeco device service with custom port
     *
     * @param ipAddress IP address of the device
     * @param port      Port number (default is 4370)
     */
    public ZKTecoDeviceService(String ipAddress, int port) {
        this.protocol = new ZKTecoProtocol(ipAddress, port);
    }

    /**
     * Connect to device
     *
     * @return true if connection successful
     */
    public boolean connect() {
        return protocol.connect();
    }

    /**
     * Disconnect from device
     */
    public void disconnect() {
        protocol.disconnect();
    }

    /**
     * Check if connected
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return protocol.isConnected();
    }

    /**
     * Get device information
     *
     * @return Device information
     */
    public DeviceInfo getDeviceInfo() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }

        return DeviceInfo.builder()
                .version(protocol.getString(ZKTecoCommand.CMD_GET_VERSION, ""))
                .osVersion(protocol.getString(ZKTecoCommand.CMD_DEVICE, "~OS"))
                .platform(protocol.getString(ZKTecoCommand.CMD_DEVICE, "~Platform"))
                .serialNumber(protocol.getString(ZKTecoCommand.CMD_DEVICE, "~SerialNumber"))
                .deviceName(protocol.getString(ZKTecoCommand.CMD_DEVICE, "~DeviceName"))
                .deviceTime(protocol.getDeviceTime())
                .build();
    }

    /**
     * Get all attendance records from device
     *
     * @return List of attendance records
     */
    public List<AttendanceRecord> getAttendance() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }

        List<AttendanceRecord> records = new ArrayList<>();

        try {
            // Disable device during data transfer
            protocol.disableDevice();

            // Request attendance data (CMD_ATT_LOG_RRQ)
            byte[] response = protocol.sendCommand(ZKTecoCommand.CMD_ATT_LOG_RRQ, null);

            // Check if device is preparing to send data
            int[] header = PacketUtil.parseHeader(response);
            if (header == null || header[0] != ZKTecoCommand.CMD_PREPARE_DATA) {
                log.warn("Device did not prepare data for attendance records");
                return records;
            }

            // Receive all data packets
            byte[] allData = protocol.receiveDataPackets();
            
            if (allData.length > 10) {
                // Skip first 10 bytes as per PHP implementation
                byte[] attData = new byte[allData.length - 10];
                System.arraycopy(allData, 10, attData, 0, attData.length);
                
                records = parseAttendanceData(attData);
            }

            log.info("Retrieved {} attendance records", records.size());
            return records;

        } finally {
            // Re-enable device
            protocol.enableDevice();
        }
    }

    /**
     * Parse attendance data from binary response
     * Matches PHP parsing logic: each record is 40 bytes
     *
     * @param data Binary data
     * @return List of attendance records
     */
    private List<AttendanceRecord> parseAttendanceData(byte[] data) {
        List<AttendanceRecord> records = new ArrayList<>();

        // Each record is exactly 40 bytes (as per PHP implementation)
        int recordSize = 40;
        int offset = 0;

        while (offset + recordSize <= data.length) {
            try {
                // Parse according to PHP logic:
                // Bytes 0-1: blank
                // Bytes 2-3: uid (little endian)
                // Bytes 4-12: user ID (9 bytes, null-terminated)
                // Bytes 13-27: padding/unknown
                // Byte 28: state
                // Bytes 29-32: timestamp (4 bytes, little endian)
                // Byte 33: type
                // Bytes 34-39: padding
                
                // Extract UID (bytes 2-3)
                int u1 = data[offset + 2] & 0xFF;
                int u2 = data[offset + 3] & 0xFF;
                long uid = u1 + (u2 * 256);
                
                // Extract user ID (bytes 4-12, 9 bytes)
                byte[] userIdBytes = new byte[9];
                System.arraycopy(data, offset + 4, userIdBytes, 0, 9);
                String userId = extractString(userIdBytes);
                
                // Extract state (byte 28)
                int state = data[offset + 28] & 0xFF;
                
                // Extract timestamp (bytes 29-32, 4 bytes little endian)
                int timestampEncoded = PacketUtil.bytesToInt(data, offset + 29, 4);
                LocalDateTime timestamp = decodeTime(timestampEncoded);
                
                // Extract type (byte 33)
                int type = data[offset + 33] & 0xFF;

                AttendanceRecord record = AttendanceRecord.builder()
                        .uid(uid)
                        .userId(userId)
                        .timestamp(timestamp)
                        .state(state)
                        .type(type)
                        .build();

                records.add(record);
                offset += recordSize;

            } catch (Exception e) {
                log.warn("Error parsing attendance record at offset {}: {}", offset, e.getMessage());
                offset += recordSize;
            }
        }

        return records;
    }
    
    /**
     * Decode ZKTeco timestamp to LocalDateTime
     * Matches PHP decodeTime() function
     *
     * @param encodedTime Encoded timestamp
     * @return LocalDateTime
     */
    private LocalDateTime decodeTime(int encodedTime) {
        int t = encodedTime;
        
        int second = t % 60;
        t = t / 60;
        
        int minute = t % 60;
        t = t / 60;
        
        int hour = t % 24;
        t = t / 24;
        
        int day = t % 31 + 1;
        t = t / 31;
        
        int month = t % 12 + 1;
        t = t / 12;
        
        int year = t + 2000;
        
        return LocalDateTime.of(year, month, day, hour, minute, second);
    }

    /**
     * Get all users from device
     *
     * @return List of users
     */
    public List<UserInfo> getUsers() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }

        List<UserInfo> users = new ArrayList<>();

        try {
            protocol.disableDevice();

            // Request user data (CMD_USER_TEMP_RRQ with FCT_USER)
            byte[] response = protocol.sendCommand(ZKTecoCommand.CMD_USER_TEMP_RRQ, new byte[]{ZKTecoCommand.FCT_USER});

            // Check if device is preparing to send data
            int[] header = PacketUtil.parseHeader(response);
            if (header == null || header[0] != ZKTecoCommand.CMD_PREPARE_DATA) {
                log.warn("Device did not prepare data for users");
                return users;
            }

            // Receive all data packets (like PHP recData())
            byte[] allData = protocol.receiveDataPackets();
            
            if (allData.length > 11) {
                // Skip first 11 bytes as per PHP implementation
                byte[] userData = new byte[allData.length - 11];
                System.arraycopy(allData, 11, userData, 0, userData.length);
                
                users = parseUserData(userData);
            }

            log.info("Retrieved {} users", users.size());
            return users;

        } finally {
            protocol.enableDevice();
        }
    }

    /**
     * Parse user data from binary response
     * Matches PHP parsing logic: each record is 72 bytes
     * PHP uses unpack('H144') which creates hex string, then extracts by hex position
     *
     * @param data Binary data
     * @return List of users
     */
    private List<UserInfo> parseUserData(byte[] data) {
        List<UserInfo> users = new ArrayList<>();
        int recordSize = 72;
        int offset = 0;

        while (offset + recordSize <= data.length) {
            try {
                // PHP hex positions to byte positions:
                // - hex pos 2-3 = byte 1 (u1)
                // - hex pos 4-5 = byte 2 (u2)
                // - hex pos 6-7 = byte 3 (role)
                // - hex pos 8-23 = bytes 4-11 (password, 8 bytes)
                // - hex pos 24-71 = bytes 12-35 (name, 24 bytes)
                // - hex pos 72-79 = bytes 36-39 (cardno, 4 bytes)
                // - hex pos 98-171 = bytes 49-85 (userid, 9 bytes)
                
                // Extract UID (bytes 1-2, little endian) - PHP skips byte 0
                int u1 = data[offset + 1] & 0xFF;
                int u2 = data[offset + 2] & 0xFF;
                int uid = u1 + (u2 * 256);
                
                // Extract role (byte 3)
                int role = data[offset + 3] & 0xFF;
                
                // Extract password (bytes 4-11, 8 bytes)
                byte[] passwordBytes = new byte[8];
                System.arraycopy(data, offset + 4, passwordBytes, 0, 8);
                String password = extractString(passwordBytes);
                
                // Extract name (bytes 12-35, 24 bytes)
                byte[] nameBytes = new byte[24];
                System.arraycopy(data, offset + 12, nameBytes, 0, 24);
                String name = extractString(nameBytes);
                
                // Extract card number (bytes 36-39, 4 bytes little endian)
                long cardno = PacketUtil.bytesToInt(data, offset + 36, 4) & 0xFFFFFFFFL;
                
                // Extract user ID (bytes 49-57, 9 bytes)
                byte[] userIdBytes = new byte[9];
                System.arraycopy(data, offset + 49, userIdBytes, 0, 9);
                String userId = extractString(userIdBytes);
                
                // If name is empty, use userId as name (as per PHP)
                if (name.isEmpty() && !userId.isEmpty()) {
                    name = userId;
                }

                UserInfo user = UserInfo.builder()
                        .uid(uid)
                        .userId(userId)
                        .name(name)
                        .password(password)
                        .cardno(cardno)
                        .role(role)
                        .build();

                users.add(user);
                offset += recordSize;

            } catch (Exception e) {
                log.warn("Error parsing user record at offset {}: {}", offset, e.getMessage());
                offset += recordSize;
            }
        }

        return users;
    }

    /**
     * Extract null-terminated string from byte array
     *
     * @param bytes Byte array
     * @return String
     */
    private String extractString(byte[] bytes) {
        int length = 0;
        while (length < bytes.length && bytes[length] != 0) {
            length++;
        }
        return new String(bytes, 0, length).trim();
    }

    /**
     * Clear all attendance records from device
     */
    public void clearAttendance() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }

        protocol.sendCommand(ZKTecoCommand.CMD_CLEAR_ATT_LOG, null);
        log.info("Cleared all attendance records");
    }

    /**
     * Restart device
     */
    public void restart() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }

        protocol.sendCommand(ZKTecoCommand.CMD_RESTART, null);
        log.info("Device restart initiated");
    }

    /**
     * Power off device
     */
    public void powerOff() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }

        protocol.sendCommand(ZKTecoCommand.CMD_POWEROFF, null);
        log.info("Device power off initiated");
    }

    /**
     * Get device time
     *
     * @return Device time
     */
    public LocalDateTime getDeviceTime() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }

        return protocol.getDeviceTime();
    }

    /**
     * Set device time
     *
     * @param time Time to set
     */
    public void setDeviceTime(LocalDateTime time) {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }

        protocol.setDeviceTime(time);
        log.info("Device time set to {}", time);
    }

    /**
     * Enable device (unlock)
     */
    public void enableDevice() {
        protocol.enableDevice();
    }

    /**
     * Disable device (lock)
     */
    public void disableDevice() {
        protocol.disableDevice();
    }
    
    /**
     * Put device to sleep
     */
    public void sleep() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        protocol.sleep();
        log.info("Device sleep mode activated");
    }
    
    /**
     * Resume device from sleep
     */
    public void resume() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        protocol.resume();
        log.info("Device resumed from sleep");
    }
    
    /**
     * Test voice - plays "Thank you"
     */
    public void testVoice() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        protocol.testVoice();
        log.info("Voice test triggered");
    }
    
    /**
     * Clear LCD screen
     */
    public void clearLCD() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        protocol.clearLCD();
        log.info("LCD cleared");
    }
    
    /**
     * Write text to LCD screen
     * 
     * @param rank Line number (0-based)
     * @param text Text to display
     */
    public void writeLCD(int rank, String text) {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        protocol.writeLCD(rank, text);
        log.info("Text written to LCD line {}: {}", rank, text);
    }
    
    /**
     * Get work code setting
     * 
     * @return Work code value
     */
    public String getWorkCode() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        return protocol.getString(ZKTecoCommand.CMD_DEVICE, "WorkCode");
    }
    
    /**
     * Get SSR (Self-Service Recorder) setting
     * 
     * @return SSR value
     */
    public String getSsr() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        return protocol.getString(ZKTecoCommand.CMD_DEVICE, "~SSR");
    }
    
    /**
     * Get PIN width setting
     * 
     * @return PIN width value
     */
    public String getPinWidth() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        return protocol.getString(ZKTecoCommand.CMD_DEVICE, "~PIN2Width");
    }
    
    /**
     * Check if face function is enabled
     * 
     * @return Face function status
     */
    public String getFaceFunctionOn() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        return protocol.getString(ZKTecoCommand.CMD_DEVICE, "FaceFunOn");
    }

    /**
     * Get MAC address
     * 
     * @return MAC address
     */
    public String getMacAddress() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        return protocol.getString(ZKTecoCommand.CMD_DEVICE, "MAC");
    }

    /**
     * Unlock the door
     *
     * @param delay Delay in seconds before locking again
     */
    public void unlockDoor(int delay) {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        byte[] commandData = new byte[2];
        commandData[0] = (byte)(delay % 256);
        commandData[1] = (byte)(delay >> 8);
        protocol.sendCommand(ZKTecoCommand.CMD_UNLOCK, commandData);
        log.info("Door unlocked for {} seconds", delay);
    }

    
    /**
     * Get firmware version
     * 
     * @return Firmware version
     */
    public String getFirmwareVersion() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        return protocol.getString(ZKTecoCommand.CMD_DEVICE, "~ZKFPVersion");
    }
    
    /**
     * Clear all users from device
     */
    public void clearUsers() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        protocol.sendCommand(ZKTecoCommand.CMD_CLEAR_DATA, null);
        log.info("All users cleared");
    }
    
    /**
     * Clear admin privileges
     */
    public void clearAdmin() {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        protocol.sendCommand(ZKTecoCommand.CMD_CLEAR_ADMIN, null);
        log.info("Admin privileges cleared");
    }
    
    /**
     * Remove user by UID
     * 
     * @param uid User unique ID
     */
    public void removeUser(int uid) {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        byte[] commandData = new byte[2];
        commandData[0] = (byte)(uid % 256);
        commandData[1] = (byte)(uid >> 8);
        protocol.sendCommand(ZKTecoCommand.CMD_DELETE_USER, commandData);
        log.info("User {} removed", uid);
    }
    
    /**
     * Set/Add user to device
     * 
     * @param uid Unique ID (max 65535)
     * @param userid User ID string (max 9 digits)
     * @param name User name (max 24 characters)
     * @param password Password (max 8 digits)
     * @param role User role (0=user, 14=admin)
     * @param cardno Card number (max 10 digits)
     */
    public void setUser(int uid, String userid, String name, String password, int role, long cardno) {
        if (!protocol.isConnected()) {
            throw new ZKTecoException("Not connected to device");
        }
        
        // Validate inputs
        if (uid <= 0 || uid > 65535) {
            throw new IllegalArgumentException("UID must be between 1 and 65535");
        }
        if (userid.length() > 9) {
            throw new IllegalArgumentException("User ID max length is 9");
        }
        if (name.length() > 24) {
            throw new IllegalArgumentException("Name max length is 24");
        }
        if (password.length() > 8) {
            throw new IllegalArgumentException("Password max length is 8");
        }
        
        // Build command data (72 bytes total)
        byte[] commandData = new byte[72];
        
        // UID (2 bytes)
        commandData[0] = (byte)(uid % 256);
        commandData[1] = (byte)(uid >> 8);
        
        // Role (1 byte)
        commandData[2] = (byte)role;
        
        // Password (8 bytes, null-padded)
        byte[] passwordBytes = password.getBytes();
        System.arraycopy(passwordBytes, 0, commandData, 3, Math.min(passwordBytes.length, 8));
        
        // Name (24 bytes, null-padded)
        byte[] nameBytes = name.getBytes();
        System.arraycopy(nameBytes, 0, commandData, 11, Math.min(nameBytes.length, 24));
        
        // Card number (4 bytes, little-endian)
        commandData[35] = (byte)(cardno & 0xFF);
        commandData[36] = (byte)((cardno >> 8) & 0xFF);
        commandData[37] = (byte)((cardno >> 16) & 0xFF);
        commandData[38] = (byte)((cardno >> 24) & 0xFF);
        
        // Group field (9 bytes): first byte is 1, rest are 0
        commandData[39] = 1;
        // Bytes 40-47 are already 0
        
        // User ID (9 bytes, null-padded) at offset 48
        byte[] useridBytes = userid.getBytes();
        System.arraycopy(useridBytes, 0, commandData, 48, Math.min(useridBytes.length, 9));
        
        // Bytes 57-71 (15 bytes) are already 0 (padding)
        
        protocol.sendCommand(ZKTecoCommand.CMD_SET_USER, commandData);
        log.info("User {} ({}) added/updated", userid, name);
    }

    @Override
    public void close() {
        protocol.close();
    }
}
