package ru.pl.kotlindevelopertestcase

// Game.kt
import kotlin.math.floor
import kotlin.random.Random

const val TIMESTAMPS_COUNT = 50000
const val PROBABILITY_SCORE_CHANGED = 0.0001
const val PROBABILITY_HOME_SCORE = 0.45
const val OFFSET_MAX_STEP = 3

data class Score(val home: Int, val away: Int)
data class Stamp(val offset: Int, val score: Score)

fun generateGame(): Array<Stamp> {
    val stamps = Array<Stamp>(TIMESTAMPS_COUNT) { _ -> Stamp(0, Score(0, 0)) }
    var currentStamp = stamps[0]
    for (i in 1 until TIMESTAMPS_COUNT) {
        currentStamp = generateStamp(currentStamp)
        stamps[i] = currentStamp
    }
    return stamps
}

fun generateStamp(previousValue: Stamp): Stamp {
    val scoreChanged = Random.nextFloat() > 1 - PROBABILITY_SCORE_CHANGED
    val homeScoreChange =
        if (scoreChanged && Random.nextFloat() > 1 - PROBABILITY_HOME_SCORE) 1 else 0
    val awayScoreChange = if (scoreChanged && homeScoreChange == 0) 1 else 0
    val offsetChange = (floor(Random.nextFloat() * OFFSET_MAX_STEP) + 1).toInt()

    return Stamp(
        previousValue.offset + offsetChange,
        Score(
            previousValue.score.home + homeScoreChange,
            previousValue.score.away + awayScoreChange
        )
    )
}

fun getScore(gameStamps: Array<Stamp>, offset: Int): Score {
    //если offset меньше минимального, возвращаем начальный счет
    val firstStamp = gameStamps.first()
    if (offset < firstStamp.offset) return firstStamp.score

    //если offset больше максимального, возвращаем финальный счет
    val lastStamp = gameStamps.last()
    if (offset > lastStamp.offset) return lastStamp.score

    //учитываем ситуацию, когда точно такого offset нет, тогда берем ближайший меньший
    var off = offset
    while (true) {
        val foundStamp = gameStamps.find { stamp -> stamp.offset == off }
        if (foundStamp == null) off-- else return foundStamp.score
    }
}


// Main.kt
fun main() {
    val gameStamps = generateGame()
    for (stamp in gameStamps) {
        println("${stamp.offset}: ${stamp.score.home} - ${stamp.score.away}")
    }

    val offset = -50000
    println("Score closest to offset $offset: ${getScore(gameStamps, offset)}")
}