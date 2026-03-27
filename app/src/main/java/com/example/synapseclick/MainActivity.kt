package com.example.synapseclick

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AutoClickerScreen(
                        onStartOverlay = {
                            if (checkOverlayPermission()) {
                                startService(Intent(this, OverlayService::class.java))
                            } else {
                                requestOverlayPermission()
                            }
                        },
                        onRequestAccessibility = {
                            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    )
                }
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }
}

@Composable
fun AutoClickerScreen(
    onStartOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    val intervalMs by ClickManager.clickIntervalMs.collectAsState()
    val delayBetweenMs by ClickManager.delayBetweenActionsMs.collectAsState()

    var textValue by remember(intervalMs) { mutableStateOf(intervalMs.toString()) }
    var textValueDelay by remember(delayBetweenMs) { mutableStateOf(delayBetweenMs.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Synapse Click Configuration",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onRequestAccessibility) {
            Text("1. Enable Accessibility Service")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Row 1: Global Loop Interval
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { ClickManager.setClickInterval(maxOf(10, intervalMs - 100)) }) {
                Text("-")
            }
            Spacer(modifier = Modifier.width(16.dp))
            
            OutlinedTextField(
                value = textValue,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    textValue = filtered
                    filtered.toLongOrNull()?.let {
                        if (it >= 10) ClickManager.setClickInterval(it)
                    }
                },
                label = { Text("Loop Interval (ms)") },
                modifier = Modifier.width(160.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { ClickManager.setClickInterval(intervalMs + 100) }) {
                Text("+")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Row 2: Action Delay
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { ClickManager.setDelayBetweenActions(maxOf(10, delayBetweenMs - 50)) }) {
                Text("-")
            }
            Spacer(modifier = Modifier.width(16.dp))
            
            OutlinedTextField(
                value = textValueDelay,
                onValueChange = { newValue ->
                    val filtered = newValue.filter { it.isDigit() }
                    textValueDelay = filtered
                    filtered.toLongOrNull()?.let {
                        if (it >= 5) ClickManager.setDelayBetweenActions(it)
                    }
                },
                label = { Text("Action Delay (ms)") },
                modifier = Modifier.width(160.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { ClickManager.setDelayBetweenActions(delayBetweenMs + 50) }) {
                Text("+")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        Button(onClick = onStartOverlay) {
            Text("2. Start Overlay Control")
        }
    }
}
