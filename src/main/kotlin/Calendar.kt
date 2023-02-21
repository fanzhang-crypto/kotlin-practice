import java.time.DayOfWeek.*
import java.time.LocalDate
import java.time.Month
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
    Calendar(Year.of(2022), Year.of(2024), Calendar.Layout(columns = 3))
        .render()
        .forEach(::println)
}

class Calendar(
    private val yearStart: Year,
    private val yearEnd: Year,
    private val layout: Layout,
    private val locale: Locale = Locale.US
) {
    data class Layout(
        val columns: Int,
        val cellWidth: Int = 3,
        val rowGap: Int = 1,
        val columnGap: Int = 1,
    )

    private val emptyCell = " ".repeat(layout.cellWidth)
    private val emptyWeek = emptyCell.repeat(WEEK_DAYS.size)

    fun render(): Sequence<String> = datesBetween(yearStart, yearEnd)
        .byYear()
        .flatMap { year ->
            year.byMonth()
                .map { month -> layoutMonth(month) }
                .chunked(layout.columns, ::joinMonths)
                .flatMap { it }
        }

    private fun layoutMonth(datesOfMonth: Sequence<LocalDate>): Sequence<String> = sequence {
        val weekDaysTitle = WEEK_DAYS.joinToString(separator = " ", prefix = " ") {
            it.getDisplayName(TextStyle.SHORT, locale).fit(layout.cellWidth - 1)
        }

        val firstDate = datesOfMonth.first()
        val month = firstDate.month

        val monthTitle = month.getDisplayName(TextStyle.FULL, locale).center(emptyWeek.length)

        val yearTitle =
            if (month == Month.JANUARY)
                firstDate.year.toString().left(emptyWeek.length)
            else emptyWeek

        yield(yearTitle)
        yield(monthTitle)
        yield(weekDaysTitle)

        val emptyWeekLines = generateSequence { emptyWeek }

        (datesOfMonth.byWeek().map { layoutWeek(it) } + emptyWeekLines)
            .take(WEEK_LINES_PER_MONTH)
            .forEach { weekLine -> yield(weekLine) }
    }

    private fun layoutWeek(week: Sequence<LocalDate>): String {
        val firstDayOfWeek = week.first().dayOfWeek
        val lastDayOfWeek = week.last().dayOfWeek

        val leftIndent = emptyCell.repeat(WEEK_DAYS.indexOf(firstDayOfWeek))
        val rightIndent = emptyCell.repeat(WEEK_DAYS.reversed().indexOf(lastDayOfWeek))
        val datesString = week.joinToString("") { date -> date.dayOfMonth.toString().fit(layout.cellWidth) }

        return leftIndent + datesString + rightIndent
    }

    private fun joinMonths(months: List<Sequence<String>>): Sequence<String> {
        val columnGap = " ".repeat(layout.columnGap)
        val row = months
            .reduce { acc, month ->
                acc.zip(month) { a, b -> "$a$columnGap$b" }
            }
            .filter { it.isNotBlank() }

        val rowGap = Array(layout.rowGap) { " " }.asSequence()
        return row + rowGap
    }

    companion object {
        private val WEEK_DAYS = arrayOf(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY)

        private const val WEEK_LINES_PER_MONTH = 6

        fun datesBetween(yearStart: Year, yearEnd: Year): Sequence<LocalDate> = sequence {
            var currentDate = yearStart.atDay(1)
            while (currentDate.year < yearEnd.value) {
                yield(currentDate)
//                println("yield $currentDate")
                currentDate = currentDate.plusDays(1)
            }
        }

        fun Sequence<LocalDate>.byYear(): Sequence<Sequence<LocalDate>> =
            this.groupByConsecutiveLazily { d1, d2 -> d1.year == d2.year }

        fun Sequence<LocalDate>.byMonth(): Sequence<Sequence<LocalDate>> =
            this.groupByConsecutive { d1, d2 -> d1.year == d2.year && d1.month == d2.month }

        fun Sequence<LocalDate>.byWeek(): Sequence<Sequence<LocalDate>> =
            this.groupByConsecutive { d1, d2 -> d1.year == d2.year && d1.month == d2.month && d1.weekOfMonth == d2.weekOfMonth }

        private fun <T> Sequence<T>.groupByConsecutive(predicate: (T, T) -> Boolean): Sequence<Sequence<T>> {
            val upstream = this

            return sequence {
                var group = mutableListOf<T>()
                for (item in upstream) {
                    if (group.isEmpty() || predicate(group.last(), item)) {
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

        private fun <T> Sequence<T>.groupByConsecutiveLazily(predicate: (T, T) -> Boolean): Sequence<Sequence<T>> {
            val upstream = this.iterator()

            return sequence {
                var lastItem: T? = null

                while (upstream.hasNext()) {
                    val group = sequence {
                        if (lastItem != null) {
                            yield(lastItem!!)
                        }
                        for (item in upstream) {
                            if (lastItem == null || predicate(lastItem!!, item)) {
                                yield(item)
                                lastItem = item
                            } else {
                                lastItem = item
                                break
                            }
                        }
                    }
                    yield(group)
                }
            }
        }

        private val LocalDate.weekOfYear: Int
            get() = get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())

        private val LocalDate.weekOfMonth: Int
            get() = get(WeekFields.of(Locale.getDefault()).weekOfMonth())

        private fun String.fit(width: Int): String = when {
            length >= width -> truncate(width)
            else -> padStart(width)
        }

        private fun String.center(width: Int): String {
            if (length >= width) {
                return this
            }
            val leftPadLength = (width - length) / 2

            return this.padStart(leftPadLength + length).padEnd(width)
        }

        private fun String.left(width: Int) = this.padEnd(width)
        private fun String.right(width: Int) = this.padStart(width)

        private fun String.truncate(size: Int): String =
            if (size >= length) this else substring(0 until size)
    }
}

