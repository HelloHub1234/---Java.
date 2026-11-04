package MyGames;

import java.util.Random;

public class Game_kmn extends Basic_class{
    private final String[] items = {"камень", "ножницы", "бумага"};
    private String userItem;
    private int randItem;
    private int attempts = 3;

    private final Random rnd = new Random();
    private int botScore;
    private int totalBotScore;

    public Game_kmn() {
        count = 1;
        gameName = "Камень, ножницы, бумага.";
        aBoolean = false;

        play();
    }

    protected void play() {

        printTitle();
        playAgain = true;
        while (playAgain) {
            startNewGame();
            playRounds();
            finishGame();
            printSeriesScore();
            playAgain = askPlayAgain();
        }

        printFarewell();
    }

    protected void printTitle() {
        System.out.println("========================================");
        System.out.println("   Игра: " + gameName);
        System.out.println("========================================");
    }

    protected void startNewGame() {
        userScore = 0;
        botScore = 0;

        System.out.println();
        System.out.println("===== Игра №" + (gamesPlayed + 1) + " =====");

        askAttempts();
    }

    private void askAttempts() {
        aBoolean = false;
        System.out.print("Выберите количество попыток (1, 3 или 5): ");
        do {
            String input = scanner.nextLine().trim();
            if (input.equals("1") || input.equals("3") || input.equals("5")) {
                attempts = Integer.parseInt(input);
                aBoolean = true;
            } else {
                System.out.print("Введите 1, 3 или 5: ");
            }
        } while (!aBoolean);
    }

    private void playRounds() {
        System.out.println();

        for (int i = 0; i < attempts; i++) {
            count = i + 1;
            System.out.println("----------------------------------------");
            System.out.println("   Попытка " + count + " из " + attempts);
            System.out.println("----------------------------------------");
            InputItem();
            randItem = getRandValue();
            chec();
            if (i < attempts - 1) {
                System.out.println();
                System.out.println("Переходим к следующей попытке...");
                System.out.println();
            }
        }
    }

    protected void finishGame() {
        totalUserScore += userScore;
        totalBotScore += botScore;
        gamesPlayed++;

        System.out.println("========================================");
        System.out.println("   Итог игры №" + gamesPlayed);
        System.out.println("========================================");
        System.out.println("Ваш счёт: " + userScore);
        System.out.println("Счёт бота: " + botScore);
        if (userScore > botScore) {
            System.out.println(">>> Вы победили эту игру! <<<");
        } else if (botScore > userScore) {
            System.out.println(">>> Бот победил эту игру! <<<");
        } else {
            System.out.println(">>> Ничья в этой игре! <<<");
        }
    }

    protected void printSeriesScore() {
        System.out.println();
        System.out.println("=== Общий счёт по играм ===");
        System.out.println("Вы: " + totalUserScore + " | Бот: " + totalBotScore);
        if (totalUserScore > totalBotScore) {
            System.out.println("В серии лидируете Вы!");
        } else if (totalBotScore > totalUserScore) {
            System.out.println("В серии лидирует бот!");
        } else {
            System.out.println("В серии ничья!");
        }
    }

    protected void printFarewell() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("   Спасибо за игру!");
        System.out.println("   Итог серии: Вы " + totalUserScore + " : " + totalBotScore + " Бот");
        System.out.println("========================================");
    }

    private int getRandValue() {
        randItem = rnd.nextInt(items.length);
        return randItem;
    }

    private void InputItem() {
        aBoolean = false;
        System.out.print("Ваш ход (камень/ножницы/бумага): ");
        do {
            userItem = scanner.nextLine().trim().toLowerCase();
            if (userItem.equals("камень") || userItem.equals("ножницы") || userItem.equals("бумага")) {
                aBoolean = true;
            } else {
                System.out.print("Неверный ввод. Введите камень, ножницы или бумага: ");
            }
        } while (!aBoolean);
    }

    private void chec() {
        String botMove = items[randItem];
        System.out.println("Бот выбрал: " + botMove);
        int result = Check.result(userItem, botMove);
        if (result == 0) {
            System.out.println("Результат: Ничья.");
        } else if (result > 0) {
            System.out.println("Результат: Вы победили!!!");
            userScore++;
        } else {
            System.out.println("Результат: Победил бот!!!");
            botScore++;
        }
    }
}