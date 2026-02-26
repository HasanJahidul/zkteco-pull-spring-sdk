package com.hasanjahidul;

import com.hasanjahidul.protocol.ZKTecoCommand;
import com.hasanjahidul.protocol.ZKTecoProtocol;

public class TestStrings {
    public static void main(String[] args) {
        System.out.println("Starting test strings...");
        ZKTecoProtocol proto = new ZKTecoProtocol("172.22.22.37");
        if (proto.connect()) {
            String[] macKeys = {"MAC", "~MAC", "DeviceMAC", "~DeviceMAC", "Mac", "mac", "MACAddress"};
            for (String k : macKeys) {
                System.out.println(k + " = '" + proto.getString(ZKTecoCommand.CMD_DEVICE, k) + "'");
            }
            
            String[] gwKeys = {"Gateway", "GATEWAY", "~Gateway", "GatewayIP", "NETGATE", "NetGate", "GatewayAddress"};
            for (String k : gwKeys) {
                System.out.println(k + " = '" + proto.getString(ZKTecoCommand.CMD_DEVICE, k) + "'");
            }
            
            proto.disconnect();
        } else {
            System.out.println("Failed to connect");
        }
    }
}
