package algorithms;

public class ExpressionComputeException extends RuntimeException {
    public ExpressionComputeException(String expression) {
        super(String.format("Expression \"%s\" computing has been interrupted", expression));
    }
}
