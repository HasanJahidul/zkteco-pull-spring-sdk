package com.hasanjahidul.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class for creating and parsing ZKTeco packets
 */
public class PacketUtil {

    private static final int PACKET_HEADER_SIZE = 8;

    /**
     * Create packet header for ZKTeco communication
     * Matches PHP createHeader() function
     *
     * @param command    Command code
     * @param sessionId  Session ID
     * @param replyId    Reply ID
     * @param data       Data payload
     * @return Complete packet as byte array
     */
    public static byte[] createPacket(int command, int sessionId, int replyId, byte[] data) {
        int dataLen = (data != null) ? data.length : 0;
        int totalLen = PACKET_HEADER_SIZE + dataLen;

        ByteBuffer buffer = ByteBuffer.allocate(totalLen);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Create initial header with zero checksum
        buffer.putShort((short) command);           // Command (2 bytes)
        buffer.putShort((short) 0);                 // Checksum placeholder (2 bytes)
        buffer.putShort((short) sessionId);         // Session ID (2 bytes)
        buffer.putShort((short) replyId);           // Reply ID (2 bytes)

        // Data payload
        if (data != null && data.length > 0) {
            buffer.put(data);
        }

        byte[] packet = buffer.array();

        // Calculate checksum (PHP-style)
        int checksum = calculateChecksum(packet);
        
        // Set checksum in packet (little endian)
        packet[2] = (byte) (checksum & 0xFF);
        packet[3] = (byte) ((checksum >> 8) & 0xFF);
        
        // Increment reply ID for next packet (matching PHP behavior)
        replyId++;
        if (replyId >= 0xFFFF) {
            replyId -= 0xFFFF;
        }
        
        // Update reply ID in packet
        packet[6] = (byte) (replyId & 0xFF);
        packet[7] = (byte) ((replyId >> 8) & 0xFF);

        return packet;
    }

    /**
     * Calculate checksum for packet (matches PHP implementation)
     *
     * @param packet Packet data
     * @return Checksum value
     */
    private static int calculateChecksum(byte[] packet) {
        int checksum = 0;
        int length = packet.length;
        int i = 0;
        
        //  Process pairs of bytes
        while (i < length - 1) {
            // Skip checksum bytes at positions 2 and 3
            if (i == 2) {
                i += 2;
                continue;
            }
            
            // Combine two bytes as unsigned short (little endian)
            int word = (packet[i] & 0xFF) | ((packet[i + 1] & 0xFF) << 8);
            checksum += word;
            
            // Keep checksum within USHRT_MAX
            if (checksum > 0xFFFF) {
                checksum -= 0xFFFF;
            }
            
            i += 2;
        }
        
        // Handle odd byte if exists
        if (i < length && i != 2 && i != 3) {
            checksum += (packet[i] & 0xFF);
        }
        
        // Final checksum adjustment (matching PHP logic)
        while (checksum > 0xFFFF) {
            checksum -= 0xFFFF;
        }
        
        if (checksum > 0) {
            checksum = -checksum;
        } else {
            checksum = Math.abs(checksum);
        }
        
        checksum -= 1;
        
        while (checksum < 0) {
            checksum += 0xFFFF;
        }
        
        return checksum & 0xFFFF;
    }

    /**
     * Parse response header from received packet
     *
     * @param response Response packet
     * @return Parsed header information as int array [command, checksum, sessionId, replyId]
     */
    public static int[] parseHeader(byte[] response) {
        if (response == null || response.length < PACKET_HEADER_SIZE) {
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(response);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int[] header = new int[4];
        header[0] = buffer.getShort() & 0xFFFF;  // Command
        header[1] = buffer.getShort() & 0xFFFF;  // Checksum
        header[2] = buffer.getShort() & 0xFFFF;  // Session ID
        header[3] = buffer.getShort() & 0xFFFF;  // Reply ID

        return header;
    }

    /**
     * Extract data payload from response packet
     *
     * @param response Response packet
     * @return Data payload
     */
    public static byte[] extractData(byte[] response) {
        if (response == null || response.length <= PACKET_HEADER_SIZE) {
            return new byte[0];
        }

        byte[] data = new byte[response.length - PACKET_HEADER_SIZE];
        System.arraycopy(response, PACKET_HEADER_SIZE, data, 0, data.length);
        return data;
    }

    /**
     * Convert byte array to int (little endian)
     *
     * @param data   Byte array
     * @param offset Offset to start reading
     * @param length Number of bytes to read
     * @return Integer value
     */
    public static int bytesToInt(byte[] data, int offset, int length) {
        int result = 0;
        for (int i = 0; i < length && (offset + i) < data.length; i++) {
            result |= (data[offset + i] & 0xFF) << (i * 8);
        }
        return result;
    }

    /**
     * Convert int to byte array (little endian)
     *
     * @param value  Integer value
     * @param length Number of bytes
     * @return Byte array
     */
    public static byte[] intToBytes(int value, int length) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) ((value >> (i * 8)) & 0xFF);
        }
        return bytes;
    }

    private PacketUtil() {
        // Utility class
    }
}
