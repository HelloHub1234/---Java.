package MyGames;

import java.util.Scanner;

/**
 * Точка входа пакета MyGames: выбирает, какую из консольных игр запустить.
 * У самих игр метода main нет — они стартуют из конструктора, который вызывает play().
 */
public class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                printMenu();
                if (!scanner.hasNextLine()) {
                    return;
                }
                String choice = scanner.nextLine().trim();
                switch (choice) {
                    case "1" -> start(new Game_rand());
                    case "2" -> start(new Game_kmn());
                    case "3" -> start(new Game_box());
                    case "0", "q", "exit" -> {
                        System.out.println("До встречи!");
                        return;
                    }
                    default -> System.out.println("Нет такой игры: " + choice);
                }
            }
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

    private static void start(Basic_class game) {
        // игра уже отработала в конструкторе, здесь только пауза перед возвратом в меню
        System.out.println();
        System.out.println("Возврат в меню...");
    }
}
