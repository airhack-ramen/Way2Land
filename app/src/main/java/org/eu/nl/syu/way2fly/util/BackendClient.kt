package org.eu.nl.syu.way2fly.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eu.nl.syu.way2fly.model.InboxMessage
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object BackendClient {
    // Replace with your computer's local IP if testing locally on a physical phone,
    // or use the EC2 IP if running against the remote server.
    private const val BASE_URL = "http://18.184.182.90:8080"
    
    // Store the token in memory
    var jwtToken: String? = null

    suspend fun exchangeToken(phone: String, pnr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/v0/admin/users/exchange-token")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val jsonBody = JSONObject().apply {
                put("phoneNumber", phone)
                put("pnr", pnr)
            }.toString()

            OutputStreamWriter(connection.outputStream).use { it.write(jsonBody) }

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                jwtToken = if (json.has("passengerToken") && !json.isNull("passengerToken")) json.getString("passengerToken") else null
                return@withContext jwtToken != null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext false
    }

    suspend fun fetchNotifications(): List<InboxMessage> = withContext(Dispatchers.IO) {
        val token = jwtToken ?: return@withContext emptyList()
        try {
            val url = URL("$BASE_URL/v0/users/me/notifications")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $token")
            
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(response)
                val list = mutableListOf<InboxMessage>()
                
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        InboxMessage(
                            id = UUID.randomUUID().toString(),
                            title = "Admin Announcement",
                            body = obj.optString("message", "New notification"),
                            timestamp = System.currentTimeMillis() // Or parse obj.optString("timestamp")
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }
}