package com.hasanjahidul.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketUtilTest {

    @Test
    void testCreatePacket() {
        int command = 1000;
        int sessionId = 0;
        int replyId = 1;
        byte[] data = new byte[]{1, 2, 3, 4};

        byte[] packet = PacketUtil.createPacket(command, sessionId, replyId, data);

        assertNotNull(packet);
        assertEquals(12, packet.length); // 8 header + 4 data
    }

    @Test
    void testCreatePacketWithoutData() {
        int command = 1000;
        int sessionId = 0;
        int replyId = 1;

        byte[] packet = PacketUtil.createPacket(command, sessionId, replyId, null);

        assertNotNull(packet);
        assertEquals(8, packet.length); // Just header
    }

    @Test
    void testParseHeader() {
        byte[] packet = PacketUtil.createPacket(1000, 123, 456, null);
        int[] header = PacketUtil.parseHeader(packet);

        assertNotNull(header);
        assertEquals(4, header.length);
        assertEquals(1000, header[0]); // Command
        assertEquals(123, header[2]);  // Session ID
        assertEquals(456, header[3]);  // Reply ID
    }

    @Test
    void testExtractData() {
        byte[] data = new byte[]{1, 2, 3, 4, 5};
        byte[] packet = PacketUtil.createPacket(1000, 0, 1, data);

        byte[] extracted = PacketUtil.extractData(packet);

        assertNotNull(extracted);
        assertEquals(5, extracted.length);
        assertArrayEquals(data, extracted);
    }

    @Test
    void testBytesToInt() {
        byte[] bytes = new byte[]{(byte) 0xFF, 0x00, 0x00, 0x00};
        int value = PacketUtil.bytesToInt(bytes, 0, 4);

        assertEquals(255, value);
    }

    @Test
    void testIntToBytes() {
        int value = 255;
        byte[] bytes = PacketUtil.intToBytes(value, 4);

        assertNotNull(bytes);
        assertEquals(4, bytes.length);
        assertEquals((byte) 0xFF, bytes[0]);
        assertEquals(0x00, bytes[1]);
    }
}
