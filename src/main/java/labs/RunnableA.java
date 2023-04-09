package labs;

import com.google.common.util.concurrent.AtomicDouble;
import data.Generator;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import static algorithms.Algorithm.*;

public class RunnableA implements Runnable {
    private static final int THREADS_NUMBER = Runtime.getRuntime().availableProcessors();

    private final BlockingQueue<Map.Entry<double[], Bounds>> blockingQueue;
    private final int thread_number;
    private final Lock D_LOCK;
    private double[][] MT;
    private double[] A, B, D;
    private AtomicDouble max_D;

    public RunnableA(int thread_number, Lock d_LOCK, BlockingQueue<Map.Entry<double[], Bounds>> blockingQueue) {
        this.thread_number = thread_number;
        this.D_LOCK = d_LOCK;
        this.blockingQueue = blockingQueue;
    }

    public void setVariables(double[][] MT, double[] A, double[] B, double[] D, AtomicDouble max_D) {
        this.MT = MT;
        this.A = A;
        this.B = B;
        this.D = D;
        this.max_D = max_D;
    }

    @Override
    public void run() {
        int start = (thread_number - 1) * Generator.DEFAULT_SIZE / THREADS_NUMBER;
        int end = thread_number * Generator.DEFAULT_SIZE / THREADS_NUMBER;

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

        double[] A_i = differenceVectors(C, E);

        Map.Entry<double[], Bounds> entry = new AbstractMap.SimpleEntry<>(A_i, new Bounds(start, end));

        try {
            blockingQueue.offer(entry, 10, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }
}
