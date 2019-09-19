package reflection.classes;

import reflection.annotation.Reflected;

@Reflected
public class Dog {
    private String name;
    private double weight;
    private double height;

    public Dog(String name, double weight, double height) {
        this.name = name;
        this.weight = weight;
        this.height = height;
    }

    public Dog() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "Dog{" +
                "name='" + name + '\'' +
                ", weight=" + weight +
                ", height=" + height +
                '}';
    }
}
