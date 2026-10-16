package com.example.edutech.utils;

import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NotificationHelper {

    // 🔑 Replace with your Firebase Console Server Key
    private static final String SERVER_KEY = "YOUR_SERVER_KEY_HERE";

    // Send notification to single user token
    public static void sendFCMNotification(String token, String title, String message) {
        new Thread(() -> {
            try {
                URL url = new URL("https://fcm.googleapis.com/fcm/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setUseCaches(false);
                conn.setDoInput(true);
                conn.setDoOutput(true);

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "key=" + SERVER_KEY);
                conn.setRequestProperty("Content-Type", "application/json");

                JSONObject json = new JSONObject();
                json.put("to", token);

                JSONObject info = new JSONObject();
                info.put("title", title);
                info.put("body", message);

                json.put("notification", info);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes());
                os.flush();
                os.close();

                conn.getInputStream(); // trigger request

                Log.d("FCM", "Notification sent to token: " + token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
