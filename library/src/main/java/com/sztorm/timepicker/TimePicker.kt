@file:Suppress("MemberVisibilityCanBePrivate")

package com.sztorm.timepicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.sztorm.timepicker.IntColorExtensions.Companion.alpha
import com.sztorm.timepicker.IntColorExtensions.Companion.withAlpha
import com.sztorm.timepicker.colliders.CanvasColliderExtensions.Companion.drawCircle
import com.sztorm.timepicker.colliders.CircleCollider
import com.sztorm.timepicker.colliders.RingCollider
import java.util.*
import kotlin.math.*

class TimePicker : View {
    companion object {
        private const val PICKER_DEFAULT_DISABLED_ALPHA = 77
        private const val DEGREE_FOR_FULL_ANGLE = 360.0
        private const val DEGREE_FOR_CLOCK_START = 90.0
        private const val DEGREE_STEP_FOR_1HOUR_IN_24H_CLOCK: Double = 360.0 / 24.0
        private const val DEGREE_STEP_FOR_1HOUR_IN_12H_CLOCK: Double = 360.0 / 12.0
        private const val DEGREE_STEP_FOR_1MINUTE_IN_24H_CLOCK: Double = 360.0 / (60.0 * 24.0)
        private const val DEGREE_STEP_FOR_1MINUTE_IN_12H_CLOCK: Double = 360.0 / (60.0 * 12.0)
        private const val RADIAN_FOR_FULL_ANGLE = 2 * PI
        private val RADIAN_FOR_CLOCK_START: Double = Math.toRadians(DEGREE_FOR_CLOCK_START)
        private val RADIAN_STEP_FOR_1HOUR_IN_24H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1HOUR_IN_24H_CLOCK)
        private val RADIAN_STEP_FOR_1HOUR_IN_12H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1HOUR_IN_12H_CLOCK)
        private val RADIAN_STEP_FOR_1MINUTE_IN_24H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1MINUTE_IN_24H_CLOCK)
        private val RADIAN_STEP_FOR_1MINUTE_IN_12H_CLOCK: Double = Math
            .toRadians(DEGREE_STEP_FOR_1MINUTE_IN_12H_CLOCK)
        private const val MINUTES_IN_HOUR = 60
        private const val HOURS_IN_DAY = 24
        private const val HOURS_IN_HALF_DAY = 12
        const val AM: Boolean = true
        const val PM: Boolean = false

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
    private val pointerCollider = CircleCollider(centerX = 0F, centerY = 0F, radius = -1F)
    private val trackCollider = RingCollider(centerX = 0F, centerY = 0F, radius = -1F, size = -1F)
    private var minOfWidthAndHeight = 0f
    private var canvasOffset = 0f
    private var mTextColor: Int = Color.BLACK
    private var mTrackColor: Int = Color.parseColor("#F57C00")
    private var mPointerColor: Int = Color.parseColor("#0FDA71")
    private var mCanvasColor: Int = Color.TRANSPARENT
    private var mClockFaceColor: Int = Color.WHITE
    private var mDisabledTextColor: Int = mTextColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledTrackColor: Int = mTrackColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledPointerColor: Int = mPointerColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledCanvasColor: Int = mCanvasColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var mDisabledClockFaceColor: Int = mClockFaceColor
        .withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    private var currentTextColor: Int = mTextColor
    private var currentTrackColor: Int = mTrackColor
    private var currentPointerColor: Int = mPointerColor
    private var currentCanvasColor: Int = mCanvasColor
    private var currentClockFaceColor: Int = mClockFaceColor
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
    private var isMoving = false
    private var timeTextCache = TimeTextCache()
    private var amPmTextCache = AmPmTextCache()
    private var mIs24Hour: Boolean = DateFormat.is24HourFormat(context)
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
        get() = if (is24Hour) hour else {
            val result: Int = hour % HOURS_IN_HALF_DAY

            if (result == 0) HOURS_IN_HALF_DAY else result
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
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TimePicker)

        try {
            mCanvasColor = typedArray.getColor(R.styleable.TimePicker_canvasColor, mCanvasColor)
            mClockFaceColor = typedArray.getColor(
                R.styleable.TimePicker_clockFaceColor, mClockFaceColor)

            mPointerColor = typedArray.getColor(R.styleable.TimePicker_pointerColor, mPointerColor)
            mTextColor = typedArray.getColor(R.styleable.TimePicker_textColor, mTextColor)
            mTrackColor = typedArray.getColor(R.styleable.TimePicker_trackColor, mTrackColor)
            mDisabledCanvasColor = typedArray.getColor(
                R.styleable.TimePicker_disabledCanvasColor,
                mCanvasColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))

            mDisabledClockFaceColor = typedArray.getColor(
                R.styleable.TimePicker_disabledClockFaceColor,
                mClockFaceColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))

            mDisabledPointerColor = typedArray.getColor(
                R.styleable.TimePicker_disabledPointerColor,
                mPointerColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))

            mDisabledTextColor = typedArray.getColor(
                R.styleable.TimePicker_disabledTextColor,
                mTextColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))

            mDisabledTrackColor = typedArray.getColor(
                R.styleable.TimePicker_disabledTrackColor,
                mTrackColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA))

            trackCollider.size = typedArray.getDimension(
                R.styleable.TimePicker_trackSize, trackCollider.size)

            pointerCollider.radius = typedArray.getDimension(
                R.styleable.TimePicker_pointerRadius, pointerCollider.radius)

            mIs24Hour = typedArray.getBoolean(R.styleable.TimePicker_is24Hour, mIs24Hour)

            isTrackTouchable = typedArray.getBoolean(
                R.styleable.TimePicker_isTrackTouchable, isTrackTouchable)

            invalidate()
        }
        finally {
            typedArray.recycle()
        }
    }

    private fun initProperties() {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        paint.isAntiAlias = true
        paint.strokeCap = Paint.Cap.ROUND
        paint.textAlign = Paint.Align.CENTER

        val calendar = Calendar.getInstance()
        hour = calendar[Calendar.HOUR_OF_DAY]
        minute = calendar[Calendar.MINUTE]

        if (is24Hour) {
            timeTextCache.setTime(hour, minute)
            calculate24HAngleFromTime(hour, minute)
        }
        else {
            var amPmHour: Int = hour % HOURS_IN_HALF_DAY
            amPmHour = if (hour == 0) HOURS_IN_HALF_DAY else amPmHour

            timeTextCache.setTime(amPmHour, minute)
            amPmTextCache.isAm = isAm
            calculate12HAngleFromTime(hour, minute)
        }
        calculatePointerPosition(angleRadians)
    }

    private fun setTimeFromAngle() {
        val degreesCorrected: Double = degreesFromClockStart

        if (is24Hour) {
            hour = (degreesCorrected / DEGREE_STEP_FOR_1HOUR_IN_24H_CLOCK)
                .toInt() % HOURS_IN_DAY
            minute = (degreesCorrected / DEGREE_STEP_FOR_1MINUTE_IN_24H_CLOCK)
                .toInt() % MINUTES_IN_HOUR
            timeTextCache.setTime(hour, minute)
            return
        }
        val prevHourAmPm: Int = hour % HOURS_IN_HALF_DAY
        val hourAmPm: Int = (degreesCorrected / DEGREE_STEP_FOR_1HOUR_IN_12H_CLOCK)
            .toInt() % HOURS_IN_HALF_DAY
        minute = (degreesCorrected / DEGREE_STEP_FOR_1MINUTE_IN_12H_CLOCK)
            .toInt() % MINUTES_IN_HOUR
        var isAm = isAm

        if (hourAmPm == 0 && prevHourAmPm == 11 || hourAmPm == 11 && prevHourAmPm == 0) {
            isAm = !isAm
        }
        hour = if (!isAm) hourAmPm + HOURS_IN_HALF_DAY else hourAmPm
        timeTextCache.setTime(if (hourAmPm == 0) HOURS_IN_HALF_DAY else hourAmPm, minute)
        amPmTextCache.isAm = isAm
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        if (enabled) {
            currentTextColor = mTextColor
            currentTrackColor = mTrackColor
            currentPointerColor = mPointerColor
            currentCanvasColor = mCanvasColor
            currentClockFaceColor = mClockFaceColor
        }
        else {
            currentTextColor = mDisabledTextColor
            currentTrackColor = mDisabledTrackColor
            currentPointerColor = mDisabledPointerColor
            currentCanvasColor = mDisabledCanvasColor
            currentClockFaceColor = mDisabledClockFaceColor
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
        calculatePointerPosition(angleRadians)
    }

    private fun drawText(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = currentTextColor
        paint.alpha = currentTextColor.alpha
        paint.textSize = timeTextSize

        canvas.drawText(
            timeTextCache.buffer, 0, TimeTextCache.SIZE, 0f, timeTextOffsetY, paint)

        if (!is24Hour) {
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
    private fun onMotionActionDown(touchX: Float, touchY: Float): Boolean {
        calculatePointerPosition(angleRadians)

        if (pointerCollider.isCollidingWith(touchX, touchY)) {
            isMoving = true
            invalidate()
            performClick()

            return true
        }
        if (isTrackTouchable && trackCollider.isCollidingWith(touchX, touchY)) {
            angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
            calculatePointerPosition(angleRadians)
            setTimeFromAngle()
            timeChangedListener?.timeChanged(time)
            isMoving = true
            invalidate()

            return true
        }
        parent.requestDisallowInterceptTouchEvent(false)
        return false
    }

    /**
     * @param touchX x component of touch position with origin in center of the canvas.
     * @param touchY y component of touch position with origin in center of the canvas.
     **/
    private fun onMotionActionMove(touchX: Float, touchY: Float): Boolean {
        if (!isMoving) {
            parent.requestDisallowInterceptTouchEvent(false)
            return false
        }
        angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
        calculatePointerPosition(angleRadians)
        setTimeFromAngle()
        timeChangedListener?.timeChanged(time)
        invalidate()

        return true
    }

    private fun onMotionActionUp() {
        isMoving = false
        invalidate()
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
            MotionEvent.ACTION_DOWN -> return onMotionActionDown(touchX, touchY)
            MotionEvent.ACTION_MOVE -> return onMotionActionMove(touchX, touchY)
            MotionEvent.ACTION_UP -> onMotionActionUp()
        }
        return true
    }

    private fun calculatePointerPosition(angle: Double) {
        pointerCollider.centerX = trackCollider.radius * cos(angle).toFloat()
        pointerCollider.centerY = trackCollider.radius * sin(angle).toFloat()
    }

    private fun calculate24HAngleFromTime(hour: Int, minute: Int) {
        angleRadians = hour * RADIAN_STEP_FOR_1HOUR_IN_24H_CLOCK +
                minute * RADIAN_STEP_FOR_1MINUTE_IN_24H_CLOCK - RADIAN_FOR_CLOCK_START
    }

    private fun calculate12HAngleFromTime(hour: Int, minute: Int) {
        angleRadians = (hour % HOURS_IN_HALF_DAY) * RADIAN_STEP_FOR_1HOUR_IN_12H_CLOCK +
                minute * RADIAN_STEP_FOR_1MINUTE_IN_12H_CLOCK - RADIAN_FOR_CLOCK_START
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

        calculate24HAngleFromTime(hour, minute)
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

        calculate12HAngleFromTime(hour, minute)
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
