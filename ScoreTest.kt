import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import ru.pl.kotlindevelopertestcase.Score
import ru.pl.kotlindevelopertestcase.Stamp
import ru.pl.kotlindevelopertestcase.getScore

class ScoreTest {

    private lateinit var stamps: Array<Stamp>

    @BeforeEach
    fun initTestData() {
        //test data
        stamps = arrayOf(
            Stamp(0, Score(0, 0)),
            Stamp(1, Score(1, 1)),
            Stamp(2, Score(1, 2)),
            Stamp(3, Score(1, 3)),
            Stamp(5, Score(1, 5)),
        )
    }

    @Test
    fun `test getScore() return min score when offset is less than minimum`() {
        val expected = Score(0, 0)
        val actual = getScore(stamps, -2)
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `test getScore() return max score when offset is more than maximum`() {
        val expected = Score(1, 5)
        val actual = getScore(stamps, 100)
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `test getScore() when offset is inside array and has exact value`() {
        val expected = Score(1, 1)
        val actual = getScore(stamps, 1)
        Assertions.assertEquals(expected, actual)
    }

    @Test
    fun `test getScore() when offset is inside array and has approximate value`() {
        val expected = Score(1, 3)
        val actual = getScore(stamps, 4)
        Assertions.assertEquals(expected, actual)
    }
}