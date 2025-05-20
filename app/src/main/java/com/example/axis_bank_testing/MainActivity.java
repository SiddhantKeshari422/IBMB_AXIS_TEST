//package com.example.axis_bank_testing;
//
//import android.os.Bundle;
//import android.util.Log;
//import android.widget.Button;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import org.json.JSONObject;
//import in.juspay.services.HyperServices;
//import in.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter;
//import in.juspay.hypersdk.data.JuspayResponseHandler;
//
//public class MainActivity extends AppCompatActivity {
//
//    private HyperServices hyperInstance;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        // Handle system UI insets (padding for system bars)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        Button button = findViewById(R.id.supabutton);
//        button.setOnClickListener(v -> {
//            Log.d("BUTTONS", "User tapped the Supabutton");
//            initiateSdkOperation();
//        });
//    }
//
//    private void initiateSdkOperation() {
//        try {
//            JSONObject initiationPayload = new JSONObject();
//            initiationPayload.put("requestId", "04a90298-4b2d-4de8-8e49-3c80fe003b39");
//            initiationPayload.put("service", "in.juspay.ibmb");
//
//            JSONObject payload = new JSONObject();
//            payload.put("action", "initiate");
//            payload.put("clientId", "axisbank");
//            payload.put("environment", "sandbox");
//            payload.put("customerLoginId", "xcvrrtdda");
//
//            initiationPayload.put("payload", payload);
//
//            // Initialize HyperServices instance
//            hyperInstance = new HyperServices(this, findViewById(R.id.main_layout));
//
//            hyperInstance.initiate(initiationPayload, new HyperPaymentsCallbackAdapter() {
//                @Override
//                public void onEvent(JSONObject data, JuspayResponseHandler handler) {
//                    try {
//                        String event = data.getString("event");
//
//                        if (event.equals("show_loader")) {
//
//                        } else if (event.equals("hide_loader")) {
//                            Log.d("SDK Event", "Hiding loader...");
//                        } else if (event.equals("initiate_result")) {
//                            JSONObject response = data.optJSONObject("payload");
//                            if (hyperInstance.isInitialised()) {
//                                JSONObject processPayload = createProcessPayload(response);
//                                hyperInstance.process(processPayload);
//                            }
//                        } else if (event.equals("process_result")) {
//                            JSONObject response = data.optJSONObject("payload");
//                            if (response != null) {
//                                Log.d("SDK Event", "Process result received: " + response.toString());
//                            } else {
//                                Log.d("SDK Event", "Empty process result received.");
//                            }
//                        } else if (event.equals("log_stream")) {
//                            Log.i("=>Clickstream", data.toString());
//                        } else if (event.equals("session_expired")) {
//                            Log.w("SDK Event", "Session expired.");
//                        }
//                    } catch (Exception e) {
//                        Log.e("SDK", "Error in event handling: " + e.getMessage());
//                    }
//                }
//            });
//        } catch (Exception e) {
//            Log.e("SDK", "Error creating initiation payload: " + e.getMessage());
//        }
//    }
//
//    private JSONObject createProcessPayload(JSONObject response) throws Exception {
//        JSONObject processPayload = new JSONObject();
//
//        processPayload.put("requestId", "04a90298-4b2d-4de8-8e49-3c80fe003b39");
//        processPayload.put("service", "in.juspay.ibmb");
//
//        JSONObject payload = new JSONObject();
//        payload.put("action", "ibmbInitiateTxn");
//        payload.put("timestamp", "1734342757");
//        payload.put("txnType", "IBMB_SCANPAY");
//        payload.put("url", "nb://pay?pn=Bhavneet&pa=000000000011@UTIB0000007&am=300");
//        payload.put("customerLoginId", "18mi7nu6by5tv4r");
//
//        processPayload.put("payload", payload);
//
//        return processPayload;
//    }
//}
//
//

package com.example.axis_bank_testing;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONObject;
import in.juspay.services.HyperServices;
import in.juspay.hypersdk.ui.HyperPaymentsCallbackAdapter;
import in.juspay.hypersdk.data.JuspayResponseHandler;

public class MainActivity extends AppCompatActivity {

    private HyperServices hyperInstance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Handle system UI insets (padding for system bars)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button button = findViewById(R.id.supabutton);
        button.setOnClickListener(v -> {
            Log.d("BUTTONS", "User tapped the Supabutton");
            initiateSdkOperation();
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (!hyperInstance.onBackPressed()) {
            super.getOnBackPressedDispatcher();
        }

    }

    private void initiateSdkOperation() {
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

            // Initialize HyperServices instance
            hyperInstance = new HyperServices(this, findViewById(R.id.main_layout));

            hyperInstance.initiate(initiationPayload, new HyperPaymentsCallbackAdapter() {
                @Override
                public void onEvent(JSONObject data, JuspayResponseHandler handler) {
                    try {
                        Log.e("localsdk", data.toString());
                        String event = data.getString("event");

                        if (event.equals("show_loader")) {
                            Log.d("SDK Event", "Showing loader...");

                        } else if (event.equals("hide_loader")) {
                            Log.d("SDK Event", "Hiding loader...");
                        }
                        else if (event.equals("initiate_result")) {
                            JSONObject response = data.optJSONObject("payload");
                            if (hyperInstance.isInitialised()) {
                                JSONObject processPayload = createProcessPayload(response);
                                hyperInstance.process(processPayload);
                            }
                        } else if (event.equals("process_result")) {
                            JSONObject response = data.optJSONObject("payload");
                            if (response != null) {
                                Log.d("SDK Event", "Process result received: " + response.toString());
//                                launchNbIntent();  // Launch Intent after process completion
                            } else {
                                Log.d("SDK Event", "Empty process result received.");
                            }
                        } else if (event.equals("log_stream")) {
                            Log.i("=>Clickstream", data.toString());
                        } else if (event.equals("session_expired")) {
                            Log.w("SDK Event", "Session expired.");
                        }
                    } catch (Exception e) {
                        Log.e("SDK", "Error in event handling: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e("SDK", "Error creating initiation payload: " + e.getMessage());
        }
    }

    private JSONObject createProcessPayload(JSONObject response) throws Exception {
        JSONObject processPayload = new JSONObject();

        processPayload.put("requestId", "04a90298-4b2d-4de8-8e49-3c80fe003b39");
        processPayload.put("service", "in.juspay.ibmb");

        JSONObject payload = new JSONObject();
        payload.put("action", "ibmbInitiateTxn");
        payload.put("timestamp", "1734342757");
        payload.put("txnType", "IBMB_SCANPAY");
        payload.put("url", "");
        payload.put("customerLoginId", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9");

        processPayload.put("payload", payload);

        return processPayload;
    }
}