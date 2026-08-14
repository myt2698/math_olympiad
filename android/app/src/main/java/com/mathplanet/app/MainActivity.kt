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
    private var selectedPlanDay: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.PURPLE
        window.decorView.systemUiVisibility = 0
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
        root.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            scaleType = ImageView.ScaleType.CENTER_CROP
        }, LayoutParams(dp(72), dp(72)).apply { bottomMargin = dp(22) })
        root.addView(Ui.text(this, "40 DAYS · THINKING QUEST", 10f, Ui.PURPLE, true))
        root.addView(Ui.text(this, "准备好了吗？\n开启你的思维挑战", 33f, Ui.INK, true).apply { margin(top = 8) })
        root.addView(Ui.text(this, "40 天循序学习，每天 2–3 个短视频、对应小题和少量复习。\n轻松坚持，不给孩子增加负担。", 14f, Ui.MUTED).apply { margin(top = 15, bottom = 30) })

        val card = verticalCard()
        card.addView(Ui.text(this, "家长设置 · 只需一次", 11f, Ui.PURPLE, true))
        card.addView(Ui.text(this, "创建挑战档案", 25f, Ui.INK, true).apply { margin(top = 8, bottom = 22) })

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
        card.addView(Ui.text(this, "日期锁定 · 开始后年级和日期不能修改，挑战记录会保持连续。", 11f, 0xFF66520B.toInt(), true).apply {
            background = Ui.rounded(0xFFFFF5BF.toInt(), 12, this@MainActivity, 0xFFE2B834.toInt())
            setPadding(dp(12), dp(12), dp(12), dp(12)); margin(top = 18, bottom = 20)
        })

        val submit = primaryButton("开启 40 天挑战")
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
        val savedDay = selectedPlanDay
        if (savedDay == null || savedDay !in plans.indices) selectedPlanDay = activeIndex
        val displayIndex = selectedPlanDay ?: activeIndex
        val active = plans.getOrNull(displayIndex)

        root.addView(challengeHeader())
        root.addView(starWallet(user, plans))
        root.addView(planBoard(user, plans, rawDay, displayIndex))

        if (active == null) {
            root.addView(Ui.text(this, "还没有导入该年级的课程。", 14f, Ui.MUTED))
            return
        }

        root.addView(Ui.text(this, "NEW CHALLENGE", 9f, 0xFFFF806F.toInt(), true).apply { margin(top = 10) })
        root.addView(Ui.text(this, "新知关卡", 24f, Ui.INK, true).apply { margin(top = 4, bottom = 15) })
        active.lessons.forEachIndexed { index, lesson -> root.addView(lessonCard(lesson, index, true)) }
        root.addView(reviewCard(active.reviews))
        repository.decompositionForDay(active.index)?.let { root.addView(decompositionCard(it)) }
    }

    private fun starWallet(user: UserPlan, plans: List<DayPlan>): View {
        val start = LocalDate.parse(user.startDate)
        var total = 0
        plans.forEach { day ->
            val date = start.plusDays(day.index.toLong())
            val videos = day.lessons.count { store.isLessonComplete(it.id) }
            val pairs = minOf(videos, store.answered(date))
            total += when { pairs >= 3 -> 5; pairs >= 2 -> 3; pairs >= 1 -> 1; else -> 0 }
            if (store.perfect(date)) total++
        }
        total += listOf(5, 10, 20, 25, plans.size).distinct().count { store.milestoneClaimed(it) } * 5
        return Ui.text(this, "我的思维星星  ·  $total 颗", 15f, Ui.NAVY, true).apply {
            background = Ui.rounded(0xFFFFF3CD.toInt(), 14, this@MainActivity, 0xFFEAD590.toInt())
            setPadding(dp(15), dp(12), dp(15), dp(12)); margin(top = 12, bottom = 15)
        }
    }

    private fun challengeHeader(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            background = Ui.rounded(Ui.SKY, 22, this@MainActivity)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            val copy = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
            copy.addView(Ui.text(this@MainActivity, "40 DAYS · THINKING QUEST", 9f, 0xFF26638F.toInt(), true))
            copy.addView(Ui.text(this@MainActivity, "思维挑战地图", 27f, Ui.NAVY, true).apply { margin(top = 5) })
            copy.addView(Ui.text(this@MainActivity, "每完成一次思考，大脑就升级一次。", 10f, 0xFF365F7B.toInt()).apply { margin(top = 5) })
            addView(copy, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.mipmap.ic_launcher)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }, LayoutParams(dp(58), dp(58)))
        }
    }

    private fun planBoard(user: UserPlan, plans: List<DayPlan>, rawDay: Int, selectedDay: Int): View {
        val start = LocalDate.parse(user.startDate)
        val card = verticalCard().apply {
            background = Ui.rounded(Color.WHITE, 22, this@MainActivity, 0xFFD7E7F2.toInt())
        }
        val dates = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(2), dp(3), dp(2), dp(5)) }
        plans.forEach { day ->
            val date = start.plusDays(day.index.toLong())
            val done = store.dayScore(date) != null
            val today = day.index == rawDay
            val past = day.index < rawDay && !done
            val selected = day.index == selectedDay
            val background = when { selected -> 0xFFE8F6EC.toInt(); done -> 0xFFE9FFF5.toInt(); today -> Ui.PURPLE; past -> 0xFFFFF9D9.toInt(); else -> 0xFFF8FCFF.toInt() }
            val color = when { selected -> 0xFF2E6843.toInt(); done -> Ui.GREEN; today -> Color.WHITE; past -> 0xFF8A650C.toInt(); else -> 0xFF718696.toInt() }
            val status = when { done -> "✓"; today -> "今日"; else -> null }
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                this.background = Ui.rounded(background, 12, this@MainActivity, if (selected) Ui.SELECTED_GREEN else 0xFFD7E7F2.toInt())
                addView(Ui.text(this@MainActivity, weekday(date), 9f, color).apply { gravity = Gravity.CENTER })
                addView(Ui.text(this@MainActivity, date.dayOfMonth.toString(), 19f, color, true).apply { gravity = Gravity.CENTER })
                status?.let { addView(Ui.text(this@MainActivity, it, 8f, color).apply { gravity = Gravity.CENTER }) }
                setOnClickListener { selectedPlanDay = day.index; render() }
            }
            dates.addView(cell, LayoutParams(dp(68), dp(76)).apply { marginEnd = dp(7) })
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
            background = Ui.rounded(if (store.isLessonComplete(lesson.id)) 0xFFF0FFF7.toInt() else 0xFFFBFDFF.toInt(), 16, this@MainActivity, if (store.isLessonComplete(lesson.id)) 0xFFB8EAD5.toInt() else 0xFFD7E7F2.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14)); margin(bottom = 10)
        }
        val number = Ui.text(this, if (store.isLessonComplete(lesson.id)) "✓" else "%02d".format(index + 1), 17f,
            if (store.isLessonComplete(lesson.id)) 0xFF154C37.toInt() else Ui.PURPLE, true).apply {
            gravity = Gravity.CENTER
            background = Ui.rounded(if (store.isLessonComplete(lesson.id)) 0xFF61D7A4.toInt() else 0xFFEAF6FF.toInt(), 12, this@MainActivity)
        }
        row.addView(number, LayoutParams(dp(42), dp(42)))
        val copy = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        copy.addView(Ui.text(this, lesson.title, 14f, Ui.INK, true))
        row.addView(copy, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        row.addView(Ui.text(this, if (store.isLessonComplete(lesson.id)) "再挑战" else "开始挑战", 11f, if (store.isLessonComplete(lesson.id)) 0xFF154C37.toInt() else Color.WHITE, true).apply {
            background = Ui.rounded(if (store.isLessonComplete(lesson.id)) 0xFF61D7A4.toInt() else Ui.PURPLE, 10, this@MainActivity)
            setPadding(dp(10), dp(9), dp(10), dp(9))
        })
        row.alpha = if (enabled) 1f else .55f
        if (enabled) row.setOnClickListener { startActivity(Intent(this, LessonActivity::class.java).putExtra("lesson_id", lesson.id)) }
        return row
    }

    private fun reviewCard(reviews: List<ReviewItem>): View {
        return verticalCard(0xFFEAFcFD.toInt()).apply {
            margin(top = 6, bottom = 10)
            background = Ui.rounded(0xFFEAFcFD.toInt(), 19, this@MainActivity, 0xFF18777D.toInt())
            addView(Ui.text(this@MainActivity, "复习站 · 记忆能量补给", 15f, 0xFF175C61.toInt(), true))
            addView(Ui.text(this@MainActivity, "先在脑中回想，记不清时再打开视频", 10f, 0xFF4B7B7F.toInt()).apply { margin(top = 5) })
            if (reviews.isEmpty()) {
                addView(Ui.text(this@MainActivity, "第一天先收集新知识，明天开启第一次记忆补给。", 11f, 0xFF4B7B7F.toInt()).apply { margin(top = 13) })
            } else {
                reviews.forEach { review ->
                    addView(LinearLayout(this@MainActivity).apply {
                        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                        background = Ui.rounded(Color.WHITE, 13, this@MainActivity, 0xFF9EDCE0.toInt())
                        setPadding(dp(12), dp(11), dp(12), dp(11)); margin(top = 8)
                        addView(Ui.text(this@MainActivity, review.label, 9f, 0xFF18777D.toInt(), true))
                        addView(Ui.text(this@MainActivity, review.lesson.title, 11f, Ui.INK, true).apply { setPadding(dp(10), 0, dp(8), 0) }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
                        addView(Ui.text(this@MainActivity, "回顾一下", 10f, 0xFF18777D.toInt(), true))
                        setOnClickListener { startActivity(Intent(this@MainActivity, LessonActivity::class.java).putExtra("lesson_id", review.lesson.id)) }
                    })
                }
            }
        }
    }

    private fun decompositionCard(task: DecompositionTask): View {
        return verticalCard(0xFFFFF7D8.toInt()).apply {
            margin(top = 6)
            background = Ui.rounded(0xFFFFF7D8.toInt(), 19, this@MainActivity, 0xFF9D5F12.toInt())
            addView(Ui.text(this@MainActivity, "THINKING BOSS", 9f, 0xFFBE6D12.toInt(), true))
            addView(Ui.text(this@MainActivity, "思维挑战关", 19f, 0xFF6B3F0D.toInt(), true).apply { margin(top = 4) })
            addView(Ui.text(this@MainActivity, "第 ${task.day} 天 · ${task.stageTitle}", 10f, 0xFF805313.toInt(), true).apply { margin(top = 5) })
            task.exercises.forEach { exercise ->
                addView(LinearLayout(this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    background = Ui.rounded(0xFFFFFDF7.toInt(), 15, this@MainActivity, 0xFFE3BB69.toInt())
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                    margin(top = 12)
                    addView(Ui.text(this@MainActivity, exercise.label, 10f, 0xFF8B5B08.toInt(), true))
                    addView(Ui.text(this@MainActivity, "挑战能力：${exercise.focus}", 11f, 0xFFA06B12.toInt(), true).apply { margin(top = 10, bottom = 8) })
                    addView(Ui.text(this@MainActivity, exercise.problem, 17f, Ui.INK, true))
                    addView(Ui.text(this@MainActivity, "陪练提示：${exercise.parentPrompt}", 12f, 0xFF756444.toInt()).apply { margin(top = 14) })
                })
            }
            addView(Ui.text(this@MainActivity, "这一关只需要说出思路，不列式、不计算", 10f, 0xFF8F6D35.toInt()).apply { margin(top = 12) })
        }
    }

    private fun verticalCard(color: Int = Color.WHITE) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = Ui.rounded(color, 22, this@MainActivity)
        setPadding(dp(22), dp(24), dp(22), dp(24))
    }

    private fun label(value: String) = Ui.text(this, value, 12f, Ui.INK, true).apply { margin(bottom = 8) }
    private fun primaryButton(value: String) = Button(this).apply {
        text = value; isAllCaps = false; textSize = 14f; setTextColor(Ui.NAVY)
        background = Ui.rounded(Ui.YELLOW, 13, this@MainActivity, Ui.NAVY)
    }
    private fun dp(value: Int) = with(Ui) { this@MainActivity.dp(value) }
}
