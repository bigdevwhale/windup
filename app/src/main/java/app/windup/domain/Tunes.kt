package app.windup.domain

/**
 * The records a finished day's score earns: old tunes, all long out of copyright, played on the music box.
 * Notes are semitones from C5; a 0 step after the first is a repeat, and [beats] stretches a note.
 */
data class Tune(val ru: String, val en: String, val notes: IntArray, val beats: IntArray = IntArray(notes.size) { 1 })

object Tunes {
    private fun t(ru: String, en: String, n: String, b: String? = null): Tune {
        val notes = n.trim().split(Regex("\\s+")).map { it.toInt() }.toIntArray()
        val beats = b?.trim()?.split(Regex("\\s+"))?.map { it.toInt() }?.toIntArray() ?: IntArray(notes.size) { 1 }
        require(beats.size == notes.size) { en }
        return Tune(ru, en, notes, beats)
    }

    val all: List<Tune> = listOf(
        t("Маленькая звёздочка", "Twinkle, Twinkle", "0 0 7 7 9 9 7 5 5 4 4 2 2 0", "1 1 1 1 1 1 2 1 1 1 1 1 1 2"),
        t("Братец Яков", "Frère Jacques", "0 2 4 0 0 2 4 0 4 5 7 4 5 7", "1 1 1 1 1 1 1 1 1 1 2 1 1 2"),
        t("У Мэри был барашек", "Mary Had a Little Lamb", "4 2 0 2 4 4 4 2 2 2 4 7 7", "1 1 1 1 1 1 2 1 1 2 1 1 2"),
        t("Горячие булочки", "Hot Cross Buns", "4 2 0 4 2 0 0 0 0 0 2 2 2 2 4 2 0", "1 1 2 1 1 2 1 1 1 1 1 1 1 1 1 1 2"),
        t("При лунном свете", "Au clair de la lune", "0 0 0 2 4 2 0 4 2 2 0", "1 1 1 1 2 2 1 1 1 1 2"),
        t("Лондонский мост", "London Bridge", "7 9 7 5 4 5 7 2 4 5 4 5 7", "1 1 1 1 1 1 2 1 1 2 1 1 2"),
        t("Лодочка", "Row, Row, Row Your Boat", "0 0 0 2 4 4 2 4 5 7 12 12 12 7 7 7 4 4 4 0 0 0 7 5 4 2 0", "2 2 1 1 2 1 1 1 1 3 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 3"),
        t("Ода к радости", "Ode to Joy", "4 4 5 7 7 5 4 2 0 0 2 4 4 2 2", "1 1 1 1 1 1 1 1 1 1 1 1 1 1 2"),
        t("Бубенцы", "Jingle Bells", "4 4 4 4 4 4 4 7 0 2 4 5 5 5 5 5 4 4 4 4 2 2 4 2 7", "1 1 2 1 1 2 1 1 1 1 3 1 1 1 1 1 1 1 1 1 1 1 1 2 2"),
        t("Янки-дудл", "Yankee Doodle", "0 0 2 4 0 4 2 0 0 2 4 0 -1", "1 1 1 1 1 1 2 1 1 1 1 2 2"),
        t("Хоп — и ласка", "Pop Goes the Weasel", "0 0 2 2 4 7 4 0", "2 1 2 1 1 1 1 2"),
        t("Колыбельная Брамса", "Brahms' Lullaby", "4 4 7 4 4 7 4 7 12 11 9 9 7", "1 1 3 1 1 3 1 1 2 2 1 1 2"),
        t("Коробейники", "Korobeiniki", "4 -1 0 2 0 -1 -3 -3 0 4 2 0 -1 0 2 4 0 -3 -3", "2 1 1 2 1 1 2 1 1 2 1 1 3 1 2 2 2 2 2"),
        t("Зелёные рукава", "Greensleeves", "4 7 9 11 12 11 9 6 2 4 6 7 4 4 3 4 6 3 -1", "1 2 1 2 1 1 2 1 2 1 1 2 1 2 1 1 2 1 2"),
        t("Утро", "Morning Mood", "7 4 2 0 2 4 7 4 2 0 2 4 2 4 7 4 7 9 4 9 7 4 2 0", "1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 1 3"),
        t("К Элизе", "Für Elise", "4 3 4 3 4 -1 2 0 -3", "1 1 1 1 1 1 1 1 3"),
        t("В пещере горного короля", "In the Hall of the Mountain King", "-3 -1 0 2 4 0 4 3 -1 3 2 -2 2 -3 -1 0 2 4 0 4 9 7 4 0 4 7", "1 1 1 1 1 1 2 1 1 2 1 1 2 1 1 1 1 1 1 1 1 1 1 1 1 3"),
    )
}
