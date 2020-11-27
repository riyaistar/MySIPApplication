package com.example.mysipapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.sip.SipException;
import android.net.sip.SipManager;
import android.net.sip.SipProfile;
import android.net.sip.SipRegistrationListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 200;
    public static String[] PERMISSIONS = {Manifest.permission.USE_SIP};

    @BindView(R.id.username)
    EditText edit_username;
    @BindView(R.id.domain)
    EditText edit_domain;
    @BindView(R.id.password)
    EditText edit_password;
    @BindView(R.id.proxy_url)
    EditText edit_proxy_url;
    @BindView(R.id.register)
    Button register;
    @BindView(R.id.status)
    TextView status;
    private static final String TAG = "MainActivity";

    public SipManager manager = null;
    public SipProfile sipprofile = null;

    public String username, domain, password;
    public Boolean registered=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        requestAllpermission();
        initializeManager();
    }
    public boolean requestAllpermission() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : PERMISSIONS) {
            result = ContextCompat.checkSelfPermission(getApplicationContext(), p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        initializeManager();
    }

    public void initializeManager() {
        if(manager == null) {
            manager = SipManager.newInstance(this);
        }
    }

    @OnClick(R.id.register)
    public void registerSip() {

            if (manager == null) {
                return;
            }

            if (sipprofile != null) {
                closeLocalProfile();
            }


            String username = edit_username.getText().toString();
            String domain = edit_domain.getText().toString();
            String password = edit_password.getText().toString();
            String proxy_url = edit_proxy_url.getText().toString();

            if (username.length() == 0 || domain.length() == 0 || password.length() == 0 || proxy_url.length() == 0) {
                Toast.makeText(this, "Update SIp Settings", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                SipProfile.Builder builder = new SipProfile.Builder(username, domain);
                builder.setPassword(password);
                builder.setOutboundProxy(proxy_url);
                sipprofile = builder.build();

                Intent i = new Intent();
                i.setAction("android.SipDemo.INCOMING_CALL");
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);
                manager.open(sipprofile, pi, null);


                // This listener must be added AFTER manager.open is called,
                // Otherwise the methods aren't guaranteed to fire.

                manager.setRegistrationListener(sipprofile.getUriString(), new SipRegistrationListener() {
                    public void onRegistering(String localProfileUri) {
                        updateStatus("Registering with SIP Server...");
                        registered = false;
                    }

                    public void onRegistrationDone(String localProfileUri, long expiryTime) {
                        updateStatus("Ready");
                        registered = true;
                    }

                    public void onRegistrationFailed(String localProfileUri, int errorCode,
                                                     String errorMessage) {
                        updateStatus("Registration failed.  Please check settings. " + errorCode + " " +errorMessage);
                        registered = false;
                    }
                });
            } catch (ParseException pe) {
                updateStatus("Connection Error.");
            } catch (SipException se) {
                updateStatus("Connection error.");
            }

    }

    private void updateStatus(String s) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        this.runOnUiThread(new Runnable() {
            public void run() {
                TextView labelView = (TextView) findViewById(R.id.status);
                labelView.setText(s);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeLocalProfile();
    }

    public Boolean closeLocalProfile() {
        Log.e("Closing profile", "closing profile " + sipprofile.getUriString());


        if (manager == null || !registered) {
            return false;
        }
        try {
            if (sipprofile != null) {
                Log.e("Unregistering profile", "Un registering profile ");
                manager.unregister(sipprofile, null);
                manager.close(sipprofile.getUriString());
            }
        } catch (Exception ee) {
            Log.d(TAG, "Failed to close local profile.", ee);
        }
        return false;
    }
}