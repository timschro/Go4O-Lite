package com.go4o.lite.data.xml

import com.go4o.lite.data.model.Course
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

object IofXmlParser {

    fun parseCourseData(input: InputStream): List<Course> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        // Detect format by looking for root element and iofVersion attribute
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                return when (parser.name) {
                    "RaceCourseData" -> parseV3(parser)
                    "CourseData" -> {
                        val version = parser.getAttributeValue(null, "iofVersion")
                        if (version != null && version.startsWith("3")) {
                            // IOF 3.0: CourseData wraps RaceCourseData
                            parseV3(parser)
                        } else {
                            parseV2(parser)
                        }
                    }
                    else -> emptyList()
                }
            }
            eventType = parser.next()
        }
        return emptyList()
    }

    // ── IOF XML 3.0 ──

    private fun parseV3(parser: XmlPullParser): List<Course> {
        val courses = mutableListOf<Course>()
        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Course") {
                parseV3Course(parser)?.let { courses.add(it) }
            }
            eventType = parser.next()
        }
        return courses
    }

    private fun parseV3Course(parser: XmlPullParser): Course? {
        var name: String? = null
        var length: Int? = null
        var climb: Int? = null
        val controls = mutableListOf<Int>()
        var depth = 1

        while (depth > 0) {
            val eventType = parser.next()
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "Name" -> name = readText(parser).also { depth-- }
                        "Length" -> {
                            length = readText(parser).trim().toDoubleOrNull()?.toInt()
                            depth--
                        }
                        "Climb" -> {
                            climb = readText(parser).trim().toDoubleOrNull()?.toInt()
                            depth--
                        }
                        "CourseControl" -> {
                            val type = parser.getAttributeValue(null, "type")
                            val control = parseV3CourseControl(parser)
                            if (type == "Control" && control != null) {
                                controls.add(control)
                            }
                            depth--
                        }
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }

        return if (name != null && controls.isNotEmpty()) {
            Course(name = name.trim(), controls = controls, length = length, climb = climb)
        } else {
            null
        }
    }

    private fun parseV3CourseControl(parser: XmlPullParser): Int? {
        var controlCode: Int? = null
        var depth = 1
        while (depth > 0) {
            val eventType = parser.next()
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (parser.name == "Control") {
                        controlCode = readText(parser).trim().toIntOrNull()
                        depth--
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }
        return controlCode
    }

    // ── IOF XML 2.0 ──

    private fun parseV2(parser: XmlPullParser): List<Course> {
        val courses = mutableListOf<Course>()
        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "Course") {
                parseV2Course(parser)?.let { courses.add(it) }
            }
            eventType = parser.next()
        }
        return courses
    }

    private fun parseV2Course(parser: XmlPullParser): Course? {
        var name: String? = null
        var length: Int? = null
        var climb: Int? = null
        val controls = mutableListOf<Int>()
        var depth = 1

        while (depth > 0) {
            val eventType = parser.next()
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    when (parser.name) {
                        "CourseName" -> {
                            name = readText(parser).trim()
                            depth--
                        }
                        "CourseLength" -> {
                            length = readText(parser).trim().toDoubleOrNull()?.toInt()
                            depth--
                        }
                        "CourseClimb" -> {
                            climb = readText(parser).trim().toDoubleOrNull()?.toInt()
                            depth--
                        }
                        "CourseControl" -> {
                            parseV2CourseControl(parser)?.let { controls.add(it) }
                            depth--
                        }
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }

        return if (name != null && controls.isNotEmpty()) {
            Course(name = name, controls = controls, length = length, climb = climb)
        } else {
            null
        }
    }

    private fun parseV2CourseControl(parser: XmlPullParser): Int? {
        var controlCode: Int? = null
        var depth = 1
        while (depth > 0) {
            val eventType = parser.next()
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    depth++
                    if (parser.name == "ControlCode") {
                        controlCode = readText(parser).trim().toIntOrNull()
                        depth--
                    }
                }
                XmlPullParser.END_TAG -> depth--
            }
        }
        return controlCode
    }

    // ── Shared ──

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }
}
