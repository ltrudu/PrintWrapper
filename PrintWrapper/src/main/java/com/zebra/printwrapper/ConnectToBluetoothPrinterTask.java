package com.zebra.printwrapper;

import android.content.Context;
import android.os.AsyncTask;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

public class ConnectToBluetoothPrinterTask extends AsyncTask<Void, Boolean, Boolean> {

    private static final String TAG = "CONNECT_BT_TASK";

    private Context context;
    private SelectedPrinterTaskCallbacks callback;
    private DiscoveredPrinter selectedPrinter = null;

    public ConnectToBluetoothPrinterTask(DiscoveredPrinter selectedPrinter, SelectedPrinterTaskCallbacks aCallback, Context aContext) {
        this.context = aContext;
        this.callback = aCallback;
        this.selectedPrinter = selectedPrinter;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        return PrinterDiscoveryDataMapHelper.populateBluetoothPrinterDiscoveryMap(selectedPrinter, callback, context);
    }

    @Override
    protected void onPostExecute(Boolean populateBluetoothDiscoDataSuccessful) {
        super.onPostExecute(populateBluetoothDiscoDataSuccessful);

        if (populateBluetoothDiscoDataSuccessful) {
            if(callback != null)
                callback.onSuccess(selectedPrinter);
        } else {
            // TODO: return resetConnectingStatus
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.RESET_CONNECTION, context.getString(R.string.reset_connection_status));
        }
    }
}