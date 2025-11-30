package io.github.harrbca.rackxlate

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

    /**
     * Finds the currently focused input field and pastes text into it.
     * @param text The text to be pasted.
     */
    fun pasteTextIntoFocusedField(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focusedNode != null && (focusedNode.isEditable || focusedNode.isFocusable)) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d("ScanAccessibility", "Pasted text: $text")
        } else {
            Log.w("ScanAccessibility", "Could not find a focused/editable field to paste text into.")
        }
        rootNode.recycle()
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