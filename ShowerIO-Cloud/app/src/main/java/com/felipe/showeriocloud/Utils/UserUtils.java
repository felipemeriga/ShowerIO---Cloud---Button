package com.felipe.showeriocloud.Utils;

import android.util.Log;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.felipe.showeriocloud.Aws.AuthorizationHandle;
import com.felipe.showeriocloud.Model.UserAnalyticsDO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

public class UserUtils {


    private static final String TAG = "UserUtils";
    public ServerCallbackObject callbackObject;
    public ServerCallbackObjects callbackObjects;
    private Gson gson;


    public UserUtils() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        this.gson = gsonBuilder.create();
    }


    public void getUserBathAnalytics(RequestQueue requestQueue, final ServerCallbackObject serverCallbackObject) {
        String ENDPOINT = "https://bzmzaknyp8.execute-api.us-east-1.amazonaws.com/tst?";
        ENDPOINT = ENDPOINT + "userId=" + AuthorizationHandle.getCurrentUserId();
        Log.i(TAG, "getUserBathAnalytics() Doing the HTTP GET request on ENDPOINT: " + ENDPOINT);
        requestQueue.add(new StringRequest(Request.Method.GET, ENDPOINT, onUserAnalyticsSuccess, onUserAnalyticsError));
        this.callbackObject = serverCallbackObject;
    }


    public Response.Listener<String> onUserAnalyticsSuccess = new Response.Listener<String>() {

        @Override
        public void onResponse(String response) {
            Log.i(TAG, "onUserAnalyticsSuccess() The HTTP request was done successfully, getting the parameters from the response");
            JsonParser jsonParser = new JsonParser();
            JsonObject json = (JsonObject) jsonParser.parse(response.toString());
            UserAnalyticsDO userAnalyticsDO = new UserAnalyticsDO();
            userAnalyticsDO.set_totalLiters(json.get("body").getAsJsonObject().get("totalLiters").getAsBigDecimal());
            userAnalyticsDO.set_totalTime(json.get("body").getAsJsonObject().get("totalTime").getAsBigDecimal());

            callbackObject.onServerCallbackObject(true, "SUCCESS",userAnalyticsDO);

        }
    };

    public Response.ErrorListener onUserAnalyticsError = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.i(TAG, "onUserAnalyticsError() Something wrong happened with the request. Error: " + error.getMessage());
            callbackObjects.onServerCallbackObject(false, error.getMessage(), null);
        }
    };


}
