#!/usr/bin/env python3
"""
plot_figure8.py — Figure 8: Daily Optical Efficiency Profiles

Reads daily_efficiency_profile.csv exported by JazariFresnelSim
TestOptimization menu option [9] and produces Figure 8.

Usage:
    java -jar JazariFresnelSim.jar   # Select option [9]
    python scripts/plot_figure8.py

Input:  daily_efficiency_profile.csv  (or data/daily_efficiency_profile.csv)
Output: fig_daily_profiles.pdf, fig_daily_profiles.png

Authors: Yunus Demirtas, Musa Atas — Siirt University
Paper: "Rapid Optical-Thermal Design of Linear Fresnel Reflectors"
       Solar Energy (Elsevier), 2026
"""

import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import os, sys

matplotlib.rcParams['font.family'] = 'serif'
matplotlib.rcParams['font.size']   = 11
matplotlib.rcParams['axes.linewidth'] = 0.8

def find_csv(name):
    for path in [name, os.path.join('data', name),
                 os.path.join('..', name), os.path.join('..', 'data', name)]:
        if os.path.exists(path):
            return path
    print(f"ERROR: {name} not found.")
    print("Run JazariFresnelSim option [9] first.")
    sys.exit(1)

# ── Load ─────────────────────────────────────────────────────────────────────
csv = find_csv('daily_efficiency_profile.csv')
print(f"Loading: {csv}")
data = np.genfromtxt(csv, delimiter=',', skip_header=1)

hours      = data[:, 0]
eta_std    = data[:, 1]   # Standard N=6
eta_hc     = data[:, 2]   # High-Conc N=10
eta_cmp    = data[:, 3]   # Compact N=4

# Zero out sunrise/sunset noise (values below 15% are unreliable
# due to low solar altitude and extreme transverse angles)
threshold = 20.0
eta_std = np.where(eta_std < threshold, 0, eta_std)
eta_hc  = np.where(eta_hc  < threshold, 0, eta_hc)
eta_cmp = np.where(eta_cmp < threshold, 0, eta_cmp)

# Solar noon annotation
solar_noon = 12.75

print(f"  Peak efficiencies:")
print(f"    Standard  (N=6):  {np.max(eta_std):.1f}% at h={hours[np.argmax(eta_std)]:.0f}")
print(f"    High-Conc (N=10): {np.max(eta_hc):.1f}%  at h={hours[np.argmax(eta_hc)]:.0f}")
print(f"    Compact   (N=4):  {np.max(eta_cmp):.1f}% at h={hours[np.argmax(eta_cmp)]:.0f}")

# ── Plot ─────────────────────────────────────────────────────────────────────
fig, ax = plt.subplots(figsize=(8, 5))

ax.plot(hours, eta_std, color='#2196F3', linewidth=2.2,
        label=r'Standard ($N=6$)')
ax.plot(hours, eta_hc,  color='#F44336', linewidth=2.2,
        label=r'High-Conc. ($N=10$)')
ax.plot(hours, eta_cmp, color='#4CAF50', linewidth=2.2,
        label=r'Compact ($N=4$)')

# Solar noon dashed line
ax.axvline(solar_noon, color='gray', linestyle='--',
           linewidth=1.0, alpha=0.8)
ax.text(solar_noon + 0.1, 5, 'Solar noon',
        fontsize=8.5, color='gray', rotation=90, va='bottom')

ax.set_xlabel(r'Local time (h)', fontsize=12)
ax.set_ylabel(r'Optical efficiency $\eta_{\mathrm{opt}}$ (%)', fontsize=12)
ax.set_xlim(5.5, 20.5)
ax.set_ylim(0, 100)
ax.set_xticks(range(6, 21, 1))
ax.legend(loc='upper left', fontsize=10, framealpha=0.95, edgecolor='0.7')
ax.grid(True, alpha=0.25, linewidth=0.5)
ax.tick_params(direction='in', top=True, right=True)

# Note about sharp cutoff (collimated-ray assumption)
ax.text(0.98, 0.04,
        'Sharp morning/evening transitions:\ngeometric blocking limit\n'
        '(collimated-ray assumption)',
        transform=ax.transAxes, fontsize=8,
        ha='right', va='bottom', color='0.4',
        bbox=dict(boxstyle='round,pad=0.3', facecolor='white',
                  edgecolor='0.7', alpha=0.8))

plt.tight_layout()

for ext in ['pdf', 'png']:
    path = f'fig_daily_profiles.{ext}'
    dpi  = 300 if ext == 'pdf' else 200
    plt.savefig(path, dpi=dpi, bbox_inches='tight')
    print(f"Saved: {path}")

plt.close()
print("Done.")
