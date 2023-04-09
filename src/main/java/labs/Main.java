package labs;

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
 * Лабораторна робота №6
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

    private static final BlockingQueue<Map.Entry<double[][], Bounds>> blockingQueueMA = new LinkedBlockingQueue<>();
    private static final BlockingQueue<Map.Entry<double[], Bounds>> blockingQueueA = new LinkedBlockingQueue<>();

    private static double[][] MA, MD, ME, MM, MT, MZ;
    private static double[] A, B, D;
    private static AtomicDouble max_D;

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

        long start = System.currentTimeMillis();

        List<RunnableMA> tasks = new ArrayList<>();

        for (int i = 1; i < THREADS_NUMBER; i++) {
            RunnableMA task = new RunnableMA(i, MD_LOCK, ME_LOCK, MZ_LOCK, blockingQueueMA);
            task.setVariables(MA, MD, ME, MM, MT, MZ);
            tasks.add(task);
        }

        tasks.forEach(executor::execute);

        try {
            int partResultNumber = 0;
            while (++partResultNumber < THREADS_NUMBER) {
                Map.Entry<double[][], Bounds> entry = blockingQueueMA.take();

                double[][] MA_i = entry.getKey();
                Bounds bounds = entry.getValue();

                writeSubMatrix(MA, MA_i, bounds.getStart(), bounds.getEnd());
            }

            System.out.println("MA calculated for " + (System.currentTimeMillis() - start) + " ms.");

            executor.shutdown();
            awaitTermination(executor);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static void calculateSecond(ExecutorService executor) {
        A = new double[Generator.DEFAULT_SIZE];
        B = Parser.parseVector("B");
        D = Parser.parseVector("D");

        long start = System.currentTimeMillis();

        List<RunnableA> tasks = new ArrayList<>();

        for (int i = 1; i < THREADS_NUMBER; i++) {
            RunnableA task = new RunnableA(i, D_LOCK, blockingQueueA);
            task.setVariables(MT, A, B, D, max_D);
            tasks.add(task);
        }

        tasks.forEach(executor::execute);

        try {
            int partResultNumber = 0;
            while (++partResultNumber < THREADS_NUMBER) {
                Map.Entry<double[], Bounds> entry = blockingQueueA.take();

                double[] A_i = entry.getKey();
                Bounds bounds = entry.getValue();

                writeSubVector(A, A_i, bounds.getStart(), bounds.getEnd());
            }

            System.out.println("A calculated for " + (System.currentTimeMillis() - start) + " ms.");

            executor.shutdown();
            awaitTermination(executor);
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

    private static ExecutorService getExecutor() {
        return Executors.newFixedThreadPool(THREADS_NUMBER);
    }
}
