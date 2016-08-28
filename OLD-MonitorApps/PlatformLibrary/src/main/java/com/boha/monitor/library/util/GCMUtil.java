package com.boha.monitor.library.util;


/**
 * Created by aubreyM on 2014/05/11.
 */

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.boha.monitor.library.dto.GcmDeviceDTO;
import com.boha.monitor.library.dto.RequestDTO;
import com.boha.monitor.library.dto.ResponseDTO;
import com.boha.platform.library.R;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;

/**
 * Handle registration of device to Google Cloud Messaging
 */
public class GCMUtil {
    public interface GCMUtilListener {
        public void onDeviceRegistered(String id);

        public void onGCMError();
    }

    static Context ctx;
    static String registrationID, msg;
    static final String LOG = "GCMUtil";
    static GoogleCloudMessaging gcm;

public static final String TAG = GCMUtil.class.getSimpleName();
    /**
     * Start device registration to Google Cloud Messaging
     * Receive GCM registration string and complete GCM registration by calling back-end
     *
     * @param context
     * @param listener
     * @see com.boha.monitor.library.util.GCMUtil.GCMUtilListener
     */
    public static void startGCMRegistration(final Context context, final GCMUtilListener listener) {
        ctx = context;
        GCMTask task = new GCMTask(listener);
        task.execute();
    }


    private static class GCMTask extends AsyncTask<Void, Void, Integer> {

        private GCMUtilListener listener;

        public GCMTask(GCMUtilListener listener) {
            this.listener = listener;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            Log.e(TAG, "doInBackground: startGCMRegistration ... humming along" );
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(ctx);
                }
                registrationID = gcm.register(ctx.getString(R.string.gcm_sender_id));
                msg = "Device registered, registration ID = \n" + registrationID;
                SharedUtil.storeRegistrationId(ctx, registrationID);
                RequestDTO w = new RequestDTO();
                w.setRequestType(RequestDTO.SEND_GCM_REGISTRATION);
                w.setGcmRegistrationID(registrationID);
                NetUtil.sendRequest(ctx, w, new NetUtil.NetUtilListener() {
                    @Override
                    public void onResponse(final ResponseDTO response) {
                        if (response.getStatusCode() == 0) {
                            Log.w(LOG, "############ Device registered to Google on MONITOR PLATFORM server GCM regime");
                            GcmDeviceDTO gcmDevice = new GcmDeviceDTO();
                            gcmDevice.setManufacturer(Build.MANUFACTURER);
                            gcmDevice.setModel(Build.MODEL);
                            gcmDevice.setSerialNumber(Build.SERIAL);
                            gcmDevice.setAndroidVersion(Build.VERSION.RELEASE);
                            gcmDevice.setProduct(Build.PRODUCT);
                            gcmDevice.setApp(ctx.getPackageName());
                            gcmDevice.setRegistrationID(registrationID);
                            SharedUtil.saveGCMDevice(ctx, gcmDevice);

                        }
                    }

                    @Override
                    public void onError(final String message) {
                        Log.e(LOG, "############ Device failed to register on server GCM regime\n" + message);

                    }

                    @Override
                    public void onWebSocketClose() {
                        Log.d(LOG, "############## GCMUtil onWebSocketClose");
                    }
                });

                Log.i(LOG, msg);

            } catch (IOException e) {
                return 9;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer p) {
            if (p == 0) {
                listener.onDeviceRegistered(registrationID);
            }
        }
    }

}
