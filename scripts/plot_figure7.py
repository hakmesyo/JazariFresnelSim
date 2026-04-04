#!/usr/bin/env python3
"""
plot_figure7.py — Figure 7: Receiver Height vs Energy Output

Reads height_sweep.csv exported by JazariFresnelSim TestOptimization
menu option [6] and produces Figure 7 for the Solar Energy manuscript.

Usage:
    java -jar JazariFresnelSim.jar   # Select option [6]
    python scripts/plot_figure7.py

Input:  height_sweep.csv  (or data/height_sweep.csv)
Output: fig_height_sweep.pdf
        fig_height_sweep.png

CSV columns:
    hr_cm, hr_wf_6mirror, energy_6mirror_W, hr_wf_10mirror, energy_10mirror_W

Authors: Yunus Demirtas, Musa Atas — Siirt University
Paper: "Rapid Optical-Thermal Design of Linear Fresnel Reflectors:
        An Open-Source Analytical Framework and Dimensionless Sizing Rules"
       Solar Energy (Elsevier), 2026
"""

import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
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
    print("Run JazariFresnelSim option [6] first.")
    sys.exit(1)

# ── Load ─────────────────────────────────────────────────────────────────────
csv = find_csv('height_sweep.csv')
print(f"Loading: {csv}")
data = np.genfromtxt(csv, delimiter=',', skip_header=1)

hr_cm    = data[:, 0]
hr_wf_6  = data[:, 1]
e6       = data[:, 2]
hr_wf_10 = data[:, 3]
e10      = data[:, 4]

# Peak indices
idx6  = np.argmax(e6)
idx10 = np.argmax(e10)

print(f"  6-mirror  peak: Hr={hr_cm[idx6]:.0f} cm "
      f"(Hr/Wf={hr_wf_6[idx6]:.2f})  E={e6[idx6]:.1f} W")
print(f"  10-mirror peak: Hr={hr_cm[idx10]:.0f} cm "
      f"(Hr/Wf={hr_wf_10[idx10]:.2f}) E={e10[idx10]:.1f} W")

# ── Plot ─────────────────────────────────────────────────────────────────────
fig, ax = plt.subplots(figsize=(7.5, 5))

# 6-mirror curve
ax.plot(hr_cm, e6,
        color='#2196F3', linewidth=2.2,
        marker='o', markersize=5,
        label=r'6-mirror ($p = 30$ cm)')

# 10-mirror curve
ax.plot(hr_cm, e10,
        color='#F44336', linewidth=2.2,
        marker='s', markersize=5,
        label=r'10-mirror ($p = 40$ cm)')

# Peak vertical lines
ax.axvline(hr_cm[idx6],  color='#2196F3', linestyle='--',
           linewidth=1.0, alpha=0.7)
ax.axvline(hr_cm[idx10], color='#F44336',  linestyle='--',
           linewidth=1.0, alpha=0.7)

# Annotate 6-mirror peak
ax.annotate(f'$H_r={hr_cm[idx6]:.0f}$',
            xy=(hr_cm[idx6], e6[idx6]),
            xytext=(hr_cm[idx6] + 8, e6[idx6] - 20),
            fontsize=9, color='#2196F3',
            arrowprops=dict(arrowstyle='->', color='#2196F3', lw=1.0))

# Annotate 10-mirror peak
ax.annotate(f'$H_r={hr_cm[idx10]:.0f}$',
            xy=(hr_cm[idx10], e10[idx10]),
            xytext=(hr_cm[idx10] + 8, e10[idx10] - 18),
            fontsize=9, color='#F44336',
            arrowprops=dict(arrowstyle='->', color='#F44336', lw=1.0))

ax.set_xlabel(r'Receiver height $H_r$ (cm)', fontsize=12)
ax.set_ylabel(r'Energy output (W)', fontsize=12)
ax.set_xlim(hr_cm[0] - 3, hr_cm[-1] + 5)
ax.legend(loc='upper right', fontsize=10, framealpha=0.95, edgecolor='0.7')
ax.grid(True, alpha=0.25, linewidth=0.5)
ax.tick_params(direction='in', top=True, right=True)

plt.tight_layout()

# ── Save ─────────────────────────────────────────────────────────────────────
for ext in ['pdf', 'png']:
    path = f'fig_height_sweep.{ext}'
    dpi  = 300 if ext == 'pdf' else 200
    plt.savefig(path, dpi=dpi, bbox_inches='tight')
    print(f"Saved: {path}")

plt.close()
print("Done.")
