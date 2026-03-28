package com.example.synapseclick

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

sealed class TargetTrigger(val id: String = UUID.randomUUID().toString())
data class ClickTrigger(var x: Float, var y: Float) : TargetTrigger()
data class SwipeTrigger(var startX: Float, var startY: Float, var endX: Float, var endY: Float) : TargetTrigger()

object ClickManager {
    private val _isClicking = MutableStateFlow(false)
    val isClicking: StateFlow<Boolean> = _isClicking

    private val _clickIntervalMs = MutableStateFlow(1000L)
    val clickIntervalMs: StateFlow<Long> = _clickIntervalMs

    private val _delayBetweenActionsMs = MutableStateFlow(50L)
    val delayBetweenActionsMs: StateFlow<Long> = _delayBetweenActionsMs

    private val _overlayAlpha = MutableStateFlow(0.85f)
    val overlayAlpha: StateFlow<Float> = _overlayAlpha

    private val _triggers = MutableStateFlow<List<TargetTrigger>>(emptyList())
    val triggers: StateFlow<List<TargetTrigger>> = _triggers

    fun toggleClicking() {
        _isClicking.value = !_isClicking.value
    }

    fun setClickInterval(interval: Long) {
        _clickIntervalMs.value = interval
    }

    fun setDelayBetweenActions(delay: Long) {
        _delayBetweenActionsMs.value = delay
    }

    fun setOverlayAlpha(alpha: Float) {
        _overlayAlpha.value = alpha
    }

    fun addClick(x: Float, y: Float): ClickTrigger {
        val trigger = ClickTrigger(x, y)
        _triggers.value = _triggers.value + trigger
        return trigger
    }

    fun addSwipe(startX: Float, startY: Float, endX: Float, endY: Float): SwipeTrigger {
        val trigger = SwipeTrigger(startX, startY, endX, endY)
        _triggers.value = _triggers.value + trigger
        return trigger
    }

    fun updateClick(id: String, x: Float, y: Float) {
        _triggers.value.find { it.id == id && it is ClickTrigger }?.let {
            (it as ClickTrigger).x = x
            it.y = y
        }
    }

    fun updateSwipeStart(id: String, x: Float, y: Float) {
        _triggers.value.find { it.id == id && it is SwipeTrigger }?.let {
            (it as SwipeTrigger).startX = x
            it.startY = y
        }
    }

    fun updateSwipeEnd(id: String, x: Float, y: Float) {
        _triggers.value.find { it.id == id && it is SwipeTrigger }?.let {
            (it as SwipeTrigger).endX = x
            it.endY = y
        }
    }

    fun clearAll() {
        _triggers.value = emptyList()
    }

    fun stopClicking() {
        _isClicking.value = false
    }
}
