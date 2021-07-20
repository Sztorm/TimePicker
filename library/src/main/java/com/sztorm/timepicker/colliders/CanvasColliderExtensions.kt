package com.sztorm.timepicker.colliders

import android.graphics.Canvas
import android.graphics.Paint

class CanvasColliderExtensions private constructor() {
    companion object {
        fun Canvas.drawCircle(collider: CircleCollider, paint: Paint) = drawCircle(
            collider.centerX, collider.centerY, collider.radius, paint)

        fun Canvas.drawCircle(collider: RingCollider, paint: Paint) = drawCircle(
            collider.centerX, collider.centerY, collider.radius, paint)

        fun Canvas.drawRect(collider: RectangleCollider, paint: Paint) = drawRect(
            collider.left, collider.top, collider.right, collider.bottom, paint)
    }
}