package com.zebra.printwrapper;

public enum SelectedPrinterTaskError {
    DEVICE_DISCONNECT_ERROR("DEVICE_DISCONNECT_ERROR"),
    WRONG_FIRMWARE("WRONG_FIRMWARE"),
    OPEN_CONNECTION_ERROR("OPEN_CONNECTION_ERROR"),
    RESET_CONNECTION("RESET_CONNECTION");

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
