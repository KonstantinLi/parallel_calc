package algorithms;

public class IllegalBoundException extends RuntimeException {
    public IllegalBoundException() {
        super("Illegal bound parameters");
    }
}
