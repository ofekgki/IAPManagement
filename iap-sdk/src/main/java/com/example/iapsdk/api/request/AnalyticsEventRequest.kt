package com.example.iapsdk.api.request

/**
 * Body for `ApiClient.sendAnalyticsEvent`.
 *
 * @property eventName  Name of the event (SDK-internal or a client custom event).
 * @property userId     The user the event is attributed to, if known.
 * @property properties Arbitrary key/value metadata for the event.
 * @property timestamp  Epoch milliseconds when the event occurred.
 */
internal data class AnalyticsEventRequest(
    val eventName: String,
    val userId: String?,
    val properties: Map<String, Any>,
    val timestamp: Long,
)
