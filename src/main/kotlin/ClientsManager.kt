package com.blum.bot

import com.blum.bot.entity.Client
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ClientsManager {
    private val clientsFile = File("clients.json")

    val clients: List<Client>
        get() = clientsFile.let { file ->
            if (!file.exists()) {
                return@let emptyList()
            }
            try {
                return@let Json.decodeFromString(file.readText())
            } catch (e: Exception) {
                return@let emptyList()
            }
        }

    fun registerClient() {
        var name: String
        while (true) {
            println("Enter client name:")
            name = readln()
            if (clients.any { it.name == name }) {
                println("Client with this name already exists")
            } else {
                break
            }
        }
        println("Enter refresh token:")
        val refreshToken = readln()
        val client = Client(name, refreshToken)

        saveClients(clients + client)

        println("Client successfully registered")
    }

    fun updateRefreshToken(oldRefreshToken: String, newRefreshToken: String) {
        val updatedClients = clients.map { client ->
            if (client.refreshToken == oldRefreshToken) {
                client.copy(refreshToken = newRefreshToken)
            } else {
                client
            }
        }

        saveClients(updatedClients)
    }

    fun removeClient(client: Client) {
        val updatedClients = clients.filter { it.name != client.name }
        saveClients(updatedClients)
    }


    private fun saveClients(clients: List<Client>) {
        clientsFile.writeText(Json.encodeToString(clients))
    }
}