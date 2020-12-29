package org.cm.podd.report.activity

import android.content.Context
import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ListView
import org.cm.podd.report.R
import org.cm.podd.report.db.ReportTypeDataSource
import org.cm.podd.report.model.ReportType
import org.cm.podd.report.util.SharedPrefUtil

class FeedFilterReportTypeActivity : AppCompatActivity() {

    private lateinit var reportTypeDataSource: ReportTypeDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feed_filter_report_type)
        val listView = findViewById<ListView>(R.id.feedFilterReportTypeList)

        reportTypeDataSource = ReportTypeDataSource(this)
        val reportTypes = reportTypeDataSource.all
        val adapter = FilterReportTypeAdapter(this, R.layout.filter_report_type_row, reportTypes)


        listView.adapter = adapter

        val context = this
        val confirmButton = findViewById<Button>(R.id.confirmFeedFilterReportType)
        confirmButton.setOnClickListener {
            val names = adapter.selectedItems.map { reportType -> reportType.name }
            val sharedPrefUtil = SharedPrefUtil(context)
            sharedPrefUtil.filterReportType = TextUtils.join(",", names)
            finish()
        }

    }

    override fun onDestroy() {
        reportTypeDataSource.close()
        super.onDestroy()
    }

    class FilterReportTypeAdapter(context: Context,
                                  @LayoutRes private val resource: Int,
                                  private val values: List<ReportType>)
        : ArrayAdapter<ReportType> (context, resource, values) {

        val selectedItems = HashSet<ReportType>()

        init {
            val sharedPrefUtil = SharedPrefUtil(context)
            sharedPrefUtil.filterReportType.split(",").forEach {name ->
                val reportType = values.find { reportType -> reportType.name == name }
                if (reportType != null) {
                    selectedItems.add(reportType)
                }
            }
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)
            val checkbox = view.findViewById<CheckBox>(R.id.reportTypeCheckbox)
            val item = getItem(position)
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    item?.let { selectedItems.add(item) }
                } else {
                    item?.let { selectedItems.remove(item) }
                }
            }
            checkbox.text = item?.name
            item?.let { checkbox.isChecked = selectedItems.contains(item) }
            return view
        }
    }
}
