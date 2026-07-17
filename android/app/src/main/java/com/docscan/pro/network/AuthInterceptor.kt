package com.docscan.pro.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Supplies the current Firebase ID token. In production this is backed by
 * FirebaseAuth.getInstance().currentUser?.getIdToken(...). The scaffold uses a
 * settable holder so the app compiles and runs without Firebase wired up yet.
 */
@Singleton
class TokenProvider @Inject constructor() {
    @Volatile
    var token: String? = null
}

/** Attaches `Authorization: Bearer <firebase id token>` to every request. */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenProvider: TokenProvider,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider.token
        val authed = if (token != null) {
            request.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(authed)
    }
}
