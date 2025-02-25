package com.stbemugen.utils;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SupabaseHelper {
    private static final String SUPABASE_URL = "https://vmftiqibigdiogfokjyl.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZtZnRpcWliaWdkaW9nZm9ranlsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3MzkxODk5NzIsImV4cCI6MjA1NDc2NTk3Mn0.duj2jxJ1l4lb8G7Cj6Htz7kSKhysxOakcx4JuVpx6Y4";
    private static final String STORAGE_BUCKET = "PORTALS-MAC";

    private static final OkHttpClient client = new OkHttpClient();

    public interface Callback {
        void onSuccess(byte[] data);

        void onFailure(String errorMessage);
    }

    public static void downloadFile(String fileName, Callback callback) {
        String url = SUPABASE_URL + "/storage/v1/object/public/" + STORAGE_BUCKET + "/" + fileName;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new okhttp3.Callback() {  // ✅ Use OkHttp Callback
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e.getMessage());  // Pass error to our custom Callback
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body().bytes());  // Pass data to our custom Callback
                } else {
                    callback.onFailure("Failed to fetch file");
                }
            }
        });
    }

}
