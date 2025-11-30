package io.github.harrbca.rackxlate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.preference.PreferenceManager
import io.github.harrbca.rackxlate.Constants.DATABASE_FILENAME
import io.github.harrbca.rackxlate.Constants.KEY_REGEX
import io.github.harrbca.rackxlate.Constants.KEY_WAREHOUSE
import java.io.File

class ScanReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ScanReceiver"
        private const val DATAWEDGE_SCAN_EXTRA = "com.symbol.datawedge.data_string"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val scanData = intent?.getStringExtra(DATAWEDGE_SCAN_EXTRA)
        if (scanData.isNullOrEmpty()) {
            return
        }

        Log.d(TAG, "Scan data: $scanData")

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val regex = sharedPreferences.getString(KEY_REGEX, "^[54063]\\d{7}$")

        if (!isValidCode(scanData, regex!!)) {
            sendText(scanData)
            return
        }

        val dbFile = context.getDatabasePath(DATABASE_FILENAME)
        if (!dbFile.exists()) {
            Log.e(TAG, "DB file does not exist at ${dbFile.path}")
            sendText(scanData) // Send original data if DB is missing
            return
        }

        val warehouse = sharedPreferences.getString(KEY_WAREHOUSE, "XXX")

        val locationId = queryLocationId(dbFile, warehouse, scanData) ?: scanData
        sendText(locationId)
    }

    private fun queryLocationId(dbFile: File, warehouse: String?, extLoc: String): String? {
        if (warehouse.isNullOrBlank()) {
            Log.e(TAG, "Warehouse is null or blank")
            return extLoc
        }
        Log.d(TAG, "Querying database for warehouse $warehouse and extLoc $extLoc")

        return try {
            SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val sql = "SELECT LOCID FROM locations WHERE WARE = ? AND EXTLOC = ?"
                db.rawQuery(sql, arrayOf(warehouse, extLoc)).use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(0)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying database", e)
            null
        }
    }

    private fun isValidCode(value: String, regex: String): Boolean {
        return value.matches(regex.toRegex())
    }

    private fun sendText(text: String) {
        Log.d(TAG, "Sending text: $text")
        RackXLateAccessibilityService.instance?.pasteTextIntoFocusedField(text)
    }
}
