package com.zebra.printwrapper;

import android.util.Log;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.printer.SGD;

public class SGDHelper {

    private static final String TAG = SGDHelper.class.getName();

    // Checks the selected printer to see if it has the pdf virtual device installed.
    public static boolean isPDFEnabled(Connection connection) throws ConnectionException {
            String printerInfo = getAplMode(connection);
            if (printerInfo.equals("pdf")) {
                return true;
            }
        return false;
    }

    public static String getAplMode(Connection connection) throws ConnectionException {
        int retryCount = 0;
        String aplEnableResponse = "";
        while (true) {
            if (aplEnableResponse.length() != 0) {
                break;
            }
            int retryCount2 = retryCount + 1;
            if (retryCount >= 10) {
                break;
            }
            if(retryCount > 0)
            {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            aplEnableResponse = SGD.GET("apl.enable", connection);
            Log.d(TAG, "getAplMode response: " + aplEnableResponse + ", retryCount: " + retryCount2);
            retryCount = retryCount2;

        }
        return aplEnableResponse;
    }
}
