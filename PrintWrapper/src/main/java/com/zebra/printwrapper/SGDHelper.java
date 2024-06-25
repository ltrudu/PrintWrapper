package com.zebra.printwrapper;

import android.print.PrinterInfo;
import android.util.Base64;
import android.util.Log;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.MultichannelTcpConnection;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterBluetooth;
import com.zebra.sdk.printer.discovery.DiscoveredPrinterNetwork;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SGDHelper {

    private static final String TAG = SGDHelper.class.getName();
    private static Connection mConnection = null;
    public static boolean mUseBase64Encoding = false;

    // Checks the selected printer to see if it has the pdf virtual device installed.
    public static boolean isPDFEnabled(Connection connection) throws ConnectionException {
        String printerInfo = getAplMode(connection);
        if (printerInfo.equals("pdf")) {
            return true;
        }
        return false;
    }

    public interface SGDHelperCallback {
        void onMessage(String message);
    }

    public static Connection connectToPrinter(DiscoveredPrinter printer, SGDHelperCallback callback) throws PrinterWrapperException {

        if (mConnection != null) {
            if (callback != null) {
                callback.onMessage("The connection object already exists.");
                callback.onMessage("Verifying if we are connected to the printer.");
            }
            if (mConnection.isConnected() == true) {
                if (callback != null) {
                    callback.onMessage("Printer is already connected.");
                }
                return mConnection;
            } else {
                try {
                    mConnection.close();
                } catch (ConnectionException e) {
                    e.printStackTrace();
                }
                try {
                    if (callback != null) {
                        callback.onMessage("Opening connection.");
                    }
                    mConnection.open();
                    if (mConnection.isConnected()) {
                        if (callback != null) {
                            callback.onMessage("Connection opened with success.");
                        }
                        return mConnection;
                    } else {
                        mConnection.close();
                        mConnection = null;
                    }
                } catch (ConnectionException e) {
                    e.printStackTrace();
                    mConnection = null;
                }
            }
        }

        if (printer instanceof DiscoveredPrinterBluetooth) {
            if (callback != null) {
                callback.onMessage("Connecting to bluetooth Printer: " + printer.address);
            }
            mConnection = new BluetoothConnection(printer.address);
            try {
                if (callback != null) {
                    callback.onMessage("Opening connection.");
                }
                mConnection.open();
                if (mConnection.isConnected() == true) {
                    if (callback != null) {
                        callback.onMessage("Connection opened with success.");
                    }
                    return mConnection;
                } else {
                    if (callback != null) {
                        callback.onMessage("Error. Could not connect to printer.");
                    }
                    try {
                        mConnection.close();
                        mConnection = null;
                    } catch (ConnectionException ex) {
                        ex.printStackTrace();
                        mConnection = null;
                        throw new PrinterWrapperException(new Exception("SGDHelper.connectToPrinter: Could not establish connection with BluetoothPrinter."));
                    }
                    return null;
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
                try {
                    mConnection.close();
                    mConnection = null;
                } catch (ConnectionException ex) {
                    ex.printStackTrace();
                    mConnection = null;
                }
                throw new PrinterWrapperException(new Exception("SGDHelper.connectToPrinter: Could not establish connection with BluetoothPrinter."));
            }
        } else if (printer instanceof DiscoveredPrinterNetwork) {
            if (callback != null) {
                callback.onMessage("Connecting to Network Printer: " + printer.address);
            }
            mConnection = new MultichannelTcpConnection(printer);
            try {
                if (callback != null) {
                    callback.onMessage("Opening connection.");
                }
                mConnection.open();
                if (mConnection.isConnected() == true) {
                    if (callback != null) {
                        callback.onMessage("Connection opened with success.");
                    }
                    return mConnection;
                } else {
                    if (callback != null) {
                        callback.onMessage("Error:Could not connect to printer.");
                    }
                    try {
                        mConnection.close();
                        mConnection = null;
                    } catch (ConnectionException ex) {
                        ex.printStackTrace();
                        mConnection = null;
                    }
                    throw new PrinterWrapperException(new Exception("SGDHelper.connectToPrinter: Could not establish connection with Network Printer."));
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
                try {
                    mConnection.close();
                    mConnection = null;
                } catch (ConnectionException ex) {
                    ex.printStackTrace();
                    mConnection = null;
                }
            }
            throw new PrinterWrapperException(new Exception("SGDHelper.connectToPrinter: Could not establish connection with Network Printer."));
        }
        throw new PrinterWrapperException(new Exception("SGDHelper.connectToPrinter: Printer connexion type not supported. USB ?"));
    }

    public static String GET(String propertyName, DiscoveredPrinter discoveredPrinter, SGDHelperCallback callback) throws PrinterWrapperException {
        if (callback != null) {
            callback.onMessage("Initiating connection with printer.");
        }
        Connection connection = connectToPrinter(discoveredPrinter, callback);
        if (connection == null) {
            throw new PrinterWrapperException(new Exception("Connexion error: connection object is null"), null);
        }
        if (callback != null) {
            callback.onMessage("Connection with printer succeeded.");
        }
        int retryCount = 0;
        String getPropertyResponse = "";
        while (true) {
            if (getPropertyResponse.length() != 0) {
                if (callback != null) {
                    callback.onMessage("GET property: " + propertyName + " succeeded.");
                }
                break;
            }
            int retryCount2 = retryCount + 1;
            if (retryCount >= 10) {
                if (callback != null) {
                    callback.onMessage("Error, could not GET property: " + propertyName + " in 10 tries.");
                }
                break;
            }

            if (callback != null) {
                callback.onMessage("GET on property: " + propertyName + " tryCount: " + retryCount);
            }

            if (retryCount > 0) {
                if (callback != null) {
                    callback.onMessage("Sleeping 300ms between method call.");
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (callback != null) {
                    callback.onMessage("GET property: " + propertyName);
                }
                getPropertyResponse = SGD.GET(propertyName, connection);
                if (callback != null && getPropertyResponse.length() > 0) {
                    callback.onMessage("Response: " + getPropertyResponse);
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
                connection = connectToPrinter(discoveredPrinter, callback);
                if (retryCount2 >= 10) {
                    throw new PrinterWrapperException(new Exception("SGDHelper.GET::Retry count:" + retryCount2 + "\nConnection Exception :", e.getCause()));
                }
            }
            Log.d(TAG, "Get property: " + propertyName + " response: " + getPropertyResponse + ", retryCount: " + retryCount2);
            retryCount = retryCount2;

        }
        if (connection != null && connection.isConnected()) {
            try {
                connection.close();
                connection = null;
            } catch (ConnectionException e) {
                throw new PrinterWrapperException(e);
            }
        }
        return getPropertyResponse;
    }

    public static Map<String, String> MULTI_GET(String propertyNamesCommaSeparated, DiscoveredPrinter discoveredPrinter, SGDHelperCallback callback) throws PrinterWrapperException {
        Log.d(TAG, "propertyNamesCommaSeparated:" + propertyNamesCommaSeparated);
        List<String> propertiesList = getListFromCommaSeparatedValues(propertyNamesCommaSeparated, mUseBase64Encoding);
        Log.d(TAG, "List size:" + propertiesList.size());
        return MULTI_GET(propertiesList, discoveredPrinter, callback);
    }

    public static Map<String, String> MULTI_GET(List<String> propertyNames, DiscoveredPrinter discoveredPrinter, SGDHelperCallback callback) throws PrinterWrapperException {
        if (callback != null) {
            callback.onMessage("Initiating connection with printer.");
        }

        Log.d(TAG, "Multiget properties: " + propertyNames.toString());

        Connection connection = connectToPrinter(discoveredPrinter, callback);
        if (connection == null) {
            throw new PrinterWrapperException(new Exception("Connexion error: connection object is null"), null);
        }
        if (callback != null) {
            callback.onMessage("Connection with printer succeeded.");
        }
        int retryCount = 0;
        Map<String, String> getPropertyResponses = new HashMap<>();
        for (String propertyName : propertyNames) {
            if (callback != null) {
                callback.onMessage("Seting up things for property: " + propertyName);
            }
            String getPropertyResponse = "";
            while (true) {
                if (getPropertyResponse.length() != 0) {
                    if (callback != null) {
                        callback.onMessage("Get of property: " + propertyName + " succeeded.");
                    }
                    break;
                }
                int retryCount2 = retryCount + 1;
                if (retryCount >= 10) {
                    if (callback != null) {
                        callback.onMessage("Error, could not get property: " + propertyName + " after 10 tries.");
                    }
                    break;
                }
                if (callback != null) {
                    callback.onMessage("GET on property: " + propertyName + " tryCount: " + retryCount);
                }
                if (retryCount > 0) {
                    if (callback != null) {
                        callback.onMessage("Sleeping 300ms between method call.");
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    if (callback != null) {
                        callback.onMessage("Trying to GET the property.");
                    }
                    getPropertyResponse = SGD.GET(propertyName, connection);
                } catch (ConnectionException e) {
                    e.printStackTrace();
                    connection = connectToPrinter(discoveredPrinter, callback);
                    if (retryCount2 >= 10) {
                        throw new PrinterWrapperException(new Exception("SGDHelper.GET::Retry count:" + retryCount2 + "\nConnection Exception :", e.getCause()));
                    }
                }
                Log.d(TAG, "Get property: " + propertyName + " response: " + getPropertyResponse + ", retryCount: " + retryCount2);
                retryCount = retryCount2;
            }
            if (getPropertyResponse.length() > 0) {
                getPropertyResponses.put(propertyName, getPropertyResponse);
            }
        }
        if (connection != null && connection.isConnected()) {
            try {
                connection.close();
                connection = null;
            } catch (ConnectionException e) {
                throw new PrinterWrapperException(e);
            }
        }
        Log.d(TAG, "getPropertyResponses: " + getPropertyResponses.toString());
        return getPropertyResponses;
    }

    public static Map<String, Boolean> MULTI_SET(String propertyNamesCommaSeparated, String propertyValuesCommaSeparated, DiscoveredPrinter discoveredPrinter, SGDHelperCallback callback) throws PrinterWrapperException {
        Map<String, String> propertiesNamesAndValues = getHashMapFromCommaSeparatedValues(propertyNamesCommaSeparated, propertyValuesCommaSeparated, mUseBase64Encoding);
        return MULTI_SET(propertiesNamesAndValues, discoveredPrinter, callback);
    }

    public static Map<String, Boolean> MULTI_SET(Map<String, String> propertyNamesAndValues, DiscoveredPrinter discoveredPrinter, SGDHelperCallback callback) throws PrinterWrapperException {
        if (callback != null) {
            callback.onMessage("Initiating connection with printer.");
        }
        Connection connection = connectToPrinter(discoveredPrinter, callback);
        if (connection == null) {
            throw new PrinterWrapperException(new Exception("Connexion error: connection object is null"), null);
        }
        if (callback != null) {
            callback.onMessage("connection with printer succeeded.");
        }
        int retryCount = 0;
        Map<String, Boolean> returnValues = new HashMap<>();
        Set<String> keySet = propertyNamesAndValues.keySet();
        String readValue = "";
        String writeValue = "";
        for (String propertyName : keySet) {
            if (callback != null) {
                callback.onMessage("Setting up things to SET the property: " + propertyName);
            }
            while (true) {
                int retryCount2 = retryCount + 1;
                if (retryCount >= 10) {
                    if (callback != null) {
                        callback.onMessage("Could not SET the property: " + propertyName + " after 10 tries.");
                    }
                    returnValues.put(propertyName, false);
                    break;
                }
                if (callback != null) {
                    callback.onMessage("SET on property: " + propertyName + " tryCount: " + retryCount);
                }

                if (retryCount > 0) {
                    if (callback != null) {
                        callback.onMessage("Sleeping 300ms between method calls.");
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    writeValue = propertyNamesAndValues.get(propertyName);
                    if (callback != null) {
                        callback.onMessage("Trying to write value: " + writeValue);
                    }
                    SGD.SET(propertyName, writeValue, connection);
                    readValue = SGD.GET(propertyName, connection);
                    if (readValue.equalsIgnoreCase(writeValue)) {
                        if (callback != null) {
                            callback.onMessage("SET on property: " + propertyName + " succeeded.");
                        }
                        returnValues.put(propertyName, true);
                        break;
                    } else {
                        if (callback != null) {
                            callback.onMessage("Error on SET on property: " + propertyName + ": values are not equal.nValueToWrite: " + writeValue + "\nValueRead: " + readValue);
                        }
                    }
                } catch (ConnectionException e) {
                    e.printStackTrace();
                    connection = connectToPrinter(discoveredPrinter, callback);
                    if (retryCount >= 10) {
                        throw new PrinterWrapperException(new Exception("SGDHelper.GET::Retry count:" + retryCount2 + "\nConnection Exception :", e.getCause()));
                    }
                }
                Log.d(TAG, "Set property: " + propertyName + "\nwrite: " + writeValue + "\nread:" + readValue + "\nretryCount: " + retryCount2);
                retryCount = retryCount2;
            }
        }
        if (connection != null && connection.isConnected()) {
            try {
                if (callback != null) {
                    callback.onMessage("SET on property: closing connection.");
                }
                connection.close();
                connection = null;
            } catch (ConnectionException e) {
                throw new PrinterWrapperException(e);
            }
        }
        return returnValues;
    }


    public static boolean SET(final String propertyName, final String propertyValue, DiscoveredPrinter printer, SGDHelperCallback callback) throws PrinterWrapperException {
        if (callback != null) {
            callback.onMessage("Initiating connection with printer.");
        }
        Connection connection = connectToPrinter(printer, callback);
        if (connection == null) {
            throw new PrinterWrapperException(new Exception("Could not connect to printer."));
        }
        if (callback != null) {
            callback.onMessage("Connection with printer succeeded.");
        }
        int retryCount = 0;
        boolean result = false;
        while (true) {
            int retryCount2 = retryCount + 1;
            if (retryCount >= 10) {
                break;
            }
            if (callback != null) {
                callback.onMessage("SET on property: " + propertyName + " tryCount: " + retryCount);
            }
            if (retryCount > 0) {
                if (callback != null) {
                    callback.onMessage("Waiting 300ms between method calls.");
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (callback != null) {
                    callback.onMessage("Trying to SET property: " + propertyName + " with value: " + propertyValue);
                }
                SGD.SET(propertyName, propertyValue, connection);
                String propertyValueRead = SGD.GET(propertyName, connection);
                if (propertyValueRead.equalsIgnoreCase(propertyValue)) {
                    if (callback != null) {
                        callback.onMessage("SET property: " + propertyName + " succeeded.");
                    }
                    result = true;
                    break;
                } else {
                    if (callback != null) {
                        callback.onMessage("SET Error, value are not equals on property: " + propertyName + " with valueToWrite: " + propertyValue + " and valueRead: " + propertyValueRead);
                    }
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
                connection = connectToPrinter(printer, callback);
                if (retryCount >= 10) {
                    throw new PrinterWrapperException(new Exception("SGDHelper.SET::Retry count:" + retryCount2 + "\nConnection Exception :", e.getCause()));
                }

            }
            retryCount = retryCount2;
        }
        if (connection != null && connection.isConnected()) {
            try {
                connection.close();
            } catch (ConnectionException e) {
                throw new PrinterWrapperException(e);
            }
        }
        return result;
    }

    public static String DO(final String setting, final String value, DiscoveredPrinter printer, SGDHelperCallback callback) throws PrinterWrapperException {
        if (callback != null) {
            callback.onMessage("Initialising printer connection.");
        }
        Connection connection = connectToPrinter(printer, callback);
        if (connection == null) {
            throw new PrinterWrapperException(new Exception("Could not connect to printer."));
        }
        if (callback != null) {
            callback.onMessage("Printer connection succeeded.");
        }
        int retryCount = 0;
        String doSettingResponse = "";
        while (true) {
            if (doSettingResponse.length() != 0) {
                if (callback != null) {
                    callback.onMessage("DO command succeeded.");
                }
                break;
            }
            int retryCount2 = retryCount + 1;
            if (retryCount >= 10) {
                if (callback != null) {
                    callback.onMessage("Error, could not execute DO command: " + setting + " with value " + value);
                }
                break;
            }
            if (callback != null) {
                callback.onMessage("Executing DO command: " + setting + " with value " + value + " tryCount:" + retryCount);
            }
            if (retryCount > 0) {
                if (callback != null) {
                    callback.onMessage("Sleeping 300ms between method calls.");
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                doSettingResponse = SGD.DO(setting, value, connection);
                if(setting.equalsIgnoreCase("device.reset") && doSettingResponse.length() == 0)
                {
                    if (callback != null) {
                        callback.onMessage("DO command succeeded.");
                    }
                    break;
                }
                if (doSettingResponse.length() > 0) {
                    if (callback != null) {
                        callback.onMessage("DO command returned: " + doSettingResponse);
                    }
                } else {
                    if (callback != null) {
                        callback.onMessage("DO command returned an empty string, trying again.");
                    }
                }
            } catch (ConnectionException e) {
                e.printStackTrace();
                connection = connectToPrinter(printer, callback);
                if (retryCount2 >= 10) {
                    throw new PrinterWrapperException(new Exception("SGDHelper.DO::Retry count:" + retryCount2 + "\nConnection Exception :", e.getCause()));
                }
            }
            Log.d(TAG, "DO setting: " + setting + " with value:" + value + " response: " + doSettingResponse + ", retryCount: " + retryCount2);
            retryCount = retryCount2;
        }
        if (connection != null && connection.isConnected()) {
            try {
                connection.close();
                connection = null;
            } catch (ConnectionException e) {
                throw new PrinterWrapperException(e);
            }
        }
        return doSettingResponse;
    }

    public static Map<String, String> MULTI_DO(String propertyNamesCommaSeparated, String propertyValuesCommaSeparated, DiscoveredPrinter discoveredPrinter, SGDHelperCallback callback) throws PrinterWrapperException {
        Map<String, String> propertiesNamesAndValues = getHashMapFromCommaSeparatedValues(propertyNamesCommaSeparated, propertyValuesCommaSeparated, mUseBase64Encoding);
        return MULTI_DO(propertiesNamesAndValues, discoveredPrinter, callback);
    }

    public static Map<String, String> MULTI_DO(Map<String, String> propertyNamesAndValues, DiscoveredPrinter discoveredPrinter, SGDHelperCallback callback) throws PrinterWrapperException {
        if (callback != null) {
            callback.onMessage("Initialising printer connection.");
        }
        Connection connection = connectToPrinter(discoveredPrinter, callback);
        if (connection == null) {
            throw new PrinterWrapperException(new Exception("Could not connect to printer."));
        }
        if (callback != null) {
            callback.onMessage("Printer connection succeeded.");
        }
        int retryCount = 0;
        Set<String> keySet = propertyNamesAndValues.keySet();
        Map<String, String> returnValues = new HashMap<>();
        String writeValue = "";
        for (String propertyName : keySet) {
            if (callback != null) {
                callback.onMessage("Setting up things for DO command: " + propertyName);
            }
            String returnValue = "";
            while (true) {
                if (returnValue.length() != 0) {
                    if (callback != null) {
                        callback.onMessage("DO command: " + propertyName + " succeeded.");
                    }
                    break;
                }
                int retryCount2 = retryCount + 1;
                if (retryCount >= 10) {
                    if (callback != null) {
                        callback.onMessage("Error, could not execute the DO command: " + propertyName);
                    }
                    break;
                }
                if (callback != null) {
                    callback.onMessage("Executing DO command: " + propertyName + " tryCount:" + retryCount);
                }
                if (retryCount > 0) {
                    if (callback != null) {
                        callback.onMessage("Waiting 300ms between method calls.");
                    }
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    writeValue = propertyNamesAndValues.get(propertyName);
                    if (callback != null) {
                        callback.onMessage("Executing DO command: " + propertyName + " with value:" + writeValue);
                    }
                    returnValue = SGD.DO(propertyName, writeValue, connection);
                    if (returnValue.length() > 0) {
                        if (callback != null) {
                            callback.onMessage("DO command return value: " + returnValue);
                        }
                        returnValues.put(propertyName, returnValue);
                    } else {
                        if (callback != null) {
                            callback.onMessage("DO command returned an empty string, trying again.");
                        }
                    }
                } catch (ConnectionException e) {
                    e.printStackTrace();
                    connection = connectToPrinter(discoveredPrinter, callback);
                    if (retryCount >= 10) {
                        throw new PrinterWrapperException(new Exception("SGDHelper.GET::Retry count:" + retryCount2 + "\nConnection Exception :", e.getCause()));
                    }
                }
                Log.d(TAG, "Set property: " + propertyName + "\nwrite: " + writeValue + "\nreturned:" + returnValue + "\nretryCount: " + retryCount2);
                retryCount = retryCount2;
            }
        }
        if (connection != null && connection.isConnected()) {
            try {
                connection.close();
                connection = null;
            } catch (ConnectionException e) {
                throw new PrinterWrapperException(e);
            }
        }
        return returnValues;
    }

    public static int getMdpi(Connection connection)
    {
        String propertyName = "head.resolution.in_dpi";
        int retryCount = 0;
        String getPropertyResponse = "";
        while (true) {
            if (getPropertyResponse.length() != 0) {
                break;
            }
            int retryCount2 = retryCount + 1;
            if (retryCount >= 10) {
                break;
            }
            if (retryCount > 0) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                getPropertyResponse = SGD.GET(propertyName, connection);
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Get property: " + propertyName + " response: " + getPropertyResponse + ", retryCount: " + retryCount2);
            retryCount = retryCount2;

        }
        if(getPropertyResponse.isEmpty() == false) {
            int dpi = Integer.valueOf(getPropertyResponse);
            return dpi;
        }
        else {
            Log.d(TAG, "[Error] Get property: " + propertyName + " is empty");
            return 0;
        }
    }

    public static String getAplMode(Connection connection) throws ConnectionException {
        String propertyName = "apl.enable";
        int retryCount = 0;
        String getPropertyResponse = "";
        while (true) {
            if (getPropertyResponse.length() != 0) {
                break;
            }
            int retryCount2 = retryCount + 1;
            if (retryCount >= 10) {
                break;
            }
            if (retryCount > 0) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                getPropertyResponse = SGD.GET(propertyName, connection);
            } catch (ConnectionException e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Get property: " + propertyName + " response: " + getPropertyResponse + ", retryCount: " + retryCount2);
            retryCount = retryCount2;

        }
        return getPropertyResponse;
    }

    protected static Map<String, String> getHashMapFromCommaSeparatedValues(String commaSeparatedKeys, String commaSeparatedValues, boolean base64Encoded) throws PrinterWrapperException {
        return getHashMapFromSeparatedValues(commaSeparatedKeys, commaSeparatedValues, ";", base64Encoded);
    }

    protected static List<String> getListFromCommaSeparatedValues(String commaSeparatedValues, boolean base64Encoded) throws PrinterWrapperException {
        return getListFromSeparatedValues(commaSeparatedValues, ";", base64Encoded);
    }

    public static Map<String, String> getHashMapFromSeparatedValues(String separatedKeys, String separatedValues, String separator, boolean base64Encoded) throws PrinterWrapperException {
        HashMap<String, String> variableData = null;
        if (separatedKeys != null && separatedKeys.isEmpty() == false) {
            String[] variableDataKeys = separatedKeys.split(separator);
            String[] variableDataValues = separatedValues.split(separator);
            variableData = new HashMap<String, String>();
            if (variableDataKeys.length != variableDataValues.length) {
                String sError = "Error, variableDataKeys.length != variableDataValues.length, both lists should contain the same number of elements.";
                throw new PrinterWrapperException(new Exception(sError), null);
            }
            for (int i = 0; i < variableDataKeys.length; i++) {
                String keyToAdd = variableDataKeys[i];
                String valueToAdd = variableDataValues[i];
                if (base64Encoded) {
                    byte[] data = Base64.decode(valueToAdd, Base64.DEFAULT);
                    try {
                        valueToAdd = new String(data, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new PrinterWrapperException(e);
                    }
                }
                variableData.put(keyToAdd, valueToAdd);
            }
        }
        return variableData;
    }

    public static List<String> getListFromSeparatedValues(String separatedValues, String separator, boolean base64Encoded) throws PrinterWrapperException {
        List<String> variableData = null;
        if (separatedValues != null && separatedValues.isEmpty() == false) {
            String[] variableDataValues = separatedValues.split(separator);
            variableData = new ArrayList<>();
            if (variableDataValues.length != 0) {
                String sError = "Error, variableDataValues.length = 0, the list should at least contains one element";
                throw new PrinterWrapperException(new Exception(sError), null);
            }
            for (int i = 0; i < variableDataValues.length; i++) {
                String valuetoAdd = variableDataValues[i];
                if (base64Encoded) {
                    byte[] data = Base64.decode(valuetoAdd, Base64.DEFAULT);
                    valuetoAdd = new String(data, StandardCharsets.UTF_8);
                }
                variableData.add(valuetoAdd);
            }
        }
        return variableData;
    }

    public static String separatedValuesListToString(List<String> listOfStrings, String separator, boolean base64Encoded) {
        String returnValues = "";
        for (String aString : listOfStrings) {
            String stringToAdd = aString;
            if (base64Encoded) {
                byte[] data = stringToAdd.getBytes(StandardCharsets.UTF_8);
                stringToAdd = Base64.encodeToString(data, Base64.DEFAULT);
            }
            if (returnValues.length() > 0) {
                returnValues += separator;
            }
            returnValues += stringToAdd;
        }
        return returnValues;
    }

    public static String separatedValuesMapSBToStringWithSeparator(Map<String, Boolean> mapOfStringsBooleans, String separator, boolean base64Encoded) {
        String returnValues = "";
        for (Map.Entry<String, Boolean> aEntry : mapOfStringsBooleans.entrySet()) {
            String keyStringToAdd = aEntry.getKey();
            String valueStringToAdd = aEntry.getValue() == true ? "true" : "false";
            if (base64Encoded) {
                byte[] data = keyStringToAdd.getBytes(StandardCharsets.UTF_8);
                keyStringToAdd = Base64.encodeToString(data, Base64.DEFAULT);
                data = valueStringToAdd.getBytes(StandardCharsets.UTF_8);
                valueStringToAdd = Base64.encodeToString(data, Base64.DEFAULT);

            }
            if (returnValues.length() > 0) {
                returnValues += separator;
            }
            returnValues += keyStringToAdd + separator + valueStringToAdd;
        }
        return returnValues;
    }

    public static String separatedValuesMapSSToStringWithSeparator(Map<String, String> mapOfStringsBooleans, String separator, boolean base64Encoded) {
        String returnValues = "";
        for (Map.Entry<String, String> aEntry : mapOfStringsBooleans.entrySet()) {
            String keyStringToAdd = aEntry.getKey();
            String valueStringToAdd = aEntry.getValue();
            if (base64Encoded) {
                byte[] data = keyStringToAdd.getBytes(StandardCharsets.UTF_8);
                keyStringToAdd = Base64.encodeToString(data, Base64.DEFAULT);
                data = valueStringToAdd.getBytes(StandardCharsets.UTF_8);
                valueStringToAdd = Base64.encodeToString(data, Base64.DEFAULT);

            }
            if (returnValues.length() > 0) {
                returnValues += separator;
            }
            returnValues += keyStringToAdd + separator + valueStringToAdd;
        }
        return returnValues;
    }

}
