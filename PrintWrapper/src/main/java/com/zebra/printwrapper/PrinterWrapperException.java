package com.zebra.printwrapper;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.util.Map;

public class PrinterWrapperException extends Exception{
    public PrinterConfig printerConfig;

    public PrinterWrapperException(Exception e, PrinterConfig aPrinterConfig)
    {
        super(e);
        printerConfig = aPrinterConfig;
    }

    public PrinterWrapperException(Exception e)
    {
        super(e);
        printerConfig = null;
    }

}
