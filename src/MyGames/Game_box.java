package MyGames;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Game_box extends Basic_class{
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
            fillBoxes();
            startNewGame();
            if(level > 10){
                System.out.println();
                System.out.println("========================================");
                System.out.println("   *** Игра окончена!!!!");
                System.out.println("========================================");
                break;
            }
            playAgain = askPlayAgain();
        }
        printFarewell();
    }
    protected void startNewGame(){
        gamesPlayed++;
        clearBoxes();
        setRandBox();
        inputUserValue();
        check();
        setBorder();
    }
    private void setBorder(){
        if(aBoolean) {
            if (level < 10) {
                count++;
                System.out.println();
                System.out.println(" *** Уровень повышается, следующий - " + count);
                border = (int) (count + ((double) count / 2.3 + 0.8));
            } else {
                level++;
            }
        }
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
            level++;
            System.out.println("Ваш счёт на данный момент: " + userScore);
        }else{
            aBoolean = false;
            System.out.println("Вы не угадали, начните следующий раунд :(");
        }
    }
    private void clearBoxes(){
        for (int i = 0; i < boxes.size(); i++) {
            boxes.set(i, "empty");
        }
    }
    private void fillBoxes(){
        boxes.clear();
        for (int i = 0; i < border; i++) {
            boxes.add("empty");
        }
    }
    protected boolean askPlayAgain() {
        return super.askPlayAgain();
    }
    protected void printFarewell() {
        totalUserScore += userScore;
        var percent = gamesPlayed > 0 ? (float) totalUserScore / gamesPlayed : 0.0f;

        System.out.println();
        System.out.println("========================================");
        System.out.println("   Спасибо за игру!");
        System.out.println("   Всего раундов сыграно: " + gamesPlayed);
        System.out.println("   Итог серии: " + totalUserScore);
        System.out.printf("   Процент угадывания: %.2f%n", percent);
        System.out.println("========================================");
    }
}
