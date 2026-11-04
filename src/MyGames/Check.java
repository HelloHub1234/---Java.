package MyGames;

/**
 * Вспомогательный класс для определения исхода одного раунда игры
 * "Камень, ножницы, бумага".
 */
public class Check {

    /**
     * Сравнивает ход пользователя и ход бота.
     *
     * @param userItem ход пользователя ("камень", "ножницы" или "бумага")
     * @param botItem  ход бота ("камень", "ножницы" или "бумага")
     * @return 0 — ничья, 1 — победил пользователь, -1 — победил бот
     */
    public static int result(String userItem, String botItem) {
        // Неизвестные ходы недопустимы: пользователь и бот могут выбрать
        // только "камень", "ножницы" или "бумага".
        if (!isValid(userItem) || !isValid(botItem)) {
            throw new IllegalArgumentException("Неизвестный ход: " + userItem + " / " + botItem);
        }
        // Если оба выбрали одинаковое — ничья.
        if (userItem.equals(botItem)) {
            return 0;
        }
        // Иначе определяем победителя по правилам игры:
        // камень бьёт ножницы, ножницы бьют бумагу, бумага бьёт камень.
        return switch (userItem) {
            case "камень" -> botItem.equals("ножницы") ? 1 : -1;
            case "ножницы" -> botItem.equals("бумага") ? 1 : -1;
            case "бумага" -> botItem.equals("камень") ? 1 : -1;
            default -> throw new IllegalStateException("Недостижимо: ход уже проверен");
        };
    }

    private static boolean isValid(String item) {
        return item != null && (item.equals("камень") || item.equals("ножницы") || item.equals("бумага"));
    }
}