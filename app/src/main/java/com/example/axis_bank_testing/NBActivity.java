package com.example.axis_bank_testing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity; // Assuming it needs to remain FragmentActivity
// If not, and AppCompat features are needed, change to: import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import in.juspay.hypersdk.data.JuspayResponseHandler;
import in.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter;
import in.juspay.services.HyperServices;

public class NBActivity extends FragmentActivity { // Or AppCompatActivity if needed

    private HyperServices hyperInstance;
    private EditText customerLoginIdEditTextNb;
    private Button startSdkButtonNb;
    private ProgressBar progressBarNb;
    private CheckBox passBankHeaderCheckboxNb;
    private CheckBox hitJuspayBackendCheckboxNb;
    private CardView customToastContainer;
    private TextView customToastTextView;
    private Handler customToastHandler = new Handler(Looper.getMainLooper());
    private Runnable hideCustomToastRunnable;

    private String incomingUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nb);

        customerLoginIdEditTextNb = findViewById(R.id.customer_login_id_input_nb);
        startSdkButtonNb = findViewById(R.id.start_sdk_button_nb);
        progressBarNb = findViewById(R.id.progress_bar_nb);
        passBankHeaderCheckboxNb = findViewById(R.id.pass_bank_header_checkbox_nb);
        hitJuspayBackendCheckboxNb = findViewById(R.id.hit_juspay_backend_checkbox_nb);
        customToastContainer = findViewById(R.id.custom_toast_container);
        customToastTextView = findViewById(R.id.custom_toast_text);

        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            incomingUrl = data.toString();
            Log.d("NBActivity", "Received Intent with URI: " + incomingUrl);
            // Don't initialize and process here anymore, wait for button click
            // initializeAndProcess(incomingUrl); // Old call
        } else {
            // Check for extras as a fallback, though deep links are preferred
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("payment_url")) {
                incomingUrl = extras.getString("payment_url");
                Log.d("NBActivity", "Received Intent with payment_url extra: " + incomingUrl);
            } else {
                Log.e("NBActivity", "No intent data (URI or extra) received for URL.");
                showCustomMessage("Error: No payment URL received.", 10000);
                startSdkButtonNb.setEnabled(false); // Disable button if no URL
            }
        }

        startSdkButtonNb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerSdkFlow();
            }
        });
    }

    private void showCustomMessage(final String message, int durationMillis) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (message == null || message.isEmpty()) {
                    if (customToastContainer != null) {
                        customToastContainer.setVisibility(View.GONE);
                    }
                    return;
                }
                if (customToastTextView != null) {
                    customToastTextView.setText(message);
                }
                if (customToastContainer != null) {
                    customToastContainer.setVisibility(View.VISIBLE);
                }

                if (hideCustomToastRunnable != null) {
                    customToastHandler.removeCallbacks(hideCustomToastRunnable);
                }

                hideCustomToastRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (customToastContainer != null) {
                            customToastContainer.setVisibility(View.GONE);
                        }
                    }
                };
                customToastHandler.postDelayed(hideCustomToastRunnable, durationMillis);
            }
        });
    }

    private void showLoaderNb(boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    if (progressBarNb != null) {
                        progressBarNb.setVisibility(View.VISIBLE);
                    }
                    if (startSdkButtonNb != null) {
                        startSdkButtonNb.setEnabled(false);
                    }
                } else {
                    if (progressBarNb != null) {
                        progressBarNb.setVisibility(View.GONE);
                    }
                    if (startSdkButtonNb != null) {
                        startSdkButtonNb.setEnabled(true);
                    }
                }
            }
        });
    }

    private void triggerSdkFlow() {
        hideKeyboard(); // Hide keyboard on button click
        showCustomMessage(null, 0); // Clear previous custom message
        showLoaderNb(true);

        String customerId = customerLoginIdEditTextNb.getText().toString();

        if (customerId.isEmpty()) {
            showCustomMessage("Error: Customer Login ID is required.", 10000);
            showLoaderNb(false);
            return;
        }

        if (incomingUrl == null || incomingUrl.isEmpty()) {
            showCustomMessage("Error: Payment URL is missing.", 10000);
            showLoaderNb(false);
            return;
        }

        initializeAndProcess(incomingUrl, customerId);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
            view.clearFocus(); // Optionally clear focus
        } else {
            // If no view has focus, try to hide it from the window token of a known view
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null && customerLoginIdEditTextNb != null) {
                imm.hideSoftInputFromWindow(customerLoginIdEditTextNb.getWindowToken(), 0);
            }
        }
        // Fallback for root view focus clear if needed
        // View rootView = findViewById(android.R.id.content);
        // if (rootView != null) {
        //     rootView.clearFocus();
        // }
    }

    private void initializeAndProcess(String url, String customerId) {
        try {
            // Using android.R.id.content to get the root view for HyperServices
            ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content).getRootView();
            hyperInstance = new HyperServices(this, rootView);

            JSONObject initiationPayload = createInitiationPayload(customerId);

            hyperInstance.initiate(initiationPayload, new HyperPaymentsCallbackAdapter() {
                @Override
                public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                    final String event;
                    String eventDataString = "Event: unknown"; // Default
                    try {
                        event = data.getString("event");
                        eventDataString = "Event: " + event + "\nData: \n" + data.toString(2);
                        Log.d("NB_SDK_EVENT", "Event: " + event + ", Data: " + data.toString());

                        final String finalEventDataString = eventDataString; // Effectively final for runOnUiThread

                        if (event.equals("initiate_result")) {
                            showCustomMessage(finalEventDataString, 10000);
                            if (hyperInstance.isInitialised()) {
                                JSONObject processPayload = createProcessPayload(data.optJSONObject("payload"), url, customerId);
                                if (processPayload != null) {
                                    hyperInstance.process(processPayload);
                                } else {
                                    showLoaderNb(false);
                                    // Error message already shown by createProcessPayload if it returns null
                                }
                            } else {
                                showLoaderNb(false);
                                showCustomMessage("Error: HyperSDK not initialized after initiate_result.", 10000);
                            }
                        } else if (event.equals("process_result")) {
                            showCustomMessage(finalEventDataString, 10000); // Show message first
                            showLoaderNb(false);
                            final JSONObject response = data.optJSONObject("payload");

                            // Delay sending result back to allow custom message to be seen
                            customToastHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (response != null) {
                                        sendResultBack(response.toString());
                                    } else {
                                        showCustomMessage("Process finished, but no payload in result. Closing.", 10000);
                                        // Delay finish if showing another message
                                        customToastHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                sendResultBack("{\"status\":\"Failed\", \"message\":\"Empty process result\"}");
                                            }
                                        }, 10000); // Show this secondary message also for 10s
                                    }
                                }
                            }, 10000); // Delay for the custom message to be visible

                        } else if (event.equals("show_loader")) {
                            // This is HyperSDK's internal loader, we are managing our own progressBarNb
                            Log.d("NB_SDK_EVENT", "HyperSDK show_loader event");
                        } else if (event.equals("hide_loader")) {
                            Log.d("NB_SDK_EVENT", "HyperSDK hide_loader event");
                        } else if (event.equals("log_stream")) {
                            Log.i("NB_Clickstream", data.toString());
                        } else if (event.equals("session_expired")) {
                            showCustomMessage(finalEventDataString, 10000);
                            showLoaderNb(false);
                        }
                    } catch (Exception e) {
                        Log.e("NB_SDK_EVENT_ERROR", "Error in onEvent: " + e.getMessage(), e);
                        final String errorMsg = "Error in SDK event handling: " + e.getMessage();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showCustomMessage(errorMsg, 10000);
                                showLoaderNb(false);
                            }
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e("NBActivity", "Error initializing SDK: " + e.getMessage(), e);
            final String errorMsg = "Error initializing SDK: " + e.getMessage();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCustomMessage(errorMsg, 10000);
                    showLoaderNb(false);
                }
            });
        }
    }

    private JSONObject createInitiationPayload(String customerId) {
        try {
            JSONObject initiationPayload = new JSONObject();
            initiationPayload.put("requestId", java.util.UUID.randomUUID().toString()); // Dynamic requestId
            initiationPayload.put("service", "in.juspay.ibmb");

            JSONObject payload = new JSONObject();
            payload.put("action", "initiate");
            payload.put("clientId", "axisbank"); // Replace with your actual clientId
            payload.put("environment", "sandbox"); // Or "production"
            payload.put("bankCustomerId", customerId); // Use passed customerId
            payload.put("passBankHeader", passBankHeaderCheckboxNb.isChecked());
            payload.put("hitPPIUrl", hitJuspayBackendCheckboxNb.isChecked());

            initiationPayload.put("payload", payload);
            return initiationPayload;
        } catch (Exception e) {
            Log.e("NBActivity", "Error creating initiation payload: " + e.getMessage());
            // Show error to user
            final String errorMsg = "Error creating initiation payload: " + e.getMessage();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showCustomMessage(errorMsg, 10000);
                }
            });
            return new JSONObject(); // Return empty or handle error appropriately
        }
    }

    private JSONObject createProcessPayload(JSONObject initiateResponse, String url, String customerId) throws Exception {
        JSONObject processPayload = new JSONObject();
        processPayload.put("requestId", java.util.UUID.randomUUID().toString()); // Dynamic requestId
        processPayload.put("service", "in.juspay.ibmb");

        JSONObject payload = new JSONObject();
        payload.put("action", "ibmbInitiateTxn");
        payload.put("timestamp", String.valueOf(System.currentTimeMillis())); // Dynamic timestamp
        payload.put("txnType", "IBMB_INTENT"); // Ensure this is correct for nb:// flow
        payload.put("url", url);
        payload.put("bankCustomerId", customerId); // Use passed customerId

        processPayload.put("payload", payload);
        return processPayload;
    }

    private void sendResultBack(String responseMessage) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("transaction_response", responseMessage);
        resultIntent.putExtra("transaction_status", "SUCCESS"); // Default to SUCCESS

        try {
            JSONObject jsonResponse = new JSONObject(responseMessage);
            String status = jsonResponse.optString("status", "UNKNOWN");
            resultIntent.putExtra("Status", status); // Consistent key
            resultIntent.putExtra("response", status); // Legacy key if needed

            // Update transaction_status based on actual SDK status if available and meaningful
            if ("SUCCESS".equalsIgnoreCase(status) || "CHARGED".equalsIgnoreCase(status)) {
                resultIntent.putExtra("transaction_status", "SUCCESS");
            } else if ("FAILURE".equalsIgnoreCase(status) || "PENDING".equalsIgnoreCase(status)) {
                resultIntent.putExtra("transaction_status", status.toUpperCase());
            }

            resultIntent.putExtra("txnId", jsonResponse.optString("txnId"));
            resultIntent.putExtra("txnRef", jsonResponse.optString("orgRefId"));
            resultIntent.putExtra("responseCode", jsonResponse.optString("txnStatusCode"));

        } catch (Exception e) {
            Log.e("NBActivity", "Error parsing JSON response for result: " + e.getMessage());
            resultIntent.putExtra("transaction_status", "ERROR_PARSING_RESPONSE");
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (hyperInstance != null) {
            hyperInstance.terminate();
        }
        if (customToastHandler != null && hideCustomToastRunnable != null) {
            customToastHandler.removeCallbacks(hideCustomToastRunnable);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (hyperInstance != null && hyperInstance.onBackPressed()) {
            // Handled by HyperSDK
        } else {
            super.onBackPressed();
        }
    }
}
