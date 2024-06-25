package com.zebra.printwrapper;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

public class GetPrinterStatusTask extends ExecutorTask<DiscoveredPrinter, Boolean, PrinterStatus>{
    public PrinterStatus printerStatus = null;

    @Override
    protected PrinterStatus doInBackground(DiscoveredPrinter... printerParams) {
        DiscoveredPrinter selectedPrinter = printerParams[0];
        PrinterStatus printerStatus = null;
        if(selectedPrinter != null)
        {
            if(selectedPrinter.getConnection() != null ) {
                Connection connection = selectedPrinter.getConnection();
                if(connection.isConnected() == false) {
                    try {
                        connection.open();
                        ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
                        if(printer != null)
                        {
                            printerStatus = printer.getCurrentStatus();
                        }
                    } catch (ConnectionException e) {
                        throw new RuntimeException(e);
                    } catch (ZebraPrinterLanguageUnknownException e) {
                        throw new RuntimeException(e);
                    }
                    finally {
                        try {
                            connection.close();
                        } catch (ConnectionException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    return printerStatus;
    }

    @Override
    protected void onPostExecute(PrinterStatus printerStatus) {
        super.onPostExecute(printerStatus);
        this.printerStatus = printerStatus;
    }
}
