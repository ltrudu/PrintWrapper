package com.zebra.printwrapper;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.ProgressMonitor;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.io.IOException;

public class SendImageTask extends AsyncTask<Void, Boolean, Boolean> {

    public enum SendImageTaskErrors
    {
        NO_PRINTER,
        EMPTY_FILE_PATH,
        PRINTER_PAUSED,
        HEAD_OPEN,
        PAPER_OUT,
        UNKNOWN_PRINTER_STATUS,
        CONNECTION_ERROR,
        PRINTER_LANGUAGE_UNKNOWN,
        RUNTIME_EXCEPTION,
        IO_EXCEPTION
    }

    public interface SendImageTaskCallback
    {
        void onError(SendImageTaskErrors error, String message);
        void onSuccess();
    }


    private static final String TAG = "SEND_IMAGE_TASK";
    private DiscoveredPrinter printer;
    private String filePath;

    private boolean storeImage = false;
    private SendImageTaskCallback callback = null;

    /**
     *
     * @param filePath
     * @param printer
     */
    public SendImageTask(String filePath, boolean storeImage, DiscoveredPrinter printer, SendImageTaskCallback callback) {
        this.printer = printer;
        this.filePath = filePath;
        this.callback = callback;
        this.storeImage = storeImage;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        sendPrint();
        return null;
    }

    // Sets the scaling on the printer and then sends the pdf file to the printer
    private void sendPrint() {
        Log.i(TAG, "sendPrint()");

        if (filePath == null)
        {
            if(callback != null)
            {
                callback.onError(SendImageTaskErrors.EMPTY_FILE_PATH, "Empty PDF file path");
            }
            return;
        }

        Connection connection = null;
        if(PrintWrapperHelpers.isBluetoothPrinter(printer))
        {
            connection = new BluetoothConnection(printer.address);
        }
        else
        {
            connection = new TcpConnection(printer.address, PrintWrapperHelpers.getNetworkPrinterPort(printer));
        }

        try {
            connection.open();
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);

            if(printer == null)
            {
                if(callback != null)
                {
                    callback.onError(SendImageTaskErrors.NO_PRINTER, "No printer found");
                }
                return;
            }

            PrinterStatus printerStatus = printer.getCurrentStatus();

            if(printerStatus.isReadyToPrint == false)
            {
                if (printerStatus.isPaused) {
                    if(callback != null)
                    {
                        callback.onError(SendImageTaskErrors.PRINTER_PAUSED, "Printer is paused");
                    }
                    return;
                    }
                else if (printerStatus.isHeadOpen)
                {
                    if(callback != null)
                    {
                        callback.onError(SendImageTaskErrors.HEAD_OPEN, "Printer's head is open");
                    }
                    return;
                } else if (printerStatus.isPaperOut)
                {
                    if(callback != null)
                    {
                        callback.onError(SendImageTaskErrors.PAPER_OUT, "Paper is out");
                    }
                    return;
                }
                else
                {
                    if(callback != null)
                    {
                        callback.onError(SendImageTaskErrors.UNKNOWN_PRINTER_STATUS, "Unknown printer status");
                    }
                    return;
                }
            }

            Bitmap image = RasterizationHelper.loadFromFile(filePath);
            ZebraImageAndroid zebraImage = new ZebraImageAndroid(image);

            if(storeImage)
            {
                printer.storeImage(filePath, zebraImage, zebraImage.getWidth(), zebraImage.getHeight());
            }
            else
            {
                printer.printImage(zebraImage, 0,0, zebraImage.getWidth(), zebraImage.getHeight(), false);
            }
        } catch (ConnectionException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendImageTaskErrors.CONNECTION_ERROR, e.getLocalizedMessage());
            }
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendImageTaskErrors.PRINTER_LANGUAGE_UNKNOWN, e.getLocalizedMessage());
            }
        } catch (ZebraIllegalArgumentException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendImageTaskErrors.RUNTIME_EXCEPTION, e.getLocalizedMessage());
            }
        } finally {
            try {
                connection.close();
                if(callback != null)
                    callback.onSuccess();
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
        }
    }
}