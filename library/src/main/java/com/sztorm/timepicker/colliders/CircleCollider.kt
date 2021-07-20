package com.sztorm.timepicker.colliders

data class CircleCollider(
    var centerX: Float,
    var centerY: Float,
    var radius: Float) : Collider2D
{
    override fun isCollidingWith(pointX: Float, pointY: Float): Boolean {
        val dx: Float = centerX - pointX
        val dy: Float = centerY - pointY
        val distanceSqr: Float = dx * dx + dy * dy

        return distanceSqr <= radius * radius
    }

    fun isCollidingWith(collider: CircleCollider): Boolean {
        val dx: Float = centerX - collider.centerX
        val dy: Float = centerY - collider.centerY
        val distanceSqr: Float = dx * dx + dy * dy
        val radiusSum = radius + collider.radius

        return distanceSqr <= radiusSum * radiusSum
    }
}