package org.cm.podd.report.fragment;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.cm.podd.report.R;
import org.cm.podd.report.model.AnimalType;
import org.cm.podd.report.model.TimeRange;
import org.cm.podd.report.util.FontUtil;
import org.cm.podd.report.util.StyleUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public class VisualizationFragment extends Fragment {
    private static final String TAG = "VisualizationAreaFragment";

    private long  id;
    private String name;
    private String parentName;

    private int month;
    private int year;

    private PieChart mChartTypeReport;
    private BarChart mChartTypeAnimals;

    public VisualizationFragment() {
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
        if(parentName.equalsIgnoreCase("null")) parentName = "";

        month = getArguments().getInt("month", -99);
        year = getArguments().getInt("year", -9999);

        View view = inflater.inflate(R.layout.fragment_visualization, container, false);

        Typeface face = StyleUtil.getDefaultTypeface(getActivity().getAssets(), Typeface.NORMAL);

        TextView textNameView = (TextView) view.findViewById(R.id.month_and_year);
        textNameView.setText(getString(R.string.visual_month) + " " + month + "/" + year);
        textNameView.setTypeface(face);

        TextView textParentNameView = (TextView) view.findViewById(R.id.name);
        textParentNameView.setText(name + " " + parentName);
        textParentNameView.setTypeface(face);

        TextView emptyReportView =  (TextView) view.findViewById(R.id.emptyReport);
        TextView emptyAnimalView =  (TextView) view.findViewById(R.id.emptyAnimalType);

        emptyReportView.setTypeface(face);
        emptyAnimalView.setTypeface(face);

        /* set total report */
        int totalReport = getArguments().getInt("totalReport");
        TextView valTotalReportView =  (TextView) view.findViewById(R.id.valTotalReport);
        valTotalReportView.setText(totalReport + "");
        valTotalReportView.setTypeface(face);

        /* set graph type report */
        if (totalReport == 0){
            mChartTypeReport = (PieChart) view.findViewById(R.id.chartReport);;
            mChartTypeReport.setVisibility(View.GONE);
        }else{
            TextView emptyAnimalType = (TextView) view.findViewById (R.id.emptyReport);
            emptyAnimalType.setVisibility(View.GONE);

            int positiveReport = getArguments().getInt("positiveReport");
            int negativeReport = getArguments().getInt("negativeReport");

            mChartTypeReport = (PieChart) view.findViewById(R.id.chartReport);
            mChartTypeReport.setDescription("");
            mChartTypeReport.setValueTypeface(face);

            String [] nameChart = { getString(R.string.positive_report), getString(R.string.negative_report) };
            int [] countChart = { positiveReport, negativeReport };

            setDataReportTypePieChart(mChartTypeReport, ColorTemplate.COLORFUL_COLORS, nameChart, countChart);
            mChartTypeReport.animateXY(500, 500);
            mChartTypeReport.setValueTypeface(face);
        }

        /* set animal type */
        ArrayList<AnimalType> animalTypes = new ArrayList<AnimalType>();
        try {
            JSONArray items = new JSONArray(getArguments().getString("animalTypes"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                String name = item.optString("name");
                int sickCount = item.optInt("sick");
                int deathCount = item.optInt("death");

                AnimalType animalType = new AnimalType(name, sickCount, deathCount);
                animalTypes.add(animalType);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (animalTypes.size() == 0){
            mChartTypeAnimals = (BarChart) view.findViewById(R.id.chartAnimal);
            mChartTypeAnimals.setVisibility(View.GONE);
        }else{
            TextView emptyAnimalType = (TextView) view.findViewById (R.id.emptyAnimalType);
            emptyAnimalType.setVisibility(View.GONE);

            mChartTypeAnimals = (BarChart) view.findViewById(R.id.chartAnimal);
            mChartTypeAnimals.setDescription("");

            setDataAnimalTypeBarChart(mChartTypeAnimals, animalTypes);
            mChartTypeAnimals.animateXY(500, 500);
            mChartTypeAnimals.setValueTypeface(face);
        }
        /* set time range */
        TimeRange maxReportTimeRanges = new TimeRange(0, 0, 0);
        try {
            JSONArray items = new JSONArray(getArguments().getString("timeRanges"));
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                int startTime = item.optInt("startTime");
                int endTime = item.optInt("endTime");
                int total = item.optInt("totalReport");

                if (total >= maxReportTimeRanges.getTotalReport()){
                    maxReportTimeRanges = new TimeRange(startTime, endTime, total);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        String timeRange = maxReportTimeRanges.toString();
        TextView valTimeRangeView =  (TextView) view.findViewById(R.id.valReportTimeRange);
        valTimeRangeView.setText(timeRange);
        valTimeRangeView.setTypeface(face);

        /* set grade */
        String grade = getArguments().getString("grade");
        TextView valGradeView =  (TextView) view.findViewById(R.id.valGrade);
        valGradeView.setText(getGrade(grade));
        valGradeView.setTypeface(face);

        FontUtil.overrideFonts(getActivity(), view);
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

    private void setDataReportTypePieChart(PieChart pieChart, int[] colorTemplate, String[] name, int[] count) {
        ArrayList<String> xValues = new ArrayList<String>();
        ArrayList<Entry> yValues = new ArrayList<Entry>();

        for (int i = 0; i < count.length; i++)
            yValues.add(new Entry((float) (count[i]), i));


        for (int i = 0; i < name.length; i++)
            xValues.add(name[i]);

        PieDataSet set = new PieDataSet(yValues, "");
        set.setSliceSpace(3f);

        ArrayList<Integer> colors = new ArrayList<Integer>();

        for (int c : colorTemplate)
            colors.add(c);

        colors.add(ColorTemplate.getHoloBlue());

        set.setColors(colors);

        PieData data = new PieData(xValues, set);
        pieChart.setData(data);
        pieChart.setHoleColor(getResources().getColor(R.color.action_bar_bg));
        pieChart.highlightValues(null);
        pieChart.invalidate();
    }

    private void setDataAnimalTypeBarChart(BarChart barChart, ArrayList<AnimalType> dataGroup) {

        ArrayList<String> xValues = new ArrayList<String>();
        for (int i = 0; i < dataGroup.size(); i++) {
            xValues.add(dataGroup.get(i).getName());
        }

        ArrayList<BarEntry> yValues1 = new ArrayList<BarEntry>();
        ArrayList<BarEntry> yValues2 = new ArrayList<BarEntry>();

        for (int i = 0; i < dataGroup.size(); i++) {
            yValues1.add(new BarEntry(dataGroup.get(i).getSickCount(), i));
        }

        for (int i = 0; i < dataGroup.size(); i++) {
            yValues2.add(new BarEntry(dataGroup.get(i).getDeathCount(), i));
        }

        BarDataSet set1 = new BarDataSet(yValues1, getString(R.string.animal_sick));
        set1.setColor(getResources().getColor(R.color.graph_animal_sick));

        BarDataSet set2 = new BarDataSet(yValues2, getString(R.string.animal_death));
        set2.setColor(getResources().getColor(R.color.graph_animal_death));

        ArrayList<BarDataSet> dataSets = new ArrayList<BarDataSet>();
        dataSets.add(set1);
        dataSets.add(set2);

        BarData data = new BarData(xValues, dataSets);

        data.setGroupSpace(80f);

        barChart.setData(data);
        barChart.invalidate();
    }

    private String getGrade(String grade){
        if(grade.equals("A"))
            return getString(R.string.grade_A);
        else if (grade.equals("B"))
            return getString(R.string.grade_B);
        return getString(R.string.grade_C);
    }

}
