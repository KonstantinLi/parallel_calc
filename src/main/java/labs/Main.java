package labs;

import com.google.common.util.concurrent.AtomicDouble;
import data.Generator;
import data.Parser;
import data.Type;

import java.util.*;

import static algorithms.Algorithm.*;

/**
 * Паралельні та розподілені обчислення
 * Лабораторна робота №1
 * Варіант 15
 * Завдання: 1) MA = MD*MT + MZ - ME*MM; 2) A = D*MT - max(D)*B
 * Ліненко Костянтин ІО-01
 * Дата 27.02.2023
 **/

public class Main {
    private static final String COMMENT = "1) MA = MD*MT + MZ - ME*MM; 2) A = D*MT - max(D)*B";

    private static double[][] MA, MD, ME, MM, MT, MZ;
    private static double[] A, B, D;
    private static AtomicDouble max_D;

    private static final Object MD_synchronized = new Object();
    private static final Object ME_synchronized = new Object();
    private static final Object MZ_synchronized = new Object();
    private static final Object D_synchronized = new Object();

    private static final int THREADS_NUMBER = Runtime.getRuntime().availableProcessors();

    public static void main(String[] args) {
        calculateFirst();
        calculateSecond();

        System.out.println("\nMA: ");
        printMatrix(MA);

        System.out.println("\nA: ");
        printVector(A);
    }

    private static void calculateFirst() {
        MA = new double[Generator.DEFAULT_SIZE][Generator.DEFAULT_SIZE];
        MD = Parser.parseMatrix("MD");
        ME = Parser.parseMatrix("ME");
        MM = Parser.parseMatrix("MM");
        MT = Parser.parseMatrix("MT");
        MZ = Parser.parseMatrix("MZ");

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < THREADS_NUMBER; i++) {
            int start = i * Generator.DEFAULT_SIZE / THREADS_NUMBER;
            int end = (i + 1) * Generator.DEFAULT_SIZE / THREADS_NUMBER;

            Thread thread = new Thread(runnable_MA(start, end));
            threads.add(thread);
        }

        long start = System.currentTimeMillis();

        awaitThreads(threads);

        System.out.println("MA calculated for " + (System.currentTimeMillis() - start) + " ms.");
    }

    private static void calculateSecond() {
        A = new double[Generator.DEFAULT_SIZE];
        B = Parser.parseVector("B");
        D = Parser.parseVector("D");

        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < THREADS_NUMBER; i++) {
            int start = i * Generator.DEFAULT_SIZE / THREADS_NUMBER;
            int end = (i + 1) * Generator.DEFAULT_SIZE / THREADS_NUMBER;

            Thread thread = new Thread(runnable_A(start, end));
            threads.add(thread);
        }

        long start = System.currentTimeMillis();

        awaitThreads(threads);

        System.out.println("A calculated for " + (System.currentTimeMillis() - start) + " ms.");
    }

    private static Runnable runnable_MA(int start, int end) {
        return () -> {
            double[][] MX_i;
            synchronized (MD_synchronized) {
                MX_i = multiplySubMatrixAndMatrix(MD, MT, start, end);
            }

            double[][] MY_i;
            synchronized (ME_synchronized) {
                MY_i = multiplySubMatrixAndMatrix(ME, MM, start, end);
            }

            double[][] MZ_i;
            synchronized (MZ_synchronized) {
                MZ_i = Arrays.stream(MZ)
                        .map(row -> Arrays.copyOfRange(row, start, end))
                        .toArray(double[][]::new);
            }

            double[][] MA_i = differenceMatrices(sumMatrices(MX_i, MZ_i), MY_i);
            writeSubMatrix(MA, MA_i, start, end);
        };
    }

    private static Runnable runnable_A(int start, int end) {
        return () -> {
            double[] C;
            synchronized (D_synchronized) {
                C = multiplySubMatrixAndVector(MT, D, start, end);
            }

            if (max_D == null) {
                synchronized (D_synchronized) {
                    max_D = new AtomicDouble(maxVector(D));
                }
            }
            double max_Di = max_D.get();

            double[] E = multiplySubVectorAndScalar(B, max_Di, start, end);
            double[] A_i = differenceVectors(C, E);

            writeSubVector(A, A_i, start, end);
        };
    }

    private static void awaitThreads(Collection<Thread> threads) {
        threads.forEach(Thread::run);
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private static void generateData() {
        Map<String, Type> variables = new HashMap<>() {{
            put("MD", Type.MATRIX);
            put("ME", Type.MATRIX);
            put("MM", Type.MATRIX);
            put("MT", Type.MATRIX);
            put("MZ", Type.MATRIX);
            put("B", Type.VECTOR);
            put("D", Type.VECTOR);
        }};
        Generator.write(variables, COMMENT);
    }
}
