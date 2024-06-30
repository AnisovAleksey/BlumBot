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
        println("3. Remove client")
        readln().toInt()
    }

    when (action) {
        1 -> CoroutineScope(Dispatchers.IO).launch {
            clientsManager.clients.forEach { client ->
                launch(context = Logger.threadLocal.asContextElement(Logger(client.name))) {
                    Logger.log("Client starting")
                    Bot(client.name, WebClient(client.refreshToken, clientsManager)).start()
                }
            }
        }.join()
        2 -> clientsManager.registerClient()
        3 -> {
            println("Enter client number to remove: ")
            val clients = clientsManager.clients
            for ((index, client) in clients.withIndex()) {
                println("${index + 1}. ${client.name}")
            }
            readln().toInt().let { index ->
                clientsManager.removeClient(clients[index - 1])
            }
        }
        else -> println("Unknown action")
    }
}