package org.cm.podd.report.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import org.cm.podd.report.model.RecordSpec
import org.cm.podd.report.util.SharedPrefUtil
import org.cm.podd.report.util.SingletonHolder
import java.util.*

/**
 * Created by pphetra on 21/8/2018 AD.
 */

class RecordSpecDataSource private constructor(context: Context) {
    private var dbHelper: ReportDatabaseHelper? = null
    private var sharedPrefUtil: SharedPrefUtil? = null

    companion object: SingletonHolder<RecordSpecDataSource, Context>(::RecordSpecDataSource) {
        val TAG = "RecordSpecDataSource"
    }


    val all: List<RecordSpec>
        get() {

            val results = ArrayList<RecordSpec>()

            val db = dbHelper!!.readableDatabase
            val cursor = db.rawQuery("select * from record_spec", null)
            while (cursor.moveToNext()) {
                val spec = toRecordSpec(cursor)
                results.add(spec)
            }

            cursor.close()
            db.close()

            return results
        }

    init {
        dbHelper = ReportDatabaseHelper(context)
        sharedPrefUtil = SharedPrefUtil(context)
    }

    private fun toRecordSpec(cursor: Cursor): RecordSpec {
        val spec = RecordSpec()
        spec.id = cursor.getLong(cursor.getColumnIndex("_id"))
        spec.name = cursor.getString(cursor.getColumnIndex("name"))
        spec.timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"))
        spec.tplHeader = cursor.getString(cursor.getColumnIndex("tpl_header"))
        spec.tplSubHeader = cursor.getString(cursor.getColumnIndex("tpl_subheader"))
        spec.typeId = cursor.getLong(cursor.getColumnIndex("type_id"))
        spec.parentId = cursor.getLong(cursor.getColumnIndex("parent_id"))
        spec.groupKey = cursor.getString(cursor.getColumnIndex("group_key"))
        return spec
    }

    fun findRootRecords(): List<RecordSpec> {
        return findByParentId(0L)
    }

    fun findByParentId(parentId: Long): List<RecordSpec> {
        return all.filter { spec -> spec.parentId == parentId }
    }

    fun insert(spec: RecordSpec): Long {
        val db = dbHelper!!.writableDatabase
        val values = createContentValues(spec)
        values.put("_id", spec.id)

        val id = db.insert("record_spec", null, values)
        db.close()
        return id
    }

    fun update(spec: RecordSpec) {
        val db = dbHelper!!.writableDatabase
        db.update("record_spec", createContentValues(spec), "_id = ?",
                arrayOf(java.lang.Long.toString(spec.id)))
        db.close()
    }

    fun count(): Int {
        var result = 0;
        val db = dbHelper!!.readableDatabase
        db.use {
            val cursor = db.rawQuery("select count(*) from record_spec", null)
            if (cursor.moveToFirst()) {
                result = cursor.getInt(0)
            }
            cursor.close()
        }
        return result
    }

    private fun createContentValues(spec: RecordSpec): ContentValues {
        val values = ContentValues()
        values.put("name", spec.name)
        values.put("timestamp", spec.timestamp)
        values.put("tpl_header", spec.tplHeader)
        values.put("tpl_subheader", spec.tplSubHeader)
        values.put("type_id", spec.typeId)
        values.put("parent_id", spec.parentId)
        values.put("group_key", spec.groupKey)

        return values
    }

    fun remove(id: Long?) {
        val db = dbHelper!!.writableDatabase
        db.delete("record_spec", "_id = ?", arrayOf(id!!.toString()))
        db.close()
    }

    fun getByReportTypeId(typeId: Long): RecordSpec? {
        var result: RecordSpec? = null

        val db = dbHelper!!.readableDatabase
        db.use {
            val cursor = db.rawQuery("select * from record_spec where type_id = ?", arrayOf(java.lang.Long.toString(typeId)))
            if (cursor.moveToFirst()) {
                result = toRecordSpec(cursor)
            }
            cursor.close()
        }

        return result
    }

    operator fun get(recordId: Long): RecordSpec? {
        var result: RecordSpec? = null

        val db = dbHelper!!.readableDatabase
        val cursor = db.rawQuery("select * from record_spec where _id = ?", arrayOf(java.lang.Long.toString(recordId)))
        if (cursor.moveToFirst()) {
            result = toRecordSpec(cursor)
        }
        cursor.close()
        db.close()

        return result

    }

}
