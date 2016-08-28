package com.boha.monitor.library.util;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.boha.monitor.library.activities.MonApp;
import com.boha.monitor.library.dto.ProjectDTO;
import com.boha.monitor.library.dto.ResponseDTO;
import com.boha.monitor.library.dto.SimpleMessageDTO;
import com.google.gson.Gson;
import com.snappydb.DB;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aubreymalabie on 7/13/16.
 */

public class Snappy {

    public interface SnappyWriteListener {
        void onDataWritten();

        void onError(String message);
    }

    public interface SnappyWriteMessageListener {
        void onDataWritten();

        void onError(String message);
    }
    public interface SnappyWriteProjectStatusListener {
        void onDataWritten();

        void onError(String message);
    }

    public interface SnappyDeleteListener {
        void onDataDeleted();

        void onError(String message);
    }

    public interface SnappyReadListener {
        void onDataRead(ResponseDTO response);

        void onError(String message);
    }

    public interface SnappyReadMessagesListener {
        void onDataRead(List<SimpleMessageDTO> messages);

        void onError(String message);
    }
    public interface SnappyReadStatusListener {
        void onDataRead(ProjectDTO project);

        void onError(String message);
    }

    static DB snappydb;

    static void setSnappyDB(Context ctx) {
        snappydb = MonApp.getSnappyDB(ctx);
    }

    public static final int SAVE_DATA = 1, GET_DATA = 2, GET_MESSAGES = 3, SAVE_MESSAGE = 4,
            GET_STATUS = 5, SAVE_STATUS = 6;
    public static final String DATA_KEY = "data", MESSAGE_KEY = "message", STATUS_KEY = "status";

    public static void saveData(ResponseDTO resp, Context ctx, SnappyWriteListener listener) {
        setSnappyDB(ctx);
        MTask task = new MTask(resp, listener);
        task.execute();
    }
    public static void saveMessage(SimpleMessageDTO resp, Context ctx, SnappyWriteMessageListener listener) {
        setSnappyDB(ctx);
        MTask task = new MTask(resp, listener);
        task.execute();
    }
    public static void saveStatus(ProjectDTO project, Context ctx, SnappyWriteProjectStatusListener listener) {
        setSnappyDB(ctx);
        MTask task = new MTask(project, listener);
        task.execute();
    }

    public static void getData(Context ctx, SnappyReadListener listener) {
        setSnappyDB(ctx);
        MTask task = new MTask(listener);
        task.execute();
    }
    public static void getMessages(Context ctx, SnappyReadMessagesListener listener) {
        setSnappyDB(ctx);
        MTask task = new MTask(listener);
        task.execute();
    }
    public static void getStatus(Context ctx,Integer projectID, SnappyReadStatusListener listener) {
        setSnappyDB(ctx);
        MTask task = new MTask(projectID,listener);
        task.execute();
    }


    static class MTask extends AsyncTask<Void, Void, Integer> {

        private ResponseDTO response;
        private SimpleMessageDTO message;
        private ProjectDTO project;
        private SnappyWriteListener writeListener;
        private SnappyReadListener readListener;
        private SnappyReadMessagesListener readMessagesListener;
        private SnappyWriteMessageListener writeMessageListener;
        private SnappyReadStatusListener readStatusListener;
        private SnappyWriteProjectStatusListener writeProjectStatusListener;
        private Integer projectID;
        private int type;
        private List<SimpleMessageDTO> messages;

        public MTask(ResponseDTO response, SnappyWriteListener listener) {
            this.response = response;
            this.writeListener = listener;
            this.type = SAVE_DATA;


        }

        public MTask(SimpleMessageDTO message, SnappyWriteMessageListener listener) {
            this.message = message;
            this.writeMessageListener = listener;
            this.type = SAVE_MESSAGE;


        }
        public MTask(ProjectDTO project, SnappyWriteProjectStatusListener listener) {
            this.project = project;
            this.writeProjectStatusListener = listener;
            this.type = SAVE_STATUS;


        }

        public MTask(SnappyReadMessagesListener listener) {
            this.readMessagesListener = listener;
            this.type = GET_MESSAGES;
        }

        public MTask(SnappyReadListener listener) {
            this.readListener = listener;
            this.type = GET_DATA;
        }
        public MTask(Integer projectID, SnappyReadStatusListener listener) {
            this.projectID = projectID;
            this.readStatusListener = listener;
            this.type = GET_STATUS;
        }


        @Override
        protected Integer doInBackground(Void... params) {
            try {
                switch (type) {
                    
                    case SAVE_DATA:
                        String json = gson.toJson(response);
                        snappydb.put(DATA_KEY, json);
                        Log.d(TAG, "doInBackground: data saved");
                        break;
                    case GET_DATA:
                        String x = snappydb.get(DATA_KEY);
                        if (x != null) {
                            response = gson.fromJson(x, ResponseDTO.class);
                            Log.d(TAG, "doInBackground: data retrieved");
                        }
                        break;

                    case SAVE_MESSAGE:
                        String json2 = gson.toJson(message);
                        snappydb.put(MESSAGE_KEY + System.currentTimeMillis(), json2);
                        Log.w(TAG, "doInBackground: message saved" );
                        break;
                    case GET_MESSAGES:
                        String[] keys = snappydb.findKeys(MESSAGE_KEY);
                        messages = new ArrayList<>();
                        for (String key : keys) {
                            String j = snappydb.get(key);
                            if (j != null)
                                messages.add(gson.fromJson(j, SimpleMessageDTO.class));
                        }
                        Log.w(TAG, "doInBackground: messages retrieved" + messages.size() );
                        break;
                    case SAVE_STATUS:
                        String json3 = gson.toJson(project);
                        snappydb.put(STATUS_KEY + project.getProjectID(), json3);
                        Log.w(TAG, "doInBackground: status saved" );
                        break;
                    case GET_STATUS:
                        String json4 = snappydb.get(STATUS_KEY + projectID);
                        project = gson.fromJson(json4,ProjectDTO.class);
                        Log.w(TAG, "doInBackground: project retrieved" + project.getProjectName() );
                        break;

                }


            } catch (Exception e) {
                Log.e(TAG, "doInBackground: ", e);
                return 9;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer p) {
            if (p > 0) {
                if (writeListener != null) {
                    writeListener.onError("Unable to write data");
                    return;
                }
                if (readListener != null) {
                    readListener.onError("unable to read data");
                    return;
                }
            }
            switch (type) {
                case GET_DATA:
                    if (readListener != null)
                        readListener.onDataRead(response);
                    break;
                case SAVE_DATA:
                    if (writeListener != null)
                        writeListener.onDataWritten();
                    break;
                case SAVE_MESSAGE:
                    if (writeMessageListener != null) {
                        writeMessageListener.onDataWritten();
                    }
                case GET_MESSAGES:
                    if (readMessagesListener != null) {
                        readMessagesListener.onDataRead(messages);
                    }
                case SAVE_STATUS:
                    if (writeProjectStatusListener != null) {
                        writeProjectStatusListener.onDataWritten();
                    }
                case GET_STATUS:
                    if (readStatusListener != null) {
                        readStatusListener.onDataRead(project);
                    }

            }
        }
    }

    public static final Gson gson = new Gson();
    public static final String TAG = Snappy.class.getSimpleName();
}
