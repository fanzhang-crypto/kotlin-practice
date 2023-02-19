import java.lang.StringBuilder
import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.Year
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

/**
 * A little calendar printer program to demo the power of kotlin sequence and FP.
 * Whose output is like this:
 *
 *        January              February                March
 *  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
 *                     1         1  2  3  4  5         1  2  3  4  5
 *   2  3  4  5  6  7  8   6  7  8  9 10 11 12   6  7  8  9 10 11 12
 *   9 10 11 12 13 14 15  13 14 15 16 17 18 19  13 14 15 16 17 18 19
 *  16 17 18 19 20 21 22  20 21 22 23 24 25 26  20 21 22 23 24 25 26
 *  23 24 25 26 27 28 29  27 28                 27 28 29 30 31
 *  30 31
 *
 *         April                  May                  June
 *  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
 *                  1  2   1  2  3  4  5  6  7            1  2  3  4
 *   3  4  5  6  7  8  9   8  9 10 11 12 13 14   5  6  7  8  9 10 11
 *  10 11 12 13 14 15 16  15 16 17 18 19 20 21  12 13 14 15 16 17 18
 *  17 18 19 20 21 22 23  22 23 24 25 26 27 28  19 20 21 22 23 24 25
 *  24 25 26 27 28 29 30  29 30 31              26 27 28 29 30
 *
 *
 *         July                 August               September
 *  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
 *                  1  2      1  2  3  4  5  6               1  2  3
 *   3  4  5  6  7  8  9   7  8  9 10 11 12 13   4  5  6  7  8  9 10
 *  10 11 12 13 14 15 16  14 15 16 17 18 19 20  11 12 13 14 15 16 17
 *  17 18 19 20 21 22 23  21 22 23 24 25 26 27  18 19 20 21 22 23 24
 *  24 25 26 27 28 29 30  28 29 30 31           25 26 27 28 29 30
 *  31
 *
 *        October              November              December
 *  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa  Su Mo Tu We Th Fr Sa
 *                     1         1  2  3  4  5               1  2  3
 *   2  3  4  5  6  7  8   6  7  8  9 10 11 12   4  5  6  7  8  9 10
 *   9 10 11 12 13 14 15  13 14 15 16 17 18 19  11 12 13 14 15 16 17
 *  16 17 18 19 20 21 22  20 21 22 23 24 25 26  18 19 20 21 22 23 24
 *  23 24 25 26 27 28 29  27 28 29 30           25 26 27 28 29 30 31
 *  30 31
 */
fun main() {
    Calendar(Year.of(2022), Year.of(2023), Locale.US)
        .render(Calendar.Layout(3))
        .let { println(it) }
}

class Calendar(
    private val yearStart: Year,
    private val yearEnd: Year,
    private val locale: Locale = Locale.US
) {
    data class Layout(
        val columns: Int,
        val rowGap: Int = 1,
        val columnGap: Int = 1,
    )

    fun render(layout: Layout): String = datesBetween(yearStart, yearEnd)
        .byMonth()
        .map { month -> layoutMonth(month) }
        .chunked(layout.columns) { monthLayouts -> combineMonthLayouts(monthLayouts, layout) }
        .map { it.joinToString("\n") }
        .joinToString("\n")

    private fun layoutMonth(datesOfMonth: Sequence<LocalDate>): Sequence<String> = sequence {
        val weekDaysTitle = WEEK_DAYS.joinToString(separator = " ", prefix = " ") {
            it.getDisplayName(TextStyle.SHORT, locale).truncate(CELL_WITH - 1)
        }

        val month = datesOfMonth.first().month
        val monthTitle = month.getDisplayName(TextStyle.FULL, locale).center(weekDaysTitle.length)

        yield(monthTitle)
        yield(weekDaysTitle)

        val weeks = datesOfMonth.byWeek().map { week ->
            val firstDayOfWeek = week.first().dayOfWeek
            val lastDayOfWeek = week.last().dayOfWeek

            val leftIndent = EMPTY_CELL.repeat(WEEK_DAYS.indexOf(firstDayOfWeek))
            val rightIndent = EMPTY_CELL.repeat(WEEK_DAYS.reversed().indexOf(lastDayOfWeek))
            val datesString = week.map { date -> String.format("%${CELL_WITH}d", date.dayOfMonth) }.joinToString("")

            StringBuilder(weekDaysTitle.length)
                .append(leftIndent)
                .append(datesString)
                .append(rightIndent)
                .toString()
        }

        yieldAll(weeks)
    }

    private fun combineMonthLayouts(monthsOfSameRow: List<Sequence<String>>, layout: Layout): Sequence<String> {
        val columnGap = " ".repeat(layout.columnGap)
        val row = monthsOfSameRow.reduce { acc, month ->
            acc.zipWithDefault(
                month,
                EMPTY_WEEK,
                EMPTY_WEEK
            ) { a, b -> "$a$columnGap$b" }
        }
        val rowGap = Array(layout.rowGap) { " " }.asSequence()

        return row + rowGap
    }

    companion object {
        private val WEEK_DAYS = arrayOf(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)

        private const val CELL_WITH = 3

        private val EMPTY_CELL = " ".repeat(CELL_WITH)
        private val EMPTY_WEEK = EMPTY_CELL.repeat(WEEK_DAYS.size)

        private fun datesBetween(yearStart: Year, yearEnd: Year): Sequence<LocalDate> = sequence {
            var currentDate = yearStart.atDay(1)
            while (currentDate.year < yearEnd.value) {
                yield(currentDate)
                currentDate = currentDate.plusDays(1)
            }
        }

        private fun Sequence<LocalDate>.byMonth(): Sequence<Sequence<LocalDate>> =
            this.groupByConsecutive { d1, d2 -> d1.year == d2.year && d1.month == d2.month }

        private fun Sequence<LocalDate>.byWeek(): Sequence<Sequence<LocalDate>> =
            this.groupByConsecutive { d1, d2 -> d1.year == d2.year && d1.month == d2.month && d1.weekOfMonth == d2.weekOfMonth }

        private fun <T> Sequence<T>.groupByConsecutive(predicate: (T, T) -> Boolean): Sequence<Sequence<T>> {
            val upstream = this

            return sequence {
                var group = mutableListOf<T>()
                for (item in upstream) {
                    if (group.isEmpty()) {
                        group += item
                        continue
                    }
                    if (predicate(group.last(), item)) {
                        group += item
                        continue
                    }
                    yield(group.asSequence())

                    group = mutableListOf()
                    group += item
                }
                yield(group.asSequence())
            }
        }

        private val LocalDate.weekOfYear: Int
            get() = get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())

        private val LocalDate.weekOfMonth: Int
            get() = get(WeekFields.of(Locale.getDefault()).weekOfMonth())

        private fun <L, R, V> Sequence<L>.zipWithDefault(
            other: Sequence<R>,
            leftDefault: L,
            rightDefault: R,
            transform: (a: L, b: R) -> V
        ): Sequence<V> = sequence {
            val leftIter = iterator()
            val rightIter = other.iterator()

            while (leftIter.hasNext() && rightIter.hasNext()) {
                yield(transform(leftIter.next(), rightIter.next()))
            }
            while (leftIter.hasNext()) {
                yield(transform(leftIter.next(), rightDefault))
            }
            while (rightIter.hasNext()) {
                yield(transform(leftDefault, rightIter.next()))
            }
        }

        private fun String.center(size: Int, pad: String = " "): String {
            if (this.length >= size) {
                return this
            }
            val leftPad = pad.repeat((size - this.length) / 2)
            val rightPad = pad.repeat(size - this.length - leftPad.length)

            return "$leftPad$this$rightPad"
        }

        private fun String.truncate(size: Int): String =
            if (size >= length) this else substring(0 until size)
    }
}

