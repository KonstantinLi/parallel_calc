package algorithms;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.StringJoiner;

public class Algorithm {
    public static double maxSubVector(double[] A, int start, int end) {
        if (start > end || end > A.length) {
            throw new IllegalBoundException();
        }

        double max = A[start];
        for (int i = start + 1; i < end; i++) {
            if (A[i] > max) max = A[i];
        }

        return max;
    }

    public static double maxVector(double[] A) {
        return maxSubVector(A, 0, A.length);
    }

    public static double[] differenceSubVectors(double[] A, double[] B, int start, int end) {
        if (start > end || end - start > A.length || A.length != B.length) {
            throw new IllegalBoundException();
        }

        double[] C = new double[end - start];
        int index = 0;

        for (int i = start; i < end; i++) {
            C[index++] = A[i] - B[i];
        }

        return C;
    }

    public static double[] differenceVectors(double[] A, double[] B) {
        return differenceSubVectors(A, B, 0, A.length);
    }

    public static double[][] sumMatrices(double[][] MA, double[][] MB) {
        if (MA.length != MB.length
            || Arrays.stream(MA).count() != Arrays.stream(MB).count())
            throw new IllegalBoundException();

        double[][] MY = new double[MA.length][MA[0].length];

        for (int i = 0; i < MY.length; i++) {
            for (int j = 0; j < MY[i].length; j++) {
                MY[i][j] = MA[i][j] + MB[i][j];
            }
        }

        return MY;
    }

    public static double[][] differenceMatrices(double[][] MA, double[][] MB) {
        if (MA.length != MB.length
                || Arrays.stream(MA).count() != Arrays.stream(MB).count())
            throw new IllegalBoundException();

        double[][] MY = new double[MA.length][MA[0].length];

        for (int i = 0; i < MY.length; i++) {
            for (int j = 0; j < MY[i].length; j++) {
                MY[i][j] = MA[i][j] - MB[i][j];
            }
        }

        return MY;
    }

    public static double[] multiplySubVectorAndScalar(double[] A, double a, int start, int end) {
        double[] B = new double[end - start];
        if (start > end) throw new IllegalBoundException();

        int index = 0;
        for (int i = start; i < end; i++) {
            B[index++] = A[i] * a;
        }

        return B;
    }

    public static double[] multiplySubMatrixAndVector(double[][] MA, double[] A, int start, int end) {
        if (MA.length != A.length || start > end) {
            throw new IllegalBoundException();
        }

        double[] B = new double[end - start];
        int index = 0;

        for (int i = 0; i < A.length; i++) {
            for (int j = start; j < end; j++) {
                B[index++] = MA[i][j] * A[i];
            }
            index = 0;
        }

        return B;
    }

    public static double[][] multiplySubMatrixAndMatrix(double[][] MA, double[][] MB, int start, int end) {
        if (MA.length != MB[0].length
                || Arrays.stream(MA).count() != MB.length
                || start > end) {
            throw new IllegalBoundException();
        }

        double[][] MX = new double[MA.length][end - start];

        for (int i = 0; i < MA.length; i++) {
            int x = 0;
            for (int j = start; j < end; j++) {
                MX[i][x] = 0;
                for (int k = 0; k < MA.length; k++) {
                    MX[i][x] = MA[i][k] * MB[k][j];
                }
                x++;
            }
        }

        return MX;
    }

    public static void writeSubVector(double[] A, double[] B, int start, int end) {
        int index = 0;
        for (int i = start; i < end; i++) {
            A[i] = B[index++];
        }
    }

    public static void writeSubMatrix(double[][] MA, double[][] MB, int start, int end) {
        for (int i = 0; i < MB.length; i++) {
            int k = 0;
            for (int j = start; j < end; j++) {
                MA[i][j] = MB[i][k++];
            }
        }
    }

    public static void printVector(double[] A) {
        StringJoiner joiner = new StringJoiner(", ");
        for (int i = 0; i < A.length; i++) {
            joiner.add(String.valueOf(A[i]));
        }

        System.out.println(joiner);
    }

    public static void printMatrix(double[][] MA) {
        StringJoiner rowJoiner = new StringJoiner("\n");
        for (int i = 0; i < MA.length; i++) {
            StringJoiner columnJoiner = new StringJoiner(", ");
            for (int j = 0; j < MA[i].length; j++) {
                columnJoiner.add(String.valueOf(MA[i][j]));
            }
            rowJoiner.add(columnJoiner.toString());
        }

        System.out.println(rowJoiner);
    }
}
