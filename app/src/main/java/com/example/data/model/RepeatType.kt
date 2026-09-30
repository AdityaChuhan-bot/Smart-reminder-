package com.example.data.model

enum class RepeatType {
    NONE,
    DAILY,
    WEEKDAYS,
    WEEKLY,
    MONTHLY,
    CUSTOM;

    fun getDisplayName(): String {
        return when (this) {
            NONE -> "Does not repeat"
            DAILY -> "Every day"
            WEEKDAYS -> "Every weekday (Mon-Fri)"
            WEEKLY -> "Every week"
            MONTHLY -> "Every month"
            CUSTOM -> "Custom days"
        }
    }

    companion object {
        fun fromString(value: String): RepeatType {
            return try {
                valueOf(value.uppercase())
            } catch (e: Exception) {
                NONE
            }
        }
    }
}
