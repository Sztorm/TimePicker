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
import com.sztorm.timepicker.timeangleconstants.*
import java.util.*
import kotlin.math.*

internal fun Int.toTwoDigitString(): String {
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

open class TimePicker : View {
    companion object {
        private const val PICKER_DEFAULT_DISABLED_ALPHA: Int = 77
        const val FORMAT_24HOUR: Boolean = true
        const val FORMAT_12HOUR: Boolean = false
        const val AM: Boolean = true
        const val PM: Boolean = false
    }

    protected val paint = Paint()
    protected val pointerCollider = CircleCollider(centerX = 0F, centerY = 0F, radius = -1F)
    protected val trackCollider = RingCollider(centerX = 0F, centerY = 0F, radius = -1F, size = -1F)
    protected var isPointerTouchDown = false
    protected var minOfWidthAndHeight = 0f
    protected var canvasOffset = 0f
    protected var mTextColor: Int = Color.BLACK
    protected var mTrackColor: Int = Color.parseColor("#F57C00")
    protected var mPointerColor: Int = Color.parseColor("#0FDA71")
    protected var mCanvasColor: Int = Color.TRANSPARENT
    protected var mClockFaceColor: Int = Color.WHITE
    protected var mDisabledTextColor: Int = mTextColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    protected var mDisabledTrackColor: Int = mTrackColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    protected var mDisabledPointerColor: Int = mPointerColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    protected var mDisabledCanvasColor: Int = mCanvasColor.withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    protected var mDisabledClockFaceColor: Int = mClockFaceColor
        .withAlpha(PICKER_DEFAULT_DISABLED_ALPHA)
    protected var currentTextColor: Int = mTextColor
    protected var currentTrackColor: Int = mTrackColor
    protected var currentPointerColor: Int = mPointerColor
    protected var currentCanvasColor: Int = mCanvasColor
    protected var currentClockFaceColor: Int = mClockFaceColor
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
    protected var angleRadians: Double = 0.0
    protected var timeTextCache = TimeTextCache()
    protected var amPmTextCache = AmPmTextCache()
    protected var mIs24Hour: Boolean = DateFormat.is24HourFormat(context)
    protected val degreesFromClockStart: Double
        get() = (Math.toDegrees(angleRadians) + DEGREE_FOR_FULL_ANGLE + DEGREE_FOR_RIGHT_START) %
                DEGREE_FOR_FULL_ANGLE
    protected open val timeTextSize: Float get() = minOfWidthAndHeight * 0.2F
    protected open val amPmTextSize: Float get() = minOfWidthAndHeight * 0.1F
    protected open val timeTextOffsetY: Float get() = timeTextSize * 0.25F
    protected open val amPmTextOffsetY: Float get() = amPmTextSize * 2
    protected val hourIn12HFormat: Int
        get() {
            val amPmHour: Int = hour % HOURS_IN_HALF_DAY

            return if (amPmHour == 0) HOURS_IN_HALF_DAY else amPmHour
        }

    var clockFaceColor: Int
        get() = mClockFaceColor
        set(value) {
            mClockFaceColor = value
            currentClockFaceColor = if (isEnabled) mClockFaceColor else mDisabledClockFaceColor
            invalidate()
        }

    var pointerColor: Int
        get() = mPointerColor
        set(value) {
            mPointerColor = value
            currentPointerColor = if (isEnabled) mPointerColor else mDisabledPointerColor
            invalidate()
        }

    var textColor: Int
        get() = mTextColor
        set(value) {
            mTextColor = value
            currentTextColor = if (isEnabled) mTextColor else mDisabledTextColor
            invalidate()
        }

    var canvasColor: Int
        get() = mCanvasColor
        set(value) {
            mCanvasColor = value
            currentCanvasColor = if (isEnabled) mCanvasColor else mDisabledCanvasColor
            invalidate()
        }

    var trackColor: Int
        get() = mTrackColor
        set(value) {
            mTrackColor = value
            currentTrackColor = if (isEnabled) mTrackColor else mDisabledTrackColor
            invalidate()
        }

    var disabledClockFaceColor: Int
        get() = mDisabledClockFaceColor
        set(value) {
            mDisabledClockFaceColor = value
            currentClockFaceColor = if (isEnabled) mClockFaceColor else mDisabledClockFaceColor
            invalidate()
        }

    var disabledPointerColor: Int
        get() = mDisabledPointerColor
        set(value) {
            mDisabledPointerColor = value
            currentPointerColor = if (isEnabled) mPointerColor else mDisabledPointerColor
            invalidate()
        }

    var disabledTextColor: Int
        get() = mDisabledTextColor
        set(value) {
            mDisabledTextColor = value
            currentTextColor = if (isEnabled) mTextColor else mDisabledTextColor
            invalidate()
        }

    var disabledCanvasColor: Int
        get() = mDisabledCanvasColor
        set(value) {
            mDisabledCanvasColor = value
            currentCanvasColor = if (isEnabled) mCanvasColor else mDisabledCanvasColor
            invalidate()
        }

    var disabledTrackColor: Int
        get() = mDisabledTrackColor
        set(value) {
            mDisabledTrackColor = value
            currentTrackColor = if (isEnabled) mTrackColor else mDisabledTrackColor
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
                setTime(hour % HOURS_IN_HALF_DAY, minute, isAm)
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
        protected set

    /**
     * Returns current picked minute
     * */
    var minute: Int = 0
        protected set

    /**
     * Returns current picked hour in time format specified by [is24Hour] property
     * */
    val hourFormatted: Int get() = if (is24Hour) hour else hourIn12HFormat

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
            mCanvasColor = typedArray.getColor(
                R.styleable.TimePicker_canvasColor, mCanvasColor)
            mClockFaceColor = typedArray.getColor(
                R.styleable.TimePicker_clockFaceColor, mClockFaceColor)
            mPointerColor = typedArray.getColor(
                R.styleable.TimePicker_pointerColor, mPointerColor)
            mTextColor = typedArray.getColor(
                R.styleable.TimePicker_textColor, mTextColor)
            mTrackColor = typedArray.getColor(
                R.styleable.TimePicker_trackColor, mTrackColor)
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
            mIs24Hour = typedArray.getBoolean(
                R.styleable.TimePicker_is24Hour, mIs24Hour)
            isTrackTouchable = typedArray.getBoolean(
                R.styleable.TimePicker_isTrackTouchable, isTrackTouchable)
            setCurrentColors()
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
            angleRadians = get24HAngleFromTime(hour, minute)
        }
        else {
            timeTextCache.setTime(hourIn12HFormat, minute)
            amPmTextCache.isAm = isAm
            angleRadians = get12HAngleFromTime(hour, minute)
        }
        setPointerPosition(angleRadians)
    }

    protected fun setTime(degreeAngle: Double) {
        if (is24Hour) {
            hour = (degreeAngle / DEGREE_STEP_FOR_1HOUR_IN_24H_CLOCK)
                .toInt() % HOURS_IN_DAY
            minute = (degreeAngle / DEGREE_STEP_FOR_1MINUTE_IN_24H_CLOCK)
                .toInt() % MINUTES_IN_HOUR
            timeTextCache.setTime(hour, minute)
            return
        }
        val prevHourAmPm: Int = hour % HOURS_IN_HALF_DAY
        val hourAmPm: Int = (degreeAngle / DEGREE_STEP_FOR_1HOUR_IN_12H_CLOCK)
            .toInt() % HOURS_IN_HALF_DAY
        minute = (degreeAngle / DEGREE_STEP_FOR_1MINUTE_IN_12H_CLOCK)
            .toInt() % MINUTES_IN_HOUR
        var isAm = isAm

        if (hourAmPm == 0 && prevHourAmPm == 11 || hourAmPm == 11 && prevHourAmPm == 0) {
            isAm = !isAm
        }
        hour = if (!isAm) hourAmPm + HOURS_IN_HALF_DAY else hourAmPm
        timeTextCache.setTime(if (hourAmPm == 0) HOURS_IN_HALF_DAY else hourAmPm, minute)
        amPmTextCache.isAm = isAm
    }

    protected fun setPointerPosition(angle: Double) {
        pointerCollider.centerX = trackCollider.radius * cos(angle).toFloat()
        pointerCollider.centerY = trackCollider.radius * sin(angle).toFloat()
    }

    protected fun get24HAngleFromTime(hour: Int, minute: Int): Double
        = hour * RADIAN_STEP_FOR_1HOUR_IN_24H_CLOCK +
          minute * RADIAN_STEP_FOR_1MINUTE_IN_24H_CLOCK - RADIAN_FOR_RIGHT_ANGLE

    protected fun get12HAngleFromTime(hour: Int, minute: Int): Double
        = (hour % HOURS_IN_HALF_DAY) * RADIAN_STEP_FOR_1HOUR_IN_12H_CLOCK +
           minute * RADIAN_STEP_FOR_1MINUTE_IN_12H_CLOCK - RADIAN_FOR_RIGHT_ANGLE

    /**
     * Super class do not need to be called when [setCurrentColors] is overridden.
     * Although it is recommended if they are used. This method is useful when initializing or
     * changing enabled state.
     **/
    protected open fun setCurrentColors() {
        if (isEnabled) {
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

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setCurrentColors()
    }

    /**
     * Super class do not need to be called when [onMeasure] is overridden.
     * Although it is recommended if inherited components do not change their position and size.
     **/
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec).toFloat()
        val height = MeasureSpec.getSize(heightMeasureSpec).toFloat()
        minOfWidthAndHeight = min(width, height)
        setMeasuredDimension(minOfWidthAndHeight.toInt(), minOfWidthAndHeight.toInt())

        trackCollider.radius = minOfWidthAndHeight * 0.4F
        canvasOffset = minOfWidthAndHeight * 0.5f
        pointerRadius = pointerCollider.radius
        trackSize = trackCollider.size

        setPointerPosition(angleRadians)
    }

    /**
     * Super class do not need to be called when [drawText] is overridden.
     **/
    protected open fun drawText(canvas: Canvas) {
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

    /**
     * Super class do not need to be called when [drawClockFace] is overridden.
     **/
    protected open fun drawClockFace(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = currentClockFaceColor
        paint.alpha = currentClockFaceColor.alpha

        canvas.drawCircle(trackCollider, paint)
    }

    /**
     * Super class do not need to be called when [drawClockTrack] is overridden.
     **/
    protected open fun drawClockTrack(canvas: Canvas) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = trackCollider.size
        paint.color = currentTrackColor
        paint.alpha = currentTrackColor.alpha

        canvas.drawCircle(trackCollider, paint)
    }

    /**
     * Super class do not need to be called when [drawPointer] is overridden.
     **/
    protected open fun drawPointer(canvas: Canvas) {
        paint.style = Paint.Style.FILL
        paint.color = currentPointerColor
        paint.alpha = currentPointerColor.alpha

        canvas.drawCircle(pointerCollider, paint)
    }

    /**
     * Super class do not need to be called when [onDraw] is overridden.
     * Although it is recommended if the drawing order of components do not change.
     **/
    override fun onDraw(canvas: Canvas) {
        canvas.translate(canvasOffset, canvasOffset)
        canvas.drawColor(currentCanvasColor)
        drawClockFace(canvas)
        drawText(canvas)
        drawClockTrack(canvas)
        drawPointer(canvas)
    }

    /**
     * Super class do not need to be called when [onMotionActionDown] is overridden.
     * @param touchX x component of touch position with origin in center of the canvas.
     * @param touchY y component of touch position with origin in center of the canvas.
     **/
    protected open fun onMotionActionDown(touchX: Float, touchY: Float) {
        when {
            pointerCollider.isCollidingWith(touchX, touchY) -> {
                isPointerTouchDown = true
                angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
                setPointerPosition(angleRadians)
                performClick()
                invalidate()

                return
            }
            isTrackTouchable && trackCollider.isCollidingWith(touchX, touchY) -> {
                isPointerTouchDown = true
                angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
                setPointerPosition(angleRadians)
                setTime(degreesFromClockStart)
                timeChangedListener?.timeChanged(time)
                invalidate()

                return
            }
        }
        parent.requestDisallowInterceptTouchEvent(false)
    }

    /**
     * Super class do not need to be called when [onMotionActionMove] is overridden.
     * @param touchX x component of touch position with origin in center of the canvas.
     * @param touchY y component of touch position with origin in center of the canvas.
     **/
    protected open fun onMotionActionMove(touchX: Float, touchY: Float) {
        when {
            isPointerTouchDown -> {
                angleRadians = atan2(touchY.toDouble(), touchX.toDouble())
                setPointerPosition(angleRadians)
                setTime(degreesFromClockStart)
                timeChangedListener?.timeChanged(time)
                invalidate()

                return
            }
        }
        parent.requestDisallowInterceptTouchEvent(false)
    }

    /**
     * Super class do not need to be called when [onMotionActionUp] is overridden.
     * @param touchX x component of touch position with origin in center of the canvas.
     * @param touchY y component of touch position with origin in center of the canvas.
     **/
    protected open fun onMotionActionUp(touchX: Float, touchY: Float) {
        when {
            isPointerTouchDown -> {
                isPointerTouchDown = false
                invalidate()
            }
        }
    }

    /**
     * Super class do not need to be called when [drawText] is overridden.
     * When overriding, methods listed below need to be invoked to work as they are depending on
     * [event].
     * * [onMotionActionDown]
     * * [onMotionActionMove]
     * * [onMotionActionUp]
     *
     * Listed methods invocation example:
     *
     * ```
     * val touchX: Float = event.x - canvasOffset
     * val touchY: Float = event.y - canvasOffset
     *
     * when (event.action) {
     *     MotionEvent.ACTION_DOWN -> onMotionActionDown(touchX, touchY)
     *     MotionEvent.ACTION_MOVE -> onMotionActionMove(touchX, touchY)
     *     MotionEvent.ACTION_UP -> onMotionActionUp(touchX, touchY)
     * }
     * ```
     **/
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

    /**
     * Sets picker's time in 24-hour format.
     * @param hour hour in range `[0, 23]`
     * @param minute minute in range `[0, 59]`
     */
    open fun setTime(hour: Int, minute: Int) {
        if (!(hour in 0..HOURS_IN_DAY && minute in 0..MINUTES_IN_HOUR)) {
            throw IllegalArgumentException("Arguments are out of range. " +
                    "Hour must be in range [0, 23] and minute must be in range [0, 59]")
        }
        mIs24Hour = true
        this.hour = hour
        this.minute = minute
        timeTextCache.setTime(hour, minute)
        angleRadians = get24HAngleFromTime(hour, minute)
        setPointerPosition(angleRadians)

        invalidate()
    }

    /**
     * Sets picker's time in 12-hour format.
     * @param hour hour in range `[0, 12]` (0 is converted to 12)
     * @param minute minute in range `[0, 59]`
     */
    open fun setTime(hour: Int, minute: Int, isAm: Boolean) {
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
        angleRadians = get12HAngleFromTime(hour, minute)
        setPointerPosition(angleRadians)

        invalidate()
    }

    /**
     * Sets picker's time with calendar object.
     * @param calendar
     */
    open fun setTime(calendar: Calendar, is24Hour: Boolean = false) {
        val hour: Int = calendar[Calendar.HOUR_OF_DAY]
        val minute: Int = calendar[Calendar.MINUTE]

        if (is24Hour) {
            return setTime(hour, minute)
        }
        val hourFormatted: Int = calendar[Calendar.HOUR]
        val isAm: Boolean = hour < HOURS_IN_HALF_DAY

        return setTime(hourFormatted, minute, isAm)
    }
}
