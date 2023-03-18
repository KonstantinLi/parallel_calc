package data;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Generator {
    private static final String FILE_PATH = "src/main/resources/app.properties";

    private static final double DEFAULT_MIN = 1.0, DEFAULT_MAX = 10_000.0;
    public static final int DEFAULT_SIZE = 100;
    private static final Random RANDOM = new Random();
    private static final Properties PROPERTIES = new Properties();

    static {
        try {
            File file = new File(FILE_PATH);
            if (!file.exists())
                file.createNewFile();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) {
        Map<String, Type> variables = new HashMap<>() {{
           put("MA", Type.MATRIX);
           put("MD", Type.MATRIX);
           put("ME", Type.MATRIX);
           put("MM", Type.MATRIX);
           put("MT", Type.MATRIX);
           put("MZ", Type.MATRIX);
           put("A", Type.VECTOR);
           put("B", Type.VECTOR);
           put("D", Type.VECTOR);
        }};

        write(variables, "MA = MD*MT + MZ - ME*MM\nA = D*MT - max(D)*B");
    }

    public static void write(Map<String, Type> variables, String comment) {
        try {
            for (String variableName : variables.keySet()) {
                Type type = variables.get(variableName);

                switch (type) {
                    case NUMBER -> {
                        double number = generateNumber();
                        PROPERTIES.setProperty(variableName, String.valueOf(number));
                    }
                    case VECTOR -> {
                        double[] array = generateArray(100, 50, 100);
                        PROPERTIES.setProperty(variableName, Arrays.toString(array));
                    }
                    case MATRIX -> {
                        double[][] matrix = generateMatrix(100, 1, 50);
                        PROPERTIES.setProperty(
                                variableName,
                                IntStream.range(0, matrix.length)
                                        .mapToObj(index -> Arrays.toString(matrix[index]))
                                        .collect(Collectors.joining(" ")));
                    }
                }
            }
            PROPERTIES.store(new FileOutputStream(FILE_PATH), comment);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static double generateNumber(double min, double max) {
        double value = RANDOM.nextDouble(min, max);
        return new BigDecimal(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static double generateNumber() {
        return generateNumber(DEFAULT_MIN, DEFAULT_MAX);
    }

    public static double[] generateArray(int size, double min, double max) {
        return IntStream.range(0, size)
                .mapToDouble(index -> generateNumber(min, max))
                .toArray();
    }

    public static double[] generateArray(int size) {
        return generateArray(size, DEFAULT_MIN, DEFAULT_MAX);
    }

    public static double[] generateArray() {
        return generateArray(DEFAULT_SIZE);
    }

    public static double[][] generateMatrix(int size, double min, double max) {
        return IntStream.range(0, size)
                .mapToObj(index -> generateArray(size, min, max))
                .toArray(double[][]::new);
    }

    public static double[][] generateMatrix(int size) {
        return generateMatrix(size, DEFAULT_MIN, DEFAULT_MAX);
    }

    public static double[][] generateMatrix() {
        return generateMatrix(DEFAULT_SIZE);
    }
}
