package com.sztorm.timepicker.colliders

data class RingCollider(
    var centerX: Float,
    var centerY: Float,
    var radius: Float,
    var size: Float) : Collider2D
{
    var halfSize: Float
        get() = size * 0.5F
        set(value) {
            size = value * 2F
        }

    var smallerRadius: Float
        get() = radius - halfSize
        set(value) {
            radius = value + halfSize
        }

    var largerRadius: Float
        get() = radius + halfSize
        set(value) {
            radius = value - halfSize
        }

    override fun isCollidingWith(pointX: Float, pointY: Float): Boolean {
        val dx: Float = centerX - pointX
        val dy: Float = centerY - pointY
        val distanceSqr: Float = dx * dx + dy * dy
        val smallerRadius: Float = this.smallerRadius
        val largerRadius: Float = this.largerRadius
        val smallerRadiusSqr: Float = smallerRadius * smallerRadius
        val largerRadiusSqr: Float = largerRadius * largerRadius

        return distanceSqr in smallerRadiusSqr..largerRadiusSqr
    }

    fun isCollidingWith(collider: RingCollider): Boolean {
        val dx: Float = centerX - collider.centerX
        val dy: Float = centerY - collider.centerY
        val distanceSqr: Float = dx * dx + dy * dy
        val smallerRadius: Float = this.smallerRadius
        val largerRadius: Float = this.largerRadius
        val colliderSmallerRadius: Float = collider.smallerRadius
        val colliderLargerRadius: Float = collider.largerRadius
        val largerRadiusSum: Float = largerRadius + colliderLargerRadius
        val dXCollLRadiusSumX: Float = dx + colliderLargerRadius
        val dXCollLRadiusSumY: Float = dy + colliderLargerRadius
        val dXLRadiusSumX: Float = dx + largerRadius
        val dYLRadiusSumY: Float = dy + largerRadius

        val smallerRadiusSqr: Float = smallerRadius * smallerRadius
        val colliderSmallerRadiusSqr: Float = colliderSmallerRadius * colliderSmallerRadius
        val largerRadiusSumSqr: Float = largerRadiusSum * largerRadiusSum
        val distancePlusLargerRadiusSqr: Float
            = dXLRadiusSumX * dXLRadiusSumX + dYLRadiusSumY * dYLRadiusSumY
        val distancePlusColliderLargerRadiusSqr: Float
            = dXCollLRadiusSumX * dXCollLRadiusSumX + dXCollLRadiusSumY * dXCollLRadiusSumY

        return distanceSqr <= largerRadiusSumSqr &&
               distancePlusColliderLargerRadiusSqr >= smallerRadiusSqr &&
               distancePlusLargerRadiusSqr >= colliderSmallerRadiusSqr
    }
}