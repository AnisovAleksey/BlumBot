package com.blum.bot.entity

import kotlinx.serialization.Serializable

@Serializable
data class Client(val name: String, val refreshToken: String)
