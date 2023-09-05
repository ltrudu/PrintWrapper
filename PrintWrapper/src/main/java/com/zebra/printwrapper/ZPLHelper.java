package com.zebra.printwrapper;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class ZPLHelper {
    private static String TAG = ZPLHelper.class.getName();


    public static native String createBitmapZPL(Bitmap bitmap);

    public static String getZPL(String filePath, int pageNumber, int dpi, int nbCopies, boolean variableLengthEnabled, int vlTopMargin ) {
        PdfRenderer renderer = null;
        try {
            renderer = new PdfRenderer(ParcelFileDescriptor.open(new File(filePath), ParcelFileDescriptor.MODE_READ_ONLY));
        } catch (IOException e) {
            Log.e(TAG, "File not found");
            return null;
        }
        int pageCount = renderer.getPageCount();
        int pageToExtract = pageNumber;
        if (pageToExtract >= pageCount) {
            Log.e(TAG, "Page:" + pageNumber + " is greater than pagecount:" + pageCount);
            Log.e(TAG, "Selecting last page: " + (pageCount - 1));
            // We select the last page
            pageToExtract = pageCount - 1;
        } else if (pageToExtract < 0) {
            Log.e(TAG, "Page:" + pageNumber + " is smaller than 0");
            Log.e(TAG, "Selecting first page: 0");
            // We select the first page
            pageToExtract = 0;
        }
        PdfRenderer.Page page = renderer.openPage(pageToExtract);
        int iWidth;
        int iHeight;
        int iSize;

        Bitmap bitmap;
        if (variableLengthEnabled == true) {
            iWidth = page.getWidth();
            iHeight = page.getHeight();
            iSize = iWidth * iHeight;
            bitmap = Bitmap.createBitmap(iWidth, iHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
            iWidth = (iWidth >> 3);
        } else {
            //Calculate Bitmap Size
            iWidth = dpi * page.getWidth() / 72;
            iHeight = dpi * page.getHeight() / 72;
            iWidth = (iWidth + 7) / 8;
            iSize = iWidth * iHeight;

            //Render the Bitmap
            Log.i(TAG, "Rendering Size :" + iWidth + "," + iHeight);
            bitmap = Bitmap.createBitmap(iWidth << 3, iHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
        }

        StringBuilder printData = new StringBuilder();
        // By default create ZPL Data
        //Create ZPL
        String ZPLBitmap = ZPLHelper.createBitmapZPL(bitmap);
        Log.i(TAG, "Creating ZPL");
        printData.append("^XA");
        if(variableLengthEnabled == true)
        {
            printData.append("^MNN");
            printData.append("^LL"+ (iHeight + vlTopMargin));
            printData.append("^LH0," + vlTopMargin);
        }
        printData.append("^PW"+(iWidth*8));
        printData.append("^FO,0,0^GFA," + iSize + "," + iSize + "," + iWidth + ",");
        printData.append(ZPLBitmap);
        if(nbCopies > 1) {
            // Add ^PQ to jobs like printing the same label many times (single page documents)
            printData.append("^PQ" + nbCopies);
        }
        printData.append("^XZ\r\n\r\n");
        Log.i(TAG, "ZPL Data: \n"+ printData);

        bitmap.recycle();
        return printData.toString();
    }
}
