package com.example.iapsdk.api

import android.util.Log
import com.example.iapsdk.api.dto.AnalyticsEventDto
import com.example.iapsdk.api.dto.PurchaseDto
import com.example.iapsdk.api.dto.PurchaseItemDto
import com.example.iapsdk.api.dto.UserEntitlementDto
import com.example.iapsdk.api.request.AnalyticsEventRequest
import com.example.iapsdk.api.request.CreatePurchaseRequest
import com.example.iapsdk.config.PurchaseSdkConfig
import com.example.iapsdk.config.SdkEnvironment
import com.example.iapsdk.models.PurchaseException
import com.example.iapsdk.models.PurchaseSdkError
import com.example.iapsdk.models.PurchaseStatus
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * The single seam between the SDK and the backend. Every networked operation lives behind these
 * suspend functions, so the rest of the SDK never touches transport details.
 *
 * ### Behavior
 * This is a **real HTTP client** for the platform backend's SDK API under `/api/v1/sdk`. It uses the
 * JDK's [HttpURLConnection] (no extra networking dependency) and Gson (already used for the local
 * cache) to read the standard `{ success, data, error, requestId }` envelope. All calls send the
 * `X-SDK-API-Key` header from `PurchaseSdk.init`.
 *
 * It is the **MOCK-mode** client: purchases are simulated server-side (no real payment), which is
 * exactly what the demo/educational flow needs. The real Google Play path lives in
 * `GooglePlayBillingProvider` and is still scaffolded.
 *
 * The backend purchase flow is two-step (`start` then `confirm`), and `confirm` needs the item/user.
 * To keep the existing `confirmPurchase(purchaseId)` signature, [createPurchase] remembers the
 * in-flight purchase's item/user/idempotency-key in [pending] and [confirmPurchase] reads them back.
 *
 * @property apiKey The API key from `PurchaseSdk.init`, sent as the `X-SDK-API-Key` header.
 * @property config Drives [sdkBaseUrl] resolution (and analytics/logging toggles elsewhere).
 */
internal class ApiClient(
    private val apiKey: String,
    private val config: PurchaseSdkConfig,
) {

    private val gson = Gson()

    /**
     * Base URL of the backend SDK API, e.g. {@code http://10.0.2.2:8080/api/v1/sdk} from the Android
     * emulator. Pass it via `PurchaseSdkConfig(baseUrl = ...)`. When null, an environment default is
     * used (placeholder hosts for SANDBOX/PRODUCTION until a real deployment exists).
     */
    val sdkBaseUrl: String = (config.baseUrl?.trimEnd('/'))
        ?: when (config.environment) {
            SdkEnvironment.SANDBOX -> "https://sandbox.api.example.com/api/v1/sdk"
            SdkEnvironment.PRODUCTION -> "https://api.example.com/api/v1/sdk"
        }

    /** In-flight purchases started but not yet confirmed: purchaseId -> (itemId, userId, idemKey). */
    private val pending = ConcurrentHashMap<String, PendingPurchase>()

    private data class PendingPurchase(val itemId: String, val userId: String, val idempotencyKey: String)

    /**
     * Fetches a single item by id. GET /items/{itemId}.
     * @throws PurchaseException `ITEM_NOT_FOUND` if the catalog has no such item.
     */
    suspend fun fetchItem(itemId: String): PurchaseItemDto {
        val data = call("GET", "/items/${encode(itemId)}")
        return parseItem(data)
    }

    /** Fetches the full active catalog. GET /items. */
    suspend fun fetchItems(): List<PurchaseItemDto> {
        val data = call("GET", "/items")
        return data.getAsJsonArray("items").map { parseItem(it.asJsonObject) }
    }

    /**
     * Starts a (CREATED) purchase on the backend. POST /purchases/start. The returned [PurchaseDto]
     * is PENDING; complete it with [confirmPurchase].
     */
    suspend fun createPurchase(request: CreatePurchaseRequest): PurchaseDto {
        val body = JsonObject().apply {
            addProperty("userId", request.userId)
            addProperty("itemId", request.itemId)
            addProperty("billingMode", "MOCK")
            request.paymentMethodId?.let { addProperty("paymentMethod", it) }
        }
        val data = call("POST", "/purchases/start", body)
        val purchaseId = data.get("purchaseId").asString
        pending[purchaseId] = PendingPurchase(request.itemId, request.userId, request.idempotencyKey)
        return PurchaseDto(
            purchaseId = purchaseId,
            itemId = request.itemId,
            userId = request.userId,
            status = optString(data, "status") ?: PurchaseStatus.PENDING.name,
            purchasedAt = System.currentTimeMillis(),
            message = null,
        )
    }

    /**
     * Confirms (completes) a previously started purchase. POST /purchases/confirm with the stable
     * `Idempotency-Key` header, so a double-tap collapses into one purchase server-side.
     */
    suspend fun confirmPurchase(purchaseId: String): PurchaseDto = withContext(Dispatchers.IO) {
        val info = pending[purchaseId]
            ?: throw PurchaseException(PurchaseSdkError.PurchaseFailed)
        val body = JsonObject().apply {
            addProperty("purchaseId", purchaseId)
            addProperty("userId", info.userId)
            addProperty("itemId", info.itemId)
            addProperty("billingMode", "MOCK")
            // TODO(google-billing): in GOOGLE_PLAY mode, attach { purchaseToken, orderId, productId }
            //  under a "googlePlay" object so the backend can verify it before granting access.
        }
        val data = call("POST", "/purchases/confirm", body, idempotencyKey = info.idempotencyKey)
        pending.remove(purchaseId)
        PurchaseDto(
            purchaseId = optString(data, "purchaseId") ?: purchaseId,
            itemId = info.itemId,
            userId = info.userId,
            status = optString(data, "status") ?: PurchaseStatus.SUCCESS.name,
            purchasedAt = System.currentTimeMillis(),
            message = "Purchase confirmed",
        )
    }

    /**
     * Marks a started purchase as cancelled locally. The backend SDK API has no cancel endpoint
     * (a CREATED purchase is simply never confirmed), so this only forgets the in-flight purchase.
     *
     * // TODO(backend): add POST /purchases/{id}/cancel if explicit server-side cancellation is needed.
     */
    suspend fun cancelPurchase(purchaseId: String): PurchaseDto {
        val info = pending.remove(purchaseId)
        return PurchaseDto(
            purchaseId = purchaseId,
            itemId = info?.itemId ?: "",
            userId = info?.userId ?: "",
            status = PurchaseStatus.CANCELLED.name,
            purchasedAt = System.currentTimeMillis(),
            message = "Purchase cancelled",
        )
    }

    /**
     * Returns the user's prior successful purchases (used by restore). POST /purchases/restore.
     * When [itemId] is non-null, only that single item is returned (per-item return).
     */
    suspend fun restorePurchases(userId: String, itemId: String? = null): List<PurchaseDto> {
        val body = JsonObject().apply {
            addProperty("userId", userId)
            addProperty("billingMode", "MOCK")
            if (itemId != null) addProperty("itemId", itemId)
        }
        val data = call("POST", "/purchases/restore", body)
        return data.getAsJsonArray("restoredPurchases").map {
            val o = it.asJsonObject
            PurchaseDto(
                purchaseId = optString(o, "purchaseId") ?: "",
                itemId = optString(o, "itemId") ?: "",
                userId = userId,
                status = optString(o, "status") ?: PurchaseStatus.SUCCESS.name,
                purchasedAt = System.currentTimeMillis(),
                message = null,
                itemName = optString(o, "itemName"),
                entitlementStatus = optString(o, "entitlementStatus"),
            )
        }
    }

    /** Returns the user's entitlements. GET /entitlements?userId=... */
    suspend fun fetchEntitlements(userId: String): List<UserEntitlementDto> {
        val data = call("GET", "/entitlements?userId=${encode(userId)}")
        return data.getAsJsonArray("entitlements").map {
            val o = it.asJsonObject
            val status = optString(o, "status")
            UserEntitlementDto(
                // The backend keys entitlements by sourceItemId for the SDK's per-item checks.
                itemId = optString(o, "sourceItemId") ?: "",
                userId = userId,
                // The SDK contract has no item type on entitlements; mapper defaults to LIFETIME.
                type = "",
                isActive = status.equals("ACTIVE", ignoreCase = true),
                grantedAt = System.currentTimeMillis(),
                expiresAt = parseInstantMillis(optString(o, "expiresAt")),
            )
        }
    }

    /** Sends one analytics event. POST /analytics/events. Never throws to callers (best-effort). */
    suspend fun sendAnalyticsEvent(request: AnalyticsEventRequest): AnalyticsEventDto {
        val props = request.properties
        val body = JsonObject().apply {
            request.userId?.let { addProperty("userId", it) }
            addProperty("eventName", request.eventName)
            (props["billingMode"]?.toString())?.let { addProperty("billingMode", it) }
            (props["item_id"]?.toString())?.let { addProperty("itemId", it) }
            (props["purchase_id"]?.toString())?.let { addProperty("purchaseId", it) }
            add("metadata", gson.toJsonTree(props))
        }
        return try {
            val data = call("POST", "/analytics/events", body)
            AnalyticsEventDto(eventId = "evt_${UUID.randomUUID()}", accepted = optBool(data, "stored"))
        } catch (t: Throwable) {
            // Analytics must never break a flow; swallow and report "not accepted".
            if (config.enableLogs) Log.w(TAG, "analytics event failed: ${request.eventName}", t)
            AnalyticsEventDto(eventId = "evt_local_${UUID.randomUUID()}", accepted = false)
        }
    }

    // --- HTTP plumbing -----------------------------------------------------------------------

    /**
     * Performs an HTTP call and returns the `data` object from the response envelope. Throws a
     * [PurchaseException] with a mapped [PurchaseSdkError] on a non-success envelope or transport
     * failure.
     */
    private suspend fun call(
        method: String,
        path: String,
        body: JsonObject? = null,
        idempotencyKey: String? = null,
    ): JsonObject = withContext(Dispatchers.IO) {
        val url = URL(sdkBaseUrl + path)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("X-SDK-API-Key", apiKey)
            idempotencyKey?.let { setRequestProperty("Idempotency-Key", it) }
        }
        try {
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = conn.responseCode
            val stream = if (status in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (config.enableLogs) Log.d(TAG, "$method $path -> $status")
            parseEnvelope(text)
        } catch (e: PurchaseException) {
            throw e
        } catch (e: IOException) {
            throw PurchaseException(PurchaseSdkError.BillingUnavailable)
        } catch (t: Throwable) {
            throw PurchaseException(PurchaseSdkError.Unknown(t.message ?: "Network error."))
        } finally {
            conn.disconnect()
        }
    }

    /** Parses the envelope, returning `data` on success or throwing a mapped error on failure. */
    private fun parseEnvelope(text: String): JsonObject {
        if (text.isBlank()) throw PurchaseException(PurchaseSdkError.BillingUnavailable)
        val root = JsonParser.parseString(text).asJsonObject
        val success = root.get("success")?.takeIf { !it.isJsonNull }?.asBoolean ?: false
        if (!success || root.get("data") == null || root.get("data").isJsonNull) {
            val err = root.getAsJsonObject("error")
            val code = err?.get("code")?.takeIf { !it.isJsonNull }?.asString ?: "UNKNOWN"
            val message = err?.get("message")?.takeIf { !it.isJsonNull }?.asString
            throw PurchaseException(mapError(code, message))
        }
        return root.getAsJsonObject("data")
    }

    /** Maps a backend [ErrorCode] string to the SDK's closed error set. */
    private fun mapError(code: String, message: String?): PurchaseSdkError = when (code) {
        "ITEM_NOT_FOUND" -> PurchaseSdkError.ItemNotFound
        "PURCHASE_CANCELLED" -> PurchaseSdkError.PurchaseCancelled
        "GOOGLE_PLAY_NOT_CONFIGURED" -> PurchaseSdkError.GooglePlayNotConfigured
        "VERIFICATION_REQUIRED", "PURCHASE_VERIFICATION_FAILED" -> PurchaseSdkError.VerificationRequired
        "PURCHASE_FAILED", "PURCHASE_NOT_FOUND", "PURCHASE_ALREADY_PROCESSED" -> PurchaseSdkError.PurchaseFailed
        "INVALID_API_KEY", "APP_DISABLED", "NOT_INITIALIZED" ->
            PurchaseSdkError.Unknown(message ?: "The SDK API key was rejected by the backend.")
        else -> PurchaseSdkError.Unknown(message ?: "Request failed ($code).")
    }

    private fun parseItem(o: JsonObject): PurchaseItemDto = PurchaseItemDto(
        id = optString(o, "itemId") ?: optString(o, "id") ?: "",
        name = optString(o, "name") ?: "",
        description = optString(o, "description") ?: "",
        price = parsePrice(optString(o, "priceDisplay")),
        currency = optString(o, "currency") ?: "USD",
        type = optString(o, "type") ?: "LIFETIME",
    )

    private fun optString(o: JsonObject, key: String): String? =
        o.get(key)?.takeIf { !it.isJsonNull }?.asString

    private fun optBool(o: JsonObject, key: String): Boolean =
        o.get(key)?.takeIf { !it.isJsonNull }?.asBoolean ?: false

    /** Extracts a numeric price from a display string like "$4.99" → 4.99 (best effort). */
    private fun parsePrice(display: String?): Double {
        if (display.isNullOrBlank()) return 0.0
        val digits = display.filter { it.isDigit() || it == '.' }
        return digits.toDoubleOrNull() ?: 0.0
    }

    /**
     * Parses an ISO-8601 instant (e.g. "2026-07-20T12:00:00Z") to epoch millis without java.time, so
     * it works on the SDK's minSdk (24). Returns null for null/blank/unparseable input.
     */
    private fun parseInstantMillis(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching {
            val core = value.substringBefore('.').take(19) // "yyyy-MM-ddTHH:mm:ss"
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            fmt.parse(core)?.time
        }.getOrNull()
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private companion object {
        const val TAG = "IapApiClient"
    }
}
