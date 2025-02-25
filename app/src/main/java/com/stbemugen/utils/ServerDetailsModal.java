package com.stbemugen.utils;

public class ServerDetailsModal {
    private String portal;
    private String mac;

    public ServerDetailsModal(String portal, String mac) {
        this.portal = portal;
        this.mac = mac;
    }

    public String getPortal() {
        return portal;
    }

    public void setPortal(String portal) {
        this.portal = portal;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }
}