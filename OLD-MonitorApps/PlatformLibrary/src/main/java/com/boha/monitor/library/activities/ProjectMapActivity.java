package com.boha.monitor.library.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.boha.monitor.library.dto.LocationTrackerDTO;
import com.boha.monitor.library.dto.ProjectDTO;
import com.boha.monitor.library.dto.ProjectTaskDTO;
import com.boha.monitor.library.dto.ProjectTaskStatusDTO;
import com.boha.monitor.library.dto.RequestDTO;
import com.boha.monitor.library.dto.ResponseDTO;
import com.boha.monitor.library.dto.TaskStatusTypeDTO;
import com.boha.monitor.library.util.NetUtil;
import com.boha.monitor.library.util.SharedUtil;
import com.boha.monitor.library.util.Statics;
import com.boha.monitor.library.util.ThemeChooser;
import com.boha.monitor.library.util.Util;
import com.boha.platform.library.R;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.awareness.snapshot.LocationResult;
import com.google.android.gms.awareness.snapshot.PlacesResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.ErrorDialogFragment;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import hugo.weaving.DebugLog;

public class ProjectMapActivity extends AppCompatActivity
        implements LocationListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {

    GoogleMap googleMap;
    GoogleApiClient mGoogleApiClient;
    Location location;
    Context ctx;

    List<Marker> markers = new ArrayList<Marker>();
    static final String LOG = ProjectMapActivity.class.getSimpleName();
    boolean mResolvingError;
    static final long ONE_MINUTE = 1000 * 60;
    static final long FIVE_MINUTES = 1000 * 60 * 5;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    ProjectDTO project;
    ResponseDTO response;
    int type;
    TextView text, txtCount;
    View topLayout;
    List<LocationTrackerDTO> trackList;
    static final Locale loc = Locale.getDefault();
    List<ProjectDTO> projectList;
    public static final int STAFF = 1, MONITOR = 2;
    DisplayMetrics displayMetrics;
    int themeDarkColor, themePrimaryColor;
    private static final String TAG = "ProjectMapActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called with: " + "savedInstanceState = [" + savedInstanceState + "]");
        ctx = getApplicationContext();

        ThemeChooser.setTheme(this);
        Resources.Theme theme = getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
        themeDarkColor = typedValue.data;
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        themePrimaryColor = typedValue.data;

        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addApi(Awareness.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project_map);

        response = (ResponseDTO) getIntent().getSerializableExtra("projects");
        projectList = response.getProjectList();
        type = getIntent().getIntExtra("type", 0);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        text = (TextView) findViewById(R.id.text);
        txtCount = (TextView) findViewById(R.id.count);
        txtCount.setText("0");
        displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay()
                .getMetrics(displayMetrics);

        txtCount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                useSmallIcons = !useSmallIcons;
                setProjectMarkers();
            }
        });
        Statics.setRobotoFontBold(ctx, text);

        topLayout = findViewById(R.id.top);


        Util.setCustomActionBar(getApplicationContext(), getSupportActionBar(),
                SharedUtil.getCompany(ctx).getCompanyName(), "Project Locations",
                ContextCompat.getDrawable(getApplicationContext(), R.drawable.glasses48));
        projectList = response.getProjectList();
        txtCount.setText("" + projectList.size());


    }

    boolean useSmallIcons;

    //todo remove
    private void temporaryWork(LatLng latLng) {
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        Log.w(LOG, "********* onMapClick");

        RequestDTO w = new RequestDTO(RequestDTO.UPDATE_PROJECT);
        ProjectDTO p = new ProjectDTO();
        p.setProjectID(project.getProjectID());
        p.setLatitude(latLng.latitude);
        p.setLongitude(latLng.longitude);
        p.setLocationConfirmed(true);
        w.setProject(p);

        NetUtil.sendRequest(ctx, w, new NetUtil.NetUtilListener() {
            @Override
            public void onResponse(ResponseDTO response) {
                if (response.getStatusCode() == 0)
                    Log.i(LOG, "+++ cool. project location updated");
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

    Activity activity;
    public static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 11;
    boolean mRequestingLocationUpdates;
    static final SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm");
    int index = 0;

    private void setGoogleMap() {
        activity = this;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                return;
            }

            return;
        }
        getLocation();
        googleMap.setMyLocationEnabled(true);
        googleMap.setBuildingsEnabled(true);

        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (location == null) {
                    Util.showErrorToast(getApplicationContext(), "device location not available");
                    return true;
                }
                LatLng latLng = marker.getPosition();
                Location loc = new Location(location);
                loc.setLatitude(latLng.latitude);
                loc.setLongitude(latLng.longitude);
                if (project != null) {

                }
                float mf = location.distanceTo(loc);
                Log.w(LOG, "######### distance, again: " + mf);

                showPopup(latLng.latitude, latLng.longitude,
                        marker.getTitle(), marker.getSnippet());

                return true;
            }
        });
        setProjectMarkers();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation();
                    setGoogleMap();

                } else {
                    throw new UnsupportedOperationException();
                }
                return;
            }
        }
    }

    static final DecimalFormat df = new DecimalFormat("###,##0.00");

    private ProjectTaskStatusDTO getLastStatus(ProjectDTO project) {
        ProjectTaskStatusDTO status = null;
        List<ProjectTaskStatusDTO> ptList = new ArrayList<>();
        for (ProjectTaskDTO task : project.getProjectTaskList()) {
            ptList.addAll(task.getProjectTaskStatusList());
        }
//        Collections.sort(ptList);
        if (!ptList.isEmpty()) {
            status = ptList.get(0);
        }

        return status;
    }

    private void setProjectMarkers() {
        googleMap.clear();
        index = 0;
        Resources resources = getResources();

        for (ProjectDTO project : projectList) {
            if (project.getLatitude() == null) continue;
            LatLng pnt = new LatLng(project.getLatitude(), project.getLongitude());
            ProjectTaskStatusDTO status = getLastStatus(project);

            Drawable icon = getIcon(index);
            BitmapDescriptor desc = null;

            if (status != null) {
                Short color = status.getTaskStatusType().getStatusColor();
                switch (color) {
                    case TaskStatusTypeDTO.STATUS_COLOR_RED:

                        icon.setColorFilter(new
                                PorterDuffColorFilter(resources.getColor(R.color.red_800), PorterDuff.Mode.MULTIPLY));
                        break;
                    case TaskStatusTypeDTO.STATUS_COLOR_GREEN:
                        icon.setColorFilter(new
                                PorterDuffColorFilter(resources.getColor(R.color.green_800), PorterDuff.Mode.MULTIPLY));
                        break;
                    case TaskStatusTypeDTO.STATUS_COLOR_AMBER:
                        icon.setColorFilter(new
                                PorterDuffColorFilter(resources.getColor(R.color.amber_800), PorterDuff.Mode.MULTIPLY));
                        break;
                    default:
                        icon.setColorFilter(new
                                PorterDuffColorFilter(resources.getColor(R.color.black), PorterDuff.Mode.MULTIPLY));

                        break;
                }
            }

            desc = BitmapDescriptorFactory.fromBitmap(Util.drawableToBitmap(icon));
            Marker m = googleMap.addMarker(new MarkerOptions()
                    .title(project.getProjectID().toString())
                    .icon(desc)
                    .snippet(project.getProjectName())
                    .position(pnt));
            markers.add(m);
            index++;
        }
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                //ensure that all markers in bounds
                if (useSmallIcons) {
                    Log.i(TAG, "onMapLoaded: useSmallIcons = true. markers: " + markers.size());
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (Marker marker : markers) {
                        builder.include(marker.getPosition());
                    }

                    LatLngBounds bounds = builder.build();
                    int padding = 60; // offset from edges of the map in pixels
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

                    txtCount.setText("" + markers.size());
                    googleMap.animateCamera(cu);
                } else {
                    Log.w(TAG, "onMapLoaded: useSmallIcons = false. markers: " + markers.size());
                    LatLng pnt = markers.get(getMarkerIndex()).getPosition();
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pnt, 1.0f));
                    if (projectList.size() == 1) {
                        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15.0f));
                    } else {
                        googleMap.animateCamera(CameraUpdateFactory.zoomTo(11.0f));
                    }
                }
            }
        });

    }

    Random rand = new Random(System.currentTimeMillis());

    private int getMarkerIndex() {

        return 0;
    }

    private void setOneProjectMarker() {

        LatLng pnt = new LatLng(project.getLatitude(),
                project.getLongitude());
        View view = getLayoutInflater().inflate(R.layout.project_name, null);
        TextView name = (TextView) view.findViewById(R.id.name);
        TextView st = (TextView) view.findViewById(R.id.statusColor);
        ProjectTaskStatusDTO status = getLastStatus(project);
        name.setText(project.getProjectName());
        st.setBackground(ContextCompat.getDrawable(ctx, R.drawable.xblack_oval_small));
        if (status != null) {
            switch (status.getTaskStatusType().getStatusColor()) {
                case TaskStatusTypeDTO.STATUS_COLOR_AMBER:
                    st.setBackground(ContextCompat.getDrawable(ctx, R.drawable.xamber_oval_small));
                    break;
                case TaskStatusTypeDTO.STATUS_COLOR_GREEN:
                    st.setBackground(ContextCompat.getDrawable(ctx, R.drawable.xgreen_oval_small));
                    break;
                case TaskStatusTypeDTO.STATUS_COLOR_RED:
                    st.setBackground(ContextCompat.getDrawable(ctx, R.drawable.xred_oval_small));
                    break;

            }
        } else {
            st.setBackground(ContextCompat.getDrawable(ctx, R.drawable.xblack_oval_small));
        }
        Bitmap bmBitmap = Util.createBitmapFromView(ctx, view, displayMetrics);
        BitmapDescriptor desc = BitmapDescriptorFactory.fromBitmap(bmBitmap);
        Marker m =
                googleMap.addMarker(new MarkerOptions()
                        .title(project.getProjectID().toString())
                        .icon(desc)
                        .snippet(project.getProjectName())
                        .position(pnt));
        markers.add(m);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pnt, 1.0f));
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(12.0f));
    }

    List<String> list;

    private void showPopup(final double lat, final double lng, final String id, String snippet) {
        list = new ArrayList<>();
        list.add(getString(R.string.directions));
        list.add(getString(R.string.streetview));
        list.add(getString(R.string.statrpt));

        Util.showPopupBasicWithHeroImage(ctx, this, list, topLayout,
                snippet,
                new Util.UtilPopupListener() {
                    @Override
                    public void onItemSelected(int index) {
                        if (list.get(index).equalsIgnoreCase(getString(R.string.directions))) {
                            startDirectionsMap(lat, lng);
                        }
                        if (list.get(index).equalsIgnoreCase(getString(R.string.streetview))) {
                            getStreetView(lat, lng);
                        }

                        if (list.get(index).equalsIgnoreCase(getString(R.string.statrpt))) {
                            Integer projectID = Integer.parseInt(id);
                            ProjectDTO g = null;
                            if (projectList != null) {
                                for (ProjectDTO x : projectList) {
                                    if (x.getProjectID().intValue() == projectID.intValue()) {
                                        g = x;
                                        break;
                                    }
                                }
                            } else {
                                g = project;
                            }
                            if (g != null) {
                                getStatusReport(g);
                            }
                        }
                    }
                });


    }

    private void getStreetView(double latitude, double longitude) {
        StringBuilder sb = new StringBuilder();
        sb.append("google.streetview:cbll=");
        sb.append(latitude).append(",").append(longitude);
        Uri gmmIntentUri = Uri.parse(sb.toString());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private void getStatusReport(ProjectDTO project) {

        Intent w = new Intent(this, StatusReportActivity.class);
        w.putExtra("project", project);
        startActivity(w);
    }


    private void startDirectionsMap(double lat, double lng) {
        Log.i(LOG, "startDirectionsMap ..........");
        String url = "http://maps.google.com/maps?saddr="
                + location.getLatitude() + "," + location.getLongitude()
                + "&daddr=" + lat + "," + lng + "&mode=driving";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.monitor_map, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final float ACCURACY_THRESHOLD = 30;
    boolean directionRequired;
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged " + location.getLatitude()
                + " " + location.getLongitude() + " " + location.getAccuracy());

        if (location.getAccuracy() <= ACCURACY_THRESHOLD) {
            this.location = location;
            stopLocationUpdates();
            mRequestingLocationUpdates = false;

            if (directionRequired) {
                directionRequired = false;
                Log.i(TAG, "startDirectionsMap ..........");
                String url = "http://maps.google.com/maps?saddr="
                        + this.location.getLatitude() + "," + this.location.getLongitude()
                        + "&daddr=" + project.getLatitude() + "," + project.getLongitude() + "&mode=driving";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setClassName("com.google.android.apps.maps",
                        "com.google.android.maps.MapsActivity");
                startActivity(intent);
            }


        }

    }
    @DebugLog
    protected void stopLocationUpdates() {
        Log.e(TAG, "### stopLocationUpdates ...");
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
            mRequestingLocationUpdates = false;
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        Log.e(LOG, "################ onStart .... connect API and location clients ");
        mGoogleApiClient.connect();

    }

    @Override
    protected void onStop() {
        Log.w(LOG, "############## onStop stopping google service clients");
        try {
            if (mGoogleApiClient != null)
                mGoogleApiClient.disconnect();
        } catch (Exception e) {
            Log.e(LOG, "Failed to Stop something", e);
        }
        super.onStop();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e(LOG, "########### onConnected .... get location...?");
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(500);
        startLocationUpdates();
    }
    LocationRequest mLocationRequest;
    @DebugLog
    protected void startLocationUpdates() {
        Log.d(TAG, "### startLocationUpdates ....");
        if (mGoogleApiClient.isConnected()) {
            mRequestingLocationUpdates = true;
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                return;
            }
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            Log.d(TAG, "## GoogleApiClient connected, requesting location updates ...");
        } else {
            Log.e(TAG, "------- GoogleApiClient is NOT connected, not sure where we are...");
            mGoogleApiClient.connect();

        }
    }
    @Override
    public void onConnectionSuspended(int i) {

    }


    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "errordialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    public void onDialogDismissed() {
        mResolvingError = false;
    }


    List<BitmapDescriptor> bmdList = new ArrayList<BitmapDescriptor>();

    private Drawable getIcon(int index) {


        try {
            switch (index) {
                case 0:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_1);

                case 1:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_2);

                case 2:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_3);

                case 3:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_4);

                case 4:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_5);

                case 5:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_6);

                case 6:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_7);

                case 7:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_8);

                case 8:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_9);

                case 9:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_10);

                case 10:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_11);

                case 11:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_12);

                case 12:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_13);

                case 13:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_14);

                case 14:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_15);

                case 15:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_16);

                case 16:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_17);

                case 17:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_18);

                case 18:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_19);

                case 19:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_20);

                case 20:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_21);

                case 21:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_22);

                case 22:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_23);

                case 23:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_24);

                case 24:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_25);

                case 25:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_26);

                case 26:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_27);

                case 27:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_28);

                case 28:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_29);

                case 29:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_30);

                case 30:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_31);

                case 31:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_32);

                case 32:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_33);

                case 33:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_34);

                case 34:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_35);

                case 35:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_36);

                case 36:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_37);

                case 37:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_38);

                case 38:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_39);

                case 39:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_40);

                case 40:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_41);

                case 41:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_42);

                case 42:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_43);

                case 43:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_44);

                case 44:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_45);

                case 45:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_46);

                case 46:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_47);

                case 47:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_48);

                case 48:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_49);

                case 49:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_50);

                case 50:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_51);

                case 51:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_52);

                case 52:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_53);

                case 53:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_54);

                case 54:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_55);

                case 55:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_56);

                case 56:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_57);

                case 57:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_58);

                case 58:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_59);

                case 59:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_60);

                case 60:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_61);

                case 61:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_62);

                case 62:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_63);

                case 63:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_64);

                case 64:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_65);

                case 65:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_66);

                case 66:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_67);

                case 67:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_68);

                case 68:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_69);

                case 69:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_70);

                case 70:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_71);

                case 71:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_72);

                case 72:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_73);

                case 73:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_74);

                case 74:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_75);

                case 75:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_76);

                case 76:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_77);

                case 77:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_78);

                case 78:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_79);

                case 79:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_80);

                case 80:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_81);

                case 81:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_82);

                case 82:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_83);

                case 83:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_84);

                case 84:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_85);

                case 85:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_86);

                case 86:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_87);

                case 87:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_88);

                case 88:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_89);

                case 89:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_90);

                case 90:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_91);

                case 91:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_92);

                case 92:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_93);

                case 93:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_94);

                case 94:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_95);

                case 95:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_96);

                case 96:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_97);

                case 97:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_98);

                case 98:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_99);

                case 99:
                    return ContextCompat.getDrawable(ctx, R.drawable.number_100);

            }
        } catch (Exception e) {
            Log.e(LOG, "Load icons failed", e);
        }

        return ContextCompat.getDrawable(ctx, R.drawable.number_1);
    }

    boolean coordsConfirmed;

    @Override
    public void onBackPressed() {
        Log.e(LOG, "######## onBackPressed, coordsConfirmed: " + coordsConfirmed);

        finish();
    }

    @Override
    public void onPause() {
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        super.onPause();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        setGoogleMap();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                return;
            }

            return;
        }
        Awareness.SnapshotApi.getLocation(mGoogleApiClient)
                .setResultCallback(new ResultCallback<LocationResult>() {
                    @Override
                    public void onResult(@NonNull LocationResult locationResult) {
                        if (!locationResult.getStatus().isSuccess()) {
                            Log.e(TAG, "###########Could not get location.");
                            return;
                        }
                        location = locationResult.getLocation();
                        Log.i(TAG, ".................onConnected: snapshot: Lat: "
                                + location.getLatitude() + ", Lon: "
                                + location.getLongitude());
                        if (googleMap != null) {
                            onMapReady(googleMap);
                        }
                    }
                });

        Awareness.SnapshotApi.getPlaces(mGoogleApiClient)
                .setResultCallback(new ResultCallback<PlacesResult>() {
                    @Override
                    public void onResult(@NonNull PlacesResult placesResult) {
                        if (!placesResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Could not get places.");
                            return;
                        }
                        List<PlaceLikelihood> placeLikelihoodList = placesResult.getPlaceLikelihoods();
                        // Show the top 5 possible location results.
                        if (placeLikelihoodList != null) {
                            for (int i = 0; i < 5 && i < placeLikelihoodList.size(); i++) {
                                PlaceLikelihood p = placeLikelihoodList.get(i);
                                Log.i(TAG, p.getPlace().getName().toString() + ", likelihood: " + p.getLikelihood());
                            }
                        } else {
                            Log.e(TAG, "Place is null.");
                        }
                    }
                });
    }
}
