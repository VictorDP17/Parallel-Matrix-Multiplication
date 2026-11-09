package benchmark;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.util.Statistics;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BenchmarkMain {
    private static final String SUMMARY_CSV = "results.csv";
    private static final String RAW_JSON = "jmh_raw.json";
    private static final int WARMUP_ITERATIONS = 1;
    private static final int MEASUREMENT_ITERATIONS = 5;

    public static void main(String[] args) throws Exception {
        boolean quick = List.of(args).contains("--quick");
        int measurementIterations = quick ? 2 : MEASUREMENT_ITERATIONS;

        printEnvironment();
        Options options = new OptionsBuilder()
                .include(".*(SequentialBenchmarkRunner|ParallelBenchmarkRunner).*\\..*")
                .mode(org.openjdk.jmh.annotations.Mode.AverageTime)
                .timeUnit(java.util.concurrent.TimeUnit.MILLISECONDS)
                .warmupIterations(WARMUP_ITERATIONS)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(measurementIterations)
                .measurementTime(TimeValue.seconds(1))
                .forks(1)
                .threads(1)
                .timeout(TimeValue.minutes(10))
                .addProfiler(GCProfiler.class)
                .result(RAW_JSON)
                .resultFormat(ResultFormatType.JSON)
                .build();

        Collection<RunResult> results = new Runner(options).run();
        saveSummary(results, measurementIterations);

        System.out.println("Summary CSV saved to " + SUMMARY_CSV);
        System.out.println("Raw JMH results saved to " + RAW_JSON);
        if (quick) {
            System.out.println("WARNING: --quick uses only two measurement iterations; do not use it for final reported results.");
        }
    }

    private static void printEnvironment() {
        System.out.println("Assignment 3 - Parallel Matrix Multiplication");
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("JVM: " + System.getProperty("java.vm.name"));
        System.out.println("Logical processors reported to this JVM: " + Runtime.getRuntime().availableProcessors());
        System.out.println("JMH harness threads: 1; worker parallelism is selected by THREAD_COUNT.");
    }

    private static void saveSummary(Collection<RunResult> results, int measurementIterations) throws IOException {
        List<BenchmarkRecord> records = new ArrayList<>();
        Map<Integer, Double> sequentialMedians = new HashMap<>();

        for (RunResult run : results) {
            String benchmarkName = run.getParams().getBenchmark();
            boolean sequential = benchmarkName.contains("SequentialBenchmarkRunner");
            int matrixSize = Integer.parseInt(run.getParams().getParam("MATRIX_SIZE"));
            int threadCount = sequential ? 1 : Integer.parseInt(run.getParams().getParam("THREAD_COUNT"));

            Statistics statistics = run.getPrimaryResult().getStatistics();
            double allocatedBytes = getAllocatedBytesPerOperation(run);
            BenchmarkRecord record = new BenchmarkRecord(
                    sequential ? "sequential_loop_reordered" : "parallel_loop_reordered",
                    matrixSize,
                    threadCount,
                    statistics.getMean(),
                    statistics.getPercentile(50),
                    statistics.getVariance(),
                    Math.sqrt(statistics.getVariance()),
                    allocatedBytes,
                    sequential
            );
            records.add(record);
            if (sequential) {
                sequentialMedians.put(matrixSize, record.medianTimeMs);
            }
        }

        records.sort(Comparator
                .comparingInt((BenchmarkRecord r) -> r.matrixSize)
                .thenComparing(r -> r.sequential ? 0 : 1)
                .thenComparingInt(r -> r.threads));

        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(SUMMARY_CSV), StandardCharsets.UTF_8)) {
            writer.write("method,matrix_size,threads,numeric_type,jmh_threads,warmup_iterations,measurement_iterations,"
                    + "mean_time_ms,median_time_ms,variance_time_ms2,stddev_time_ms,allocated_bytes_per_op,speedup,efficiency");
            writer.newLine();

            for (BenchmarkRecord record : records) {
                double speedup = record.sequential
                        ? 1.0
                        : sequentialMedians.get(record.matrixSize) / record.medianTimeMs;
                double efficiency = speedup / record.threads;

                writer.write(String.format(Locale.US,
                        "%s,%d,%d,int,1,%d,%d,%.9f,%.9f,%.9f,%.9f,%.3f,%.6f,%.6f",
                        record.method,
                        record.matrixSize,
                        record.threads,
                        WARMUP_ITERATIONS,
                        measurementIterations,
                        record.meanTimeMs,
                        record.medianTimeMs,
                        record.varianceTimeMs2,
                        record.stddevTimeMs,
                        record.allocatedBytesPerOp,
                        speedup,
                        efficiency));
                writer.newLine();
            }
        }
    }

    private static double getAllocatedBytesPerOperation(RunResult run) {
        Result<?> allocation = run.getSecondaryResults().get("gc.alloc.rate.norm");
        return allocation == null ? Double.NaN : allocation.getScore();
    }

    private record BenchmarkRecord(
            String method,
            int matrixSize,
            int threads,
            double meanTimeMs,
            double medianTimeMs,
            double varianceTimeMs2,
            double stddevTimeMs,
            double allocatedBytesPerOp,
            boolean sequential
    ) {
    }
}
