package com.vmeasure.app.data.repository

import android.content.Context
import com.vmeasure.app.data.db.dao.AppConfigDao
import com.vmeasure.app.data.db.entity.AppConfigEntity
import com.vmeasure.app.data.db.entity.ConfigKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appConfigDao: AppConfigDao,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val KEY_ACCESS_TOKEN  = "driveAccessToken"
        private const val KEY_REFRESH_TOKEN = "driveRefreshToken"
        private const val KEY_TOKEN_EXPIRY  = "driveTokenExpiry"
    }

    // ── Store tokens after first sign-in ──────────────────────────────────────

    suspend fun storeTokensFromAuthCode(
        serverAuthCode: String,
        clientId: String,
        clientSecret: String
    ): Result<String> {
        return try {
            val body = FormBody.Builder()
                .add("code",          serverAuthCode)
                .add("client_id",     clientId)
                .add("client_secret", clientSecret)
                .add("redirect_uri",  "")
                .add("grant_type",    "authorization_code")
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: return Result.failure(Exception("Empty response from token endpoint"))

            val json = JSONObject(responseBody)

            if (!response.isSuccessful) {
                val error = json.optString("error_description", "Token exchange failed")
                return Result.failure(Exception(error))
            }

            val accessToken  = json.getString("access_token")
            val refreshToken = json.optString("refresh_token", "")
            val expiresIn    = json.optLong("expires_in", 3600L)
            val expiryMillis = System.currentTimeMillis() + (expiresIn * 1000L)

            appConfigDao.set(AppConfigEntity(KEY_ACCESS_TOKEN,  accessToken))
            appConfigDao.set(AppConfigEntity(KEY_TOKEN_EXPIRY,  expiryMillis.toString()))
            if (refreshToken.isNotBlank()) {
                appConfigDao.set(AppConfigEntity(KEY_REFRESH_TOKEN, refreshToken))
            }

            Result.success(accessToken)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Get a valid access token (refresh if expired) ─────────────────────────

    suspend fun getValidAccessToken(
        clientId: String,
        clientSecret: String
    ): Result<String> {
        val storedToken  = appConfigDao.get(KEY_ACCESS_TOKEN)
        val expiryStr    = appConfigDao.get(KEY_TOKEN_EXPIRY)
        val expiryMillis = expiryStr?.toLongOrNull() ?: 0L

        // If token exists and not expired (with 60s buffer), return it
        if (!storedToken.isNullOrBlank() &&
            expiryMillis > System.currentTimeMillis() + 60_000L
        ) {
            return Result.success(storedToken)
        }

        // Attempt refresh
        val refreshToken = appConfigDao.get(KEY_REFRESH_TOKEN)
        if (refreshToken.isNullOrBlank()) {
            return Result.failure(Exception("No refresh token. Please sign in again."))
        }

        return refreshAccessToken(refreshToken, clientId, clientSecret)
    }

    // ── Refresh the access token ──────────────────────────────────────────────

    private suspend fun refreshAccessToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): Result<String> {
        return try {
            val body = FormBody.Builder()
                .add("refresh_token", refreshToken)
                .add("client_id",     clientId)
                .add("client_secret", clientSecret)
                .add("grant_type",    "refresh_token")
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string()
                ?: return Result.failure(Exception("Empty response from token refresh"))

            val json = JSONObject(responseBody)

            if (!response.isSuccessful) {
                val error = json.optString("error_description", "Token refresh failed")
                return Result.failure(Exception(error))
            }

            val accessToken  = json.getString("access_token")
            val expiresIn    = json.optLong("expires_in", 3600L)
            val expiryMillis = System.currentTimeMillis() + (expiresIn * 1000L)

            appConfigDao.set(AppConfigEntity(KEY_ACCESS_TOKEN, accessToken))
            appConfigDao.set(AppConfigEntity(KEY_TOKEN_EXPIRY, expiryMillis.toString()))

            Result.success(accessToken)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Clear all tokens (sign out) ───────────────────────────────────────────

    suspend fun clearTokens() {
        appConfigDao.delete(KEY_ACCESS_TOKEN)
        appConfigDao.delete(KEY_REFRESH_TOKEN)
        appConfigDao.delete(KEY_TOKEN_EXPIRY)
    }

    suspend fun hasStoredTokens(): Boolean {
        return !appConfigDao.get(KEY_ACCESS_TOKEN).isNullOrBlank() ||
                !appConfigDao.get(KEY_REFRESH_TOKEN).isNullOrBlank()
    }
}