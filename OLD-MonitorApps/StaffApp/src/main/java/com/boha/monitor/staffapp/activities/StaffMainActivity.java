package com.boha.monitor.staffapp.activities;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.boha.monitor.library.activities.GPSActivity;
import com.boha.monitor.library.activities.PhotoListActivity;
import com.boha.monitor.library.activities.PictureActivity;
import com.boha.monitor.library.activities.ProfilePhotoActivity;
import com.boha.monitor.library.activities.ProjectMapActivity;
import com.boha.monitor.library.activities.StatusReportActivity;
import com.boha.monitor.library.activities.ThemeSelectorActivity;
import com.boha.monitor.library.activities.UpdateActivity;
import com.boha.monitor.library.activities.VideoActivity;
import com.boha.monitor.library.dto.CompanyDTO;
import com.boha.monitor.library.dto.LocationTrackerDTO;
import com.boha.monitor.library.dto.MonitorDTO;
import com.boha.monitor.library.dto.PhotoUploadDTO;
import com.boha.monitor.library.dto.ProjectDTO;
import com.boha.monitor.library.dto.RequestDTO;
import com.boha.monitor.library.dto.ResponseDTO;
import com.boha.monitor.library.dto.SimpleMessageDTO;
import com.boha.monitor.library.dto.StaffDTO;
import com.boha.monitor.library.fragments.MediaDialogFragment;
import com.boha.monitor.library.fragments.MonitorListFragment;
import com.boha.monitor.library.fragments.PageFragment;
import com.boha.monitor.library.fragments.ProjectListFragment;
import com.boha.monitor.library.fragments.SimpleMessageFragment;
import com.boha.monitor.library.fragments.StaffListFragment;
import com.boha.monitor.library.fragments.StaffProfileFragment;
import com.boha.monitor.library.fragments.TaskTypeListFragment;
import com.boha.monitor.library.services.PhotoUploadService;
import com.boha.monitor.library.services.RequestSyncService;
import com.boha.monitor.library.util.DepthPageTransformer;
import com.boha.monitor.library.util.NetUtil;
import com.boha.monitor.library.util.SharedUtil;
import com.boha.monitor.library.util.Snappy;
import com.boha.monitor.library.util.ThemeChooser;
import com.boha.monitor.library.util.Util;
import com.boha.monitor.staffapp.R;
import com.boha.monitor.staffapp.fragments.NavigationDrawerFragment;
import com.boha.monitor.staffapp.services.StaffGCMListenerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hugo.weaving.DebugLog;

import static com.boha.monitor.library.activities.ProjectMapActivity.MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static com.boha.monitor.library.util.Util.createSnackBar;

/**
 * This class is the main activity that receives control from
 * the SignInActivity. It controls the sliding drawer menu and
 * hosts a ViewPager that contains the UI fragments.
 * Uses a GoogleApiClient object for location requirements
 *
 * @see ProjectListFragment
 * @see MonitorListFragment
 * @see StaffListFragment
 */
public class StaffMainActivity extends AppCompatActivity implements
        MonitorListFragment.MonitorListListener,
        StaffProfileFragment.StaffFragmentListener,
        StaffListFragment.CompanyStaffListListener,
        ProjectListFragment.ProjectListFragmentListener,
        SimpleMessageFragment.SimpleMessageFragmentListener,
        LocationListener,
        NavigationDrawerFragment.NavigationDrawerListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    StaffProfileFragment staffProfileFragment;
    MonitorListFragment monitorListFragment;
    StaffListFragment staffListFragment;
    ProjectListFragment projectListFragment;
    SimpleMessageFragment simpleMessageFragment;
    ActionBar actionBar;
    Location mLocation;

    @Override
    public void onResume() {
        Log.w(TAG, "++++++++ ############## onResume - will get cache data");
        super.onResume();
        if (navImage != null) {
            navImage.setImageDrawable(Util.getRandomBackgroundImage(ctx));
        } else {
            Log.e(TAG, "navImage is null");
        }
        getCache();

    }

    NavigationDrawerFragment mNavigationDrawerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ctx = getApplicationContext();

        ThemeChooser.setTheme(this);
        Resources.Theme theme = getTheme();
        TypedValue typedValue = new TypedValue();
        theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
        themeDarkColor = typedValue.data;
        theme.resolveAttribute(R.attr.colorPrimary, typedValue, true);
        themePrimaryColor = typedValue.data;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_staff_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mNavigationDrawerFragment.setPrimaryDarkColor(themeDarkColor);
        mNavigationDrawerFragment.setPrimaryColor(themePrimaryColor);
        logo = R.drawable.ic_action_pin;
        //
        mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout), NavigationDrawerFragment.FROM_MAIN);

        mPager = (ViewPager) findViewById(R.id.viewpager);
        mPager.setOffscreenPageLimit(4);
        PagerTitleStrip strip = (PagerTitleStrip) mPager.findViewById(R.id.pager_title_strip);
        strip.setVisibility(View.GONE);
        strip.setBackgroundColor(themeDarkColor);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(themeDarkColor);
            window.setNavigationBarColor(themeDarkColor);
        }

        Util.setCustomActionBar(getApplicationContext(), getSupportActionBar(),
                SharedUtil.getCompany(ctx).getCompanyName(), "Project Monitoring",
                ContextCompat.getDrawable(getApplicationContext(), com.boha.platform.library.R.drawable.glasses48));

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.registerReceiver(new PhotoBroadcastReceiver(), new IntentFilter(PhotoUploadService.BROADCAST_PHOTO_UPLOADED));
        bm.registerReceiver(new MessageBroadcastReceiver(), new IntentFilter(StaffGCMListenerService.BROADCAST_MESSAGE_RECEIVED));
    }


    List<ProjectDTO> projectsList;

    private List<ProjectDTO> getProjectsLocationConfirmed() {
        projectsList = new ArrayList<>();
        for (ProjectDTO m : response.getProjectList()) {
            if (m.getLocationConfirmed() != null) {
                projectsList.add(m);
            }
        }
        return projectsList;
    }

    @DebugLog
    private void getCache() {
        Snappy.getData(getApplicationContext(), new Snappy.SnappyReadListener() {
            @Override
            public void onDataRead(ResponseDTO r) {
                response = r;
                if (!response.getProjectList().isEmpty()) {
                    getProjectsLocationConfirmed();
                    buildPages();
                }
                getRemoteStaffData(false);

            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "onError....: " + message);
            }
        });
    }

    @DebugLog
    private void getRemoteStaffData(boolean showBusy) {
        RequestDTO w = new RequestDTO(RequestDTO.GET_STAFF_DATA);
        w.setStaffID(SharedUtil.getCompanyStaff(ctx).getStaffID());
        w.setZipResponse(false);

        companyDataRefreshed = false;
        setRefreshActionButtonState(showBusy);
        if (showBusy) {
            snackbar = Util.createSnackBar(mPager, "Refreshing your data, this may take a minute or two ...", "OK", "CYAN");
        }
        NetUtil.sendRequest(getApplicationContext(), w, new NetUtil.NetUtilListener() {
            @Override
            public void onResponse(final ResponseDTO r) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setRefreshActionButtonState(false);
                        if (snackbar != null)
                            snackbar.dismiss();
                        companyDataRefreshed = true;
                        response = r;
                        buildPages();
                        Snappy.saveData(response, ctx, new Snappy.SnappyWriteListener() {
                            @Override
                            public void onDataWritten() {
                                Log.d(TAG, "onDataWritten: cached to snappy db");
                            }

                            @Override
                            public void onError(String message) {
                                createSnackBar(mPager, message, "Not OK", "RED");
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
                        setRefreshActionButtonState(false);
                        Util.showErrorToast(getApplicationContext(), message);
                    }
                });
            }

            @Override
            public void onWebSocketClose() {

            }
        });
    }

    Snackbar snackbar;

    @DebugLog
    private void buildPages() {
        pageFragmentList = new ArrayList<>();

        staffProfileFragment = StaffProfileFragment.newInstance(SharedUtil.getCompanyStaff(ctx));
        monitorListFragment = MonitorListFragment.newInstance(response.getMonitorList(), MonitorListFragment.STAFF);
        staffListFragment = StaffListFragment.newInstance(response.getStaffList());
        projectListFragment = ProjectListFragment.newInstance(response);
        simpleMessageFragment = new SimpleMessageFragment();


        staffProfileFragment.setThemeColors(themePrimaryColor, themeDarkColor);
        monitorListFragment.setThemeColors(themePrimaryColor, themeDarkColor);
        staffListFragment.setThemeColors(themePrimaryColor, themeDarkColor);
        projectListFragment.setThemeColors(themePrimaryColor, themeDarkColor);
        simpleMessageFragment.setThemeColors(themePrimaryColor, themeDarkColor);

        pageFragmentList.add(projectListFragment);
        pageFragmentList.add(staffListFragment);
        pageFragmentList.add(monitorListFragment);
        pageFragmentList.add(simpleMessageFragment);
        pageFragmentList.add(staffProfileFragment);

        adapter = new StaffPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(adapter);
        mPager.setPageTransformer(true, new DepthPageTransformer());

        mPager.setCurrentItem(currentPageIndex, true);
        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentPageIndex = position;
                pageFragmentList.get(position).animateHeroHeight();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }

    @DebugLog
    protected void startLocationUpdates() {
        Log.d(TAG, "### startLocationUpdates ....");
        if (googleApiClient.isConnected()) {
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
                    googleApiClient, mLocationRequest, this);
            Log.d(TAG, "## GoogleApiClient connected, requesting location updates ...");
        } else {
            Log.e(TAG, "------- GoogleApiClient is NOT connected, not sure where we are...");
            googleApiClient.connect();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();

                } else {
                    throw new UnsupportedOperationException();
                }
                return;
            }
        }
    }

    @DebugLog
    protected void stopLocationUpdates() {
        Log.e(TAG, "### stopLocationUpdates ...");
        if (googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    googleApiClient, this);
            mRequestingLocationUpdates = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mMenu = menu;
        return true;
    }

    static final int THEME_REQUESTED = 1762;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            getRemoteStaffData(true);
            return true;
        }
        if (id == R.id.action_theme) {
            Intent w = new Intent(this, ThemeSelectorActivity.class);
            w.putExtra("darkColor", themeDarkColor);
            startActivityForResult(w, THEME_REQUESTED);
            return true;
        }
        if (id == R.id.action_help) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle b) {
        Log.w(TAG, "onSaveInstanceState");
        b.putSerializable("selectedProject", selectedProject);
        super.onSaveInstanceState(b);
    }

    @Override
    public void onRestoreInstanceState(Bundle b) {
        Log.w(TAG, "onRestoreInstanceState");
        selectedProject = (ProjectDTO) b.getSerializable("selectedProject");
        super.onRestoreInstanceState(b);
    }

    @Override
    @DebugLog
    public void onStart() {
        Log.d(TAG,
                "## onStart - GoogleApiClient connecting ... ");
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
        Log.i(TAG, "## onStart Bind to PhotoUploadService, RequestService");
        Intent intent = new Intent(this, PhotoUploadService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        Intent intentw = new Intent(this, RequestSyncService.class);
        bindService(intentw, rConnection, Context.BIND_AUTO_CREATE);
        super.onStart();
    }

    @Override
    @DebugLog
    public void onStop() {
        super.onStop();
        if (googleApiClient != null) {
            googleApiClient.disconnect();
            Log.e(TAG, "### onStop - locationClient disconnecting ");
        }
        Log.e(TAG, "## onStop unBind from PhotoUploadService, RequestService");
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        if (rBound) {
            unbindService(rConnection);
            rBound = false;
        }

    }

    @Override
    @DebugLog
    public void onConnected(Bundle bundle) {
        Log.i(TAG,
                "+++  GoogleApiClient onConnected() ...");
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
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setFastestInterval(500);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged " + location.getLatitude()
                + " " + location.getLongitude() + " " + location.getAccuracy());

        if (location.getAccuracy() <= ACCURACY_THRESHOLD) {
            mLocation = location;
            stopLocationUpdates();
            mRequestingLocationUpdates = false;

            if (directionRequired) {
                directionRequired = false;
                Log.i(TAG, "startDirectionsMap ..........");
                String url = "http://maps.google.com/maps?saddr="
                        + mLocation.getLatitude() + "," + mLocation.getLongitude()
                        + "&daddr=" + selectedProject.getLatitude() + "," + selectedProject.getLongitude() + "&mode=driving";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.setClassName("com.google.android.apps.maps",
                        "com.google.android.maps.MapsActivity");
                startActivity(intent);
            }
            if (sendLocation) {
                sendLocation = false;
                submitTrack();
            }

        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMonitorSelected(MonitorDTO monitor) {

    }

    @Override
    public void onMonitorPhotoRequired(MonitorDTO monitor) {

    }

    @Override
    public void onMonitorEditRequested(MonitorDTO monitor) {

    }

    @Override
    public void onMessagingRequested(MonitorDTO monitor) {

    }

    boolean sendLocation;
    List<Integer> monitorList, staffList;

    @Override
    public void onLocationSendRequired(List<Integer> monitorList, List<Integer> staffList) {
        sendLocation = true;
        this.monitorList = monitorList;
        this.staffList = staffList;

        setBusy(true);
        Snackbar.make(mPager, "Getting device GPS coordinates, may take a few seconds ...", Snackbar.LENGTH_LONG).show();
        startLocationUpdates();
    }

    private void submitTrack() {
        RequestDTO w = new RequestDTO(RequestDTO.SEND_LOCATION);
        LocationTrackerDTO dto = new LocationTrackerDTO();
        StaffDTO staff = SharedUtil.getCompanyStaff(ctx);

        dto.setStaffID(staff.getStaffID());
        dto.setDateTracked(new Date().getTime());
        dto.setLatitude(mLocation.getLatitude());
        dto.setLongitude(mLocation.getLongitude());
        dto.setAccuracy(mLocation.getAccuracy());
        dto.setStaffName(staff.getFullName());
        dto.setMonitorList(monitorList);
        dto.setStaffList(staffList);
        dto.setGcmDevice(SharedUtil.getGCMDevice(ctx));
        dto.getGcmDevice().setRegistrationID(null);
        w.setLocationTracker(dto);

        setBusy(true);
        NetUtil.sendRequest(ctx, w, new NetUtil.NetUtilListener() {
            @Override
            public void onResponse(ResponseDTO response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBusy(false);
                        Util.showToast(ctx, "Location has been sent");
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setBusy(false);
                    }
                });
            }

            @Override
            public void onWebSocketClose() {

            }
        });

    }


    static final int CHECK_FOR_REFRESH = 3121, STAFF_PICTURE_REQUESTED = 3472;
    boolean companyDataRefreshed;


    private void refreshProjectStatus() {
        RequestDTO w = new RequestDTO(RequestDTO.GET_PROJECT_STATUS_PHOTOS);
        w.setProjectID(selectedProject.getProjectID());
        Log.w(TAG, "refreshProjectStatus sending request ...");
        NetUtil.sendRequest(ctx, w, new NetUtil.NetUtilListener() {
            @Override
            public void onResponse(ResponseDTO response) {
                Log.i(TAG, "refreshProjectStatus onResponse");
                selectedProject.setPhotoUploadList(response.getPhotoUploadList());
                selectedProject.setProjectTaskList(response.getProjectTaskList());
                projectListFragment.refreshProject(selectedProject);

                //update cache
                Snappy.getData(ctx, new Snappy.SnappyReadListener() {
                    @Override
                    public void onDataRead(ResponseDTO response) {
                        List<ProjectDTO> list = new ArrayList<>();
                        for (ProjectDTO p : response.getProjectList()) {
                            if (p.getProjectID().intValue() == selectedProject.getProjectID().intValue()) {
                                list.add(selectedProject);
                            } else {
                                list.add(p);
                            }

                        }
                        response.setProjectList(list);
                        Snappy.saveData(response, ctx, null);
                    }

                    @Override
                    public void onError(String message) {

                    }
                });
            }

            @Override
            public void onError(String message) {

            }

            @Override
            public void onWebSocketClose() {

            }
        });

    }

    @Override
    @DebugLog
    public void onActivityResult(int reqCode, final int resCode, Intent data) {
        Log.d(TAG, "onActivityResult reqCode " + reqCode + " resCode " + resCode);
        switch (reqCode) {
            case REQUEST_CAMERA:
                if (resCode == RESULT_OK) {
                    getRemoteStaffData(true);
//                    refreshProjectStatus();
                }
                break;
            case REQUEST_STATUS_UPDATE:
                if (resCode == RESULT_OK) {
                    boolean statusCompleted =
                            data.getBooleanExtra("statusCompleted", false);
                    if (statusCompleted) {
                        Log.e(TAG, "StaffMainActivity statusCompleted, getting refreshed");
//                        getRemoteStaffData(true);
                        refreshProjectStatus();
                    }
                }
                break;
            case STAFF_PICTURE_REQUESTED:
                if (resCode == RESULT_OK) {
                    PhotoUploadDTO x = (PhotoUploadDTO) data.getSerializableExtra("photo");
                    SharedUtil.savePhoto(getApplicationContext(), x);
                    Log.e(TAG, "photo returned uri: " + x.getUri());
                    staffProfileFragment.setPicture(x);
                }
                break;
            case LOCATION_REQUESTED:
                if (resCode == RESULT_OK) {
                    getRemoteStaffData(true);
                }
                break;
            case THEME_REQUESTED:
                if (resCode == RESULT_OK) {
                    finish();
                    Intent w = new Intent(this, StaffMainActivity.class);
                    startActivity(w);
                }
                break;
            case CHECK_FOR_REFRESH:
                if (resCode == RESULT_OK) {
                    ResponseDTO x = (ResponseDTO) data.getSerializableExtra("response");
                    company.setStaffList(x.getStaffList());
                    company.setMonitorList(x.getMonitorList());
                    company.setProjectStatusTypeList(x.getProjectStatusTypeList());
                    company.setTaskStatusTypeList(x.getTaskStatusTypeList());
                    company.setPortfolioList(x.getPortfolioList());
                    companyDataRefreshed = true;

                }
        }
    }

    @Override
    public void setBusy(boolean busy) {
        setRefreshActionButtonState(busy);
    }

    @Override
    public void onStaffPictureRequired(StaffDTO staff) {
        Intent w = new Intent(this, ProfilePhotoActivity.class);
        w.putExtra("staff", staff);
        startActivityForResult(w, STAFF_PICTURE_REQUESTED);
    }

    @Override
    public void onStaffAdded(StaffDTO staff) {

    }

    @Override
    public void onStaffUpdated(StaffDTO staff) {

    }

    @Override
    public void onAppInvitationRequested(StaffDTO staff, int appType) {

    }

    @Override
    public void onNewCompanyStaff() {

    }


    @Override
    public void onCompanyStaffInvitationRequested(List<StaffDTO> companyStaffList, int index) {

    }

    @Override
    public void onCompanyStaffPictureRequested(StaffDTO companyStaff) {

    }

    @Override
    public void onCompanyStaffEditRequested(StaffDTO companyStaff) {

    }

    static final int REQUEST_CAMERA = 3329,
            REQUEST_VIDEO = 3488,
            LOCATION_REQUESTED = 9031, REQUEST_STATUS_UPDATE = 3291;

    ProjectDTO selectedProject;

    @Override
    @DebugLog
    public void onCameraRequired(final ProjectDTO project) {
        selectedProject = project;
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
        MediaDialogFragment mdf = new MediaDialogFragment();
        mdf.setCancelable(false);
        mdf.setListener(new MediaDialogFragment.MediaDialogListener() {
            @Override
            public void onVideoSelected() {
                Intent w = new Intent(getApplicationContext(), VideoActivity.class);
                w.putExtra("project", project);
                startActivityForResult(w, REQUEST_VIDEO);
            }

            @Override
            public void onPhotoSelected() {

                Intent w = new Intent(getApplicationContext(), PictureActivity.class);
                w.putExtra("project", project);
                w.putExtra("type", PhotoUploadDTO.PROJECT_IMAGE);
                startActivityForResult(w, REQUEST_CAMERA);
            }
        });
        mdf.show(getSupportFragmentManager(), "projectDiag");

    }

    @Override
    @DebugLog
    public void onStatusUpdateRequired(ProjectDTO project) {
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
        selectedProject = project;
        Intent w = new Intent(this, UpdateActivity.class);
        w.putExtra("project", project);
        w.putExtra("darkColor", themeDarkColor);
        w.putExtra("type", TaskTypeListFragment.STAFF);
        startActivityForResult(w, REQUEST_STATUS_UPDATE);
    }

    Activity activity;

    @Override
    @DebugLog
    public void onLocationRequired(final ProjectDTO project) {
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
        selectedProject = project;
        activity = this;
        if (project.getLatitude() != null) {
            AlertDialog.Builder c = new AlertDialog.Builder(this);
            c.setTitle("Project Location")
                    .setMessage("Do you want to update the location of the project?\n\n"
                            + project.getProjectName())
                    .setPositiveButton("Update GPS Location", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent w = new Intent(activity, GPSActivity.class);
                            w.putExtra("project", project);
                            startActivityForResult(w, LOCATION_REQUESTED);
                        }
                    })
                    .setNegativeButton("View Map", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent w = new Intent(activity, ProjectMapActivity.class);
                            ResponseDTO responseDTO = new ResponseDTO();
                            responseDTO.setProjectList(new ArrayList<ProjectDTO>());
                            responseDTO.getProjectList().add(project);
                            w.putExtra("projects", responseDTO);
                            startActivity(w);
                        }
                    })
                    .show();

            return;
        }

        AlertDialog.Builder c = new AlertDialog.Builder(this);
        c.setTitle("Project Location")
                .setMessage("You are about to set the location coordinates of the project: " + project.getProjectName() +
                        ". Please step as close as possible to the project and begin GPS scan.")
                .setPositiveButton("Start Scan", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent w = new Intent(activity, GPSActivity.class);
                        w.putExtra("project", project);
                        startActivityForResult(w, LOCATION_REQUESTED);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();

    }

    boolean directionRequired;
    //ProjectDTO project;

    @Override
    public void onDirectionsRequired(ProjectDTO project) {
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
        selectedProject = project;
        if (project.getLatitude() == null) {
            Util.showErrorToast(ctx, "Project has not been located yet!");
            return;
        }
        directionRequired = true;
        //this.project = project;
        startLocationUpdates();

    }

    @Override
    public void onMessagingRequired(ProjectDTO project) {
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
    }

    @Override
    public void onGalleryRequired(ProjectDTO project) {
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
        Intent w = new Intent(this, PhotoListActivity.class);
        w.putExtra("project", project);
        startActivity(w);
    }

    @Override
    public void onStatusReportRequired(ProjectDTO project) {
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
        Intent w = new Intent(this, StatusReportActivity.class);
        w.putExtra("project", project);
        w.putExtra("darkColor", themeDarkColor);
        startActivity(w);
    }

    @Override
    public void onMapRequired(ProjectDTO project) {
        SharedUtil.saveLastProjectID(ctx, project.getProjectID());
    }

    @Override
    public void onDestinationSelected(int position, String text) {
        Log.w(TAG, "onDestinationSelected: " + text);

        if (text.equalsIgnoreCase(ctx.getString(R.string.projects_on_map))) {

            Intent m = new Intent(this, ProjectMapActivity.class);
            ResponseDTO r = new ResponseDTO();
            r.setProjectList(projectsList);
            m.putExtra("projects", r);
            startActivity(m);
            return;
        }
        mPager.setCurrentItem(position, true);

    }

    /**
     * Adapter to manage fragments in view pager
     */
    private static class StaffPagerAdapter extends FragmentStatePagerAdapter {

        public StaffPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {

            return (Fragment) pageFragmentList.get(i);
        }

        @Override
        public int getCount() {
            return pageFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            PageFragment pf = pageFragmentList.get(position);
            return pf.getPageTitle();
        }
    }

    public void setRefreshActionButtonState(final boolean refreshing) {
        if (mMenu != null) {
            final MenuItem refreshItem = mMenu.findItem(R.id.action_refresh);
            if (refreshItem != null) {
                if (refreshing) {
                    refreshItem.setActionView(R.layout.action_bar_progess);
                } else {
                    refreshItem.setActionView(null);
                }
            }
        }
    }

    boolean mBound, rBound;
    PhotoUploadService mService;
    RequestSyncService rService;


    private ServiceConnection rConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.w(TAG, "## RequestSyncService ServiceConnection onServiceConnected");
            RequestSyncService.LocalBinder binder = (RequestSyncService.LocalBinder) service;
            rService = binder.getService();
            rBound = true;
            rService.startSyncCachedRequests(new RequestSyncService.RequestSyncListener() {
                @Override
                public void onTasksSynced(int goodResponses, int badResponses) {
                    Log.i(TAG, "## onTasksSynced, goodResponses: " + goodResponses + " badResponses: " + badResponses);
                }

                @Override
                public void onError(String message) {
                    Log.e(TAG, "Error with sync: " + message);
                }
            });

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w(TAG, "## RequestSyncService onServiceDisconnected");
            mBound = false;
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.w(TAG, "## PhotoUploadService ServiceConnection onServiceConnected");
            PhotoUploadService.LocalBinder binder = (PhotoUploadService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            mService.uploadCachedPhotos(new PhotoUploadService.UploadListener() {
                @Override
                public void onUploadsComplete(List<PhotoUploadDTO> list) {
                    Log.w(TAG, "$$$ onUploadsComplete, list: " + list.size());
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w(TAG, "## PhotoUploadService onServiceDisconnected");
            mBound = false;
        }
    };


    static final String TAG = StaffMainActivity.class.getSimpleName();
    static final int ACCURACY_THRESHOLD = 20;
    private DrawerLayout mDrawerLayout;
    StaffPagerAdapter adapter;
    Context ctx;
    int currentPageIndex;
    Location mCurrentLocation;
    ResponseDTO response;
    PagerTitleStrip strip;
    ViewPager mPager;
    static List<PageFragment> pageFragmentList;
    boolean mRequestingLocationUpdates;
    LocationRequest mLocationRequest;
    GoogleApiClient googleApiClient;
    ProgressBar progressBar;
    int themeDarkColor, themePrimaryColor, logo;
    boolean goToAlerts;
    ImageView navImage;
    TextView navText;
    NavigationView navigationView;
    Menu mMenu;
    CompanyDTO company;

    private void doPhotoSnack(int count) {
        createSnackBar(mPager, "Photos uploaded: " + count, "OK", "GREEN");
    }

    private void doMessageSnack(SimpleMessageDTO message) {
        createSnackBar(mPager, "Message received", "OK", "GREEN");
    }

    private class PhotoBroadcastReceiver extends android.content.BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int uploaded = intent.getIntExtra("uploaded", 0);
            Log.w(TAG, "onReceive: photos uploaded: " + uploaded);
            doPhotoSnack(uploaded);
        }
    }

    private class MessageBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            SimpleMessageDTO message = (SimpleMessageDTO) intent.getSerializableExtra("message");
            Log.w(TAG, "onReceive: message received ");
            doMessageSnack(message);
        }
    }

}
