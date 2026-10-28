package com.aiagentteam.app.agents

enum class AgentRole(val title: String) {
    ANALYST("Analyst"),
    PERSON("Person Analyst"),
    STYLE("Style Analyst"),
    PLANNER("Planner"),
    EXECUTOR("Executor"),
    REVIEWER("Reviewer"),
    CORRECTOR("Corrector")
}

enum class AgentStatus(val label: String) {
    WAITING("Waiting"),
    WORKING("Working"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

data class LogEntry(
    val role: AgentRole,
    val message: String,
    val time: Long = System.currentTimeMillis()
)

enum class RunMode { REVIEW, AUTO }

data class TaskInput(
    val app: String,
    val person: String,
    val personInfo: String,
    val instruction: String,
    val mode: RunMode
)
