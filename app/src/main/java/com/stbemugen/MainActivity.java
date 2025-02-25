package com.stbemugen;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.AppCompatRatingBar;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.opencsv.CSVReader;
import com.polyak.iconswitch.IconSwitch;
import com.stbemugen.utils.ServerDetailsModal;
import com.stbemugen.utils.SupabaseHelper;
import com.stbemugen.utils.CloudflareR2Helper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

public class MainActivity extends AppCompatActivity {

    private TextView tvError;
    private TextInputLayout tilPortal, tilMAC;
    private TextInputEditText etPortal, etMAC;
    private MaterialButton mainButton, copyButton, shareButton;
    private List<ServerDetailsModal> serverDetailsList;
    private ServerDetailsModal server;
    private boolean showRatingBar = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadBannerAd();
        loadInterstitialAd();
        loadRewardedAd();

        setupTheme();

        setMenuDialog();
        setLoadingDialog();
        setErrorDialog();

        MaterialToolbar materialToolbar = findViewById(R.id.main_toolbar);
        materialToolbar.setNavigationOnClickListener(v -> menuDialog.show());

        tvError = findViewById(R.id.main_text_error);

        tilPortal = findViewById(R.id.main_til_portal);
        tilMAC = findViewById(R.id.main_til_mac);

        etPortal = findViewById(R.id.main_et_portal);
        etMAC = findViewById(R.id.main_et_mac);

        mainButton = findViewById(R.id.main_button);
        copyButton = findViewById(R.id.main_button_copy);
        shareButton = findViewById(R.id.main_button_share);

        serverDetailsList = new ArrayList<>();

        tilPortal.setEndIconVisible(false);
        tilMAC.setEndIconVisible(false);

        mainButton.setOnClickListener(v -> {
            if (tvError.getVisibility() == View.VISIBLE)
                tvError.setVisibility(View.GONE);

            String text = mainButton.getText().toString();
            if (text.equalsIgnoreCase("Generate")) {

                mainButton.setText("Generating");

                copyButton.setVisibility(View.GONE);
                shareButton.setVisibility(View.GONE);

                new Handler().postDelayed(() -> {
                    Random random = new Random();

                    do {
                        server = serverDetailsList.get(random.nextInt(serverDetailsList.size() + 1));
                    } while (server.getPortal().isEmpty() || server.getMac().isEmpty());

                    etPortal.setText(server.getPortal());
                    etMAC.setText(maskMacAddress(server.getMac()));

                    showRatingBar = false;

                    tilPortal.setEndIconVisible(false);
                    tilMAC.setEndIconVisible(false);

                    mainButton.setText("View Password");

                    showInterstitialAd();
                }, 3000);

            } else if (text.equalsIgnoreCase("Check Status")) {
                //REMOVED - showInterstitialAd();

                mainButton.setText("Checking");
                checkServer();
            } else if (text.equalsIgnoreCase("View Password")) {

                mainButton.setText("Loading");

                new Handler().postDelayed(() -> {
                    showRewardedInterstitialAd();

                    etMAC.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    etMAC.setTypeface(etMAC.getTypeface(), Typeface.BOLD);

                    etMAC.setText(server.getMac());

                    showRatingBar = true;

                    tilPortal.setEndIconVisible(true);
                    tilMAC.setEndIconVisible(true);

                    mainButton.setText("Generate Another");

                    copyButton.setVisibility(View.VISIBLE);
                    shareButton.setVisibility(View.VISIBLE);
                }, 3000);
            } else if (text.equalsIgnoreCase("Generate Another")) {

                mainButton.setText("Generating");

                copyButton.setVisibility(View.GONE);
                shareButton.setVisibility(View.GONE);

                new Handler().postDelayed(() -> {
                    Random random = new Random();

                    do {
                        server = serverDetailsList.get(random.nextInt(serverDetailsList.size() + 1));
                    } while (server.getPortal().isEmpty() || server.getMac().isEmpty());

                    etPortal.setText(server.getPortal());
                    etMAC.setText(maskMacAddress(server.getMac()));

                    showRatingBar = false;

                    tilPortal.setEndIconVisible(false);
                    tilMAC.setEndIconVisible(false);

                    mainButton.setText("View Password");

                    showInterstitialAd();
                }, 3000);
            }
        });

        tilPortal.setEndIconOnClickListener(v -> {
            if (server != null) {
                //REMOVED - showInterstitialAd();

                String text = server.getPortal();

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied Portal", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "Portal copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        tilMAC.setEndIconOnClickListener(v -> {

            if (server != null) {
                //REMOVED -  showInterstitialAd();

                String text = server.getMac();

                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Copied MAC Address", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(MainActivity.this, "MAC Address copied to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        copyButton.setOnClickListener(v -> {
            //REMOVED -  showInterstitialAd();

            String text = server.getPortal() + "\n" + server.getMac();

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Server", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(MainActivity.this, "Server copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        shareButton.setOnClickListener(v -> {
            //REMOVED - showInterstitialAd();

            String text = server.getPortal() + "\n" + server.getMac();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(shareIntent, "Share via"));

        });

        onBackPress();

        if (serverDetailsList.isEmpty())
            loadFileFromCloudflareR2();

        //Update
        checkForAppUpdate();
    }

    //update
    private void checkForAppUpdate() {
        AppUpdateManagerFactory.create(this).getAppUpdateInfo()
                .addOnSuccessListener(appUpdateInfo -> {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        new AlertDialog.Builder(getApplicationContext())
                                .setTitle("Update Available")
                                .setMessage("A new version of the app is available. Please update to keep the app running smoothly.")
                                .setNegativeButton("Later", (dialogInterface, i) -> dialogInterface.dismiss())
                                .setPositiveButton("Update now", (dialogInterface, i) -> {

                                }).show();
                    }
                });
    }

    public String maskMacAddress(String mac) {
        String[] parts = mac.split(":");
        if (parts.length == 6) {
            etMAC.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            etMAC.setTypeface(etMAC.getTypeface(), Typeface.BOLD);

            return parts[0] + ":" + parts[1] + ":" + parts[2] + ":XX:XX:XX";
        }else{
            etMAC.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            etMAC.setTypeface(etMAC.getTypeface(), Typeface.BOLD);
        }
        return mac; // Return original if format is incorrect
    }

    // APP THEME
    private void setupTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences("ThemePref", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;

        if (isDarkMode && currentNightMode != android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (!isDarkMode && currentNightMode != android.content.res.Configuration.UI_MODE_NIGHT_NO) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    // DIALOG
    private AlertDialog menuDialog, loadingDialog, errorDialog;

    private void setMenuDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_main_menu, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);

        IconSwitch iconSwitch = dialogView.findViewById(R.id.menu_icon_switch);

        SharedPreferences sharedPreferences = getSharedPreferences("ThemePref", MODE_PRIVATE);
        boolean isDarkMode = sharedPreferences.getBoolean("isDarkMode", false);

        if (isDarkMode)
            iconSwitch.setChecked(IconSwitch.Checked.RIGHT);
        else
            iconSwitch.setChecked(IconSwitch.Checked.LEFT);

        iconSwitch.setCheckedChangeListener(current -> {
            if (current == IconSwitch.Checked.RIGHT) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);  // Switch to dark theme

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isDarkMode", true);
                editor.apply();
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);  // Switch to light theme

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("isDarkMode", false);
                editor.apply();
            }

            etPortal.setText("");
            etMAC.setText("");
        });

        dialogView.findViewById(R.id.menu_close).setOnClickListener(v -> menuDialog.dismiss());

        menuDialog = dialogBuilder.create();
    }

    private void setLoadingDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading_file, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);

        loadingDialog = dialogBuilder.create();
    }

    private void setErrorDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_error_loading, null);

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);

        dialogView.findViewById(R.id.error_retry).setOnClickListener(v -> {
            errorDialog.dismiss();
            loadFileFromCloudflareR2();
        });

        dialogView.findViewById(R.id.error_exit).setOnClickListener(v -> finish());

        errorDialog = dialogBuilder.create();
    }

    // ADS
    private InterstitialAd mInterstitialAd;
    private RewardedAd rewardedAd;

    private void loadBannerAd() {
        //AdView adView1 = findViewById(R.id.main_banner_ad_view_1);
        AdView adView2 = findViewById(R.id.main_banner_ad_view_2);

        AdRequest adRequest = new AdRequest.Builder().build();

        //adView1.loadAd(adRequest);
        adView2.loadAd(adRequest);
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, getString(R.string.admob_interstitial_id), adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        // The interstitialAd reference will be null until
                        // an ad is loaded.
                        mInterstitialAd = interstitialAd;

                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                // Called when ad is dismissed.
                                loadInterstitialAd();
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                                // Called when ad fails to show.
                                loadInterstitialAd();
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                // Called when ad is shown.
                                mInterstitialAd = null;  // Set the ad reference to null so you don't show the ad again.
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError adError) {
                        // Handle the error
                        mInterstitialAd = null;
                    }
                });
    }

    private void showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        } else {
            loadInterstitialAd();
        }
    }

    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();

        RewardedAd.load(this, getString(R.string.admob_rewarded_id), adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rAd) {
                rewardedAd = rAd;

                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                    }

                    @Override
                    public void onAdDismissedFullScreenContent() {
                        super.onAdDismissedFullScreenContent();
                        loadRewardedAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        super.onAdFailedToShowFullScreenContent(adError);
                        loadRewardedAd();
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        super.onAdShowedFullScreenContent();
                        loadRewardedAd();
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
                loadRewardedAd();
            }
        });
    }

    private void showRewardedInterstitialAd() {
        if (rewardedAd != null) {
            rewardedAd.show(MainActivity.this, rewardItem -> {

            });
        } else {
            loadRewardedAd();
        }
    }

    // BACK BUTTON
    private void onBackPress() {
        OnBackPressedDispatcher onBackPressedDispatcher = getOnBackPressedDispatcher();

        // Register a callback to handle back presses
        onBackPressedDispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (showRatingBar) {
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_rate, null, false);
                    AppCompatRatingBar ratingBar = dialogView.findViewById(R.id.dialog_rate_bar);

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Love this app?")
                            .setMessage("Please take a moment to rate us")
                            .setView(dialogView)
                            .setPositiveButton("Rate Now", (
                                    dialog, which) -> {
                                if (ratingBar.getRating() >= 3) {
                                    final String appPackageName = getPackageName();
                                    try {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                                    } catch (android.content.ActivityNotFoundException e) {
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                                    }
                                } else
                                    Toast.makeText(MainActivity.this, "Thanks for feedback", Toast.LENGTH_SHORT).show();

                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Exit")
                                        .setMessage("Do you want to exit the application?")
                                        .setPositiveButton("Exit", (dialog1, which1) -> finishAffinity())
                                        .setNegativeButton("Cancel", (dialog1, which1) -> dialog1.dismiss())
                                        .show();
                            })
                            .setNegativeButton("Later", (dialog, which) -> {
                                new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Exit")
                                        .setMessage("Do you want to exit the application?")
                                        .setPositiveButton("Exit", (dialog1, which1) -> finishAffinity())
                                        .setNegativeButton("Cancel", (dialog1, which1) -> dialog1.dismiss())
                                        .show();
                                dialog.dismiss();
                            })
                            .show();
                } else
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Exit")
                            .setMessage("Do you want to exit the application?")
                            .setPositiveButton("Exit", (dialog, which) -> finishAffinity())
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .show();
            }
        });

    }

    // LOAD FILE

    // private void loadFileFromSupabase() {
    //     loadingDialog.show();

    //     SupabaseHelper.downloadFile("stalker_server.csv.gz", new SupabaseHelper.Callback() {
    //         @Override
    //         public void onSuccess(byte[] data) {
    //             List<String[]> csvData = decompressAndParseCsv(data);
    //             populateServerDetailsList(csvData);

    //             runOnUiThread(() -> {
    //                 if (!serverDetailsList.isEmpty()) {
    //                     loadingDialog.dismiss();
    //                     errorDialog.dismiss();
    //                 } else {
    //                     loadFileFromAppwrite();
    //                 }
    //             });
    //         }

    //         @Override
    //         public void onFailure(String errorMessage) {
    //             loadFileFromAppwrite();
    //         }
    //     });
    // }

    private void loadFileFromCloudflareR2() { 
        CloudflareR2Helper cloudflareR2Helper = new CloudflareR2Helper();

        cloudflareR2Helper.downloadFile("stalker_server.csv.gz", new CloudflareR2Helper.Callback() {
            @Override
            public void onSuccess(byte[] data) {
                List<String[]> csvData = decompressAndParseCsv(data);
                populateServerDetailsList(csvData);
                
                runOnUiThread(() -> {
                    if (!serverDetailsList.isEmpty()) {
                        loadingDialog.dismiss();
                        errorDialog.dismiss();
                    } else {
                        handleFailure();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                handleFailure();
            }
        });
    }

    private void loadFileFromAppwrite() {
        // Create a background thread pool
        ExecutorService executorService = Executors.newSingleThreadExecutor();

        // Run the network operation on a background thread
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                String projectId = "67b32ab7003d0ac3a18c";  // Replace with your Appwrite project ID
                String bucketId = "67b32acd002d8f262c5c";  // Replace with your Appwrite bucket ID
                String fileId = "servers";  // Replace with your Appwrite file ID

                String endpoint = "https://cloud.appwrite.io/v1/storage/buckets/" + bucketId + "/files/" + fileId + "/download";
                String apiKey = "standard_401a0b44cd32be62d0cf381bf3e7309d5b49843b9bf963ad5e04916c3b928f1efbbd61c208563ea8da6866657fdc428db2177b8d82dd35f718342e78851c19487ef0a32fe325d8ab9876b02ad625a5db6762263438d2f6ca9c32111ab4600732dfe852fd04bbabc51b52f413c151353d71fe4afb7b1764edd94f967c2c5673b4";

                try {
                    URL url = new URL(endpoint);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-Appwrite-Project", projectId);
                    connection.setRequestProperty("X-Appwrite-Key", apiKey);  // Replace with your Appwrite API key
                    connection.setConnectTimeout(3000);  // Set timeout (optional)
                    connection.setReadTimeout(5000);     // Set read timeout (optional)

                    int statusCode = connection.getResponseCode();
                    if (statusCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = connection.getInputStream();
                        byte[] data = readInputStream(inputStream);
                        List<String[]> csvData = decompressAndParseCsv(data);
                        populateServerDetailsList(csvData);

                        // Post the result back to the main thread
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (!serverDetailsList.isEmpty()) {
                                    loadingDialog.dismiss();
                                    errorDialog.dismiss();
                                } else {
                                    loadingDialog.dismiss();
                                    errorDialog.show();
                                }
                            }
                        });
                    } else {
                        // Handle non-OK response
                        handleFailure();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    handleFailure();
                }
            }
        });

        // You can shut down the executor service when done
        executorService.shutdown();
    }


    private byte[] readInputStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        return byteArrayOutputStream.toByteArray();
    }

    private void handleFailure() {
        runOnUiThread(() -> {
            loadingDialog.dismiss();
            errorDialog.show();
        });
    }

    private List<String[]> decompressAndParseCsv(byte[] gzippedData) {
        List<String[]> data = new ArrayList<>();
        try {
            // Decompress the gzipped data
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(gzippedData);
            GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
            InputStreamReader inputStreamReader = new InputStreamReader(gzipInputStream);
            CSVReader csvReader = new CSVReader(inputStreamReader);
            String[] nextLine;

            // Read the CSV file line by line
            while ((nextLine = csvReader.readNext()) != null) {
                data.add(nextLine);  // Add each line to the list
            }

            csvReader.close();
            gzipInputStream.close();
        } catch (Exception e) {
            Log.e("decompressAndParseCsv", "An error occurred : ", e);
        }
        return data;
    }

    private void populateServerDetailsList(List<String[]> csvData) {

        for (String[] row : csvData) {
            if (row.length == 2) {
                String portal = row[0];
                String mac = row[1];

                ServerDetailsModal serverDetails = new ServerDetailsModal(portal, mac);
                serverDetailsList.add(serverDetails);
            }
        }
    }

    // CHECK FILE
    @SuppressLint("SetTextI18n")
    private void checkServer() {
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        // Countdown timer for 10 seconds (10000 milliseconds)
        CountDownTimer countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mainButton.setText("Checking (" + millisUntilFinished / 1000 + ")");
            }

            @Override
            public void onFinish() {
                requestQueue.cancelAll("serverCheck");

                mainButton.setText("Generate");

                tvError.setText("This server is not working.\nGenerate another");
                tvError.setVisibility(View.VISIBLE);

                playFailedSound();
            }
        };
        countDownTimer.start();

        String portal = server.getPortal();

        if (portal.endsWith("/"))
            portal = portal.substring(0, portal.length() - 1);

        String url = portal +
                "/portal.php?type=account_info&action=get_main_info&JsHttpRequest=1-xml&mac="
                + server.getMac();

        // Create a JsonObjectRequest
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject jsonObject = response.getJSONObject("js");
                        String phone = jsonObject.getString("phone");

                        countDownTimer.cancel();
                        mainButton.setText("View Password");

                        if (phone.isEmpty()) {
                            tvError.setText("Unable to show server details");
                            tvError.setVisibility(View.VISIBLE);

                            return;
                        } else {
                            tvError.setText(phone);
                            tvError.setVisibility(View.VISIBLE);
                        }

                        playSuccessSound();

                    } catch (JSONException e) {
                        Log.e("checkServer", "An error occurred : ", e);
                        tvError.setText("Unable to show server details");
                        tvError.setVisibility(View.VISIBLE);

                        countDownTimer.cancel();
                        mainButton.setText("View Password");
                    }
                },
                error -> {
                    // Handle the error
                    countDownTimer.cancel();
                    mainButton.setText("Generate");

                    tvError.setText("This server is not working.\nGenerate another");
                    tvError.setVisibility(View.VISIBLE);
                    //Toast.makeText(MainActivity.this, "Server not working. Try again", Toast.LENGTH_SHORT).show();

                    playFailedSound();
                });

        jsonObjectRequest.setTag("serverCheck");

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                10000, // Timeout in milliseconds (10 seconds)
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, // Number of retries
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT // Backoff multiplier
        ));

        // Add the request to the RequestQueue
        requestQueue.add(jsonObjectRequest);
    }

    private MediaPlayer successAudio, failedAudio;

    private void playSuccessSound() {
        // Release any existing MediaPlayer before creating a new one
        if (successAudio != null) {
            successAudio.release();
            successAudio = null;
        }

        // Initialize a new MediaPlayer instance with the sound file
        successAudio = MediaPlayer.create(this, R.raw.success);

        // Start playing the sound
        if (successAudio != null) {
            successAudio.start();

            // Release the MediaPlayer when playback is complete
            successAudio.setOnCompletionListener(mp -> {
                mp.release();
                successAudio = null; // Set mediaPlayer to null after release
            });
        }
    }

    private void playFailedSound() {
        // Release any existing MediaPlayer before creating a new one
        if (failedAudio != null) {
            failedAudio.release();
            failedAudio = null;
        }

        // Initialize a new MediaPlayer instance with the sound file
        failedAudio = MediaPlayer.create(this, R.raw.failed);

        // Start playing the sound
        if (failedAudio != null) {
            failedAudio.start();

            // Release the MediaPlayer when playback is complete
            failedAudio.setOnCompletionListener(mp -> {
                mp.release();
                failedAudio = null; // Set mediaPlayer to null after release
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the MediaPlayer when the activity is destroyed
        if (successAudio != null) {
            successAudio.release();
            successAudio = null;
        }
        if (failedAudio != null) {
            failedAudio.release();
            failedAudio = null;
        }
    }
}