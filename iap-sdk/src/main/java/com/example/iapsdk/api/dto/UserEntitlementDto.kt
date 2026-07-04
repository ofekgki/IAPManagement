package com.example.iapsdk.api.dto

/**
 * Wire representation of a user entitlement. Map with `UserEntitlementDto.toUserEntitlement()`.
 */
internal data class UserEntitlementDto(
    val itemId: String,
    val userId: String,
    val type: String,
    val isActive: Boolean,
    val grantedAt: Long,
    val expiresAt: Long? = null,
)
