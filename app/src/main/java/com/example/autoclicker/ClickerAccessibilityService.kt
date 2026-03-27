package com.example.autoclicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class ClickerAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var clickJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AutoClicker", "Accessibility Service Connected")
        
        scope.launch {
            ClickManager.isClicking.collect { isClicking ->
                if (isClicking) {
                    startClicking()
                } else {
                    stopClicking()
                }
            }
        }
    }

    private fun startClicking() {
        if (clickJob?.isActive == true) return
        clickJob = scope.launch {
            // Buffer delay to guarantee Android's WindowManager fully commits FLAG_NOT_TOUCHABLE to all overlays
            // before the Accessibility Service begins firing physical touch gestures onto the screen framework.
            delay(300)
            while (isActive) {
                if (ClickManager.triggers.value.isEmpty()) {
                    delay(500)
                    continue
                }
                for ((index, trigger) in ClickManager.triggers.value.withIndex()) {
                    if (!isActive) break
                    performTrigger(trigger)
                    
                    // Specific Action Delay applied between active targets. 
                    // The Global Loop Interval is exclusively preserved for the final restart buffer.
                    if (index < ClickManager.triggers.value.size - 1) {
                        delay(ClickManager.delayBetweenActionsMs.value)
                    } else {
                        delay(ClickManager.clickIntervalMs.value)
                    }
                }
            }
        }
    }

    private fun stopClicking() {
        clickJob?.cancel()
        clickJob = null
    }

    private fun performTrigger(trigger: TargetTrigger) {
        val path = Path()
        val duration: Long
        when (trigger) {
            is ClickTrigger -> {
                path.moveTo(trigger.x, trigger.y)
                path.lineTo(trigger.x + 1f, trigger.y + 1f)
                duration = 50L
            }
            is SwipeTrigger -> {
                path.moveTo(trigger.startX, trigger.startY)
                path.lineTo(trigger.endX, trigger.endY)
                
                // Automatically compute an optimized scroll duration.
                // 1) Find total pixel distance traveled
                val dx = trigger.endX - trigger.startX
                val dy = trigger.endY - trigger.startY
                val distancePx = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())
                
                // 2) Convert pixels to native density-independent hardware units (DP)
                val density = resources.displayMetrics.density
                val distanceDp = distancePx / density
                
                // 3) Scale DP linearly into milliseconds. (1.5ms per DP enforces a perfectly
                // deliberate smooth scroll, avoiding aggressive device fling physics)
                var computedDuration = (distanceDp * 1.5).toLong()
                
                // 4) Clamp structural bounds to prevent crash anomalies on extremely short/long paths
                if (computedDuration < 100L) computedDuration = 100L
                if (computedDuration > 3000L) computedDuration = 3000L
                
                duration = computedDuration
            }
            else -> return
        }
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, duration))
        val gesture = builder.build()
        val result = dispatchGesture(gesture, null, null)
        Log.d("AutoClicker", "Dispatched trigger ${trigger.id}: result $result")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
