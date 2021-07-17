package com.sztorm.timepicker

data class PickedTime(val hour: Int, val minute: Int, val is24Hour: Boolean) {
    val hourFormatted: Int
        get() = if (is24Hour) hour else {
            val result: Int = hour % 12

            if (result == 0) 12 else result
        }

    val isAm
        get() = hour < 12

    val isPm
        get() = hour >= 12

    private fun to24HString(): String = StringBuilder(5)
        .appendTwoDigitInt(hour)
        .append(':')
        .appendTwoDigitInt(minute)
        .toString()

    private fun to12HString(): String {
        val hour = this.hour % 12
        val resultBuilder = StringBuilder(8)
            .appendTwoDigitInt(if (hour == 0) 12 else hour)
            .append(':')
            .appendTwoDigitInt(minute)
            .append(' ')
            .append(if (isAm) "AM" else "PM")

        return resultBuilder.toString()
    }

    override fun toString(): String = if(is24Hour) to24HString() else to12HString()

    companion object {
        private fun StringBuilder.appendTwoDigitInt(i: Int): StringBuilder {
            if (i < 10) {
                append('0')
            }
            append(i)

            return this
        }
    }
}