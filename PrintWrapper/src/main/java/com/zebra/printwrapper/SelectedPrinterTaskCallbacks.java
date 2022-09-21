package com.zebra.printwrapper;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

public interface SelectedPrinterTaskCallbacks
{
    void onSuccess(DiscoveredPrinter printer);
    void onError(SelectedPrinterTaskError error, String errorMessage);
}
