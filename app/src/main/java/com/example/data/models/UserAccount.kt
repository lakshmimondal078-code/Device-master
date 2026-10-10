package com.example.data.models

data class UserAccount(
    val id: String = "user_default",
    val displayName: String = "Lakshmi Mondal",
    val email: String = "lakshmimondal078@gmail.com",
    val photoUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val securityLevel: String = "Offline Protected",
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
