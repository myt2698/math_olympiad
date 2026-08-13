package com.mathplanet.app

import android.content.Context
import org.json.JSONArray

class CourseRepository(private val context: Context) {
    private val allLessons: List<Lesson> by lazy { loadLessons() }
    private val allQuestions: Map<String, Question> by lazy { loadQuestions() }
    private val decompositionTasks: List<DecompositionTask> by lazy { loadDecompositionTasks() }

    fun lessonById(id: String): Lesson? = allLessons.firstOrNull { it.id == id }
        ?: demoLessons(id.substringAfter('g').substringBefore('-').toIntOrNull() ?: 1).firstOrNull { it.id == id }

    fun questionByVideoId(id: String): Question? = allQuestions[id]
    fun decompositionForDay(index: Int): DecompositionTask? = decompositionTasks.getOrNull(index)

    fun planForGrade(grade: Int): List<DayPlan> {
        val uploadedLessons = allLessons.filter { it.grade == grade }
        val gradeLessons = uploadedLessons.ifEmpty { demoLessons(grade) }
        val groupedLessons = if (uploadedLessons.isNotEmpty()) {
            List(30) { day -> gradeLessons.filterIndexed { index, _ -> index * 30 / gradeLessons.size == day } }
        } else {
            gradeLessons.chunked(3)
        }
        return groupedLessons.mapIndexed { index, lessons ->
            val questions = if (uploadedLessons.isNotEmpty()) {
                lessons.mapNotNull { allQuestions[it.id] }
            } else {
                questionsFor(grade, lessons.firstOrNull()?.topic ?: "思维训练", index)
            }
            DayPlan(index, lessons, questions)
        }
    }

    private fun demoLessons(grade: Int): List<Lesson> {
        val gradeTopics = mapOf(
            1 to listOf("找规律", "图形计数", "趣味加减"),
            2 to listOf("间隔问题", "等量代换", "一笔画"),
            3 to listOf("和差问题", "植树问题", "枚举法"),
            4 to listOf("鸡兔同笼", "平均数", "图形面积"),
            5 to listOf("相遇追及", "容斥原理", "工程问题"),
            6 to listOf("比例模型", "排列组合", "综合推理")
        )
        val topics = gradeTopics[grade] ?: gradeTopics.getValue(1)
        val stages = listOf("认识方法", "例题拆解", "举一反三")
        return List(90) { index ->
            val topic = topics[(index / 3) % topics.size]
            Lesson(
                id = "g$grade-demo-${index + 1}", grade = grade,
                title = "$topic · ${stages[index % 3]}", topic = topic,
                durationMinutes = 5 + index % 4, videoUrl = ""
            )
        }
    }

    private fun loadLessons(): List<Lesson> {
        val source = context.assets.open("curriculum.json").bufferedReader().use { it.readText() }
        val array = JSONArray(source)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    Lesson(
                        id = item.getString("id"),
                        grade = item.getInt("grade"),
                        title = item.getString("title"),
                        topic = item.getString("topic"),
                        durationMinutes = item.optInt("durationMinutes", 6),
                        videoUrl = item.optString("videoUrl")
                    )
                )
            }
        }
    }

    private fun loadQuestions(): Map<String, Question> {
        val source = context.assets.open("questions.json").bufferedReader().use { it.readText() }
        val array = JSONArray(source)
        return buildMap {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val options = item.getJSONArray("opts")
                val videoId = item.getString("videoId")
                put(
                    videoId,
                    Question(
                        title = item.getString("q"),
                        options = List(options.length()) { options.getString(it) },
                        answerIndex = item.getInt("answer"),
                        explanation = item.getString("explain"),
                        videoId = videoId,
                        sourceTitle = item.getString("sourceTitle")
                    )
                )
            }
        }
    }

    private fun loadDecompositionTasks(): List<DecompositionTask> {
        val source = context.assets.open("decomposition.json").bufferedReader().use { it.readText() }
        val array = JSONArray(source)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            DecompositionTask(
                day = item.getInt("day"),
                stageTitle = item.getString("stageTitle"),
                focus = item.getString("focus"),
                problem = item.getString("problem"),
                parentPrompt = item.getString("parentPrompt")
            )
        }
    }

    private fun questionsFor(grade: Int, topic: String, day: Int): List<Question> {
        val base = grade * 2 + day + 3
        return listOf(
            Question(
                "学习“$topic”后，观察 $base、${base + 2}、${base + 4}，下一个数是？",
                listOf("${base + 5}", "${base + 6}", "${base + 7}"), 1,
                "相邻两个数都增加 2。"
            ),
            Question(
                "有 $base 颗星，又点亮了 ${grade + 2} 颗，现在一共有多少颗？",
                listOf("${base + grade + 1}", "${base + grade + 2}", "${base * 2}"), 1,
                "把原来的星星和新点亮的星星相加。"
            ),
            Question(
                "遇到一道暂时不会的奥数题，哪种方法更好？",
                listOf("立刻放弃", "只猜一个答案", "画图并从简单情况试起"), 2,
                "画图和尝试简单情况，能帮助我们发现规律。"
            )
        )
    }
}
