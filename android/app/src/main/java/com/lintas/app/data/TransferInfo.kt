package com.lintas.app.data

import kotlinx.serialization.Serializable

@Serializable
data class TransferInfo(
    val code: String = "",
    val filename: String = "",
    val size: Long = 0,
    val type: String = "",
    val status: String = "",
    val created_at: String = "",
    val expires_at: String = ""
)

@Serializable
data class ApiResponse(
    val success: Boolean = false,
    val message: String = "",
    val data: TransferInfo? = null
)
