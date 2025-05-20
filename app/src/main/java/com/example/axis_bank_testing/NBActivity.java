package com.example.axis_bank_testing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.FragmentActivity;
import in.juspay.services.HyperServices;
import in.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter;
import in.juspay.hypersdk.data.JuspayResponseHandler;
import org.json.JSONObject;

public class NBActivity extends FragmentActivity {

    private HyperServices hyperInstance;
    private TextView statusTextView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nb);

        // Find the TextView to update status
        statusTextView = findViewById(R.id.nb_textview);
        // Find the ProgressBar
        progressBar = findViewById(R.id.nb_progressbar);

        // Show the loader initially
        progressBar.setVisibility(View.VISIBLE);
        statusTextView.setText("Initializing payment process...");

        // Check for intent data
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null) {
            String uriData = data.toString();
            Log.d("NBActivity", "Received Intent with URI: " + uriData);
            initializeAndProcess(uriData);
        } else {
            Bundle extras = intent.getExtras();
            if (extras != null && extras.containsKey("payment_url")) {
                String paymentUrl = extras.getString("payment_url");
                statusTextView.setText("Processing payment URL from extras");
                initializeAndProcess(paymentUrl);
            } else {
                Log.e("NBActivity", "No intent data received");
                statusTextView.setText("Error: No intent data received");
                // Hide loader on error
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    private void initializeAndProcess(String uriData) {
        try {
            ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
            hyperInstance = new HyperServices(this, rootView);

            JSONObject initiationPayload = createInitiationPayload();

            hyperInstance.initiate(initiationPayload, new HyperPaymentsCallbackAdapter() {
                @Override
                public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                    try {
                        String event = data.getString("event");
                        Log.d("SDK Event", "Received event: " + event);

                        if (event.equals("initiate_result")) {
                            // When initiation completes, create and send process payload.
                            runOnUiThread(() -> statusTextView.setText("Initiation complete, processing transaction..."));
                            if (hyperInstance.isInitialised()) {
                                JSONObject processPayload = createProcessPayload(null, uriData);
                                hyperInstance.process(processPayload);
                            }
                        } else if (event.equals("process_result")) {
                            // When process completes, but don't show the JSON response
                            JSONObject response = data.optJSONObject("payload");
                            if (response != null) {
                                // For debugging, still log the response
                                Log.d("SDK Event", "Process result received: " + response.toString());

                                // But just show a simple completion message to the user
                                runOnUiThread(() -> {
                                    statusTextView.setText("Transaction completed successfully");
                                    // Hide loader when processing is done
                                    progressBar.setVisibility(View.GONE);
                                });

                                // Continue with sending back the result
                                sendResultBack(response.toString());
                            } else {
                                Log.d("SDK Event", "Empty process result received.");
                                runOnUiThread(() -> {
                                    statusTextView.setText("Process completed");
                                    // Hide loader when processing is done
                                    progressBar.setVisibility(View.GONE);
                                });
                            }
                        } else if (event.equals("show_loader")) {
                            // Show our custom loader
                            runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
                        } else if (event.equals("hide_loader")) {
                            // Hide our custom loader
                            runOnUiThread(() -> progressBar.setVisibility(View.GONE));
                        } else if (event.equals("log_stream")) {
                            Log.i("=>Clickstream", data.toString());
                        } else if (event.equals("session_expired")) {
                            Log.w("SDK Event", "Session expired.");
                            runOnUiThread(() -> {
                                statusTextView.setText("Session expired");
                                // Hide loader on session expired
                                progressBar.setVisibility(View.GONE);
                            });
                        }
                    } catch (Exception e) {
                        Log.e("SDK", "Error in event handling: " + e.getMessage());
                        runOnUiThread(() -> {
                            statusTextView.setText("Error handling event: " + e.getMessage());
                            // Hide loader on error
                            progressBar.setVisibility(View.GONE);
                        });
                    }
                }
            });
        } catch (Exception e) {
            Log.e("NBActivity", "Error initializing SDK: " + e.getMessage(), e);
            statusTextView.setText("Error initializing SDK: " + e.getMessage());
            // Hide loader on error
            progressBar.setVisibility(View.GONE);
        }
    }

    private JSONObject createInitiationPayload() {
        try {
            JSONObject initiationPayload = new JSONObject();
            initiationPayload.put("requestId", "04a90298-4b2d-4de8-8e49-3c80fe003b39");
            initiationPayload.put("service", "in.juspay.ibmb");

            JSONObject payload = new JSONObject();
            payload.put("action", "initiate");
            payload.put("clientId", "axisbank");
            payload.put("environment", "sandbox");
            payload.put("customerLoginId", "xcvrrtdda");

            initiationPayload.put("payload", payload);
            return initiationPayload;
        } catch (Exception e) {
            Log.e("NBActivity", "Error creating initiation payload: " + e.getMessage());
            return new JSONObject();
        }
    }

    private JSONObject createProcessPayload(JSONObject response, String url) throws Exception {
        JSONObject processPayload = new JSONObject();
        processPayload.put("requestId", "04a90298-4b2d-4de8-8e49-3c80fe003b39");
        processPayload.put("service", "in.juspay.ibmb");

        JSONObject payload = new JSONObject();
        payload.put("action", "ibmbInitiateTxn");
        payload.put("timestamp", "1734342757");
        payload.put("txnType", "IBMB_INTENT");
        payload.put("url", url);
        payload.put("customerLoginId", "18mi7nu6by5tv4r");

        processPayload.put("payload", payload);
        return processPayload;
    }

    /**
     * Sends the successful response back to the IntentTrigger app.
     *
     * @param responseMessage The transaction response from the SDK.
     */
    private void sendResultBack(String responseMessage) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("transaction_response", responseMessage);

        // Change from COMPLETED to SUCCESS as requested
        resultIntent.putExtra("transaction_status", "SUCCESS");

        try {
            JSONObject jsonResponse = new JSONObject(responseMessage);
            if (jsonResponse.has("status")) {
                String status = jsonResponse.getString("status");
                resultIntent.putExtra("Status", status);
                resultIntent.putExtra("response", status);
            }

            if (jsonResponse.has("txnId"))
                resultIntent.putExtra("txnId", jsonResponse.getString("txnId"));

            if (jsonResponse.has("orgRefId"))
                resultIntent.putExtra("txnRef", jsonResponse.getString("orgRefId"));

            if (jsonResponse.has("txnStatusCode"))
                resultIntent.putExtra("responseCode", jsonResponse.getString("txnStatusCode"));
        } catch (Exception e) {
            Log.e("NBActivity", "Error parsing JSON response: " + e.getMessage());
        }

        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (hyperInstance != null) {
            hyperInstance.terminate();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        boolean handleBackpress = hyperInstance.onBackPressed();
        if (!handleBackpress) {
            super.onBackPressed();
        }
    }
}
