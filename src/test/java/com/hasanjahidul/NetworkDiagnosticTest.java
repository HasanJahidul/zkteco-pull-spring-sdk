package com.hasanjahidul;

import com.hasanjahidul.protocol.ZKTecoCommand;
import com.hasanjahidul.protocol.ZKTecoProtocol;

public class NetworkDiagnosticTest {
    public static void main(String[] args) {
        System.out.println("Starting network diagnosis sweep...");
        ZKTecoProtocol proto = new ZKTecoProtocol("172.22.22.37");
        if (proto.connect()) {
            System.out.println("--- IP Candidates ---");
            String[] ipKeys = {"IPAddress", "~IPAddress", "IPAddress", "IP", "IpAddress", "NetIP", "NetIp"};
            for (String k : ipKeys) {
                System.out.println(k + " = '" + proto.getString(ZKTecoCommand.CMD_DEVICE, k) + "'");
            }
            
            System.out.println("--- Gateway Candidates ---");
            String[] gwKeys = {"GATEWAY", "GateWay", "~Gateway", "Gateway", "GatewayIP", "NetGate"};
            for (String k : gwKeys) {
                System.out.println(k + " = '" + proto.getString(ZKTecoCommand.CMD_DEVICE, k) + "'");
            }
            
            proto.disconnect();
        } else {
            System.out.println("Failed to connect");
        }
    }
}

