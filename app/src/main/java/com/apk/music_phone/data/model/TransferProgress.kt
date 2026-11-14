package com.apk.music_phone.data.model

enum class TransferStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED
}

data class TransferProgress(
    val transferId: String,
    val fileName: String,
    val trackTitle: String,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val progress: Float = 0f,
    val status: TransferStatus = TransferStatus.PENDING,
    val errorMessage: String? = null
)
