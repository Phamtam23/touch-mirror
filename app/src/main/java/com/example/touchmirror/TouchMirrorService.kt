package com.example.touchmirror

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.graphics.Typeface
import kotlin.math.max
import kotlin.math.min

/**
 * Experimental touch mirror. It records the touch path on the lower third, then dispatches
 * one synthetic gesture into the upper third when the user lifts their finger.
 * This is deliberately not advertised as real-time touch forwarding.
 */
class TouchMirrorService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var overlay: FrameLayout? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var touchAreaHeight = 0
    private var paused = false
    private val samples = mutableListOf<Sample>()
    private var gestureStartTime = 0L

    data class Sample(val x: Float, val y: Float, val time: Long)

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        refreshDisplaySize()
        createStopNotification()
        showOverlay()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { hideOverlay() }

    override fun onDestroy() {
        hideOverlay()
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun refreshDisplaySize() {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager?.defaultDisplay?.getRealMetrics(metrics)
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        touchAreaHeight = max(1, screenHeight / 3)
    }

    fun showOverlay() {
        if (overlay != null) return
        refreshDisplaySize()
        val panel = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(105, 30, 130, 80))
            isClickable = true
            isFocusable = false
        }
        val label = TextView(this).apply {
            text = "TOUCH MIRROR  •  chạm/kéo ở đây → vùng trên"
            textSize = 12f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(190, 15, 70, 45))
            setPadding(4, 4, 4, 4)
        }
        panel.addView(label, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, dp(28), Gravity.TOP))
        panel.setOnTouchListener { _, event -> handleTouch(event, panel) }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            touchAreaHeight,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = 0
            y = 0
        }
        try {
            windowManager?.addView(panel, params)
            overlay = panel
            overlayParams = params
        } catch (_: Exception) {
            overlay = null
            overlayParams = null
        }
    }

    fun hideOverlay() {
        val current = overlay ?: return
        try { windowManager?.removeView(current) } catch (_: Exception) { }
        overlay = null
        overlayParams = null
        samples.clear()
    }

    private fun handleTouch(event: MotionEvent, panel: View): Boolean {
        if (paused) return true
        val now = android.os.SystemClock.uptimeMillis()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                samples.clear()
                gestureStartTime = now
                samples.add(mapSample(event.x, event.y, now, panel))
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                // Include historical points to retain the shape of quick swipes.
                for (i in 0 until event.historySize) {
                    val t = event.getHistoricalEventTime(i)
                    samples.add(mapSample(event.getHistoricalX(i), event.getHistoricalY(i), t, panel))
                }
                samples.add(mapSample(event.x, event.y, now, panel))
                // Bound memory in very long drags while keeping the most recent path samples.
                if (samples.size > 500) {
                    val keep = samples.filterIndexed { index, _ -> index % 2 == 0 }
                    samples.clear(); samples.addAll(keep)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                samples.add(mapSample(event.x, event.y, now, panel))
                dispatchRecordedGesture()
                samples.clear()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                samples.clear()
                return true
            }
        }
        return true
    }

    /** Map the bottom panel's relative coordinates to the top third of the physical display. */
    private fun mapSample(x: Float, y: Float, time: Long, panel: View): Sample {
        val localW = max(1, panel.width).toFloat()
        val localH = max(1, panel.height).toFloat()
        val targetX = (x / localW * screenWidth).coerceIn(0f, (screenWidth - 1).toFloat())
        val targetY = (y / localH * touchAreaHeight).coerceIn(0f, (touchAreaHeight - 1).toFloat())
        return Sample(targetX, targetY, time)
    }

    private fun dispatchRecordedGesture() {
        if (samples.isEmpty()) return
        val points = samples.toList()
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            if (points.size == 1) {
                lineTo(points.first().x + 0.1f, points.first().y + 0.1f)
            } else {
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
        }
        val elapsed = max(1L, points.last().time - points.first().time)
        // Android accepts a gesture duration in milliseconds. A minimum duration helps taps register.
        val duration = min(10_000L, max(40L, elapsed))
        val stroke = GestureDescription.StrokeDescription(path, 0, duration)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun createStopNotification() {
        val channelId = "touch_mirror_controls"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(channelId, "Touch Mirror", NotificationManager.IMPORTANCE_LOW))
        }
        val stopIntent = Intent(this, StopReceiver::class.java).setAction(StopReceiver.ACTION_STOP)
        val pending = PendingIntent.getBroadcast(this, 100, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Touch Mirror đang bật")
            .setContentText("Chạm Dừng để đóng vùng điều khiển")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Dừng", pending).build())
            .build()
        manager.notify(101, notification)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        @Volatile var instance: TouchMirrorService? = null
            private set
    }
}
