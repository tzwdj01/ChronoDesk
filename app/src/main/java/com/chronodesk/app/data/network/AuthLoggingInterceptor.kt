package com.chronodesk.app.data.network

import android.util.Log
import com.chronodesk.app.data.security.TokenRedactor
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class AuthLoggingInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val safeUrl = TokenRedactor.redactUrl(request.url.toString())
        val redactedHeaders = request.headers.names().joinToString(", ") { name ->
            val value = if (name.equals("Authorization", ignoreCase = true)) {
                TokenRedactor.redactAuthHeader(request.header(name))
            } else {
                request.header(name)
            }
            "$name: $value"
        }

        Log.d(TAG, "--> ${request.method} $safeUrl [$redactedHeaders]")

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            Log.e(TAG, "<-- HTTP FAILED: $safeUrl : ${e.message}")
            throw e
        }

        Log.d(TAG, "<-- ${response.code} ${response.message} ($safeUrl)")
        return response
    }

    companion object {
        private const val TAG = "ChronoDeskNetwork"
    }
}
