package com.blum.bot

import kotlinx.coroutines.*

fun main(args: Array<String>) = runBlocking {
    val clientsManager = ClientsManager()
    println("Found ${clientsManager.clients.size} clients")

    val action = if (args.size >= 2 && args[0] == "-a") {
        args[1].toInt()
    } else {
        println("Choice option:")
        println("1. Start bot")
        println("2. Register client")
        readln().toInt()
    }

    when (action) {
        1 -> CoroutineScope(Dispatchers.IO).launch {
            clientsManager.clients.forEach { client ->
                launch {
                    println("Launch client `${client.name}`")
                    Bot(client.name, WebClient(client.refreshToken, clientsManager)).start()
                }
            }
        }.join()
        2 -> ClientsManager().registerClient()
        else -> println("Unknown action")
    }
}