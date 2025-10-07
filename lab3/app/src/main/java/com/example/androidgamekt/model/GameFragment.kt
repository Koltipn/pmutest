package com.example.androidgamekt.model

import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.androidgamekt.R
import kotlin.LazyThreadSafetyMode
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import java.util.concurrent.TimeUnit

class GameFragment : Fragment() {

    private lateinit var fieldView: FrameLayout
    private lateinit var scoreView: TextView
    private lateinit var missesView: TextView
    private lateinit var statusView: TextView
    private lateinit var timerView: TextView
    private lateinit var restartButton: Button

    private val settingsViewModel: GameSettingsViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(requireActivity())[GameSettingsViewModel::class.java]
    }

    private val spawnHandler = Handler(Looper.getMainLooper())
    private val spawnRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            spawnBug()
            spawnHandler.postDelayed(this, spawnDelay)
        }
    }

    private var isRunning = false
    private var score = 0
    private var misses = 0
    private var fieldWidth = 0
    private var fieldHeight = 0
    private var spawnDelay = DEFAULT_SPAWN_DELAY
    private var maxBugsOnScreen = DEFAULT_MAX_BUGS
    private var bonusIntervalMillis = DEFAULT_BONUS_INTERVAL
    private var roundDurationSeconds = DEFAULT_ROUND_DURATION
    private var remainingRoundMillis = DEFAULT_ROUND_DURATION * 1000L
    private var roundTimer: CountDownTimer? = null
    private var lastBonusSpawnAt = SystemClock.uptimeMillis()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fieldView = view.findViewById(R.id.gameField)
        scoreView = view.findViewById(R.id.tvScore)
        missesView = view.findViewById(R.id.tvMisses)
        statusView = view.findViewById(R.id.tvStatus)
        timerView = view.findViewById(R.id.tvTimer)
        restartButton = view.findViewById(R.id.btnRestart)

        restartButton.setOnClickListener { resetGame() }

        fieldView.setOnClickListener {
            registerMiss()
        }

        fieldView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                fieldWidth = fieldView.width
                fieldHeight = fieldView.height
                if (fieldWidth > 0 && fieldHeight > 0 && fieldView.viewTreeObserver.isAlive) {
                    fieldView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })

        updateScoreboard()
        updateTimerView()

        settingsViewModel.settings.observe(viewLifecycleOwner) { settings ->
            applySettings(settings)
        }
    }

    override fun onResume() {
        super.onResume()
        settingsViewModel.settings.value?.let { applySettings(it) }
        startGame()
    }

    override fun onPause() {
        super.onPause()
        stopGame()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopGame()
        fieldView.removeAllViews()
    }

    private fun startGame() {
        if (isRunning) return
        if (remainingRoundMillis <= 0L) {
            remainingRoundMillis = roundDurationSeconds * 1000L
        }
        isRunning = true
        lastBonusSpawnAt = SystemClock.uptimeMillis()
        startRoundTimer(remainingRoundMillis)
        spawnHandler.post(spawnRunnable)
        statusView.text = getString(R.string.status_running)
    }

    private fun stopGame() {
        isRunning = false
        spawnHandler.removeCallbacks(spawnRunnable)
        roundTimer?.cancel()
        roundTimer = null
    }

    private fun resetGame() {
        stopGame()
        score = 0
        misses = 0
        fieldView.removeAllViews()
        remainingRoundMillis = roundDurationSeconds * 1000L
        lastBonusSpawnAt = SystemClock.uptimeMillis()
        updateScoreboard()
        updateTimerView()
        statusView.text = getString(R.string.status_restart)
        startGame()
    }

    private fun spawnBug() {
        if (fieldWidth == 0 || fieldHeight == 0) return
        if (fieldView.childCount >= maxBugsOnScreen) return

        val bugType = pickBugType()
        val bugDrawable = when (bugType) {
            BugType.NORMAL -> NORMAL_BUGS.random()
            BugType.BONUS -> R.drawable.bug_bonus
            BugType.POISON -> R.drawable.bug_poison
        }

        val bugSize = resources.getDimensionPixelSize(R.dimen.bug_size)
        val bugView = ImageView(requireContext()).apply {
            layoutParams = FrameLayout.LayoutParams(bugSize, bugSize)
            setImageDrawable(ContextCompat.getDrawable(requireContext(), bugDrawable))
            contentDescription = when (bugType) {
                BugType.NORMAL -> getString(R.string.cd_normal_bug)
                BugType.BONUS -> getString(R.string.cd_bonus_bug)
                BugType.POISON -> getString(R.string.cd_poison_bug)
            }
            isClickable = true
            isFocusable = true
        }

        val maxX = max(1, fieldWidth - bugSize)
        val maxY = max(1, fieldHeight - bugSize)
        val startX = Random.nextInt(maxX).toFloat()
        val startY = Random.nextInt(maxY).toFloat()
        bugView.x = startX
        bugView.y = startY

        bugView.setOnClickListener {
            bugView.animate().cancel()
            fieldView.removeView(bugView)
            when (bugType) {
                BugType.NORMAL -> {
                    score += NORMAL_SCORE
                    statusView.text = getString(R.string.status_bug_down)
                }
                BugType.BONUS -> {
                    score += BONUS_SCORE
                    statusView.text = getString(R.string.status_bonus)
                }
                BugType.POISON -> {
                    score += POISON_PENALTY
                    statusView.text = getString(R.string.status_poison)
                }
            }
            updateScoreboard()
        }

        fieldView.addView(bugView)

        val targetX = Random.nextInt(maxX).toFloat()
        val targetY = Random.nextInt(maxY).toFloat()
        val duration = when (bugType) {
            BugType.BONUS -> BONUS_DURATION
            BugType.POISON -> POISON_DURATION
            BugType.NORMAL -> NORMAL_DURATION
        }

        bugView.animate()
            .x(targetX)
            .y(targetY)
            .setDuration(duration)
            .setInterpolator(LinearInterpolator())
            .withEndAction {
                fieldView.removeView(bugView)
            }
            .start()
    }

    private fun registerMiss() {
        if (!isRunning) return
        score += MISS_PENALTY
        misses += 1
        statusView.text = getString(R.string.status_miss)
        updateScoreboard()
    }

    private fun updateScoreboard() {
        scoreView.text = getString(R.string.score_template, score)
        missesView.text = getString(R.string.misses_template, misses)
    }

    private fun updateTimerView() {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingRoundMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingRoundMillis) % 60
        timerView.text = getString(R.string.timer_template, minutes, seconds)
    }

    private fun startRoundTimer(durationMillis: Long) {
        roundTimer?.cancel()
        remainingRoundMillis = durationMillis
        updateTimerView()
        if (durationMillis <= 0L) {
            finishRound()
            return
        }
        roundTimer = object : CountDownTimer(durationMillis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingRoundMillis = millisUntilFinished
                updateTimerView()
            }

            override fun onFinish() {
                finishRound()
            }
        }.also { it.start() }
    }

    private fun finishRound() {
        remainingRoundMillis = 0L
        updateTimerView()
        statusView.text = getString(R.string.status_round_over)
        fieldView.removeAllViews()
        stopGame()
    }

    private fun applySettings(settings: GameSettings) {
        val newSpawnDelay = computeSpawnDelay(settings.gameSpeed)
        val newMaxBugs = max(settings.maxBugs, 1)
        val newBonusInterval = max(settings.bonusIntervalSeconds, 0) * 1000L
        val newRoundDuration = max(settings.roundDurationSeconds, MIN_ROUND_DURATION)

        val spawnDelayChanged = newSpawnDelay != spawnDelay
        val bonusIntervalChanged = newBonusInterval != bonusIntervalMillis
        val roundDurationChanged = newRoundDuration != roundDurationSeconds

        spawnDelay = newSpawnDelay
        maxBugsOnScreen = newMaxBugs
        bonusIntervalMillis = newBonusInterval

        if (bonusIntervalChanged && bonusIntervalMillis > 0L) {
            lastBonusSpawnAt = SystemClock.uptimeMillis()
        }

        roundDurationSeconds = newRoundDuration
        val newRoundDurationMillis = newRoundDuration * 1000L

        if (isRunning) {
            trimBugsToLimit()
            if (roundDurationChanged) {
                remainingRoundMillis = newRoundDurationMillis
                startRoundTimer(remainingRoundMillis)
            } else {
                remainingRoundMillis = min(remainingRoundMillis, newRoundDurationMillis)
                startRoundTimer(remainingRoundMillis)
            }
            if (spawnDelayChanged) {
                restartSpawnLoop()
            }
        } else {
            remainingRoundMillis = newRoundDurationMillis
            updateTimerView()
        }
    }

    private fun restartSpawnLoop() {
        spawnHandler.removeCallbacks(spawnRunnable)
        spawnHandler.postDelayed(spawnRunnable, spawnDelay)
    }

    private fun trimBugsToLimit() {
        while (fieldView.childCount > maxBugsOnScreen) {
            fieldView.removeViewAt(0)
        }
    }

    private fun pickBugType(): BugType {
        val now = SystemClock.uptimeMillis()
        if (bonusIntervalMillis > 0L && now - lastBonusSpawnAt >= bonusIntervalMillis) {
            lastBonusSpawnAt = now
            return BugType.BONUS
        }
        val roll = Random.nextInt(100)
        val type = when {
            roll < 10 -> BugType.BONUS
            roll < 25 -> BugType.POISON
            else -> BugType.NORMAL
        }
        if (type == BugType.BONUS) {
            lastBonusSpawnAt = now
        }
        return type
    }

    private enum class BugType { NORMAL, BONUS, POISON }

    companion object {
        private const val DEFAULT_SPAWN_DELAY = 1200L
        private const val DEFAULT_MAX_BUGS = 15
        private const val DEFAULT_BONUS_INTERVAL = 10000L
        private const val DEFAULT_ROUND_DURATION = 60
        private const val MIN_ROUND_DURATION = 5
        private const val NORMAL_DURATION = 3500L
        private const val BONUS_DURATION = 2500L
        private const val POISON_DURATION = 3200L
        private const val NORMAL_SCORE = 10
        private const val BONUS_SCORE = 50
        private const val POISON_PENALTY = -20
        private const val MISS_PENALTY = -5
        private const val BASE_SPAWN_DELAY = 2000L
        private const val SPAWN_DELAY_STEP = 150L
        private const val MIN_GAME_SPEED = 1

        private val NORMAL_BUGS = listOf(
            R.drawable.bug_orange
        )

        private fun computeSpawnDelay(gameSpeed: Int): Long {
            val speed = max(gameSpeed, MIN_GAME_SPEED)
            val adjusted = BASE_SPAWN_DELAY - (speed - MIN_GAME_SPEED) * SPAWN_DELAY_STEP
            return max(400L, adjusted)
        }
    }
}
