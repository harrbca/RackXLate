package io.github.harrbca.rackxlate

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class RackXLateApplication : Application() {
    lateinit var sharedPreferences: SharedPreferences
    lateinit var dbUpdateManager: DBUpdateManager

    override fun onCreate() {
        super.onCreate()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        dbUpdateManager = DBUpdateManager(this, sharedPreferences)
        scheduleDBUpdateWorker()
    }

    fun scheduleDBUpdateWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DBUpdateWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork("rackxlate-db-update",
                ExistingPeriodicWorkPolicy.KEEP, request)
    }
}