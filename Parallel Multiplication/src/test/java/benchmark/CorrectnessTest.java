package benchmark;

import control.MatrixGenerator;
import control.MatrixMultiplier;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;

public final class CorrectnessTest {

    public static void main(String[] args) {
        checkKnownProduct();
        checkIdentityAndZeroMatrices();
        checkSequentialAgainstParallel();
        System.out.println("All Assignment correctness checks passed.");
    }

    private static void checkKnownProduct() {
        int[][] a = {{1, 2}, {3, 4}};
        int[][] b = {{5, 6}, {7, 8}};
        int[][] expected = {{19, 22}, {43, 50}};
        assertMatrixEquals(expected, MatrixMultiplier.multiplyLoopReordered(a, b), "known product");

        for (int threads : new int[]{1, 2, 6}) {
            try (ForkJoinPool pool = new ForkJoinPool(threads)) {
                assertMatrixEquals(expected,
                        MatrixMultiplier.multiplyParallelLoopReordered(a, b, pool),
                        "known product with " + threads + " threads");
            }
        }
    }

    private static void checkIdentityAndZeroMatrices() {
        int[][] a = {{2, 0, 3}, {1, 5, 7}, {4, 2, 6}};
        int[][] identity = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        int[][] zero = new int[3][3];

        assertMatrixEquals(a, MatrixMultiplier.multiplyLoopReordered(a, identity), "A x I");
        assertMatrixEquals(zero, MatrixMultiplier.multiplyLoopReordered(a, zero), "A x 0");

        try (ForkJoinPool pool = new ForkJoinPool(4)) {
            assertMatrixEquals(a,
                    MatrixMultiplier.multiplyParallelLoopReordered(a, identity, pool),
                    "parallel A x I");
            assertMatrixEquals(zero,
                    MatrixMultiplier.multiplyParallelLoopReordered(a, zero, pool),
                    "parallel A x 0");
        }
    }

    private static void checkSequentialAgainstParallel() {
        int[][] a = MatrixGenerator.generateMatrix(47, 777);
        int[][] b = MatrixGenerator.generateMatrix(47, 778);
        int[][] expected = MatrixMultiplier.multiplyLoopReordered(a, b);

        for (int threads : new int[]{1, 2, 4, 6, 8, 12}) {
            try (ForkJoinPool pool = new ForkJoinPool(threads)) {
                int[][] actual = MatrixMultiplier.multiplyParallelLoopReordered(a, b, pool);
                assertMatrixEquals(expected, actual, "random matrix with " + threads + " threads");
            }
        }
    }

    private static void assertMatrixEquals(int[][] expected, int[][] actual, String description) {
        if (!Arrays.deepEquals(expected, actual)) {
            throw new AssertionError("Incorrect result for " + description);
        }
    }
}
