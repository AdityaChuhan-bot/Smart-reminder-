package com.adityachuhan.smartreminder

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TimetableEntry(val dayOfWeek: Int, val hour: Int, val minute: Int, val title: String)

object TimetableOcr {
    private val dayPattern = Regex("(?i)\\b(mon(?:day)?|tue(?:sday)?|wed(?:nesday)?|thu(?:rsday)?|fri(?:day)?|sat(?:urday)?|sun(?:day)?)\\b")
    private val timePattern = Regex("(?i)\\b(\\d{1,2})(?::|[.]|h)(\\d{2})\\s*(am|pm)?\\b")

    suspend fun extract(context: Context, uri: Uri): List<TimetableEntry> {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val result = suspendCancellableCoroutine<com.google.mlkit.vision.text.Text> { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                    .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
            }
            parse(result.text)
        } finally { recognizer.close() }
    }

    private fun parse(text: String): List<TimetableEntry> {
        var currentDay: Int? = null
        val output = mutableListOf<TimetableEntry>()
        text.lines().forEach { raw ->
            val line = raw.replace(Regex("[|•·]"), " ").replace(Regex("\\s+"), " ").trim()
            if (line.isBlank()) return@forEach
            dayPattern.find(line)?.let { currentDay = dayNumber(it.value) }
            val time = timePattern.find(line) ?: return@forEach
            val day = currentDay ?: return@forEach
            var hour = time.groupValues[1].toIntOrNull() ?: return@forEach
            val minute = time.groupValues[2].toIntOrNull() ?: return@forEach
            if (minute !in 0..59 || hour !in 0..23) return@forEach
            if (time.groupValues[3].equals("pm", true) && hour < 12) hour += 12
            if (time.groupValues[3].equals("am", true) && hour == 12) hour = 0
            val title = line.replace(time.value, " ").replace(dayPattern, " ").replace(Regex("\\s+"), " ").trim(' ', '-', ':', '|')
            if (title.length >= 2 && !title.matches(Regex("(?i)^(time|period|subject|class|room|faculty)$")))
                output += TimetableEntry(day, hour, minute, title.take(80))
        }
        return output.distinctBy { "${it.dayOfWeek}|${it.hour}|${it.minute}|${it.title.lowercase()}" }
    }

    private fun dayNumber(value: String): Int = when (value.lowercase().take(3)) {
        "sun" -> 1; "mon" -> 2; "tue" -> 3; "wed" -> 4; "thu" -> 5; "fri" -> 6; else -> 7
    }
}
