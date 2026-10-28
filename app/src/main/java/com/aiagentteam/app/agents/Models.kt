package com.aiagentteam.app.agents

enum class AgentRole(val title: String) {
    ANALYST("المحلل"),
    PERSON("محلل الشخص"),
    STYLE("محلل الأسلوب"),
    PLANNER("المخطط"),
    EXECUTOR("المنفذ"),
    REVIEWER("المراجع"),
    CORRECTOR("المصحح")
}

enum class AgentStatus(val label: String) {
    WAITING("ينتظر"),
    WORKING("يعمل"),
    COMPLETED("مكتمل"),
    FAILED("فشل")
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
    val mode: RunMode,
    val pkg: String = ""
)
