package com.zebra.printwrapper;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.util.List;

public interface PrinterDiscoveryCallback {
    void onPrinterDiscovered(DiscoveredPrinter printer);
    void onDiscoveryFinished(List<DiscoveredPrinter> printerList);
    void onDiscoveryFailed(String message);

}
