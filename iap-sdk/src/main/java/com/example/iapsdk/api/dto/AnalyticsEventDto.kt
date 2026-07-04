package com.example.iapsdk.api.dto

/**
 * Wire representation of an analytics event acknowledged by the backend. Returned (or ignored) by
 * `ApiClient.sendAnalyticsEvent`; present so the analytics endpoint has a typed response shape for
 * when a real backend is wired up.
 */
internal data class AnalyticsEventDto(
    val eventId: String,
    val accepted: Boolean,
)
