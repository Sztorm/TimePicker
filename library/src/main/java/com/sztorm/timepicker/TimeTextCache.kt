package com.sztorm.timepicker

class TimeTextCache {
    companion object {
        const val SIZE: Int = 5
    }

    val buffer = CharArray(SIZE)
    var minute: Int
        get() = (buffer[3] - '0') * 10 + (buffer[4] - '0')
        set(value) {
            buffer[3] = ((value / 10) + '0'.code).toChar()
            buffer[4] = ((value % 10) + '0'.code).toChar()
        }
    var hour: Int
        get() = (buffer[0] - '0') * 10 + (buffer[1] - '0')
        set(value) {
            buffer[0] = ((value / 10) + '0'.code).toChar()
            buffer[1] = ((value % 10) + '0'.code).toChar()
        }

    init {
        buffer[0] = '0'
        buffer[1] = '0'
        buffer[2] = ':'
        buffer[3] = '0'
        buffer[4] = '0'
    }

    fun setTime(hour: Int, minute: Int) {
        this.hour = hour
        this.minute = minute
    }
}