package com.sztorm.timepicker

internal class IntColorExtensions private constructor() {
    companion object {
        val Int.alpha: Int
            get() = this ushr 24

        val Int.red: Int
            get() = this shr 16 and 0xFF

        val Int.green: Int
            get() = this shr 8 and 0xFF

        val Int.blue: Int
            get() = this and 0xFF

        fun Int.withAlpha(value: Int): Int = this and 0x00FFFFFF or (value shl 24)

        fun Int.withRed(value: Int): Int = this and 0xFF00FFFF.toInt() or (value shl 16)

        fun Int.withGreen(value: Int): Int = this and 0xFFFF00FF.toInt() or (value shl 8)

        fun Int.withBlue(value: Int): Int = this and 0xFFFFFF00.toInt() or value
    }
}