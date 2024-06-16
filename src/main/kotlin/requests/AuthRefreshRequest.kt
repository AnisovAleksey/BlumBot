package com.blum.bot.requests

import kotlinx.serialization.Serializable

@Serializable
class AuthRefreshRequest(val refresh: String)