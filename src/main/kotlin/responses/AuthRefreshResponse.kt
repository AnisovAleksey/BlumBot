package com.blum.bot.responses

import kotlinx.serialization.Serializable


@Serializable
class AuthRefreshResponse(val access: String, val refresh: String)