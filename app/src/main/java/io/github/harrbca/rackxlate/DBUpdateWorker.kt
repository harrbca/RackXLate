package io.github.harrbca.rackxlate

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DBUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        val TAG = "DBUpdateWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val application = applicationContext as? RackXLateApplication
            if (application == null) {
                Log.e(TAG, "Application context is not an instance of RackXLateApplication")
                return Result.failure()
            }

            val dbUpdateManager = application.dbUpdateManager

            Log.d(TAG, "Running periodic DB update check")
            dbUpdateManager.checkAndUpdateIfNeeded()

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DB update work failed", e)
            // You can choose retry, but success is usually fine here
            Result.retry()
        }
    }
}