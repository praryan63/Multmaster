package com.multmaster.app.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.multmaster.app.AppContainer
import com.multmaster.app.domain.*
import com.multmaster.app.util.app
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class Feedback(val correct: Boolean, val given: Int, val answer: Int, val explanation: String)

data class PracticeUiState(
    val started: Boolean = false,
    val question: Question? = null,
    val index: Int = 0,
    val total: Int = 0,
    val input: String = "",
    val feedback: Feedback? = null,
    val finished: Boolean = false,
    val paused: Boolean = false,
    val remaining: Int? = null,
    val results: List<ResultItem> = emptyList(),
    val config: PracticeConfig = PracticeConfig(PracticeMode.MIXED),
    val fastestMs: Long = 0
)

class PracticeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(PracticeUiState())
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    private var queue: List<Question> = emptyList()
    private var questionStart = 0L
    private var sessionStart = 0L
    private var sessionId = 0L
    private var timerJob: Job? = null
    private var recorded = false

    fun start(config: PracticeConfig) {
        if (sessionId != 0L) return
        sessionId = System.currentTimeMillis()
        sessionStart = sessionId
        viewModelScope.launch {
            val pool = if (config.mode == PracticeMode.MISTAKE) {
                val now = System.currentTimeMillis()
                container.mistakeRepo.activeList().filter { it.dueAt <= now }.map { it.a to it.b }
            } else emptyList()
            val questions = QuestionGenerator.build(config, pool)
            if (questions.isEmpty()) {
                _state.value = PracticeUiState(started = true, finished = true, config = config)
                return@launch
            }
            queue = questions
            questionStart = System.currentTimeMillis()
            _state.value = PracticeUiState(
                started = true, question = questions.first(), index = 1,
                total = questions.size, config = config,
                remaining = config.timerSeconds
            )
            if (config.timerSeconds != null) startTimer(config.timerSeconds)
        }
    }

    private fun startTimer(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                if (_state.value.paused || _state.value.finished) continue
                remaining -= 1
                _state.update { it.copy(remaining = remaining) }
            }
            finishSession()
        }
    }

    fun onDigit(d: String) {
        val s = _state.value
        if (s.feedback != null || s.finished) return
        if (s.input.length < 5) _state.update { it.copy(input = it.input + d) }
    }

    fun onBackspace() {
        val s = _state.value
        if (s.feedback != null) return
        _state.update { it.copy(input = it.input.dropLast(1)) }
    }

    fun onClear() = _state.update { it.copy(input = "") }

    fun submit() {
        val s = _state.value
        val q = s.question ?: return
        if (s.feedback != null || s.finished) return
        val given = s.input.toIntOrNull() ?: return
        val ms = (System.currentTimeMillis() - questionStart).coerceAtLeast(1)
        val isCorrect = given == q.answer
        val item = ResultItem(q, given, isCorrect, ms)
        val results = s.results + item
        record(q, given, isCorrect, ms, s.config.mode)
        val fastest = if (isCorrect && (s.fastestMs == 0L || ms < s.fastestMs)) ms else s.fastestMs
        if (s.config.instantFeedback) {
            _state.update {
                it.copy(
                    feedback = Feedback(isCorrect, given, q.answer, q.explanation),
                    results = results, fastestMs = fastest
                )
            }
        } else {
            advance(results, fastest)
        }
    }

    fun skip() {
        val s = _state.value
        val q = s.question ?: return
        if (s.finished) return
        val ms = (System.currentTimeMillis() - questionStart).coerceAtLeast(1)
        val results = s.results + ResultItem(q, null, false, ms)
        record(q, null, false, ms, s.config.mode)
        advance(results, s.fastestMs)
    }

    fun next() {
        val s = _state.value
        if (s.feedback == null) return
        advance(s.results, s.fastestMs)
    }

    fun togglePause() = _state.update { it.copy(paused = !it.paused) }

    fun finishSession() {
        if (recorded) return
        recorded = true
        timerJob?.cancel()
        val s = _state.value
        _state.update { it.copy(finished = true, feedback = null) }
        viewModelScope.launch {
            container.progressRepo.finishSession(sessionId, s.config.mode, sessionStart, s.results)
        }
    }

    private fun advance(results: List<ResultItem>, fastest: Long) {
        val s = _state.value
        if (s.index >= s.total) {
            _state.update { it.copy(results = results, fastestMs = fastest) }
            finishSession()
            return
        }
        val nextQuestion = queue[s.index]
        questionStart = System.currentTimeMillis()
        _state.update {
            it.copy(
                question = nextQuestion, index = s.index + 1, input = "",
                feedback = null, results = results, fastestMs = fastest
            )
        }
    }

    private fun record(q: Question, given: Int?, correct: Boolean, ms: Long, mode: PracticeMode) {
        viewModelScope.launch {
            container.progressRepo.recordAttempt(q, given, ms, mode, sessionId)
            if (correct) {
                if (mode == PracticeMode.MISTAKE) container.mistakeRepo.onCorrectRevision(q)
            } else if (given != null) {
                container.mistakeRepo.onWrong(q, given)
            } else {
                container.mistakeRepo.onWrong(q, -1)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { PracticeViewModel(app().container) }
        }
    }
}