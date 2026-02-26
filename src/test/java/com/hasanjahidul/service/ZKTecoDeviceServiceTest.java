package com.hasanjahidul.service;

import com.hasanjahidul.exception.ZKTecoException;
import com.hasanjahidul.protocol.ZKTecoCommand;
import com.hasanjahidul.protocol.ZKTecoProtocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ZKTecoDeviceServiceTest {

    private ZKTecoProtocol mockProtocol;
    private ZKTecoDeviceService service;

    @BeforeEach
    void setUp() {
        // Mock the underlying protocol so we don't need a real device
        mockProtocol = mock(ZKTecoProtocol.class);
        service = new ZKTecoDeviceService(mockProtocol);
    }

    @Test
    void testInitialization() {
        assertNotNull(service);
        // Ensure default is not connected if mock returns false
        when(mockProtocol.isConnected()).thenReturn(false);
        assertFalse(service.isConnected());
    }

    @Test
    void testGetAttendanceWithoutConnection() {
        when(mockProtocol.isConnected()).thenReturn(false);
        assertThrows(ZKTecoException.class, () -> service.getAttendance());
    }

    @Test
    void testGetDeviceInfoWithoutConnection() {
        when(mockProtocol.isConnected()).thenReturn(false);
        assertThrows(ZKTecoException.class, () -> service.getDeviceInfo());
    }

    @Test
    void testGetUsersWithoutConnection() {
        when(mockProtocol.isConnected()).thenReturn(false);
        assertThrows(ZKTecoException.class, () -> service.getUsers());
    }

    @Test
    void testGetMacAddressWithConnection() {
        when(mockProtocol.isConnected()).thenReturn(true);
        when(mockProtocol.getString(ZKTecoCommand.CMD_DEVICE, "MAC"))
                .thenReturn("00:11:22:33:44:55");

        String mac = service.getMacAddress();

        assertEquals("00:11:22:33:44:55", mac);
        verify(mockProtocol).getString(ZKTecoCommand.CMD_DEVICE, "MAC");
    }

    @Test
    void testUnlockDoorWithConnection() {
        when(mockProtocol.isConnected()).thenReturn(true);
        
        service.unlockDoor(5);

        // Verify that protocol.sendCommand was called with correct delay logic
        verify(mockProtocol).sendCommand(eq(ZKTecoCommand.CMD_UNLOCK), argThat(data -> 
            data != null && data.length == 2 && data[0] == 5 && data[1] == 0
        ));
    }

    @Test
    void testClose() {
        assertDoesNotThrow(() -> service.close());
        verify(mockProtocol).close();
    }
}
