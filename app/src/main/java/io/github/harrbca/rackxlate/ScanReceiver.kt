package io.github.harrbca.rackxlate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class ScanReceiver : BroadcastReceiver(){
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d("ScanReceiver", "Scan received")

        // TODO do something with received scan
    }

}