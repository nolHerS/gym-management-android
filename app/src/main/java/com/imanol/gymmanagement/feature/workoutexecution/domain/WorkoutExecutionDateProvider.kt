package com.imanol.gymmanagement.feature.workoutexecution.domain

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

interface WorkoutExecutionDateProvider {
    fun todayIsoDate(): String

    fun nowEpochMillis(): Long
}

class SystemWorkoutExecutionDateProvider @Inject constructor() : WorkoutExecutionDateProvider {
    override fun todayIsoDate(): String =
        SimpleDateFormat(ISO_DATE_PATTERN, Locale.ROOT).format(Calendar.getInstance().time)

    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

fun isoDayOfWeek(isoDate: String): Int {
    val date = parseIsoDate(isoDate)
    return Calendar.getInstance().apply { time = date }.let { calendar ->
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> 7
            else -> calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 1
        }
    }
}

fun mondayOfIsoWeek(isoDate: String): String {
    val calendar = Calendar.getInstance().apply { time = parseIsoDate(isoDate) }
    val daysSinceMonday = when (calendar.get(Calendar.DAY_OF_WEEK)) {
        Calendar.SUNDAY -> 6
        else -> calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY
    }
    calendar.add(Calendar.DAY_OF_MONTH, -daysSinceMonday)
    return SimpleDateFormat(ISO_DATE_PATTERN, Locale.ROOT).format(calendar.time)
}

private fun parseIsoDate(value: String) =
    SimpleDateFormat(ISO_DATE_PATTERN, Locale.ROOT).apply { isLenient = false }.let { parser ->
        ParsePosition(0).let { position ->
            parser.parse(value, position)
                ?.takeIf { position.index == value.length }
                ?: throw IllegalArgumentException("Invalid ISO date: $value")
        }
    }

private const val ISO_DATE_PATTERN = "yyyy-MM-dd"
