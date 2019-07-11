package com.felipe.showeriocloud;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.felipe.showeriocloud.Model.BathStatisticsMonthly;
import com.felipe.showeriocloud.Model.DevicePersistance;
import com.felipe.showeriocloud.Utils.ServerCallbackObject;
import com.felipe.showeriocloud.Utils.StatisticsUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.util.Calendar;
import java.util.Date;

import static com.facebook.login.widget.ProfilePictureView.TAG;

public class IOShowerService extends FirebaseMessagingService {

    public static String CHANNEL_ID = "IO-SHOWER";
    private StatisticsUtils statisticsUtils;
    public RequestQueue requestQueue;

    public IOShowerService() {
        Log.d(TAG, "Service Initialized");
        this.statisticsUtils = new StatisticsUtils();
        this.requestQueue = Volley.newRequestQueue(getApplicationContext());

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Message Id: " + remoteMessage.getMessageId());

        if (remoteMessage.getFrom().equals("/topics/curiosity")) {
            handleCuriosityTopic(remoteMessage.getNotification().getBody());
        }
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
    }


    public void handleCuriosityTopic(String message) {
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.shower)
                        .setContentTitle("IO-Shower")
                        .setContentText(message)
                        .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "IO-Shower channel 1",
                    NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }


    public void batchJob() {

        int year = Calendar.getInstance().get(Calendar.YEAR);
        int month = Calendar.getInstance().get(Calendar.MONTH);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.shower)
                        .setContentTitle("IO-Shower")
                        .setAutoCancel(true);


        statisticsUtils.getMonthlyStatistics(Integer.toString(year), Integer.toString(month), DevicePersistance.selectedDevice, requestQueue, new ServerCallbackObject() {
            @Override
            public void onServerCallbackObject(Boolean status, String response, Object object) {
                BathStatisticsMonthly bathStatisticsMonthly = (BathStatisticsMonthly) object;

                String totalHoursText = String.valueOf(Math.floor(bathStatisticsMonthly.getTotalTime() / 3600)).split("\\.")[0] + " horas e "
                        + String.valueOf(Math.floor(Double.parseDouble("0." + Double.toString(bathStatisticsMonthly.getTotalTime() / 3600).split("\\.")[1]) * 60)).split("\\.")[0] + " minutos";

                String totalLitersText = bathStatisticsMonthly.getTotalLiters().toString() + " litros de água";

                String aproximateElectricalEnergyText = "R$ " + Double.toString((bathStatisticsMonthly.getTotalTime() / 3600) * 6800 * bathStatisticsMonthly.getEnergyPrice() / 1000);

                String message = "Esse mês você consumiu aproximadamente " + totalLitersText + " litros de água, em um valor de 100 R$";

                notificationBuilder.setContentText(message);

                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

                // Since android Oreo notification channel is needed.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                            "IO-Shower channel 1",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    notificationManager.createNotificationChannel(channel);
                }
                notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
            }

        });
    }

}
