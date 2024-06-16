package com.blum.bot.responses

import kotlinx.serialization.Serializable

@Serializable
data class UserBalanceResponse(
    val availableBalance: Float,
    val playPasses: Int,
    val timestamp: Long,
    val farming: Farming? = null
)

@Serializable
data class Farming(
    val startTime: Long,
    val endTime: Long,
    val earningsRate: Float,
    val balance: Float
)