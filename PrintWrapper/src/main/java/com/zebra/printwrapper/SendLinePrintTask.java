package com.zebra.printwrapper;

import android.os.AsyncTask;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

public class SendLinePrintTask extends ExecutorTask<Void, Boolean, Boolean> {

    public enum SendLinePrintTaskErrors
    {
        NO_PRINTER,
        EMPTY_LINEPRINT_STRING,
        PRINTER_PAUSED,
        HEAD_OPEN,
        PAPER_OUT,
        UNKNOWN_PRINTER_STATUS,
        CONNECTION_ERROR,
        PRINTER_LANGUAGE_UNKNOWN,
        ERROR_ZPL_PRINTER,
        ERROR_CPCL_PRINTER
    }

    public interface SendLinePrintTaskCallback
    {
        void onError(SendLinePrintTaskErrors error, String message);
        void onSuccess();
    }


    private static final String TAG = "SEND_LINEPRINT_TASK";
    private DiscoveredPrinter printer;
    private String lineprintString;
    private SendLinePrintTaskCallback callback = null;

    public SendLinePrintTask(String lineprintString, DiscoveredPrinter printer, SendLinePrintTaskCallback callback) {
        this.printer = printer;
        this.lineprintString = lineprintString;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        sendPrint();
        return null;
    }

    private void sendPrint() {
        Log.i(TAG, "sendPrint()");

        if (lineprintString == null)
        {
            if(callback != null)
            {
                callback.onError(SendLinePrintTaskErrors.EMPTY_LINEPRINT_STRING, "Empty lineprint string");
            }
            return;
        }

        Connection connection = printer.getConnection();

        try {
            connection.open();
            ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);

            if(printer == null)
            {
                if(callback != null)
                {
                    callback.onError(SendLinePrintTaskErrors.NO_PRINTER, "No printer found");
                }
                return;
            }

            PrinterStatus printerStatus = printer.getCurrentStatus();

            if(printerStatus.isReadyToPrint == false)
            {
                if (printerStatus.isPaused) {
                    if(callback != null)
                    {
                        callback.onError(SendLinePrintTaskErrors.PRINTER_PAUSED, "Printer is paused");
                    }
                    return;
                    }
                else if (printerStatus.isHeadOpen)
                {
                    if(callback != null)
                    {
                        callback.onError(SendLinePrintTaskErrors.HEAD_OPEN, "Printer's head is open");
                    }
                    return;
                } else if (printerStatus.isPaperOut)
                {
                    if(callback != null)
                    {
                        callback.onError(SendLinePrintTaskErrors.PAPER_OUT, "Paper is out");
                    }
                    return;
                }
                else
                {
                    if(callback != null)
                    {
                        callback.onError(SendLinePrintTaskErrors.UNKNOWN_PRINTER_STATUS, "Unknown printer status");
                    }
                    return;
                }
            }

            PrinterLanguage pl = printer.getPrinterControlLanguage();
            if (pl != PrinterLanguage.LINE_PRINT) {
                if(callback != null)
                {
                    callback.onError(pl == PrinterLanguage.ZPL ? SendLinePrintTask.SendLinePrintTaskErrors.ERROR_ZPL_PRINTER : SendLinePrintTask.SendLinePrintTaskErrors.ERROR_CPCL_PRINTER, "Can't send lineprint content to a " + (pl == PrinterLanguage.ZPL ? "ZPL" : "CPCL") + " printer.");
                }
                return;
            }

            byte[] lineprintBytes = lineprintString.getBytes();
            connection.write(lineprintBytes);
        } catch (ConnectionException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendLinePrintTaskErrors.CONNECTION_ERROR, e.getLocalizedMessage());
            }
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendLinePrintTaskErrors.PRINTER_LANGUAGE_UNKNOWN, e.getLocalizedMessage());
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