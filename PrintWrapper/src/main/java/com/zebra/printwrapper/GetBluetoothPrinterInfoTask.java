package com.zebra.printwrapper;

import android.content.Context;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;

import java.util.Map;

public class GetBluetoothPrinterInfoTask extends ExecutorTask<Void, Boolean, Map<String, String>> {

    private static final String TAG = "CONNECT_BT_TASK";

    private Context context;
    private SelectedPrinterTaskCallbacks callback;
    private DiscoveredPrinter selectedPrinter = null;

    public GetBluetoothPrinterInfoTask(DiscoveredPrinter selectedPrinter, SelectedPrinterTaskCallbacks aCallback, Context aContext) {
        this.context = aContext;
        this.callback = aCallback;
        this.selectedPrinter = selectedPrinter;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected Map<String,String> doInBackground(Void... params) {
        return PrinterDiscoveryDataMapHelper.populateBluetoothPrinterDiscoveryMap(selectedPrinter, callback, context);
    }

    @Override
    protected void onPostExecute(Map<String,String> discoveryMap) {
        super.onPostExecute(discoveryMap);

        if (discoveryMap.size()>0) {
            if(callback != null)
                callback.onSuccess(selectedPrinter, discoveryMap);
        } else {
            // TODO: return resetConnectingStatus
            if(callback != null)
                callback.onError(SelectedPrinterTaskError.RESET_CONNECTION, context.getString(R.string.reset_connection_status));
        }
    }
}