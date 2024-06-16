package com.blum.bot

import kotlinx.coroutines.*

fun main() = runBlocking {
    val clientsManager = ClientsManager()
    println("Found ${clientsManager.clients.size} clients")
    println("Choice option:")
    println("1. Start bot")
    println("2. Register client")

    when (readln().toInt()) {
        1 -> CoroutineScope(Dispatchers.IO).launch {
            clientsManager.clients.forEach { client ->
                launch {
                    println("Launch client `${client.name}`")
                    Bot(WebClient(client.refreshToken, clientsManager)).start()
                }
            }
        }.join()
        2 -> ClientsManager().registerClient()
    }
}