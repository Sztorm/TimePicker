package com.sztorm.timepicker

fun interface TimeChangedListener {
    fun timeChanged(time: PickedTime)
}
