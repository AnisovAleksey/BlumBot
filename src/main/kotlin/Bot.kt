package com.blum.bot

import com.blum.bot.exceptions.RefreshTokenException
import com.blum.bot.exceptions.RefreshTokenExpiredException
import com.blum.bot.responses.TaskStatus
import kotlinx.coroutines.delay
import kotlin.random.Random

class Bot(private val name: String, private val webClient: WebClient) {
    suspend fun start() {
        while (true) {
            try {
                webClient.refreshAuthToken()

                if (webClient.checkDailyReward() && webClient.claimDailyReward()) {
                    Logger.log("Daily reward successfully received")
                }

                var balance = webClient.getUserBalance()
                Logger.log("Current balance: ${balance.availableBalance} | Available plays: ${balance.playPasses}")
                if (balance.farming != null && balance.farming!!.endTime > System.currentTimeMillis()) {
                    Logger.log("Current farming time to end: ${(balance.farming!!.endTime - System.currentTimeMillis()) / 1000}s")
                } else {
                    Logger.log("No current farming")
                }

                var preferredSleepTime = 0L
                if (balance.farming != null) {
                    if (balance.farming!!.endTime < System.currentTimeMillis()) {
                        val farmingReward = balance.farming!!.balance
                        balance = webClient.claimReward()
                        Logger.log("Farming reward claimed | Current balance: ${balance.availableBalance} (+$farmingReward)")
                    } else {
                        preferredSleepTime = balance.farming!!.endTime - System.currentTimeMillis()
                    }
                } else {
                    val farming = webClient.startFarming()
                    Logger.log("Farming started, will be end after ${(farming.endTime - System.currentTimeMillis()) / 1000}s")
                }

                checkTasks()

                if (balance.playPasses > 0) {
                    if (doGame() && balance.playPasses > 1) {
                        preferredSleepTime = 1000 * 20
                    }
                }


                if (preferredSleepTime == 0L) {
                    preferredSleepTime = 1000 * 60 * 20 // 20 min
                }
                Logger.log("Sleep for ${preferredSleepTime / 1000} s")
                delay(preferredSleepTime)
            } catch (e: RefreshTokenException) {
                Logger.log("Sleep for 1 hour due to token update error")
                delay(1000 * 60 * 60)
            } catch (e: RefreshTokenExpiredException) {
                Logger.log("Close instance because token is expired")
                break
            } catch (e: Exception) {
                Logger.error("Error: ${e.message}\nSleep for half-hour", e)
                delay(1000 * 60 * 30)
            }
        }
    }

    private suspend fun checkTasks() {
        val tasks = webClient.getTasks()
            .filter { it.status != TaskStatus.FINISHED && it.status != TaskStatus.STARTED }
        if (tasks.isNotEmpty()) {
            val socialSubTasks = tasks.filter { it.type == "SOCIAL_SUBSCRIPTION" }
            Logger.log("Found ${socialSubTasks.size} social subscription tasks")
            socialSubTasks.forEach { task ->
                if (task.status == TaskStatus.NOT_STARTED) {
                    webClient.startTask(task.id)
                }
                delay(1000 * 4)
                val tasksMap = webClient.getTasks().associateBy { it.id }
                if (tasksMap[task.id]!!.status == TaskStatus.READY_FOR_CLAIM) {
                    webClient.claimTaskReward(task.id)
                    val balance = webClient.getUserBalance()
                    Logger.log("Task `${task.title}` completed | Current balance: ${balance.availableBalance} (+${task.reward})")
                }
            }

            tasks.filter { it.status == TaskStatus.READY_FOR_CLAIM }.forEach {
                webClient.claimTaskReward(it.id)
                val balance = webClient.getUserBalance()
                Logger.log("Task `${it.title}` completed | Current balance: ${balance.availableBalance} (+${it.reward})")
            }
        }
    }


    private suspend fun doGame(): Boolean {
        val gameId = webClient.startGame()
        Logger.log("Game started with id: $gameId")
        delay(1000 * 31)

        val points = Random.nextInt(240, 270)
        val finished = webClient.finishGame(gameId, points)
        if (finished) {
            val balance = webClient.getUserBalance()
            Logger.log("Game finished with result: $points | Current balance: ${balance.availableBalance}")
            return true
        } else {
            Logger.log("Game finished with error")
            return false
        }
    }
}