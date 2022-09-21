package com.zebra.printwrapper;

import android.content.Context;
import android.util.Log;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;

import java.util.ArrayList;
import java.util.List;

public class LocalNetworkPrinterDiscovery {

    private static final String TAG = "LocalDiscovery";
    private Context context;
    private PrinterDiscoveryCallback callback = null;
    private List<DiscoveredPrinter> discoveredPrinters = null;

    public LocalNetworkPrinterDiscovery(Context aContext, PrinterDiscoveryCallback aCallback)
    {
        context = aContext;
        callback = aCallback;
    }

    public void startDiscovery()
    {
        discoveredPrinters = new ArrayList<>();
        // Init Bluetooth Discovery
        try {
            Log.i(TAG, "Searching for printers...");
            NetworkDiscoverer.localBroadcast(new DiscoveryHandler() {
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
                    Log.i(TAG, "Discovery error");

                    if(callback != null)
                    {
                        callback.onDiscoveryFailed(e);
                    }
                }
            });
        } catch (DiscoveryException e) {
            Log.i(TAG, "Discovery error");
            if(callback != null)
            {
                callback.onDiscoveryFailed(e.getMessage());
            }
        }
    }
}
