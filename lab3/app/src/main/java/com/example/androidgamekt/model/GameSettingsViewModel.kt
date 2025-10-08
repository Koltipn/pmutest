package com.example.androidgamekt.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class GameSettings(
    val gameSpeed: Int,
    val bugSpeed: Int,
    val maxBugs: Int,
    val bonusIntervalSeconds: Int,
    val roundDurationSeconds: Int
)

class GameSettingsViewModel : ViewModel() {

    private val _settings = MutableLiveData(
        GameSettings(
            gameSpeed = DEFAULT_GAME_SPEED,
            bugSpeed = DEFAULT_BUG_SPEED,
            maxBugs = DEFAULT_MAX_BUGS,
            bonusIntervalSeconds = DEFAULT_BONUS_INTERVAL_SECONDS,
            roundDurationSeconds = DEFAULT_ROUND_DURATION_SECONDS
        )
    )
    val settings: LiveData<GameSettings> = _settings

    fun setGameSpeed(speed: Int) {
        updateSettings { current ->
            current.copy(gameSpeed = speed.coerceAtLeast(MIN_GAME_SPEED))
        }
    }

    fun setBugSpeed(speed: Int) {
        updateSettings { current ->
            current.copy(bugSpeed = speed.coerceAtLeast(MIN_BUG_SPEED))
        }
    }

    fun setMaxBugs(maxBugs: Int) {
        updateSettings { current ->
            current.copy(maxBugs = maxBugs.coerceAtLeast(MIN_MAX_BUGS))
        }
    }

    fun setBonusInterval(seconds: Int) {
        updateSettings { current ->
            current.copy(bonusIntervalSeconds = seconds.coerceAtLeast(0))
        }
    }

    fun setRoundDuration(seconds: Int) {
        updateSettings { current ->
            current.copy(roundDurationSeconds = seconds.coerceAtLeast(MIN_ROUND_DURATION_SECONDS))
        }
    }

    private fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val current = _settings.value ?: DEFAULT_SETTINGS
        _settings.value = transform(current)
    }

    companion object {
        private const val MIN_GAME_SPEED = 1
        private const val MIN_BUG_SPEED = 1
        private const val MIN_MAX_BUGS = 1
        private const val MIN_ROUND_DURATION_SECONDS = 5
        private const val DEFAULT_GAME_SPEED = 5
        private const val DEFAULT_BUG_SPEED = 5
        private const val DEFAULT_MAX_BUGS = 15
        private const val DEFAULT_BONUS_INTERVAL_SECONDS = 10
        private const val DEFAULT_ROUND_DURATION_SECONDS = 60

        private val DEFAULT_SETTINGS = GameSettings(
            gameSpeed = DEFAULT_GAME_SPEED,
            bugSpeed = DEFAULT_BUG_SPEED,
            maxBugs = DEFAULT_MAX_BUGS,
            bonusIntervalSeconds = DEFAULT_BONUS_INTERVAL_SECONDS,
            roundDurationSeconds = DEFAULT_ROUND_DURATION_SECONDS
        )
    }
}
