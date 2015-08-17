package org.cm.podd.report.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by sudarat on 8/4/15 AD.
 */
public class FollowAlertDataSource {

    private static final String TAG = "FollowAlertDataSource";
    private ReportDatabaseHelper reportDatabaseHelper;

    private int STATUS_ALERT = 0;
    private int STATUS_DONE = 1;

    public FollowAlertDataSource(Context context) {
        reportDatabaseHelper = new ReportDatabaseHelper(context);
    }

    public void close() {
        reportDatabaseHelper.close();
    }


    public long createFollowAlert(long reportId, int triggerNo, String message, int requestCode, long date, long reportTypeId) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("report_id", reportId);
        values.put("trigger_no", triggerNo);
        values.put("message", message);
        values.put("status", STATUS_ALERT);
        values.put("request_code", requestCode);
        values.put("date", date);
        values.put("report_type", reportTypeId);

        Log.d(TAG, String.format("create follow_alert with report_id = %d, trigger_no = %d, request_code = %d", reportId, triggerNo, requestCode));
        long id = db.insert("follow_alert", null, values);
        db.close();
        return id;
    }

    public Cursor getAll() {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM follow_alert order by _id desc", null);
    }

    public Cursor getUnDone() {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        return db.rawQuery("SELECT * FROM follow_alert where status = 0 order by _id desc", null);
    }

    public int getTriggerNoByNow1Day(long reportId, long nextDay) {
        int triggerNo = -1;
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();

        Calendar cal = Calendar.getInstance();
        Cursor cursor = db.rawQuery("select min(trigger_no) trigger_no from follow_alert where report_id=? and date >= ? and date < ?",
                new String[]{Long.toString(reportId), cal.getTimeInMillis() + "", Long.toString(nextDay)});

        while (cursor.moveToNext()) {
            triggerNo = cursor.getInt(cursor.getColumnIndex("trigger_no"));
        }
        cursor.close();
        db.close();
        return triggerNo;
    }

    public List<Integer> getRequestCodes(long reportId, int triggerNo) {
        SQLiteDatabase db = reportDatabaseHelper.getReadableDatabase();
        ArrayList<Integer> requestCodes = new ArrayList<Integer>();

        Cursor cursor = db.rawQuery("select * from follow_alert where report_id= ?"
                + " and trigger_no=? order by _id asc", new String[]{Long.toString(reportId), Integer.toString(triggerNo)});

        while (cursor.moveToNext()) {
            int request_code = cursor.getInt(cursor.getColumnIndex("request_code"));
            requestCodes.add(request_code);
        }
        cursor.close();
        db.close();
        return requestCodes;
    }


    public void updateStatusDone(long reportId, int triggerNo) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("status", STATUS_DONE);
        db.update("follow_alert", values, "report_id = ? and trigger_no = ?", new String[]{Long.toString(reportId), Integer.toString(triggerNo)});
        db.close();
    }

    public void deleteFollowAlert(long _id) {
        SQLiteDatabase db = reportDatabaseHelper.getWritableDatabase();
        db.delete("follow_alert", "_id = ?", new String[]{Long.toString(_id)});
        db.close();
    }

}
