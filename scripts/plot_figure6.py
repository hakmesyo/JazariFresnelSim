#!/usr/bin/env python3
"""
plot_figure6.py — Figure 6: Inter-mirror Shading Loss vs Spacing-to-Width Ratio

Reads spacing_sweep.csv exported by JazariFresnelSim TestOptimization
menu option [5] and produces Figure 6 for the Solar Energy manuscript.

Design Rule 1: p/w > 2.5 → inter-mirror shading loss < 2%

Usage:
    java -jar JazariFresnelSim.jar   # Select option [5]
    python scripts/plot_figure6.py

Input:  spacing_sweep.csv  (or data/spacing_sweep.csv)
Output: fig_spacing_sweep.pdf
        fig_spacing_sweep.png

CSV columns:
    spacing_cm, cosine_efficiency_pct, shading_loss_pct, pw_ratio

Authors: Yunus Demirtas, Musa Atas — Siirt University
Paper: "Rapid Optical-Thermal Design of Linear Fresnel Reflectors:
        An Open-Source Analytical Framework and Dimensionless Sizing Rules"
       Solar Energy (Elsevier), 2026
"""

import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import os, sys

# ── Style ────────────────────────────────────────────────────────────────────
matplotlib.rcParams['font.family']    = 'serif'
matplotlib.rcParams['font.size']      = 11
matplotlib.rcParams['axes.linewidth'] = 0.8

# ── Find CSV ─────────────────────────────────────────────────────────────────
def find_csv(name):
    for path in [name,
                 os.path.join('data', name),
                 os.path.join('..', name),
                 os.path.join('..', 'data', name)]:
        if os.path.exists(path):
            return path
    print(f"ERROR: {name} not found.")
    print("Run JazariFresnelSim option [5] first.")
    sys.exit(1)

# ── Load ─────────────────────────────────────────────────────────────────────
csv = find_csv('spacing_sweep.csv')
print(f"Loading: {csv}")
data = np.genfromtxt(csv, delimiter=',', skip_header=1)

spacing  = data[:, 0]   # cm
cosine   = data[:, 1]   # %
shading  = data[:, 2]   # %
pw_ratio = data[:, 3]   # p/w

# Design Rule 1 threshold
pw_threshold  = 3.0
shad_threshold = 2.0   # %

# Find shading value at p/w = 2.5
idx_25 = np.argmin(np.abs(pw_ratio - pw_threshold))
shad_at_threshold = shading[idx_25]

print(f"  p/w = {pw_threshold} → p = {spacing[idx_25]:.0f} cm → "
      f"shading = {shad_at_threshold:.1f}%")
print(f"  Design Rule 1: p/w > {pw_threshold} → shading < {shad_threshold}%")

# ── Plot ─────────────────────────────────────────────────────────────────────
fig, ax = plt.subplots(figsize=(7.5, 5))

# Shading loss curve
ax.plot(pw_ratio, shading,
        color='#F44336', linewidth=2.4,
        marker='s', markersize=6,
        label='Shading loss (daily average, June 21)')

# Design Rule 1 threshold lines
ax.axvline(pw_threshold, color='#555555', linestyle='--',
           linewidth=1.3, alpha=0.85, label=f'$p/w = {pw_threshold}$ (Design Rule 1)')
ax.axhline(shad_threshold, color='#888888', linestyle=':',
           linewidth=1.0, alpha=0.8)

# Shade the "acceptable" region (p/w > 2.5, shading < 2%)
ax.fill_betweenx([0, shad_threshold],
                 pw_threshold, pw_ratio[-1] + 0.1,
                 alpha=0.08, color='#4CAF50',
                 label=r'Shading $< 2\%$ region')

# Annotate threshold point
ax.annotate(
    f'$p/w = {pw_threshold}$\nShading $\\approx$ {shad_at_threshold:.1f}%',
    xy=(pw_threshold, shad_at_threshold),
    xytext=(pw_threshold + 0.25, shad_at_threshold + 4),
    fontsize=9.5, color='#555555',
    arrowprops=dict(arrowstyle='->', color='#555555', lw=1.2,
                    connectionstyle='arc3,rad=-0.15'),
    bbox=dict(boxstyle='round,pad=0.3', facecolor='white',
              edgecolor='#AAAAAA', alpha=0.9))

# Annotate 2% line
ax.text(pw_ratio[-1] + 0.05, shad_threshold + 0.3,
        '2%', fontsize=9, color='#888888', va='bottom')

# Labels and formatting
ax.set_xlabel(r'Spacing-to-width ratio $p/w$', fontsize=12)
ax.set_ylabel(r'Inter-mirror shading loss (%)', fontsize=12)
ax.set_xlim(pw_ratio[0] - 0.1, pw_ratio[-1] + 0.2)
ax.set_ylim(0, max(shading) + 3)

# Secondary x-axis showing actual spacing in cm
ax2 = ax.twiny()
ax2.set_xlim(ax.get_xlim())
pw_ticks = pw_ratio
ax2.set_xticks(pw_ticks)
ax2.set_xticklabels([f'{int(s)}' for s in spacing], fontsize=9)
ax2.set_xlabel(r'Mirror spacing $p$ (cm)', fontsize=11)

ax.legend(loc='upper right', fontsize=10, framealpha=0.95, edgecolor='0.7')
ax.grid(True, alpha=0.25, linewidth=0.5)
ax.tick_params(direction='in', top=False, right=True)

plt.tight_layout()

# ── Save ─────────────────────────────────────────────────────────────────────
for ext in ['pdf', 'png']:
    path = f'fig_spacing_sweep.{ext}'
    dpi  = 300 if ext == 'pdf' else 200
    plt.savefig(path, dpi=dpi, bbox_inches='tight')
    print(f"Saved: {path}")

plt.close()
print("Done.")
