package MyGames;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Game_rand extends Basic_class {
    private static final int MAX_BORDER = 1_000_000;

    private int border;
    private int userValue;
    private int randValue;
    private final Random rnd = new Random();
    private final Set<Integer> previousGuesses = new HashSet<>();
    private int totalCount = 0;

    public Game_rand() {
        gameName = "Угадай число.";
        play();
    }

    private void inputBorder() {
        System.out.print("Угадывай число от 0 до x. Введи x (x > 0): ");
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                // nextInt(border + 1) на границе Integer.MAX_VALUE переполняется и падает
                border = Integer.parseInt(input);
                if (border > 0 && border < MAX_BORDER) {
                    return;
                }
            } catch (NumberFormatException ignored) {
                // нечисловой ввод — просим повторить ниже
            }
            System.out.print("Введите целое число от 1 до " + (MAX_BORDER - 1) + ": ");
        }
    }

    private void setRandValue() {
        randValue = rnd.nextInt(border + 1);
    }

    protected void startNewGame() {
        count = 1;
        aBoolean = false;
        gamesPlayed++;
        previousGuesses.clear();
        inputBorder();
        setRandValue();
        finalResult();
    }

    private void setUserValue() {
        System.out.println("Игра №" + gamesPlayed + ". Введите число: ");
        userValue = readGuess();
    }

    private int readInt() {
        while (true) {
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.print("Неверный ввод. Введите целое число: ");
            }
        }
    }

    private int readGuess() {
        while (true) {
            int guess = readInt();
            // загаданное лежит в [0, border]; ввод вне диапазона раньше попадал в previousGuesses
            // и бесконечно считался «уже вводился», не давая подсказок — отсекаем сразу
            if (guess < 0 || guess > border) {
                System.out.print("Число вне диапазона. Введите число от 0 до " + border + ": ");
                continue;
            }
            if (!previousGuesses.add(guess)) {
                System.out.print("Это число уже вводилось ранее. Введите другое число: ");
                continue;
            }
            return guess;
        }
    }

    private void finalResult() {
        setUserValue();
        aBoolean = isCorrect();
        while (!aBoolean) {
            System.out.println("Вы не угадали число. (Попытка " + count + ") Введите ещё раз: ");
            count++;
            userValue = readGuess();
            aBoolean = isCorrect();
        }
        System.out.println("Вы угадали число!!! Затрачено попыток: " + count);
        userScore++;
        totalCount += count;
    }

    private boolean isCorrect() {
        if (userValue == randValue) {
            return true;
        }
        System.out.println(userValue < randValue ? "Загаданное число больше." : "Загаданное число меньше.");
        return false;
    }

    protected void finishGame() {
        totalUserScore += userScore;
        // раньше процент считался как угаданные числа / попытки и легко превышал 100%
        float percent = totalCount > 0 ? (float) totalUserScore / gamesPlayed * 100 : 0.0f;
        System.out.println();
        System.out.println("========================================");
        System.out.println("   Число раундов " + gamesPlayed);
        System.out.println("========================================");
        System.out.println("Ваш счёт угаданных чисел (за все раунды): " + totalUserScore);
        System.out.println("Ваш счёт попыток (за все раунды): " + totalCount);
        System.out.printf("Процент угадывания: %.2f%%%n", percent);
    }

    protected boolean askPlayAgain() {
        return super.askPlayAgain();
    }
}
