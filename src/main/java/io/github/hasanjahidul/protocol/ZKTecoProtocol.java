package io.github.hasanjahidul.protocol;

import io.github.hasanjahidul.exception.ZKTecoException;
import io.github.hasanjahidul.util.PacketUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;

/**
 * Low-level protocol handler for ZKTeco devices
 */
@Slf4j
public class ZKTecoProtocol implements AutoCloseable {

    private static final int DEFAULT_PORT = 4370;
    private static final int SOCKET_TIMEOUT = 60000; // 60 seconds
    private static final int BUFFER_SIZE = 4096;
    private static final int USHRT_MAX = 65535;

    private final String ipAddress;
    private final int port;
    private DatagramSocket socket;
    private InetAddress deviceAddress;
    private int sessionId = 0;
    private int replyId = USHRT_MAX - 1; // PHP uses -1 + USHRT_MAX = 65534
    /**
     * -- GETTER --
     *  Check if connected to device
     *
     * @return true if connected
     */
    @Getter
    private boolean connected = false;
    private byte[] lastDataReceived = new byte[0];

    /**
     * Create ZKTeco protocol handler with default port
     *
     * @param ipAddress IP address of the device
     */
    public ZKTecoProtocol(String ipAddress) {
        this(ipAddress, DEFAULT_PORT);
    }

    /**
     * Create ZKTeco protocol handler with custom port
     *
     * @param ipAddress IP address of the device
     * @param port      Port number (default is 4370)
     */
    public ZKTecoProtocol(String ipAddress, int port) {
        this.ipAddress = ipAddress;
        this.port = port;
    }

    /**
     * Connect to ZKTeco device
     *
     * @return true if connection successful
     */
    public boolean connect() {
        try {
            deviceAddress = InetAddress.getByName(ipAddress);
            socket = new DatagramSocket();
            socket.setSoTimeout(SOCKET_TIMEOUT);

            // Initial connection: session=0, reply_id=65534 (USHRT_MAX-1)
            sessionId = 0;
            replyId = USHRT_MAX - 1;
            
            // Send connect command
            byte[] packet = PacketUtil.createPacket(ZKTecoCommand.CMD_CONNECT, sessionId, replyId, null);
            
            DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, deviceAddress, port);
            socket.send(sendPacket);
            
            // Receive response
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);
            
            byte[] response = new byte[receivePacket.getLength()];
            System.arraycopy(buffer, 0, response, 0, receivePacket.getLength());
            lastDataReceived = response;
            
            // Parse response
            int[] header = PacketUtil.parseHeader(response);
            if (header != null && (header[0] == ZKTecoCommand.CMD_ACK_OK || header[0] == ZKTecoCommand.CMD_ACK_UNAUTH)) {
                sessionId = header[2];
                replyId = header[3]; // Use reply ID from response
                connected = true;
                log.info("Connected to device at {}:{}  (Session ID: {})", ipAddress, port, sessionId);
                return true;
            }
            
            log.warn("Connection failed: Invalid response from device");
            return false;
        } catch (SocketTimeoutException e) {
            log.error("Connection timeout: Device at {}:{} not responding", ipAddress, port);
            return false;
        } catch (IOException e) {
            log.error("Failed to connect to device: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Disconnect from device
     */
    public void disconnect() {
        if (connected) {
            try {
                sendCommand(ZKTecoCommand.CMD_EXIT, null);
            } catch (Exception e) {
                log.warn("Error during disconnect: {}", e.getMessage());
            }
            connected = false;
            sessionId = 0;
            replyId = 0;
            log.info("Disconnected from device");
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Send command to device
     *
     * @param command Command code
     * @param data    Data payload
     * @return Response data
     */
    public byte[] sendCommand(int command, byte[] data) {
        if (!connected && command != ZKTecoCommand.CMD_CONNECT) {
            throw new ZKTecoException("Not connected to device");
        }

        try {
            // Extract current reply ID from last received data if available
            if (lastDataReceived.length >= 8) {
                int[] lastHeader = PacketUtil.parseHeader(lastDataReceived);
                if (lastHeader != null) {
                    replyId = lastHeader[3];
                }
            }
            
            // Create and send packet
            byte[] packet = PacketUtil.createPacket(command, sessionId, replyId, data);
            
            DatagramPacket sendPacket = new DatagramPacket(packet, packet.length, deviceAddress, port);
            socket.send(sendPacket);

            // Receive response
            byte[] buffer = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(receivePacket);

            byte[] response = new byte[receivePacket.getLength()];
            System.arraycopy(buffer, 0, response, 0, receivePacket.getLength());
            lastDataReceived = response;

            // Validate response
            int[] header = PacketUtil.parseHeader(response);
            if (header == null) {
                throw new ZKTecoException("Invalid response from device");
            }

            if (header[0] == ZKTecoCommand.CMD_ACK_ERROR) {
                throw new ZKTecoException("Device returned error for command: " + command);
            }

            return response;
        } catch (SocketTimeoutException e) {
            throw new ZKTecoException("Device response timeout", e);
        } catch (IOException e) {
            throw new ZKTecoException("Communication error: " + e.getMessage(), e);
        }
    }

    /**
     * Get string from device
     *
     * @param command Command code
     * @return String value
     */
    public String getString(int command) {
        return getString(command, null);
    }
    
    /**
     * Get string from device with command string parameter
     *
     * @param command Command code
     * @param commandString Command parameter string
     * @return String value
     */
    public String getString(int command, String commandString) {
        byte[] commandData = null;
        if (commandString != null && !commandString.isEmpty()) {
            commandData = commandString.getBytes();
        }
        
        byte[] response = sendCommand(command, commandData);
        byte[] data = PacketUtil.extractData(response);
        if (data.length > 0) {
            // Find null terminator
            int length = 0;
            while (length < data.length && data[length] != 0) {
                length++;
            }
            String result = new String(data, 0, length).trim();
            
            // If command string was provided and response contains '=', extract value after '='
            // This handles responses like "~SerialNumber=A8N5225060143 "
            if (commandString != null && !commandString.isEmpty() && result.contains("=")) {
                int equalsIndex = result.indexOf('=');
                if (equalsIndex >= 0 && equalsIndex < result.length() - 1) {
                    result = result.substring(equalsIndex + 1).trim();
                }
            }
            
            return result;
        }
        return "";
    }

    /**
     * Get integer from device
     *
     * @param command Command code
     * @return Integer value
     */
    public int getInt(int command) {
        byte[] response = sendCommand(command, null);
        byte[] data = PacketUtil.extractData(response);
        if (data.length >= 4) {
            return PacketUtil.bytesToInt(data, 0, 4);
        }
        return 0;
    }

    /**
     * Get device time
     *
     * @return Device time as LocalDateTime
     */
    public LocalDateTime getDeviceTime() {
        byte[] response = sendCommand(ZKTecoCommand.CMD_GET_TIME, null);
        byte[] data = PacketUtil.extractData(response);

        if (data.length >= 4) {
            int encodedTime = PacketUtil.bytesToInt(data, 0, 4);
            // Decode using ZKTeco's custom time encoding (same as attendance records)
            return decodeTime(encodedTime);
        }
        return null;
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
     * Set device time
     * Uses ZKTeco's custom time encoding (same as attendance records)
     *
     * @param time Time to set
     */
    public void setDeviceTime(LocalDateTime time) {
        int encodedTime = encodeTime(time);
        byte[] timeData = new byte[4];
        timeData[0] = (byte)(encodedTime & 0xFF);
        timeData[1] = (byte)((encodedTime >> 8) & 0xFF);
        timeData[2] = (byte)((encodedTime >> 16) & 0xFF);
        timeData[3] = (byte)((encodedTime >> 24) & 0xFF);
        sendCommand(ZKTecoCommand.CMD_SET_TIME, timeData);
    }
    
    /**
     * Encode time to ZKTeco format
     * Matches PHP encodeTime() function
     * 
     * @param time LocalDateTime to encode
     * @return Encoded time as integer
     */
    private int encodeTime(LocalDateTime time) {
        int year = time.getYear() % 100;
        int month = time.getMonthValue();
        int day = time.getDayOfMonth();
        int hour = time.getHour();
        int minute = time.getMinute();
        int second = time.getSecond();
        
        return ((year * 12 * 31 + (month - 1) * 31 + day - 1) * (24 * 60 * 60)) +
               ((hour * 60 + minute) * 60) + second;
    }

    /**
     * Receive large data from device in multiple packets
     * Matches PHP recData() function
     *
     * @return Complete data as byte array
     */
    public byte[] receiveDataPackets() {
        try {
            // Check if device is sending data (CMD_PREPARE_DATA)
            int[] header = PacketUtil.parseHeader(lastDataReceived);
            if (header == null || header[0] != ZKTecoCommand.CMD_PREPARE_DATA) {
                return new byte[0];
            }
            
            // Get total size from prepare data response
            byte[] prepareData = PacketUtil.extractData(lastDataReceived);
            if (prepareData.length < 4) {
                return new byte[0];
            }
            
            int totalSize = PacketUtil.bytesToInt(prepareData, 0, 4);
            log.debug("Receiving {} bytes of data in packets", totalSize);
            
            ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
            int received = 0;
            int errors = 0;
            int maxErrors = 10;
            boolean first = true;
            
            while (received < totalSize && errors < maxErrors) {
                try {
                    byte[] buffer = new byte[1032]; // PHP uses 1032 byte buffer
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    
                    byte[] packetData = new byte[packet.getLength()];
                    System.arraycopy(buffer, 0, packetData, 0, packet.getLength());
                    
                    // Skip first 8 bytes (header) except for first packet
                    int offset = first ? 0 : 8;
                    int dataLength = packet.getLength() - offset;
                    
                    if (dataLength > 0) {
                        dataStream.write(packetData, offset, dataLength);
                        received += dataLength;
                    }
                    
                    first = false;
                    errors = 0; // Reset error count on successful receive
                    
                } catch (SocketTimeoutException e) {
                    errors++;
                    log.warn("Timeout receiving data packet (attempt {}/{})", errors, maxErrors);
                    if (errors >= maxErrors) {
                        log.error("Too many errors receiving data. Received {}/{} bytes", received, totalSize);
                        break;
                    }
                    // Wait a bit before retry
                    Thread.sleep(100);
                }
            }
            
            // Flush socket - read any remaining packets
            try {
                socket.setSoTimeout(100); // Short timeout
                byte[] flushBuffer = new byte[1024];
                DatagramPacket flushPacket = new DatagramPacket(flushBuffer, flushBuffer.length);
                socket.receive(flushPacket);
                socket.setSoTimeout(SOCKET_TIMEOUT); // Restore original timeout
            } catch (SocketTimeoutException e) {
                // Expected - no more data
                socket.setSoTimeout(SOCKET_TIMEOUT);
            }
            
            byte[] result = dataStream.toByteArray();
            log.debug("Successfully received {}/{} bytes", result.length, totalSize);
            return result;
            
        } catch (Exception e) {
            log.error("Error receiving data packets: {}", e.getMessage());
            return new byte[0];
        }
    }

    /**
     * Disable device (shows "Processing..." on device)
     */
    public void disableDevice() {
        sendCommand(ZKTecoCommand.CMD_DISABLE_DEVICE, new byte[]{0, 0});
    }

    /**
     * Enable device (returns to normal state)
     */
    public void enableDevice() {
        sendCommand(ZKTecoCommand.CMD_ENABLE_DEVICE, null);
    }
    
    /**
     * Restart device
     */
    public void restart() {
        sendCommand(ZKTecoCommand.CMD_RESTART, new byte[]{0, 0});
    }
    
    /**
     * Power off device
     */
    public void powerOff() {
        sendCommand(ZKTecoCommand.CMD_POWEROFF, new byte[]{0, 0});
    }
    
    /**
     * Put device to sleep
     */
    public void sleep() {
        sendCommand(ZKTecoCommand.CMD_SLEEP, new byte[]{0, 0});
    }
    
    /**
     * Resume device from sleep
     */
    public void resume() {
        sendCommand(ZKTecoCommand.CMD_RESUME, new byte[]{0, 0});
    }
    
    /**
     * Test voice - plays "Thank you"
     */
    public void testVoice() {
        sendCommand(ZKTecoCommand.CMD_TESTVOICE, new byte[]{0, 0});
    }
    
    /**
     * Clear LCD screen
     */
    public void clearLCD() {
        sendCommand(ZKTecoCommand.CMD_CLEAR_LCD, null);
    }
    
    /**
     * Write text to LCD screen
     * 
     * @param rank Line number (0-based)
     * @param text Text to display
     */
    public void writeLCD(int rank, String text) {
        byte[] textBytes = text.getBytes();
        byte[] commandData = new byte[3 + 1 + textBytes.length];
        commandData[0] = (byte)(rank % 256);
        commandData[1] = (byte)(rank >> 8);
        commandData[2] = 0;
        commandData[3] = ' ';
        System.arraycopy(textBytes, 0, commandData, 4, textBytes.length);
        sendCommand(ZKTecoCommand.CMD_WRITE_LCD, commandData);
    }
    
    /**
     * Clear attendance log
     */
    public void clearAttendance() {
        sendCommand(ZKTecoCommand.CMD_CLEAR_ATT_LOG, null);
    }
    
    /**
     * Get session ID
     *
     * @return Session ID
     */
    public int getSessionId() {
        return sessionId;
    }

    @Override
    public void close() {
        disconnect();
    }
}
