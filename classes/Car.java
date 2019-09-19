package reflection.classes;

import reflection.annotation.Reflected;

@Reflected
public class Car {
    private String title;
    private double price;
    private String engine;

    public Car(String title, double price, String engine) {
        this.title = title;
        this.price = price;
        this.engine = engine;
    }

    public Car() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    @Override
    public String toString() {
        return "Car{" +
                "title='" + title + '\'' +
                ", price=" + price +
                ", engine='" + engine + '\'' +
                '}';
    }
}
