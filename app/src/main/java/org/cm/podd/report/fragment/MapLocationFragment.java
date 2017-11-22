package org.cm.podd.report.fragment;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import org.cm.podd.report.R;
import org.cm.podd.report.util.StyleUtil;

/**
 *
 */
public class MapLocationFragment extends Fragment implements OnMapReadyCallback {


    public static final String ARGS_LATITUDE = "latitude";
    public static final String ARGS_LONGITUDE = "longitude";
    public static final String ARGS_EDITABLE = "editable";
    private MapView mapView;
    private double latitude;
    private double longitude;
    private boolean editable = true;
    private double originLatitude;
    private double originLongitude;

    private GoogleMap googleMap;
    private CheckBox changeLocationCb;
    private MenuItem mapType;

    private static final CharSequence[] MAP_TYPE_ITEMS =
            {"Road Map", "Hybrid", "Satellite", "Terrain"};


    public MapLocationFragment() {
        // Required empty public constructor
    }

    public static MapLocationFragment newInstance(double latitude, double longitude, boolean editable) {

        Bundle args = new Bundle();
        args.putDouble(ARGS_LATITUDE, latitude);
        args.putDouble(ARGS_LONGITUDE, longitude);
        args.putBoolean(ARGS_EDITABLE, editable);

        MapLocationFragment fragment = new MapLocationFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            this.latitude = args.getDouble(ARGS_LATITUDE, 0.0);
            this.longitude = args.getDouble(ARGS_LONGITUDE, 0.0);
            this.editable = args.getBoolean(ARGS_EDITABLE, true);

            originLatitude = latitude;
            originLongitude = longitude;
        }

        setHasOptionsMenu(true);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map_location, container, false);
        mapView = (MapView) view.findViewById(R.id.map_location);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        changeLocationCb = (CheckBox) view.findViewById(R.id.change_position_btn);
        changeLocationCb.setVisibility(editable ? View.VISIBLE : View.GONE);
        changeLocationCb.setTextAppearance(getContext(), R.style.ReportTextLabel);
        changeLocationCb.setTypeface(StyleUtil.getDefaultTypeface(getContext().getAssets(), Typeface.NORMAL));
        changeLocationCb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean value) {
                googleMap.getUiSettings().setAllGesturesEnabled(value);
                if (!value) {
                    // reset to default
                    latitude = originLatitude;
                    longitude = originLongitude;
                    moveCamera();
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mapType = menu.add("Map Type");
        mapType.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                showMapTypeSelectorDialog();
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (this.latitude != 0.0) {
            moveCamera();
        }
        googleMap.getUiSettings().setAllGesturesEnabled(false);
        googleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                latitude = cameraPosition.target.latitude;
                longitude = cameraPosition.target.longitude;
            }
        });
    }

    public boolean setLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;

        originLatitude = latitude;
        originLongitude = longitude;

        if (googleMap != null) {
            moveCamera();
            return true;
        }

        return false;
    }

    private void moveCamera() {
        LatLng position = new LatLng(latitude, longitude);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15));
    }


    public boolean isUserChangePosition() {
        return changeLocationCb.isChecked();
    }

    public LatLng getCustomPosition() {
        return googleMap.getCameraPosition().target;
    }

    private void showMapTypeSelectorDialog() {
        // Prepare the dialog by setting up a Builder.
        final String fDialogTitle = "Select Map Type";
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setTitle(fDialogTitle);

        // Find the current map type to pre-check the item representing the current state.
        int checkItem = googleMap.getMapType() - 1;

        // Add an OnClickListener to the dialog, so that the selection will be handled.
        builder.setSingleChoiceItems(
                MAP_TYPE_ITEMS,
                checkItem,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int item) {
                        // Locally create a finalised object.

                        // Perform an action depending on which item was selected.
                        switch (item) {
                            case 1:
                                googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                                break;
                            case 2:
                                googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                                break;
                            case 3:
                                googleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                                break;
                            default:
                                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        }
                        dialog.dismiss();
                    }
                }
        );

        // Build the dialog and show it.
        AlertDialog fMapTypeDialog = builder.create();
        fMapTypeDialog.setCanceledOnTouchOutside(true);
        fMapTypeDialog.show();
    }
}
