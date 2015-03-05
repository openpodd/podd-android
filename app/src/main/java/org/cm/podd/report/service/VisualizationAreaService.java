package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.cm.podd.report.activity.VisualizationAreaActivity;
import org.cm.podd.report.db.VisualizationAreaDataSource;
import org.cm.podd.report.model.VisualizationAdministrationArea;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class VisualizationAreaService extends IntentService {

    public static final String SYNC = "VisualizationArea.sync";

    private static final String TAG = "VisualAreaService";

    public VisualizationAreaService() {
        super(VisualizationAreaService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent sendIntent = new Intent(SYNC);

        long areaId = intent.getLongExtra("id", -99);
        int month = intent.getIntExtra("month", -99);
        int year = intent.getIntExtra("year", -99);

        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        String accessToken = sharedPrefUtil.getAccessToken();
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/summary/areas/show-detail/?month=" + month + "/" + year + "&administrationAreaId=" + areaId, null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            VisualizationAreaDataSource dbSource = new VisualizationAreaDataSource(this);
            VisualizationAdministrationArea origArea = dbSource.getFromAreaByMonth(areaId, month, year);

            try {
                JSONObject updateArea = new JSONObject(resp.getRawData());
                VisualizationAdministrationArea area = requireAreaUpdate(updateArea, origArea);

                if (area != null) {
                    Log.d(TAG, "Update area id= " + area.getId());

                    area.setName(updateArea.optString("name"));
                    area.setParentName(updateArea.optString("parentName"));
                    area.setTotalReport(updateArea.optInt("totalReport"));
                    area.setPositiveReport(updateArea.optInt("positiveReport"));
                    area.setNegativeReport(updateArea.optInt("negativeReport"));
                    area.setVolunteers(updateArea.optString("reporters"));
                    area.setAnimalType(updateArea.optString("animalTypes"));
                    area.setTimeRanges(updateArea.optString("timeRanges"));
                    area.setGrade(updateArea.optString("grade"));
                    area.setMonth(month);
                    area.setYear(year);
                    dbSource.update(area);
                }else{
                    Long visualAreaId = updateArea.optLong("id");
                    String name = updateArea.optString("name");
                    String parentName = updateArea.optString("parentName");

                    Log.d(TAG, "Found new area id= " + visualAreaId);

                    area = new VisualizationAdministrationArea(visualAreaId, name, parentName);
                    area.setTotalReport(updateArea.optInt("totalReport"));
                    area.setPositiveReport(updateArea.optInt("positiveReport"));
                    area.setNegativeReport(updateArea.optInt("negativeReport"));
                    area.setVolunteers(updateArea.optString("reporters"));
                    area.setAnimalType(updateArea.optString("animalTypes"));
                    area.setTimeRanges(updateArea.optString("timeRanges"));
                    area.setGrade(updateArea.optString("grade"));
                    area.setMonth(month);
                    area.setYear(year);

                    dbSource.insert(area);
                }

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
                sendIntent.putExtra("error", e.getMessage());
            }
            dbSource.close();
        } else {
            Log.e(TAG, "Server error");
            sendIntent.putExtra("error", "Server error");
        }
        sendBroadcast(sendIntent);
    }

    private VisualizationAdministrationArea requireAreaUpdate(JSONObject updateArea, VisualizationAdministrationArea area) {
        VisualizationAdministrationArea found = null;

        Long updateAreaId = updateArea.optLong("id");
        String name = updateArea.optString("name");
        String parentName = updateArea.optString("parentName");
        int totalReport = updateArea.optInt("totalReport");
        int positiveReport = updateArea.optInt("positiveReport");
        int negativeReport = updateArea.optInt("negativeReport");
        String volunteers = updateArea.optString("reporters");
        String animalTypes = updateArea.optString("animalTypes");
        String timeRanges = updateArea.optString("timeRanges");
        String grade = updateArea.optString("grade");
        int month = updateArea.optInt("month");
        int year = updateArea.optInt("year");

        if (area != null && updateAreaId == area.getId()) {
            found = area;
            found.setName(name);
            found.setParentName(parentName);
            found.setTotalReport(totalReport);
            found.setPositiveReport(positiveReport);
            found.setNegativeReport(negativeReport);
            found.setVolunteers(volunteers);
            found.setAnimalType(animalTypes);
            found.setTimeRanges(timeRanges);
            found.setGrade(grade);
            found.setMonth(month);
            found.setYear(year);

            if (!name.equals(area.getName()) || !parentName.equals(area.getParentName()) ||
                    totalReport != area.getTotalReport() || positiveReport != area.getPositiveReport() ||
                    negativeReport != area.getNegativeReport() || !volunteers.equals(area.getVolunteers()) ||
                    !animalTypes.equals(area.getAnimalType()) || !timeRanges.equals(area.getTimeRanges()) ||
                    !grade.equals(area.getGrade()) || month != area.getMonth() || year != area.getYear()){
                Log.d(TAG, "id:" + updateAreaId + ": update");

                found.setName(name);
                found.setParentName(parentName);
                found.setTotalReport(totalReport);
                found.setPositiveReport(positiveReport);
                found.setNegativeReport(negativeReport);
                found.setVolunteers(volunteers);
                found.setAnimalType(animalTypes);
                found.setTimeRanges(timeRanges);
                found.setGrade(grade);
                found.setMonth(month);
                found.setYear(year);
            }
        }
        return found;
    }
}
