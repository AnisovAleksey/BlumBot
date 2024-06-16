package com.blum.bot

import com.blum.bot.responses.TaskStatus
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.random.Random

class Bot(private val name: String, private val webClient: WebClient) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    suspend fun start() {
        while (true) {
            try {
                webClient.refreshAuthToken()
                var balance = webClient.getUserBalance()
                log("Current balance: ${balance.availableBalance} | Available plays: ${balance.playPasses}")
                if (balance.farming != null && balance.farming!!.endTime > System.currentTimeMillis()) {
                    log("Current farming time to end: ${(balance.farming!!.endTime - System.currentTimeMillis()) / 1000}s")
                } else {
                    log("No current farming")
                }

                var preferredSleepTime = 0L
                if (balance.farming != null) {
                    if (balance.farming!!.endTime < System.currentTimeMillis()) {
                        val farmingReward = balance.farming!!.balance
                        balance = webClient.claimReward()
                        log("Farming reward claimed | Current balance: ${balance.availableBalance} (+$farmingReward)")
                    } else {
                        preferredSleepTime = balance.farming!!.endTime - System.currentTimeMillis()
                    }
                } else {
                    val farming = webClient.startFarming()
                    log("Farming started, will be end after ${(farming.endTime - System.currentTimeMillis()) / 1000}s")
                }

                checkTasks()

                if (balance.playPasses > 0) {
                    if (doGame() && balance.playPasses > 1) {
                        preferredSleepTime = 1000 * 20
                    }
                }


                log("Sleeping for ${preferredSleepTime / 1000} s")
                delay(preferredSleepTime)
            } catch (e: Exception) {
                logger.error("Error: ${e.message}", e)
                delay(5000)
            }
        }
    }

    private suspend fun checkTasks() {
        val tasks = webClient.getTasks()
            .filter { it.status != TaskStatus.FINISHED }
        if (tasks.isNotEmpty()) {
            val socialSubTasks = tasks.filter { it.type == "SOCIAL_SUBSCRIPTION" }
            log("Found ${socialSubTasks.size} social subscription tasks")
            socialSubTasks.forEach { task ->
                if (task.status == TaskStatus.NOT_STARTED) {
                    webClient.startTask(task.id)
                }
                delay(1000 * 4)
                val tasksMap = webClient.getTasks().associateBy { it.id }
                if (tasksMap[task.id]!!.status == TaskStatus.READY_FOR_CLAIM) {
                    webClient.claimTaskReward(task.id)
                    val balance = webClient.getUserBalance()
                    log("Task `${task.title}` completed | Current balance: ${balance.availableBalance} (+${task.reward})")
                }
            }

            tasks.filter { it.status == TaskStatus.READY_FOR_CLAIM }.forEach {
                webClient.claimTaskReward(it.id)
                val balance = webClient.getUserBalance()
                log("Task `${it.title}` completed | Current balance: ${balance.availableBalance} (+${it.reward})")
            }
        }
    }


    private suspend fun doGame(): Boolean {
        val gameId = webClient.startGame()
        log("Game started with id: $gameId")
        delay(1000 * 31)

        val points = Random.nextInt(250, 300)
        val finished = webClient.finishGame(gameId, points)
        if (finished) {
            val balance = webClient.getUserBalance()
            log("Game finished with result: $points | Current balance: ${balance.availableBalance}")
            return true
        } else {
            log("Game finished with error")
            return false
        }
    }

    private fun log(text: String) {
        logger.info("[$name] $text")
    }
}