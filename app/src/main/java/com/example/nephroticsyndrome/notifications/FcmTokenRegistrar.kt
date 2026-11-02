package com.example.nephroticsyndrome.notifications

import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Call after Supabase sign-in with the session access token and user UUID. */
object FcmTokenRegistrar {
    private val http = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    private const val URL = "https://nsjnkhcmlnshyjblzolh.supabase.co/rest/v1/push_tokens"
    private const val KEY = "sb_publishable_1sXk7LPwOZmgLi-H0DAQOA_f_va5kXA"

    suspend fun register(accessToken: String, userId: String, fcmToken: String) {
        val response = http.post("$URL?on_conflict=token") {
            header("apikey", KEY)
            header("Authorization", "Bearer $accessToken")
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("patient_id", userId)
                put("token", fcmToken)
                put("platform", "android")
            })
        }
        check(response.status.value in 200..299) {
            "Could not register Android push token (${response.status.value})"
        }
    }

    suspend fun unregister(accessToken: String, fcmToken: String) {
        val encoded = java.net.URLEncoder.encode(fcmToken, "UTF-8")
        http.delete("$URL?token=eq.$encoded") {
            header("apikey", KEY)
            header("Authorization", "Bearer $accessToken")
        }
    }
}
