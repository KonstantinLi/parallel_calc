package labs;

import algorithms.ExpressionComputeException;
import com.google.common.util.concurrent.AtomicDouble;
import data.Generator;
import data.Parser;
import data.Type;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static algorithms.Algorithm.*;

/**
 * Паралельні та розподілені обчислення
 * Лабораторна робота №4
 * Варіант 15
 * Завдання: 1) MA = MD*MT + MZ - ME*MM; 2) A = D*MT - max(D)*B
 * Ліненко Костянтин ІО-01
 * Дата 10.04.2023
 **/

public class Main {
    private static final String FIRST_EXPRESSION = "MA = MD*MT + MZ - ME*MM";
    private static final String SECOND_EXPRESSION = "A = D*MT - max(D)*B";

    private static double[][] MA, MD, ME, MM, MT, MZ;
    private static double[] A, B, D;
    private static AtomicDouble max_D;
    private static final int THREADS_NUMBER = Runtime.getRuntime().availableProcessors();

    private static final Lock MD_LOCK = new ReentrantLock(true);
    private static final Lock ME_LOCK = new ReentrantLock(true);
    private static final Lock MZ_LOCK = new ReentrantLock(true);
    private static final Lock D_LOCK = new ReentrantLock(true);

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

        Map<Callable<double[][]>, Bounds> tasks = new HashMap<>();


        for (int i = 0; i < THREADS_NUMBER; i++) {
            int start = i * Generator.DEFAULT_SIZE / THREADS_NUMBER;
            int end = (i + 1) * Generator.DEFAULT_SIZE / THREADS_NUMBER;

            tasks.put(callable_MA(start, end), new Bounds(start, end));
        }

        long start = System.currentTimeMillis();

        try {
            for (Callable<double[][]> task : tasks.keySet()) {
                Bounds bounds = tasks.get(task);

                Future<double[][]> future = executor.submit(task);
                double[][] MA_i = future.get(30, TimeUnit.SECONDS);

                writeSubMatrix(MA, MA_i, bounds.getStart(), bounds.getEnd());
            }

            System.out.println("MA calculated for " + (System.currentTimeMillis() - start) + " ms.");
        } catch (ExecutionException | InterruptedException | TimeoutException ex) {
            throw new ExpressionComputeException(FIRST_EXPRESSION);
        }

        executor.shutdown();
        try {
            awaitTermination(executor);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static void calculateSecond(ExecutorService executor) {
        A = new double[Generator.DEFAULT_SIZE];
        B = Parser.parseVector("B");
        D = Parser.parseVector("D");

        Map<Callable<double[]>, Bounds> tasks = new HashMap<>();

        for (int i = 0; i < THREADS_NUMBER; i++) {
            int start = i * Generator.DEFAULT_SIZE / THREADS_NUMBER;
            int end = (i + 1) * Generator.DEFAULT_SIZE / THREADS_NUMBER;

            tasks.put(callable_A(start, end), new Bounds(start, end));
        }

        long start = System.currentTimeMillis();

        try {
            for (Callable<double[]> task : tasks.keySet()) {
                Bounds bounds = tasks.get(task);

                Future<double[]> future = executor.submit(task);
                double[] A_i = future.get(30, TimeUnit.SECONDS);

                writeSubVector(A, A_i, bounds.getStart(), bounds.getEnd());
            }

            System.out.println("A calculated for " + (System.currentTimeMillis() - start) + " ms.");
        } catch (ExecutionException | InterruptedException | TimeoutException ex) {
            throw new ExpressionComputeException(FIRST_EXPRESSION);
        }

        executor.shutdown();
        try {
            awaitTermination(executor);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static Callable<double[][]> callable_MA(int start, int end) {
        return () -> {
            double[][] MX_i;
            double[][] MY_i;
            double[][] MZ_i;

            MD_LOCK.lock();
            try {
                MX_i = multiplySubMatrixAndMatrix(MD, MT, start, end);
            } finally {
                MD_LOCK.unlock();
            }

            ME_LOCK.lock();
            try {
                MY_i = multiplySubMatrixAndMatrix(ME, MM, start, end);
            } finally {
                ME_LOCK.unlock();
            }

            MZ_LOCK.lock();
            try {
                MZ_i = Arrays.stream(MZ)
                        .map(row -> Arrays.copyOfRange(row, start, end))
                        .toArray(double[][]::new);
            } finally {
                MZ_LOCK.unlock();
            }

            return differenceMatrices(sumMatrices(MX_i, MZ_i), MY_i);
        };
    }

    private static Callable<double[]> callable_A(int start, int end) {
        return () -> {
            double[] C;

            D_LOCK.lock();
            try {
                C = multiplySubMatrixAndVector(MT, D, start, end);
            } finally {
                D_LOCK.unlock();
            }

            if (max_D == null) {
                D_LOCK.lock();
                try {
                    max_D = new AtomicDouble(maxVector(D));
                } finally {
                    D_LOCK.unlock();
                }
            }

            double max_Di = max_D.get();
            double[] E = multiplySubVectorAndScalar(B, max_Di, start, end);

            return differenceVectors(C, E);
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
