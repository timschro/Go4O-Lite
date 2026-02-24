package com.go4o.lite.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.go4o.lite.data.model.AppSettings
import com.go4o.lite.data.model.Course
import com.go4o.lite.data.model.ReadoutResult
import com.go4o.lite.data.model.ResultStatus
import com.go4o.lite.data.model.SiCardResult
import com.go4o.lite.data.model.SiPunchData
import com.go4o.lite.data.persistence.AppDataStore
import com.go4o.lite.data.xml.IofXmlParser
import com.go4o.lite.domain.CourseEvaluator
import com.go4o.lite.domain.SiDataFrameConverter
import com.go4o.lite.si.SiStationManager
import com.go4o.lite.ui.SoundPlayer
import com.go4o.lite.ui.strings.stringsFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.gecosi.CommStatus

data class ReadoutEntry(
    val index: Int,
    val result: ReadoutResult,
    val timestampMs: Long
)

data class MainUiState(
    val courses: List<Course> = emptyList(),
    val connectionStatus: String = "Disconnected",
    val isConnected: Boolean = false,
    val currentResult: ReadoutResult? = null,
    val readoutHistory: List<ReadoutEntry> = emptyList(),
    val errorMessage: String? = null,
    val settings: AppSettings = AppSettings()
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var stationManager: SiStationManager? = null
    private var readoutCounter = 0
    private var dataStore: AppDataStore? = null

    fun initStationManager(context: Context) {
        if (stationManager != null) return

        if (dataStore == null) {
            dataStore = AppDataStore(context.applicationContext)
            val courses = dataStore!!.loadCourses()
            val readouts = dataStore!!.loadReadouts()
            val settings = dataStore!!.loadSettings()
            readoutCounter = readouts.maxOfOrNull { it.index } ?: 0
            _uiState.value = _uiState.value.copy(
                courses = courses,
                readoutHistory = readouts,
                settings = settings,
                connectionStatus = stringsFor(settings.language).statusDisconnected
            )
        }

        stationManager = SiStationManager(
            context = context.applicationContext,
            onCardRead = { dataFrame ->
                val cardResult = SiDataFrameConverter.convert(dataFrame)
                val result = CourseEvaluator.evaluate(cardResult, _uiState.value.courses)
                readoutCounter++
                val entry = ReadoutEntry(
                    index = readoutCounter,
                    result = result,
                    timestampMs = System.currentTimeMillis()
                )
                val newHistory = _uiState.value.readoutHistory + entry
                _uiState.value = _uiState.value.copy(
                    currentResult = result,
                    readoutHistory = newHistory,
                    errorMessage = null
                )
                dataStore?.saveReadouts(newHistory)
                when (result.status) {
                    ResultStatus.PASS -> SoundPlayer.playPass()
                    ResultStatus.FAIL, ResultStatus.NO_COURSE -> SoundPlayer.playFail()
                }
            },
            onStatusChange = { status ->
                val s = stringsFor(_uiState.value.settings.language)
                val statusText = when (status) {
                    CommStatus.OFF -> s.statusDisconnected
                    CommStatus.STARTING -> s.statusStarting
                    CommStatus.ON -> s.statusConnected
                    CommStatus.READY -> s.statusReady
                    CommStatus.PROCESSING -> s.statusReadingCard
                    CommStatus.PROCESSING_ERROR -> s.statusReadError
                    CommStatus.FATAL_ERROR -> s.statusFatalError
                }
                _uiState.value = _uiState.value.copy(
                    connectionStatus = statusText,
                    isConnected = status == CommStatus.READY || status == CommStatus.ON || status == CommStatus.PROCESSING
                )
            },
            onError = { message ->
                _uiState.value = _uiState.value.copy(errorMessage = message)
            }
        ).also {
            it.registerReceiver()
        }
    }

    fun importCourses(context: Context, uri: Uri) {
        val s = stringsFor(_uiState.value.settings.language)
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val courses = IofXmlParser.parseCourseData(inputStream)
                _uiState.value = _uiState.value.copy(
                    courses = courses,
                    errorMessage = if (courses.isEmpty()) s.noCoursesFoundInFile else null
                )
                dataStore?.saveCourses(courses)
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = s.importFailed(e.message ?: ""))
        }
    }

    fun connectStation() {
        stationManager?.connect()
    }

    fun disconnectStation() {
        stationManager?.disconnect()
        val s = stringsFor(_uiState.value.settings.language)
        _uiState.value = _uiState.value.copy(connectionStatus = s.statusDisconnected, isConnected = false)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(currentResult = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearHistory() {
        readoutCounter = 0
        _uiState.value = _uiState.value.copy(readoutHistory = emptyList())
        dataStore?.saveReadouts(emptyList())
    }

    fun updateCourse(index: Int, course: Course) {
        val courses = _uiState.value.courses.toMutableList()
        if (index in courses.indices) {
            courses[index] = course
            _uiState.value = _uiState.value.copy(courses = courses)
            dataStore?.saveCourses(courses)
        }
    }

    fun deleteCourse(index: Int) {
        val courses = _uiState.value.courses.toMutableList()
        if (index in courses.indices) {
            courses.removeAt(index)
            _uiState.value = _uiState.value.copy(courses = courses)
            dataStore?.saveCourses(courses)
        }
    }

    fun clearAll() {
        readoutCounter = 0
        _uiState.value = _uiState.value.copy(
            courses = emptyList(),
            readoutHistory = emptyList()
        )
        dataStore?.clearAll()
    }

    fun updateSettings(settings: AppSettings) {
        _uiState.value = _uiState.value.copy(settings = settings)
        dataStore?.saveSettings(settings)
    }

    fun previewResult(status: ResultStatus) {
        val dummyCard = SiCardResult(
            siNumber = "12345",
            startTime = 0L,
            finishTime = 3723000L,
            checkTime = 0L,
            punches = listOf(
                SiPunchData(code = 31, timestampMs = 600000L),
                SiPunchData(code = 32, timestampMs = 1200000L),
                SiPunchData(code = 33, timestampMs = 1800000L)
            ),
            cardSeries = "SI-10"
        )
        val missingControls = if (status == ResultStatus.FAIL) listOf(34, 35) else emptyList()
        val result = ReadoutResult(
            siCardResult = dummyCard,
            matchedCourse = null,
            status = status,
            runningTimeMs = 3723000L,
            missingControls = missingControls
        )
        _uiState.value = _uiState.value.copy(currentResult = result)
    }

    override fun onCleared() {
        stationManager?.disconnect()
        stationManager?.unregisterReceiver()
        super.onCleared()
    }
}
