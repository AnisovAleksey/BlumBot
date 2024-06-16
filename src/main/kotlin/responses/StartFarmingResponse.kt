package com.blum.bot.responses

import kotlinx.serialization.Serializable

@Serializable
class StartFarmingResponse(val startTime: Long, val endTime: Long, val earningsRate: Float, val balance: Float)
