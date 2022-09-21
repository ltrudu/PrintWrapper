package com.zebra.printwrapper;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryException;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.NetworkDiscoverer;

import java.util.ArrayList;
import java.util.List;

public class NearMePrinterDiscovery {

    private static final String TAG = "NearMeDiscovery";
    private Context context;
    private PrinterDiscoveryCallback callback = null;
    private List<DiscoveredPrinter> discoveredPrinters = null;

    public NearMePrinterDiscovery(Context aContext, PrinterDiscoveryCallback aCallback)
    {
        context = aContext;
        callback = aCallback;
    }

    public void startDiscovery()
    {
        discoveredPrinters = new ArrayList<>();
        // Init SubNet Discovery
        try {
            Log.i(TAG, "Searching for printers...");
            WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            WifiManager.MulticastLock lock = wifi.createMulticastLock("wifi_multicast_lock");
            lock.setReferenceCounted(true);
            lock.acquire();
            NetworkDiscoverer.findPrinters(new DiscoveryHandler() {
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
            lock.release();
        } catch (DiscoveryException e) {
            Log.i(TAG, "Discovery error:" + e.getLocalizedMessage());
            if(callback != null)
            {
                callback.onDiscoveryFailed(e.getMessage());
            }
        }
    }
}
