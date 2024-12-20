package com.zebra.printwrapper;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

public class BitmapCPCLConverter {
    private static final String TAG = "BitmapCPCLConverter";

    private static final char[] HEXMAP = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    public static String createBitmapCPC(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null");
            return null;
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int monoSize = (width * height) / 8;

        // Convert to grayscale
        int[] greyImage = new int[width * height];
        convertToGreyscale(bitmap, greyImage);

        // Apply dithering
        applyDithering(greyImage, width, height);

        // Convert to mono
        byte[] monoImage = convertToMono(greyImage, width, height);

        // Convert to hex string
        return convertToHexString(monoImage);
    }

    private static void convertToGreyscale(Bitmap bitmap, int[] greyImage) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int a = Color.alpha(pixel);
                int r = Color.red(pixel);
                int g = Color.green(pixel);
                int b = Color.blue(pixel);

                // Convert to grayscale using the same formula as the C code
                int m = (int)(0.299 * r + 0.587 * g + 0.114 * b);
                m = ((a * m) + ((255 - a) * 255)) / 255;
                m = Math.min(255, Math.max(0, m));

                greyImage[y * width + x] = m;
            }
        }
    }

    private static void applyDithering(int[] greyImage, int width, int height) {
        for (int y = 0; y < height; y++) {
            boolean hasBottom = y < height - 1;

            for (int x = 0; x < width; x++) {
                boolean hasLeft = x > 0;
                boolean hasRight = x < width - 1;

                int pos = y * width + x;
                int oldPixel = greyImage[pos];
                int newPixel = oldPixel < 128 ? 0 : 255;
                greyImage[pos] = newPixel;

                int error = oldPixel - newPixel;

                if (hasRight)
                    greyImage[pos + 1] += (error * 7) / 16;
                if (hasLeft && hasBottom)
                    greyImage[pos + width - 1] += (error * 3) / 16;
                if (hasBottom)
                    greyImage[pos + width] += (error * 5) / 16;
                if (hasRight && hasBottom)
                    greyImage[pos + width + 1] += (error * 1) / 16;
            }
        }
    }

    private static byte[] convertToMono(int[] greyImage, int width, int height) {
        byte[] monoImage = new byte[(width * height) / 8];
        int pos = 0;
        byte value = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                value <<= 1;
                if (greyImage[y * width + x] < 128) value |= 1;
                if (pos % 8 == 7) monoImage[pos >> 3] = value;
                pos++;
            }
        }

        return monoImage;
    }

    private static String convertToHexString(byte[] monoImage) {
        StringBuilder sb = new StringBuilder(monoImage.length * 2);

        for (byte b : monoImage) {
            sb.append(HEXMAP[(b & 0xF0) >> 4]);
            sb.append(HEXMAP[b & 0x0F]);
        }

        return sb.toString();
    }
}