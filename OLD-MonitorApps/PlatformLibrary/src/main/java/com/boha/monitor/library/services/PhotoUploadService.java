package com.boha.monitor.library.services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.boha.monitor.library.dto.PhotoUploadDTO;
import com.boha.monitor.library.dto.ResponseDTO;
import com.boha.monitor.library.util.CDNUploader;
import com.boha.monitor.library.util.PhotoCacheUtil;
import com.boha.monitor.library.util.Util;
import com.boha.monitor.library.util.WebCheck;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the uploading of photos from a list held in cache. Uploads each photo
 * and notifies any activity bound to it on completion. Otherwise these cached photos
 * are uploaded in a silent process not visible to the user.
 * <p/>
 * It may be started by a startService call or may be bound to an activity via the
 * IBinder interface.
 * <p/>
 * Entry points: onHandleIntent, uploadCachedVideos
 */
public class PhotoUploadService extends IntentService {

    public PhotoUploadService() {
        super("PhotoUploadService");
    }

    public interface UploadListener {
        public void onUploadsComplete(List<PhotoUploadDTO> list);
    }

    UploadListener uploadListener;
    public static final String JSON_PHOTO = "photos.json", BROADCAST_PHOTO_UPLOADED = "BROADCAST_PHOTO_UPLOADED";

    public void uploadCachedPhotos(UploadListener listener) {
        uploadListener = listener;
        Log.d(TAG, "#### uploadCachedPhotos, getting cached photos - will start uploads if wifi is up");
        if (WebCheck.checkNetworkAvailability(getApplicationContext()).isNetworkUnavailable()) {
            Log.e(TAG, "--- No Network: boolean = isNetworkUnavailable");
            return;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ResponseDTO response = new ResponseDTO();
                response.setPhotoUploadList(new ArrayList<PhotoUploadDTO>());
                FileInputStream stream;
                try {
                    stream = getApplicationContext().openFileInput(JSON_PHOTO);
                    response = Util.getResponseData(stream);
                    list = response.getPhotoUploadList();

                    if (list.isEmpty()) {
                        Log.w(TAG, "--- no cached photos for upload");
                        if (uploadListener != null)
                            uploadListener.onUploadsComplete(new ArrayList<PhotoUploadDTO>());
                        return;
                    }
                    getLog(response);
                    int pending = 0;
                    for (PhotoUploadDTO x: list) {
                        if (x.getDateUploaded() == null) {
                            pending++;
                        }
                    }
                    if (pending == 0) {
                        if (uploadListener != null)
                            uploadListener.onUploadsComplete(new ArrayList<PhotoUploadDTO>());
                        return;
                    } else {
                        Log.e(TAG,"### ...pending photo uploads: " + pending);
                    }
                    index = 0;
                    controlUploads();
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "############# photo cache file not found. possibly virgin trip");

                } catch (IOException e) {
                    Log.e(TAG, "Failed", e);
                }
            }
        });
        thread.start();


    }

    private static void getLog(ResponseDTO cache) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Photos currently in the cache: ")
                .append(cache.getPhotoUploadList().size()).append(" - ");
        int up = 0, not = 0;
        for (PhotoUploadDTO p : cache.getPhotoUploadList()) {
            if (p.getDateUploaded() != null)
                up++;
            else
                not++;

        }
        sb.append("photos uploaded: " + up + " pending: " + not);
        Log.i(TAG, sb.toString());
    }


    List<PhotoUploadDTO> uploadedList = new ArrayList<>();


    @Override
    protected void onHandleIntent(Intent intent) {
        Log.w(TAG, "## PhotoUploadService onHandleIntent .... starting service");

        if (intent != null) {
            PhotoCacheUtil.getCachedPhotos(getApplicationContext(), new PhotoCacheUtil.PhotoCacheListener() {
                @Override
                public void onFileDataDeserialized(ResponseDTO response) {
                    uploadedList = new ArrayList<>();
                    list = response.getPhotoUploadList();
                    index = 0;
                    controlUploads();

                }

                @Override
                public void onDataCached(PhotoUploadDTO photo) {

                }

                @Override
                public void onError() {

                }
            });
        }



    }

    static List<PhotoUploadDTO> list;
    int index;

    private void controlUploads() {
        if (index < list.size()) {
            if (list.get(index).getDateUploaded() == null) {
                executeUpload(list.get(index));
            } else {
                index++;
                controlUploads();
            }

        } else {
            LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
            Intent m = new Intent(BROADCAST_PHOTO_UPLOADED);
            m.putExtra("uploaded", uploadedList.size());
            bm.sendBroadcastSync(m);
            Log.d(TAG,"#############.....................Broadcast about photo sent");
            if (uploadListener != null) {
                uploadListener.onUploadsComplete(uploadedList);
            }
        }

    }


    private void executeUpload(final PhotoUploadDTO dto) {
//        Log.d(TAG, "** executeUpload, projectID: " + dto.getProjectID());

        CDNUploader.uploadFile(getApplicationContext(), dto, new CDNUploader.CDNUploaderListener() {
            @Override
            public void onFileUploaded(PhotoUploadDTO photo) {
                Log.w(TAG, "onFileUploaded: photo size:  " + photo.getBytes() );
                uploadedList.add(dto);
                PhotoCacheUtil.updateUploadedPhoto(getApplicationContext(), dto);
                index++;
                controlUploads();
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, message);
                failedUploads.add(dto);
                index++;
                controlUploads();
            }
        });

    }


    List<PhotoUploadDTO> failedUploads = new ArrayList<>();
    static final String TAG = PhotoUploadService.class.getSimpleName();
    public class LocalBinder extends Binder {

        public PhotoUploadService getService() {
            return PhotoUploadService.this;
        }

    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private final IBinder mBinder = new LocalBinder();

}
