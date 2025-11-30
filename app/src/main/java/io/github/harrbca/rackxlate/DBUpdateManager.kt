package io.github.harrbca.rackxlate

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager
import io.github.harrbca.rackxlate.Constants.DATABASE_FILENAME
import io.github.harrbca.rackxlate.Constants.KEY_MANIFEST_URL
import io.github.harrbca.rackxlate.Constants.KEY_MANIFEST_VERSION
import io.github.harrbca.rackxlate.DBUpdateManager.Companion.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import androidx.core.content.edit
import io.github.harrbca.rackxlate.Constants.KEY_MANIFEST_LAST_DOWNLOADED
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class UpdateStatus {
    object Idle: UpdateStatus()
    object Checking: UpdateStatus()
    data class Success(val version: Int): UpdateStatus()
    data class Error(val message: String): UpdateStatus()
}

class DBUpdateManager(
    private val context: Context,
    private val sharedPreferences: SharedPreferences
) {

    companion object {
        private const val TAG = "DBUpdateManager"
    }

    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus = _updateStatus.asStateFlow()

    suspend fun checkAndUpdateIfNeeded() = withContext(Dispatchers.IO) {
        _updateStatus.value = UpdateStatus.Checking
        val manifestUrl = sharedPreferences.getString(KEY_MANIFEST_URL, null)
        Log.d("DBUpdateManager", "Checking for DB update at $manifestUrl")

        if ( manifestUrl.isNullOrBlank()) {
            Log.w(TAG, "Manifest URL is not set")
            _updateStatus.value = UpdateStatus.Error("Manifest URL is not set")
            return@withContext
        }

        val manifest = fetchManifest(manifestUrl)
        if (manifest == null) {
            Log.e(TAG, "Failed to fetch manifest")
            _updateStatus.value = UpdateStatus.Error("Failed to fetch manifest")
            return@withContext
        }

        val currentVersion = sharedPreferences.getInt(KEY_MANIFEST_VERSION, -1)

        // get the dbFile
        val dbFile = context.getDatabasePath(DATABASE_FILENAME)

        // do we already file the db file
        if (dbFile.exists() && manifest.version <= currentVersion) {
            Log.i(TAG, "DB file already exists and is up to date ( local version = ${currentVersion}, remote version = ${manifest.version})")
            return@withContext
        }

        Log.i(TAG, "DB file does not exist or is outdated ( local version = ${currentVersion}, remote version = ${manifest.version})")


        // download the db file
        val tmpFile = File(dbFile.parent, "${dbFile.name}.tmp")

        if (!downloadFile(manifest.downloadUrl, tmpFile)) {
            Log.e(TAG, "Download failed")
            tmpFile.delete()
            return@withContext
        }

        Log.i(TAG, "Download succeeded and saved to ${tmpFile.absolutePath}")

        val actualSha = sha256(tmpFile)
        Log.i(TAG, "Actual SHA256: $actualSha")
        Log.i(TAG, "Expected SHA256: ${manifest.sha256}")

        if (!actualSha.equals(manifest.sha256, ignoreCase =true)) {
            Log.e(TAG, "SHA256 mismatch")
            tmpFile.delete()
            return@withContext
        }

        // Ensure directory exists
        dbFile.parentFile?.let { parent ->
            if (!parent.exists()) parent.mkdirs()
        }

        // Replace existing DB atomically-ish
        if (dbFile.exists()) {
            dbFile.delete()
        }

        if (!tmpFile.renameTo(dbFile)) {
            Log.e(TAG, "Failed to move temp DB into place")
            tmpFile.delete()
            return@withContext
        }

        sharedPreferences.edit {
            putInt(KEY_MANIFEST_VERSION, manifest.version)
            putLong(KEY_MANIFEST_LAST_DOWNLOADED, System.currentTimeMillis())
        }

        _updateStatus.value = UpdateStatus.Success(manifest.version)

    }

    private suspend fun fetchManifest(manifestUrl : String): DBManifest? {
        return withContext(Dispatchers.IO) {
            var url: URL? = null
            try {
                url = URL(manifestUrl)
            } catch (e: Exception) {
                Log.i(TAG, "Invalid manifest URL: $manifestUrl")
                return@withContext null
            }


            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
            }

            try {
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) {
                    Log.e("DBUpdateManager", "Failed to fetch manifest. Response code: $code")
                    return@withContext null // Use qualified return for withContext
                }

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("DBUpdateManager", "Fetched manifest: $json")
                parseManifest(json) // This is now called from the background
            } catch (e: Exception) {
                Log.e("DBUpdateManager", "Exception while fetching manifest", e)
                return@withContext null
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun parseManifest(json: String): DBManifest? {
        return try {
            val root = JSONObject(json)
            val version = root.getInt("version")
            val dbNode = root.getJSONObject("db")
            val url = dbNode.getString("downloadUrl") // Corrected from "downlaodUrl"
            val sha256 = dbNode.getString("sha256")

            Log.d("DBUpdateManager", "Parsed manifest: version=$version, url=$url, sha256=$sha256")
            DBManifest(version, url, sha256)
        } catch (e: Exception) {
            Log.e("DBUpdateManager", "Failed to parse JSON manifest", e)
            null
        }
    }

    private fun downloadFile(urlString: String, dest: File): Boolean {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 30_000
        }

        return try {
            val code = conn.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Failed to download file. Response code: $code")
                return false
            }

            conn.inputStream.use { input ->
                FileOutputStream(dest).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading DB", e)
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = fis.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val bytes = digest.digest()
        return bytes.joinToString("") { "%02x".format(it) }
    }
}