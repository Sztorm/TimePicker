package com.sztorm.timepicker.colliders

interface Collider2D {
    fun isCollidingWith(pointX: Float, pointY: Float): Boolean
}