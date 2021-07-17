@file:Suppress("MemberVisibilityCanBePrivate")

package com.sztorm.timepicker

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.format.DateFormat
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.util.*
import kotlin.math.*

class TimePicker : View {
    companion object {
        private const val PICKER_DISABLED_ALPHA = 77
        private const val PICKER_ENABLED_ALPHA = 255
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
            result[0] = ((this / 10) + '0'.toInt()).toChar()
            result[1] = ((this % 10) + '0'.toInt()).toChar()

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
    private var paintAlpha = PICKER_ENABLED_ALPHA
    private val rectF = RectF()
    private var minOfWidthAndHeight = 0f
    private var radius = 0f
    private var offset = 0f
    private var slopX = 0f
    private var slopY = 0f
    private var pointerX = 0f
    private var pointerY = 0f
    private var mTextColor = Color.BLACK
    private var mClockColor = Color.parseColor("#F57C00")
    private var mPointerColor = Color.parseColor("#0FDA71")
    private var mCanvasColor = Color.TRANSPARENT
    private var mTrackSize: Float = -1F
    private var mPointerRadius: Float = -1F

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
    private var mIs24Hour: Boolean = DateFormat.is24HourFormat(getContext())
    private val degreesFromClockStart: Double
        get() = (Math.toDegrees(angleRadians) + DEGREE_FOR_FULL_ANGLE + DEGREE_FOR_CLOCK_START) %
            DEGREE_FOR_FULL_ANGLE

    var clockColor: Int
        get() = mClockColor
        set(value) {
            mClockColor = value
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

    var trackSize: Float
        get() = mTrackSize
        set(value) {
            mTrackSize = when {
                value <= 0 -> (minOfWidthAndHeight / 25)
                //mPointerRadius > 0 && value > 2 * mPointerRadius -> (minOfWidthAndHeight / 25)
                else -> value
            }
        }

    var pointerRadius: Float
        get() = mPointerRadius
        set(value) {
            mPointerRadius = if (value <= 0) (radius / 7) else value
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

    private fun initAttributeDependentProps(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TimePicker)

        try {
            mTextColor = typedArray.getColor(R.styleable.TimePicker_textColor, mTextColor)
            Log.i("TimePicker", "pointer color before" + mPointerColor.toString())
            mPointerColor = typedArray.getColor(R.styleable.TimePicker_pointerColor, mPointerColor)
            Log.i("TimePicker", "pointer color after" + mPointerColor.toString())
            mClockColor = typedArray.getColor(R.styleable.TimePicker_clockColor, mClockColor)
            mCanvasColor = typedArray.getColor(R.styleable.TimePicker_canvasColor, mCanvasColor)
            mTrackSize = typedArray.getDimension(R.styleable.TimePicker_trackSize, mTrackSize)
            mPointerRadius = typedArray.getDimension(R.styleable.TimePicker_pointerRadius, mPointerRadius)
            Log.i("TimePicker", mTrackSize.toString())
            mIs24Hour = typedArray.getBoolean(R.styleable.TimePicker_is24Hour, mIs24Hour)
            isTrackTouchable = typedArray.getBoolean(R.styleable.TimePicker_isTrackTouchable, isTrackTouchable)
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
            var amPmHour: Int = hour % 12
            amPmHour = if (hour == 0) 12 else amPmHour

            timeTextCache.setTime(amPmHour, minute)
            amPmTextCache.isAm = isAm
            calculate12HAngleFromTime(hour, minute)
        }
        calculatePointerPosition(angleRadians)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        paintAlpha = if (enabled) PICKER_ENABLED_ALPHA else PICKER_DISABLED_ALPHA
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val height = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        minOfWidthAndHeight = min(width, height)
        setMeasuredDimension(minOfWidthAndHeight.toInt(), minOfWidthAndHeight.toInt())

        offset = minOfWidthAndHeight * 0.5f
        val padding = minOfWidthAndHeight / 20
        radius = minOfWidthAndHeight / 2 - padding * 2
        pointerRadius = mPointerRadius
        trackSize = mTrackSize
        rectF[-radius, -radius, radius] = radius
        calculatePointerPosition(angleRadians)
    }

    private fun drawText(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = mTextColor
        paint.alpha = paintAlpha
        paint.textSize = minOfWidthAndHeight / 5

        canvas.drawText(
            timeTextCache.buffer, 0, TimeTextCache.SIZE, 0f, paint.textSize / 4, paint)

        if (!is24Hour) {
            paint.textSize = minOfWidthAndHeight / 10

            canvas.drawText(
                amPmTextCache.buffer, 0, AmPmTextCache.SIZE, 0f, paint.textSize * 2, paint)
        }
    }

    private fun drawClockDial(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = mTrackSize
        paint.color = mClockColor
        paint.alpha = paintAlpha

        canvas.drawOval(rectF, paint)
    }

    private fun drawPointer(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        Log.i("TimePicker", "pointer color on draw" + mPointerColor.toString())
        paint.color = mPointerColor
        paint.alpha = paintAlpha

        canvas.drawCircle(pointerX, pointerY, pointerRadius, paint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.translate(offset, offset)
        canvas.drawColor(mCanvasColor)
        drawText(canvas)
        drawClockDial(canvas)
        drawPointer(canvas)
    }

    private fun onMotionActionDown(posX: Float, posY: Float): Boolean {
        calculatePointerPosition(angleRadians)

        if (posX >= pointerX - pointerRadius && posX <= pointerX + pointerRadius &&
            posY >= pointerY - pointerRadius && posY <= pointerY + pointerRadius) {
            slopX = posX - pointerX
            slopY = posY - pointerY
            isMoving = true
            invalidate()
            performClick()

            return true
        }
        val distance = sqrt(posX * posX + posY * posY)

        if (isTrackTouchable && distance <= radius + mTrackSize && distance >= radius - mTrackSize) {
            angleRadians = atan2(posY.toDouble(), posX.toDouble())
            setTimeFromAngle()
            timeChangedListener?.timeChanged(time)
            isMoving = true
            invalidate()

            return true
        }
        parent.requestDisallowInterceptTouchEvent(false)
        return false
    }

    private fun onMotionActionMove(posX: Float, posY: Float): Boolean {
        if (!isMoving) {
            parent.requestDisallowInterceptTouchEvent(false)
            return false
        }
        angleRadians = atan2(posY - slopY.toDouble(), posX - slopX.toDouble())
        calculatePointerPosition(angleRadians)
        setTimeFromAngle()
        timeChangedListener?.timeChanged(time)
        invalidate()

        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        parent.requestDisallowInterceptTouchEvent(true)

        val posX: Float = event.x - offset
        val posY: Float = event.y - offset

        when (event.action) {
            MotionEvent.ACTION_DOWN -> return onMotionActionDown(posX, posY)
            MotionEvent.ACTION_MOVE -> return onMotionActionMove(posX, posY)
            MotionEvent.ACTION_UP -> {
                isMoving = false
                invalidate()
            }
        }
        return true
    }

    private fun calculatePointerPosition(angle: Double) {
        pointerX = (radius * cos(angle)).toFloat()
        pointerY = (radius * sin(angle)).toFloat()
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
