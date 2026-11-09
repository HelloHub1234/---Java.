# AGENTS.md

Руководство для контрибьюторов и AI-агентов, работающих в этом репозитории.

## Что это за проект

Кликер-игра на Java Swing (`Тестовый кликкер`) плюс набор консольных мини-игр. Проект без системы сборки:
компиляция выполняется напрямую через `javac`, внешних библиотек нет — только JDK (Swing/AWT)
и стандартная библиотека.

- Точка входа кликера: `src/WindowApps/clicker/Clicker.java` (`WindowApps.clicker.Clicker`).
- Точка входа мини-игр: `src/MyGames/Launcher.java` (`MyGames.Launcher`).
- Корневой `src/Main.java` — пустая заглушка, в сборку не входит.

## Структура проекта

```
src/WindowApps/                 # общий каркас (переиспользуется приложениями)
  Basic.java                    # базовое окно: темы, кнопки, частицы, эффекты, actions, restart()
  ClickSound.java               # синтезированные звуки UI (кнопка, клик, достижение, сброс), без аудиофайлов
  AppIconPainter.java           # единственный источник иконки: окно + assets/clicker.ico для .exe
  Theme.java                    # enum палитр (STANDARD, CYBERPUNK, FOREST, SOLAR) + byName()
  Text.java                     # i18n через ResourceBundle
  ProgressSnapshot.java         # снимок состояния прогресса
  ProgressStorage.java          # бинарное хранилище: HMAC-SHA256 + AES/GCM, clicker.sav
  messages*.properties          # локали: ru (default), en, de, es, fr
  clicker/                      # конкретное приложение «Кликер»
    Clicker.java                # точка входа: UI, действия, таймер автокликера
    model/                      # бизнес-модель приложения
      GameProgress.java         # клики, апгрейды, достижения, слушатели
      Achievement.java          # перечень достижений и условия получения
    storage/                    # сохранение прогресса
      ProgressStorage.java      # тонкая обёртка над WindowApps.ProgressStorage
    ui/                         # окна, панели и уведомления
      ControlPanel.java         # нижняя панель кнопок
      CountPanel.java           # счётчик и главная кнопка
      ShopDialog.java           # окно магазина апгрейдов
      AchievementDialog.java    # окно достижений
      AchievementToast.java     # всплывающее уведомление о достижении
      ResetCountdown.java       # окно обратного отсчёта 3-2-1 перед сбросом прогресса
      SettingsDialog.java       # окно настроек: язык и тема
      component/                # переиспользуемые визуальные компоненты
        CardPanel.java          # стеклянная карточка
        ClickButton.java        # главная кнопка клика
      theme/                    # оформление
        ClickerTheme.java       # константы размеров, шрифтов, задержек
src/MyGames/                    # консольные мини-игры, точка входа Launcher.java
src/tools/IconExport.java       # генератор assets/clicker.ico (headless, окно не открывает)
assets/clicker.ico              # иконка .exe для jpackage --icon (коммитится)
tools/build-clicker.ps1         # локальная сборка Clicker-windows.zip (зеркало CI)
tools/generate-icon.ps1         # перегенерация .ico после правок AppIconPainter
.github/workflows/build.yml     # CI: сборка jar и app-image (только кликер)
README.md                       # пользовательское описание проекта
```

Тестов в репозитории нет; `src` — единственный source root (см. `untitled.iml`).

## Сборка и запуск

```powershell
# кликер: компиляция (нужны ВСЕ классы src/WindowApps, а не только Basic.java —
# иначе javac не найдёт Theme, Text, ClickSound и т.д.)
New-Item -ItemType Directory -Force -Path classes | Out-Null
javac -encoding UTF-8 -d classes (Get-ChildItem src/WindowApps/*.java) (Get-ChildItem src/WindowApps/clicker/*.java -Recurse)

# Исходники компилируются без ресурсов, но для UI нужны messages*.properties в classpath:
Copy-Item src/WindowApps/messages*.properties classes/WindowApps/

# запуск
java -cp classes WindowApps.clicker.Clicker

# мини-игры
javac -encoding UTF-8 -d classes (Get-ChildItem src/MyGames/*.java)
java -cp classes MyGames.Launcher
```

`-encoding UTF-8` обязателен: исходники и ресурсы содержат кириллицу.
`java.util.Properties` читает `.properties` в ISO-8859-1 — `messages*.properties` должны
оставаться в этой кодировке (или в `\uXXXX`-эскейпах), а не в UTF-8.

Полный пайплайн (в CI, `windows-latest`, JDK 21): `javac` → `jar --create --main-class WindowApps.clicker.Clicker`
→ `jpackage --type app-image` → артефакт `dist/Clicker-windows.zip`. Тег `v*` дополнительно
публикует релиз на GitHub. Тот же пайплайн локально: `powershell -File tools/build-clicker.ps1`.
`src/MyGames` в CI не собирается — только вручную или из IDE.

## Иконка приложения

Рисуется кодом в `WindowApps.AppIconPainter` (градиент в цветах темы + курсор-стрелка), а не берётся из картинки:
окно показывает её через `Basic.createAppIcons()` и перекрашивает при смене темы. Чтобы `.exe` и окно
совпадали, `src/tools/IconExport.java` рендерит тот же рисунок в `assets/clicker.ico` (7 размеров),
а CI передаёт его в `jpackage --icon`. Поменяли геометрию — перегенерируйте `.ico`:

```powershell
powershell -File tools/generate-icon.ps1
```

Скрипты в `tools/` держите в ASCII: Windows PowerShell 5.1 читает `.ps1` без BOM в ANSI-кодировке,
и кириллица ломает парсер на русской локали.

`.ico` коммитится в репозиторий, потому что CI не запускает генератор (лишний шаг сборки; при расхождении
иконка просто станет старой версии). Смена `--icon` у уже установленного `Clicker.exe` не подхватывается —
ярлык может кэшировать старую иконку.

## Стиль кода и соглашения

- Java 17+ (в CI — 21); используйте `var`-совместимый синтаксис осторожно: в коде стиль явных типов,
  `record`-ов нет, кроме `ProgressSnapshot`.
- Отступы — 4 пробела, фигурные скобки на той же строке, длина строки ~120 символов.
- Имена: классы `PascalCase`, методы/поля `camelCase`, константы `UPPER_SNAKE_CASE` (числовые
  константы вынесены в `ClickerTheme` / `Basic`).
- UI-строки добавляйте ключами в `messages.properties` и читайте через `Text.get(...)` /
  `Text.format(...)`; не хардкодьте текст в компонентах. Дублируйте ключ во всех пяти локалях —
  при отсутствии ключа возвращается сам ключ.
- Новые команды кнопок — константы (`ControlPanel.CLICK`, `Basic.EXIT`) и запись в
  `Map<String, Runnable> actions` в `initActions()`; обработка незнакомой команды уже есть в `handle(...)`.
- Вся работа со Swing — только в EDT (`SwingUtilities.invokeLater`).
- Звук — только через `WindowApps.ClickSound` (сэмплы синтезируются в памяти, файлов нет).
  Звук кнопок уже висит в `Basic.RoundedButton`, отдельный тембр главной кнопки — в `ClickerTheme.createClickButton`;
  не дублируйте `ClickSound.play()` в своих `ActionListener`, иначе будет двойной тик.
  Готовые тембры: `playButton()`, `playClick()`, `playAchievement()`, `playReset()`.
- Эффекты — методы `Basic`: `spawnParticles(Point)`, `spawnFloatingNumber(Point, String)`,
  `spawnFireworks()`, `spawnResetWave()`, `spawnFramePulse()`. Рисуются в `ParticleLayer` поверх
  содержимого окна, координаты — в системе панели. Точку нажатия по всему окну можно получить
  подпиской на `panel.addMouseListener` (событие всплывает от дочерних компонентов, включая кнопки).
- Эффекты и частицы не должны будить EDT вхолостую: таймеры запускаются при спавне и
  останавливаются, когда список пуст; перерисовка — через `repaintOverlay()`, а не `repaint()` окна.
  Фон окна кэшируется в `BufferedImage` и пересобирается только при смене размера или темы.
- Сброс прогресса идёт через `ResetCountdown.show(...)`: окно отсчёта 3-2-1 с кнопкой отмены,
  автокликер на это время замирает. Не вызывайте `storage.resetAll()` напрямую из обработчика кнопки.
- Изменяйте `GameProgress` через его методы (они выставляют `markDirty()` и вызывают слушателей);
  UI обновляется подпиской `progress.addListener(...)` — счётчик и перезапуск автокликера подписаны там же.
- Прогресс лежит в бинарном `clicker.sav` в `%APPDATA%\Clicker` (на не-Windows — `~/.clicker`),
  логика — в `WindowApps.ProgressStorage`. При добавлении поля обновите `ProgressRecord.encode()` /
  `decode()` и обработку в `load()`. Старый `config.properties` — только легаси-миграция, не воскрешайте его.
- Полный сброс — `ProgressStorage.resetAll()` (удаляет `clicker.sav`, обнуляет модель и достижения
  через `deleteStore()`); частичный — `GameProgress.reset()` (только клики).
- Язык меняется только вместе с пересборкой окна (`Basic.restart()`): тексты уже созданных компонентов
  не обновляются. Тема применяется живьём через `Basic.setTheme(...)`.
- Подпись под счётчиком «Автокликер: N в секунду» показывает реальную прибавку кликов
  (`clicksPerAction() * уровень`), а не число тиков таймера. При правках формулы скорости
  держите в синхроне `Clicker` и строку `count.hint.auto`.

## VCS: коммиты и pull request

- Git-репозиторий в рабочей копии может отсутствовать — проверьте `git status` перед операциями.
- Сообщения коммитов: короткая императивная строка на русском, без префиксов
  (`добавить магазин апгрейдов`, `исправить сохранение прогресса`).
- PR: опишите изменение и затронутые файлы, приложите скриншот UI до/после для визуальных правок,
  убедитесь, что CI-джоба `build-clicker` зелёная.
- Не коммитьте конфигурацию и локальный прогресс: `config.properties`, `clicker.sav`, `out/`, `classes/`, `dist/`,
  `.icon-build/`, `.idea/`, `.aiforopenide/`, `.neo/`, `.veai/`.
