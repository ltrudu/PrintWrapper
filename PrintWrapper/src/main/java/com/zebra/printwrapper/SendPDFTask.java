package com.zebra.printwrapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfRenderer;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
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

    private boolean bVariableLength = false;

    private int nVariableLengthTopMargin = 10;
    private SendPDFTaskCallback callback = null;

    private int nbCopies = 1;

    private int mDPI = 200;
    private int mJpegQuality = 75;
    private Context context = null;

    private static boolean pdfBoxInitialized = false;

    /**
     *
     * @param filePath
     * @param printer
     * @param scaleFactorX ScaleFactor on X axis in percentage (0-100)
     * @param scaleFactorY ScaleFactor on Y axis in percentage (0-100)
     */
    public SendPDFTask(Context context, String filePath, DiscoveredPrinter printer, int nbCopies, int scaleFactorX, int scaleFactorY, boolean variableLengthMode, int nVariableLengthTopMargin, SendPDFTaskCallback callback) {
        this.printer = printer;
        this.filePath = filePath;
        this.scaleFactorX = scaleFactorX;
        this.scaleFactorY = scaleFactorY;
        this.callback = callback;
        this.context = context;
        this.bVariableLength = variableLengthMode;
        this.nVariableLengthTopMargin = nVariableLengthTopMargin;
        this.nbCopies = nbCopies;
        if(pdfBoxInitialized == false)
        {
            PDFBoxResourceLoader.init(context.getApplicationContext());
            pdfBoxInitialized = true;
        }
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

            if(bVariableLength && pageCount > 1)
            {
                // Flatten document into one big page.
                File finalPathFile = FlattenDocumentIntoOnePage(new File(filePath));
                finalPath = finalPathFile.getPath();
            }

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
                        String zpl = RasterizationHelper.getZPLFromPDFFile(filePath, page, mDPI, nbCopies, bVariableLength, nVariableLengthTopMargin);
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

    private File FlattenDocumentIntoOnePage(File originalTempFile)
    {
        try {

            // Extract all the pages as bitmap files
            PdfRenderer renderer = new PdfRenderer(ParcelFileDescriptor.open(originalTempFile,ParcelFileDescriptor.MODE_READ_ONLY));
            int pageCount = renderer.getPageCount();

            File newFile = File.createTempFile(originalTempFile.getName() + ".flat.pdf", null, context.getCacheDir());
            newFile.deleteOnExit();
            PdfRenderer.Page page = null;
            ArrayList<Bitmap> trimmedPages = new ArrayList<>(renderer.getPageCount());
            for(int currentPage = 0; currentPage < renderer.getPageCount(); currentPage++)
            {
                // Let's get pages one by one
                page = renderer.openPage(currentPage);

                //We keep the original document sizes
                //int iWidth = page.getWidth();
                //int iHeight = page.getHeight();

                //Calculate Bitmap Size according to printer data
                int iWidth = mDPI * page.getWidth() / 72;
                int iHeight = mDPI * page.getHeight() / 72;
                iWidth = ((iWidth + 7) / 8)<<3;

                //Render the Bitmap
                Log.i(TAG,"Rendering Size :" + iWidth + "," + iHeight);
                Bitmap bitmap = Bitmap.createBitmap(iWidth, iHeight, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);

                // Now let's trim the bitmap image
                Bitmap trimmed = imageWithMargin(bitmap, 0, 1, false);
                bitmap.recycle();

                // Add the trimmed page to the list
                trimmedPages.add(trimmed);

                page.close();
            }

            Bitmap toPrint = null;
            if(renderer.getPageCount() > 1) {
                // Combine all the bitmaps into one
                toPrint = combineImageIntoOne(trimmedPages);
                for (Bitmap toRecycle : trimmedPages) {
                    toRecycle.recycle();
                }
            }
            else
            {
                toPrint = trimmedPages.get(0);
            }

            //Old method that generates too many problems
            //boolean succeeded = createPDFFileFromBitmap(combined, newFile);
            // TODO: Tests should be done with JPEG Quality to reduce the file size and make it
            // usable on a PDFDirect printer.
            // This settings should be available when using a PDFDirect printer in variable length mode (printQuality from 0.5f to 1.0f)
            // Explaining that it has impact on the size of the data transfered to the printer
            boolean succeeded = createPDFFileFromBitmapPDFBox(toPrint, newFile, mJpegQuality/100.0f);
            toPrint.recycle();
            return succeeded ? newFile : null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap combineImageIntoOne(ArrayList<Bitmap> bitmap) {
        if(bitmap.size() == 1)
            return bitmap.get(0);

        int w = 0, h = 0;

        for (int i = 0; i < bitmap.size(); i++) {
            if (i < bitmap.size() - 1) {
                w = bitmap.get(i).getWidth() > bitmap.get(i + 1).getWidth() ? bitmap.get(i).getWidth() : bitmap.get(i + 1).getWidth();
            }
            h += bitmap.get(i).getHeight();
        }

        Bitmap temp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(temp);
        int top = 0;
        for (int i = 0; i < bitmap.size(); i++) {
            Log.d(TAG, "Combine: "+i+"/"+bitmap.size()+1);

            top = (i == 0 ? 0 : top+bitmap.get(i-1).getHeight());
            canvas.drawBitmap(bitmap.get(i), 0f, top, null);
        }
        return temp;
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

    private boolean createPDFFileFromBitmapPDFBox(Bitmap bitmap, File targetFile, float jpegQuality) {
        try {
            PDDocument document = new PDDocument();
            PDPage page = new PDPage(new PDRectangle(bitmap.getWidth(), bitmap.getHeight()));
            document.addPage(page);

            // Define a content stream for adding to the PDF

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Here you have great control of the compression rate and DPI on your image.
            // Update 2017/11/22: The DPI param actually is useless as of current version v1.8.9.1 if you take a look into the source code. Compression rate is enough to achieve a much smaller file size.
            PDImageXObject ximage = JPEGFactory.createFromImage(document, bitmap, 0.70f);

            // You may want to call PDPage.getCropBox() in order to place your image
            // somewhere inside this page rect with (x, y) and (width, height).
            contentStream.drawImage(ximage, 0, 0);

            // Make sure that the content stream is closed:
            contentStream.close();

            document.save(targetFile);
            document.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}