package com.zebra.printwrapper;

import android.content.Context;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.MultichannelTcpConnection;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.io.IOException;
import java.util.Map;

public class PrinterDiscoveryDataMapHelper {

    private static final String TAG = "BTHelper";

    public static boolean populateBluetoothPrinterDiscoveryMap(final DiscoveredPrinter selectedPrinter, final SelectedPrinterTaskCallbacks callback, final Context context)
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

        boolean result = false;

        try {
            Connection connection = new BluetoothConnectionQuickClose(selectedPrinter.address);
            connection.open();
            try {
                ZebraPrinter zebraPrinter = ZebraPrinterFactory.getInstance(connection);
                boolean isPdfPrinter = SGDHelper.isPDFEnabled(connection);
                if (!isPdfPrinter) {
                    String errorMessage = context.getString(R.string.wrong_firmware);
                    if(callback != null)
                        callback.onError(SelectedPrinterTaskError.WRONG_FIRMWARE, errorMessage);
                    connection.close();
                    return false;
                }
            }catch (ZebraPrinterLanguageUnknownException e)
            {
                if(callback != null)
                    callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, context.getString(R.string.open_connection_error));
                Log.e(TAG, context.getString(R.string.open_connection_error), e);
            };

            // Add some information that are not reported by the bluetooth discoverer
            Map<String, String> discoveryMap = selectedPrinter.getDiscoveryDataMap();
            discoveryMap.put(PrinterDiscoveryDataMapKeys.LINK_OS_MAJOR_VER, SGD.GET("appl.link_os_version", connection));
            discoveryMap.put(PrinterDiscoveryDataMapKeys.PRODUCT_NAME, SGD.GET("device.product_name", connection));
            discoveryMap.put(PrinterDiscoveryDataMapKeys.SYSTEM_NAME, SGD.GET("bluetooth.friendly_name", connection));
            discoveryMap.put(PrinterDiscoveryDataMapKeys.HARDWARE_ADDRESS, SGD.GET("bluetooth.address", connection));
            discoveryMap.put(PrinterDiscoveryDataMapKeys.FIRMWARE_VER, SGD.GET("appl.name", connection));
            discoveryMap.put(PrinterDiscoveryDataMapKeys.SERIAL_NUMBER, SGD.GET("device.unique_id", connection));
            discoveryMap.put(PrinterDiscoveryDataMapKeys.APL_MODE, SGDHelper.getAplMode(connection));
            discoveryMap.put(PrinterDiscoveryDataMapKeys.PDF_ENABLED, discoveryMap.get(PrinterDiscoveryDataMapKeys.APL_MODE).equalsIgnoreCase("pdf") ? "true" : "false");
            discoveryMap.put(PrinterDiscoveryDataMapKeys.CONNEXION_TYPE, PrinterDiscoveryDataMapKeys.CONNEXION_TYPE_BLUETOOTH);

            result = true;

            connection.close();

        } catch (ConnectionException e) {
            Log.e(TAG, "Open connection error", e);
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, R.string.open_connection_error + ":" + e.getLocalizedMessage());
            result = false;
        }

        return result;

    }

    public static boolean populateNetworkPrinterDiscoveryMap(final DiscoveredPrinter selectedPrinter, final SelectedPrinterTaskCallbacks callback, final Context context)
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


        boolean result = false;
        Connection connection = new MultichannelTcpConnection(selectedPrinter);
        try {
            Map<String, String> discoveryDataMap = selectedPrinter.getDiscoveryDataMap();
            connection.open();
            boolean isPdfPrinter = SGDHelper.isPDFEnabled(connection);

            try {
                ZebraPrinter zebraPrinter = ZebraPrinterFactory.getInstance(connection);
                if (!isPdfPrinter) {
                    String errorMessage = context.getString(R.string.wrong_firmware);
                    if(callback != null)
                        callback.onError(SelectedPrinterTaskError.WRONG_FIRMWARE, errorMessage);
                    connection.close();
                    return false;
                }
            }catch (ZebraPrinterLanguageUnknownException e)
            {
                if(callback != null)
                    callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, context.getString(R.string.open_connection_error));
                Log.e(TAG, context.getString(R.string.open_connection_error), e);
            };

            Map<String, String> discoveryMap = selectedPrinter.getDiscoveryDataMap();
            discoveryMap.put(PrinterDiscoveryDataMapKeys.APL_MODE, SGDHelper.getAplMode(connection));
            discoveryMap.put(PrinterDiscoveryDataMapKeys.PDF_ENABLED, isPdfPrinter ? "true" : "false");
            discoveryMap.put(PrinterDiscoveryDataMapKeys.CONNEXION_TYPE, PrinterDiscoveryDataMapKeys.CONNEXION_TYPE_NETWORK);
            result = true;

            connection.close();

        } catch (ConnectionException e) {
            Log.e(TAG, "Open connection error", e);
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.OPEN_CONNECTION_ERROR, R.string.open_connection_error + ":" + e.getLocalizedMessage());
            result = false;
        }

        return result;

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
