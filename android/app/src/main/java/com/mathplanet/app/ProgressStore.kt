package com.mathplanet.app

import android.content.Context
import java.time.LocalDate

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("math_planet_progress", Context.MODE_PRIVATE)

    fun userPlan(): UserPlan? {
        val name = prefs.getString("child_name", null) ?: return null
        val date = prefs.getString("start_date", null) ?: return null
        return UserPlan(name, prefs.getInt("grade", 1), date)
    }

    fun createPlan(name: String, grade: Int, startDate: LocalDate): Boolean {
        if (userPlan() != null) return false
        prefs.edit()
            .putString("child_name", name)
            .putInt("grade", grade)
            .putString("start_date", startDate.toString())
            .apply()
        return true
    }

    fun markLessonComplete(id: String) {
        prefs.edit().putBoolean("lesson_$id", true).apply()
    }

    fun isLessonComplete(id: String): Boolean = prefs.getBoolean("lesson_$id", false)

    fun saveQuestionAnswer(videoId: String, correct: Boolean) {
        prefs.edit()
            .putBoolean("question_answered_$videoId", true)
            .putBoolean("question_correct_$videoId", correct || isQuestionCorrect(videoId))
            .putBoolean("question_wrong_$videoId", !correct || hadQuestionError(videoId))
            .apply()
    }

    fun isQuestionAnswered(videoId: String): Boolean = prefs.getBoolean("question_answered_$videoId", false)
    fun isQuestionCorrect(videoId: String): Boolean = prefs.getBoolean("question_correct_$videoId", false)
    fun hadQuestionError(videoId: String): Boolean = prefs.getBoolean("question_wrong_$videoId", false)

    fun markDayComplete(date: LocalDate, score: Int, total: Int) {
        prefs.edit().putString("day_$date", "$score/$total").apply()
    }

    fun dayScore(date: LocalDate): String? = prefs.getString("day_$date", null)

    fun saveDayReward(date: LocalDate, answered: Int, correct: Int, perfect: Boolean) {
        prefs.edit()
            .putInt("answered_$date", maxOf(answered, answered(date)))
            .putInt("correct_$date", maxOf(correct, correct(date)))
            .putBoolean("perfect_$date", perfect || perfect(date))
            .apply()
    }

    fun answered(date: LocalDate): Int = prefs.getInt("answered_$date", 0)
    fun correct(date: LocalDate): Int = prefs.getInt("correct_$date", 0)
    fun perfect(date: LocalDate): Boolean = prefs.getBoolean("perfect_$date", false)

    fun claimMilestone(day: Int): Boolean {
        if (prefs.getBoolean("milestone_$day", false)) return false
        prefs.edit().putBoolean("milestone_$day", true).apply()
        return true
    }

    fun milestoneClaimed(day: Int): Boolean = prefs.getBoolean("milestone_$day", false)

    fun streak(today: LocalDate = LocalDate.now()): Int {
        var result = 0
        var cursor = today
        while (dayScore(cursor) != null) {
            result++
            cursor = cursor.minusDays(1)
        }
        return result
    }
}
