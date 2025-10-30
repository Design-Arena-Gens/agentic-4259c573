package com.max.pandaai.intent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import android.widget.Toast
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Parses natural language commands into platform intents with safety checks.
 */
class IntentHandler(private val context: Context) {

    fun handle(command: String): Boolean {
        val lower = command.lowercase(Locale.getDefault())
        return when {
            lower.contains("open youtube") -> launchApp("com.google.android.youtube")
            lower.contains("open whatsapp") -> launchApp("com.whatsapp")
            lower.contains("open instagram") -> launchApp("com.instagram.android")
            lower.contains("play music") -> launchApp("com.google.android.music")
            lower.startsWith("search on google for") -> {
                val query = lower.substringAfter("search on google for").trim()
                openWebSearch(query)
                true
            }
            lower.startsWith("search for") -> {
                val query = lower.substringAfter("search for").trim()
                openWebSearch(query)
                true
            }
            lower.contains("add calendar") || lower.contains("create event") -> {
                createCalendarEvent(command)
                true
            }
            lower.contains("open camera") -> {
                openCamera()
                true
            }
            lower.contains("set alarm") -> {
                setAlarm(command)
                true
            }
            lower.contains("what time") || lower.contains("current time") -> {
                speakTime()
                true
            }
            lower.contains("what date") || lower.contains("today's date") -> {
                speakDate()
                true
            }
            else -> false
        }
    }

    private fun launchApp(packageName: String): Boolean {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        return if (launchIntent != null) {
            context.startActivity(launchIntent)
            true
        } else {
            Toast.makeText(context, "App not found on device.", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun openWebSearch(query: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun extractPhoneNumber(command: String): String? {
        val matcher = PHONE_PATTERN.matcher(command)
        return if (matcher.find()) matcher.group().replace(" ", "") else null
    }

    fun extractSmsPayload(command: String): SmsPayload? {
        val matcher = SMS_PATTERN.matcher(command)
        return if (matcher.find()) {
            SmsPayload(matcher.group(1), matcher.group(2))
        } else null
    }

    fun openDialer(number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun sendSmsIntent(payload: SmsPayload) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${payload.number}")
            putExtra("sms_body", payload.message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun createCalendarEvent(command: String) {
        val beginTime = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, command)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.timeInMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, beginTime.timeInMillis + 60 * 60 * 1000)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            Toast.makeText(context, "Camera app not found.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setAlarm(command: String) {
        val matcher = ALARM_PATTERN.matcher(command)
        val hour: Int
        val minutes: Int

        if (matcher.find()) {
            hour = matcher.group(1)?.toIntOrNull() ?: 7
            minutes = matcher.group(2)?.toIntOrNull() ?: 0
        } else {
            val calendar = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
            hour = calendar.get(Calendar.HOUR_OF_DAY)
            minutes = calendar.get(Calendar.MINUTE)
        }

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minutes)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Panda AI reminder")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun speakTime() {
        val calendar = Calendar.getInstance()
        val time = android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
        Toast.makeText(context, "Current time: $time", Toast.LENGTH_LONG).show()
    }

    private fun speakDate() {
        val calendar = Calendar.getInstance()
        val date = android.text.format.DateFormat.getMediumDateFormat(context).format(calendar.time)
        Toast.makeText(context, "Today is $date", Toast.LENGTH_LONG).show()
    }

    companion object {
        private val PHONE_PATTERN = Pattern.compile("\\d{3,}")
        private val SMS_PATTERN = Pattern.compile("(?:send sms to|send message to) (\\d{3,}) (?:saying|with message) (.+)", Pattern.CASE_INSENSITIVE)
        private val ALARM_PATTERN = Pattern.compile("set alarm (?:at )?(\\d{1,2})(?::(\\d{2}))?", Pattern.CASE_INSENSITIVE)
    }

    data class SmsPayload(val number: String, val message: String)
}
