package MyGames;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Точка входа пакета MyGames: выбирает, какую из консольных игр запустить.
 * У самих игр метода main нет — они стартуют из конструктора, который вызывает play().
 */
public class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        // Scanner не закрываем: System.in не должен закрываться, а BufferedReader после игр
        // (со своим Scanner(System.in)) при закрытии утащил бы за собой стандартный ввод
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        while (true) {
            printMenu();
            String choice = readLine(in);
            if (choice == null) {
                return;
            }
            switch (choice.trim()) {
                case "1" -> start(new Game_rand(), in);
                case "2" -> start(new Game_kmn(), in);
                case "3" -> start(new Game_box(), in);
                case "0", "q", "exit" -> {
                    System.out.println("До встречи!");
                    return;
                }
                default -> System.out.println("Нет такой игры: " + choice);
            }
        }
    }

    private static String readLine(BufferedReader in) {
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("   Мои игры");
        System.out.println("========================================");
        System.out.println("   1 — Угадай число");
        System.out.println("   2 — Камень, ножницы, бумага");
        System.out.println("   3 — Угадай, в какой коробке Verity");
        System.out.println("   0 — Выход");
        System.out.println("========================================");
        System.out.print("Выбор: ");
    }

    private static void start(Basic_class game, BufferedReader in) {
        // игра уже отработала в конструкторе; предлагаем новую серию без выхода в меню
        while (true) {
            System.out.println();
            System.out.print("Начать новую серию? (да/нет): ");
            String answer = readLine(in);
            if (answer == null || !isYes(answer)) {
                System.out.println();
                System.out.println("Возврат в меню...");
                return;
            }
            game.playAgainSeries();
        }
    }

    private static boolean isYes(String answer) {
        String normalized = answer.trim().toLowerCase();
        return normalized.equals("да") || normalized.equals("д") || normalized.equals("yes") || normalized.equals("y");
    }
}
