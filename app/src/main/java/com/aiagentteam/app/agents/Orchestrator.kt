package com.aiagentteam.app.agents

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.delay

class Orchestrator {
    val statuses = mutableStateMapOf<AgentRole, AgentStatus>()
    val log = mutableStateListOf<LogEntry>()
    val running = mutableStateOf(false)

    init { reset() }

    fun reset() {
        AgentRole.values().forEach { statuses[it] = AgentStatus.WAITING }
        log.clear()
    }

    private fun say(role: AgentRole, msg: String) {
        log.add(LogEntry(role, msg))
    }

    private suspend fun step(role: AgentRole, work: () -> String) {
        statuses[role] = AgentStatus.WORKING
        delay(400)
        try {
            say(role, work())
            statuses[role] = AgentStatus.COMPLETED
        } catch (e: Exception) {
            say(role, "Error: ${e.message}")
            statuses[role] = AgentStatus.FAILED
            throw e
        }
    }

    suspend fun run(task: TaskInput) {
        if (running.value) return
        running.value = true
        reset()
        try {
            step(AgentRole.ANALYST) {
                "Task received for ${task.app}: ${task.instruction.take(80)}"
            }
            step(AgentRole.PERSON) {
                if (task.personInfo.isBlank()) "No extra info about ${task.person}"
                else "Context loaded for ${task.person} (${task.personInfo.length} chars)"
            }
            step(AgentRole.STYLE) {
                "Style profile not connected yet (no writing samples)"
            }
            step(AgentRole.PLANNER) {
                "Plan: open ${task.app}, find ${task.person}, prepare message"
            }
            step(AgentRole.EXECUTOR) {
                if (task.mode == RunMode.REVIEW) "Prepared. Waiting for your approval"
                else "Auto mode selected. Accessibility service not connected yet"
            }
            step(AgentRole.REVIEWER) { "Review pass done. Model not connected yet" }
            step(AgentRole.CORRECTOR) { "No corrections needed" }
        } catch (e: Exception) {
            // status already recorded
        } finally {
            running.value = false
        }
    }
}
