@file:Suppress("MemberVisibilityCanBePrivate")

package com.sztorm.timepicker

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.sztorm.timepicker.IntColorExtensions.Companion.alpha
import com.sztorm.timepicker.IntColorExtensions.Companion.withAlpha
import com.sztorm.timepicker.colliders.CanvasColliderExtensions.Companion.drawCircle
import com.sztorm.timepicker.colliders.CircleCollider
import com.sztorm.timepicker.colliders.RectangleCollider
import com.sztorm.timepicker.colliders.RingCollider
import java.util.*
import kotlin.math.*

class TwoStepTimePicker : View {
    companion object {
        private const val PICKER_DEFAULT_DISABLED_ALPHA: Int = 77
        private const val ANGLE_VALUE_ANIMATION_DURATION_IN_MILLIS: Long = 500
        private const val DEGREE_FOR_FULL_ANGLE: Double  = 360.0
        private const val DEGREE_FOR_HALF_ANGLE: Double  = 180.0
        private const val DEGREE_FOR_CLOCK_START: Double  = 90.0
        private const val DEGREE_STEP_FOR_1HOUR_IN_24H_CLOCK: Double = 360.0 / 24.0
        private const val DEGREE_STEP_FOR_1HOUR_IN_12H_CLOCK: Double = 360.0 / 12.0
        private const val DEGREE_STEP_FOR_1MINUTE_IN_24H_CLOCK: Double = 360.0 / (60.0 * 24.0)
        private const val DEGREE_STEP_FOR_1MINUTE_IN_12H_CLOCK: Double = 360.0 / (60.0 * 12.0)
        private const val DEGREE_STEP_FOR_1MINUTE_IN_1H_CLOCK: Double = 360.0 / 60.0
        private const val RADIAN_FOR_FULL_ANGLE: Double  = 2 * PI
        private const val RADIAN_FOR_HALF_ANGLE: Double  = PI
        private val RADIAN_FOR_CLOCK_START: Double = Math.toRadians(DEGREE_FOR_CLOCK_START)
        private val RADIAN_STEP_FOR_1HOUR_IN_24H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1HOUR_IN_24H_CLOCK)
        private val RADIAN_STEP_FOR_1HOUR_IN_12H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1HOUR_IN_12H_CLOCK)
        private val RADIAN_STEP_FOR_1MINUTE_IN_24H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1MINUTE_IN_24H_CLOCK)
        private val RADIAN_STEP_FOR_1MINUTE_IN_12H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1MINUTE_IN_12H_CLOCK)
        private val RADIAN_STEP_FOR_1MINUTE_IN_1H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1MINUTE_IN_1H_CLOCK)
        private const val MINUTES_IN_HOUR: Int = 60
        private const val HOURS_IN_DAY: Int = 24
        private const val HOURS_IN_HALF_DAY: Int = 12
        const val PM: Boolean = false
        const val AM: Boolean = true
        const val HOUR_PICK_STEP: Boolean = false
        const val MINUTE_PICK_STEP: Boolean = true

        private fun get24HAngleFromHour(hour: Int): Double
            = hour * RADIAN_STEP_FOR_1HOUR_IN_24H_CLOCK - RADIAN_FOR_CLOCK_START

        private fun get12HAngleFromHour(hour: Int): Double = (hour % HOURS_IN_HALF_DAY) *
            RADIAN_STEP_FOR_1HOUR_IN_12H_CLOCK - RADIAN_FOR_CLOCK_START

        private fun getAngleFromMinute(minute: Int): Double
            = minute * RADIAN_STEP_FOR_1MINUTE_IN_1H_CLOCK - RADIAN_FOR_CLOCK_START

        private fun Int.toTwoDigitString(): String {
            val result = CharArray(2)
            result[0] = ((this / 10) + '0'.code).toChar()
            result[1] = ((this % 10) + '0'.code).toChar()

            return String(result)
        }

        fun Int.toStringMinuteFormat(): String {
            if (this < 0 || this >= MINUTES_IN_HOUR) {
                throw IllegalArgumentException("Minute must be a value from range [0, 59].")
            }
            return this.toTwoDigitString()
        }

        fun Int.toStringHourFormat(): String {
            if (this < 0 || this >= HOURS_IN_DAY) {
                throw IllegalArgumentException("Hour must be a value from range [0, 23].")
            }
            return this.toTwoDigitString()
        }
    }

    private val paint = Paint()
    private val angleValueAnimator = ValueAnimator()
    private val pointerCollider = CircleCollider(centerX = 0F, centerY = 0F, radius = -1F)
    private val trackCollider = RingCollider(centerX = 0F, centerY = 0F, radius = -1F, size = -1F)
    private val hourTextCollider = RectangleCollider(0F, 0F, 0F, 0F)
    private val minuteTextCollider = RectangleCollider(0F, 0F, 0F, 0F)
    private var isHourTextTouchDown: Boolean = false
    private var isMinuteTextTouchDown: Boolean = false
    private var isPointerTouchDown: Boolean = false
    private var minOfWidthAndHeight: Float = 0f
    private var canvasOffset: Float = 0f
    private var halfTwoDigitsSize: Float = 0F
    private var halfColonSize: Float = 0F
    private var mTextColor: Int = Color.BLACK
    private var mTrackColor: Int = Color.parseColor("#F57C00")
    private var mPointerColor: Int = Color.parseColor("#0FDA71")
    private var mCanvasColor: Int = Color.TRANSPARENT
    private var mClockFaceColor: Int = Color.WHITE
    private var mPickedTextColor: Int = Color.GRAY
    private var mDisabledTextColor: Int = mTextColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledTrackColor: Int = mTrackColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledPointerColor: Int = mPointerColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledCanvasColor: Int = mCanvasColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledClockFaceColor: Int = mClockFaceColor
        .withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledPickedTextColor: Int = mPickedTextColor
        .withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var currentTextColor: Int = mTextColor
    private var currentTrackColor: Int = mTrackColor
    private var currentPointerColor: Int = mPointerColor
    private var currentCanvasColor: Int = mCanvasColor
    private var currentClockFaceColor: Int = mClockFaceColor
    private var currentPickedTextColor: Int = mPickedTextColor
    /**
     * 00:00 -> -0.5 * PI
     *
     * 06:00 -> 0
     *
     * 12:00 -> 0.5 * PI
     *
     * 17:59 -> ~PI
     *
     * 18:00 -> ~-PI
     */
    private var angleRadians: Double = 0.0
    private var timeTextCache = TimeTextCache()
    private var amPmTextCache = AmPmTextCache()
    private var mIs24Hour: Boolean = DateFormat.is24HourFormat(context)
    private var mPickedStep: Boolean = HOUR_PICK_STEP
    private val degreesFromClockStart: Double
        get() = (Math.toDegrees(angleRadians) + DEGREE_FOR_FULL_ANGLE + DEGREE_FOR_CLOCK_START) %
                DEGREE_FOR_FULL_ANGLE

    private val timeTextSize: Float
        get() = minOfWidthAndHeight * 0.2F

    private val amPmTextSize: Float
        get() = minOfWidthAndHeight * 0.1F

    private val timeTextOffsetY: Float
        get() = timeTextSize * 0.25F

    private val amPmTextOffsetY: Float
        get() = amPmTextSize * 2

    private val hourIn12HFormat: Int
        get() {
            val amPmHour: Int = hour % HOURS_IN_HALF_DAY

            return if (hour == 0) HOURS_IN_HALF_DAY else amPmHour
        }

    var clockFaceColor: Int
        get() = mClockFaceColor
        set(value) {
            mClockFaceColor = value
            invalidate()
        }

    var pointerColor: Int
        get() = mPointerColor
        set(value) {
            mPointerColor = value
            invalidate()
        }

    var textColor: Int
        get() = mTextColor
        set(value) {
            mTextColor = value
            invalidate()
        }

    var canvasColor: Int
        get() = mCanvasColor
        set(value) {
            mCanvasColor = value
            invalidate()
        }

    var trackColor: Int
        get() = mTrackColor
        set(value) {
            mTrackColor = value
            invalidate()
        }

    var pickedTextColor: Int
        get() = mPickedTextColor
        set(value) {
            mPickedTextColor = value
            invalidate()
        }

    var disabledClockFaceColor: Int
        get() = mDisabledClockFaceColor
        set(value) {
            mDisabledClockFaceColor = value
            invalidate()
        }

    var disabledPointerColor: Int
        get() = mDisabledPointerColor
        set(value) {
            mDisabledPointerColor = value
            invalidate()
        }

    var disabledTextColor: Int
        get() = mDisabledTextColor
        set(value) {
            mDisabledTextColor = value
            invalidate()
        }

    var disabledCanvasColor: Int
        get() = mDisabledCanvasColor
        set(value) {
            mDisabledCanvasColor = value
            invalidate()
        }

    var disabledTrackColor: Int
        get() = mDisabledTrackColor
        set(value) {
            mDisabledTrackColor = value
            invalidate()
        }

    var disabledPickedTextColor: Int
        get() = mDisabledPickedTextColor
        set(value) {
            mPickedTextColor = value
            invalidate()
        }

    var trackSize: Float
        get() = trackCollider.size
        set(value) {
            trackCollider.size = when {
                value <= 0 -> (minOfWidthAndHeight / 25)
                else -> value
            }
        }

    var pointerRadius: Float
        get() = pointerCollider.radius
        set(value) {
            pointerCollider.radius = if (value <= 0) (trackCollider.radius / 7) else value
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

    /**
     * Enables adjusting time by touching clock's track
     */
    var isTrackTouchable = true

    var timeChangedListener: TimeChangedListener? = null

    val time: PickedTime
        get() = PickedTime(hour, minute, is24Hour)

    var is24Hour: Boolean
        get() = mIs24Hour
        set(value) {
            if (mIs24Hour == value) {
                return
            }
            if (!value) {
                setTime(hour, minute)
            }
            else {
                setTime(hour % HOURS_IN_HALF_DAY, minute, isAm = hour < HOURS_IN_HALF_DAY)
            }
        }

    val isAm: Boolean
        get() = hour < HOURS_IN_HALF_DAY

    val isPm: Boolean
        get() = hour >= HOURS_IN_HALF_DAY

    /**
     * Returns current picked hour in 24-hour time format
     * */
    var hour: Int = 0
        private set

    /**
     * Returns current picked minute
     * */
    var minute: Int = 0
        private set

    /**
     * Returns current picked hour in time format specified by [is24Hour] property
     * */
    val hourFormatted: Int
        get() = if (is24Hour) hour else hourIn12HFormat

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

            invalidate()
        }
        finally {
            typedArray.recycle()
        }
    }

    private fun initProperties() {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        angleValueAnimator.interpolator = DecelerateInterpolator()
        angleValueAnimator.duration = ANGLE_VALUE_ANIMATION_DURATION_IN_MILLIS
        angleValueAnimator.addUpdateListener {
            val value = it.animatedValue as Float
            angleRadians = value.toDouble()
            calculatePointerPosition(angleRadians)
            invalidate()
        }

        paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND
        paint.textAlign = Paint.Align.CENTER

        val calendar = Calendar.getInstance()
        hour = calendar[Calendar.HOUR_OF_DAY]
        minute = calendar[Calendar.MINUTE]
        pickedStep = HOUR_PICK_STEP

        if (is24Hour) {
            timeTextCache.setTime(hour, minute)
            angleRadians = get24HAngleFromHour(hour)
        }
        else {
            timeTextCache.setTime(hourIn12HFormat, minute)
            amPmTextCache.isAm = isAm
            angleRadians = get12HAngleFromHour(hour)
        }
        calculatePointerPosition(angleRadians)
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

        if (diff > RADIAN_FOR_HALF_ANGLE) {
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

    private fun setHourFromAngle() {
        val degreesCorrected: Double = degreesFromClockStart

        if (is24Hour) {
            hour = (degreesCorrected / DEGREE_STEP_FOR_1HOUR_IN_24H_CLOCK).toInt() % HOURS_IN_DAY
            timeTextCache.hour = hour

            return
        }
        val prevHourAmPm: Int = hour % HOURS_IN_HALF_DAY
        val hourAmPm: Int = (degreesCorrected / DEGREE_STEP_FOR_1HOUR_IN_12H_CLOCK)
            .toInt() % HOURS_IN_HALF_DAY
        var isAm = isAm

        if (hourAmPm == 0 && prevHourAmPm == 11 || hourAmPm == 11 && prevHourAmPm == 0) {
            isAm = !isAm
        }
        hour = if (!isAm) hourAmPm + HOURS_IN_HALF_DAY else hourAmPm
        timeTextCache.hour = if (hourAmPm == 0) HOURS_IN_HALF_DAY else hourAmPm
        amPmTextCache.isAm = isAm
    }

    private fun setMinuteFromAngle() {
        minute = (degreesFromClockStart / DEGREE_STEP_FOR_1MINUTE_IN_1H_CLOCK)
            .toInt() % MINUTES_IN_HOUR
        timeTextCache.minute = minute
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        if (enabled) {
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
        val width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val height = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        minOfWidthAndHeight = min(width, height)
        setMeasuredDimension(minOfWidthAndHeight.toInt(), minOfWidthAndHeight.toInt())

        trackCollider.radius = minOfWidthAndHeight * 0.4F
        canvasOffset = minOfWidthAndHeight * 0.5f
        pointerRadius = pointerCollider.radius
        trackSize = trackCollider.size

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

        calculatePointerPosition(angleRadians)
    }

    private fun drawText(canvas: Canvas) {
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

    private fun drawClockFace(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = currentClockFaceColor
        paint.alpha = currentClockFaceColor.alpha

        canvas.drawCircle(trackCollider, paint)
    }

    private fun drawClockTrack(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = trackCollider.size
        paint.color = currentTrackColor
        paint.alpha = currentTrackColor.alpha

        canvas.drawCircle(trackCollider, paint)
    }

    private fun drawPointer(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = currentPointerColor
        paint.alpha = currentPointerColor.alpha

        canvas.drawCircle(pointerCollider, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(canvasOffset, canvasOffset)
        canvas.drawColor(currentCanvasColor)
        drawClockFace(canvas)
        drawText(canvas)
        drawClockTrack(canvas)
        drawPointer(canvas)
    }

    /**
     * @param touchX x component of touch position with origin in center of the canvas.
     * @param touchY y component of touch position with origin in center of the canvas.
     **/
    private fun onMotionActionDown(touchX: Float, touchY: Float) {
        when {
            pointerCollider.isCollidingWith(touchX, touchY) -> {
                isPointerTouchDown = true

                angleValueAnimator.end()
                angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
                calculatePointerPosition(angleRadians)
                invalidate()
                performClick()

                return
            }
            isTrackTouchable && trackCollider.isCollidingWith(touchX, touchY) -> {
                isPointerTouchDown = true

                angleValueAnimator.end()
                angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
                calculatePointerPosition(angleRadians)

                if (mPickedStep == HOUR_PICK_STEP) {
                    setHourFromAngle()
                }
                else {
                    setMinuteFromAngle()
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

    /**
     * @param touchX x component of touch position with origin in center of the canvas.
     * @param touchY y component of touch position with origin in center of the canvas.
     **/
    private fun onMotionActionMove(touchX: Float, touchY: Float) {
        if (isPointerTouchDown) {
            angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
            calculatePointerPosition(angleRadians)

            if (mPickedStep == HOUR_PICK_STEP) {
                setHourFromAngle()
            }
            else {
                setMinuteFromAngle()
            }
            timeChangedListener?.timeChanged(time)
            invalidate()

            return
        }
        parent.requestDisallowInterceptTouchEvent(false)
    }

    private fun onMotionActionUp(touchX: Float, touchY: Float) {
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        parent.requestDisallowInterceptTouchEvent(true)

        val touchX: Float = event.x - canvasOffset
        val touchY: Float = event.y - canvasOffset

        when (event.action) {
            MotionEvent.ACTION_DOWN -> onMotionActionDown(touchX, touchY)
            MotionEvent.ACTION_MOVE -> onMotionActionMove(touchX, touchY)
            MotionEvent.ACTION_UP -> onMotionActionUp(touchX, touchY)
        }
        return true
    }

    private fun calculatePointerPosition(angle: Double) {
        pointerCollider.centerX = trackCollider.radius * cos(angle).toFloat()
        pointerCollider.centerY = trackCollider.radius * sin(angle).toFloat()
    }

    /**
     * Sets picker's time in 24-hour format
     * @param hour hour in range `[0, 23]`
     * @param minute minute in range `[0, 59]`
     */
    fun setTime(hour: Int, minute: Int) {
        if (!(hour in 0..HOURS_IN_DAY && minute in 0..MINUTES_IN_HOUR)) {
            throw IllegalArgumentException("Arguments are out of range. " +
                    "Hour must be in range [0, 23] and minute must be in range [0, 59]")
        }
        this.mIs24Hour = true
        this.hour = hour
        this.minute = minute
        this.timeTextCache.setTime(hour, minute)
        this.mPickedStep = HOUR_PICK_STEP

        angleRadians = get24HAngleFromHour(hour)
        calculatePointerPosition(angleRadians)
        this.invalidate()
    }

    /**
     * Sets picker's time in 12-hour format
     * @param hour hour in range `[0, 12]` (0 is converted to 12)
     * @param minute minute in range `[0, 59]`
     */
    fun setTime(hour: Int, minute: Int, isAm: Boolean) {
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
        this.mIs24Hour = false
        this.hour = resultHour
        this.minute = minute
        this.amPmTextCache.isAm = isAm
        this.timeTextCache.setTime(if (hour == 0) HOURS_IN_HALF_DAY else hour, minute)
        this.mPickedStep = HOUR_PICK_STEP

        angleRadians = get12HAngleFromHour(hour)
        calculatePointerPosition(angleRadians)
        invalidate()
    }

    /**
     * This method is used to set picker's time with calendar object
     * @param calendar
     */
    fun setTime(calendar: Calendar, is24Hour: Boolean = false) {
        val hour: Int = calendar[Calendar.HOUR_OF_DAY]
        val minute: Int = calendar[Calendar.MINUTE]

        if (is24Hour) {
            return this.setTime(hour, minute)
        }
        val hourFormatted: Int = calendar[Calendar.HOUR]
        val isAm: Boolean = hour < HOURS_IN_HALF_DAY

        return this.setTime(hourFormatted, minute, isAm)
    }
}
