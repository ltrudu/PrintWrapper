package com.zebra.printwrapper;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.ProgressMonitor;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

public class SendPDFTask extends AsyncTask<Void, Boolean, Boolean> {

    public enum SendPDFTaskErrors
    {
        NO_PRINTER,
        EMPTY_PDF_FILE_PATH,
        PRINTER_PAUSED,
        HEAD_OPEN,
        PAPER_OUT,
        UNKNOWN_PRINTER_STATUS,
        CONNECTION_ERROR,
        PRINTER_LANGUAGE_UNKNOWN
    }

    public interface SendPDFTaskCallback
    {
        void onError(SendPDFTaskErrors error, String message);
        void onPrintProgress(String fileName, int progress,  int bytesWritten, int totalBytes);
        void onSuccess();
    }


    private static final String TAG = "SEND_PDF_TASK";
    private DiscoveredPrinter printer;
    private String filePath;
    private int scaleFactorX = -1;
    private int scaleFactorY = -1;
    private SendPDFTaskCallback callback = null;

    /**
     *
     * @param filePath
     * @param printer
     * @param scaleFactorX ScaleFactor on X axis in percentage (0-100)
     * @param scaleFactorY ScaleFactor on Y axis in percentage (0-100)
     */
    public SendPDFTask(String filePath, DiscoveredPrinter printer, int scaleFactorX, int scaleFactorY, SendPDFTaskCallback callback) {
        this.printer = printer;
        this.filePath = filePath;
        this.scaleFactorX = scaleFactorX;
        this.scaleFactorY = scaleFactorY;
        this.callback = callback;
    }

    public SendPDFTask(String filePath, DiscoveredPrinter printer, SendPDFTaskCallback callback) {
        this.printer = printer;
        this.filePath = filePath;
        this.scaleFactorX = -1;
        this.scaleFactorY = -1;
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
                callback.onError(SendPDFTaskErrors.EMPTY_PDF_FILE_PATH, "Empty PDF file path");
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
                    callback.onError(SendPDFTaskErrors.NO_PRINTER, "No printer found");
                }
                return;
            }

            PrinterStatus printerStatus = printer.getCurrentStatus();

            if(printerStatus.isReadyToPrint == false)
            {
                if (printerStatus.isPaused) {
                    if(callback != null)
                    {
                        callback.onError(SendPDFTaskErrors.PRINTER_PAUSED, "Printer is paused");
                    }
                    return;
                    }
                else if (printerStatus.isHeadOpen)
                {
                    if(callback != null)
                    {
                        callback.onError(SendPDFTaskErrors.HEAD_OPEN, "Printer's head is open");
                    }
                    return;
                } else if (printerStatus.isPaperOut)
                {
                    if(callback != null)
                    {
                        callback.onError(SendPDFTaskErrors.PAPER_OUT, "Paper is out");
                    }
                    return;
                }
                else
                {
                    if(callback != null)
                    {
                        callback.onError(SendPDFTaskErrors.UNKNOWN_PRINTER_STATUS, "Unknown printer status");
                    }
                    return;
                }
            }

            String scale = "dither scale-to-fit";
            if(scaleFactorY != -1 && scaleFactorY != -1)
            {
                scale = "dither scale=" + (int) scaleFactorX + "x" + (int) scaleFactorY;
            }

            SGD.SET("apl.settings",scale,connection);

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
                callback.onError(SendPDFTaskErrors.CONNECTION_ERROR, e.getLocalizedMessage());
            }
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendPDFTaskErrors.PRINTER_LANGUAGE_UNKNOWN, e.getLocalizedMessage());
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