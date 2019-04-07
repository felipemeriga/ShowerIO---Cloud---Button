package com.felipe.showeriocloud.Processes;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.felipe.showeriocloud.Model.DeviceDO;
import com.felipe.showeriocloud.Utils.ServerCallbackObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class FullScan extends SeekDevices {

    public FullScan(String subnet, RequestQueue requestQueue, ServerCallbackObject callback) {
        super(subnet, requestQueue, callback);
    }

    @Override
    protected String doInBackground(Void... records) {
        try {
            int timeout = 50;
            for (int i = 2; i < 255; i++) {
                String host = "";
                host = this.subnet + "." + i;
                String fixedUrl = "http://";
                if (InetAddress.getByName(host).isReachable(timeout)) {
                    Log.d("doInBackground()", host + " is reachable");
                    fixedUrl = fixedUrl + host + this.esp8266RestUrl;

                    try {
                        StringRequest stringRequest = new StringRequest(Request.Method.GET, fixedUrl,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        Log.d("doInBackground()", "restHttpRequest(): The HTTP request was done successfully");
                                        Log.d("doInBackground()", "Found a responding device!");
                                        DeviceDO foundShower;
                                        foundShower = gson.fromJson(response.toString(), DeviceDO.class);
                                        shower = foundShower;
                                        Boolean hasAlreadyFound = false;
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("doInBackground()", "restHttpRequest(): There was an error in the HTTP request");
                                Log.d("doInBackground()", "restHttpRequest(): Error: " + error.getMessage());


                            }
                        });

                        this.requestQueue.add(stringRequest);

                    } catch (Exception e) {
                        Log.d("WaferRestService Class", "restHttpRequest(): Error: " + e.getMessage());
                        throw e;
                    }
                }
            }
        } catch (UnknownHostException e) {
            Log.d("doInBackground()", " UnknownHostException e : " + e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.d("doInBackground()", "checkHosts() :: IOException e : " + e);
            e.printStackTrace();
        } finally {
            Log.d("checkHosts()", "All the ip Address where scanned!");
        }
        return "finished";
    }

}
