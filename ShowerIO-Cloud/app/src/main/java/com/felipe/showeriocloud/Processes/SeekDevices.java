package com.felipe.showeriocloud.Processes;

import android.os.AsyncTask;
import android.os.Handler;

import com.android.volley.RequestQueue;
import com.felipe.showeriocloud.Model.DeviceDO;
import com.felipe.showeriocloud.Utils.ServerCallback;
import com.felipe.showeriocloud.Utils.ServerCallbackObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public abstract class SeekDevices extends AsyncTask<Void, String, String> {

    public ServerCallbackObject callback;
    public static int RETRY = 0;
    public String esp8266RestUrl = "/check";
    public Gson gson;
    public DeviceDO shower;
    public String subnet;
    public RequestQueue requestQueue;

    public SeekDevices(String subnet, RequestQueue requestQueue, ServerCallbackObject callback){
        super();
        this.subnet = subnet;
        this.requestQueue = requestQueue;
        this.callback = callback;
        GsonBuilder gsonBuilder = new GsonBuilder();
        this.gson = gsonBuilder.create();
    }


    @Override
    protected void onPostExecute(String result) {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (RETRY != 0) {
                    RETRY--;
                    execute();
                } else {
                    if(shower == null){
                        callback.onServerCallbackObject(false,"FAIL",shower);
                    } else {
                        callback.onServerCallbackObject(true,"SUCCESS",shower);
                    }

                }
            }
        }, 6000);

    }

    @Override
    protected abstract String doInBackground(Void... records);


}
