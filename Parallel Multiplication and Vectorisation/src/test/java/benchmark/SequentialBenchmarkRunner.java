package benchmark;

import control.MatrixGenerator;
import control.MatrixMultiplier;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SequentialBenchmarkRunner {
    @Param({"100", "200", "300", "400", "500"})
    public int MATRIX_SIZE;

    private int[][] matrixA;
    private int[][] matrixB;

    @Setup(Level.Trial)
    public void setup() {
        matrixA = MatrixGenerator.generateMatrix(MATRIX_SIZE, 777);
        matrixB = MatrixGenerator.generateMatrix(MATRIX_SIZE, 778);
    }

    @Benchmark
    public void multiplyLoopReordered(Blackhole blackhole) {
        blackhole.consume(MatrixMultiplier.multiplyLoopReordered(matrixA, matrixB));
    }
}
