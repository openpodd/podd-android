package org.cm.podd.report.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.cm.podd.report.R;
import org.cm.podd.report.activity.VisualizationActivity;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class VisualizationAreaFragment extends Fragment {
    private static final String TAG = "VisualizationAreaFragment";

    private long  id;
    private String name;
    private String parentName;

    private int month;
    private int year;

    private PieChart mChart;

    public VisualizationAreaFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        id = getArguments().getInt("id", -99);
        name = getArguments().getString("name");
        parentName = getArguments().getString("parentName");

        month = getArguments().getInt("month", -99);
        year = getArguments().getInt("year", -9999);

        View view = inflater.inflate(R.layout.fragment_visualization_area, container, false);

        Typeface face = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);

        TextView textNameView = (TextView) view.findViewById(R.id.month_and_year);
        textNameView.setText("เดือน :" + month + "/" + year);
        textNameView.setTypeface(face, Typeface.BOLD);

        TextView textParentNameView = (TextView) view.findViewById(R.id.name);
        textParentNameView.setText(name + " " + parentName);
        textParentNameView.setTypeface(face, Typeface.BOLD);

        String grade = getArguments().getString("grade");
        TextView textGradeView =  (TextView) view.findViewById(R.id.grade);
        String textGrade = (String) textGradeView.getText();
        textGradeView.setText(textGrade + " : " + grade);
        textGradeView.setTypeface(face);

        int totalReport = getArguments().getInt("totalReport");
        TextView textTotalReportView =  (TextView) view.findViewById(R.id.totalReport);
        String textTotal = (String) textTotalReportView.getText();
        textTotalReportView.setText(textTotal + " : " + totalReport);
        textTotalReportView.setTypeface(face);

        int positiveReport = getArguments().getInt("positiveReport");
        int negativeReport = getArguments().getInt("negativeReport");

        mChart = (PieChart) view.findViewById(R.id.chart1);
        mChart.setDescription("");
        mChart.setValueTypeface(face);

        String [] nameChart = { getString(R.string.positive_report), getString(R.string.negative_report) };
        int [] countChart = { positiveReport, negativeReport };

        setData(mChart, nameChart, countChart);
        mChart.animateXY(500, 500);

        return view;
    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }

    private void setData(PieChart pieChart, String[] name, int[] count) {
        ArrayList<String> xValues = new ArrayList<String>();
        ArrayList<Entry> yValues = new ArrayList<Entry>();

        for (int i = 0; i < count.length; i++)
            yValues.add(new Entry((float) (count[i]), i));


        for (int i = 0; i < name.length; i++)
            xValues.add(name[i]);

        PieDataSet set = new PieDataSet(yValues, "");
        set.setSliceSpace(3f);

        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int c : ColorTemplate.PASTEL_COLORS)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());

        set.setColors(colors);

        PieData data = new PieData(xValues, set);
        pieChart.setData(data);
        pieChart.setHoleColor(getResources().getColor(R.color.action_bar_bg));
        pieChart.highlightValues(null);
        pieChart.invalidate();
    }

}
