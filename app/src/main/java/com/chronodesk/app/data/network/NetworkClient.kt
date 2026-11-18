package com.chronodesk.app.data.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(AuthLoggingInterceptor())
            .build()
    }

    val authApi: CodexAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://auth.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CodexAuthApi::class.java)
    }

    val usageApi: CodexUsageApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://chatgpt.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CodexUsageApi::class.java)
    }

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApi::class.java)
    }

    const val CODEX_CLIENT_ID = "app_EMoamEEZ73f0CkXaXp7hrann"
    const val DEVICE_REDIRECT_URI = "https://auth.openai.com/deviceauth/callback"
    const val DEVICE_VERIFY_URL = "https://auth.openai.com/codex/device"
}
