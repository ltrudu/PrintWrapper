package com.zebra.printwrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class SendPDFTask extends ExecutorTask<Void, Boolean, Boolean> {

    public enum SendPDFTaskErrors
    {
        NO_PRINTER,
        EMPTY_PDF_FILE_PATH,
        PRINTER_PAUSED,
        HEAD_OPEN,
        PAPER_OUT,
        UNKNOWN_PRINTER_STATUS,
        CONNECTION_ERROR,
        PRINTER_LANGUAGE_UNKNOWN,

        FILE_NOT_FOUND_ERROR,

        CONFIGURATION_ERROR
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

    private int nbCopies = 1;

    private int mDPI = 200;
    private int mJpegQuality = 75;
    private Context context = null;

    /**
     *
     * @param filePath
     * @param printer
     * @param scaleFactorX ScaleFactor on X axis in percentage (0-100)
     * @param scaleFactorY ScaleFactor on Y axis in percentage (0-100)
     */
    public SendPDFTask(Context context, String filePath, DiscoveredPrinter printer, int nbCopies, int scaleFactorX, int scaleFactorY, SendPDFTaskCallback callback) {
        this.printer = printer;
        this.filePath = filePath;
        this.scaleFactorX = scaleFactorX;
        this.scaleFactorY = scaleFactorY;
        this.callback = callback;
        this.context = context;
        this.nbCopies = nbCopies;
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

        Connection connection = printer.getConnection();

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

            String finalPath = filePath;
            mDPI = SGDHelper.getMdpi(connection);

            PdfRenderer renderer = null;
            try {
                renderer = new PdfRenderer(ParcelFileDescriptor.open(new File(filePath), ParcelFileDescriptor.MODE_READ_ONLY));
            } catch (IOException e) {
                Log.e(TAG, "File not found");
                if(callback != null)
                {
                    callback.onError(SendPDFTaskErrors.FILE_NOT_FOUND_ERROR, e.getLocalizedMessage());
                    return;
                }
            }
            int pageCount = renderer.getPageCount();

            String scale = "dither scale-to-fit";
            if (scaleFactorY != -1 && scaleFactorY != -1) {
                scale = "dither scale=" + (int) scaleFactorX + "x" + (int) scaleFactorY;
            }

            SGD.SET("apl.settings", scale, connection);

            if(SGDHelper.isPDFEnabled(connection)) {

                for(int copy = 0; copy < nbCopies; copy++) {
                    printer.sendFileContents(finalPath, new ProgressMonitor() {
                        @Override
                        public void updateProgress(int bytesWritten, int totalBytes) {
                            // Calc Progress
                            double rawProgress = bytesWritten * 100 / totalBytes;
                            int progress = (int) Math.round(rawProgress);

                            // Notify progress
                            if (callback != null) {
                                callback.onPrintProgress(filePath, progress, bytesWritten, totalBytes);
                            }
                        }
                    });
                }
            }
            else
            {
                PrinterLanguage pl = printer.getPrinterControlLanguage();
                if (pl == PrinterLanguage.ZPL) {

                    for (int page = 0; page < pageCount; page++) {
                        // Create a zpl for each page and send it to the printer
                        String zpl = RasterizationHelper.getZPLFromPDFFile(filePath, page, mDPI, nbCopies);
                        Log.v(TAG, "Zpl to print:\n" + "--------------------------\n" + zpl + "\n--------------------");
                        Log.v(TAG, "Printing page:" + (page + 1) + " using ZPL.");
                        byte[] zplBytes = zpl.getBytes();
                        connection.write(zplBytes);
                    }
                }
                else
                {
                    if(pl == PrinterLanguage.CPCL)
                    {
                        for (int page = 0; page < pageCount; page++) {
                            // Create a cpcl for each page and send it to the printer
                            String zpl = RasterizationHelper.getCPCFromPDFFile(filePath, page, mDPI, nbCopies);
                            Log.v(TAG, "Zpl to print:\n" + "--------------------------\n" + zpl + "\n--------------------");
                            Log.v(TAG, "Printing page:" + (page + 1) + " using ZPL.");
                            byte[] zplBytes = zpl.getBytes();
                            connection.write(zplBytes);
                        }
                    }
                    else if (pl == PrinterLanguage.LINE_PRINT)
                    {
                        if(callback != null)
                        {
                            callback.onError(SendPDFTaskErrors.CONFIGURATION_ERROR, "Pdf can't be printed in line_print mode");
                            return;
                        }
                    }
                    else
                    {
                        Log.e(TAG, "Language not found");
                        if(callback != null)
                        {
                            callback.onError(SendPDFTaskErrors.PRINTER_LANGUAGE_UNKNOWN, "pl = " + pl.toString());
                            return;
                        }
                    }
                }
            }
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

    public static Bitmap imageWithMargin(Bitmap bitmap, int color, int maxMargin, boolean removeLeftRightMargin) {
        int maxTop = 0, maxBottom = 0, maxLeft = bitmap.getWidth(), maxRight = 0;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] bitmapArray = new int[width * height];
        bitmap.getPixels(bitmapArray, 0, width, 0, 0, width, height);

        // Find first non-color pixel from top of bitmap
        searchTopMargin:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (bitmapArray[width * y + x] != color) {
                    maxTop = (y > maxMargin ? y - maxMargin : 0);
                    break searchTopMargin;
                }
            }
        }

        // Find first non-color pixel from bottom of bitmap
        searchBottomMargin:
        for (int y = height - 1; y >= 0; y--) {
            for (int x = width - 1; x >= 0; x--) {
                if (bitmapArray[width * y + x] != color) {
                    maxBottom = y < height - maxMargin ? y + maxMargin : height;
                    break searchBottomMargin;
                }
            }
        }

        if(removeLeftRightMargin) {
            // We scan all the image from top to bottom to get the minimal margin for left
            searchLeftMargin:
            for (int x = 0; x < width; x++) {
                for (int y = (maxTop + maxMargin); y < (maxBottom - maxMargin); y++) {
                    if (bitmapArray[width * y + x] != color) {
                        int foundMaxLeft = (x > maxMargin ? x - maxMargin : 0);
                        maxLeft = (foundMaxLeft < maxLeft) ? foundMaxLeft : maxLeft;
                    }
                }
            }

            // We scan all the image from top to bottom to get the maximal margin for right
            searchRightMargin:
            for (int x = width - 1; x >= 0; x--) {
                for (int y = (maxTop + maxMargin); y < (maxBottom - maxMargin); y++) {
                    if (bitmapArray[width * y + x] != color) {
                        int foundMaxRight = x < width - maxMargin ? x + maxMargin : width;
                        maxRight = foundMaxRight > maxRight ? foundMaxRight : maxRight;
                    }
                }
            }
        }
        else
        {
            maxLeft = 0;
            maxRight = bitmap.getWidth();
        }

        return Bitmap.createBitmap(bitmap, maxLeft, maxTop, maxRight - maxLeft, maxBottom - maxTop);
    }
}