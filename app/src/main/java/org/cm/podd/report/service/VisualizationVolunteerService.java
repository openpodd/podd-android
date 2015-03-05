package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.cm.podd.report.db.VisualizationVolunteerDataSource;
import org.cm.podd.report.model.VisualizationVolunteer;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

public class VisualizationVolunteerService extends IntentService {

    public static final String SYNC = "VisualizationVolunteer.sync";

    private static final String TAG = "VisualVolunteerService";

    public VisualizationVolunteerService() {
        super(VisualizationVolunteerService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent sendIntent = new Intent(SYNC);

        long userId = intent.getLongExtra("id", -99);
        int month = intent.getIntExtra("month", -99);
        int year = intent.getIntExtra("year", -99);

        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        String accessToken = sharedPrefUtil.getAccessToken();
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/users/volunteer/visualization/?month=" + month + "/" + year + "&userId=" + userId, null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            VisualizationVolunteerDataSource dbSource = new VisualizationVolunteerDataSource(this);
            VisualizationVolunteer origArea = dbSource.getFromVolunteerFromMonth(userId, month, year);

            try {
                JSONObject updateArea = new JSONObject(resp.getRawData());
                VisualizationVolunteer volunteer = requireAreaUpdate(updateArea, origArea);

                if (volunteer != null) {
                    Log.d(TAG, "Update volunteer id= " + volunteer.getId());

                    volunteer.setName(updateArea.optString("name"));
                    volunteer.setTotalReport(updateArea.optInt("totalReport"));
                    volunteer.setPositiveReport(updateArea.optInt("positiveReport"));
                    volunteer.setNegativeReport(updateArea.optInt("negativeReport"));
                    volunteer.setAnimalType(updateArea.optString("animalTypes"));
                    volunteer.setTimeRanges(updateArea.optString("timeRanges"));
                    volunteer.setGrade(updateArea.optString("grade"));
                    volunteer.setMonth(month);
                    volunteer.setYear(year);
                    dbSource.update(volunteer);
                }else{
                    Long visualVolunteerId = updateArea.optLong("id");
                    String name = updateArea.optString("name");

                    Log.d(TAG, "Found new volunteer id= " + visualVolunteerId);

                    volunteer = new VisualizationVolunteer(visualVolunteerId, name);
                    volunteer.setTotalReport(updateArea.optInt("totalReport"));
                    volunteer.setPositiveReport(updateArea.optInt("positiveReport"));
                    volunteer.setNegativeReport(updateArea.optInt("negativeReport"));
                    volunteer.setAnimalType(updateArea.optString("animalTypes"));
                    volunteer.setTimeRanges(updateArea.optString("timeRanges"));
                    volunteer.setGrade(updateArea.optString("grade"));
                    volunteer.setMonth(month);
                    volunteer.setYear(year);

                    dbSource.insert(volunteer);
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

    private VisualizationVolunteer requireAreaUpdate(JSONObject updateArea, VisualizationVolunteer volunteer) {
        VisualizationVolunteer found = null;

        Long updateVolunteerId = updateArea.optLong("id");
        String name = updateArea.optString("name");
        int totalReport = updateArea.optInt("totalReport");
        int positiveReport = updateArea.optInt("positiveReport");
        int negativeReport = updateArea.optInt("negativeReport");
        String animalTypes = updateArea.optString("animalTypes");
        String timeRanges = updateArea.optString("timeRanges");
        String grade = updateArea.optString("grade");
        int month = updateArea.optInt("month");
        int year = updateArea.optInt("year");

        if (volunteer != null && updateVolunteerId == volunteer.getId()) {
            found = volunteer;
            found.setName(name);
            found.setTotalReport(totalReport);
            found.setPositiveReport(positiveReport);
            found.setNegativeReport(negativeReport);
            found.setAnimalType(animalTypes);
            found.setTimeRanges(timeRanges);
            found.setGrade(grade);
            found.setMonth(month);
            found.setYear(year);

            if (!name.equals(volunteer.getName()) || totalReport != volunteer.getTotalReport() ||
                    positiveReport != volunteer.getPositiveReport() || negativeReport != volunteer.getNegativeReport() ||
                    !animalTypes.equals(volunteer.getAnimalType()) || !timeRanges.equals(volunteer.getTimeRanges()) ||
                    !grade.equals(volunteer.getGrade()) || month != volunteer.getMonth() || year != volunteer.getYear()){
                Log.d(TAG, "id:" + updateVolunteerId + ": update");

                found.setName(name);
                found.setTotalReport(totalReport);
                found.setPositiveReport(positiveReport);
                found.setNegativeReport(negativeReport);
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
