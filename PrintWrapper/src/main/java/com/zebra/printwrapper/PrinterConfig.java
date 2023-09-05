package com.zebra.printwrapper;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.util.HashMap;
import java.util.Map;

public class PrinterConfig {
    public DiscoveredPrinter discoveredPrinter = null;
    public Map<String, String> discoveryMap = null;
    public E_PrinterLanguage e_printerLanguage = E_PrinterLanguage.UNKNOWN;
    public E_PrinterConnectionType e_printerConnectionType = E_PrinterConnectionType.NOT_CONNECTED;
    public Map<String, DiscoveredPrinter> discoveredPrinterMap = new HashMap<>();

}
