package app.windup.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Stairs
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkspacePremium
import app.windup.ui.components.Tip

/** What each screen's guide says: a title and a few points. */
class Guide(val key: String, val title: String, val tips: List<Tip>)

object Guides {
    fun shelf(w: Words) = Guide(
        "shelf", w.t("Как устроена полка", "How the shelf works"),
        listOf(
            Tip(Icons.Rounded.TouchApp, w.t("Нажми на игрушку — откроются её упражнения и состояние.", "Tap a toy for its movements and how it's doing.")),
            Tip(Icons.Rounded.Place, w.t("«Завести» спросит, где ты, и предложит то, что подходит прямо сейчас.", "\"Wind up\" asks where you are and offers what fits right now.")),
            Tip(Icons.Rounded.CheckCircle, w.t("Полоска на табличке — сколько осталось завода, ✓ — сегодня уже заводили.", "The bar on a plate is the winding left; ✓ means wound today.")),
            Tip(Icons.Rounded.Celebration, w.t("Заведи все игрушки за день — будет парад и капсула в мастерской.", "Wind every toy in a day for a parade and a capsule in the workshop.")),
        ),
    )

    fun box(w: Words) = Guide(
        "box", w.t("Как играет шкатулка", "How the music box plays"),
        listOf(
            Tip(Icons.Rounded.Album, w.t("Каждый завод — штифт на барабане, у каждой игрушки своя нота.", "Every winding is a pin on the drum; each toy has its own note.")),
            Tip(Icons.Rounded.PlayArrow, w.t("Нажми ▶ — день сыграет мелодией, час за секунду.", "Press ▶ and the day plays as a tune, an hour a second.")),
            Tip(Icons.Rounded.CalendarMonth, w.t("Выбери день в ленте — сыграет он. Чёрточки под датой — сколько было штифтов.", "Pick a day in the strip to play it. The bars under the date show its pins.")),
            Tip(Icons.Rounded.Equalizer, w.t("Ровность — доля двухчасовых окон дня со штифтом. Простой — самый долгий перерыв.", "Evenness is the share of two-hour windows with a pin. Still is the longest gap.")),
        ),
    )

    fun stats(w: Words) = Guide(
        "stats", w.t("Что здесь считается", "What's counted here"),
        listOf(
            Tip(Icons.Rounded.LocalFireDepartment, w.t("Серия — сколько дней подряд был хотя бы один завод.", "The streak is days in a row with at least one winding.")),
            Tip(Icons.Rounded.BarChart, w.t("Столбики — заводы по дням, пунктир — среднее. Нажми на столбик — покажет число.", "Bars are windings per day, the dashed line the average. Tap a bar for its number.")),
            Tip(Icons.Rounded.Schedule, w.t("«Когда ты двигаешься» — в какие часы ты заводишь игрушки чаще.", "\"When you move\" shows the hours you wind most.")),
        ),
    )

    fun shop(w: Words) = Guide(
        "shop", w.t("Что есть в мастерской", "What's in the workshop"),
        listOf(
            Tip(Icons.Rounded.Inventory2, w.t("Капсула — каждый день и за каждые 3 штифта: вызов, раскраска или детали.", "A capsule a day and one per 3 pins: a challenge, a paint job or parts.")),
            Tip(Icons.Rounded.Storefront, w.t("По выходным открыта ярмарка — новые игрушки и новые движения.", "At weekends the fair opens with new toys and new movements.")),
            Tip(Icons.Rounded.WorkspacePremium, w.t("Переливай игрушки в металл покрепче — нужны детали и подходы с ними.", "Recast toys in finer metals — it takes parts and sets with them.")),
            Tip(Icons.Rounded.FitnessCenter, w.t("Во вкладке «Дома» отметь снаряжение — появятся упражнения с ним.", "Under Kit, tick what you have and movements with it appear.")),
        ),
    )

    fun toy(w: Words) = Guide(
        "toy", w.t("Карточка игрушки", "A toy's card"),
        listOf(
            Tip(Icons.Rounded.PlayArrow, w.t("Сверху — что с игрушкой сейчас: идёт, остановилась или сегодня не заводили.", "On top: what the toy is doing — running, stopped or not wound today.")),
            Tip(Icons.Rounded.TouchApp, w.t("Выбери упражнение — сделаешь подход, и игрушка заведётся на два часа.", "Pick a movement, do a set, and the toy runs for two hours.")),
            Tip(Icons.Rounded.WaterDrop, w.t("Без заводов игрушка ржавеет; совсем заржавевшей нужно три подхода за день.", "Left unwound a toy rusts; a fully rusted one needs three sets in a day.")),
        ),
    )

    fun where(w: Words) = Guide(
        "where", w.t("Где ты сейчас?", "Where are you?"),
        listOf(
            Tip(Icons.Rounded.Place, w.t("Выбери место — полка предложит то, что можно сделать прямо там.", "Pick a place and the shelf offers what you can do right there.")),
            Tip(Icons.Rounded.Build, w.t("Сначала — остановившиеся и ржавые игрушки: им завод нужнее.", "Stopped and rusty toys come first: they need it most.")),
        ),
    )

    fun exercise(w: Words) = Guide(
        "exercise", w.t("Как делать подход", "How a set goes"),
        listOf(
            Tip(Icons.Rounded.TouchApp, w.t("Сделал повтор — нажми «Щёлк!». Каждый щелчок поворачивает ключ.", "Did a rep? Press \"Click!\" — each click turns the key.")),
            Tip(Icons.Rounded.PhoneAndroid, w.t("«В кармане» — положи телефон в карман, он сам посчитает приседания и прыжки.", "\"In pocket\": put the phone away and it counts squats and jumps itself.")),
            Tip(Icons.Rounded.Stairs, w.t("«Легче» и «сложнее» меняют ступень упражнения.", "\"Easier\" and \"harder\" move the movement a step.")),
        ),
    )
}
