package com.zebra.printwrapper;

public enum SelectedPrinterTaskError {
    DEVICE_DISCONNECT_ERROR("DEVICE_DISCONNECT_ERROR"),
    WRONG_FIRMWARE("WRONG_FIRMWARE"),
    OPEN_CONNECTION_ERROR("OPEN_CONNECTION_ERROR"),
    CLOSE_CONNECTION_ERROR("CLOSE_CONNECTION_ERROR"),
    RESET_CONNECTION("RESET_CONNECTION"),
    DEVICE_DISCOVERY_ERROR("DEVICE_DISCOVERY_ERROR"),
    DEVICE_NOT_FOUND("DEVICE_NOT_FOUND");

    private String enumString;
    private SelectedPrinterTaskError(String confName)
    {
        this.enumString = confName;
    }

    @Override
    public String toString()
    {
        return enumString;
    }
}
