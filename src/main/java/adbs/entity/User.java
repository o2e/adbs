package adbs.entity;

public class User {

    public final int id;

    public final String name;

    public final boolean isRunning;

    public User(int id, String name, boolean isRunning) {
        this.id = id;
        this.name = name;
        this.isRunning = isRunning;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isRunning=" + isRunning +
                '}';
    }
}
