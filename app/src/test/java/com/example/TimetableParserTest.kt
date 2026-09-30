package com.example

import com.example.timetable.TimetableParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableParserTest {

    @Test
    fun testParseSimpleSchedule() {
        val input = """
            Monday
            9:30 Business Studies
            10:30 Economics
            11:30 Accounting
            
            Tuesday
            9:30 Management
            10:30 Business Studies
        """.trimIndent()

        val parsed = TimetableParser.parse(input)
        assertEquals(5, parsed.size)

        // First item: Monday 09:30 Business Studies
        assertEquals(1, parsed[0].dayNumber)
        assertEquals(9, parsed[0].startHour)
        assertEquals(30, parsed[0].startMinute)
        assertEquals(10, parsed[0].endHour)
        assertEquals("Business Studies", parsed[0].subject)

        // Fifth item: Tuesday 10:30 Business Studies
        assertEquals(2, parsed[4].dayNumber)
        assertEquals(10, parsed[4].startHour)
        assertEquals(30, parsed[4].startMinute)
        assertEquals("Business Studies", parsed[4].subject)
    }

    @Test
    fun testParseWithRoomAndTeacher() {
        val input = """
            Wednesday
            09:30 - 10:45 Chemistry Room 204 with Dr. Watson
            11:00 - 12:00 Physics Rm 12 with Prof. Einstein
        """.trimIndent()

        val parsed = TimetableParser.parse(input)
        assertEquals(2, parsed.size)

        val chem = parsed[0]
        assertEquals(3, chem.dayNumber)
        assertEquals(9, chem.startHour)
        assertEquals(30, chem.startMinute)
        assertEquals(10, chem.endHour)
        assertEquals(45, chem.endMinute)
        assertTrue(chem.subject.contains("Chemistry"))
        assertTrue(chem.room.contains("204"))
        assertTrue(chem.teacher.contains("Watson"))
    }
}
