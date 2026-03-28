package com.example.synapseclick

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private val targetViews = mutableListOf<View>()
    
    private var clickCount = 0
    private var runnerJob: Job? = null

    private lateinit var btnPlayStop: Button
    private lateinit var txtTimer: TextView

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundServiceConfig()
        setupOverlay()
        observeClickState()
    }

    private fun startForegroundServiceConfig() {
        val channelId = "OverlayServiceChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Synapse Click Overlay", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Synapse Click Overlay")
            .setContentText("Overlay is running")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()
        startForeground(1, notification)
    }

    private fun getButtonBg(colorHex: String, density: Float, radiusDp: Float = 14f): RippleDrawable {
        val shape = GradientDrawable().apply {
            setColor(Color.parseColor(colorHex))
            cornerRadius = radiusDp * density
        }
        return RippleDrawable(
            ColorStateList.valueOf(Color.parseColor("#55FFFFFF")),
            shape,
            null
        )
    }

    private fun setupOverlay() {
        val density = resources.displayMetrics.density
        val btnSize = (45 * density).toInt()  // Reduced HUD footprint by 25%
        val margin = (4 * density).toInt()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CC121212")) // 80% very dark gray/black
                cornerRadius = 20 * density
            }
            setPadding((8*density).toInt(), (8*density).toInt(), (8*density).toInt(), (8*density).toInt())
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val dragHandle = TextView(this).apply {
            text = "✥"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(btnSize, (28*density).toInt()).apply {
                setMargins(0, 0, 0, margin)
            }
        }

        var isCollapsed = false
        val buttonsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val btnCollapse = Button(this).apply {
            text = "-"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = getButtonBg("#FF424242", density, 10f)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(btnSize, (28*density).toInt()).apply {
                setMargins(0, 0, 0, margin)
            }
            setOnClickListener {
                isCollapsed = !isCollapsed
                buttonsContainer.visibility = if (isCollapsed) View.GONE else View.VISIBLE
                text = if (isCollapsed) "+" else "-"
            }
        }

        txtTimer = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            textSize = 10f
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(btnSize, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, margin)
            }
        }

        btnPlayStop = Button(this).apply {
            text = "PLAY"
            textSize = 10f
            setTextColor(Color.WHITE)
            background = getButtonBg("#FF2E7D32", density) // Emerald green
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins(0, 0, 0, margin)
            }
            setOnClickListener {
                if (ClickManager.isClicking.value || runnerJob?.isActive == true) {
                    ClickManager.stopClicking()
                } else {
                    runnerJob = scope.launch {
                        text = "STOP"
                        background = getButtonBg("#FFC62828", density) // Red
                        txtTimer.visibility = View.VISIBLE
                        txtTimer.setTextColor(Color.parseColor("#FFFFD600")) // Yellow
                        for (i in 3 downTo 1) {
                            txtTimer.text = "Wait $i"
                            delay(1000)
                        }
                        ClickManager.toggleClicking()
                        txtTimer.setTextColor(Color.parseColor("#FF00E676")) // Bright green
                        var secs = 0
                        while (isActive) {
                            val m = secs / 60
                            val s = secs % 60
                            txtTimer.text = String.format("%02d:%02d", m, s)
                            delay(1000)
                            secs++
                        }
                    }
                }
            }
        }
        
        val btnAddClick = Button(this).apply {
            text = "+CLK"
            textSize = 10f
            setTextColor(Color.parseColor("#FFBBDEFB"))
            background = getButtonBg("#FF1565C0", density) // Blue
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins(0, 0, 0, margin)
            }
            setOnClickListener { addClickOverlayView() }
        }

        val btnAddSwipe = Button(this).apply {
            text = "+SWP"
            textSize = 10f
            setTextColor(Color.parseColor("#FFBBDEFB"))
            background = getButtonBg("#FF1565C0", density)
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins(0, 0, 0, margin)
            }
            setOnClickListener { 
                if (ClickManager.triggers.value.any { it is SwipeTrigger }) {
                    android.widget.Toast.makeText(this@OverlayService, "Only 1 Swipe allowed", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    addSwipeOverlayViews()
                }
            }
        }

        val btnClear = Button(this).apply {
            text = "CLR"
            textSize = 10f
            setTextColor(Color.parseColor("#FFFFCDD2"))
            background = getButtonBg("#FFD84315", density) // Deep Orange/Red
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins(0, 0, 0, margin)
            }
            setOnClickListener { clearOverlays() }
        }

        val btnClose = Button(this).apply {
            text = "CLOSE"
            textSize = 9f
            setTextColor(Color.WHITE)
            background = getButtonBg("#FF616161", density) // Medium gray
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(btnSize, btnSize).apply {
                setMargins(0, 0, 0, margin) // No bottom margin needed for last item, but safe
            }
            setOnClickListener { 
                val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
                stopSelf() 
            }
        }

        buttonsContainer.addView(txtTimer)
        buttonsContainer.addView(btnPlayStop)
        buttonsContainer.addView(btnAddClick)
        buttonsContainer.addView(btnAddSwipe)
        buttonsContainer.addView(btnClear)
        buttonsContainer.addView(btnClose)

        layout.addView(dragHandle)
        layout.addView(btnCollapse)
        layout.addView(buttonsContainer)

        overlayView = layout

        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }

        windowManager.addView(overlayView, params)

        // Real-time Opacity Slider Collector mapping to current Alpha state
        scope.launch {
            ClickManager.overlayAlpha.collect { alpha ->
                overlayView.alpha = alpha
            }
        }
    }

    private fun addClickOverlayView() {
        clickCount++
        
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        val trigger = ClickManager.addClick(centerX.toFloat(), centerY.toFloat())
        val params = createDynamicTargetParams(centerX - 75, centerY - 75) // 150px static layout bounds radius

        val targetIcon = TextView(this).apply {
            text = "$clickCount"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#8800AA00"))
                setStroke(4, Color.WHITE)
            }
        }

        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f

        targetIcon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(targetIcon, params)
                    targetIcon.post {
                        val loc = IntArray(2)
                        targetIcon.getLocationOnScreen(loc)
                        ClickManager.updateClick(trigger.id, loc[0] + targetIcon.width / 2f, loc[1] + targetIcon.height / 2f)
                    }
                    true
                }
                else -> false
            }
        }
        windowManager.addView(targetIcon, params)
        targetIcon.post {
            val loc = IntArray(2)
            targetIcon.getLocationOnScreen(loc)
            ClickManager.updateClick(trigger.id, loc[0] + targetIcon.width / 2f, loc[1] + targetIcon.height / 2f)
        }
        targetViews.add(targetIcon)
    }

    private fun addSwipeOverlayViews() {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val centerX = screenWidth / 2
        val centerY = screenHeight / 2
        
        val density = resources.displayMetrics.density
        // Automatically default distance dynamically mapped to display hardware
        // Swiping UP to scroll DOWN is natural, so Start is Bottom (+), End is Top (-)
        val startY = centerY + (75 * density).toInt()
        val endY = centerY - (75 * density).toInt()

        val trigger = ClickManager.addSwipe(centerX.toFloat(), startY.toFloat(), centerX.toFloat(), endY.toFloat())
        val paramsStart = createDynamicTargetParams(centerX - 75, startY - 75)
        val paramsEnd = createDynamicTargetParams(centerX - 75, endY - 75)

        val targetIconStart = TextView(this).apply {
            text = "S"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#880000FF"))
                setStroke(4, Color.WHITE)
            }
        }
        val targetIconEnd = TextView(this).apply {
            text = "E"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#88FF0000"))
                setStroke(4, Color.WHITE)
            }
        }

        fun setupDrag(view: View, params: WindowManager.LayoutParams, isStart: Boolean) {
            var initialX = 0; var initialY = 0
            var initialTouchX = 0f; var initialTouchY = 0f
            view.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(view, params)
                        view.post {
                            val loc = IntArray(2)
                            view.getLocationOnScreen(loc)
                            val finalX = loc[0] + view.width / 2f
                            val finalY = loc[1] + view.height / 2f
                            if (isStart) {
                                ClickManager.updateSwipeStart(trigger.id, finalX, finalY)
                            } else {
                                ClickManager.updateSwipeEnd(trigger.id, finalX, finalY)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
        }

        setupDrag(targetIconStart, paramsStart, true)
        setupDrag(targetIconEnd, paramsEnd, false)

        windowManager.addView(targetIconStart, paramsStart)
        targetIconStart.post {
            val loc = IntArray(2)
            targetIconStart.getLocationOnScreen(loc)
            ClickManager.updateSwipeStart(trigger.id, loc[0] + targetIconStart.width / 2f, loc[1] + targetIconStart.height / 2f)
        }
        
        windowManager.addView(targetIconEnd, paramsEnd)
        targetIconEnd.post {
            val loc = IntArray(2)
            targetIconEnd.getLocationOnScreen(loc)
            ClickManager.updateSwipeEnd(trigger.id, loc[0] + targetIconEnd.width / 2f, loc[1] + targetIconEnd.height / 2f)
        }

        targetViews.add(targetIconStart)
        targetViews.add(targetIconEnd)
    }

    private fun createDynamicTargetParams(x: Int, y: Int): WindowManager.LayoutParams {
        val params = WindowManager.LayoutParams(
            150, 150,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y
        return params
    }

    private fun clearOverlays() {
        for (view in targetViews) {
            try { windowManager.removeView(view) } catch (e: Exception) {}
        }
        targetViews.clear()
        clickCount = 0
        ClickManager.clearAll()
    }

    private fun observeClickState() {
        scope.launch {
            ClickManager.isClicking.collect { isClicking ->
                if (!isClicking) {
                    btnPlayStop.text = "PLAY"
                    btnPlayStop.background = getButtonBg("#FF2E7D32", resources.displayMetrics.density)
                    txtTimer.visibility = View.GONE
                    runnerJob?.cancel()
                }

                for (view in targetViews) {
                    try {
                        val targetParams = view.layoutParams as WindowManager.LayoutParams
                        if (isClicking) {
                            targetParams.flags = targetParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        } else {
                            targetParams.flags = targetParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
                        }
                        windowManager.updateViewLayout(view, targetParams)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ClickManager.stopClicking()
        if (::windowManager.isInitialized) {
            try { windowManager.removeView(overlayView) } catch (e: Exception) {}
            clearOverlays()
        }
    }
}
