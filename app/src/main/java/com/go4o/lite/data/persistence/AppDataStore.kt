package com.go4o.lite.data.persistence

import android.content.Context
import com.go4o.lite.data.model.AppSettings
import com.go4o.lite.data.model.Course
import com.go4o.lite.data.model.Language
import com.go4o.lite.data.model.OverlayStyle
import com.go4o.lite.ui.viewmodel.ReadoutEntry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class AppDataStore(context: Context) {

    private val prefs = context.getSharedPreferences("go4o_lite_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveCourses(courses: List<Course>) {
        prefs.edit().putString(KEY_COURSES, gson.toJson(courses)).apply()
    }

    fun loadCourses(): List<Course> {
        val json = prefs.getString(KEY_COURSES, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<Course>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveReadouts(entries: List<ReadoutEntry>) {
        prefs.edit().putString(KEY_READOUTS, gson.toJson(entries)).apply()
    }

    fun loadReadouts(): List<ReadoutEntry> {
        val json = prefs.getString(KEY_READOUTS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<ReadoutEntry>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putInt(KEY_OVERLAY_DURATION, settings.overlayDurationSeconds)
            .putString(KEY_OVERLAY_STYLE, settings.overlayStyle.name)
            .putString(KEY_LANGUAGE, settings.language.name)
            .putBoolean(KEY_KEEP_SCREEN_ON, settings.keepScreenOn)
            .putString(KEY_SMILEY_PASS, settings.smileyPass)
            .putString(KEY_SMILEY_FAIL, settings.smileyFail)
            .apply()
    }

    fun loadSettings(): AppSettings {
        val duration = prefs.getInt(KEY_OVERLAY_DURATION, 10)
        val styleName = prefs.getString(KEY_OVERLAY_STYLE, OverlayStyle.TEXT.name)
        val style = try {
            OverlayStyle.valueOf(styleName ?: OverlayStyle.TEXT.name)
        } catch (e: Exception) {
            OverlayStyle.TEXT
        }
        val langName = prefs.getString(KEY_LANGUAGE, Language.EN.name)
        val language = try {
            Language.valueOf(langName ?: Language.EN.name)
        } catch (e: Exception) {
            Language.EN
        }
        val keepScreenOn = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        val smileyPass = prefs.getString(KEY_SMILEY_PASS, "\uD83D\uDE0A") ?: "\uD83D\uDE0A"
        val smileyFail = prefs.getString(KEY_SMILEY_FAIL, "\uD83D\uDE1E") ?: "\uD83D\uDE1E"
        return AppSettings(overlayDurationSeconds = duration, overlayStyle = style, language = language, keepScreenOn = keepScreenOn, smileyPass = smileyPass, smileyFail = smileyFail)
    }

    fun clearAll() {
        prefs.edit()
            .remove(KEY_COURSES)
            .remove(KEY_READOUTS)
            .apply()
    }

    companion object {
        private const val KEY_COURSES = "courses"
        private const val KEY_READOUTS = "readouts"
        private const val KEY_OVERLAY_DURATION = "overlay_duration"
        private const val KEY_OVERLAY_STYLE = "overlay_style"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        private const val KEY_SMILEY_PASS = "smiley_pass"
        private const val KEY_SMILEY_FAIL = "smiley_fail"
    }
}
