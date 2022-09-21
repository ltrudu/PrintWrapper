package com.zebra.printwrapper;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterNetwork;

import java.util.Map;

public class PrintWrapperHelpers {

    /**
     * Verify if printer is a bluetooth printer
     * @param printer
     * @return true if printer is a bluetooth printer
     */
    public static boolean isBluetoothPrinter(DiscoveredPrinter printer)
    {
        return printer instanceof DiscoveredPrinterBluetooth;
    }

    /**
     * Verify if printer is a network printer
     * @param printer
     * @return true if printer is a network printer
     */
    public static boolean isNetworkPrinter(DiscoveredPrinter printer)
    {
        return printer instanceof DiscoveredPrinterNetwork;
    }

    /**
     * Get network printer port
     * @param printer
     * @return Port number if the printer is a Network printer, -1 if it is a Bluetooth printer
     */
    public static int getNetworkPrinterPort(DiscoveredPrinter printer)
    {
        if(isBluetoothPrinter(printer))
            return -1;
        Map<String, String> discoveryDataMap = printer.getDiscoveryDataMap();
        return Integer.parseInt(discoveryDataMap.get(PrinterDiscoveryDataMapKeys.PORT_NUMBER));
    }

    /**
     * Get printer primary language CPCL or ZPL
     * @param printer
     * @return Primary language of the printer
     */
    public static String getPrinterPrimaryLanguage(DiscoveredPrinter printer)
    {
        Map<String, String> discoveryDataMap = printer.getDiscoveryDataMap();
        return discoveryDataMap.get(PrinterDiscoveryDataMapKeys.PRIMARY_LANGUAGE);
    }
}
