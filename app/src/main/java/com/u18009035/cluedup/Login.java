package com.u18009035.cluedup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Login extends AppCompatActivity {
    public static final String wheatleyPass = "dTE4MDA5MDM1OkcxMHZAbm4x";
    public static final String PREFS_NAME = "session";
    private static final String PREF_API = "api";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //Force Portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_login);

        //Retrieve saved API from SharedPreferences
        SharedPreferences pref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String api = pref.getString(PREF_API, null);

        //Check if user has already logged in then go directly to Discover
        if (api != null) {
            Intent Discover = new Intent(this, com.u18009035.cluedup.Discover.class);
            startActivity(Discover);
        }

        //Add signin button action
        Button btn = findViewById(R.id.btnSignIn);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText emailField = findViewById(R.id.edtEmail);
                String email = emailField.getText().toString();

                EditText passwordField = findViewById(R.id.edtPassword);
                String pass = passwordField.getText().toString();

                //make the php API call
                LoginAPICall(email, pass);
            }
        });

        //Redirect signup to website registration
        TextView signup = findViewById(R.id.txtSignup);
        signup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uri uri = Uri.parse("http://wheatley.cs.up.ac.za/u18009035/assignment4/signup.php");
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

    }

    //Helper function to convert a stream to a string
    public static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        String s = sb.toString();
        return s;
    }

    //Take action on PHP json response for login
    private void validateLogin(String ApiReturn) {
        try {
            JSONObject jsonRes = new JSONObject(ApiReturn);
            String status = jsonRes.getString("status");
            Log.i("LOGIN", status);
            if (status.equals("success")) {
                //LOGIN
                //Set Api key in SharedPreferences
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().putString(PREF_API, jsonRes.getString("data")).commit();

                //Start discover activity
                Intent Discover = new Intent(this, com.u18009035.cluedup.Discover.class);
                startActivity(Discover);
            } else {
                //invalid credentials
                Login.this.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Incorrect email / password", Toast.LENGTH_LONG).show();
                    }
                });
            }

        } catch(JSONException e) {
            //invalid json return
            Login.this.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(getApplicationContext(), "Couldn't communicate with server", Toast.LENGTH_LONG).show();
                }
            });
        }

    }

    private void LoginAPICall(final String uEmail, final String uPassword) {
        Thread thread = new Thread(new Runnable() {@Override
        public void run() {
            try {
                URL url = new URL("http://@wheatley.cs.up.ac.za/u18009035/api.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                //Authenticate wheatley
                String basicAuth = "Basic " + wheatleyPass;
                conn.setRequestProperty("Authorization", basicAuth);

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                //Create JSON request
                JSONObject jsonRequest = new JSONObject();
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("type", "login");
                jsonParam.put("uEmail", uEmail);
                jsonParam.put("uPassword", uPassword);
                jsonRequest.put("request", jsonParam);

                DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                os.writeBytes(jsonRequest.toString());

                os.flush();
                os.close();

                Log.i("STATUS", String.valueOf(conn.getResponseCode()));
                Log.i("MSG", conn.getResponseMessage());

                //Send JSON response to validateLogin
                validateLogin(convertStreamToString(conn.getInputStream()));

                conn.disconnect();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        });

        thread.start();
    }

}