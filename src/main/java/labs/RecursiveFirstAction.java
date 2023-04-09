package labs;

import data.Generator;

import java.util.Arrays;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.locks.Lock;

import static algorithms.Algorithm.*;
import static algorithms.Algorithm.writeSubMatrix;

public class RecursiveFirstAction extends RecursiveAction {
    private static final int THREADS_NUMBER = Runtime.getRuntime().availableProcessors();

    private final int thread_number;
    private final Lock MD_LOCK;
    private final Lock ME_LOCK;
    private final Lock MZ_LOCK;

    private double[][] MA, MD, ME, MM, MT, MZ;

    public RecursiveFirstAction(int thread_number, Lock MD_LOCK, Lock ME_LOCK, Lock MZ_LOCK) {
        this.thread_number = thread_number;
        this.MD_LOCK = MD_LOCK;
        this.ME_LOCK = ME_LOCK;
        this.MZ_LOCK = MZ_LOCK;
    }

    public void setVariables(double[][] MA, double[][] MD, double[][] ME, double[][] MM, double[][] MT, double[][] MZ) {
        this.MA = MA;
        this.MD = MD;
        this.ME = ME;
        this.MM = MM;
        this.MT = MT;
        this.MZ = MZ;
    }

    @Override
    protected void compute() {
        int start = (thread_number - 1) * Generator.DEFAULT_SIZE / THREADS_NUMBER;
        int end = thread_number * Generator.DEFAULT_SIZE / THREADS_NUMBER;

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

        double[][] MA_i = differenceMatrices(sumMatrices(MX_i, MZ_i), MY_i);
        writeSubMatrix(MA, MA_i, start, end);

        if (thread_number < THREADS_NUMBER) {
            RecursiveFirstAction subAction = new RecursiveFirstAction(thread_number + 1, MD_LOCK, ME_LOCK, MZ_LOCK);
            subAction.setVariables(MA, MD, ME, MM, MT, MZ);
            subAction.invoke();
        }
    }
}
