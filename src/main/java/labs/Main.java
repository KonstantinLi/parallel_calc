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
 * Лабораторна робота №5
 * Варіант 15
 * Завдання: 1) MA = MD*MT + MZ - ME*MM; 2) A = D*MT - max(D)*B
 * Ліненко Костянтин ІО-01
 * Дата 10.04.2023
 **/

public class Main {
    private static final String FIRST_EXPRESSION = "MA = MD*MT + MZ - ME*MM";
    private static final String SECOND_EXPRESSION = "A = D*MT - max(D)*B";
    private static final int THREADS_NUMBER = Runtime.getRuntime().availableProcessors();
    private static final Lock MD_LOCK = new ReentrantLock(true);
    private static final Lock ME_LOCK = new ReentrantLock(true);
    private static final Lock MZ_LOCK = new ReentrantLock(true);
    private static final Lock D_LOCK = new ReentrantLock(true);

    private static double[][] MA, MD, ME, MM, MT, MZ;
    private static double[] A, B, D;
    private static AtomicDouble max_D;

    public static void main(String[] args) {
        calculateFirst(getForkJoinPool());
        calculateSecond(getForkJoinPool());

        System.out.println("\nMA: ");
        printMatrix(MA);

        System.out.println("\nA: ");
        printVector(A);
    }

    private static void calculateFirst(ForkJoinPool pool) {
        MA = new double[Generator.DEFAULT_SIZE][Generator.DEFAULT_SIZE];
        MD = Parser.parseMatrix("MD");
        ME = Parser.parseMatrix("ME");
        MM = Parser.parseMatrix("MM");
        MT = Parser.parseMatrix("MT");
        MZ = Parser.parseMatrix("MZ");

        long start = System.currentTimeMillis();

        RecursiveFirstAction firstAction = new RecursiveFirstAction(1, MD_LOCK, ME_LOCK, MZ_LOCK);
        firstAction.setVariables(MA, MD, ME, MM, MT, MZ);
        pool.invoke(firstAction);

        System.out.println("MA calculated for " + (System.currentTimeMillis() - start) + " ms.");

        pool.shutdown();
        try {
            awaitTermination(pool);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static void calculateSecond(ForkJoinPool pool) {
        A = new double[Generator.DEFAULT_SIZE];
        B = Parser.parseVector("B");
        D = Parser.parseVector("D");

        long start = System.currentTimeMillis();

        RecursiveSecondAction secondAction = new RecursiveSecondAction(1, D_LOCK);
        secondAction.setVariables(MT, A, B, D, max_D);
        pool.invoke(secondAction);

        System.out.println("A calculated for " + (System.currentTimeMillis() - start) + " ms.");

        pool.shutdown();
        try {
            awaitTermination(pool);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
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

    private static ForkJoinPool getForkJoinPool() {
        return new ForkJoinPool(
                THREADS_NUMBER,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                null,
                true);
    }
}
