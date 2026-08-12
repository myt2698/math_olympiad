package com.mathplanet.app

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MainActivity : android.app.Activity() {
    private lateinit var store: ProgressStore
    private lateinit var repository: CourseRepository
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.CREAM
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        store = ProgressStore(this)
        repository = CourseRepository(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized && store.userPlan() != null) render()
    }

    private fun render() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(18), dp(20), dp(32))
            setBackgroundColor(Ui.CREAM)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        val plan = store.userPlan()
        if (plan == null) renderSetup() else renderDashboard(plan)
    }

    private fun renderSetup() {
        root.addView(Ui.text(this, "✦  思维星球", 19f, Ui.PURPLE, true).apply { margin(bottom = 42) })
        root.addView(Ui.text(this, "把奥数，变成每天\n20 分钟的小期待", 33f, Ui.INK, true))
        root.addView(Ui.text(this, "每天 2–3 个短视频，再做 3 道小题。\n轻松坚持，不给孩子增加负担。", 14f, Ui.MUTED).apply { margin(top = 15, bottom = 30) })

        val card = verticalCard()
        card.addView(Ui.text(this, "家长设置 · 只需一次", 11f, Ui.PURPLE, true))
        card.addView(Ui.text(this, "制定学习计划", 25f, Ui.INK, true).apply { margin(top = 8, bottom = 22) })

        card.addView(label("孩子的名字"))
        val nameInput = EditText(this).apply {
            hint = "例如：小宇"
            singleLine = true
            background = Ui.rounded(Color.WHITE, 12, this@MainActivity, 0xFFE3E1EB.toInt())
            setPadding(dp(14), 0, dp(14), 0)
        }
        card.addView(nameInput, LayoutParams(LayoutParams.MATCH_PARENT, dp(52)).apply { bottomMargin = dp(16) })

        card.addView(label("目前年级"))
        val gradeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("一年级", "二年级", "三年级", "四年级", "五年级", "六年级"))
            background = Ui.rounded(Color.WHITE, 12, this@MainActivity, 0xFFE3E1EB.toInt())
        }
        card.addView(gradeSpinner, LayoutParams(LayoutParams.MATCH_PARENT, dp(52)).apply { bottomMargin = dp(16) })

        card.addView(label("计划开始日期"))
        var selectedDate = LocalDate.now()
        val dateButton = Button(this).apply {
            text = selectedDate.toString()
            isAllCaps = false
            background = Ui.rounded(Color.WHITE, 12, this@MainActivity, 0xFFE3E1EB.toInt())
            setOnClickListener {
                DatePickerDialog(this@MainActivity, { _, y, m, d ->
                    selectedDate = LocalDate.of(y, m + 1, d)
                    text = selectedDate.toString()
                }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).apply {
                    datePicker.minDate = System.currentTimeMillis() - 1000
                }.show()
            }
        }
        card.addView(dateButton, LayoutParams(LayoutParams.MATCH_PARENT, dp(52)))
        card.addView(Ui.text(this, "🔒  开始后年级和日期将锁定，保证学习记录连续。", 11f, Ui.MUTED).apply {
            background = Ui.rounded(0xFFF3F2FB.toInt(), 12, this@MainActivity)
            setPadding(dp(12), dp(12), dp(12), dp(12)); margin(top = 18, bottom = 20)
        })

        val submit = primaryButton("开始思维之旅  →")
        submit.setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isBlank()) {
                Toast.makeText(this, "请先填写孩子的名字", Toast.LENGTH_SHORT).show()
            } else {
                store.createPlan(name, gradeSpinner.selectedItemPosition + 1, selectedDate)
                render()
            }
        }
        card.addView(submit, LayoutParams(LayoutParams.MATCH_PARENT, dp(54)))
        root.addView(card)
    }

    private fun renderDashboard(user: UserPlan) {
        val start = LocalDate.parse(user.startDate)
        val rawDay = ChronoUnit.DAYS.between(start, LocalDate.now()).toInt()
        val plans = repository.planForGrade(user.grade)
        val activeIndex = rawDay.coerceIn(0, (plans.size - 1).coerceAtLeast(0))
        val active = plans.getOrNull(activeIndex)

        root.addView(planBoard(user, plans, rawDay))
        root.addView(starWallet(user, plans))

        if (active == null) {
            root.addView(Ui.text(this, "还没有导入该年级的课程。", 14f, Ui.MUTED))
            return
        }

        active.lessons.forEachIndexed { index, lesson -> root.addView(lessonCard(lesson, index, rawDay in plans.indices)) }
    }

    private fun starWallet(user: UserPlan, plans: List<DayPlan>): View {
        val start = LocalDate.parse(user.startDate)
        var total = 0
        plans.forEach { day ->
            val date = start.plusDays(day.index.toLong())
            val videos = day.lessons.count { store.isLessonComplete(it.id) }
            val pairs = minOf(videos, store.answered(date))
            total += when { pairs >= day.lessons.size -> 5; pairs >= 2 -> 3; pairs >= 1 -> 1; else -> 0 }
            if (store.perfect(date)) total++
        }
        total += listOf(5, 10, 20, 25, plans.size).distinct().count { store.milestoneClaimed(it) } * 5
        return Ui.text(this, "★  我的思维星星：$total 颗", 15f, 0xFF9A6B08.toInt(), true).apply {
            background = Ui.rounded(0xFFFFF7DC.toInt(), 14, this@MainActivity, 0xFFF2D786.toInt())
            setPadding(dp(15), dp(12), dp(15), dp(12)); margin(top = 12, bottom = 14)
        }
    }

    private fun planBoard(user: UserPlan, plans: List<DayPlan>, rawDay: Int): View {
        val start = LocalDate.parse(user.startDate)
        val card = verticalCard().apply {
            margin(top = 22)
            background = Ui.rounded(Color.WHITE, 22, this@MainActivity, 0xFFE3E1F1.toInt())
        }
        val dates = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(2), dp(3), dp(2), dp(5)) }
        plans.forEach { day ->
            val date = start.plusDays(day.index.toLong())
            val done = store.dayScore(date) != null
            val today = day.index == rawDay
            val past = day.index < rawDay && !done
            val background = when { done -> 0xFFF0FBF5.toInt(); today -> Ui.PURPLE; past -> 0xFFFFF7E7.toInt(); else -> 0xFFF5F4F8.toInt() }
            val color = when { done -> Ui.GREEN; today -> Color.WHITE; past -> 0xFFAA6F0C.toInt(); else -> 0xFF9997A4.toInt() }
            val status = when { done -> "✓"; today -> "今日"; past -> "待补"; else -> "🔒" }
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                this.background = Ui.rounded(background, 12, this@MainActivity)
                addView(Ui.text(this@MainActivity, weekday(date), 9f, color).apply { gravity = Gravity.CENTER })
                addView(Ui.text(this@MainActivity, date.dayOfMonth.toString(), 19f, color, true).apply { gravity = Gravity.CENTER })
                addView(Ui.text(this@MainActivity, "第${day.index + 1}天 · $status", 8f, color).apply { gravity = Gravity.CENTER })
            }
            dates.addView(cell, LayoutParams(dp(68), dp(88)).apply { marginEnd = dp(7) })
        }
        card.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(dates) })
        return card
    }

    private fun gradeNames(grade: Int) = listOf("", "一年级", "二年级", "三年级", "四年级", "五年级", "六年级").getOrElse(grade) { "一年级" }
    private fun shortDate(date: LocalDate) = date.format(DateTimeFormatter.ofPattern("M月d日"))
    private fun weekday(date: LocalDate) = listOf("一", "二", "三", "四", "五", "六", "日")[date.dayOfWeek.value - 1]

    private fun lessonCard(lesson: Lesson, index: Int, enabled: Boolean): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = Ui.rounded(if (store.isLessonComplete(lesson.id)) 0xFFF5FFF9.toInt() else Color.WHITE, 16, this@MainActivity, 0xFFE5E3EB.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14)); margin(bottom = 10)
        }
        val number = Ui.text(this, if (store.isLessonComplete(lesson.id)) "✓" else "%02d".format(index + 1), 17f,
            if (store.isLessonComplete(lesson.id)) Ui.GREEN else Ui.PURPLE, true).apply { gravity = Gravity.CENTER }
        row.addView(number, LayoutParams(dp(42), dp(42)))
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        copy.addView(Ui.text(this, lesson.title, 14f, Ui.INK, true))
        copy.addView(Ui.text(this, "▶ ${lesson.durationMinutes} 分钟 · ${lesson.topic}", 10f, Ui.MUTED).apply { margin(top = 6) })
        val completed = store.isLessonComplete(lesson.id)
        val status = when {
            store.isQuestionAnswered(lesson.id) -> if (store.isQuestionCorrect(lesson.id)) "✓ 视频完成 · 题目答对" else "✓ 视频完成 · 继续努力"
            completed -> "✓ 视频完成 · 题目待答"
            else -> "○ 未完成"
        }
        copy.addView(Ui.text(this, status, 10f, if (completed) Ui.GREEN else Ui.MUTED, true).apply { margin(top = 7) })
        row.addView(copy, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Ui.text(this, if (store.isLessonComplete(lesson.id)) "再看" else "去学习 →", 12f, Ui.PURPLE, true))
        row.alpha = if (enabled) 1f else .55f
        if (enabled) row.setOnClickListener { startActivity(Intent(this, LessonActivity::class.java).putExtra("lesson_id", lesson.id)) }
        return row
    }

    private fun verticalCard(color: Int = Color.WHITE) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.rounded(color, 22, this@MainActivity)
        setPadding(dp(22), dp(24), dp(22), dp(24))
    }

    private fun label(value: String) = Ui.text(this, value, 12f, Ui.INK, true).apply { margin(bottom = 8) }
    private fun primaryButton(value: String) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 14f; setTextColor(Color.WHITE)
        background = Ui.rounded(Ui.PURPLE, 13, this@MainActivity)
    }
    private fun dp(value: Int) = with(Ui) { this@MainActivity.dp(value) }
}
