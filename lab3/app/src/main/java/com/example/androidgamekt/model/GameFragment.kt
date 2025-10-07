package com.example.androidgamekt.model

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.example.androidgamekt.R
import kotlin.math.max
import kotlin.random.Random

class GameFragment : Fragment() {

    private lateinit var fieldView: FrameLayout
    private lateinit var scoreView: TextView
    private lateinit var missesView: TextView
    private lateinit var statusView: TextView
    private lateinit var restartButton: Button

    private val spawnHandler = Handler(Looper.getMainLooper())
    private val spawnRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            spawnBug()
            spawnHandler.postDelayed(this, SPAWN_DELAY)
        }
    }

    private var isRunning = false
    private var score = 0
    private var misses = 0
    private var fieldWidth = 0
    private var fieldHeight = 0

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
    }

    override fun onResume() {
        super.onResume()
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
        isRunning = true
        spawnHandler.post(spawnRunnable)
        statusView.text = getString(R.string.status_running)
    }

    private fun stopGame() {
        isRunning = false
        spawnHandler.removeCallbacks(spawnRunnable)
    }

    private fun resetGame() {
        score = 0
        misses = 0
        fieldView.removeAllViews()
        updateScoreboard()
        statusView.text = getString(R.string.status_restart)
        if (!isRunning) {
            startGame()
        }
    }

    private fun spawnBug() {
        if (fieldWidth == 0 || fieldHeight == 0) return

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
        score += MISS_PENALTY
        misses += 1
        statusView.text = getString(R.string.status_miss)
        updateScoreboard()
    }

    private fun updateScoreboard() {
        scoreView.text = getString(R.string.score_template, score)
        missesView.text = getString(R.string.misses_template, misses)
    }

    private fun pickBugType(): BugType {
        val roll = Random.nextInt(100)
        return when {
            roll < 10 -> BugType.BONUS
            roll < 25 -> BugType.POISON
            else -> BugType.NORMAL
        }
    }

    private enum class BugType { NORMAL, BONUS, POISON }

    companion object {
        private const val SPAWN_DELAY = 1200L
        private const val NORMAL_DURATION = 3500L
        private const val BONUS_DURATION = 2500L
        private const val POISON_DURATION = 3200L
        private const val NORMAL_SCORE = 10
        private const val BONUS_SCORE = 50
        private const val POISON_PENALTY = -20
        private const val MISS_PENALTY = -5

        private val NORMAL_BUGS = listOf(
            R.drawable.bug_green,
            R.drawable.bug_blue,
            R.drawable.bug_orange
        )
    }
}
