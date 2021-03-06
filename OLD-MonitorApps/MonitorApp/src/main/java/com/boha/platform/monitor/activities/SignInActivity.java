package com.boha.platform.monitor.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.boha.monitor.library.activities.ThemeSelectorActivity;
import com.boha.monitor.library.dto.GcmDeviceDTO;
import com.boha.monitor.library.dto.MonitorDTO;
import com.boha.monitor.library.dto.RequestDTO;
import com.boha.monitor.library.dto.ResponseDTO;
import com.boha.monitor.library.util.GCMUtil;
import com.boha.monitor.library.util.NetUtil;
import com.boha.monitor.library.util.SharedUtil;
import com.boha.monitor.library.util.Snappy;
import com.boha.monitor.library.util.Util;
import com.boha.platform.monitor.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.util.ArrayList;

import static com.boha.monitor.library.util.Util.showErrorToast;
import static com.boha.monitor.library.util.Util.showToast;

public class SignInActivity extends AppCompatActivity {

    Spinner spinnerEmail;
    TextView txtApp, txtEmail, label;
    EditText ePin, editEmail;
    Button btnSave;
    Context ctx;
    String email;
    ImageView banner;

    GcmDeviceDTO gcmDevice;
    static final String LOG = SignInActivity.class.getSimpleName();
    Activity activity;
    boolean gcmOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_in);
        ctx = getApplicationContext();
        activity = this;
        banner = (ImageView)findViewById(R.id.SI_banner);

        setFields();
        banner.setImageDrawable(Util.getRandomBackgroundImage(ctx));
    }
    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG, "#################### onResume");
        checkPermission();
        checkVirgin();
    }
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_ACCESS_FINE_LOCATION);

            }
        }

    }
    static final int MY_PERMISSIONS_ACCESS_FINE_LOCATION = 77;
    static final int MY_PERMISSIONS_GET_ACCOUNTS = 75;
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.w(LOG,"ACCESS_FINE_LOCATION permission granted");

                } else {
                    Log.e(LOG,"ACCESS_FINE_LOCATION permission denied");

                }
                return;
            }
            case MY_PERMISSIONS_GET_ACCOUNTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.w(LOG,"GET_ACCOUNTS permission granted");

                } else {
                    Log.e(LOG,"GET_ACCOUNTS permission denied");

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    private void checkVirgin() {
        boolean force = getIntent().getBooleanExtra("force",false);
        if (force) {
            registerGCMDevice();
            return;
        }
        MonitorDTO dto = SharedUtil.getMonitor(ctx);
        if (dto != null) {
            Log.i(LOG, "++++++++ Not a virgin anymore? yes ...checking GCM registration....");
            String id = SharedUtil.getRegistrationId(getApplicationContext());
            if (id == null) {
                gcmOnly = true;
                registerGCMDevice();
            }

            Intent intent = new Intent(ctx, MonitorAppDrawerActivity.class);
            startActivity(intent);
            //
            finish();
            return;
        }

        registerGCMDevice();
    }

    private void registerGCMDevice() {
        gcmDevice = new GcmDeviceDTO();
        gcmDevice.setManufacturer(Build.MANUFACTURER);
        gcmDevice.setModel(Build.MODEL);
        gcmDevice.setSerialNumber(Build.SERIAL);
        gcmDevice.setAndroidVersion(Build.VERSION.RELEASE);
        gcmDevice.setProduct(Build.PRODUCT);
        gcmDevice.setApp(ctx.getPackageName());

        Snackbar.make(btnSave, "Just a second, checking services ...",Snackbar.LENGTH_LONG)
                .setAction("CLOSE", null)
                .show();
        boolean ok = checkPlayServices();
        setBusyIndicator(true);
        if (ok) {
            Log.e(LOG, "############# Starting Google Cloud Messaging registration");
            GCMUtil.startGCMRegistration(getApplicationContext(), new GCMUtil.GCMUtilListener() {
                @Override
                public void onDeviceRegistered(String id) {
                    Log.i(LOG, "############# GCM - we cool, GcmDeviceDTO waiting to be sent with signin .....: " + id);
                    setBusyIndicator(false);
                    gcmDevice.setRegistrationID(id);
                    btnSave.setEnabled(true);
                    if (gcmOnly) {
                        RequestDTO w = new RequestDTO(RequestDTO.UPDATE_MONITOR_DEVICE);
                        MonitorDTO mon = SharedUtil.getMonitor(ctx);
                        gcmDevice.setMonitor(mon);
                        w.setGcmDevice(gcmDevice);
                        //update monitor device on server
                        NetUtil.sendRequest(ctx, w, new NetUtil.NetUtilListener() {
                            @Override
                            public void onResponse(ResponseDTO response) {
                                gcmOnly = false;
                                if (response.getGcmDeviceList() != null) {
                                    if (!response.getGcmDeviceList().isEmpty()) {
                                        SharedUtil.saveGCMDevice(ctx,response.getGcmDeviceList().get(0));
                                    }
                                }
                            }

                            @Override
                            public void onError(String message) {

                            }

                            @Override
                            public void onWebSocketClose() {

                            }
                        });
                    }


                }

                @Override
                public void onGCMError() {
                    Log.e(LOG, "############# onGCMError --- we got GCM problems");
                    setBusyIndicator(false);
                    btnSave.setEnabled(true);

                }
            });
        }
    }
    private void sendSignIn() {
        if (ePin.getText().toString().isEmpty()) {
            showErrorToast(ctx, "Enter PIN");
            return;
        }
        if (email == null) {
            if (editEmail.getText().toString().isEmpty()) {
                showErrorToast(ctx, getString(R.string.select_account));
                return;
            } else {
                email = editEmail.getText().toString();
            }
        }

        RequestDTO r = new RequestDTO();
        r.setRequestType(RequestDTO.LOGIN_MONITOR);
        r.setEmail(email);
        r.setPin(ePin.getText().toString());
        r.setGcmDevice(gcmDevice);

       setBusyIndicator(true);
        NetUtil.sendRequest(ctx,r,new NetUtil.NetUtilListener() {
            @Override
            public void onResponse(final ResponseDTO response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBusyIndicator(false);
                        if (response.getStatusCode() > 0) {
                            showErrorToast(ctx, response.getMessage());
                            return;
                        }

                        SharedUtil.saveCompany(ctx, response.getCompany());
                        SharedUtil.saveMonitor(ctx, response.getMonitor());
                        SharedUtil.saveGCMDevice(ctx, response.getGcmDeviceList().get(0));
                        if (!response.getPhotoUploadList().isEmpty()) {
                            SharedUtil.savePhoto(ctx, response.getPhotoUploadList().get(0));
                        }

                        Snappy.saveData(response, ctx, new Snappy.SnappyWriteListener() {
                            @Override
                            public void onDataWritten() {
                                Intent intent = new Intent(ctx, ThemeSelectorActivity.class);
                                startActivity(intent);
                            }

                            @Override
                            public void onError(String message) {

                            }
                        });


                    }
                });
            }

            @Override
            public void onError(final String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBusyIndicator(false);
                        showErrorToast(ctx, message);
                        btnSave.setEnabled(true);
                    }
                });
            }

            @Override
            public void onWebSocketClose() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                    }
                });
            }
        });



    }
    private void setFields() {
        ePin = (EditText) findViewById(R.id.SI_pin);
        editEmail = (EditText) findViewById(R.id.SI_editEmail);
        txtEmail = (TextView) findViewById(R.id.SI_txtEmail);
        label = (TextView) findViewById(R.id.SI_welcome);
        txtApp = (TextView)findViewById(R.id.SI_app);
        btnSave = (Button)findViewById(R.id.btnRed);
        txtApp.setText(R.string.monitor);
        btnSave.setText("Sign In");
        btnSave.setEnabled(false);

        txtEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.flashOnce(txtEmail,300, new Util.UtilAnimationListener() {
                    @Override
                    public void onAnimationEnded() {
                        Util.showPopupBasicWithHeroImage(ctx, activity, tarList,
                                label, getString(R.string.select_email),
                                new Util.UtilPopupListener() {
                            @Override
                            public void onItemSelected(int index) {
                                if (index == 0) {
                                    email = null;
                                } else {
                                    email = tarList.get(index);
                                    txtEmail.setText(email);
                                    btnSave.setEnabled(true);
                                }
                            }
                        });
                    }
                });
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Util.flashOnce(btnSave, 100, new Util.UtilAnimationListener() {
                    @Override
                    public void onAnimationEnded() {
                        sendSignIn();
                        btnSave.setEnabled(false);
                    }
                });
            }
        });
    }


    private boolean checkPlayServices() {
        Log.w(LOG, "checking GooglePlayServices .................");
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(ctx);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.gms")));
                return false;
            } else {
                Log.i(LOG, "This device is not supported.");
                throw new UnsupportedOperationException("GooglePlayServicesUtil resultCode: " + resultCode);
            }
        }
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.signin, menu);
        mMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_help) {
            showToast(ctx, getString(R.string.under_cons));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }



    ArrayList<String> tarList = new ArrayList<String>();
    Menu mMenu;
    public void setBusyIndicator(final boolean refreshing) {
        if (mMenu != null) {
            final MenuItem refreshItem = mMenu.findItem(R.id.action_help);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.action_bar_progess);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }
    @Override
    public void onPause() {
        super.onPause();
    }

}
