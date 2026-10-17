package com.example.aprendemoslavida.data.model

enum class AnswerColor {
    DEFAULT, CORRECT, WRONG
}
data class AnswerVisualState(
    val selectedIndex: Int,
    val correctIndex: Int,
    val selectedColor: AnswerColor,
    val correctColor: AnswerColor
)

object AnswerFeedback {
    fun resolve(answer: GameAnswer, question: PresentedQuestion, selectedIndex: Int): AnswerVisualState {
        val correctIndex = answer.correctOptionId?.let { id ->
            question.options.indexOfFirst { it.id == id }.takeIf { it >= 0 } ?: -1
        } ?: -1
        return if (answer.correct) {
            AnswerVisualState(selectedIndex, selectedIndex, AnswerColor.CORRECT, AnswerColor.CORRECT)
        } else {
            AnswerVisualState(selectedIndex, correctIndex, AnswerColor.WRONG, AnswerColor.CORRECT)
        }
    }
}
