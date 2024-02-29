package com.zebra.printwrapper;

import android.os.AsyncTask;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.ProgressMonitor;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

public class SendFileTask extends ExecutorTask<Void, Boolean, Boolean> {

    public enum SendFileTaskErrors
    {
        NO_PRINTER,
        EMPTY_FILE_PATH,
        PRINTER_PAUSED,
        HEAD_OPEN,
        PAPER_OUT,
        UNKNOWN_PRINTER_STATUS,
        CONNECTION_ERROR,
        PRINTER_LANGUAGE_UNKNOWN
    }

    public interface SendFileTaskCallback
    {
        void onError(SendFileTaskErrors error, String message);
        void onPrintProgress(String fileName, int progress, int bytesWritten, int totalBytes);
        void onSuccess();
    }


    private static final String TAG = "SEND_FILE_TASK";
    private DiscoveredPrinter printer;
    private String filePath;
    private SendFileTaskCallback callback = null;

    /**
     *
     * @param filePath
     * @param printer
     */
    public SendFileTask(String filePath, DiscoveredPrinter printer, SendFileTaskCallback callback) {
        this.printer = printer;
        this.filePath = filePath;
        this.callback = callback;
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
                callback.onError(SendFileTaskErrors.EMPTY_FILE_PATH, "Empty PDF file path");
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
                    callback.onError(SendFileTaskErrors.NO_PRINTER, "No printer found");
                }
                return;
            }

            PrinterStatus printerStatus = printer.getCurrentStatus();

            if(printerStatus.isReadyToPrint == false)
            {
                if (printerStatus.isPaused) {
                    if(callback != null)
                    {
                        callback.onError(SendFileTaskErrors.PRINTER_PAUSED, "Printer is paused");
                    }
                    return;
                    }
                else if (printerStatus.isHeadOpen)
                {
                    if(callback != null)
                    {
                        callback.onError(SendFileTaskErrors.HEAD_OPEN, "Printer's head is open");
                    }
                    return;
                } else if (printerStatus.isPaperOut)
                {
                    if(callback != null)
                    {
                        callback.onError(SendFileTaskErrors.PAPER_OUT, "Paper is out");
                    }
                    return;
                }
                else
                {
                    if(callback != null)
                    {
                        callback.onError(SendFileTaskErrors.UNKNOWN_PRINTER_STATUS, "Unknown printer status");
                    }
                    return;
                }
            }

            printer.sendFileContents(filePath, new ProgressMonitor() {
                @Override
                public void updateProgress(int bytesWritten, int totalBytes) {
                    // Calc Progress
                    double rawProgress = bytesWritten * 100 / totalBytes;
                    int progress = (int) Math.round(rawProgress);

                    // Notify progress
                    if(callback != null)
                    {
                        callback.onPrintProgress(filePath, progress, bytesWritten, totalBytes);
                    }
                }
            });
        } catch (ConnectionException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendFileTaskErrors.CONNECTION_ERROR, e.getLocalizedMessage());
            }
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendFileTaskErrors.PRINTER_LANGUAGE_UNKNOWN, e.getLocalizedMessage());
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