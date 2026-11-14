package com.apk.music_phone.data.model

import org.json.JSONArray
import org.json.JSONObject

data class WatchFileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val itemCount: Int = 0
) {
    val formattedSize: String
        get() {
            if (isDirectory) return "$itemCount items"
            if (sizeBytes <= 0) return ""
            val mb = sizeBytes / (1024.0 * 1024.0)
            return String.format(java.util.Locale.US, "%.1f MB", mb)
        }
}

data class WatchDirectoryContent(
    val currentPath: String = "/sdcard/Music",
    val parentPath: String? = "/sdcard",
    val items: List<WatchFileItem> = emptyList()
) {
    companion object {
        fun parseJson(jsonString: String): WatchDirectoryContent {
            if (jsonString.isBlank()) return WatchDirectoryContent()
            return try {
                val obj = JSONObject(jsonString)
                val currentPath = obj.optString("currentPath", "/sdcard/Music")
                val parentPath = if (obj.has("parentPath")) obj.getString("parentPath") else if (currentPath != "/sdcard/Music" && currentPath != "/sdcard") "/sdcard/Music" else null
                val itemsArray = obj.optJSONArray("items") ?: JSONArray()
                val itemsList = mutableListOf<WatchFileItem>()

                for (i in 0 until itemsArray.length()) {
                    val itemObj = itemsArray.optJSONObject(i) ?: continue
                    itemsList.add(
                        WatchFileItem(
                            name = itemObj.optString("name", "Unknown"),
                            path = itemObj.optString("path", ""),
                            isDirectory = itemObj.optBoolean("isDirectory", false),
                            sizeBytes = itemObj.optLong("sizeBytes", 0L),
                            itemCount = itemObj.optInt("itemCount", 0)
                        )
                    )
                }

                WatchDirectoryContent(
                    currentPath = currentPath,
                    parentPath = parentPath,
                    items = itemsList
                )
            } catch (e: Exception) {
                e.printStackTrace()
                WatchDirectoryContent()
            }
        }
    }
}
