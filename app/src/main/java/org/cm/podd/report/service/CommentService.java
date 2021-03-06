package org.cm.podd.report.service;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.cm.podd.report.db.CommentDataSource;
import org.cm.podd.report.model.Comment;
import org.cm.podd.report.util.RequestDataUtil;
import org.cm.podd.report.util.SharedPrefUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CommentService extends IntentService {

    public static final String SYNC = "Comment.sync";

    private static final String TAG = "CommentService";

    public CommentService() {
        super(CommentService.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long reportId = intent.getLongExtra("reportId", -99);

        SharedPrefUtil sharedPrefUtil = new SharedPrefUtil(getApplicationContext());
        String accessToken = sharedPrefUtil.getAccessToken();
        RequestDataUtil.ResponseObject resp = RequestDataUtil.get(
                "/reportComments/?reportId=" + reportId, null, accessToken);

        if (resp.getStatusCode() == HttpURLConnection.HTTP_OK) {
            CommentDataSource dbSource = new CommentDataSource(this);
            List<Comment> origComment = dbSource.getAllFromReport(reportId);

            ArrayList<Long> removeIds = new ArrayList<Long>();
            for (Comment comment : origComment) {
                removeIds.add(comment.getId());
            }

            try {
                JSONArray items = new JSONArray(resp.getRawData());

                for (int i = 0; i < items.length(); i++) {
                    JSONObject updateComment = items.getJSONObject(i);
                    removeIds.remove(new Long(updateComment.optInt("id")));

                    Comment comment = requireAreaUpdate(updateComment, origComment);

                    if (comment != null) {
                        Log.d(TAG, "Update comment id= " + comment.getId());


                        comment.setReportId(updateComment.optLong("reportId"));
                        comment.setMessage(updateComment.optString("message"));
                        comment.setFileUrl(updateComment.optString("fileUrl"));
                        String avatarCreatedBy = "";
                        String createdBy = "";

                        try {
                            JSONObject jsonCreatedBy = new JSONObject(updateComment.optString("createdBy"));
                            createdBy = jsonCreatedBy.getString("firstName") + " " + jsonCreatedBy.getString("lastName");
                            avatarCreatedBy = jsonCreatedBy.getString("thumbnailAvatarUrl");
                        }catch (Exception ex){}

                        comment.setAvatarCreatedBy(avatarCreatedBy);
                        comment.setCreatedBy(createdBy);

                        comment.setCreatedAt(updateComment.optString("createdAt"));

                        dbSource.update(comment);
                    }else{
                        long commentId = updateComment.optInt("id");
                        String message = updateComment.optString("message");
                        String fileUrl = updateComment.optString("fileUrl");
                        String avatarCreatedBy = "";
                        String createdBy = "";

                        try {
                            JSONObject jsonCreatedBy = new JSONObject(updateComment.optString("createdBy"));
                            createdBy = jsonCreatedBy.getString("firstName") + " " + jsonCreatedBy.getString("lastName");
                            avatarCreatedBy = jsonCreatedBy.getString("thumbnailAvatarUrl");
                        }catch (Exception ex){}


                        String createdAt = updateComment.optString("createdAt");

                        Log.d(TAG, "Found new comment id= " + commentId);

                        comment = new Comment(commentId, reportId, message, avatarCreatedBy, fileUrl, createdBy, createdAt);
                        dbSource.insert(comment);
                    }
                }

            } catch (JSONException e) {
                Log.e(TAG, e.getMessage(), e);
            }

            for (Long id : removeIds) {
                dbSource.removeAdministrationArea(id);
            }

            dbSource.close();
        } else {
            Log.e(TAG, "Server error");
        }
        sendBroadcast(new Intent(SYNC));
    }

    private Comment requireAreaUpdate(JSONObject updateComment, List<Comment> comments) {
        Comment found = null;
        Iterator<Comment> iterator = comments.iterator();

        while (iterator.hasNext() && found == null) {
            Comment comment = iterator.next();
            long reportId = updateComment.optLong("reportId");
            String message = updateComment.optString("message");
            String fileUrl = updateComment.optString("fileUrl");
            String avatarCreatedBy = "";
            String createdBy = "";

            try {
                JSONObject jsonCreatedBy = new JSONObject(updateComment.optString("createdBy"));
                createdBy = jsonCreatedBy.getString("firstName") + " " + jsonCreatedBy.getString("lastName");
                avatarCreatedBy = jsonCreatedBy.getString("thumbnailAvatarUrl");
            }catch (Exception ex){}

            String createdAt = updateComment.optString("createdAt");

            long updateCommentId = updateComment.optInt("id");

            if (updateCommentId == comment.getId()) {
                found = comment;
                found.setReportId(comment.getReportId());
                found.setMessage(comment.getMessage());
                found.setFileUrl(comment.getFileUrl());
                found.setAvatarCreatedBy(comment.getAvatarCreatedBy());
                found.setCreatedBy(comment.getCreatedBy());
                found.setCreatedAt(comment.getCreatedAt());

                if (!message.equals(comment.getMessage()) || !fileUrl.equals(comment.getFileUrl()) ||
                        !createdBy.equals(comment.getCreatedBy()) || !createdAt.equals(comment.getCreatedAt()) ||
                        !avatarCreatedBy.equals(comment.getAvatarCreatedBy())) {
                    Log.d(TAG, "id:" + updateCommentId + ": update");

                    found.setReportId(reportId);
                    found.setMessage(message);
                    found.setFileUrl(fileUrl);
                    found.setAvatarCreatedBy(avatarCreatedBy);
                    found.setCreatedBy(createdBy);
                    found.setCreatedAt(createdAt);
                }
            }
        }
        return found;
    }
}
