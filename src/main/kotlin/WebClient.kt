package com.blum.bot

import com.blum.bot.requests.AuthRefreshRequest
import com.blum.bot.responses.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.net.URL

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
        val url = URL("https://gateway.blum.codes/v1/auth/refresh")
        val body = jsonParser.encodeToString(AuthRefreshRequest(refresh = refreshToken))
        val response = client.post(url) {
            createHeaders(url=url, content=body)
            setBody(body)
        }.body<AuthRefreshResponse>()

        accessToken = response.access
        clientsManager.updateRefreshToken(
            oldRefreshToken = refreshToken,
            newRefreshToken = response.refresh
        )
        refreshToken = response.refresh

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
}