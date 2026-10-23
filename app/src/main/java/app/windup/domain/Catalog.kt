package app.windup.domain

import app.windup.domain.Place.CALL
import app.windup.domain.Place.DESK
import app.windup.domain.Place.KETTLE
import app.windup.domain.Place.KIT
import app.windup.domain.Place.OUTSIDE
import app.windup.domain.Place.QUEUE
import app.windup.domain.Place.TEETH
import app.windup.domain.Toy.ACRO
import app.windup.domain.Toy.BALLET
import app.windup.domain.Toy.BEAR
import app.windup.domain.Toy.BIRD
import app.windup.domain.Toy.FROG
import app.windup.domain.Toy.MONKEY
import app.windup.domain.Toy.PENGUIN
import app.windup.domain.Toy.ROBOT
import app.windup.domain.Toy.SOLDIER

/** Every movement, by toy. Short enough for a kettle, a queue or a phone call. */
object Catalog {
    private fun lad(s: String) = s.split(';').map { it.substringBefore('|') to it.substringAfter('|') }

    private fun e(
        key: String, toy: Toy, ru: String, en: String, cueRu: String, cueEn: String, n: Int, places: Set<Place>,
        unit: Unit = Unit.REPS, kit: Kit? = null, ladder: String? = null, step: Int = 0, pocket: Boolean = false,
    ) = Ex(key, toy, ru, en, cueRu, cueEn, n, unit, places, kit, ladder?.let(::lad) ?: emptyList(), step, pocket)

    private val S = Unit.SECS

    val all: List<Ex> = listOf(
        // The frog: squats and jumps.
        e("frog.chair", FROG, "Присед у стула", "Chair squat", "Коснись сиденья и встань. Пятки на полу, колени смотрят за носки.", "Touch the seat and stand. Heels down, knees track the toes.", 10, setOf(DESK, KETTLE, CALL),
            ladder = "Присед на стул|Sit-to-stand;Присед у стула|Chair-tap squat;Полный присед|Full squat;Присед с паузой|Pause squat;Пистолет на стул|Pistol to a chair", step = 1, pocket = true),
        e("frog.lunge", FROG, "Выпады назад", "Reverse lunges", "Шаг назад, колено почти до пола. Ноги по очереди.", "Step back, knee almost to the floor. Alternate legs.", 10, setOf(KETTLE, CALL), pocket = true),
        e("frog.jump", FROG, "Лягушачьи прыжки", "Frog jumps", "Глубокий присед — и прыжок вперёд. Приземляйся мягко.", "Deep squat, then jump forward. Land softly.", 6, setOf(OUTSIDE, KETTLE), pocket = true),
        e("frog.wall", FROG, "Стульчик у стены", "Wall sit", "Спина к стене, бёдра параллельно полу. Дыши.", "Back to the wall, thighs level with the floor. Breathe.", 30, setOf(KETTLE, DESK), S),
        e("frog.split", FROG, "Болгарские выпады", "Split squats", "Задняя нога на стуле, опускайся прямо вниз. По 5 на ногу.", "Back foot on a chair, sink straight down. Five a leg.", 10, setOf(KETTLE)),
        e("frog.sumo", FROG, "Сумо-присед", "Sumo squat", "Широкая стойка, носки врозь, спина ровная.", "Wide stance, toes out, back straight.", 12, setOf(CALL, KETTLE, TEETH), pocket = true),
        e("frog.goblet", FROG, "Гоблет-присед", "Goblet squat", "Гиря у груди, локти внутри коленей.", "Kettlebell at the chest, elbows inside the knees.", 10, setOf(KIT), kit = Kit.KETTLEBELL, pocket = true),

        // The monkey: pushing.
        e("monkey.desk", MONKEY, "Отжимания от стола", "Desk push-ups", "Руки на край стола, тело — доска. Грудью к краю.", "Hands on the desk edge, body a plank. Chest to the edge.", 10, setOf(DESK, KETTLE),
            ladder = "От стены|Wall push-ups;От стола|Desk push-ups;От колен|Knee push-ups;От пола|Floor push-ups;Лучник|Archer push-ups", step = 1),
        e("monkey.dips", MONKEY, "Обратные отжимания", "Chair dips", "Ладони на сиденье стула за спиной, локти уходят назад.", "Palms on the chair seat behind you, elbows go back.", 10, setOf(DESK, KETTLE)),
        e("monkey.press", MONKEY, "Ладонь в ладонь", "Palm press", "Сожми ладони перед грудью изо всех сил. Никто не заметит.", "Press your palms together as hard as you can. Nobody will notice.", 20, setOf(DESK, QUEUE, CALL), S),
        e("monkey.pike", MONKEY, "Отжимания домиком", "Pike push-ups", "Таз вверх, макушкой к полу между ладоней.", "Hips high, crown of the head down between the hands.", 6, setOf(KETTLE)),
        e("monkey.db", MONKEY, "Жим гантелей стоя", "Dumbbell press", "Над головой, без прогиба в пояснице.", "Overhead, no arch in the lower back.", 10, setOf(KIT), kit = Kit.DUMBBELLS),
        e("monkey.band", MONKEY, "Жим с резинкой", "Band chest press", "Резинка за спиной, руки вперёд и обратно.", "Band behind your back, push forward and back.", 12, setOf(KIT), kit = Kit.BAND),

        // The bear: the trunk, crawling.
        e("bear.plank", BEAR, "Планка", "Plank", "Живот подтянут, таз не провисает. Смотри в пол.", "Belly in, hips don't sag. Look at the floor.", 30, setOf(KETTLE), S,
            ladder = "На столе|Desk plank;На коленях|Knee plank;На предплечьях|Forearm plank;С подъёмом ноги|Leg-lift plank;На одной руке|One-arm plank", step = 2),
        e("bear.crawl", BEAR, "Медвежья походка", "Bear crawl", "На руках и ногах, колени в ладони от пола. Вперёд и назад.", "On hands and feet, knees a palm off the floor. Forward and back.", 20, setOf(KETTLE, OUTSIDE), S),
        e("bear.vacuum", BEAR, "Вакуум", "Stomach vacuum", "Выдохни и втяни живот к позвоночнику. Держи.", "Breathe out and pull the belly to the spine. Hold.", 20, setOf(DESK, QUEUE, CALL), S),
        e("bear.bug", BEAR, "Мёртвый жук", "Dead bug", "Лёжа на спине: рука и противоположная нога тянутся к полу.", "On your back: one arm and the opposite leg reach for the floor.", 10, setOf(KETTLE)),
        e("bear.hollow", BEAR, "Лодочка", "Hollow hold", "Поясница прижата, руки и ноги над полом.", "Lower back pressed down, arms and legs off the floor.", 20, setOf(KETTLE), S),
        e("bear.knees", BEAR, "Колени к груди сидя", "Seated knee tucks", "Сядь на край стула, подтяни колени к груди.", "Sit on the edge of the chair, draw your knees to the chest.", 12, setOf(DESK)),
        e("bear.carry", BEAR, "Прогулка с рюкзаком", "Suitcase carry", "Тяжёлый рюкзак в одной руке — иди и не кренись.", "A heavy bag in one hand — walk and don't lean.", 60, setOf(KIT, OUTSIDE), S, kit = Kit.BACKPACK),

        // The ballerina: balance and ease.
        e("ballet.stork", BALLET, "Стойка на одной ноге", "One-leg stand", "Полщётки на левой, полщётки на правой. Можно закрыть глаза.", "Half the brushing on the left, half on the right. Try closing your eyes.", 30, setOf(TEETH, QUEUE, CALL, KETTLE), S,
            ladder = "У опоры|Holding on;Без опоры|Free;С закрытыми глазами|Eyes closed;На носке|On the toes;На подушке|On a pillow", step = 1),
        e("ballet.swallow", BALLET, "Ласточка", "Single-leg reach", "Наклон вперёд на одной ноге, вторая уходит назад. Спина прямая.", "Hinge forward on one leg, the other goes back. Straight back.", 6, setOf(KETTLE)),
        e("ballet.hips", BALLET, "Круги бёдрами", "Hip circles", "Широкие медленные круги тазом, как хула-хуп.", "Wide slow circles with the hips, like a hula hoop.", 10, setOf(CALL, KETTLE)),
        e("ballet.door", BALLET, "Раскрытие в дверях", "Doorway stretch", "Предплечья на косяк, шаг вперёд — грудь раскрывается.", "Forearms on the frame, step through — the chest opens.", 30, setOf(DESK), S),
        e("ballet.fold", BALLET, "Наклон к носкам", "Forward fold", "Колени мягкие, тянись макушкой вниз.", "Soft knees, let the head hang down.", 30, setOf(KETTLE, TEETH), S),
        e("ballet.neck", BALLET, "Наклоны шеи", "Neck tilts", "Ухо к плечу, медленно, без рывков.", "Ear to shoulder, slowly, no jerks.", 8, setOf(DESK, QUEUE, CALL)),
        e("ballet.cat", BALLET, "Кошка-корова", "Cat-cow", "На четвереньках: спина колесом, потом прогиб.", "On all fours: round the back, then arch it.", 8, setOf(KETTLE)),

        // The drummer: posture and walking.
        e("soldier.angel", SOLDIER, "Ангел у стены", "Wall angels", "Затылок, лопатки и локти у стены. Руки вверх-вниз.", "Head, shoulder blades and elbows on the wall. Arms up and down.", 10, setOf(DESK, KETTLE)),
        e("soldier.march", SOLDIER, "Марш на месте", "March in place", "Колени высоко, руки работают.", "Knees high, arms swinging.", 40, setOf(CALL, KETTLE), pocket = true),
        e("soldier.stairs", SOLDIER, "Лестница", "Stairs", "Три этажа пешком вместо лифта.", "Three floors on foot instead of the lift.", 3, setOf(QUEUE, OUTSIDE), Unit.FLOORS),
        e("soldier.blades", SOLDIER, "Сведение лопаток", "Blade squeeze", "Сведи лопатки и опусти вниз — будто прячешь их в задние карманы.", "Squeeze the shoulder blades and drop them, as if into your back pockets.", 12, setOf(DESK, QUEUE)),
        e("soldier.chin", SOLDIER, "Втянуть подбородок", "Chin tucks", "Голова назад, как будто делаешь двойной подбородок.", "Head straight back, as if making a double chin.", 10, setOf(DESK, QUEUE, CALL)),
        e("soldier.walk", SOLDIER, "Быстрая прогулка", "Brisk walk", "Пять минут бодрым шагом — до угла и обратно.", "Five minutes at a brisk pace — to the corner and back.", 300, setOf(OUTSIDE, CALL), S),
        e("soldier.farmer", SOLDIER, "Прогулка фермера", "Farmer's walk", "Гантели в руках, плечи вниз, иди ровно.", "Dumbbells in hand, shoulders down, walk tall.", 60, setOf(KIT), S, kit = Kit.DUMBBELLS),

        // The bird: hinging and the glutes.
        e("bird.morning", BIRD, "Наклоны «доброе утро»", "Good mornings", "Руки за голову, таз назад, спина прямая.", "Hands behind the head, hips back, straight back.", 10, setOf(KETTLE, CALL),
            ladder = "Неглубокие|Shallow;До параллели|To parallel;С паузой|With a pause;На одной ноге|Single-leg;С рюкзаком|With a backpack", step = 1),
        e("bird.bridge", BIRD, "Ягодичный мост", "Glute bridge", "Лёжа, пятки у таза — подними таз и сожми ягодицы.", "Lying down, heels near the hips — lift and squeeze.", 12, setOf(KETTLE)),
        e("bird.squeeze", BIRD, "Сжать ягодицы", "Glute squeeze", "Сидя или стоя: сожми и держи. Тайное упражнение.", "Sitting or standing: squeeze and hold. A secret one.", 20, setOf(DESK, QUEUE, CALL), S),
        e("bird.kick", BIRD, "Отведение ноги назад", "Standing kickbacks", "Держись за раковину или стол, нога назад, корпус ровно.", "Hold the sink or desk, leg back, trunk still.", 10, setOf(TEETH, CALL, KETTLE)),
        e("bird.swing", BIRD, "Махи гирей", "Kettlebell swings", "Толчок тазом, руки — просто верёвки.", "Drive with the hips, the arms are just ropes.", 15, setOf(KIT), kit = Kit.KETTLEBELL, pocket = true),
        e("bird.rdl", BIRD, "Тяга с рюкзаком", "Backpack deadlift", "Рюкзак в руках, таз назад, спина прямая.", "Bag in hands, hips back, straight back.", 10, setOf(KIT), kit = Kit.BACKPACK),

        // The robot: turning and the sides.
        e("robot.twist", ROBOT, "Повороты сидя", "Seated twists", "Руки на груди, поворачивай корпус, таз неподвижен.", "Arms crossed, turn the trunk, hips still.", 10, setOf(DESK)),
        e("robot.side", ROBOT, "Наклоны в стороны", "Side bends", "Одна рука скользит по бедру вниз, вторая — над головой.", "One hand slides down the thigh, the other over the head.", 10, setOf(QUEUE, CALL, KETTLE)),
        e("robot.splank", ROBOT, "Боковая планка", "Side plank", "На предплечье, тело — одна линия.", "On the forearm, body in one line.", 20, setOf(KETTLE), S),
        e("robot.windmill", ROBOT, "Мельница", "Windmill", "Ноги широко, рука тянется к противоположной стопе.", "Feet wide, one hand reaches for the opposite foot.", 10, setOf(KETTLE, OUTSIDE)),
        e("robot.book", ROBOT, "Открытая книга", "Open book", "Лёжа на боку, верхняя рука раскрывается назад.", "On your side, the top arm opens back like a page.", 8, setOf(KETTLE)),
        e("robot.chop", ROBOT, "Дровосек", "Band woodchop", "Резинка в двери, тяни по диагонали сверху вниз.", "Band in the door, pull diagonally from high to low.", 10, setOf(KIT), kit = Kit.BAND),

        // The penguin: feet and calves.
        e("penguin.calf", PENGUIN, "Подъёмы на носки", "Calf raises", "Медленно вверх, пауза, медленно вниз.", "Slowly up, pause, slowly down.", 20, setOf(QUEUE, TEETH, CALL, DESK),
            ladder = "На двух|Both feet;С паузой|With a pause;На одной|One foot;С края ступеньки|Off a step;С рюкзаком|With a backpack", step = 0),
        e("penguin.ankle", PENGUIN, "Круги стопами", "Ankle circles", "Под столом, по десять в каждую сторону.", "Under the desk, ten each way.", 10, setOf(DESK)),
        e("penguin.heels", PENGUIN, "Ходьба на пятках", "Heel walk", "Носки вверх, мелкими шагами.", "Toes up, small steps.", 20, setOf(KETTLE, CALL), S),
        e("penguin.towel", PENGUIN, "Собрать полотенце", "Towel scrunch", "Пальцами ног подтягивай полотенце к себе.", "Scrunch a towel towards you with your toes.", 10, setOf(DESK)),
        e("penguin.pogo", PENGUIN, "Прыжки на носках", "Pogo hops", "Мелкие пружинистые прыжки, пятки не касаются пола.", "Small springy hops, heels never touch.", 20, setOf(KETTLE, OUTSIDE), pocket = true),
        e("penguin.rope", PENGUIN, "Скакалка", "Jump rope", "Мягко на носках.", "Light on the toes.", 50, setOf(KIT, OUTSIDE), kit = Kit.ROPE, pocket = true),

        // The acrobat: pulling.
        e("acro.towel", ACRO, "Тяга полотенца", "Door towel row", "Полотенце за ручку двери, отклонись назад и тяни себя.", "Towel round a door handle, lean back and pull yourself in.", 10, setOf(KETTLE, DESK)),
        e("acro.apart", ACRO, "Растянуть полотенце", "Towel pull-apart", "Тяни концы полотенца в стороны изо всех сил.", "Pull the ends of a towel apart as hard as you can.", 20, setOf(DESK, CALL), S),
        e("acro.table", ACRO, "Тяга под столом", "Under-table row", "Лёжа под крепким столом, подтягивай грудь к краю.", "Lying under a sturdy table, pull your chest to the edge.", 8, setOf(KETTLE)),
        e("acro.hang", ACRO, "Вис на турнике", "Bar hang", "Просто висеть. Плечи чуть вниз.", "Just hang. Shoulders slightly down.", 20, setOf(KIT, OUTSIDE), S, kit = Kit.BAR),
        e("acro.pullup", ACRO, "Подтягивания", "Pull-ups", "Подбородок над перекладиной, без рывков.", "Chin over the bar, no swinging.", 5, setOf(KIT, OUTSIDE), kit = Kit.BAR,
            ladder = "Негативные|Negatives;С резинкой|Band-assisted;Полные|Full;С паузой|With a pause;Лучник|Archer", step = 1),
        e("acro.band", ACRO, "Тяга резинки", "Band row", "Резинка в двери, локти назад, лопатки вместе.", "Band in the door, elbows back, blades together.", 12, setOf(KIT), kit = Kit.BAND),
    )

    private val byKey = all.associateBy { it.key }
    fun ex(key: String): Ex? = byKey[key]
    fun of(t: Toy) = all.filter { it.toy == t }
}
