package control;

import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public final class MatrixMultiplier {
    private static final int ROWS_PER_TASK = 4;

    public static int[][] multiplyLoopReordered(int[][] matrixA, int[][] matrixB) {
        int n = validateInputs(matrixA, matrixB);
        int[][] matrixC = new int[n][n];
        multiplyRows(matrixA, matrixB, matrixC, 0, n);
        return matrixC;
    }

    public static int[][] multiplyParallelLoopReordered(
            int[][] matrixA,
            int[][] matrixB,
            ForkJoinPool pool
    ) {
        int n = validateInputs(matrixA, matrixB);
        Objects.requireNonNull(pool, "ForkJoinPool must not be null");

        int[][] matrixC = new int[n][n];
        pool.invoke(new RowRangeTask(matrixA, matrixB, matrixC, 0, n));
        return matrixC;
    }

    private static void multiplyRows(
            int[][] matrixA,
            int[][] matrixB,
            int[][] matrixC,
            int rowStart,
            int rowEnd
    ) {
        int n = matrixA.length;
        for (int i = rowStart; i < rowEnd; i++) {
            int[] rowA = matrixA[i];
            int[] rowC = matrixC[i];

            for (int k = 0; k < n; k++) {
                int valueA = rowA[k];
                int[] rowB = matrixB[k];
                for (int j = 0; j < n; j++) {
                    rowC[j] += valueA * rowB[j];
                }
            }
        }
    }

    private static int validateInputs(int[][] matrixA, int[][] matrixB) {
        Objects.requireNonNull(matrixA, "Matrix A must not be null");
        Objects.requireNonNull(matrixB, "Matrix B must not be null");

        if (matrixA.length == 0 || matrixB.length != matrixA.length) {
            throw new IllegalArgumentException("Matrices must be non-empty and have the same dimensions");
        }

        int n = matrixA.length;
        for (int i = 0; i < n; i++) {
            if (matrixA[i] == null || matrixB[i] == null
                    || matrixA[i].length != n || matrixB[i].length != n) {
                throw new IllegalArgumentException("Both input matrices must be square n x n matrices");
            }
        }
        return n;
    }

    private static final class RowRangeTask extends RecursiveAction {
        private final int[][] matrixA;
        private final int[][] matrixB;
        private final int[][] matrixC;
        private final int rowStart;
        private final int rowEnd;

        private RowRangeTask(
                int[][] matrixA,
                int[][] matrixB,
                int[][] matrixC,
                int rowStart,
                int rowEnd
        ) {
            this.matrixA = matrixA;
            this.matrixB = matrixB;
            this.matrixC = matrixC;
            this.rowStart = rowStart;
            this.rowEnd = rowEnd;
        }

        @Override
        protected void compute() {
            if (rowEnd - rowStart <= ROWS_PER_TASK) {
                multiplyRows(matrixA, matrixB, matrixC, rowStart, rowEnd);
                return;
            }

            int middle = rowStart + (rowEnd - rowStart) / 2;
            invokeAll(
                    new RowRangeTask(matrixA, matrixB, matrixC, rowStart, middle),
                    new RowRangeTask(matrixA, matrixB, matrixC, middle, rowEnd)
            );
        }
    }
}
