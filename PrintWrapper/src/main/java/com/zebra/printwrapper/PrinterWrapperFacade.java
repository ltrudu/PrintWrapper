package com.zebra.printwrapper;

import android.content.Context;

import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterNetwork;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterUsb;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class PrinterWrapperFacade {
    private static Context context;
    private static PrinterConfig printerConfig = null;

    public interface PrinterWrapperFacadeCallback
    {
        void onStatusChanged(String status);
    }

    public static void Initialize(Context pContext)
    {
        context = pContext;
        printerConfig = new PrinterConfig();
    }

    /**
     * Verify if printer is a bluetooth printer
     * @return true if printer is a bluetooth printer
     */
    public static void DiscoverPrinters()
    {

    }

    /**
     * Add discovered bluetooth, and network printers to the printer list
     * @return Map<String, DiscoveredPrinter> discovered printers in this method call
     */
    public static Map<String, DiscoveredPrinter> DiscoverBluetoothPrinters() throws PrinterWrapperException
    {
        return DiscoverBluetoothPrinters(false);
    }

    /**
     * Add discovered bluetooth printers to the printer list
     * @return Map<String, DiscoveredPrinter> discovered printers in this method call
     */
    public static Map<String, DiscoveredPrinter> DiscoverBluetoothPrinters(boolean cleanList) throws PrinterWrapperException
    {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Map<String, DiscoveredPrinter> discoveredPrinters = new HashMap<>();
        if(cleanList)
        {
            printerConfig.discoveredPrinterMap.clear();
        }

        BluetoothPrinterDiscovery bluetoothPrinterDiscoverer = new BluetoothPrinterDiscovery(context, new PrinterDiscoveryCallback()
        {
            @Override
            public void onPrinterDiscovered(DiscoveredPrinter printer) {
                    discoveredPrinters.put(printer.address, printer);
            }

            @Override
            public void onDiscoveryFinished(List<DiscoveredPrinter> printerList) {
                for(DiscoveredPrinter printer : printerList)
                {
                    if(printerConfig.discoveredPrinterMap.containsKey(printer.address) == false)
                        printerConfig.discoveredPrinterMap.put(printer.address, printer);
                }
                countDownLatch.countDown();
            }

            @Override
            public void onDiscoveryFailed(String message){
                DiscoveredPrinter errorObject = new DiscoveredPrinterBluetooth("0", message);
                discoveredPrinters.put("Error", errorObject);
                countDownLatch.countDown();
            }
        });

        bluetoothPrinterDiscoverer.startDiscovery();

        try {
            countDownLatch.await();

            if(discoveredPrinters.size() == 1 && discoveredPrinters.containsKey("Error"))
            {
                throw new PrinterWrapperException(new Exception(((DiscoveredPrinterBluetooth)discoveredPrinters.get("Error")).friendlyName), printerConfig);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new PrinterWrapperException(e,printerConfig);
        }
        return discoveredPrinters;
    }

    /**
     * Add discovered local network printers to the printer list
     * @return Map<String, DiscoveredPrinter> discovered printers in this method call
     */
    public static Map<String, DiscoveredPrinter> DiscoverLocalNetworkPrinters(boolean cleanList) throws PrinterWrapperException
    {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Map<String, DiscoveredPrinter> discoveredPrinters = new HashMap<>();
        if(cleanList)
        {
            printerConfig.discoveredPrinterMap.clear();
        }

        LocalNetworkPrinterDiscovery localNetworkPrinterDiscovery = new LocalNetworkPrinterDiscovery(context, new PrinterDiscoveryCallback()
        {
            @Override
            public void onPrinterDiscovered(DiscoveredPrinter printer) {
                    discoveredPrinters.put(printer.address, printer);
            }

            @Override
            public void onDiscoveryFinished(List<DiscoveredPrinter> printerList) {
                for(DiscoveredPrinter printer : printerList)
                {
                    if(printerConfig.discoveredPrinterMap.containsKey(printer.address) == false)
                        printerConfig.discoveredPrinterMap.put(printer.address, printer);
                }
                countDownLatch.countDown();
            }

            @Override
            public void onDiscoveryFailed(String message) {
                DiscoveredPrinter errorObject = new DiscoveredPrinterBluetooth("0", message);
                discoveredPrinters.put("Error", errorObject);
                countDownLatch.countDown();

            }
        });

        localNetworkPrinterDiscovery.startDiscovery();

        try {
            countDownLatch.await();

            if(discoveredPrinters.size() == 1 && discoveredPrinters.containsKey("Error"))
            {
                throw new PrinterWrapperException(new Exception(((DiscoveredPrinterBluetooth)discoveredPrinters.get("Error")).friendlyName), printerConfig);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new PrinterWrapperException(e,printerConfig);
        }
        return discoveredPrinters;
    }

    /**
     * Add discovered multicast network printers to the printer list
     * @return Map<String, DiscoveredPrinter> discovered printers in this method call
     */
    public static Map<String, DiscoveredPrinter> DiscoverMultiCastNetworkPrinters(int multicastHops, boolean cleanList) throws PrinterWrapperException
    {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Map<String, DiscoveredPrinter> discoveredPrinters = new HashMap<>();
        if(cleanList)
        {
            printerConfig.discoveredPrinterMap.clear();
        }

        MultiCastPrinterDiscovery multiCastPrinterDiscovery = new MultiCastPrinterDiscovery(context,multicastHops, new PrinterDiscoveryCallback()
        {
            @Override
            public void onPrinterDiscovered(DiscoveredPrinter printer) {
                discoveredPrinters.put(printer.address, printer);
            }

            @Override
            public void onDiscoveryFinished(List<DiscoveredPrinter> printerList) {
                for(DiscoveredPrinter printer : printerList)
                {
                    if(printerConfig.discoveredPrinterMap.containsKey(printer.address) == false)
                        printerConfig.discoveredPrinterMap.put(printer.address, printer);
                }
                countDownLatch.countDown();
            }

            @Override
            public void onDiscoveryFailed(String message) {
                DiscoveredPrinter errorObject = new DiscoveredPrinterBluetooth("0", message);
                discoveredPrinters.put("Error", errorObject);
                countDownLatch.countDown();
            }
        });

        multiCastPrinterDiscovery.startDiscovery();

        try {
            countDownLatch.await();

            if(discoveredPrinters.size() == 1 && discoveredPrinters.containsKey("Error"))
            {
                throw new PrinterWrapperException(new Exception(((DiscoveredPrinterBluetooth)discoveredPrinters.get("Error")).friendlyName), printerConfig);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new PrinterWrapperException(e,printerConfig);
        }
        return discoveredPrinters;
    }


    /**
     * Verify if printer is a bluetooth printer
     * @return true if printer is a bluetooth printer
     */
    public static boolean isBluetoothPrinter() throws PrinterWrapperException {
        checkConnection();
        return printerConfig.discoveredPrinter instanceof DiscoveredPrinterBluetooth;
    }

    /**
     * Verify if printer is a network printer
     * @return true if printer is a network printer
     */
    public static boolean isNetworkPrinter() throws PrinterWrapperException {
        checkConnection();
        return printerConfig.discoveredPrinter instanceof DiscoveredPrinterNetwork;
    }

    /**
     * Verify if printer is a usb printer
     * @return true if printer is a usb printer
     */
    public static boolean isUSBPrinter() throws PrinterWrapperException {
        checkConnection();
        return printerConfig.discoveredPrinter instanceof DiscoveredPrinterUsb;
    }

    private static void checkConnection() throws PrinterWrapperException {
        if(printerConfig == null)
        {
            throw new PrinterWrapperException(new Exception("No printer config."), null);
        }
        else if(printerConfig.discoveredPrinter == null)
        {
            throw new PrinterWrapperException(new Exception("No printer selected."), null);
        }
    }
}
