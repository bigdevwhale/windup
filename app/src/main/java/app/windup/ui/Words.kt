package app.windup.ui

import app.windup.domain.Ex
import app.windup.domain.Kit
import app.windup.domain.Place
import app.windup.domain.Reading
import app.windup.domain.Toy
import app.windup.domain.Unit
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/** The words of the workshop that come from data rather than resources, in both languages. */
class Words(val ru: Boolean) {
    fun t(r: String, e: String) = if (ru) r else e

    fun plural(n: Int, one: String, few: String, many: String): String {
        val a = abs(n) % 100; val b = a % 10
        return when { a in 11..19 -> many; b == 1 -> one; b in 2..4 -> few; else -> many }
    }

    /** "He stopped" / "she stopped" for a toy. */
    fun g(t: Toy, f: String, m: String) = if (t.fem) f else m

    fun toy(t: Toy) = when (t) {
        Toy.FROG -> t("Лягушка", "Frog"); Toy.MONKEY -> t("Обезьянка", "Monkey"); Toy.BEAR -> t("Медведь", "Bear")
        Toy.BALLET -> t("Балерина", "Ballerina"); Toy.SOLDIER -> t("Барабанщик", "Drummer"); Toy.BIRD -> t("Птичка", "Bird")
        Toy.ROBOT -> t("Робот", "Robot"); Toy.PENGUIN -> t("Пингвин", "Penguin"); Toy.ACRO -> t("Акробат", "Acrobat")
        Toy.SKIER -> t("Лыжник", "Skier"); Toy.HARE -> t("Заяц", "Hare"); Toy.DIVER -> t("Водолаз", "Diver")
        Toy.MINER -> t("Шахтёр", "Miner"); Toy.CLOWN -> t("Клоун", "Clown"); Toy.SAILOR -> t("Морячок", "Sailor")
    }

    fun pattern(t: Toy) = when (t) {
        Toy.FROG -> t("присед и прыжки", "squats and jumps"); Toy.MONKEY -> t("жим", "pushing")
        Toy.BEAR -> t("корпус и ползание", "core and crawling"); Toy.BALLET -> t("баланс и гибкость", "balance and ease")
        Toy.SOLDIER -> t("осанка и шаг", "posture and walking"); Toy.BIRD -> t("наклоны и ягодицы", "hinges and glutes")
        Toy.ROBOT -> t("повороты и бока", "turning and the sides"); Toy.PENGUIN -> t("стопы и икры", "feet and calves")
        Toy.ACRO -> t("тяга", "pulling")
        Toy.SKIER -> t("ноги вбок", "legs, side to side"); Toy.HARE -> t("прыжки и сердце", "jumps and the heart")
        Toy.DIVER -> t("дыхание", "breathing"); Toy.MINER -> t("толкать и нести", "pushing and carrying")
        Toy.CLOWN -> t("координация", "coordination"); Toy.SAILOR -> t("плечи", "shoulders")
    }

    fun paint(t: Toy) = when (t) {
        Toy.FROG -> t("Ёлочная", "Christmas"); Toy.MONKEY -> t("Цирковая", "Circus"); Toy.BEAR -> t("Белый", "Polar")
        Toy.BALLET -> t("Лебедь", "Swan"); Toy.SOLDIER -> t("Гвардеец", "Guardsman"); Toy.BIRD -> t("Снегирь", "Bullfinch")
        Toy.ROBOT -> t("Лунный", "Lunar"); Toy.PENGUIN -> t("Императорский", "Emperor"); Toy.ACRO -> t("Цирковой", "Big top")
        Toy.SKIER -> t("Олимпийский", "Olympic"); Toy.HARE -> t("Русак", "Brown hare"); Toy.DIVER -> t("Серебряный шлем", "Silver helmet")
        Toy.MINER -> t("Красная вагонетка", "Red cart"); Toy.CLOWN -> t("Рыжий", "Auguste"); Toy.SAILOR -> t("Капитан", "Captain")
    }

    fun tin(t: Toy) = g(t, t("Жестяная", "Tin"), t("Жестяной", "Tin"))

    fun alloy(a: Int) = listOf(t("Жесть", "Tin"), t("Латунь", "Brass"), t("Медь", "Copper"), t("Серебро", "Silver"), t("Золото", "Gold"))[a.coerceIn(0, 4)]

    /** What the next metal brings. */
    fun alloyGift(a: Int) = when (a) {
        1 -> t("латунные ключ и табличка", "a brass key and plate")
        2 -> t("трюк: сальто каждый шестой круг", "a trick: a somersault every sixth round")
        3 -> t("серебряные искры на ходу", "silver glints as it runs")
        else -> t("позолота с головы до ног", "gilt from head to toe")
    }

    fun tune(i: Int): String {
        val tn = app.windup.domain.Tunes.all[i % app.windup.domain.Tunes.all.size]
        val round = i / app.windup.domain.Tunes.all.size
        return t(tn.ru, tn.en) + if (round > 0) t(" · запись ${round + 1}", " · take ${round + 1}") else ""
    }

    fun place(p: Place) = when (p) {
        Place.DESK -> t("За столом", "At the desk"); Place.KETTLE -> t("Пока закипает чайник", "While the kettle boils")
        Place.QUEUE -> t("Очередь, лифт", "Queue, lift"); Place.TEETH -> t("Чищу зубы", "Brushing teeth")
        Place.CALL -> t("Говорю по телефону", "On a call"); Place.OUTSIDE -> t("На улице", "Outside")
        Place.KIT -> t("С инвентарём", "With kit")
    }

    fun placeSub(p: Place) = when (p) {
        Place.DESK -> t("тихо, без пота", "quiet, no sweat"); Place.KETTLE -> t("три минуты, кухня", "three minutes, kitchen")
        Place.QUEUE -> t("незаметно", "unnoticed"); Place.TEETH -> t("две минуты", "two minutes")
        Place.CALL -> t("руки заняты", "hands busy"); Place.OUTSIDE -> t("площадка, лестница", "a park, the stairs")
        Place.KIT -> t("что есть дома", "what's at home")
    }

    fun kit(k: Kit) = when (k) {
        Kit.KETTLEBELL -> t("гиря", "kettlebell"); Kit.DUMBBELLS -> t("гантели", "dumbbells"); Kit.BAND -> t("резинка", "band")
        Kit.BAR -> t("турник", "pull-up bar"); Kit.ROPE -> t("скакалка", "jump rope"); Kit.BACKPACK -> t("рюкзак", "backpack")
    }

    fun name(e: Ex, step: Int) = if (e.ladder.isNotEmpty()) e.ladder[step].let { t(it.first, it.second) } else t(e.ru, e.en)
    fun cue(e: Ex) = t(e.cueRu, e.cueEn)

    fun amount(n: Int, u: Unit) = when (u) {
        Unit.REPS -> if (ru) "$n ${plural(n, "раз", "раза", "раз")}" else "$n rep${if (n == 1) "" else "s"}"
        Unit.SECS -> secs(n)
        Unit.FLOORS -> if (ru) "$n ${plural(n, "этаж", "этажа", "этажей")}" else "$n floor${if (n == 1) "" else "s"}"
    }

    fun secs(n: Int) = when {
        n >= 120 && n % 60 == 0 -> minutes(n / 60)
        else -> if (ru) "$n сек" else "$n s"
    }

    fun minutes(m: Int) = if (ru) "$m мин" else "$m min"
    fun hm(m: Int) = if (m >= 60) (if (ru) "${m / 60} ч ${"%02d".format(m % 60)}" else "${m / 60} h ${"%02d".format(m % 60)}") else minutes(m)
    fun pins(n: Int) = if (ru) "$n ${plural(n, "штифт", "штифта", "штифтов")}" else "$n pin${if (n == 1) "" else "s"}"
    fun gears(n: Int) = if (ru) "$n ${plural(n, "шестерёнка", "шестерёнки", "шестерёнок")}" else "$n gear${if (n == 1) "" else "s"}"
    fun springs(n: Int) = if (ru) "$n ${plural(n, "пружина", "пружины", "пружин")}" else "$n spring${if (n == 1) "" else "s"}"
    fun clock(minuteOfDay: Int) = "%02d:%02d".format(minuteOfDay / 60 % 24, minuteOfDay % 60)

    fun stopped(t: Toy) = "${toy(t)} ${g(t, t("остановилась", "has stopped"), t("остановился", "has stopped"))}…"

    fun nudgeLine(t: Toy, seed: Long): String {
        val lines = if (ru) listOf(
            "Минутка где угодно — и ${g(t, "она снова пойдёт", "он снова пойдёт")}.",
            "Ключ ждёт. Пока закипает чайник — самое время.",
            "Пара минут движения — и полка снова оживёт.",
        ) else listOf(
            "A minute anywhere and it's off again.",
            "The key is waiting. The kettle's time is enough.",
            "A couple of minutes of moving and the shelf comes alive.",
        )
        return lines[((seed / 60_000) % lines.size).toInt()]
    }

    fun state(t: Toy, wind: Float, rust: Float, wound: Boolean) = buildString {
        append(
            when {
                wind > 0 -> t("идёт, завода ${(wind * 100).toInt()}%", "running, ${(wind * 100).toInt()}% wound")
                wound -> g(t, t("стоит", "stopped"), t("стоит", "stopped"))
                else -> t("сегодня не заводили", "not wound today")
            },
        )
        if (rust > .05f) append(t(" · ржавчина ${(rust * 100).toInt()}%", " · rust ${(rust * 100).toInt()}%"))
    }

    fun day(d: LocalDate, today: LocalDate) = when (d) {
        today -> t("Сегодня", "Today")
        today.minusDays(1) -> t("Вчера", "Yesterday")
        else -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH).replaceFirstChar { it.uppercase() } + " " + d.dayOfMonth
    }

    /** What the music box says about a day. */
    fun verdict(r: Reading) = when {
        r.pins == 0 -> t("Пустой барабан — тишина.", "An empty drum — silence.")
        r.pins > 3 && r.span < 60 -> t("Аккордовая каша: ${pins(r.pins)} за ${minutes(r.span.coerceAtLeast(1))}. Пружина перекручена, а игрушки весь день стояли.",
            "A chord mush: ${pins(r.pins)} in ${minutes(r.span.coerceAtLeast(1))}. The spring was overwound and the toys stood still all day.")
        r.even >= 75 -> t("Мелодия: штифты разбросаны по всему дню, ноты не толкаются.", "A tune: the pins are spread across the day and the notes don't crowd.")
        else -> t("Мелодия с паузами: есть долгие часы тишины.", "A tune with rests: there are long hours of silence.")
    }
}
