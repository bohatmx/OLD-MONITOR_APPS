package com.boha.monitor.library.services;

import android.Manifest;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.boha.monitor.library.dto.LocationTrackerDTO;
import com.boha.monitor.library.dto.MonitorDTO;
import com.boha.monitor.library.dto.RequestDTO;
import com.boha.monitor.library.dto.ResponseDTO;
import com.boha.monitor.library.dto.SimpleMessageDTO;
import com.boha.monitor.library.dto.SimpleMessageDestinationDTO;
import com.boha.monitor.library.dto.StaffDTO;
import com.boha.monitor.library.util.NetUtil;
import com.boha.monitor.library.util.SharedUtil;
import com.boha.monitor.library.util.WebCheck;
import com.boha.platform.library.R;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class GPSLocationService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    public GPSLocationService() {
    }

    Location mLocation;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    boolean mRequestingLocationUpdates;
    List<LocationTrackerDTO> locationTrackerList;
    SimpleMessageDTO simpleMessage;
    static final float ACCURACY_THRESHOLD = 20;
    static final String LOG = GPSLocationService.class.getSimpleName();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(LOG, "onStartCommand - connect mGoogleApiClient");
        if (intent != null) {
            simpleMessage = (SimpleMessageDTO) intent.getSerializableExtra("simpleMessage");
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Awareness.API)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        } else {
            if (mGoogleApiClient.isConnected()) {
                startLocationScan();
            }
        }


        return Service.START_STICKY;
    }

    public static final String TAG = GPSLocationService.class.getSimpleName();

    @Override
    public void onConnected(Bundle bundle) {
        Log.w(LOG, "GPSLocationService onConnected: ");
        startLocationScan();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private void startLocationScan() {
        Log.w(LOG, "startLocationScan");

        if (mGoogleApiClient.isConnected()) {
            mRequestingLocationUpdates = true;
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            Log.w(LOG, "###### startLocationUpdates: " + new Date().toString());
            if (mGoogleApiClient.isConnected()) {
                Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                        .setResultCallback(new ResultCallback<LocationResult>() {
                            @Override
                            public void onResult(@NonNull LocationResult locationResult) {
                                if (!locationResult.getStatus().isSuccess()) {
                                    Log.e(TAG, "###########Could not get location.");
                                } else {
                                    Log.w(TAG, "onResult: location found, accuracy: " + locationResult.getLocation().getAccuracy() );
                                    mLocation = locationResult.getLocation();
                                    if (simpleMessage == null) {
                                        sendLocationTrack();
                                    } else {
                                        sendLocationResponse();
                                    }
                                }
                            }
                        });
            }
        }
    }

    private void sendLocationTrack() {
        Log.d(LOG, "sendLocationTrack, accuracy: " + mLocation.getAccuracy());
        final LocationTrackerDTO dto = new LocationTrackerDTO();
        StaffDTO s = SharedUtil.getCompanyStaff(
                getApplicationContext());
        if (s != null)
            dto.setStaffID(s.getStaffID());
        MonitorDTO m = SharedUtil.getMonitor(
                getApplicationContext());
        if (m != null)
            dto.setMonitorID(m.getMonitorID());
        dto.setAccuracy(mLocation.getAccuracy());
        dto.setDateTracked(new Date().getTime());
        dto.setLatitude(mLocation.getLatitude());
        dto.setLongitude(mLocation.getLongitude());
        dto.setGcmDevice(SharedUtil.getGCMDevice(getApplicationContext()));
        if (dto.getGcmDevice().getGcmDeviceID() == null) {
            Log.d(LOG, "GCMDeviceID is NULL, quitting sendLocationTrack");
            return;
        }
        try {
            dto.setGeocodedAddress(getAddress());
            if (dto.getGeocodedAddress() == null) {
                dto.setGeocodedAddress(getString(R.string.no_address));
            }
        } catch (Exception e) {
            dto.setGeocodedAddress(getString(R.string.no_address));
        }

        RequestDTO w = new RequestDTO(RequestDTO.ADD_LOCATION_TRACK);
        w.setLocationTracker(dto);
        w.setZipResponse(false);
        NetUtil.sendRequest(getApplicationContext(), w, new NetUtil.NetUtilListener() {
            @Override
            public void onResponse(ResponseDTO response) {

            }

            @Override
            public void onError(String message) {
                Log.e(LOG, message);
            }

            @Override
            public void onWebSocketClose() {
            }
        });


    }

    private void sendLocationResponse() {
        final SimpleMessageDTO sm = new SimpleMessageDTO();
        sm.setSimpleMessageDestinationList(new ArrayList<SimpleMessageDestinationDTO>());
        final LocationTrackerDTO dto = new LocationTrackerDTO();
        StaffDTO s = SharedUtil.getCompanyStaff(
                getApplicationContext());
        if (s != null) {
            dto.setStaffID(s.getStaffID());
            dto.setStaffName(s.getFullName());
            sm.setStaffName(s.getFullName());
        }
        MonitorDTO m = SharedUtil.getMonitor(
                getApplicationContext());
        if (m != null) {
            dto.setMonitorID(m.getMonitorID());
            dto.setMonitorName(m.getFullName());
            sm.setMonitorName(m.getFullName());
        }
        dto.setAccuracy(mLocation.getAccuracy());
        dto.setDateTracked(new Date().getTime());
        dto.setLatitude(mLocation.getLatitude());
        dto.setLongitude(mLocation.getLongitude());
        dto.setGcmDevice(SharedUtil.getGCMDevice(getApplicationContext()));
        try {
            dto.setGeocodedAddress(getAddress());
        } catch (Exception e) {
            Log.w(LOG, "Geocoding not available");
        }

        sm.setLocationTracker(dto);

        if (simpleMessage.getStaffID() != null) {
            SimpleMessageDestinationDTO dest = new SimpleMessageDestinationDTO();
            dest.setStaffID(simpleMessage.getStaffID());
            sm.getSimpleMessageDestinationList().add(dest);
        }
        if (simpleMessage.getMonitorID() != null) {
            SimpleMessageDestinationDTO dest = new SimpleMessageDestinationDTO();
            dest.setMonitorID(simpleMessage.getMonitorID());
            sm.getSimpleMessageDestinationList().add(dest);
        }

        RequestDTO w = new RequestDTO(RequestDTO.SEND_SIMPLE_MESSAGE);
        w.setSimpleMessage(sm);
        Log.d(LOG, "Sending location request response...");
        NetUtil.sendRequest(getApplicationContext(), w, new NetUtil.NetUtilListener() {
            @Override
            public void onResponse(ResponseDTO response) {
                Log.d(LOG, "location response sent to requestor");
                simpleMessage = null;
            }

            @Override
            public void onError(String message) {
                Log.e(LOG, "location response error: " + message);
                simpleMessage = null;
            }

            @Override
            public void onWebSocketClose() {

            }
        });


    }

    private String getAddress() {

        if (WebCheck.checkNetworkAvailability(getApplicationContext()).isNetworkUnavailable()) {
            return "Address not available";
        }
        Log.d(LOG, "getAddress ...");
        Geocoder geocoder = new Geocoder(getApplicationContext(),
                Locale.getDefault());
        try {
            List<Address> list = geocoder.getFromLocation(
                    mLocation.getLatitude(), mLocation.getLongitude(), 1);
            if (list == null || list.isEmpty()) {
                return getApplicationContext().getString(R.string.no_address);
            }
            Address address = list.get(0);
            int cnt = address.getMaxAddressLineIndex();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < cnt; i++) {
                sb.append(address.getAddressLine(i));
                if (i < (cnt - 1)) {
                    sb.append(", ");
                }
            }
            return sb.toString();

        } catch (IOException e) {
            Log.e(LOG, "Failed", e);
            return getString(R.string.no_address);
        } catch (IllegalArgumentException e) {
            Log.e(LOG, "Failed", e);
            return getString(R.string.no_address);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(LOG, "onConnectionFailed: " + connectionResult.toString());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
