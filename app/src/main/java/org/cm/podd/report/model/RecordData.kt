package org.cm.podd.report.model

import java.io.Serializable

/**
 * Created by pphetra on 23/8/2018 AD.
 */
data class RecordData(val id: String,
                      val header: String,
                      val subHeader: String,
                      val timestamp: Long,
                      val reportGuid: String,
                      val parentReportGuid: String,
                      val recordSpecId: Long,
                      val parentRecordSpecId: Long,
                      val typeId: Long,
                      val username: String,
                      val startDate: Long,
                      val color: String,
                      val formData: String
                      ): Serializable {

    constructor(): this("", "", "",
            0, "",  "", 0, 0,
            0, "", 0, "#ffffff", "{}")
}