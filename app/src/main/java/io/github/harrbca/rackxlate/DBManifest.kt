package io.github.harrbca.rackxlate

data class DBManifest(
    val version: Int,
    val downloadUrl: String,
    val sha256: String
)