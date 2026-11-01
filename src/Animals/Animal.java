package Animals;

public class Animal {
    protected int age = 0;
    protected String name;
    protected static String basicName;

    public Animal (int age, String name){
        this.age = age;
        this.name = name;
    }

    public void eat(Animal animal){
        System.out.println(animal.name + " " + basicName + " ест.");
    }
}
