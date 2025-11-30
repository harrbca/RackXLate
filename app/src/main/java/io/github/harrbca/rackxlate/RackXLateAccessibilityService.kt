package io.github.harrbca.rackxlate

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager

class RackXLateAccessibilityService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val scanReceiver = ScanReceiver()

    companion object {
        var instance: RackXLateAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        Log.d("RackXLateAccessibilityService", "Accessibility Service connected")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        registerScanReceiver()
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not yet implemented
    }

    override fun onInterrupt() {
        // Not yet implemented
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(scanReceiver)
        Log.d("RackXLateAccessibilityService", "Accessibility Service destroyed")
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "intent_action") {
            unregisterReceiver(scanReceiver)
            Log.d("ScanReceiver", "Scan receiver unregistered due to preference change")
            registerScanReceiver()
        }
    }

    private fun registerScanReceiver() {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val intentAction = sharedPreferences.getString("intent_action", "io.github.harrbca.rackxlate.ACTION")
        Log.d("ScanReceiver", "Intent action: $intentAction")
        val intentFilter = IntentFilter(intentAction)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scanReceiver, intentFilter, Context.RECEIVER_EXPORTED)
            Log.d("ScanReceiver", "Scan receiver registered")
        } else {
            registerReceiver(scanReceiver, intentFilter)
            Log.d("ScanReceiver", "Scan receiver registered")
        }
    }
}