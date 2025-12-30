package com.tofiq.peekdetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * BroadcastReceiver for handling screen on/off events for smart detection.
 * Exposes screen state as observable StateFlow for reactive consumption.
 * 
 * Requirements: 6.2, 6.3
 */
class ScreenStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "ScreenStateReceiver"
        
        // Observable screen state as StateFlow
        private val _isScreenOn = MutableStateFlow(true)
        val isScreenOn: StateFlow<Boolean> = _isScreenOn.asStateFlow()
        
        /**
         * Creates an IntentFilter for screen state events.
         */
        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
        }
        
        /**
         * Resets the screen state to default (on).
         * Should be called when the receiver is unregistered.
         */
        fun resetState() {
            _isScreenOn.value = true
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                Log.d(TAG, "Screen turned OFF")
                _isScreenOn.value = false
            }
            Intent.ACTION_SCREEN_ON -> {
                Log.d(TAG, "Screen turned ON")
                _isScreenOn.value = true
            }
        }
    }
}
