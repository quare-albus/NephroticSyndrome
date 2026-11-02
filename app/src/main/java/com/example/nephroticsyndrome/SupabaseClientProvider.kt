package com.example.nephroticsyndrome

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

/**
 * Supabase is the shared backend for the website and Android app.
 * The anon/publishable key is public client configuration; never place a service-role key here.
 */
object SupabaseClientProvider {
    const val PROJECT_URL = "https://nsjnkhcmlnshyjblzolh.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_1sXk7LPwOZmgLi-H0DAQOA_f_va5kXA"

    val client = createSupabaseClient(
        supabaseUrl = PROJECT_URL,
        supabaseKey = PUBLISHABLE_KEY
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}
