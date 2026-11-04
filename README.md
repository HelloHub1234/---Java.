# Java Games & Clicker

Учебный Java-проект: кликер-игра на Swing и набор консольных мини-игр.
Внешних зависимостей нет — только JDK 21 (Swing/AWT и стандартная библиотека), система сборки
не используется, компиляция выполняется напрямую через `javac`.

## Что внутри

| Модуль | Описание | Точка входа |
|---|---|---|
| `WindowApps` | Кликер `Тестовый кликкер`: клики, магазин прокачек, автокликер, 4 темы, 5 языков | `WindowApps.clicker.Clicker` |
| `MyGames` | Консольные игры: «Угадай число», «Камень, ножницы, бумага», «Угадай, в какой коробке Verity» | `MyGames.Launcher` |

## Кликер (`WindowApps`)

Нажимайте «Нажми», копите клики и покупайте прокачки в магазине:

- **+1 клик за нажатие** — увеличивает доход с одного нажатия;
- **Автокликер** — кликает сам, каждый следующий уровень ускоряет таймер (раз в секунду на уровень);
- **Множитель** — умножает клики за действие.

Есть сброс прогресса, ручное сохранение, окно настроек (язык и тема), 4 палитры оформления
(стандартная, киберпанк, лесная, солнечная) и локализация на русский, английский, немецкий,
испанский и французский.

### Сборка и запуск

```powershell
New-Item -ItemType Directory -Force -Path classes | Out-Null
javac -encoding UTF-8 -d classes src/WindowApps/Basic.java (Get-ChildItem src/WindowApps/clicker/*.java)
Copy-Item src/WindowApps/messages*.properties classes/WindowApps/
java -cp classes WindowApps.clicker.Clicker
```

`-encoding UTF-8` обязателен: исходники и ресурсы содержат кириллицу. Копировать
`messages*.properties` в `classes` нужно обязательно — без них интерфейс покажет ключи
ресурсов вместо текста.

### Где лежит прогресс

Бинарный файл `clicker.sav` в `%APPDATA%\Clicker\` (на не-Windows — `~/.clicker`):
подпись HMAC-SHA256 + шифрование AES/GCM. Старый `config.properties` читается один раз
как миграция и после переноса удаляется.

## Мини-игры (`MyGames`)

```powershell
New-Item -ItemType Directory -Force -Path classes | Out-Null
javac -encoding UTF-8 -d classes (Get-ChildItem src/MyGames/*.java)
java -cp classes MyGames.Launcher
```

Лончер показывает меню выбора игры. Общая логика (заголовок, счёт, вопрос о повторе)
вынесена в `Basic_class`, сами игры — `Game_rand`, `Game_kmn`, `Game_box`.
Подробнее — в [`src/MyGames/README.md`](src/MyGames/README.md).

## Структура

```
src/
  Main.java                     # пустая заглушка, в сборку не входит
  WindowApps/                   # общий каркас UI + кликер
    Basic.java                  # базовое окно: темы, кнопки, частицы, actions
    Theme.java                  # палитры (STANDARD, CYBERPUNK, FOREST, SOLAR)
    Text.java                   # i18n через ResourceBundle
    ProgressSnapshot.java       # снимок состояния прогресса
    ProgressStorage.java        # бинарное хранилище прогресса
    messages*.properties        # локали: ru (default), en, de, es, fr
    clicker/                    # приложение «Кликер»
  MyGames/                      # консольные игры
.github/workflows/build.yml     # CI: сборка jar и app-image для кликера
AGENTS.md                       # соглашения для контрибьюторов и AI-агентов
```

## CI и релизы

GitHub Actions (`windows-latest`, JDK 21) собирает кликер: `javac` → `jar` → `jpackage --type app-image`
→ артефакт `dist/Clicker-windows.zip`. Пуш тега `v*` дополнительно публикует релиз.

## Соглашения

Стиль кода, правила локализации и работы с прогрессом описаны в [AGENTS.md](AGENTS.md).
Коротко: 4 пробела, отступы без табов, все UI-строки — через `Text.get(...)` с ключами во всех
пяти локалях, работа со Swing — только в EDT, сообщения коммитов — императивная строка на русском.
