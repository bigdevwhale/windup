# Google Play — всё для ручной публикации

Тексты лежат в `listing/<язык>/*.txt` (копируйте как есть), графика — в `graphics/`, пост о выходе — в `post/`.
Ниже — что вписать в каждое поле Play Console и в каком порядке.

## 0. Перед стартом

- **Нужен AAB, не APK.** Релизный workflow теперь кладёт в GitHub Release `Windup-v<версия>.aab` рядом с
  `Windup.apk`. В релизах до 1.1.1 включительно AAB нет — для первой загрузки нужен **`Windup-v1.1.2.aab`**
  (запустить Release с версией `1.1.2` или запушить тег `v1.1.2`).
- **Языки.** AAB собирается без разбиения по языкам (`bundle.language.enableSplit = false`), иначе Play
  ставит только язык телефона и переключение языка в настройках перестаёт работать. Не удаляйте этот блок.
- **Регистрация имени пакета.** Play Console при создании приложения пишет «You can't use this package
  name because it hasn't been registered». Порядок
  ([справка Google](https://support.google.com/googleplay/android-developer/answer/16761053)):
  1. Play Console → **Android developer verification** → **Package names** → **Register package name** → `app.windup`.
  2. **Select key** → ключ с SHA-256 `1a75a3974a44f5df805c908bc99982fdbb2f98f92c4061ac7c82eaf01c66143b`
     (сертификат CN=Windup, O=Stillcraft — им подписаны все релизы, проверено по `Windup.apk` из v1.1.1).
  3. **Upload APK** → скопировать сниппет и запустить workflow **Package registration proof**
     (Actions → Run workflow → вставить сниппет). Он соберёт релизный APK с файлом
     `assets/adi-registration.properties`, подпишет тем же ключом и проверит отпечаток; APK — в артефактах запуска.
  4. Дождаться письма о регистрации, потом создавать приложение.
- **Подпись (Play App Signing).** При первой загрузке выберите **«Использовать свой ключ»** и загрузите ключ
  из того же keystore через PEPK (инструкция в самом диалоге). Тогда APK из GitHub и версия из Play
  обновляют друг друга. Если оставить «Google создаст ключ подписи», они друг на друга не встанут.
- **versionCode** берётся из номера запуска workflow (`github.run_number`), он только растёт — Play это устраивает.
- **Новый личный аккаунт разработчика** (создан после ноября 2023): перед выходом в production нужен
  закрытый тест — минимум 12 тестировщиков, которые не выходят из теста 14 дней подряд. Если аккаунт уже
  прошёл это требование с другими приложениями Stillcraft, можно сразу в production.
- **Политика конфиденциальности** — `PRIVACY.md` в корне (на двух языках). После push ссылка:
  `https://github.com/bigdevwhale/windup/blob/main/PRIVACY.md`
- **Автозагрузка из CI** (`.github/workflows/play.yml`) — после каждого успешного Release берёт AAB из
  релиза и отправляет в Play. «What's new»: en-US — из текста релиза (раздел версии в `CHANGELOG.md`),
  ru-RU — из скрытого блока `<!-- tg ... -->` того же раздела, обрезается до 500 символов. Если такой релиз
  на треке уже есть, обновляются только заметки. До первой ручной загрузки workflow не нужен, поэтому:
  1. В репозитории Settings → Environments → создать **`google-play`** с Required reviewer (вы) —
     иначе job запустится без подтверждения сразу после релиза.
  2. Play Console → Users and permissions → пригласить `play-publisher@intense-context-514611-h3.iam.gserviceaccount.com`
     с правом выпускать релизы для Windup.
  3. В Google Cloud у пула `github` (Workload Identity Federation) разрешить репозиторию
     `bigdevwhale/windup` действовать от имени этого сервисного аккаунта — как для остальных приложений.
  Пока это не сделано, отклоняйте запуск «Google Play» после релиза (или он упадёт на авторизации — без вреда).
  По умолчанию workflow выкладывает в **production**; пока идёт закрытый тест, запускайте его вручную
  с треком `alpha` (closed testing).

## 1. Создание приложения

| Поле | Значение |
|---|---|
| App name | `Windup: Exercise Snacks` |
| Default language | English (United States) – en-US |
| App or game | **App** |
| Free or paid | Free |
| Declarations | обе галочки (Developer Program Policies, US export laws) |

**App, а не Game.** Игрушки — оформление; суть — короткие подходы в течение дня. В Health & Fitness его найдут
по «exercise snacks», «desk exercises», «movement reminder»; в разделе Games оно бы терялось.

**Название.** «Exercise snacks» — устоявшийся термин (короткие подходы по минуте-две в течение дня) и то, что ищут.
В ru-RU — «Заводилка: зарядка весь день». Само имя на иконке берётся из сборки: Windup / Заводилка.

## 2. Store listing → Main store listing

| Поле | en-US | ru-RU (Add translations → Russian) |
|---|---|---|
| App name (≤30) | `listing/en-US/title.txt` | `listing/ru-RU/title.txt` |
| Short description (≤80) | `listing/en-US/short_description.txt` | `listing/ru-RU/short_description.txt` |
| Full description (≤4000) | `listing/en-US/full_description.txt` | `listing/ru-RU/full_description.txt` |
| App icon 512×512 | `graphics/icon-512.png` | та же |
| Feature graphic 1024×500 | `graphics/feature-graphic-en.png` | `graphics/feature-graphic-ru.png` |
| Phone screenshots (2–8) | `graphics/screenshots-en/1…8.png` | `graphics/screenshots-ru/1…8.png` |
| Tablet screenshots | можно не загружать (только телефон, портрет) | — |
| Video | пусто | — |

Длины: названия 23 и 28, короткие описания 79 и 76, полные около 2900 и 2800 символов.

Скриншоты 1080×1920 собирает `tools/play.py` из снимков README (`docs/screenshots/<язык>/*.jpg`, 1080×2400,
демо-сборка `-Pdemo -Pshots`, `tools/shots.py`): статус-бар закрашивается чистым (9:41), Play не берёт картинки
длиннее двух ширин, поэтому экран уходит за нижний край под подписью. Подписи — Nunito, шрифт приложения, кремом и
латунью на зелёных полосатых обоях полки, под линейкой — ключ. Порядок: полка, «Где вы?», упражнение, барабан дня,
шкатулка, ярмарка, металлы, статистика в вечерней теме. Баннер и пост — стена под лампой и пять игрушек на доске.
Иконка — лягушка с ключом: средние две трети адаптивной иконки, квадрат без скругления (Play скругляет сам).
Как пересобрать:

```bash
python tools/shots.py <папка с сырыми снимками>   # только если поменялись экраны
python tools/play.py                              # графика для Play (нужен pip install pillow)
```

## 3. Store settings

| Поле | Значение |
|---|---|
| App category | App → **Health & Fitness** |
| Tags (до 5) | из списка Play: **Health & fitness**, **Workout**, **Home workout**, **Fitness tracker**; пятым — **Stretching** или **Habit tracker**, если есть в списке |
| Email address | *ваш публичный email* — обязательно, его увидят все |
| Website | `https://bigdevwhale.github.io/stillcraft/apps/windup/` |
| Phone | пусто |
| External marketing | можно оставить включённым |

Набор тегов Play меняет; берите самые близкие к «короткие тренировки без инвентаря дома и на работе».
Не берите теги про бег, йогу и вес — их в приложении нет.

**Имя разработчика** (Settings → Developer account) — **Stillcraft**, одинаковое для всех приложений:
тогда Play сам покажет под карточкой «Другие приложения этого разработчика».

## 4. App content (Policy → App content)

**Privacy policy** — URL выше.

**App access** — *All functionality is available without any access restrictions.* (Аккаунтов нет.)

**Ads** — *No, my app does not contain ads.*

**Content rating** (анкета IARC)
- Email — ваш; категория — **All Other App Types** (это не игра).
- Насилия, страха, сексуального контента, ненормативной лексики, наркотиков — **No**.
- **Gambling / simulated gambling** — **No**. Капсулы из автомата — внутриигровая награда за подходы:
  открываются шестерёнками и пружинами, которые только зарабатываются, за деньги ничего не купить.
- **Users interact / share content** — **No**. **Shares location** — **No**.
- **Digital purchases** — No. **Web browser / unrestricted internet** — No.
- Ожидаемый результат: Everyone / PEGI 3 / 0+.

**Target audience and content**
- Target age: **13–15, 16–17, 18 and over**. Младше 13 не отмечать: иначе действует политика Families с
  отдельными требованиями (и упражнения с гирей и турником детям давать не стоит).
- *Could your store listing unintentionally appeal to children?* → **No**: это приложение для движения в течение
  рабочего дня, тексты — про стол, звонки, чайник и инвентарь; жестяные игрушки — винтажное оформление. Если
  Google не согласится, ответ меняется на Yes без последствий для 13+.

**News apps** — No. **COVID-19** — My app is not a publicly available COVID-19 app.
**Government apps** — No. **Financial features** — *My app doesn't provide any financial features.*

**Health apps** — отметить **Activity and fitness** (упражнения и подходы). Больше ничего: нет медицинских
функций, Health Connect, датчиков здоровья и данных о болезнях. Счёт повторов акселерометром — это не
медицинское измерение. Если форма спросит про регулирование — *not a medical device*, приложение не ставит
диагнозов и не лечит; в описании и `PRIVACY.md` есть оговорка «не медицинское приложение».

**Data safety**

Здесь всё просто: у приложения нет разрешения `INTERNET` (проверено по итоговому манифесту release-сборки),
значит, ничего не уходит с телефона.

- *Does your app collect or share any of the required user data types?* → **No**.
- Итог в карточке: «No data collected», «No data shared with third parties».
- Шифрование и удаление данных при ответе No не спрашиваются.
- Ссылки сообщества (Telegram) открываются во внешнем приложении по нажатию — это не сбор данных приложением.

**Advertising ID** — *No* (рекламы и SDK аналитики нет, разрешения `AD_ID` в сборке нет).

**Разрешения** — особых деклараций не нужно. В release-сборке: `POST_NOTIFICATIONS` (одно напоминание), и от
WorkManager — `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`. Если Play
покажет форму **Foreground service permissions** — ответ: приложение не запускает своих фоновых служб,
разрешение пришло из библиотеки WorkManager. Точных будильников, местоположения, фото, микрофона нет.
Акселерометр разрешения не требует.

## 5. Pricing & distribution

- Countries / regions: все доступные.
- Free, без встроенных покупок.

## 6. Релиз

1. Testing → **Closed testing** (для нового аккаунта; иначе сразу Production или Internal testing) → Create new release.
2. Загрузить `Windup-v1.1.2.aab` из GitHub Release.
3. Release name — номер версии. Release notes:

```
<en-US>
(listing/en-US/release_notes.txt)
</en-US>
<ru-RU>
(listing/ru-RU/release_notes.txt)
</ru-RU>
```

   В `release_notes.txt` сейчас «First release on Google Play» и главное из 1.1.x (≤500 символов).
4. Review release → Start rollout. Первая проверка Google обычно занимает от нескольких дней до недели.
5. После установки из Play проверьте: Настройки → Язык → Русский/English — экран переключается сразу и
   название становится «Заводилка»; подход с «считать в кармане» считает приседания; через заданное время
   приходит напоминание; вечерняя тема включается.

## Следующие версии

- Ничего отдельно писать не нужно: `play.yml` берёт «What's new» из раздела версии в `CHANGELOG.md`
  (en-US — видимый текст, ru-RU — блок `<!-- tg -->`). Релиз уходит в Play после подтверждения в
  окружении `google-play`. Для версий с `<!-- tg skip -->` ru-RU заметок не будет — пишите обычный блок.
- Скриншоты переснимать, только если поменялись показанные экраны: `tools/shots.py` → `python tools/play.py`.

## Риск: капсулы

В мастерской есть автомат с капсулами — случайная награда. Это не лутбокс по правилам Play: капсулы приходят
каждый день и за каждый третий штифт, открываются без денег, ничего не продаётся. Если ревью всё же спросит —
так и ответить; в описании карточки капсулы намеренно не выпячиваются.

## После публикации

Добавьте в README бейдж и ссылку `https://play.google.com/store/apps/details?id=app.windup`, строку
«Get it on Google Play» — в заметки релиза (`release.yml`, как в Glow), ссылку — на страницу сайта.
