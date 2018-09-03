package org.cm.podd.report.db

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.cm.podd.report.BuildConfig
import org.cm.podd.report.PoddApplication
import org.cm.podd.report.R
import org.cm.podd.report.model.RecordData
import org.cm.podd.report.model.RecordSpec
import org.cm.podd.report.model.Report
import org.cm.podd.report.model.TemplateEvaluator
import org.cm.podd.report.util.SharedPrefUtil
import org.cm.podd.report.util.SingletonHolder
import org.jetbrains.anko.doAsync
import org.json.JSONObject


/**
 * Created by pphetra on 23/8/2018 AD.
 */
interface RecordDataSource {

    enum class Event {
        ADD, CHANGE, REMOVE, ALL
    }

    fun subscribe(datas: ArrayList<RecordData>, cb: (Event, Int)->Unit)

    fun persist(report : Report) : Unit

}

class PreferenceContext private constructor(context: Context) {
    val settings: SharedPreferences
    val sharedPrefUtil: SharedPrefUtil
    val serverUrl: String
    val accessToken: String
    val appContext: Context
    val username: String

    init {
        sharedPrefUtil = SharedPrefUtil(context)
        settings = PoddApplication.getAppContext().getSharedPreferences("PoddPrefsFile", 0);
        serverUrl = settings.getString("serverUrl", BuildConfig.SERVER_URL)
        accessToken = sharedPrefUtil.accessToken
        appContext = context
        username = sharedPrefUtil.userName
    }

    companion object : SingletonHolder<PreferenceContext, Context>(::PreferenceContext)
}

class FirebaseContext private constructor(preferenceContext: PreferenceContext) {
    val database: FirebaseDatabase
    val recordsRef: DatabaseReference
    val preferences: PreferenceContext
    var firebaseToken: String? = null
    val mAuth: FirebaseAuth
    var loginSuccess: Boolean = false

    init {
        database = FirebaseDatabase.getInstance()
        recordsRef = database.getReference("records");
        preferences = preferenceContext
        mAuth = FirebaseAuth.getInstance()
    }
    companion object : SingletonHolder<FirebaseContext, PreferenceContext>(::FirebaseContext)

    fun auth(activity: Activity, cb: (Boolean) -> Unit) {
        if (!loginSuccess) {

            doAsync {
                val client = OkHttpClient();
                val MIMEType = MediaType.parse("application/json; charset=utf-8")
                val requestBody = RequestBody.create(MIMEType, "{}")
                val request = Request.Builder()
                        .url("${preferences.serverUrl}/firebase/token/")
                        .post(requestBody)
                        .header("Authorization", "Token ${preferences.accessToken}")
                        .build()
                var response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body()!!.string()
                    val jsonResult = JSONObject(bodyString)

                    firebaseToken = jsonResult.getString("firebase_token")


                    mAuth.signInWithCustomToken(firebaseToken!!).addOnCompleteListener(activity) {task ->
                        loginSuccess = task.isSuccessful
                        cb(loginSuccess)
                        Log.d("xxxx", "login $loginSuccess")
                    }
                }
            }
        } else {
            cb(loginSuccess)
        }
    }



    fun recordDataSource(spec: RecordSpec, parentReportGuid: String?): RecordDataSource {
        return object: RecordDataSource {

            override fun subscribe(datas: ArrayList<RecordData>, cb: (type: RecordDataSource.Event, position: Int) -> Unit) {
                var q : Query
                if (spec.parentId != 0L) {
                    q = recordsRef.child(spec.id.toString())
                            .child(spec.groupKey).orderByChild("parentReportGuid")
                            .equalTo(parentReportGuid)
                } else {
                    q = recordsRef.child(spec.id.toString()).child(spec.groupKey).orderByKey()
                }
                q.addChildEventListener(object: ChildEventListener {
                    override fun onCancelled(error: DatabaseError) {

                    }

                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                    }

                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                        val value = snapshot.getValue(RecordData::class.java)
                        val idx = datas.indexOfFirst { item -> item.id == value?.id }
                        if (idx >= 0) {
                            datas[idx] = value!!
                        }
                        cb(RecordDataSource.Event.CHANGE, idx)
                    }

                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val value = snapshot.getValue(RecordData::class.java)
                        datas.add(value!!)
                        cb(RecordDataSource.Event.ADD, datas.size - 1)
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        val value = snapshot.getValue(RecordData::class.java)
                        if (value != null) {
                            val idx = datas.indexOfFirst { item -> item.id == value?.id }
                            datas.removeAt(idx)
                            cb(RecordDataSource.Event.REMOVE, idx)
                        }
                    }
                })
            }

            override fun persist(report: Report) {
                val jsonData = report.submitJSONFormData.toString()
                var header = TemplateEvaluator.instance.evaluate(
                        spec.tplHeader, jsonData
                )
                val subHeader = TemplateEvaluator.instance.evaluate(
                        spec.tplSubHeader, jsonData
                )

                if (report.isTestReport) {
                    header = "[${preferences.appContext.resources.getString(R.string.test_title)}] $header"
                }
                if (report.draft == 1) {
                    header = "[${preferences.appContext.resources.getString(R.string.draft)}] $header"
                }


                val data = RecordData(
                        report.guid,
                        header,
                        subHeader,
                        report.date.time,
                        report.guid,
                        if (parentReportGuid != null) parentReportGuid else "",
                        spec.id,
                        spec.parentId,
                        report.type,
                        preferences.username
                )

                val ref = recordsRef.child(spec.id.toString()).child(spec.groupKey).push()
                ref.setValue(data)
            }
        }
    }
}

