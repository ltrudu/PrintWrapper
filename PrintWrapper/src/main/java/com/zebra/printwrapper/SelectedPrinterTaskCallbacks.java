package com.zebra.printwrapper;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.util.Map;

public interface SelectedPrinterTaskCallbacks
{
    void onSuccess(DiscoveredPrinter printer, Map<String,String> discoveryMap);
    void onError(SelectedPrinterTaskError error, String errorMessage);
}
