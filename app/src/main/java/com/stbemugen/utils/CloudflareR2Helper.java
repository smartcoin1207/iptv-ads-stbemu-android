package com.stbemugen.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class CloudflareR2Helper {

    private static final String TAG = "CloudflareR2Helper";

    private static final String accessKey = "637ad4ebd52548e7575e0b60a63e8415";
    private static final String secretKey = "32551cb6b0d7c822d24a37f3b5f4c213e5a41b1d8c37ae5e748b49ae44ae696d";
    private static final String bucketName = "stbemu";
    private static final String region = "auto";
    private static final String endpointUrl = "https://145ef3f7a9832804bef0e31548db8a83.r2.cloudflarestorage.com";
    private AmazonS3Client s3Client;

    // Make Callback interface public and static
    public static interface Callback {
        void onSuccess(byte[] data);
        void onFailure(String errorMessage);
    }

    public CloudflareR2Helper() { 
        initS3Client();
    }

    private void initS3Client() {
        BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        s3Client = new AmazonS3Client(credentials);
        s3Client.setEndpoint(endpointUrl);
    }

    public void downloadFile(String fileName, Callback callback) {
        new DownloadFileTask(fileName, callback).execute();
    }

    private class DownloadFileTask extends AsyncTask<Void, Void, byte[]> {

        private String fileName;
        private Callback callback;

        public DownloadFileTask(String fileName, Callback callback) {
            this.fileName = fileName;
            this.callback = callback;
        }

        @Override
        protected byte[] doInBackground(Void... params) {
            try {
                // Download the file from Cloudflare R2
                S3Object s3Object = s3Client.getObject(bucketName, fileName);
                S3ObjectInputStream inputStream = s3Object.getObjectContent();

                // Convert inputStream to byte array
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                return byteArrayOutputStream.toByteArray();

            } catch (AmazonServiceException | IOException e) {
                Log.e(TAG, "Error downloading file: ", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(byte[] data) {
            if (data != null) {
                callback.onSuccess(data); // Pass data on success
            } else {
                callback.onFailure("Failed to download file"); // Pass error message on failure
            }
        }
    }
}
