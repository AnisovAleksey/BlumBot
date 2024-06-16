package com.blum.bot

import kotlinx.coroutines.delay
import kotlin.random.Random

class Bot(private val webClient: WebClient) {
    suspend fun start() {
        while (true) {
            try {
                webClient.refreshAuthToken()
                var balance = webClient.getUserBalance()
                println("Current balance: ${balance.availableBalance} | Available plays: ${balance.playPasses}")
                if (balance.farming != null && balance.farming!!.endTime > System.currentTimeMillis()) {
                    println("Current farming time to end: ${(balance.farming!!.endTime - System.currentTimeMillis()) / 1000}s")
                } else {
                    println("No current farming")
                }
                var preferredSleepTime = 0L
                if (balance.farming != null) {
                    if (balance.farming!!.endTime < System.currentTimeMillis()) {
                        val farmingReward = balance.farming!!.balance
                        balance = webClient.claimReward()
                        println("Farming reward claimed | Current balance: ${balance.availableBalance} (+$farmingReward)")
                    } else {
                        preferredSleepTime = balance.farming!!.endTime - System.currentTimeMillis()
                    }
                } else {
                    val farming = webClient.startFarming()
                    println("Farming started, will be end after ${(farming.endTime - System.currentTimeMillis()) / 1000}s")
                }

                if (balance.playPasses > 0) {
                    if (doGame() && balance.playPasses > 1) {
                        preferredSleepTime = 1000 * 20
                    }
                }

                println("Sleeping for ${preferredSleepTime / 1000} s")
                delay(preferredSleepTime)
            } catch (e: Exception) {
                println("Error: ${e.message}")
                delay(5000)
            }
        }
    }


    private suspend fun doGame(): Boolean {
        val gameId = webClient.startGame()
        println("Game started with id: $gameId")
        delay(1000 * 31)

        val points = Random.nextInt(240, 270)
        val finished = webClient.finishGame(gameId, points)
        if (finished) {
            println("Game finished with result: $points")
            return true
        } else {
            println("Game finished with error")
            return false
        }
    }
}