package org.cm.podd.report.fragment;



import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.cm.podd.report.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReportLocationFragment#newInstance} factory method to
 * create an instance of this fragment.
 *
 */
public class ReportLocationFragment extends Fragment {

    private static final String ARG_REPORT_ID = "reportId";
    private static final int THRESHOLD = 1000 * 5;
    private static final String TAG = "ReportLocationFragment";
    private long reportId;

    private ProgressBar progressBar;
    private View locationInfo;
    public LocationManager locationManager;
    public Location previousBestLocation = null;


    private ReportNavigationInterface navigationInterface;

    private Timer timeoutTimer;
    private MyLocationListener listener;


    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param reportId Parameter 1.
     * @return A new instance of fragment ReportLocationFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportLocationFragment newInstance(long reportId) {
        ReportLocationFragment fragment = new ReportLocationFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_REPORT_ID, reportId);
        fragment.setArguments(args);
        return fragment;
    }
    public ReportLocationFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            reportId = getArguments().getLong(ARG_REPORT_ID);
        }
        navigationInterface.setPrevVisible(true);
        navigationInterface.setPrevEnable(true);
        navigationInterface.setNextEnable(false);

        timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finishLocation();
                    }
                });
            }
        }, 15000);



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report_location, container, false);
        progressBar = (ProgressBar) view.findViewById(R.id.report_location_progress_bar);
        locationInfo = view.findViewById(R.id.report_location_info);
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        navigationInterface = (ReportNavigationInterface) activity;

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        java.util.List providers = locationManager.getAllProviders();
        if (providers.contains(LocationManager.GPS_PROVIDER)) {
            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (isBetterLocation(loc, previousBestLocation)) {
                previousBestLocation = loc;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, THRESHOLD, 0, listener);

        } else {
            //TODO prompt user & use wellknown location
            Log.d(TAG, "no GPS provider found");

        }

    }

    @Override
    public void onDetach() {
        super.onDetach();
        navigationInterface = null;
        timeoutTimer.cancel();

        locationManager.removeUpdates(listener);
    }


    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > THRESHOLD;
        boolean isSignificantlyOlder = timeDelta < -THRESHOLD;
        boolean isNewer = timeDelta > 0;

        // If it's been more than THRESHOLD since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public class MyLocationListener implements LocationListener {

        public void onLocationChanged(final Location loc) {
            Log.i("**************************************", "Location changed");
            if (isBetterLocation(loc, previousBestLocation)) {
                previousBestLocation = loc;
                finishLocation();
            }
        }

        public void onProviderDisabled(String provider) {
//            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }


        public void onProviderEnabled(String provider) {
//            Toast.makeText(getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

    }

    private void finishLocation() {
        locationManager.removeUpdates(listener);
        timeoutTimer.cancel();

        navigationInterface.setNextEnable(true);
        progressBar.setVisibility(View.GONE);

        TextView latView = (TextView) locationInfo.findViewById(R.id.latitude_info);
        TextView longView = (TextView) locationInfo.findViewById(R.id.longitude_info);

        if (previousBestLocation != null) {

            latView.setText(Double.toString(previousBestLocation.getLatitude()));
            longView.setText(Double.toString(previousBestLocation.getLongitude()));

        } else {
            latView.setText("Can't detect location");
            longView.setText("Use default location");
        }


        locationInfo.setVisibility(View.VISIBLE);

    }
}
