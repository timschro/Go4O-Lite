package com.go4o.lite.data.xml

import org.junit.Assert.*
import org.junit.Test

class IofXmlParserTest {

    // ── IOF XML 3.0 ──

    @Test
    fun parseV3SampleCourses() {
        val input = javaClass.classLoader!!.getResourceAsStream("sample_courses.xml")!!
        val courses = IofXmlParser.parseCourseData(input)

        assertEquals(2, courses.size)

        val short = courses[0]
        assertEquals("Short", short.name)
        assertEquals(listOf(31, 32, 33), short.controls)
        assertEquals(2500, short.length)
        assertEquals(80, short.climb)

        val long = courses[1]
        assertEquals("Long", long.name)
        assertEquals(listOf(31, 42, 53, 64, 33), long.controls)
        assertEquals(5000, long.length)
        assertEquals(150, long.climb)
    }

    @Test
    fun parseV3WrappedInCourseData() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CourseData xmlns="http://www.orienteering.org/datastandard/3.0"
                        iofVersion="3.0" createTime="2026-02-22T22:46:13" creator="Condes">
              <Event><Name>Test</Name></Event>
              <RaceCourseData>
                <Course>
                  <Name>1</Name>
                  <Length>180</Length>
                  <Climb>0</Climb>
                  <CourseControl type="Start"><Control>S</Control></CourseControl>
                  <CourseControl type="Control"><Control>31</Control></CourseControl>
                  <CourseControl type="Control"><Control>33</Control></CourseControl>
                  <CourseControl type="Control"><Control>38</Control></CourseControl>
                  <CourseControl type="Finish"><Control>Z</Control></CourseControl>
                </Course>
              </RaceCourseData>
            </CourseData>
        """.trimIndent()

        val courses = IofXmlParser.parseCourseData(xml.byteInputStream())
        assertEquals(1, courses.size)
        assertEquals("1", courses[0].name)
        assertEquals(listOf(31, 33, 38), courses[0].controls)
        assertEquals(180, courses[0].length)
        assertEquals(0, courses[0].climb)
    }

    @Test
    fun parseV3SkipsStartAndFinishControls() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RaceCourseData>
              <Course>
                <Name>Test</Name>
                <CourseControl type="Start"><Control>100</Control></CourseControl>
                <CourseControl type="Control"><Control>45</Control></CourseControl>
                <CourseControl type="Control"><Control>67</Control></CourseControl>
                <CourseControl type="Finish"><Control>200</Control></CourseControl>
              </Course>
            </RaceCourseData>
        """.trimIndent()

        val courses = IofXmlParser.parseCourseData(xml.byteInputStream())
        assertEquals(1, courses.size)
        assertEquals(listOf(45, 67), courses[0].controls)
    }

    @Test
    fun parseEmptyV3ReturnsEmptyList() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <RaceCourseData>
            </RaceCourseData>
        """.trimIndent()

        val courses = IofXmlParser.parseCourseData(xml.byteInputStream())
        assertTrue(courses.isEmpty())
    }

    // ── IOF XML 2.0 ──

    @Test
    fun parseV2Course() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CourseData>
              <Course>
                <CourseName> 119 </CourseName>
                <CourseVariation>
                  <CourseLength> 160 </CourseLength>
                  <CourseClimb> 20 </CourseClimb>
                  <StartPointCode> S1 </StartPointCode>
                  <CourseControl>
                    <Sequence> 1 </Sequence>
                    <ControlCode> 36 </ControlCode>
                  </CourseControl>
                  <CourseControl>
                    <Sequence> 2 </Sequence>
                    <ControlCode> 40 </ControlCode>
                  </CourseControl>
                  <CourseControl>
                    <Sequence> 3 </Sequence>
                    <ControlCode> 45 </ControlCode>
                  </CourseControl>
                  <FinishPointCode> F1 </FinishPointCode>
                </CourseVariation>
              </Course>
            </CourseData>
        """.trimIndent()

        val courses = IofXmlParser.parseCourseData(xml.byteInputStream())
        assertEquals(1, courses.size)
        assertEquals("119", courses[0].name)
        assertEquals(listOf(36, 40, 45), courses[0].controls)
        assertEquals(160, courses[0].length)
        assertEquals(20, courses[0].climb)
    }

    @Test
    fun parseV2MultipleCourses() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CourseData>
              <Course>
                <CourseName> A </CourseName>
                <CourseVariation>
                  <CourseLength> 100 </CourseLength>
                  <CourseControl>
                    <ControlCode> 31 </ControlCode>
                  </CourseControl>
                  <CourseControl>
                    <ControlCode> 32 </ControlCode>
                  </CourseControl>
                </CourseVariation>
              </Course>
              <Course>
                <CourseName> B </CourseName>
                <CourseVariation>
                  <CourseLength> 200 </CourseLength>
                  <CourseControl>
                    <ControlCode> 41 </ControlCode>
                  </CourseControl>
                  <CourseControl>
                    <ControlCode> 42 </ControlCode>
                  </CourseControl>
                  <CourseControl>
                    <ControlCode> 43 </ControlCode>
                  </CourseControl>
                </CourseVariation>
              </Course>
            </CourseData>
        """.trimIndent()

        val courses = IofXmlParser.parseCourseData(xml.byteInputStream())
        assertEquals(2, courses.size)
        assertEquals("A", courses[0].name)
        assertEquals(listOf(31, 32), courses[0].controls)
        assertEquals("B", courses[1].name)
        assertEquals(listOf(41, 42, 43), courses[1].controls)
    }

    @Test
    fun parseEmptyV2ReturnsEmptyList() {
        val xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CourseData>
            </CourseData>
        """.trimIndent()

        val courses = IofXmlParser.parseCourseData(xml.byteInputStream())
        assertTrue(courses.isEmpty())
    }
}
