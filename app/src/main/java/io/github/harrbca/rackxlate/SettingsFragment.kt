package io.github.harrbca.rackxlate

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import io.github.harrbca.rackxlate.Constants.KEY_MANIFEST_LAST_DOWNLOADED
import io.github.harrbca.rackxlate.Constants.KEY_MANIFEST_URL
import io.github.harrbca.rackxlate.Constants.KEY_MANIFEST_VERSION
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : PreferenceFragmentCompat() {

    // 1. Get the singleton DBUpdateManager instance
    private val dbUpdateManager: DBUpdateManager by lazy {
        (requireActivity().application as RackXLateApplication).dbUpdateManager
    }

    // 2. Get the SharedPreferences instance once, as it's needed for UI updates
    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private var downloadManifestButton: Preference? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Now that the view is created, it's safe to start observing the lifecycle-aware flow
        observeUpdateStatus()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        downloadManifestButton  = findPreference("download_manifest")

        downloadManifestButton?.setOnPreferenceClickListener {

            val manifestUrl = sharedPreferences.getString(KEY_MANIFEST_URL, null)

            if (manifestUrl.isNullOrBlank()) {
                Toast.makeText(requireContext(), "Manifest ULR is not set.", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            lifecycleScope.launch {
                dbUpdateManager.checkAndUpdateIfNeeded()
            }

            true
        }
        updateDownloadSummary()
    }

    private fun observeUpdateStatus() {
        dbUpdateManager.updateStatus
            .flowWithLifecycle(viewLifecycleOwner.lifecycle) // Lifecycle-aware collection
            .onEach { status ->
                // This block will run every time the status changes
                when (status) {
                    is UpdateStatus.Success -> {
                        Toast.makeText(requireContext(), "Update successful! New version: ${status.version}", Toast.LENGTH_SHORT).show()
                        updateDownloadSummary() // Refresh the UI with new data
                    }
                    is UpdateStatus.Error -> {
                        Toast.makeText(requireContext(), "Error: ${status.message}", Toast.LENGTH_LONG).show()
                        updateDownloadSummary() // Refresh to show last known good state
                    }
                    is UpdateStatus.Checking -> {
                        Toast.makeText(requireContext(), "Checking for updates...", Toast.LENGTH_SHORT).show()
                    }
                    is UpdateStatus.Idle -> {
                        // Do nothing on idle state
                    }
                }
            }
            .launchIn(viewLifecycleOwner.lifecycleScope) // Launch the collector
    }

    private fun updateDownloadSummary() {
        val version = sharedPreferences.getInt(KEY_MANIFEST_VERSION, -1)
        val lastDownloaded = sharedPreferences.getLong(KEY_MANIFEST_LAST_DOWNLOADED, 0L)

        if (version > 0) {
            val date = Date(lastDownloaded)
            val format = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            )
            val dateString = format.format(date)
            downloadManifestButton?.summary = "Version: $version\nLast downloaded: $dateString"
        } else {
            downloadManifestButton?.summary = "Manifest has not been downloaded yet."
        }
    }

}
