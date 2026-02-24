package com.go4o.lite.domain

import com.go4o.lite.data.model.SiCardResult
import com.go4o.lite.data.model.SiPunchData
import net.gecosi.dataframe.SiDataFrame

object SiDataFrameConverter {

    fun convert(dataFrame: SiDataFrame): SiCardResult {
        val punches = dataFrame.punches.map { punch ->
            SiPunchData(code = punch.code(), timestampMs = punch.timestamp())
        }
        return SiCardResult(
            siNumber = dataFrame.siNumber,
            startTime = dataFrame.startTime,
            finishTime = dataFrame.finishTime,
            checkTime = dataFrame.checkTime,
            punches = punches,
            cardSeries = dataFrame.siSeries
        )
    }
}
