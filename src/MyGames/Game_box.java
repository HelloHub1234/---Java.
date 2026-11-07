package MyGames;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game_box extends Basic_class{
    private static final int MAX_LEVEL = 10;

    private int border = 3;
    private int level = 1;
    private final String verity = "Хэй, привет, я Верити!!!";
    private int userValue;
    private final Random rnd = new Random();
    private List<String> boxes = new ArrayList<>();

    public Game_box(){
        gameName = "Угадай в какой коробке Verity";
        count = 1;
        aBoolean = false;
        play();
    }
    protected void play(){
        printTitle();
        playAgain = true;
        while (playAgain) {
            if (level > MAX_LEVEL) {
                System.out.println();
                System.out.println("========================================");
                System.out.println("   *** Игра окончена!!!!");
                System.out.println("========================================");
                break;
            }
            startNewGame();
            playAgain = askPlayAgain();
        }
        printFarewell();
    }
    protected void startNewGame(){
        gamesPlayed++;
        fillBoxes();
        setRandBox();
        inputUserValue();
        check();
        setBorder();
    }
    @Override
    protected void newSeries(){
        userScore = 0;
        level = 1;
        count = 1;
        // Верити снова в трёх коробках, как в самом начале
        border = 3;
    }
    private void setBorder(){
        if(!aBoolean) {
            return;
        }
        // Каждое угадывание повышает уровень; число коробок растёт с первого уровня
        level++;
        count = level;
        border = (int) (count + ((double) count / 2.3));
        System.out.println();
        System.out.println(" *** Уровень повышается: " + count + ", коробок в следующем раунде — " + border);
    }
    private int setRandValue(){
        return rnd.nextInt(border);
    }
    private void setRandBox(){
        boxes.set(setRandValue(), verity);
    }
    private void inputUserValue() {
        System.out.println();
        System.out.println("========================================");
        System.out.println();
        System.out.println("Раунд " + gamesPlayed);
        System.out.println();
        System.out.println("========================================");
        while (true) {
            System.out.print("Угадайте в какой коробке Верити (всего коробок - " + border + "): ");
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= 1 && value <= border) {
                    userValue = value - 1;
                    return;
                }
            } catch (NumberFormatException ignored) {
                // Некорректный ввод — запрашиваем повторно
            }
            System.out.println("Введите число от 1 до " + border + ".");
        }
    }
    private void check(){
        if (verity.equals(boxes.get(userValue))) {
            System.out.println(verity + " Вы угадали!!!");
            System.out.println("+1 verity");
            userScore++;
            aBoolean = true;
            System.out.println("Ваш счёт на данный момент: " + userScore);
        }else{
            aBoolean = false;
            // показываем, где на самом деле была Верити, иначе игрок ничего не узнаёт о ходе
            System.out.println("Вы не угадали. Верити была в коробке " + (boxes.indexOf(verity) + 1) + ".");
            System.out.println("Игра окончена — уровень не повышен. Ваш счёт: " + userScore);
        }
    }
    private void fillBoxes(){
        boxes.clear();
        for (int i = 0; i < border; i++) {
            boxes.add("empty");
        }
    }
    protected boolean askPlayAgain() {
        if (!aBoolean) {
            // проигрыш завершает текущую серию; вопрос про новую серию задаёт launcher
            System.out.println("Серия завершена.");
            return false;
        }
        return true;
    }
    protected void printFarewell() {
        totalUserScore += userScore;
        var percent = gamesPlayed > 0 ? (float) totalUserScore / gamesPlayed * 100 : 0.0f;

        System.out.println();
        System.out.println("========================================");
        System.out.println("   Спасибо за игру!");
        System.out.println("   Всего раундов сыграно: " + gamesPlayed);
        System.out.println("   Итог серии: " + totalUserScore);
        System.out.printf("   Процент угадывания: %.2f%%%n", percent);
        System.out.println("========================================");
    }
}
