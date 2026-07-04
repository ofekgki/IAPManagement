package com.example.iapsdk.api

import com.example.iapsdk.api.dto.PurchaseDto
import com.example.iapsdk.api.dto.PurchaseItemDto
import com.example.iapsdk.api.dto.UserEntitlementDto
import com.example.iapsdk.models.PurchaseItem
import com.example.iapsdk.models.PurchaseItemType
import com.example.iapsdk.models.PurchaseResult
import com.example.iapsdk.models.PurchaseStatus
import com.example.iapsdk.models.UserEntitlement

/**
 * Conversions between internal wire DTOs and public models. Centralized here so enum parsing and
 * default handling live in exactly one place.
 *
 * String enums are parsed leniently: an unknown/missing value falls back to a safe default rather
 * than throwing, so a backend adding a new enum constant never crashes an older client.
 */

internal fun PurchaseItemDto.toPurchaseItem(): PurchaseItem = PurchaseItem(
    id = id,
    name = name,
    description = description,
    price = price,
    currency = currency,
    type = parseItemType(type),
)

internal fun PurchaseItem.toDto(): PurchaseItemDto = PurchaseItemDto(
    id = id,
    name = name,
    description = description,
    price = price,
    currency = currency,
    type = type.name,
)

internal fun PurchaseDto.toPurchaseResult(): PurchaseResult = PurchaseResult(
    purchaseId = purchaseId,
    itemId = itemId,
    userId = userId,
    status = parseStatus(status),
    purchasedAt = purchasedAt,
    message = message,
    itemName = itemName,
    entitlementStatus = entitlementStatus,
)

internal fun UserEntitlementDto.toUserEntitlement(): UserEntitlement = UserEntitlement(
    itemId = itemId,
    userId = userId,
    type = parseItemType(type),
    isActive = isActive,
    grantedAt = grantedAt,
    expiresAt = expiresAt,
)

private fun parseItemType(raw: String): PurchaseItemType =
    PurchaseItemType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: PurchaseItemType.LIFETIME

private fun parseStatus(raw: String): PurchaseStatus =
    PurchaseStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        ?: PurchaseStatus.PENDING
