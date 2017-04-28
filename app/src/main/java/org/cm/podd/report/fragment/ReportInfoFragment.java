package org.cm.podd.report.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import org.cm.podd.report.R;
import org.cm.podd.report.TouchHighlightImageButton;
import org.cm.podd.report.activity.ImageActivity;
import org.cm.podd.report.activity.ReportViewActivity;
import org.cm.podd.report.model.FeedAdapter;
import org.cm.podd.report.util.DateUtil;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReportInfoFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ReportInfoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReportInfoFragment extends Fragment{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    private ScrollView scrollView;
    private View contentWrapper;

    private TextView dateView;
    private TextView incidentDateView;
    private TextView typeView;
    private TextView areaView;
    private TextView createdByView;
    private TextView createdByTelephoneView;
    private TextView createdByProjectTelephoneView;
    private TextView formDataExplanationView;

    private LinearLayout imageListView;
    private int mShortAnimationDuration;
    private SharedPrefUtil sharedPrefUtil;

    private MapView mapView;
    private LinearLayout mapButton;

    private GoogleMap mMap;
    private double latitude = -1;
    private double longitude = -1;

    private Animator mCurrentAnimator;
    private View rootView;

    private SupportMapFragment supportMapFragment;

    public ReportInfoFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReportInfoFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReportInfoFragment newInstance(String param1, String param2) {
        ReportInfoFragment fragment = new ReportInfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        ((ReportViewActivity)getActivity()).showProgressBar();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_report_info, null);
        FontUtil.overrideFonts(getContext(), view);
        rootView = view;

        contentWrapper = view.findViewById(R.id.report_view_content);

        dateView = (TextView) view.findViewById(R.id.report_view_report_date);
        incidentDateView = (TextView) view.findViewById(R.id.report_view_report_incidentDate);
        typeView = (TextView) view.findViewById(R.id.report_view_report_type);
        areaView = (TextView) view.findViewById(R.id.report_view_report_area);
        createdByView = (TextView) view.findViewById(R.id.report_view_report_created_by);
        createdByTelephoneView = (TextView) view.findViewById(R.id.report_view_reporter_telephone);
        createdByProjectTelephoneView = (TextView) view.findViewById(R.id.report_view_reporter_project_telephone);
        formDataExplanationView = (TextView) view.findViewById(R.id.report_view_form_data_explanation);

        // image list view.
        imageListView = (LinearLayout) view.findViewById(R.id.report_image_list);

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        sharedPrefUtil = new SharedPrefUtil(getContext());
        mapButton = (LinearLayout) view.findViewById(R.id.map_button);

        String _report = getArguments().getString("report");
        try {
            JSONObject report = new JSONObject(_report);
            dateView.setText(DateUtil.formatLocaleDateTime(
                    DateUtil.fromJsonDateString(report.getString("date"))));
            incidentDateView.setText(DateUtil.formatLocaleDate(
                    DateUtil.fromJsonDateString(report.getString("incidentDate"))));
            typeView.setText(report.getString("reportTypeName"));
            areaView.setText(report.getString("administrationAreaAddress"));
            createdByView.setText(report.getString("createdBy"));

            String telephone = report.getString("createdByTelephone");
            createdByTelephoneView.setText(linkify(telephone, "tel:" + telephone));
            createdByTelephoneView.setMovementMethod(LinkMovementMethod.getInstance());

            String projectTelephone = report.getString("createdByProjectMobileNumber");
            createdByProjectTelephoneView.setText(linkify(projectTelephone, "tel:" + projectTelephone));
            createdByProjectTelephoneView.setMovementMethod(LinkMovementMethod.getInstance());

            if (projectTelephone.equals("null")){
                LinearLayout projectTelephoneLayout = (LinearLayout) view.findViewById(R.id.reporter_project_telephone_layout);
                projectTelephoneLayout.setVisibility(View.GONE);
            }

            formDataExplanationView.setText(FeedAdapter.stripHTMLTags(
                    report.getString("formDataExplanation")));

            // Add image controller.
            JSONArray images;
            try {
                images = report.getJSONArray("images");

                View rootView = getView();

                for (int i = 0; i != images.length(); ++i) {
                    JSONObject item = images.getJSONObject(i);
                    final TouchHighlightImageButton imageView = new TouchHighlightImageButton(getContext());

                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    int widthRootView = getResources().getDisplayMetrics().widthPixels;
                    int widthMargin = StyleUtil.convertDpToPx(23.0F, getResources().getDisplayMetrics());

                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            widthRootView - 2 * widthMargin,
                            StyleUtil.convertDpToPx(157.50F, getResources().getDisplayMetrics()));
                    layoutParams.setMargins(0, 0,
                            StyleUtil.convertDpToPx(10F, getResources().getDisplayMetrics()), 0);
                    imageView.setLayoutParams(layoutParams);

                    final String imageUrl = item.getString("imageUrl");
                    imageView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // zoomImageFromThumb(imageView, imageUrl);
                            Intent intent = new Intent(getContext(), ImageActivity.class);
                            intent.putExtra("imagePath", imageUrl);
                            startActivity(intent);
                        }
                    });

                    Picasso.with(getContext())
                            .load(item.getString("thumbnailUrl"))
                            .fit()
                            .centerCrop()
                            .into(imageView);

                    imageListView.addView(imageView);
                }

                if (imageListView.getChildCount() == 0){
                    HorizontalScrollView horizontalScrollView = (HorizontalScrollView) view.findViewById(R.id.report_image_list_scrollview);
                    horizontalScrollView.setVisibility(View.GONE);
                }
            } catch (JSONException e) {
                // nothing.
            }

            JSONObject reportLocation = report.getJSONObject("reportLocation");

            if (reportLocation.getString("type").equalsIgnoreCase("Point")) {
                latitude = Double.parseDouble(reportLocation.getJSONArray("coordinates").get(1).toString());
                longitude = Double.parseDouble(reportLocation.getJSONArray("coordinates").get(0).toString());
                refreshMap();
            }
            mapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Uri gmmIntentUri = Uri.parse("geo:"+ latitude + "," + longitude + "?q="+ latitude + "," + longitude);

                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

        ((ReportViewActivity)getActivity()).hideProgressBar();

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
        supportMapFragment = (SupportMapFragment) fragmentManager.findFragmentById(R.id.map);
        supportMapFragment = SupportMapFragment.newInstance();
        fragmentManager.beginTransaction().replace(R.id.map, supportMapFragment).commit();
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                refreshMap();
            }
        });
    }

    private static Spanned linkify(String text, String uri) {
        return Html.fromHtml("<a href=\"" + uri + "\">" + text + "</a>");
    }

    public void refreshMap() {
        if (mMap == null) return;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(true);

        LatLng position = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(position));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 12));

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    /**
     * "Zooms" in a thumbnail view by assigning the high resolution image to a hidden "zoomed-in"
     * image view and animating its bounds to fit the entire activity content area. More
     * specifically:
     *
     * <ol>
     *   <li>Assign the high-res image to the hidden "zoomed-in" (expanded) image view.</li>
     *   <li>Calculate the starting and ending bounds for the expanded view.</li>
     *   <li>Animate each of four positioning/sizing properties (X, Y, SCALE_X, SCALE_Y)
     *       simultaneously, from the starting bounds to the ending bounds.</li>
     *   <li>Zoom back out by running the reverse animation on click.</li>
     * </ol>
     *
     * @param thumbView  The thumbnail view to zoom in.
     * @param imageUrl The high-resolution version of the image represented by the thumbnail.
     */
    private void zoomImageFromThumb(final View thumbView, String imageUrl) {
        // If there's an animation in progress, cancel it immediately and proceed with this one.
        if (mCurrentAnimator != null) {
            mCurrentAnimator.cancel();
        }

        // Load the high-resolution "zoomed-in" image.
        final ImageView expandedImageView = (ImageView) rootView.findViewById(R.id.expanded_image);
        new ReportViewActivity.RemoteImageAsyncTask() {
            @Override
            protected void onPostExecute(Drawable thumb) {
                expandedImageView.setImageDrawable(thumb);

                // Calculate the starting and ending bounds for the zoomed-in image. This step
                // involves lots of math. Yay, math.
                final Rect startBounds = new Rect();
                final Rect finalBounds = new Rect();
                final Point globalOffset = new Point();

                // The start bounds are the global visible rectangle of the thumbnail, and the
                // final bounds are the global visible rectangle of the container view. Also
                // set the container view's offset as the origin for the bounds, since that's
                // the origin for the positioning animation properties (X, Y).
                thumbView.getGlobalVisibleRect(startBounds);
                contentWrapper.getGlobalVisibleRect(finalBounds, globalOffset);
                startBounds.offset(-globalOffset.x, -globalOffset.y);
                finalBounds.offset(-globalOffset.x, -globalOffset.y);

                // Adjust the start bounds to be the same aspect ratio as the final bounds using the
                // "center crop" technique. This prevents undesirable stretching during the animation.
                // Also calculate the start scaling factor (the end scaling factor is always 1.0).
                float startScale;
                if ((float) finalBounds.width() / finalBounds.height()
                        > (float) startBounds.width() / startBounds.height()) {
                    // Extend start bounds horizontally
                    startScale = (float) startBounds.height() / finalBounds.height();
                    float startWidth = startScale * finalBounds.width();
                    float deltaWidth = (startWidth - startBounds.width()) / 2;
                    startBounds.left -= deltaWidth;
                    startBounds.right += deltaWidth;
                } else {
                    // Extend start bounds vertically
                    startScale = (float) startBounds.width() / finalBounds.width();
                    float startHeight = startScale * finalBounds.height();
                    float deltaHeight = (startHeight - startBounds.height()) / 2;
                    startBounds.top -= deltaHeight;
                    startBounds.bottom += deltaHeight;
                }

                // Hide the thumbnail and show the zoomed-in view. When the animation begins,
                // it will position the zoomed-in view in the place of the thumbnail.
                // thumbView.setAlpha(0f);
                expandedImageView.setVisibility(View.VISIBLE);

                // Set the pivot point for SCALE_X and SCALE_Y transformations to the top-left corner of
                // the zoomed-in view (the default is the center of the view).
                expandedImageView.setPivotX(0f);
                expandedImageView.setPivotY(0f);

                // Construct and run the parallel animation of the four translation and scale properties
                // (X, Y, SCALE_X, and SCALE_Y).
                AnimatorSet set = new AnimatorSet();
                set
                        .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left,
                                finalBounds.left))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top,
                                finalBounds.top))
                        .with(ObjectAnimator.ofInt(expandedImageView, "backgroundColor", 0x00000000,
                                0xDD000000))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
                set.setDuration(mShortAnimationDuration);
                set.setInterpolator(new DecelerateInterpolator());
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mCurrentAnimator = null;
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        mCurrentAnimator = null;
                    }
                });
                set.start();
                mCurrentAnimator = set;

                // Upon clicking the zoomed-in image, it should zoom back down to the original bounds
                // and show the thumbnail instead of the expanded image.
                final float startScaleFinal = startScale;
                expandedImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mCurrentAnimator != null) {
                            mCurrentAnimator.cancel();
                        }

                        // Animate the four positioning/sizing properties in parallel, back to their
                        // original values.
                        AnimatorSet set = new AnimatorSet();
                        set
                                .play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                                .with(ObjectAnimator
                                        .ofFloat(expandedImageView, View.SCALE_X, startScaleFinal))
                                .with(ObjectAnimator
                                        .ofFloat(expandedImageView, View.SCALE_Y, startScaleFinal));
                        set.setDuration(mShortAnimationDuration);
                        set.setInterpolator(new DecelerateInterpolator());
                        set.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                thumbView.setAlpha(1f);
                                expandedImageView.setVisibility(View.GONE);
                                mCurrentAnimator = null;
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {
                                thumbView.setAlpha(1f);
                                expandedImageView.setVisibility(View.GONE);
                                mCurrentAnimator = null;
                            }
                        });
                        set.start();
                        mCurrentAnimator = set;
                    }
                });
            }
        }.execute(imageUrl);
    }


}
