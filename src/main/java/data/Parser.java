package data;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

public class Parser {
    private static final String FILE_PATH = "src/main/resources/app.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try {
            PROPERTIES.load(new FileInputStream(FILE_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        double[][] MA = parseMatrix("MA");
        double[] A = parseVector("A");
    }

    public static double parseNumber(String key) {
        String value = PROPERTIES.getProperty(key);
        return Double.parseDouble(value);
    }

    public static double[] parseVector(String key) {
        String[] values = PROPERTIES.getProperty(key)
                .replaceAll("[\\[\\]]", "")
                .split(", ");

        return Arrays.stream(values).mapToDouble(Double::parseDouble).toArray();
    }

    public static double[][] parseMatrix(String key) {
        String[] values = Arrays.stream(PROPERTIES.getProperty(key)
                .split("] \\["))
                .map(row -> row.replaceAll("[\\[\\]]", "")).toArray(String[]::new);

        return Arrays.stream(values)
                .map(str -> Arrays.stream(str.split(", "))
                        .mapToDouble(Double::parseDouble).toArray())
                .toArray(double[][]::new);
    }
}
