package org.eu.nl.syu.way2fly.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eu.nl.syu.way2fly.BuildConfig
import org.eu.nl.syu.way2fly.model.InboxMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

data class PassengerTokenResponse(
    val passengerToken: String,
    val expiresIn: Long
)

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

data class OrangeCircleArea(
    val areaType: String,
    val center: GeoPoint,
    val radius: Double
)

data class LocationRetrievalResponse(
    val lastLocationTime: String,
    val area: OrangeCircleArea
)

data class LocationVerificationResponse(
    val verificationResult: String,
    val lastLocationTime: String,
    val matchRate: Int? = null
)

data class DeviceReachabilityResponse(
    val lastStatusTime: String,
    val reachabilityStatus: String
)

data class StatusResponse(
    val status: String
)

data class GroupCreateResponse(
    val groupId: String,
    val joinCode: String
)

data class GroupJoinResponse(
    val groupId: String
)

data class RouteDestinationResponse(
    val estimatedMinutes: Int,
    val remainingCheckpoints: List<String>,
    val routeGeoJson: String
)

data class RouteStrollResponse(
    val routeGeoJson: String,
    val returnTime: String
)

data class BackendApiException(
    val statusCode: Int,
    override val message: String
) : Exception(message)

object Way2LandApiClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 15_000
    private val baseUrl = BuildConfig.BACKEND_BASE_URL.trimEnd('/')

    suspend fun exchangePassengerToken(phoneNumber: String, pnr: String): PassengerTokenResponse {
        val response = postJson(
            path = "/v0/admin/users/exchange-token",
            payload = JSONObject()
                .put("phoneNumber", phoneNumber)
                .put("pnr", pnr)
        )

        return PassengerTokenResponse(
            passengerToken = response.getString("passengerToken"),
            expiresIn = response.getLong("expiresIn")
        )
    }

    suspend fun getUserNotifications(passengerToken: String): List<InboxMessage> {
        val response = getJson(
            path = "/v0/users/me/notifications",
            passengerToken = passengerToken
        )

        return buildList {
            for (index in 0 until response.length()) {
                val item = response.getJSONObject(index)
                add(
                    InboxMessage(
                        id = item.optString("id", "backend-notification-$index"),
                        title = item.optString("title", "Airport update"),
                        body = item.optString("message", item.optString("body", "")),
                        timestamp = parseTimestamp(item.optString("timestamp")),
                        actionLabel = item.optString("actionLabel").takeIf { it.isNotBlank() },
                        stepToComplete = if (item.has("stepToComplete") && !item.isNull("stepToComplete")) item.getInt("stepToComplete") else null
                    )
                )
            }
        }
    }

    suspend fun syncUserTags(passengerToken: String, tags: List<String>, replace: Boolean = false): StatusResponse {
        val response = postJsonWithAuth(
            path = "/v0/users/me/tags",
            passengerToken = passengerToken,
            payload = JSONObject()
                .put("tags", JSONArray(tags))
                .put("replace", replace)
        )

        return StatusResponse(status = response.optString("status", "SUCCESS"))
    }

    suspend fun createGroup(passengerToken: String): GroupCreateResponse {
        val response = postJsonWithAuth(
            path = "/v0/groups",
            passengerToken = passengerToken,
            payload = JSONObject()
        )

        return GroupCreateResponse(
            groupId = response.getString("groupId"),
            joinCode = response.getString("joinCode")
        )
    }

    suspend fun joinGroup(passengerToken: String, joinCode: String): GroupJoinResponse {
        val response = postJsonWithAuth(
            path = "/v0/groups/join",
            passengerToken = passengerToken,
            payload = JSONObject().put("joinCode", joinCode)
        )

        return GroupJoinResponse(groupId = response.getString("groupId"))
    }

    suspend fun leaveGroup(passengerToken: String, groupId: String): StatusResponse {
        val response = deleteJsonWithAuth(
            path = "/v0/groups/$groupId/leave",
            passengerToken = passengerToken
        )

        return StatusResponse(status = response.optString("status", "SUCCESS"))
    }

    suspend fun getRouteToDestination(passengerToken: String): RouteDestinationResponse {
        val response = getJsonObjectWithAuth(
            path = "/v0/users/me/routes/destination",
            passengerToken = passengerToken
        )

        return RouteDestinationResponse(
            estimatedMinutes = response.getInt("estimatedMinutes"),
            remainingCheckpoints = response.getJSONArray("remainingCheckpoints").toStringList(),
            routeGeoJson = response.getString("routeGeoJson")
        )
    }

    suspend fun getRouteToStroll(passengerToken: String): RouteStrollResponse {
        val response = getJsonObjectWithAuth(
            path = "/v0/users/me/routes/stroll",
            passengerToken = passengerToken
        )

        return RouteStrollResponse(
            routeGeoJson = response.getString("routeGeoJson"),
            returnTime = response.getString("returnTime")
        )
    }

    suspend fun updateUserLocation(passengerToken: String, latitude: Double, longitude: Double, floor: Int): String {
        val response = patchJson(
            path = "/v0/users/me/location",
            passengerToken = passengerToken,
            payload = JSONObject()
                .put("coordinates", JSONArray().put(latitude).put(longitude))
                .put("floor", floor)
        )

        return response.optString("status", "ACCEPTED")
    }

    suspend fun registerDevice(phoneNumber: String): JSONObject {
        return postJson(
            path = "/v0/admin/devices",
            payload = JSONObject().put("phoneNumber", phoneNumber)
        )
    }

    suspend fun retrieveLocation(phoneNumber: String, maxAge: Int? = null): LocationRetrievalResponse {
        val device = JSONObject().put("phoneNumber", phoneNumber)
        val payload = JSONObject().put("device", device)
        if (maxAge != null) {
            payload.put("maxAge", maxAge)
        }

        val response = postJson("/v0/admin/camara/location-retrieval", payload)
        return LocationRetrievalResponse(
            lastLocationTime = response.getString("lastLocationTime"),
            area = response.getJSONObject("area").toOrangeCircleArea()
        )
    }

    suspend fun verifyLocation(phoneNumber: String, centerLatitude: Double, centerLongitude: Double, radiusMeters: Double, maxAge: Int? = null): LocationVerificationResponse {
        val payload = JSONObject()
            .put("device", JSONObject().put("phoneNumber", phoneNumber))
            .put(
                "area",
                JSONObject()
                    .put("areaType", "CIRCLE")
                    .put(
                        "center",
                        JSONObject()
                            .put("latitude", centerLatitude)
                            .put("longitude", centerLongitude)
                    )
                    .put("radius", radiusMeters)
            )

        if (maxAge != null) {
            payload.put("maxAge", maxAge)
        }

        val response = postJson("/v0/admin/camara/location-verification", payload)
        return LocationVerificationResponse(
            verificationResult = response.getString("verificationResult"),
            lastLocationTime = response.getString("lastLocationTime"),
            matchRate = if (response.has("matchRate") && !response.isNull("matchRate")) response.getInt("matchRate") else null
        )
    }

    suspend fun retrieveDeviceReachability(phoneNumber: String): DeviceReachabilityResponse {
        val response = postJson(
            path = "/v0/admin/camara/device-reachability",
            payload = JSONObject().put("device", JSONObject().put("phoneNumber", phoneNumber))
        )

        return DeviceReachabilityResponse(
            lastStatusTime = response.getString("lastStatusTime"),
            reachabilityStatus = response.getString("reachabilityStatus")
        )
    }

    private suspend fun postJson(path: String, payload: JSONObject): JSONObject {
        return requestJson("POST", path, payload)
    }

    private suspend fun postJsonWithAuth(path: String, passengerToken: String, payload: JSONObject): JSONObject {
        return requestJson("POST", path, payload, passengerToken)
    }

    private suspend fun patchJson(path: String, passengerToken: String, payload: JSONObject): JSONObject {
        return requestJson("PATCH", path, payload, passengerToken)
    }

    private suspend fun deleteJsonWithAuth(path: String, passengerToken: String): JSONObject {
        return requestJson("DELETE", path, JSONObject(), passengerToken)
    }

    private suspend fun getJson(path: String, passengerToken: String): org.json.JSONArray {
        return withContext(Dispatchers.IO) {
            val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $passengerToken")
            }

            try {
                val statusCode = connection.responseCode
                val responseBody = readBody(if (statusCode in 200..299) connection.inputStream else connection.errorStream)

                if (statusCode !in 200..299) {
                    throw BackendApiException(
                        statusCode = statusCode,
                        message = responseBody.ifBlank { "Backend request failed with HTTP $statusCode" }
                    )
                }

                org.json.JSONArray(responseBody)
            } finally {
                connection.disconnect()
            }
        }
    }

    private suspend fun getJsonObjectWithAuth(path: String, passengerToken: String): JSONObject {
        return withContext(Dispatchers.IO) {
            val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer $passengerToken")
            }

            try {
                val statusCode = connection.responseCode
                val responseBody = readBody(if (statusCode in 200..299) connection.inputStream else connection.errorStream)

                if (statusCode !in 200..299) {
                    throw BackendApiException(
                        statusCode = statusCode,
                        message = responseBody.ifBlank { "Backend request failed with HTTP $statusCode" }
                    )
                }

                if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)
            } finally {
                connection.disconnect()
            }
        }
    }

    private suspend fun requestJson(method: String, path: String, payload: JSONObject, passengerToken: String? = null): JSONObject {
        return withContext(Dispatchers.IO) {
            val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                if (passengerToken != null) {
                    setRequestProperty("Authorization", "Bearer $passengerToken")
                }
                if (method != "GET" && method != "HEAD") {
                    doOutput = true
                }
            }

            try {
                if (method != "GET" && method != "HEAD") {
                    connection.outputStream.use { outputStream ->
                        outputStream.write(payload.toString().toByteArray(Charsets.UTF_8))
                    }
                }

                val statusCode = connection.responseCode
                val responseBody = readBody(if (statusCode in 200..299) connection.inputStream else connection.errorStream)

                if (statusCode !in 200..299) {
                    throw BackendApiException(
                        statusCode = statusCode,
                        message = responseBody.ifBlank { "Backend request failed with HTTP $statusCode" }
                    )
                }

                if (responseBody.isBlank()) JSONObject() else JSONObject(responseBody)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun readBody(inputStream: InputStream?): String {
        if (inputStream == null) return ""
        return inputStream.bufferedReader().use { bufferedReader -> bufferedReader.readText() }
    }

    private fun parseTimestamp(value: String?): Long {
        if (value.isNullOrBlank()) return System.currentTimeMillis()
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(System.currentTimeMillis())
    }
}

private fun JSONObject.toOrangeCircleArea(): OrangeCircleArea {
    return OrangeCircleArea(
        areaType = getString("areaType"),
        center = getJSONObject("center").toGeoPoint(),
        radius = getDouble("radius")
    )
}

private fun JSONObject.toGeoPoint(): GeoPoint {
    return GeoPoint(
        latitude = getDouble("latitude"),
        longitude = getDouble("longitude")
    )
}

private fun JSONArray.toStringList(): List<String> {
    return buildList {
        for (index in 0 until length()) {
            add(getString(index))
        }
    }
}