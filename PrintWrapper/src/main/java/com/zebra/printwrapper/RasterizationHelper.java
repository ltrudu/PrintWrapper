package com.zebra.printwrapper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.IOException;



@SuppressWarnings("JavaJniMissingFunction")
public class RasterizationHelper {
    static {
        System.loadLibrary("ZebraUtils");
    }

    private static String TAG = RasterizationHelper.class.getName();

    public static native String getUtilsVersion();
    public static native String createBitmapZPL(Bitmap bitmap);
    public static native String createBitmapCPC(Bitmap bitmap);

    public static Bitmap loadFromFile(String filename) {
        try {//from   ww  w.j a  va  2  s.c  om
            File f = new File(filename);
            if (!f.exists()) {
                return null;
            }
            Bitmap tmp = BitmapFactory.decodeFile(filename);
            return tmp;
        } catch (Exception e) {
            return null;
        }
    }
    public static String getCPCFromPDFFile(String filePath, int pageNumber, int dpi, int copies) {
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
        /*if (variableLengthEnabled == true) {
            iWidth = page.getWidth();
            iHeight = page.getHeight();
            iSize = iWidth * iHeight;
            bitmap = Bitmap.createBitmap(iWidth, iHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
            iWidth = (iWidth >> 3);
        } else */
        {
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

        String CPCString = getCPCFromBitmap(bitmap, dpi, iWidth, iHeight);
        bitmap.recycle();
        return CPCString;
    }

    public static String getCPCFromBitmap(Bitmap bitmap, int dpi, int iWidth, int iHeight)
    {
        StringBuilder printData = new StringBuilder();
        Log.i(TAG, "Creating CPC");
        printData.append("! 0 " + dpi + " " + dpi + " " + iWidth + " 1\r\n");
        printData.append("EG " + iWidth + " " + iHeight + " 0 0 ");
        printData.append(RasterizationHelper.createBitmapCPC(bitmap));
        printData.append("\r\n");
        printData.append("PRINT\r\n");
        Log.i(TAG, "CPCL Data: \n"+ printData);
        return printData.toString();
    }


    public static String getZPLFromPDFFile(String filePath, int pageNumber, int dpi, int nbCopies) {
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
        /*if (variableLengthEnabled == true) {
            iWidth = page.getWidth();
            iHeight = page.getHeight();
            iSize = iWidth * iHeight;
            bitmap = Bitmap.createBitmap(iWidth, iHeight, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
            iWidth = (iWidth >> 3);
        } else */{
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

        String zplString = getZPLFromBitmap(bitmap, iWidth, iHeight, nbCopies);
        bitmap.recycle();
        return zplString;
    }

    public static String getZPLFromBitmap(Bitmap bitmap)
    {
        return getZPLFromBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), 1);
    }
    public static String getZPLVariableLengthFromBitmap(Bitmap bitmap, int vlTopMargin)
    {
        return getZPLFromBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), 1);
    }

    public static String getZPLFromBitmap(Bitmap bitmap, int nbCopies)
    {
        return getZPLFromBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), nbCopies);
    }
    public static String getZPLFromBitmap(Bitmap bitmap, int iWidth, int iHeight)
    {
        return getZPLFromBitmap(bitmap,  iWidth, iHeight, 1);
    }

    public static String getZPLFromBitmap(Bitmap bitmap, int iWidth, int iHeight, int nbCopies)
    {
        StringBuilder printData = new StringBuilder();
        // By default create ZPL Data
        //Create ZPL
        String ZPLBitmap = RasterizationHelper.createBitmapZPL(bitmap);
        Log.i(TAG, "Creating ZPL");
        printData.append("^XA");
        printData.append("^PW"+(iWidth*8));
        printData.append("^FO,0,0^GFA," + iWidth*iHeight + "," + iWidth*iHeight + "," + iWidth + ",");
        printData.append(ZPLBitmap);
        if(nbCopies > 1) {
            // Add ^PQ to jobs like printing the same label many times (single page documents)
            printData.append("^PQ" + nbCopies);
        }
        printData.append("^XZ\r\n\r\n");
        Log.i(TAG, "ZPL Data: \n"+ printData);

        return printData.toString();
    }

    public static String getZPLFromBitmapSoftware(Bitmap bitmap, int iWidth, int iHeight, int nbCopies)
    {
        StringBuilder printData = new StringBuilder();
        // By default create ZPL Data
        //Create ZPL
        String ZPLBitmap = BitmapZPLConverter.createBitmapZPL(bitmap);
        Log.i(TAG, "Creating ZPL");
        printData.append("^XA");

        printData.append("^PW"+(iWidth*8));
        printData.append("^FO,0,0^GFA," + iWidth*iHeight + "," + iWidth*iHeight + "," + iWidth + ",");
        printData.append(ZPLBitmap);
        if(nbCopies > 1) {
            // Add ^PQ to jobs like printing the same label many times (single page documents)
            printData.append("^PQ" + nbCopies);
        }
        printData.append("^XZ\r\n\r\n");
        Log.i(TAG, "ZPL Data: \n"+ printData);

        return printData.toString();
    }
}
