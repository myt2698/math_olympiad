package com.mathplanet.app

data class Lesson(
    val id: String,
    val grade: Int,
    val title: String,
    val topic: String,
    val durationMinutes: Int,
    val videoUrl: String
)

data class Question(
    val title: String,
    val options: List<String>,
    val answerIndex: Int,
    val explanation: String,
    val videoId: String = "",
    val sourceTitle: String = ""
)

data class DayPlan(
    val index: Int,
    val lessons: List<Lesson>,
    val questions: List<Question>
)

data class DecompositionTask(
    val day: Int,
    val stageTitle: String,
    val focus: String,
    val problem: String,
    val parentPrompt: String
)

data class UserPlan(
    val childName: String,
    val grade: Int,
    val startDate: String
)
