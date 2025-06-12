package com.example.axis_bank_testing;

import com.example.axis_bank_testing.R;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.media.Image; // Added import for android.media.Image
import android.util.Size;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage; // Added import
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
// import androidx.core.graphics.Insets; // Commented out as ViewCompat.setOnApplyWindowInsetsListener will be removed
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowCompat; // Added for setDecorFitsSystemWindows
import androidx.core.view.WindowInsetsControllerCompat; // Added
import androidx.lifecycle.LifecycleOwner;
import android.view.Window; // Added
import android.view.WindowManager; // Added
import android.os.Build; // Added

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import in.juspay.hypersdk.data.JuspayResponseHandler;
import in.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter;
import in.juspay.services.HyperServices;

public class MainActivity extends AppCompatActivity {

    private HyperServices hyperInstance;
    private EditText customerLoginIdEditText;
    private Button startSdkButton;
    private ProgressBar progressBar;
    private CheckBox passBankHeaderCheckbox;
    private CheckBox hitJuspayBackendCheckbox;
    private CardView customToastContainer;
    private TextView customToastTextView;
    private Handler customToastHandler = new Handler(Looper.getMainLooper());
    private Runnable hideCustomToastRunnable;

    private String scannedUrl;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;

    // CameraX and ML Kit variables
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private ImageAnalysis imageAnalysis;
    private final AtomicBoolean isScanning = new AtomicBoolean(false);
    private ProcessCameraProvider cameraProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply initial window settings
        applySystemUISettings();

        customerLoginIdEditText = findViewById(R.id.customer_login_id_input);
        startSdkButton = findViewById(R.id.supabutton);
        progressBar = findViewById(R.id.progressBar);
        passBankHeaderCheckbox = findViewById(R.id.pass_bank_header_checkbox);
        hitJuspayBackendCheckbox = findViewById(R.id.hit_juspay_backend_checkbox);
        customToastContainer = findViewById(R.id.custom_toast_include);
        customToastTextView = findViewById(R.id.custom_toast_text);
        previewView = findViewById(R.id.camera_preview); // Initialize PreviewView

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Configure ML Kit Barcode Scanner
        BarcodeScannerOptions options
                = new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build();
        barcodeScanner = BarcodeScanning.getClient(options);

        // ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
        //     Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
        //     v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
        //     return insets;
        // });
        // The above listener is removed as applySystemUISettings and fitsSystemWindows in XML will handle it.
        startSdkButton.setOnClickListener(v -> {
            Log.d("BUTTONS", "User tapped the Start SDK button");
            // Toggle scanning state
            if (isScanning.get()) {
                stopCameraAndScanner();
                showCustomMessage("Scanner stopped.", 2000);
                startSdkButton.setText("Start Scan & SDK");
            } else {
                checkCameraPermissionAndSetupCamera();
                startSdkButton.setText("Stop Scan");
            }
        });
        // Initially hide preview
        previewView.setVisibility(View.GONE);
    }

    private void showCustomMessage(final String message, int durationMillis) {
        runOnUiThread(() -> {
            if (message == null || message.isEmpty()) {
                customToastContainer.setVisibility(View.GONE);
                return;
            }
            customToastTextView.setText(message);
            customToastContainer.setVisibility(View.VISIBLE);

            if (hideCustomToastRunnable != null) {
                customToastHandler.removeCallbacks(hideCustomToastRunnable);
            }

            hideCustomToastRunnable = () -> customToastContainer.setVisibility(View.GONE);
            customToastHandler.postDelayed(hideCustomToastRunnable, durationMillis);
        });
    }

    private void showLoader(boolean show) {
        runOnUiThread(() -> {
            if (show) {
                progressBar.setVisibility(View.VISIBLE);
                // Keep scan button enabled to allow stopping the scan
                // startSdkButton.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE);
                startSdkButton.setEnabled(true);
                startSdkButton.setText("Start Scan & SDK"); // Reset button text
            }
        });
    }

    private void checkCameraPermissionAndSetupCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        }
    }

    private void setupCamera() {
        if (isScanning.getAndSet(true)) { // Already scanning or setup in progress
            return;
        }
        previewView.setVisibility(View.VISIBLE); // Show preview when starting scan
        startSdkButton.setText("Stop Scan");

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (Exception e) {
                Log.e("CAMERA_X", "Error setting up camera provider", e);
                showCustomMessage("Error setting up camera: " + e.getMessage(), 5000);
                isScanning.set(false);
                previewView.setVisibility(View.GONE);
                startSdkButton.setText("Start Scan & SDK");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) {
            Log.e("CAMERA_X", "Camera provider not available");
            isScanning.set(false);
            previewView.setVisibility(View.GONE);
            startSdkButton.setText("Start Scan & SDK");
            return;
        }

        Preview cameraPreview = new Preview.Builder()
                .setTargetResolution(new Size(previewView.getWidth(), previewView.getHeight())) // Use PreviewView dimensions
                .build();
        cameraPreview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 480)) // A common resolution for analysis
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, image -> {
            if (!isScanning.get()) { // Check if scanning was stopped
                image.close();
                return;
            }
            @androidx.camera.core.ExperimentalGetImage
            android.media.Image mediaImage = image.getImage();
            if (mediaImage != null) {
                InputImage inputImage = InputImage.fromMediaImage(mediaImage, image.getImageInfo().getRotationDegrees());
                barcodeScanner.process(inputImage)
                        .addOnSuccessListener(barcodes -> {
                            if (!isScanning.get()) { // Double check after async operation
                                image.close();
                                return;
                            }
                            handleBarcodes(barcodes);
                            image.close(); // Crucial to close the image
                        })
                        .addOnFailureListener(e -> {
                            if (!isScanning.get()) {
                                image.close();
                                return;
                            }
                            Log.e("ML_KIT", "Barcode scanning failed", e);
                            // Optionally show a transient error, but don't stop scanning for every failed frame
                            image.close(); // Crucial to close the image
                        });
            } else {
                image.close(); // Ensure image is closed even if mediaImage is null
            }
        });

        try {
            cameraProvider.unbindAll(); // Unbind previous use cases
            Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, cameraPreview, imageAnalysis);
        } catch (Exception e) {
            Log.e("CAMERA_X", "Use case binding failed", e);
            showCustomMessage("Error binding camera: " + e.getMessage(), 5000);
            isScanning.set(false);
            previewView.setVisibility(View.GONE);
            startSdkButton.setText("Start Scan & SDK");
        }
    }

    private void handleBarcodes(List<Barcode> barcodes) {
        if (!barcodes.isEmpty() && isScanning.get()) { // Process only if still scanning
            Barcode barcode = barcodes.get(0); // Get the first detected barcode
            String rawValue = barcode.getRawValue();
            if (rawValue != null && !rawValue.isEmpty()) {
                scannedUrl = rawValue;
                Log.d("ML_KIT", "QR Code Scanned: " + scannedUrl);

                // Stop scanning and camera once a QR code is found
                stopCameraAndScanner();

                runOnUiThread(() -> {
                    Toast.makeText(this, "Scanned: " + scannedUrl, Toast.LENGTH_LONG).show();
                    hideKeyboard();
                    showLoader(true);
                    initiateSdkOperation();
                });
            }
        }
    }

    private void stopCameraAndScanner() {
        if (isScanning.getAndSet(false)) { // Ensure this runs only once
            if (imageAnalysis != null) {
                imageAnalysis.clearAnalyzer(); // Stop analysis
            }
            if (cameraProvider != null) {
                cameraProvider.unbindAll(); // Unbind all use cases to release camera
            }
            runOnUiThread(() -> {
                previewView.setVisibility(View.GONE); // Hide preview
                startSdkButton.setText("Start Scan & SDK"); // Reset button text
                showLoader(false); // Ensure loader is hidden if scan is stopped manually
            });
            Log.d("CAMERA_X", "Camera and scanner stopped.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera(); // Permission granted, setup camera
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                isScanning.set(false); // Ensure scanning state is reset
                previewView.setVisibility(View.GONE);
                startSdkButton.setText("Start Scan & SDK");
            }
        }
    }

    // onActivityResult is no longer needed for QR scanning with ML Kit + CameraX
    @Override
    public void onBackPressed() {
        if (isScanning.get()) { // If scanner is active, stop it first
            stopCameraAndScanner();
        } else if (hyperInstance != null && !hyperInstance.onBackPressed()) {
            // If HyperServices handled it, do nothing further.
            // If not, and we want to fall back to default behavior:
            super.getOnBackPressedDispatcher().onBackPressed(); // Corrected method
        } else {
            super.getOnBackPressedDispatcher().onBackPressed(); // Corrected method
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus();
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && customerLoginIdEditText != null) {
                imm.hideSoftInputFromWindow(customerLoginIdEditText.getWindowToken(), 0);
            }
        }
    }

    private void initiateSdkOperation() {
        hideKeyboard();
        showLoader(true);
        try {
            JSONObject initiationPayload = new JSONObject();
            initiationPayload.put("requestId", "04a90298-4b2d-4de8-8e49-3c80fe003b39"); // Consider making this dynamic
            initiationPayload.put("service", "in.juspay.ibmb");

            JSONObject payload = new JSONObject();
            payload.put("action", "initiate");
            payload.put("clientId", "axisbank");
            payload.put("environment", "sandbox");
            String customerId = customerLoginIdEditText.getText().toString();
            payload.put("bankCustomerId", customerId);
            payload.put("passBankHeader", passBankHeaderCheckbox.isChecked());
            payload.put("hitPPIUrl", hitJuspayBackendCheckbox.isChecked());

            initiationPayload.put("payload", payload);

            ViewGroup sdkUiContainerMain = findViewById(R.id.sdk_ui_container_main);
            hyperInstance = new HyperServices(this, sdkUiContainerMain);

            hyperInstance.initiate(initiationPayload, new HyperPaymentsCallbackAdapter() {
                @Override
                public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                    try {
                        Log.d("HYPER_SDK_EVENT", data.toString());
                        final String event = data.getString("event");
                        final String eventDataString = "Event: " + event + "\nData: \n" + data.toString(2);
                        Log.d("UI_UPDATE", "Event received: " + event + ", Data: " + eventDataString);

                        runOnUiThread(() -> {
                            try {
                                if (event.equals("show_loader")) {
                                    Log.d("SDK Event", "HyperSDK event: show_loader");
                                    // We manage our own loader, but good to log
                                } else if (event.equals("hide_loader")) {
                                    Log.d("SDK Event", "HyperSDK event: hide_loader");
                                    // We manage our own loader
                                } else if (event.equals("initiate_result")) {
                                    Log.d("SDK Event", "HyperSDK event: initiate_result");
                                    showCustomMessage(eventDataString, 500);
                                    JSONObject response = data.optJSONObject("payload");
                                    if (hyperInstance.isInitialised()) {
                                        JSONObject processPayload = createProcessPayload(response);
                                        if (processPayload != null) {
                                            hyperInstance.process(processPayload);
                                            customToastHandler.postDelayed(MainActivity.this::applySystemUISettings, 500);
                                        } else {
                                            showLoader(false);
                                            showCustomMessage("Error: Could not create process payload (scanned URL missing?).", 10000);
                                        }
                                    } else {
                                        showLoader(false);
                                        showCustomMessage("Error: HyperInstance not initialised.", 10000);
                                    }
                                } else if (event.equals("process_result")) {
                                    Log.d("SDK Event", "HyperSDK event: process_result");
                                    showCustomMessage(eventDataString, 10000);
                                    customToastHandler.postDelayed(MainActivity.this::applySystemUISettings, 500);
                                    showLoader(false); // Hide loader after process_result
                                } else if (event.equals("log_stream")) {
                                    Log.i("=>Clickstream", data.toString());
                                } else if (event.equals("session_expired")) {
                                    Log.w("SDK Event", "Session expired.");
                                    showCustomMessage(eventDataString, 10000);
                                    showLoader(false);
                                } else {
                                    // For other events, just show the data
                                    showCustomMessage(eventDataString, 5000);
                                }
                            } catch (Exception e) {
                                Log.e("SDK_UI_ERROR", "Error in onEvent UI update: " + e.getMessage(), e);
                                showCustomMessage("Error in event handling UI: \n" + e.getMessage(), 10000);
                                showLoader(false);
                            }
                        });
                    } catch (Exception e) {
                        Log.e("HYPER_SDK_ON_EVENT_ERROR", "Error processing onEvent data: " + e.getMessage(), e);
                        final String errorMsg = "Critical error in SDK event: " + e.getMessage();
                        runOnUiThread(() -> {
                            showCustomMessage(errorMsg, 15000);
                            showLoader(false);
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e("SDK_INIT_ERROR", "Error creating initiation payload: " + e.getMessage(), e);
            final String errorMessage = "Error creating initiation payload: \n" + e.getMessage();
            runOnUiThread(() -> showCustomMessage(errorMessage, 10000));
            showLoader(false);
        }
    }

    private JSONObject createProcessPayload(JSONObject response) throws Exception { // response from initiate_result is not used here
        JSONObject processPayload = new JSONObject();
        processPayload.put("requestId", java.util.UUID.randomUUID().toString());
        processPayload.put("service", "in.juspay.ibmb");

        JSONObject payload = new JSONObject();
        payload.put("action", "ibmbInitiateTxn");
        payload.put("timestamp", String.valueOf(System.currentTimeMillis()));
        payload.put("txnType", "IBMB_SCANPAY");

        if (scannedUrl == null || scannedUrl.isEmpty()) {
            Log.e("SDK_PAYLOAD_ERROR", "Scanned URL is missing!");
            final String errorMsg = "Error: Scanned URL is missing in createProcessPayload.";
            runOnUiThread(() -> {
                showCustomMessage("Scanned URL is missing. Please scan again.\n" + errorMsg, 10000);
                showLoader(false); // Ensure loader is hidden
            });
            return null;
        }
        payload.put("url", scannedUrl);
        String customerIdProcess = customerLoginIdEditText.getText().toString();
        payload.put("bankCustomerId", customerIdProcess);

        processPayload.put("payload", payload);
        return processPayload;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        // Ensure camera is released
        if (isScanning.get() && cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        // Remove any pending toast messages
        if (customToastHandler != null && hideCustomToastRunnable != null) {
            customToastHandler.removeCallbacks(hideCustomToastRunnable);
        }
    }

    private void applySystemUISettings() {
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For Android 11 (API 30) and above
            WindowCompat.setDecorFitsSystemWindows(window, true);

            WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(window, window.getDecorView());
            if (insetsController != null) {
                insetsController.show(WindowInsetsCompat.Type.statusBars());
                insetsController.show(WindowInsetsCompat.Type.navigationBars());
            }
        } else {
            // For versions below Android 11 (API 30)
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }
}
