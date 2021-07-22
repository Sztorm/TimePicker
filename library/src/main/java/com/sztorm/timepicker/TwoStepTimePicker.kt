@file:Suppress("MemberVisibilityCanBePrivate")

package com.sztorm.timepicker

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.animation.DecelerateInterpolator
import com.sztorm.timepicker.IntColorExtensions.Companion.alpha
import com.sztorm.timepicker.IntColorExtensions.Companion.withAlpha
import com.sztorm.timepicker.colliders.RectangleCollider
import com.sztorm.timepicker.timeangleconstants.*
import java.util.*
import kotlin.math.*

class TwoStepTimePicker : TimePicker {
    companion object {
        private const val PICKER_DEFAULT_DISABLED_ALPHA: Int = 77
        private const val ANGLE_VALUE_ANIMATION_DURATION_IN_MILLIS: Long = 500
        const val AM: Boolean = true
        const val PM: Boolean = false
        const val FORMAT_24HOUR: Boolean = true
        const val FORMAT_12HOUR: Boolean = false
        const val HOUR_PICK_STEP: Boolean = false
        const val MINUTE_PICK_STEP: Boolean = true

        private fun get24HAngleFromHour(hour: Int): Double
            = hour * RADIAN_STEP_FOR_1HOUR_IN_24H_CLOCK - RADIAN_FOR_RIGHT_ANGLE

        private fun get12HAngleFromHour(hour: Int): Double
            = (hour % HOURS_IN_HALF_DAY) * RADIAN_STEP_FOR_1HOUR_IN_12H_CLOCK - RADIAN_FOR_RIGHT_ANGLE

        private fun getAngleFromMinute(minute: Int): Double
            = minute * RADIAN_STEP_FOR_1MINUTE_IN_1H_CLOCK - RADIAN_FOR_RIGHT_ANGLE
    }

    private val angleValueAnimator = ValueAnimator()
    private val hourTextCollider = RectangleCollider(0F, 0F, 0F, 0F)
    private val minuteTextCollider = RectangleCollider(0F, 0F, 0F, 0F)
    private var isHourTextTouchDown: Boolean = false
    private var isMinuteTextTouchDown: Boolean = false
    private var halfTwoDigitsSize: Float = 0F
    private var halfColonSize: Float = 0F
    private var mPickedTextColor: Int = Color.GRAY
    private var mDisabledPickedTextColor: Int = mPickedTextColor
        .withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var currentPickedTextColor: Int = mPickedTextColor
    private var mPickedStep: Boolean = HOUR_PICK_STEP

    var pickedTextColor: Int
        get() = mPickedTextColor
        set(value) {
            mPickedTextColor = value
            currentPickedTextColor = if (isEnabled) mPickedTextColor else mDisabledPickedTextColor
            invalidate()
        }

    var disabledPickedTextColor: Int
        get() = mDisabledPickedTextColor
        set(value) {
            mDisabledPickedTextColor = value
            currentPickedTextColor = if (isEnabled) mPickedTextColor else mDisabledPickedTextColor
            invalidate()
        }

    var pickedStep: Boolean
        get() = mPickedStep
        set(value) {
            when (value) {
                HOUR_PICK_STEP -> {
                    mPickedStep = HOUR_PICK_STEP

                    if(is24Hour) {
                        timeTextCache.hour = hour
                    }
                    else {
                        timeTextCache.hour = hourIn12HFormat
                        amPmTextCache.isAm = isAm
                    }
                    restartPointerAnimation()
                    invalidate()
                }
                else -> {
                    mPickedStep = MINUTE_PICK_STEP
                    timeTextCache.minute = minute
                    restartPointerAnimation()
                    invalidate()
                }
            }
        }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        initAttributeDependentProps(attrs)
        initProperties()
    }

    constructor(context: Context, attrs: AttributeSet) :
            super(context, attrs) {
        initAttributeDependentProps(attrs)
        initProperties()
    }

    constructor(context: Context) : super(context) {
        initProperties()
    }

    private fun initAttributeDependentProps(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TwoStepTimePicker)

        try {
            mCanvasColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_canvasColor, mCanvasColor)
            mClockFaceColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_clockFaceColor, mClockFaceColor)
            mPointerColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_pointerColor, mPointerColor)
            mTextColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_textColor, mTextColor)
            mTrackColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_trackColor, mTrackColor)
            mPickedTextColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_pickedTextColor, mPickedTextColor)
            mDisabledCanvasColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_disabledCanvasColor,
                mCanvasColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))
            mDisabledClockFaceColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_disabledClockFaceColor,
                mClockFaceColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))
            mDisabledPointerColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_disabledPointerColor,
                mPointerColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))
            mDisabledTextColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_disabledTextColor,
                mTextColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))
            mDisabledTrackColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_disabledTrackColor,
                mTrackColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))
            mDisabledPickedTextColor = typedArray.getColor(
                R.styleable.TwoStepTimePicker_pickedTextColor,
                mPickedTextColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))
            trackCollider.size = typedArray.getDimension(
                R.styleable.TwoStepTimePicker_trackSize, trackCollider.size)
            pointerCollider.radius = typedArray.getDimension(
                R.styleable.TwoStepTimePicker_pointerRadius, pointerCollider.radius)
            mIs24Hour = typedArray.getBoolean(
                R.styleable.TwoStepTimePicker_is24Hour, mIs24Hour)
            isTrackTouchable = typedArray.getBoolean(
                R.styleable.TwoStepTimePicker_isTrackTouchable, isTrackTouchable)
            setCurrentColors()
        }
        finally {
            typedArray.recycle()
        }
    }

    private fun initProperties() {
        val calendar = Calendar.getInstance()
        hour = calendar[Calendar.HOUR_OF_DAY]
        minute = calendar[Calendar.MINUTE]

        if (is24Hour) {
            timeTextCache.setTime(hour, minute)
            angleRadians = get24HAngleFromHour(hour)
        }
        else {
            timeTextCache.setTime(hourIn12HFormat, minute)
            amPmTextCache.isAm = isAm
            angleRadians = get12HAngleFromHour(hour)
        }
        angleValueAnimator.interpolator = DecelerateInterpolator()
        angleValueAnimator.duration = ANGLE_VALUE_ANIMATION_DURATION_IN_MILLIS
        angleValueAnimator.addUpdateListener {
            val value = it.animatedValue as Float
            angleRadians = value.toDouble()
            setPointerPosition(angleRadians)
            invalidate()
        }
        pickedStep = HOUR_PICK_STEP
    }

    private fun restartPointerAnimation() {
        var startAngle: Double = angleRadians
        var targetAngle: Double = if (pickedStep == MINUTE_PICK_STEP) getAngleFromMinute(minute)
        else {
            if (is24Hour) get24HAngleFromHour(hour)
            else get12HAngleFromHour(hour)
        }
        startAngle = (startAngle + RADIAN_FOR_FULL_ANGLE) % RADIAN_FOR_FULL_ANGLE
        targetAngle = (targetAngle + RADIAN_FOR_FULL_ANGLE) % RADIAN_FOR_FULL_ANGLE
        val diff: Double = abs(targetAngle - startAngle) % RADIAN_FOR_FULL_ANGLE

        if (diff > RADIAN_FOR_STRAIGHT_ANGLE) {
            if (startAngle > targetAngle) {
                startAngle -= RADIAN_FOR_FULL_ANGLE
            }
            else {
                targetAngle -= RADIAN_FOR_FULL_ANGLE
            }
        }
        if (angleValueAnimator.isRunning) {
            angleValueAnimator.end()
        }
        angleValueAnimator.setFloatValues(startAngle.toFloat(), targetAngle.toFloat())
        angleValueAnimator.start()
    }

    private fun setHour(degreeAngle: Double) {
        if (is24Hour) {
            hour = (degreeAngle / DEGREE_STEP_FOR_1HOUR_IN_24H_CLOCK).toInt() % HOURS_IN_DAY
            timeTextCache.hour = hour

            return
        }
        val prevHourAmPm: Int = hour % HOURS_IN_HALF_DAY
        val hourAmPm: Int = (degreeAngle / DEGREE_STEP_FOR_1HOUR_IN_12H_CLOCK)
            .toInt() % HOURS_IN_HALF_DAY
        var isAm = isAm

        if (hourAmPm == 0 && prevHourAmPm == 11 || hourAmPm == 11 && prevHourAmPm == 0) {
            isAm = !isAm
        }
        hour = if (!isAm) hourAmPm + HOURS_IN_HALF_DAY else hourAmPm
        timeTextCache.hour = if (hourAmPm == 0) HOURS_IN_HALF_DAY else hourAmPm
        amPmTextCache.isAm = isAm
    }

    private fun setMinute(degreeAngle: Double) {
        minute = (degreeAngle / DEGREE_STEP_FOR_1MINUTE_IN_1H_CLOCK)
            .toInt() % MINUTES_IN_HOUR
        timeTextCache.minute = minute
    }

    override fun setCurrentColors() {
        if (isEnabled) {
            currentTextColor = mTextColor
            currentTrackColor = mTrackColor
            currentPointerColor = mPointerColor
            currentCanvasColor = mCanvasColor
            currentClockFaceColor = mClockFaceColor
            currentPickedTextColor = mPickedTextColor
        }
        else {
            currentTextColor = mDisabledTextColor
            currentTrackColor = mDisabledTrackColor
            currentPointerColor = mDisabledPointerColor
            currentCanvasColor = mDisabledCanvasColor
            currentClockFaceColor = mDisabledClockFaceColor
            currentPickedTextColor = mDisabledPickedTextColor
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        paint.textSize = timeTextSize
        halfTwoDigitsSize = paint.measureText("00") * 0.5F
        halfColonSize = paint.measureText(":") * 0.5F

        hourTextCollider.centerX = - halfTwoDigitsSize - halfColonSize
        hourTextCollider.centerY = -timeTextOffsetY * 0.4F
        hourTextCollider.width = halfTwoDigitsSize * 2F
        hourTextCollider.height = timeTextSize

        minuteTextCollider.centerX = halfTwoDigitsSize + halfColonSize
        minuteTextCollider.centerY = -timeTextOffsetY * 0.4F
        minuteTextCollider.width = halfTwoDigitsSize * 2F
        minuteTextCollider.height = timeTextSize
    }

    override fun drawText(canvas: Canvas) {
        val hourTextColor: Int
        val minuteTextColor: Int
        val timeTextY: Float = timeTextOffsetY
        val hourTextX: Float = -halfTwoDigitsSize - halfColonSize
        val minuteTextX: Float = halfTwoDigitsSize + halfColonSize

        if (mPickedStep == HOUR_PICK_STEP) {
            hourTextColor = currentPickedTextColor
            minuteTextColor = currentTextColor
        }
        else {
            hourTextColor = currentTextColor
            minuteTextColor = currentPickedTextColor
        }
        paint.style = Paint.Style.FILL
        paint.color = hourTextColor
        paint.alpha = hourTextColor.alpha
        paint.textSize = timeTextSize
        canvas.drawText(timeTextCache.buffer, 0, 2, hourTextX, timeTextY, paint)

        paint.color = currentTextColor
        paint.alpha = currentTextColor.alpha
        canvas.drawText(timeTextCache.buffer, 2, 1, 0F, timeTextY, paint)

        paint.color = minuteTextColor
        paint.alpha = minuteTextColor.alpha
        canvas.drawText(timeTextCache.buffer, 3, 2, minuteTextX, timeTextY, paint)

        if (!is24Hour) {
            paint.color = currentTextColor
            paint.alpha = currentTextColor.alpha
            paint.textSize = amPmTextSize

            canvas.drawText(
                amPmTextCache.buffer, 0, AmPmTextCache.SIZE, 0f, amPmTextOffsetY, paint)
        }
    }

    override fun onMotionActionDown(touchX: Float, touchY: Float) {
        when {
            pointerCollider.isCollidingWith(touchX, touchY) -> {
                isPointerTouchDown = true

                angleValueAnimator.end()
                angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
                setPointerPosition(angleRadians)
                performClick()
                invalidate()

                return
            }
            isTrackTouchable && trackCollider.isCollidingWith(touchX, touchY) -> {
                isPointerTouchDown = true

                angleValueAnimator.end()
                angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
                setPointerPosition(angleRadians)

                if (mPickedStep == HOUR_PICK_STEP) {
                    setHour(degreesFromClockStart)
                }
                else {
                    setMinute(degreesFromClockStart)
                }
                timeChangedListener?.timeChanged(time)
                invalidate()

                return
            }
            hourTextCollider.isCollidingWith(touchX, touchY) -> {
                isHourTextTouchDown = true
                return
            }
            minuteTextCollider.isCollidingWith(touchX, touchY) -> {
                isMinuteTextTouchDown = true
                return
            }
        }
        parent.requestDisallowInterceptTouchEvent(false)
    }

    override fun onMotionActionMove(touchX: Float, touchY: Float) {
        if (isPointerTouchDown) {
            angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
            setPointerPosition(angleRadians)

            if (mPickedStep == HOUR_PICK_STEP) {
                setHour(degreesFromClockStart)
            }
            else {
                setMinute(degreesFromClockStart)
            }
            timeChangedListener?.timeChanged(time)
            invalidate()

            return
        }
        parent.requestDisallowInterceptTouchEvent(false)
    }

    override fun onMotionActionUp(touchX: Float, touchY: Float) {
        when {
            isPointerTouchDown -> {
                isPointerTouchDown = false

                if (mPickedStep == HOUR_PICK_STEP) {
                    pickedStep = MINUTE_PICK_STEP
                }
                else {
                    restartPointerAnimation()
                }
                invalidate()
                return
            }
            isHourTextTouchDown && hourTextCollider.isCollidingWith(touchX, touchY) -> {
                isHourTextTouchDown = false

                if (pickedStep != HOUR_PICK_STEP) {
                    pickedStep = HOUR_PICK_STEP
                    invalidate()
                }
                return
            }
            isMinuteTextTouchDown && minuteTextCollider.isCollidingWith(touchX, touchY) -> {
                isMinuteTextTouchDown = false

                if (pickedStep != MINUTE_PICK_STEP) {
                    pickedStep = MINUTE_PICK_STEP
                    invalidate()
                }
                return
            }
        }
    }

    /**
     * Sets picker's time in 24-hour format
     * @param hour hour in range `[0, 23]`
     * @param minute minute in range `[0, 59]`
     */
    override fun setTime(hour: Int, minute: Int) {
        if (!(hour in 0..HOURS_IN_DAY && minute in 0..MINUTES_IN_HOUR)) {
            throw IllegalArgumentException("Arguments are out of range. " +
                    "Hour must be in range [0, 23] and minute must be in range [0, 59]")
        }
        mIs24Hour = true
        this.hour = hour
        this.minute = minute
        timeTextCache.setTime(hour, minute)
        mPickedStep = HOUR_PICK_STEP
        angleRadians = get24HAngleFromHour(hour)
        restartPointerAnimation()

        invalidate()
    }

    /**
     * Sets picker's time in 12-hour format
     * @param hour hour in range `[0, 12]` (0 is converted to 12)
     * @param minute minute in range `[0, 59]`
     */
    override fun setTime(hour: Int, minute: Int, isAm: Boolean) {
        if (!(hour in 0..HOURS_IN_HALF_DAY && minute in 0..MINUTES_IN_HOUR)) {
            throw IllegalArgumentException("Arguments are out of range. " +
                    "Hour must be in range [0, 12] and minute must be in range [0, 59]")
        }
        var resultHour = hour

        if (hour == HOURS_IN_HALF_DAY) {
            resultHour = 0
        }
        if (!isAm) {
            resultHour += HOURS_IN_HALF_DAY
        }
        mIs24Hour = false
        this.hour = resultHour
        this.minute = minute
        amPmTextCache.isAm = isAm
        timeTextCache.setTime(if (hour == 0) HOURS_IN_HALF_DAY else hour, minute)
        mPickedStep = HOUR_PICK_STEP
        angleRadians = get12HAngleFromHour(hour)
        restartPointerAnimation()

        invalidate()
    }
}
