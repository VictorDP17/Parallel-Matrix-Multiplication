from __future__ import annotations

import csv
from pathlib import Path
from collections import defaultdict

import matplotlib.pyplot as plt

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "results.csv"
PLOT_DIR = ROOT / "results" / "plots"


def load_rows():
    if not CSV_PATH.exists():
        raise FileNotFoundError(
            f"Could not find {CSV_PATH}. Run Assignment3BenchmarkMain first."
        )
    with CSV_PATH.open("r", newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    if not rows:
        raise ValueError("The results CSV has a header but no benchmark data rows.")
    for row in rows:
        for key in ("matrix_size", "threads"):
            row[key] = int(row[key])
        for key in ("median_time_ms", "mean_time_ms", "stddev_time_ms", "allocated_bytes_per_op", "speedup", "efficiency"):
            row[key] = float(row[key])
    return rows


def save_time_plot(rows):
    by_series = defaultdict(list)
    for row in rows:
        label = "Sequential" if row["method"] == "sequential_loop_reordered" else f"Parallel ({row['threads']} threads)"
        by_series[label].append((row["matrix_size"], row["median_time_ms"]))

    fig, ax = plt.subplots(figsize=(9, 5.5))
    for label, values in sorted(by_series.items()):
        values.sort()
        ax.plot([v[0] for v in values], [v[1] for v in values], marker="o", label=label)
    ax.set_title("Median execution time by matrix size")
    ax.set_xlabel("Matrix size n (n x n)")
    ax.set_ylabel("Median time (ms/op)")
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=8, ncol=2)
    fig.tight_layout()
    fig.savefig(PLOT_DIR / "execution_time.png", dpi=180)
    plt.close(fig)


def save_speedup_plot(rows):
    by_size = defaultdict(list)
    for row in rows:
        if row["method"] == "parallel_loop_reordered":
            by_size[row["matrix_size"]].append((row["threads"], row["speedup"]))

    fig, ax = plt.subplots(figsize=(8, 5.5))
    for size, values in sorted(by_size.items()):
        values.sort()
        ax.plot([v[0] for v in values], [v[1] for v in values], marker="o", label=f"n={size}")
    ax.axhline(1.0, linestyle="--", linewidth=1)
    ax.set_title("Parallel speedup by worker count")
    ax.set_xlabel("ForkJoinPool parallelism (threads)")
    ax.set_ylabel("Speedup (sequential median / parallel median)")
    ax.grid(True, alpha=0.3)
    ax.legend(title="Matrix size", fontsize=8)
    fig.tight_layout()
    fig.savefig(PLOT_DIR / "speedup.png", dpi=180)
    plt.close(fig)


def save_efficiency_plot(rows):
    by_size = defaultdict(list)
    for row in rows:
        if row["method"] == "parallel_loop_reordered":
            by_size[row["matrix_size"]].append((row["threads"], row["efficiency"]))

    fig, ax = plt.subplots(figsize=(8, 5.5))
    for size, values in sorted(by_size.items()):
        values.sort()
        ax.plot([v[0] for v in values], [v[1] for v in values], marker="o", label=f"n={size}")
    ax.axhline(1.0, linestyle="--", linewidth=1)
    ax.set_title("Parallel efficiency by worker count")
    ax.set_xlabel("ForkJoinPool parallelism (threads)")
    ax.set_ylabel("Efficiency (speedup / threads)")
    ax.grid(True, alpha=0.3)
    ax.legend(title="Matrix size", fontsize=8)
    fig.tight_layout()
    fig.savefig(PLOT_DIR / "efficiency.png", dpi=180)
    plt.close(fig)


def save_allocation_plot(rows):
    by_series = defaultdict(list)
    for row in rows:
        label = "Sequential" if row["method"] == "sequential_loop_reordered" else f"Parallel ({row['threads']} threads)"
        by_series[label].append((row["matrix_size"], row["allocated_bytes_per_op"]))

    fig, ax = plt.subplots(figsize=(9, 5.5))
    for label, values in sorted(by_series.items()):
        values.sort()
        ax.plot([v[0] for v in values], [v[1] for v in values], marker="o", label=label)
    ax.set_title("Allocated memory per multiplication")
    ax.set_xlabel("Matrix size n (n x n)")
    ax.set_ylabel("Allocated bytes per operation (B/op)")
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=8, ncol=2)
    fig.tight_layout()
    fig.savefig(PLOT_DIR / "allocated_memory.png", dpi=180)
    plt.close(fig)


def main():
    PLOT_DIR.mkdir(parents=True, exist_ok=True)
    rows = load_rows()
    save_time_plot(rows)
    save_speedup_plot(rows)
    save_efficiency_plot(rows)
    save_allocation_plot(rows)
    print(f"Created four figures in: {PLOT_DIR}")


if __name__ == "__main__":
    main()
