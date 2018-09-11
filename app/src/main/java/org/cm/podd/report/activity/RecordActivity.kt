package org.cm.podd.report.activity

import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.android.synthetic.main.record_activity.*
import kotlinx.android.synthetic.main.record_list_item.view.*
import org.cm.podd.report.R
import org.cm.podd.report.db.*
import org.cm.podd.report.model.RecordData
import org.cm.podd.report.model.RecordSpec
import org.cm.podd.report.util.StyleUtil


/**
 * Created by pphetra on 21/8/2018 AD.
 */
class RecordActivity : AppCompatActivity() {
    val TAG = "RecordActivity"

    private var recordSpecId: Long = -1
    private var recordSpec: RecordSpec? = null
    private lateinit var  recordSpecDataSource : RecordSpecDataSource
    private var recordDataSource : RecordDataSource? = null
    private lateinit var  reportDataSource : ReportDataSource
    private lateinit var firebaseContext: FirebaseContext
    private val recordDatas = ArrayList<RecordData>()
    private val recordAdapter = RecordListAdapter(recordDatas)
    private var parentRecord: RecordData? = null
    private var searchView : SearchView? = null

    private var parentReportGuid: String? = null

    var hasChildRecordSpec = false

    private val REQUEST_NEW_REPORT: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.record_activity)

        empty_view.typeface = StyleUtil.getDefaultTypeface(assets, Typeface.NORMAL);

        recordSpecDataSource = RecordSpecDataSource.getInstance(this)
        reportDataSource = ReportDataSource(this)

        recordSpecId = intent.getLongExtra("recordSpecId", 0)
        val tmp = intent.getSerializableExtra("parentRecord")
        if (tmp != null) {
            parentRecord =  tmp as RecordData

        }
        recordSpec = recordSpecDataSource[recordSpecId]
        hasChildRecordSpec = recordSpecDataSource.findByParentId(recordSpecId).isNotEmpty()

        parentReportGuid = intent.getStringExtra("parentReportGuid")

        firebaseContext = FirebaseContext.getInstance(PreferenceContext.getInstance(applicationContext))
        firebaseContext.auth(this) { success ->
            Log.d(TAG, "login $success")
            recordDataSource = firebaseContext.recordDataSource(recordSpec!!, parentReportGuid)

            recordDataSource?.subscribe(recordDatas) { type: RecordDataSource.Event, position: Int ->
                when (type) {
                    RecordDataSource.Event.ADD -> recordAdapter.notifyItemInserted(position)
                    RecordDataSource.Event.CHANGE -> recordAdapter.notifyItemChanged(position)
                    RecordDataSource.Event.REMOVE -> recordAdapter.notifyItemRemoved(position)
                }
            }
        }

        recycleView.layoutManager = LinearLayoutManager(this)
        recycleView.adapter = recordAdapter
        recordAdapter.listener = object: RecordListAdapterListener {
            override fun onZoomIn(record: RecordData) {
                zoomInRecord(record)
            }

            override fun onSelected(record: RecordData) {
                selectRecord(record)
            }
        }

        recordAdapter.registerAdapterDataObserver(object: RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                checkEmpty()
            }

            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                checkEmpty()
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                checkEmpty()
            }

            fun checkEmpty() {
                empty_view.visibility = if (recordAdapter.itemCount == 0) View.VISIBLE else View.GONE
            }
        })



        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = recordSpec!!.name

        addNewRecord.setOnClickListener{ _ ->
            if (parentReportGuid != null) {
                val preloadFormData = parentRecord?.formData ?: "{}"
                startActivityForResult(
                        ReportActivity.followReportFromRecord(this, parentReportGuid, recordSpec!!.typeId, preloadFormData),
                        REQUEST_NEW_REPORT)
            } else {
                startActivityForResult(
                        ReportActivity.newReportIntent(this, recordSpec!!.typeId, false),
                        REQUEST_NEW_REPORT)
            }
        }
    }

    private fun zoomInRecord(record: RecordData) {
        Log.d(TAG, "record --> $record.name was zoomin")
        val specs = recordSpecDataSource.findByParentId(recordSpecId)

        // fix
        val intent = Intent(this, RecordActivity::class.java)
        intent.putExtra("recordSpecId", specs[0].id)
        intent.putExtra("parentReportGuid", record.reportGuid)
        intent.putExtra("parentRecord", record)
        startActivityForResult(intent, 0)
    }

    private fun selectRecord(record: RecordData) {
        Log.d(TAG, "record --> $record.name was selected")
    }

    interface RecordListAdapterListener {
        fun onSelected(record: RecordData)

        fun onZoomIn(record: RecordData)
    }

    inner class RecordListAdapter(var items: List<RecordData>)
        : RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {

        val view_type_footer = 1
        val view_type_content = 0

        var filteredItems: List<RecordData> = items
        var listener: RecordListAdapterListener? = null

        inner class RecordViewHolder(card: View) : RecyclerView.ViewHolder(card) {
            val header: TextView = card.header
            val subHeader: TextView = card.subheader
            val nextBtn: ImageButton = card.nextBtn

            init {
                header.typeface = StyleUtil.getDefaultTypeface(card.context.assets, Typeface.NORMAL)
                subHeader.typeface = StyleUtil.getDefaultTypeface(card.context.assets, Typeface.NORMAL)
                nextBtn.setOnClickListener {
                    val record = filteredItems[adapterPosition]
                    listener?.onZoomIn(record)
                }

            }
        }

        inner class FooterViewHolder(view: View): RecyclerView.ViewHolder(view) {

        }

        override fun getItemViewType(position: Int): Int {
            if (position == filteredItems.size) {
                return view_type_footer
            }
            return view_type_content
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            if (viewType == view_type_content) {
                return RecordViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.record_list_item, parent, false))
            }
            return FooterViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.record_footer_item, parent, false))
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
            Log.d("debug", "onbind at $position")
            if (holder != null) {
                when (holder.itemViewType) {
                    view_type_content -> {
                        val item = filteredItems[position]
                        val contentHolder = holder as RecordViewHolder
                        contentHolder.header.text = item.header
                        contentHolder.subHeader.text = item.subHeader
                        if (hasChildRecordSpec) {
                            contentHolder.nextBtn.visibility = View.VISIBLE
                        } else {
                            contentHolder.nextBtn.visibility = View.GONE
                        }
                    }
                    view_type_footer -> {
                        // do nothing
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            val size = filteredItems.size
            return if (size > 0) size + 1 else 0
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(charSequence: CharSequence?): FilterResults {
                    val charString = charSequence.toString()
                    if (charString.isEmpty()) {
                        filteredItems = items
                    } else {
                        filteredItems = items.filter { record -> record.header.contains(charString) ||
                                record.subHeader.contains(charString) }
                    }
                    val filterResults = FilterResults()
                    filterResults.values = filteredItems
                    return filterResults
                }

                override fun publishResults(charSequence: CharSequence?, filterResults: FilterResults?) {
                    @Suppress("UNCHECKED_CAST")
                    filteredItems = filterResults?.values as List<RecordData>
                    notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroy() {
        recordDataSource?.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.record_activity_main, menu)
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

        searchView = menu?.findItem(R.id.action_search)?.actionView as SearchView
        if (searchView != null) {
            searchView?.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchView?.maxWidth = 1000
            searchView?.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    recordAdapter.filter.filter(query)
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    recordAdapter.filter.filter(newText)
                    return false
                }
            })
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        if (id == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }


}