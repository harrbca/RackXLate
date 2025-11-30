package io.github.harrbca.rackxlate.db

data class DBManifest(
    val version: Int,
    val downloadUrl: String,
    val sha256: String
)