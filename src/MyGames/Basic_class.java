package MyGames;

import java.util.Scanner;

public class Basic_class {
    protected int count;

    protected boolean aBoolean;
    protected boolean playAgain;

    protected int userScore = 0;
    protected int totalUserScore = 0;
    protected int gamesPlayed = 0;

    protected static Scanner scanner = new Scanner(System.in);
    protected String answer;

    protected static String gameName;

    public Basic_class(Basic_class basicClass) {
        basicClass.play();
    }
    public Basic_class() {
    }
    protected void play(){
        printTitle();
        playAgain = true;
        while (playAgain) {
            startNewGame();
            playAgain = askPlayAgain();
        }
        printFarewell();
    }
    protected void startNewGame(){

    }
    protected void printTitle() {
        System.out.println("========================================");
        System.out.println("   Игра: " + gameName);
        System.out.println("========================================");
    }
    protected void finishGame() {
        totalUserScore += userScore;

        System.out.println("========================================");
        System.out.println("   Итог игры №" + gamesPlayed);
        System.out.println("========================================");
        System.out.println("Ваш счёт: " + userScore);
    }
    protected void printFarewell() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("   Спасибо за игру!");
        System.out.println("   Итог серии: " + totalUserScore);
        System.out.println("========================================");
    }

    /** Запускает ещё одну серию после проигрыша или завершения уровня. */
    public void playAgainSeries() {
        newSeries();
        playAgain = true;
        play();
    }

    /** Сброс состояния серии перед новым запуском; переопределяется играми со своим прогрессом. */
    protected void newSeries() {
    }
    protected boolean askPlayAgain() {
        System.out.print("Сыграть ещё раз? (да/нет): ");
        answer = scanner.nextLine().trim().toLowerCase();
        if (answer.equals("да") || answer.equals("yes") || answer.equals("y") || answer.equals("д")) {
            System.out.println();
            System.out.println("Начинаем новую игру...");
            return true;
        }
        return false;
    }
}
