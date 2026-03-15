#!/usr/bin/env python3
"""
plot_convergence.py — Generate Figure 9 for the Solar Energy manuscript.

Reads convergence CSV files exported by JazariFresnelSim TestOptimization
(menu option [8]) and produces the convergence plot.

Usage:
    # Option A: Use archived data (already in data/ directory)
    python scripts/plot_convergence.py

    # Option B: Generate fresh data first
    java -jar JazariFresnelSim.jar   # Select option [8], wait ~2.5 minutes
    python scripts/plot_convergence.py

Input files (searched in this order):
    1. ./convergence_SA.csv  (current directory — freshly generated)
    2. ./data/convergence_SA.csv  (archived in repo)

Output:
    fig_convergence.pdf
    fig_convergence.png

Requirements:
    pip install matplotlib pandas numpy

Authors: Yunus Demirtaş, Musa Ataş — Siirt University
Paper: "Rapid Optical–Thermal Design of Linear Fresnel Reflectors"
       Solar Energy (Elsevier), 2026
"""

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib
import os
import sys

matplotlib.rcParams['font.family'] = 'serif'
matplotlib.rcParams['font.size'] = 11
matplotlib.rcParams['axes.linewidth'] = 0.8


def find_csv(name):
    """Search for CSV file in current dir, then data/ dir, then parent dirs."""
    candidates = [
        name,
        os.path.join("data", name),
        os.path.join("..", name),
        os.path.join("..", "data", name),
    ]
    for path in candidates:
        if os.path.exists(path):
            return path
    print(f"ERROR: {name} not found in any of: {candidates}")
    print("Run JazariFresnelSim option [8] first to generate convergence data.")
    sys.exit(1)


def load_convergence(filepath):
    """Load convergence CSV and return a matrix (runs × iterations)."""
    print(f"  Loading: {filepath}")
    df = pd.read_csv(filepath)
    runs = sorted(df['run'].unique())
    max_iter = df.groupby('run')['iteration'].max().max() + 1

    matrix = np.full((len(runs), max_iter), np.nan)
    for r in runs:
        run_data = df[df['run'] == r].sort_values('iteration')
        vals = run_data['best_fitness'].values
        matrix[r, :len(vals)] = vals
        if len(vals) < max_iter:
            matrix[r, len(vals):] = vals[-1]

    return matrix


def main():
    print("=" * 60)
    print("  JazariFresnelSim — Convergence Plot Generator (Fig. 9)")
    print("=" * 60)
    print()

    # Load data
    sa_data = load_convergence(find_csv("convergence_SA.csv"))
    ga_data = load_convergence(find_csv("convergence_GA.csv"))
    pso_data = load_convergence(find_csv("convergence_PSO.csv"))

    print(f"\n  SA:  {sa_data.shape[0]} runs x {sa_data.shape[1]} iterations")
    print(f"  GA:  {ga_data.shape[0]} runs x {ga_data.shape[1]} iterations")
    print(f"  PSO: {pso_data.shape[0]} runs x {pso_data.shape[1]} iterations")

    # Normalize iteration axes to [0, 1]
    sa_x = np.linspace(0, 1, sa_data.shape[1])
    ga_x = np.linspace(0, 1, ga_data.shape[1])
    pso_x = np.linspace(0, 1, pso_data.shape[1])

    # Compute statistics
    sa_med, sa_min, sa_max = np.median(sa_data, 0), np.min(sa_data, 0), np.max(sa_data, 0)
    ga_med, ga_min, ga_max = np.median(ga_data, 0), np.min(ga_data, 0), np.max(ga_data, 0)
    pso_med, pso_min, pso_max = np.median(pso_data, 0), np.min(pso_data, 0), np.max(pso_data, 0)

    # Final values for annotations
    sa_finals = sa_data[:, -1]
    ga_finals = ga_data[:, -1]
    pso_finals = pso_data[:, -1]

    # Print summary table
    print(f"\n{'':=<60}")
    print(f"  Summary Statistics (Table 10 verification)")
    print(f"{'':=<60}")
    print(f"  {'Algorithm':<6} {'Best':>10} {'Mean':>10} {'Std':>10} {'Median':>10}")
    print(f"  {'-'*46}")
    for name, finals in [("SA", sa_finals), ("GA", ga_finals), ("PSO", pso_finals)]:
        print(f"  {name:<6} {np.max(finals):>10.1f} {np.mean(finals):>10.1f} "
              f"{np.std(finals):>10.1f} {np.median(finals):>10.1f}")
    print()

    # --- Plot ---
    fig, ax = plt.subplots(1, 1, figsize=(8, 5.5))

    # SA - dashed (red)
    ax.fill_between(sa_x, sa_min, sa_max, alpha=0.12, color='#c0392b')
    ax.plot(sa_x, sa_med, color='#c0392b', linewidth=2.2, linestyle='--',
            label=f'SA (median, $n$={sa_data.shape[0]})')

    # GA - solid (green)
    ax.fill_between(ga_x, ga_min, ga_max, alpha=0.12, color='#27ae60')
    ax.plot(ga_x, ga_med, color='#27ae60', linewidth=2.2, linestyle='-',
            label=f'GA (median, $n$={ga_data.shape[0]})')

    # PSO - dotted (blue)
    ax.fill_between(pso_x, pso_min, pso_max, alpha=0.12, color='#2980b9')
    ax.plot(pso_x, pso_med, color='#2980b9', linewidth=2.5, linestyle=':',
            label=f'PSO (median, $n$={pso_data.shape[0]})')

    ax.set_xlabel('Normalized iteration progress', fontsize=12)
    ax.set_ylabel('Best fitness (kW/m$^2$)', fontsize=12)
    ax.set_xlim(0, 1.02)
    ax.legend(loc='lower right', fontsize=10, framealpha=0.95, edgecolor='0.7')
    ax.grid(True, alpha=0.25, linewidth=0.5)
    ax.tick_params(direction='in', top=True, right=True)

    # GA annotation - most consistent (lowest std)
    ax.annotate(
        f'GA: $\\sigma$ = {np.std(ga_finals):.1f}\n(most consistent)',
        xy=(0.88, ga_med[int(0.88 * len(ga_med))]),
        xytext=(0.15, np.max(ga_max) * 1.01),
        fontsize=9.5, color='#27ae60', fontweight='bold',
        arrowprops=dict(arrowstyle='->', color='#27ae60', lw=1.3,
                        connectionstyle='arc3,rad=-0.15'),
        bbox=dict(boxstyle='round,pad=0.3', facecolor='white',
                  edgecolor='#27ae60', alpha=0.9))

    # PSO annotation - highest single-run yield
    pso_idx = int(0.75 * len(pso_med))
    ax.annotate(
        f'PSO best: {np.max(pso_finals):.1f} kW/m$^2$',
        xy=(0.75, pso_med[pso_idx]),
        xytext=(0.50, np.median(sa_finals) * 0.82),
        fontsize=9.5, color='#2980b9', fontweight='bold',
        arrowprops=dict(arrowstyle='->', color='#2980b9', lw=1.3,
                        connectionstyle='arc3,rad=0.25'),
        bbox=dict(boxstyle='round,pad=0.3', facecolor='white',
                  edgecolor='#2980b9', alpha=0.9))

    # SA annotation - local optima
    sa_idx = int(0.45 * len(sa_med))
    ax.annotate(
        f'SA: $\\sigma$ = {np.std(sa_finals):.1f}\n(local optima)',
        xy=(0.45, sa_med[sa_idx]),
        xytext=(0.08, sa_med[sa_idx] * 0.72),
        fontsize=9.5, color='#c0392b',
        arrowprops=dict(arrowstyle='->', color='#c0392b', lw=1.3,
                        connectionstyle='arc3,rad=0.15'),
        bbox=dict(boxstyle='round,pad=0.3', facecolor='white',
                  edgecolor='#c0392b', alpha=0.9))

    plt.tight_layout()

    # Save
    pdf_path = 'fig_convergence.pdf'
    png_path = 'fig_convergence.png'
    plt.savefig(pdf_path, dpi=300, bbox_inches='tight')
    plt.savefig(png_path, dpi=200, bbox_inches='tight')

    print(f"  Figure saved: {pdf_path}")
    print(f"  Figure saved: {png_path}")
    print(f"\n  Done! Copy fig_convergence.pdf to your LaTeX figs/ directory.")
    print()


if __name__ == "__main__":
    main()
