package com.zebra.printwrapper;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;

import java.util.ArrayList;
import java.util.List;

public class USBPrinterDiscovery {

    private static final String TAG = "USBDiscovery";
    private Context context;
    private PrinterDiscoveryCallback callback = null;
    private List<DiscoveredPrinter> discoveredPrinters = null;

    public USBPrinterDiscovery(Context aContext, PrinterDiscoveryCallback aCallback)
    {
        context = aContext;
        callback = aCallback;
    }

    public void startDiscovery()
    {
        discoveredPrinters = new ArrayList<>();
        // Init SubNet Discovery
        try {
            Log.i(TAG, "Searching for USB printers...");
            UsbDiscoverer.findPrinters(context, new DiscoveryHandler() {
                @Override
                public void foundPrinter(DiscoveredPrinter discoveredPrinter) {
                    Log.i(TAG, "Discovered Printer: " + discoveredPrinter.address);
                    discoveredPrinters.add(discoveredPrinter);
                    if(callback != null)
                    {
                        callback.onPrinterDiscovered(discoveredPrinter);
                    }
                }

                @Override
                public void discoveryFinished() {
                    Log.i(TAG, "Discovery finished");

                    // Verify If Printer Was Found
                    if (discoveredPrinters.isEmpty()) {
                        Log.i(TAG, "No Printers Found");
                    }

                    if(callback != null)
                    {
                        callback.onDiscoveryFinished(discoveredPrinters);
                    }
                }

                @Override
                public void discoveryError(String e) {
                    Log.i(TAG, "Discovery error:" + e);

                    if(callback != null)
                    {
                        callback.onDiscoveryFailed(e);
                    }
                }
            });
        } catch (Exception e) {
            Log.i(TAG, "Discovery error:" + e.getLocalizedMessage());
            if(callback != null)
            {
                callback.onDiscoveryFailed(e.getMessage());
            }
        }
    }
}
