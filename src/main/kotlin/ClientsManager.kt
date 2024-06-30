package com.blum.bot

import com.blum.bot.entity.Client
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ClientsManager {
    private val clientsFile = File("clients.json")
    private val mutex = Mutex()

    private var innerClients: List<Client> = clientsFile.let { file ->
        if (!file.exists()) {
            return@let emptyList()
        }
        return@let Json.decodeFromString(file.readText())
    }

    val clients: List<Client>
        get() = runBlocking {
            mutex.withLock {
                innerClients
            }
        }

    suspend fun registerClient() = mutex.withLock {
        var name: String
        while (true) {
            println("Enter client name:")
            name = readln()
            if (innerClients.any { it.name == name }) {
                println("Client with this name already exists")
            } else {
                break
            }
        }
        println("Enter refresh token:")
        val refreshToken = readln()
        val client = Client(name, refreshToken)

        saveClients(innerClients + client)

        println("Client successfully registered")
    }

    suspend fun updateRefreshToken(oldRefreshToken: String, newRefreshToken: String) = mutex.withLock {
        val updatedClients = innerClients.map { client ->
            if (client.refreshToken == oldRefreshToken) {
                client.copy(refreshToken = newRefreshToken)
            } else {
                client
            }
        }

        saveClients(updatedClients)
    }

    suspend fun removeClient(client: Client) = mutex.withLock {
        val updatedClients = innerClients.filter { it.name != client.name }
        saveClients(updatedClients)
    }


    private fun saveClients(clients: List<Client>) {
        innerClients = clients
        clientsFile.writeText(Json.encodeToString(clients))
    }
}