package com.zebra.printwrapper;

import android.content.Context;
import android.util.Log;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;

import java.util.ArrayList;
import java.util.List;

public class DirectIPNetworkPrinterDiscovery {

    private static final String TAG = "LocalDiscovery";
    private Context context;
    private PrinterDiscoveryCallback callback = null;
    private List<DiscoveredPrinter> discoveredPrinters = null;
    private String printerIP;
    private int waitForResponsesTimeout = -1;

    public DirectIPNetworkPrinterDiscovery(Context context, String printerIP, PrinterDiscoveryCallback callback)
    {
        this.context = context;
        this.callback = callback;
        this.printerIP = printerIP;
        waitForResponsesTimeout = -1;
    }

    public DirectIPNetworkPrinterDiscovery(Context context, String printerIP, int waitForResponsesTimeout, PrinterDiscoveryCallback callback)
    {
        this.context = context;
        this.callback = callback;
        this.printerIP = printerIP;
        this.waitForResponsesTimeout = waitForResponsesTimeout;
    }

    public void startDiscovery()
    {
        discoveredPrinters = new ArrayList<>();
        DiscoveryHandler discoveryHandler = new DiscoveryHandler() {
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
        };

        // Init Bluetooth Discovery
        try {
            Log.i(TAG, "Searching for printers...");
            if(waitForResponsesTimeout == -1)
            {
                NetworkDiscoverer.directedBroadcast(discoveryHandler, printerIP);
            }
            else
            {
                NetworkDiscoverer.directedBroadcast(discoveryHandler, printerIP, waitForResponsesTimeout);
            }
        } catch (DiscoveryException e) {
            Log.i(TAG, "Discovery error:" + e.getLocalizedMessage());
            if(callback != null)
            {
                callback.onDiscoveryFailed(e.getMessage());
            }
        }


    }
}
