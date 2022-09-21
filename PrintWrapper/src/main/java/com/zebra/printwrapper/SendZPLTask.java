package com.zebra.printwrapper;

import android.os.AsyncTask;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.device.ProgressMonitor;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

public class SendZPLTask extends AsyncTask<Void, Boolean, Boolean> {

    public enum SendZPLTaskErrors
    {
        NO_PRINTER,
        EMPTY_ZPL_STRING,
        PRINTER_PAUSED,
        HEAD_OPEN,
        PAPER_OUT,
        UNKNOWN_PRINTER_STATUS,
        CONNECTION_ERROR,
        PRINTER_LANGUAGE_UNKNOWN,
        ERROR_CPCL_PRINTER,
        ERROR_LINEPRINT_PRINTER
    }

    public interface SendZPLTaskCallback
    {
        void onError(SendZPLTaskErrors error, String message);
        void onSuccess();
    }


    private static final String TAG = "SEND_ZPL_TASK";
    private DiscoveredPrinter printer;
    private String zplString;
    private SendZPLTaskCallback callback = null;

    public SendZPLTask(String zplString, DiscoveredPrinter printer, SendZPLTaskCallback callback) {
        this.printer = printer;
        this.zplString = zplString;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        sendPrint();
        return null;
    }

    private void sendPrint() {
        Log.i(TAG, "sendPrint()");

        if (zplString == null)
        {
            if(callback != null)
            {
                callback.onError(SendZPLTaskErrors.EMPTY_ZPL_STRING, "Empty ZPL string");
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
                    callback.onError(SendZPLTaskErrors.NO_PRINTER, "No printer found");
                }
                return;
            }

            PrinterStatus printerStatus = printer.getCurrentStatus();

            if(printerStatus.isReadyToPrint == false)
            {
                if (printerStatus.isPaused) {
                    if(callback != null)
                    {
                        callback.onError(SendZPLTaskErrors.PRINTER_PAUSED, "Printer is paused");
                    }
                    return;
                    }
                else if (printerStatus.isHeadOpen)
                {
                    if(callback != null)
                    {
                        callback.onError(SendZPLTaskErrors.HEAD_OPEN, "Printer's head is open");
                    }
                    return;
                } else if (printerStatus.isPaperOut)
                {
                    if(callback != null)
                    {
                        callback.onError(SendZPLTaskErrors.PAPER_OUT, "Paper is out");
                    }
                    return;
                }
                else
                {
                    if(callback != null)
                    {
                        callback.onError(SendZPLTaskErrors.UNKNOWN_PRINTER_STATUS, "Unknown printer status");
                    }
                    return;
                }
            }

            PrinterLanguage pl = printer.getPrinterControlLanguage();
            if (pl != PrinterLanguage.ZPL) {
                if(callback != null)
                {
                    callback.onError(pl == PrinterLanguage.CPCL ? SendZPLTaskErrors.ERROR_CPCL_PRINTER : SendZPLTaskErrors.ERROR_LINEPRINT_PRINTER, "Can't send ZPL content to a " + (pl == PrinterLanguage.CPCL ? "CPCL" : "LINEPRINT") + " printer.");
                }
                return;
            }

            byte[] zplBytes = zplString.getBytes();
            connection.write(zplBytes);
        } catch (ConnectionException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendZPLTaskErrors.CONNECTION_ERROR, e.getLocalizedMessage());
            }
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendZPLTaskErrors.PRINTER_LANGUAGE_UNKNOWN, e.getLocalizedMessage());
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