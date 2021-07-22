package com.sztorm.timepicker

class AmPmTextCache {
    companion object {
        const val SIZE: Int = 2
    }

    val buffer = CharArray(SIZE)
    var isAm: Boolean
        get() = buffer[0] == 'A'
        set(value) {
            buffer[0] = if (value) 'A' else 'P'
            buffer[1] = 'M'
        }
    var isPm: Boolean
        get() = buffer[0] == 'P'
        set(value) {
            buffer[0] = if (value) 'P' else 'A'
            buffer[1] = 'M'
        }

    init {
        buffer[0] = 'A'
        buffer[1] = 'M'
    }
}