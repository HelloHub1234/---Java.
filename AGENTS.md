# AGENTS.md

Руководство для контрибьюторов и AI-агентов, работающих в этом репозитории.

## Что это за проект

Кликер-игра на Java Swing (`Тестовый кликкер`). Проект без системы сборки: компиляция выполняется
напрямую через `javac`, внешних библиотек нет — только JDK (Swing/AWT) и стандартная библиотека.

- Точка входа приложения: `src/WindowApps/clicker/Clicker.java` (`WindowApps.clicker.Clicker`).
- Корневой `src/Main.java` — пустая заглушка, в сборку не входит.

## Структура проекта

```
src/WindowApps/                 # общий каркас (переиспользуется приложениями)
  Basic.java                    # базовое окно: темы, кнопки, частицы, actions
  Theme.java                    # enum палитр (STANDARD, CYBERPUNK, FOREST, SOLAR)
  Text.java                     # i18n через ResourceBundle
  ProgressSnapshot.java         # снимок состояния прогресса
  messages*.properties          # локали: ru (default), en, de, es, fr
  clicker/                      # конкретное приложение «Кликер»
    Clicker.java                # UI, действия, таймер автокликера
    GameProgress.java           # бизнес-модель: клики, апгрейды, слушатели
    ProgressStorage.java        # load/save в config.properties
    ControlPanel.java           # вертикальная панель кнопок
    ShopDialog.java             # окно магазина апгрейдов
    ClickerTheme.java           # константы размеров, шрифтов, задержек
config.properties               # сохранённый прогресс (создаётся при сохранении)
.github/workflows/build.yml     # CI: сборка jar и app-image
```

Тестов в репозитории нет; `src` — единственный source root (см. `untitled.iml`).

## Сборка и запуск

```powershell
# компиляция (Windows PowerShell)
New-Item -ItemType Directory -Force -Path classes | Out-Null
javac -encoding UTF-8 -d classes src/WindowApps/Basic.java (Get-ChildItem src/WindowApps/clicker/*.java)

# запуск
java -cp classes WindowApps.clicker.Clicker
```

`-encoding UTF-8` обязателен: исходники и ресурсы содержат кириллицу.
`java.util.Properties` читает `.properties` в ISO-8859-1 — `messages*.properties` должны
оставаться в этой кодировке (или в `\uXXXX`-эскейпах), а не в UTF-8.

Полный пайплайн (в CI, `windows-latest`, JDK 21): `javac` → `jar --create --main-class WindowApps.clicker.Clicker`
→ `jpackage --type app-image` → артефакт `dist/Clicker-windows.zip`. Тег `v*` дополнительно
публикует релиз на GitHub. Локальную сборку удобнее запускать через конфигурации IDE.

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
- Изменяйте `GameProgress` через его методы (они выставляют `markDirty()` и вызывают слушателей);
  UI обновляется подпиской `progress.addListener(this::showCount)`.
- Файл прогресса — `config.properties` в рабочем каталоге; при добавлении поля обновите
  ключи в `ProgressStorage` и значения по умолчанию в `load()`.

## VCS: коммиты и pull request

- Git-репозиторий в рабочей копии может отсутствовать — проверьте `git status` перед операциями.
- Сообщения коммитов: короткая императивная строка на русском, без префиксов
  (`добавить магазин апгрейдов`, `исправить сохранение прогресса`).
- PR: опишите изменение и затронутые файлы, приложите скриншот UI до/после для визуальных правок,
  убедитесь, что CI-джоба `build-clicker` зелёная.
- Не коммитьте `config.properties` с локальным прогрессом, `out/`, `classes/`, `dist/`.
