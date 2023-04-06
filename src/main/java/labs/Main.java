package labs;

import algorithms.ExpressionComputeException;
import com.google.common.util.concurrent.AtomicDouble;
import data.Generator;
import data.Parser;
import data.Type;

import java.util.*;
import java.util.concurrent.*;

import static algorithms.Algorithm.*;

/**
 * Паралельні та розподілені обчислення
 * Лабораторна робота №3
 * Варіант 15
 * Завдання: 1) MA = MD*MT + MZ - ME*MM; 2) A = D*MT - max(D)*B
 * Ліненко Костянтин ІО-01
 * Дата 06.04.2023
 **/

public class Main {
    private static final String FIRST_EXPRESSION = "MA = MD*MT + MZ - ME*MM";
    private static final String SECOND_EXPRESSION = "A = D*MT - max(D)*B";

    private static double[][] MA, MD, ME, MM, MT, MZ;
    private static double[] A, B, D;
    private static AtomicDouble max_D;
    private static final int THREADS_NUMBER = Runtime.getRuntime().availableProcessors();
    private static final Semaphore MD_SEMAPHORE = new Semaphore(1);
    private static final Semaphore ME_SEMAPHORE = new Semaphore(1);
    private static final Semaphore MZ_SEMAPHORE = new Semaphore(1);
    private static final Semaphore D_SEMAPHORE = new Semaphore(1);

    public static void main(String[] args) {
        calculateFirst(getExecutor());
        calculateSecond(getExecutor());

        System.out.println("\nMA: ");
        printMatrix(MA);

        System.out.println("\nA: ");
        printVector(A);
    }

    private static void calculateFirst(ExecutorService executor) {
        MA = new double[Generator.DEFAULT_SIZE][Generator.DEFAULT_SIZE];
        MD = Parser.parseMatrix("MD");
        ME = Parser.parseMatrix("ME");
        MM = Parser.parseMatrix("MM");
        MT = Parser.parseMatrix("MT");
        MZ = Parser.parseMatrix("MZ");

        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < THREADS_NUMBER; i++) {
            int start = i * Generator.DEFAULT_SIZE / THREADS_NUMBER;
            int end = (i + 1) * Generator.DEFAULT_SIZE / THREADS_NUMBER;
            tasks.add(runnable_MA(start, end));
        }

        long start = System.currentTimeMillis();

        tasks.forEach(executor::execute);
        executor.shutdown();

        try {
            awaitTermination(executor);
            System.out.println("MA calculated for " + (System.currentTimeMillis() - start) + " ms.");
        } catch (InterruptedException ex) {
            throw new ExpressionComputeException(FIRST_EXPRESSION);
        }
    }

    private static void calculateSecond(ExecutorService executor) {
        A = new double[Generator.DEFAULT_SIZE];
        B = Parser.parseVector("B");
        D = Parser.parseVector("D");

        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < THREADS_NUMBER; i++) {
            int start = i * Generator.DEFAULT_SIZE / THREADS_NUMBER;
            int end = (i + 1) * Generator.DEFAULT_SIZE / THREADS_NUMBER;
            tasks.add(runnable_A(start, end));
        }

        long start = System.currentTimeMillis();

        tasks.forEach(executor::execute);
        executor.shutdown();

        try {
            awaitTermination(executor);
            System.out.println("A calculated for " + (System.currentTimeMillis() - start) + " ms.");
        } catch (InterruptedException ex) {
            throw new ExpressionComputeException(SECOND_EXPRESSION);
        }
    }

    private static Runnable runnable_MA(int start, int end) {
        return () -> {
            try {
                MD_SEMAPHORE.acquire();
                double[][] MX_i = multiplySubMatrixAndMatrix(MD, MT, start, end);
                MD_SEMAPHORE.release();

                ME_SEMAPHORE.acquire();
                double[][]MY_i = multiplySubMatrixAndMatrix(ME, MM, start, end);
                ME_SEMAPHORE.release();

                MZ_SEMAPHORE.acquire();
                double[][] MZ_i = Arrays.stream(MZ)
                            .map(row -> Arrays.copyOfRange(row, start, end))
                            .toArray(double[][]::new);
                MZ_SEMAPHORE.release();

                double[][] MA_i = differenceMatrices(sumMatrices(MX_i, MZ_i), MY_i);
                writeSubMatrix(MA, MA_i, start, end);

            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private static Runnable runnable_A(int start, int end) {
        return () -> {
            try {
                D_SEMAPHORE.acquire();
                double[] C = multiplySubMatrixAndVector(MT, D, start, end);
                D_SEMAPHORE.release();

                if (max_D == null) {
                    D_SEMAPHORE.acquire();
                    max_D = new AtomicDouble(maxVector(D));
                    D_SEMAPHORE.release();
                }
                double max_Di = max_D.get();

                double[] E = multiplySubVectorAndScalar(B, max_Di, start, end);
                double[] A_i = differenceVectors(C, E);

                writeSubVector(A, A_i, start, end);

            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    private static void awaitTermination(ExecutorService executor) throws InterruptedException {
        if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }
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
        Generator.write(variables, FIRST_EXPRESSION + "; " + SECOND_EXPRESSION);
    }

    private static ExecutorService getExecutor() {
        return Executors.newFixedThreadPool(THREADS_NUMBER);
    }
}
