package com.sztorm.timepicker.colliders

data class RectangleCollider(
    var centerX: Float,
    var centerY: Float,
    var width: Float,
    var height: Float) : Collider2D
{
    var halfWidth: Float
        get() = width * 0.5F
        set(value) {
            width = value * 2F
        }

    var halfHeight: Float
        get() = height * 0.5F
        set(value) {
            height = value * 2F
        }

    var left: Float
        get() = centerX - halfWidth
        set(value) {
            centerX = value + halfWidth
        }

    var right: Float
        get() = centerX + halfWidth
        set(value) {
            centerX = value - halfWidth
        }

    var top: Float
        get() = centerY + halfHeight
        set(value) {
            centerY = value - halfHeight
        }

    var bottom: Float
        get() = centerY - halfHeight
        set(value) {
            centerY = value + halfHeight
        }

    override fun isCollidingWith(pointX: Float, pointY: Float): Boolean
        = pointX in left..right && pointY in bottom..top

    fun isCollidingWith(collider: RectangleCollider): Boolean
        = left <= collider.right && right >= collider.left &&
          bottom <= collider.top && top >= collider.bottom
}