package labs;

import com.google.common.util.concurrent.AtomicDouble;
import data.Generator;

import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.Lock;

import static algorithms.Algorithm.*;

public class RecursiveSecondAction extends RecursiveAction {
    private static final int THREADS_NUMBER = Runtime.getRuntime().availableProcessors();

    private final int thread_number;
    private final Lock D_LOCK;

    private double[][] MT;
    private double[] A, B, D;
    private AtomicDouble max_D;

    public RecursiveSecondAction(int thread_number, Lock d_LOCK) {
        this.thread_number = thread_number;
        D_LOCK = d_LOCK;
    }

    public void setVariables(double[][] MT, double[] A, double[] B, double[] D, AtomicDouble max_D) {
        this.MT = MT;
        this.A = A;
        this.B = B;
        this.D = D;
        this.max_D = max_D;
    }

    @Override
    protected void compute() {
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
        writeSubVector(A, A_i, start, end);

        if (thread_number < THREADS_NUMBER) {
            RecursiveSecondAction subAction = new RecursiveSecondAction(thread_number + 1, D_LOCK);
            subAction.setVariables(MT, A, B, D, max_D);
            subAction.invoke();
        }
    }
}
