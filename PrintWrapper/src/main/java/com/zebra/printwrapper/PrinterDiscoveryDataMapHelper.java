package com.zebra.printwrapper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.*;
import android.util.Log;
import android.widget.Toast;

import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.comm.*;
import com.zebra.sdk.printer.discovery.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static androidx.core.content.ContextCompat.getSystemService;

public class PrinterDiscoveryDataMapHelper {

    private static final String TAG = "USBConnect";

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    static DiscoveredPrinterUsb discoveredPrinterUsb = null;
    public static Map<String,String> populateBluetoothPrinterDiscoveryMap(final DiscoveredPrinter selectedPrinter, final SelectedPrinterTaskCallbacks callback, final Context context)
    {
        class BluetoothConnectionQuickClose extends BluetoothConnection {

            public BluetoothConnectionQuickClose(String address) {
                super(address);
            }

            @Override
            public void close() throws ConnectionException {
                this.friendlyName = "";
                if (this.isConnected) {
                    this.isConnected = false;

                    try {
                        this.inputStream.close();
                        this.outputStream.close();
                        this.commLink.close();
                    } catch (IOException e) {
                        // Ugly... don't even know if it will be helpful or not...
                        if(callback != null)
                            callback.onError(SelectedPrinterTaskError.DEVICE_DISCONNECT_ERROR, context.getString(R.string.could_not_disconnect_device) + ":" + e.getLocalizedMessage());
                        throw new ConnectionException(context.getString(R.string.could_not_disconnect_device) + ":" + e.getMessage());
                    }
                }
            }
        }

        Map<String, String> discoveryMap = null;
        Connection connection = null;
        try {
            connection = new BluetoothConnectionQuickClose(selectedPrinter.address);
            connection.open();

            if(connection.isConnected()) {
                // Add some information that are not reported by the bluetooth discoverer
                discoveryMap = selectedPrinter.getDiscoveryDataMap();
                discoveryMap.put(PrinterDiscoveryDataMapKeys.LINK_OS_MAJOR_VER, SGD.GET("appl.link_os_version", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.PRODUCT_NAME, SGD.GET("device.product_name", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.SYSTEM_NAME, SGD.GET("bluetooth.friendly_name", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.HARDWARE_ADDRESS, SGD.GET("bluetooth.address", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.FIRMWARE_VER, SGD.GET("appl.name", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.SERIAL_NUMBER, SGD.GET("device.unique_id", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.APL_MODE, SGD.GET("apl.enable", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.DEVICE_LANGUAGE, SGD.GET("device.languages", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.PDF_ENABLED, discoveryMap.get(PrinterDiscoveryDataMapKeys.APL_MODE).equalsIgnoreCase("pdf") ? "true" : "false");
                discoveryMap.put(PrinterDiscoveryDataMapKeys.CONNEXION_TYPE, PrinterDiscoveryDataMapKeys.CONNEXION_TYPE_BLUETOOTH);
            }
            else
            {
                discoveryMap = null;
            }

            connection.close();
        } catch (ConnectionException e) {
            Log.e(TAG, "Open connection error", e);
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, R.string.open_connection_error + ":" + e.getLocalizedMessage());
            discoveryMap = null;
        }

        return discoveryMap;

    }

    public static Map<String,String> populateNetworkPrinterDiscoveryMap(final DiscoveredPrinter selectedPrinter, final SelectedPrinterTaskCallbacks callback, final Context context)
    {
        class MultiChannelQuickClose extends MultichannelTcpConnection {

            public MultiChannelQuickClose(DiscoveredPrinter printer) {
                super(printer);
            }

            @Override
            public void close() throws ConnectionException {
                if (this.isConnected()) {
                       this.closePrintingChannel();
                       this.closeStatusChannel();
                 }
            }
        }
        Map<String, String> discoveryMap;
        Connection connection = new MultiChannelQuickClose(selectedPrinter);
        try {
            connection.open();

            if(connection.isConnected())
            {
                discoveryMap = selectedPrinter.getDiscoveryDataMap();
                discoveryMap.put(PrinterDiscoveryDataMapKeys.DEVICE_LANGUAGE, SGD.GET("device.languages", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.APL_MODE, SGDHelper.getAplMode(connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.PDF_ENABLED, discoveryMap.get(PrinterDiscoveryDataMapKeys.APL_MODE).equalsIgnoreCase("pdf") ? "true" : "false");
                discoveryMap.put(PrinterDiscoveryDataMapKeys.CONNEXION_TYPE, PrinterDiscoveryDataMapKeys.CONNEXION_TYPE_NETWORK);
            }
            else
                discoveryMap = null;
            connection.close();

        } catch (ConnectionException e) {
            Log.e(TAG, "Open connection error", e);
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, R.string.open_connection_error + ":" + e.getLocalizedMessage());
            discoveryMap = null;
        }

        return discoveryMap;

    }

    public static Map<String,String> populateUSBPrinterDiscoveryMap(final DiscoveredPrinter selectedPrinter, final SelectedPrinterTaskCallbacks callback, final Context context)
    {
        PendingIntent mPermissionIntent;
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);

        CountDownLatch countDownLatch = new CountDownLatch(1);

        new Thread(new Runnable() {
            public void run() {
                // Find connected printers
                UsbDiscoveryHandler handler = new UsbDiscoveryHandler();
                UsbDiscoverer.findPrinters(context, handler);
                boolean hasPermissionToCommunicate = false;
                try {
                    while (!handler.discoveryComplete) {
                        Thread.sleep(100);
                    }

                    if (handler.printers != null && handler.printers.size() > 0) {
                        for(DiscoveredPrinterUsb discover : handler.printers)
                        {
                            Log.v(TAG, "Discover address:" + discover.address);
                            if(discover.address.equalsIgnoreCase(selectedPrinter.address))
                            {
                                discoveredPrinterUsb = discover;
                                break;
                            }
                        }
                        if(discoveredPrinterUsb == null)
                        {
                            if(callback != null)
                                callback.onError(SelectedPrinterTaskError.DEVICE_NOT_FOUND, "device address:" + selectedPrinter.address);
                            countDownLatch.countDown();
                        }
                        else {
                            if (!usbManager.hasPermission(discoveredPrinterUsb.device)) {
                                usbManager.requestPermission(discoveredPrinterUsb.device, mPermissionIntent);
                            } else {
                                hasPermissionToCommunicate = true;
                                countDownLatch.countDown();
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Device usb discovery error", e);
                    if(callback != null)
                        callback.onError(SelectedPrinterTaskError.DEVICE_DISCOVERY_ERROR, R.string.Usb_discovery_error + ":" + e.getLocalizedMessage());
                    countDownLatch.countDown();
                }
            }
        }).start();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "Device usb discovery error", e);
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.DEVICE_DISCOVERY_ERROR, R.string.Usb_discovery_error + ":" + e.getLocalizedMessage());
        }

        if(discoveredPrinterUsb == null)
        {
            return null;
        }
        Map<String, String> discoveryMap = null;
        Connection connection = null;
        try {
            connection = discoveredPrinterUsb.getConnection();
            connection.open();

            if(connection.isConnected()) {
                // Add some information that are not reported by the bluetooth discoverer
                discoveryMap = selectedPrinter.getDiscoveryDataMap();
                discoveryMap.put(PrinterDiscoveryDataMapKeys.LINK_OS_MAJOR_VER, SGD.GET("appl.link_os_version", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.PRODUCT_NAME, SGD.GET("device.product_name", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.FIRMWARE_VER, SGD.GET("appl.name", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.SERIAL_NUMBER, SGD.GET("device.unique_id", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.APL_MODE, SGD.GET("apl.enable", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.DEVICE_LANGUAGE, SGD.GET("device.languages", connection));
                discoveryMap.put(PrinterDiscoveryDataMapKeys.PDF_ENABLED, discoveryMap.get(PrinterDiscoveryDataMapKeys.APL_MODE).equalsIgnoreCase("pdf") ? "true" : "false");
                discoveryMap.put(PrinterDiscoveryDataMapKeys.CONNEXION_TYPE, PrinterDiscoveryDataMapKeys.CONNEXION_TYPE_USB);
            }
            else
            {
                discoveryMap = null;
            }

            connection.close();
        } catch (ConnectionException e) {
            Log.e(TAG, "Open connection error", e);
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, R.string.open_connection_error + ":" + e.getLocalizedMessage());
            discoveryMap = null;
        }

        Log.v(TAG, "DiscoveryMap: " + discoveryMap.toString());
        return discoveryMap;

    }

    // Handles USB device discovery
    static class UsbDiscoveryHandler implements DiscoveryHandler {
        public List<DiscoveredPrinterUsb> printers;
        public boolean discoveryComplete = false;

        public UsbDiscoveryHandler() {
            printers = new LinkedList<DiscoveredPrinterUsb>();
        }

        public void foundPrinter(final DiscoveredPrinter printer) {
            printers.add((DiscoveredPrinterUsb) printer);
        }

        public void discoveryFinished() {
            discoveryComplete = true;
        }

        public void discoveryError(String message) {
            discoveryComplete = true;
        }
    }
    public static boolean isLinkOsPrinter(DiscoveredPrinter printer) {
        Map<String, String> discoSettings = printer.getDiscoveryDataMap();
        if (discoSettings == null || !discoSettings.containsKey("LINK_OS_MAJOR_VER")) {
            return false;
        }
        try {
            return Double.parseDouble(discoSettings.get("LINK_OS_MAJOR_VER")) != 0.0d;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAplEnabled(DiscoveredPrinter printer) {
        Map<String, String> discoSettings = printer.getDiscoveryDataMap();
        return discoSettings != null && discoSettings.containsKey("APL_MODE") && !discoSettings.get("APL_MODE").equals("none");
    }

    public static String getAPLMode(DiscoveredPrinter printer)
    {
        Map<String, String> discoSettings = printer.getDiscoveryDataMap();
        return discoSettings.get(PrinterDiscoveryDataMapKeys.APL_MODE);
    }

}
