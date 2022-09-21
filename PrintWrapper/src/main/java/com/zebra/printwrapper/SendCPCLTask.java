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

public class SendCPCLTask extends AsyncTask<Void, Boolean, Boolean> {

    public enum SendCPCLTaskErrors
    {
        NO_PRINTER,
        EMPTY_CPCL_STRING,
        PRINTER_PAUSED,
        HEAD_OPEN,
        PAPER_OUT,
        UNKNOWN_PRINTER_STATUS,
        CONNECTION_ERROR,
        PRINTER_LANGUAGE_UNKNOWN,
        ERROR_ZPL_PRINTER,
        ERROR_LINEPRINT_PRINTER
    }

    public interface SendCPCLTaskCallback
    {
        void onError(SendCPCLTaskErrors error, String message);
        void onSuccess();
    }


    private static final String TAG = "SEND_ZPL_TASK";
    private DiscoveredPrinter printer;
    private String cpclString;
    private SendCPCLTaskCallback callback = null;

    public SendCPCLTask(String cpclString, DiscoveredPrinter printer, SendCPCLTaskCallback callback) {
        this.printer = printer;
        this.cpclString = cpclString;
        this.callback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        sendPrint();
        return null;
    }

    private void sendPrint() {
        Log.i(TAG, "sendPrint()");

        if (cpclString == null)
        {
            if(callback != null)
            {
                callback.onError(SendCPCLTaskErrors.EMPTY_CPCL_STRING, "Empty CPCL string");
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
                    callback.onError(SendCPCLTaskErrors.NO_PRINTER, "No printer found");
                }
                return;
            }

            PrinterStatus printerStatus = printer.getCurrentStatus();

            if(printerStatus.isReadyToPrint == false)
            {
                if (printerStatus.isPaused) {
                    if(callback != null)
                    {
                        callback.onError(SendCPCLTaskErrors.PRINTER_PAUSED, "Printer is paused");
                    }
                    return;
                    }
                else if (printerStatus.isHeadOpen)
                {
                    if(callback != null)
                    {
                        callback.onError(SendCPCLTaskErrors.HEAD_OPEN, "Printer's head is open");
                    }
                    return;
                } else if (printerStatus.isPaperOut)
                {
                    if(callback != null)
                    {
                        callback.onError(SendCPCLTaskErrors.PAPER_OUT, "Paper is out");
                    }
                    return;
                }
                else
                {
                    if(callback != null)
                    {
                        callback.onError(SendCPCLTaskErrors.UNKNOWN_PRINTER_STATUS, "Unknown printer status");
                    }
                    return;
                }
            }

            PrinterLanguage pl = printer.getPrinterControlLanguage();
            if (pl != PrinterLanguage.CPCL) {
                if(callback != null)
                {
                    callback.onError(pl == PrinterLanguage.ZPL ? SendCPCLTask.SendCPCLTaskErrors.ERROR_ZPL_PRINTER : SendCPCLTask.SendCPCLTaskErrors.ERROR_LINEPRINT_PRINTER, "Can't send CPCL content to a " + (pl == PrinterLanguage.ZPL ? "ZPL" : "LINEPRINT") + " printer.");
                }
                return;
            }

            byte[] cpclBytes = cpclString.getBytes();
            connection.write(cpclBytes);
        } catch (ConnectionException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendCPCLTaskErrors.CONNECTION_ERROR, e.getLocalizedMessage());
            }
        } catch (ZebraPrinterLanguageUnknownException e) {
            e.printStackTrace();
            if(callback != null)
            {
                callback.onError(SendCPCLTaskErrors.PRINTER_LANGUAGE_UNKNOWN, e.getLocalizedMessage());
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