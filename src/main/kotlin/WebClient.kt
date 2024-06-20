package com.blum.bot

import com.blum.bot.exceptions.RefreshTokenException
import com.blum.bot.exceptions.RefreshTokenExpiredException
import com.blum.bot.requests.AuthRefreshRequest
import com.blum.bot.responses.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.URL
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class WebClient(private var refreshToken: String, private val clientsManager: ClientsManager) {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json = jsonParser)
        }
        install(ContentEncoding) {
            deflate(1.0F)
            gzip(0.9F)
        }
    }

    private var accessToken: String? = null

    suspend fun refreshAuthToken() {
        if (isTokenExpired(refreshToken)) {
            throw RefreshTokenExpiredException()
        }

        val url = URL("https://gateway.blum.codes/v1/auth/refresh")
        val body = jsonParser.encodeToString(AuthRefreshRequest(refresh = refreshToken))
        val response = client.post(url) {
            createHeaders(url=url, content=body)
            setBody(body)
        }

        try {
            val authRefreshResponse = response.body<AuthRefreshResponse>()
            accessToken = authRefreshResponse.access
            clientsManager.updateRefreshToken(
                oldRefreshToken = refreshToken,
                newRefreshToken = authRefreshResponse.refresh
            )
            println("new refresh token: $refreshToken")
            refreshToken = authRefreshResponse.refresh
        } catch (e: JsonConvertException) {
            Logger.error("Failed to refresh auth token: ${response.bodyAsText()}", e)
            throw RefreshTokenException()
        }
    }

    suspend fun getUserBalance(): UserBalanceResponse {
        val url = URL("https://game-domain.blum.codes/api/v1/user/balance")
        return client.get(url) {
            createHeaders(url=url)
            headers { append("Authorization", "Bearer $accessToken") }
        }.body<UserBalanceResponse>()
    }

    suspend fun claimReward(): UserBalanceResponse {
        val url = URL("https://game-domain.blum.codes/api/v1/farming/claim")
        return client.post(url) {
            createHeaders(url = url)
            headers { append("Authorization", "Bearer $accessToken") }
        }.body<UserBalanceResponse>()
    }

    suspend fun startFarming(): StartFarmingResponse {
        val url = URL("https://game-domain.blum.codes/api/v1/farming/start")
        return client.post(url) {
            createHeaders(url = url)
            headers { append("Authorization", "Bearer $accessToken") }
        }.body<StartFarmingResponse>()
    }

    suspend fun startGame(): String {
        val url = URL("https://game-domain.blum.codes/api/v1/game/play")

        val response = client.post(url) {
            createHeaders(url = url)
            headers { append("Authorization", "Bearer $accessToken") }
        }.body<JsonElement>()

        return response.jsonObject["gameId"]!!.jsonPrimitive.content
    }

    suspend fun finishGame(gameId: String, points: Int): Boolean {
        val url = URL("https://game-domain.blum.codes/api/v1/game/claim")
        val body = jsonParser.encodeToString(JsonObject(mapOf("gameId" to JsonPrimitive(gameId), "points" to JsonPrimitive(points))))
        val response = client.post(url) {
            createHeaders(url = url, content = body)
            headers { append("Authorization", "Bearer $accessToken") }
            setBody(body)
        }.body<String>()

        return response == "OK"
    }

    suspend fun getTasks(): List<TaskResponse> {
        val url = URL("https://game-domain.blum.codes/api/v1/tasks")
        return client.get(url) {
            createHeaders(url = url)
            headers { append("Authorization", "Bearer $accessToken") }
        }.body()
    }

    suspend fun startTask(taskId: String): Boolean {
        val url = URL("https://game-domain.blum.codes/api/v1/tasks/$taskId/start")
        val response = client.post(url) {
            createHeaders(url = url)
            headers { append("Authorization", "Bearer $accessToken") }
        }.bodyAsText()

        try {
            val task = jsonParser.decodeFromString<TaskResponse>(response)
            return task.status == TaskStatus.STARTED
        } catch (e: Exception) {
            return jsonParser.decodeFromString<JsonElement>(response)
                .jsonObject["message"]
                ?.let { it.jsonPrimitive.content.equals("Task is already started", ignoreCase=true) } ?: false
        }
    }

    suspend fun claimTaskReward(taskId: String): Boolean {
        val url = URL("https://game-domain.blum.codes/api/v1/tasks/$taskId/claim")
        val task = client.post(url) {
            createHeaders(url = url)
            headers { append("Authorization", "Bearer $accessToken") }
        }.body<TaskResponse>()
        return task.status == TaskStatus.FINISHED
    }

    suspend fun checkDailyReward(): Boolean {
        val url = URL("https://game-domain.blum.codes/api/v1/daily-reward?offset=-420")
        val response = client.get(url) {
            createHeaders(url = url)
            headers { append("Authorization", "Bearer $accessToken") }
        }

        return response.status.value == 200
    }

    suspend fun claimDailyReward(): Boolean {
        val url = URL("https://game-domain.blum.codes/api/v1/daily-reward?offset=-420")
        val response = client.post(url) {
            createHeaders(url = url)
            headers { append("Authorization", "Bearer $accessToken") }
        }

        return response.status.value == 200
                && response.bodyAsText().equals("OK", ignoreCase = true)
    }
}

data class JwtPayload(val exp: Long)

@OptIn(ExperimentalEncodingApi::class)
fun decodeBase64(base64: String): String {
    return String(Base64.decode(base64))
}

fun parseJwtPayload(token: String): JwtPayload? {
    val parts = token.split(".")
    if (parts.size != 3) return null

    val payloadJson = decodeBase64(parts[1])
    val regex = """"exp"\s*:\s*(\d+)""".toRegex()
    val matchResult = regex.find(payloadJson) ?: return null
    val exp = matchResult.groupValues[1].toLongOrNull() ?: return null

    return JwtPayload(exp)
}

fun isTokenExpired(token: String): Boolean {
    val payload = parseJwtPayload(token) ?: return true
    val currentTime = System.currentTimeMillis() / 1000
    return currentTime > payload.exp
}