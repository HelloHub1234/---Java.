import Package1.Person;

public class Test {
    public static void main(String[] args){
        // модификаторы доступа:
        // 1. public (доступ всем),
        // 2. private (в пределах одного класса),
        // 3. default (в пределах одного пакета),
        // 4. protected (в пределах одного пакета, доступны всем подклассам при наследовании,
        // даже если эти подклассы вне пакета).

        Person person1 = new Person();

    }
}
