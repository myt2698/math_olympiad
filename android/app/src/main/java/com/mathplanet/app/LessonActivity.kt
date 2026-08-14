package com.mathplanet.app

import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.File
import java.time.LocalDate

class LessonActivity : android.app.Activity() {
    private lateinit var store: ProgressStore
    private var player: MediaPlayer? = null
    private val progressHandler = Handler(Looper.getMainLooper())
    private var playbackUnlocked = false
    private var progressCheck: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Ui.PURPLE
        window.decorView.systemUiVisibility = 0
        store = ProgressStore(this)
        val lessonId = intent.getStringExtra("lesson_id") ?: return finish()
        val repository = CourseRepository(this)
        val lesson = repository.lessonById(lessonId) ?: return finish()
        val question = repository.questionByVideoId(lessonId)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(26))
            setBackgroundColor(Ui.CREAM)
        }
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)

        val back = Ui.text(this, "返回挑战地图", 13f, Ui.PURPLE, true).apply { setPadding(0, dp(8), 0, dp(18)); setOnClickListener { finish() } }
        root.addView(back)
        root.addView(Ui.text(this, "${lesson.grade} 年级 · 思维训练", 11f, Ui.PURPLE, true))
        root.addView(Ui.text(this, lesson.title, 28f, Ui.INK, true).apply { margin(top = 8, bottom = 10) })
        root.addView(Ui.text(this, "通过视频认识“${lesson.topic}”，学会观察、尝试和验证。", 13f, Ui.MUTED).apply { margin(bottom = 20) })

        val video = VideoView(this).apply {
            setBackgroundColor(0xFF34355D.toInt())
            setMediaController(MediaController(this@LessonActivity).also { it.setAnchorView(this) })
        }
        root.addView(video, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(230)))

        val status = Ui.text(this, "准备播放 · ${lesson.durationMinutes} 分钟", 12f, Ui.MUTED).apply { gravity = Gravity.CENTER; margin(top = 14, bottom = 18) }
        root.addView(status)

        val questionBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Ui.rounded(Color.WHITE, 17, this@LessonActivity, 0xFFD7E7F2.toInt())
            setPadding(dp(18), dp(18), dp(18), dp(18)); margin(top = 4)
        }
        root.addView(questionBox, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        fun updateDayProgress() {
            val user = store.userPlan() ?: return
            val plans = repository.planForGrade(user.grade)
            val day = plans.indexOfFirst { plan -> plan.lessons.any { it.id == lesson.id } }
            if (day < 0) return
            val plan = plans[day]
            val date = LocalDate.parse(user.startDate).plusDays(day.toLong())
            val answered = plan.lessons.count { store.isQuestionAnswered(it.id) }
            val correctCount = plan.lessons.count { store.isQuestionCorrect(it.id) }
            val total = minOf(plan.lessons.size, plan.questions.size)
            val perfect = answered >= total && correctCount == total && plan.lessons.none { store.hadQuestionError(it.id) }
            store.saveDayReward(date, answered, correctCount, perfect)
            if (answered >= total && plan.lessons.all { store.isLessonComplete(it.id) }) store.markDayComplete(date, correctCount, total)
            var completedDays = 0
            for (item in plans) {
                if (store.dayScore(LocalDate.parse(user.startDate).plusDays(item.index.toLong())) == null) break
                completedDays++
            }
            listOf(5, 10, 20, 25, plans.size).distinct().filter { it <= completedDays }.forEach(store::claimMilestone)
        }

        fun renderQuestion() {
            questionBox.removeAllViews()
            if (question == null) {
                questionBox.addView(Ui.text(this, "这节视频的题目正在准备中。", 13f, Ui.MUTED, true))
                return
            }
            if (!store.isLessonComplete(lesson.id)) {
                questionBox.addView(Ui.text(this, "🔒 看完视频后，这道题会在这里解锁", 13f, Ui.MUTED, true).apply { gravity = Gravity.CENTER })
                return
            }
            questionBox.addView(Ui.text(this, "视频后的思维挑战", 11f, 0xFFFF806F.toInt(), true))
            questionBox.addView(Ui.text(this, question.title, 18f, Ui.INK, true).apply { margin(top = 9, bottom = 13) })
            val alreadyCorrect = store.isQuestionCorrect(lesson.id)
            question.options.forEachIndexed { index, option ->
                val button = Button(this).apply {
                    text = "${'A' + index}.  $option"; isAllCaps = false; gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setTextColor(Ui.INK); background = Ui.rounded(0xFFF4FAFF.toInt(), 13, this@LessonActivity, Ui.PURPLE)
                    setOnClickListener {
                        val correct = index == question.answerIndex
                        store.saveQuestionAnswer(lesson.id, correct)
                        updateDayProgress()
                        renderQuestion()
                        Toast.makeText(this@LessonActivity, if (correct) "答对啦！认真思考的你真棒 🌟" else "别灰心，看看解析再试一次 💪", Toast.LENGTH_SHORT).show()
                    }
                    isEnabled = !alreadyCorrect
                    alpha = if (isEnabled) 1f else .62f
                }
                questionBox.addView(button, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52)).apply { bottomMargin = dp(8) })
            }
            if (store.isQuestionAnswered(lesson.id)) {
                val correct = store.isQuestionCorrect(lesson.id)
                questionBox.addView(Ui.text(this, (if (correct) "🌟 答对啦！认真思考的你真棒！\n" else "💪 这次还没答对，再试一次吧！\n") + question.explanation,
                    12f, if (correct) Ui.GREEN else 0xFFA86D16.toInt(), true).apply { margin(top = 6) })
            }
        }
        renderQuestion()

        val uri = resolveVideo(lesson.videoUrl)
        playbackUnlocked = store.isLessonComplete(lesson.id)
        if (uri == null) {
            status.text = "视频尚未导入。请在 curriculum.json 中填写地址。"
        } else {
            video.setVideoURI(uri)
            video.setOnPreparedListener { media ->
                player = media
                status.text = "视频已准备好，点击画面开始播放"
                progressCheck = object : Runnable {
                    override fun run() {
                        val duration = media.duration
                        val position = media.currentPosition
                        if (!playbackUnlocked && duration > 0 && (position.toFloat() / duration >= .95f || duration - position <= 2_000)) {
                            playbackUnlocked = true
                            store.markLessonComplete(lesson.id)
                            status.text = "视频播放完成，下面的题目已解锁"
                            renderQuestion()
                        }
                        if (!playbackUnlocked) progressHandler.postDelayed(this, 500)
                    }
                }.also { progressHandler.post(it) }
            }
            video.setOnCompletionListener {
                playbackUnlocked = true
                store.markLessonComplete(lesson.id)
                status.text = "视频播放完成，下面的题目已解锁"
                renderQuestion()
            }
            video.setOnErrorListener { _, _, _ ->
                status.text = "视频无法播放，请检查文件或网络地址。"
                true
            }
        }
    }

    private fun resolveVideo(value: String): Uri? {
        if (value.isBlank()) return null
        return when {
            value.startsWith("https://") -> Uri.parse(value)
            value.startsWith("asset://") -> {
                val assetPath = value.removePrefix("asset://")
                val extension = assetPath.substringAfterLast('.', "mp4")
                val cachedVideo = File(cacheDir, "lesson-${assetPath.hashCode()}.$extension")
                if (!cachedVideo.exists()) {
                    cacheDir.listFiles { file -> file.name.startsWith("lesson-") }?.forEach { it.delete() }
                    assets.open(assetPath).use { source ->
                        cachedVideo.outputStream().use { target -> source.copyTo(target) }
                    }
                }
                Uri.fromFile(cachedVideo)
            }
            value.startsWith("file://") -> Uri.parse(value)
            else -> File(value).takeIf { it.exists() }?.let(Uri::fromFile)
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
        progressCheck?.let(progressHandler::removeCallbacks)
    }

    private fun dp(value: Int) = with(Ui) { this@LessonActivity.dp(value) }
}
