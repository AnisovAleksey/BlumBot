package com.blum.bot.responses

import kotlinx.serialization.Serializable

@Serializable
data class TaskResponse(val id: String, val kind: String, val type: String, val status: TaskStatus, val title: String, val reward: Int, val subTasks: List<TaskResponse> = listOf())

@Serializable
enum class TaskStatus {
    STARTED, NOT_STARTED, FINISHED, READY_FOR_CLAIM
}