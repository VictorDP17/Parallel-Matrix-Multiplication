package control;

import java.util.Random;

public final class MatrixGenerator {

    public static int[][] generateMatrix(int n, int seed) {
        if (n <= 0) {
            throw new IllegalArgumentException("Matrix size must be positive");
        }

        Random random = new Random(seed);
        int[][] matrix = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = random.nextInt(100);
            }
        }
        return matrix;
    }
}
