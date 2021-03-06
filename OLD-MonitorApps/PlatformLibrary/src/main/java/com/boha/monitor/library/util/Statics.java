package com.boha.monitor.library.util;


import android.content.Context;
import android.graphics.Typeface;
import android.widget.TextView;


public class Statics {

    /*
     * REMOTE URL - bohamaker back end - production
     * 68.169.60.130
     */

//    public static final String WEBSOCKET_URL = "ws://bohamaker.com:3030/mp/";
//    public static final String URL = "http://bohamaker.com:3030/mp/";
//    public static final String IMAGE_URL = "http://bohamaker.com:3030/monitor_images/";
//    public static final String PDF_URL = "http://bohamaker.com:3030/monitor_documents/";

    //pecanwood IS MY HOME!!!
    public static final String URL = "http://192.168.1.233:40405/mp/";
    public static final String IMAGE_URL = "http://192.168.1.233:40405/monitor_images/";
    public static final String PDF_URL = "http://192.168.1.233:40405/monitor_documents/";

//codetribe

//    public static final String WEBSOCKET_URL = "ws://192.168.2.64:8080/mwp/";
//    public static final String URL = "http://192.168.2.64:8080/mwp/";
//    public static final String IMAGE_URL = "http://192.168.2.64:8080/";
//    public static final String PDF_URL = "http://192.168.2.64:8080/monitor_documents/";

    public static final String INVITE_DESTINATION = "https://play.google.com/store/apps/details?id=";
    public static final String INVITE_EXEC = INVITE_DESTINATION + "com.boha.monitor.exec";
    public static final String INVITE_OPERATIONS_MGR = INVITE_DESTINATION + "com.boha.monitor.operations";
    public static final String INVITE_PROJECT_MGR = INVITE_DESTINATION + "com.boha.monitor.pmanager";
    public static final String INVITE_SITE_MGR = INVITE_DESTINATION + "com.boha.monitor.site";

    public static final String UPLOAD_URL_REQUEST = "uploadUrl?";
    public static final String CRASH_REPORTS_URL = URL + "crash?";

    public static final String
            REQUEST_ENDPOINT = "wsrequest",
            COMPANY_ENDPOINT = "wscompany",
            CACHED_REQUEST_ENDPOINT = "wsrequest",
            GATEWAY_SERVLET = "gate?JSON=",
            CACHED_REQUEST_SERVLET = "cachedRequests?JSON=";

    public static final String SESSION_ID = "sessionID";
    public static final String SENT_TOKEN_TO_SERVER = "sentTokenToServer";
    public static final String REGISTRATION_COMPLETE = "registrationComplete";




    public static void setDroidFontBold( Context ctx,  TextView txt) {
        Typeface font = Typeface.createFromAsset(ctx.getAssets(),
                "DroidSerif-Bold");
        txt.setTypeface(font);
    }

    public static void setRobotoFontBoldCondensed( Context ctx,  TextView txt) {
        Typeface font = Typeface.createFromAsset(ctx.getAssets(),
                "fonts/Roboto-BoldCondensed.ttf");
        txt.setTypeface(font);
    }

    public static void setRobotoFontRegular( Context ctx,  TextView txt) {
        Typeface font = Typeface.createFromAsset(ctx.getAssets(),
                "fonts/Roboto-Regular.ttf");
        txt.setTypeface(font);
    }

    public static void setRobotoFontLight( Context ctx,  TextView txt) {
        Typeface font = Typeface.createFromAsset(ctx.getAssets(),
                "fonts/Roboto-Light.ttf");
        txt.setTypeface(font);
    }

    public static void setRobotoFontBold( Context ctx,  TextView txt) {
        Typeface font = Typeface.createFromAsset(ctx.getAssets(),
                "fonts/Roboto-Bold.ttf");
        txt.setTypeface(font);
    }

    public static void setRobotoItalic( Context ctx,  TextView txt) {
        Typeface font = Typeface.createFromAsset(ctx.getAssets(),
                "fonts/Roboto-Italic.ttf");
        txt.setTypeface(font);
    }

    public static void setRobotoRegular( Context ctx,  TextView txt) {
        Typeface font = Typeface.createFromAsset(ctx.getAssets(),
                "fonts/Roboto-Regular.ttf");
        txt.setTypeface(font);
    }

}
