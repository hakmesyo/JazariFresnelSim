<p align="center">
  <img src="docs/banner.png" alt="JazariFresnelSim" width="800"/>
</p>

<h1 align="center">JazariFresnelSim</h1>

<p align="center">
  <b>Rapid Optical–Thermal Simulation & Design Optimization of Linear Fresnel Reflectors</b>
</p>

<p align="center">
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-green.svg" alt="License: MIT"/></a>
  <a href="https://www.java.com"><img src="https://img.shields.io/badge/Java-17%2B-orange.svg" alt="Java 17+"/></a>
  <a href="#"><img src="https://img.shields.io/badge/Platform-Windows%20%7C%20macOS%20%7C%20Linux-blue.svg" alt="Platform"/></a>
  <a href="#citation"><img src="https://img.shields.io/badge/Paper-Solar%20Energy%20(2026)-red.svg" alt="Paper"/></a>
</p>

<p align="center">
  <a href="#-quick-start-2-minutes">Quick Start</a> •
  <a href="#-features">Features</a> •
  <a href="#-for-researchers">For Researchers</a> •
  <a href="#-validation">Validation</a> •
  <a href="#citation">Citation</a>
</p>

---

## What is JazariFresnelSim?

JazariFresnelSim is an **open-source analytical simulation framework** for the optical–thermal design of Linear Fresnel Reflector (LFR) concentrated solar power systems. It evaluates complete LFR configurations **over 500× faster** than Monte Carlo ray-tracing tools like SolTrace, enabling:

- **Instant parametric exploration** — sweep thousands of designs in seconds
- **Metaheuristic optimization** — PSO, GA, and SA find optimal geometries in under 3 seconds
- **Dimensionless design rules** — generalized sizing guidelines validated across multiple locations
- **Interactive 3D visualization** — real-time mirror tracking, ray paths, and performance metrics

<p align="center">
  <img src="docs/screenshots/gui_screenshot.png" alt="Interactive 3D Environment" width="700"/>
</p>

> **Accompanying paper:** *"Rapid Optical–Thermal Design of Linear Fresnel Reflectors: An Open-Source Analytical Framework and Dimensionless Sizing Rules"* — submitted to *Solar Energy* (Elsevier), 2026.

---

## 🚀 Quick Start (2 minutes)

### Prerequisites

- **Java 17 or later** — [Download from Oracle](https://www.oracle.com/tr/java/technologies/downloads/)
- Verify installation: open a terminal and type `java -version`

### Option A: Download and Run (easiest — no IDE needed)

1. **Download** the latest release: [**⬇ JazariFresnelSim.zip**](https://github.com/hakmesyo/JazariFresnelSim/releases/latest/download/JazariFresnelSim.zip)
2. **Extract** the ZIP to any folder
3. **Double-click** `JazariFresnelSim.jar` inside the extracted folder
4. The launcher window opens — choose **Interactive 3D Simulator** or **Validation & Optimization Tests**

> **Important:** Do not move the JAR file out of its folder. The `lib/` and `natives/` folders must stay next to it.

<p align="center">
  <img src="docs/screenshots/launcher_screenshot.png" alt="JazariFresnelSim Launcher" width="500"/>
</p>

### Option B: Run from terminal

```bash
# Extract and run
unzip JazariFresnelSim.zip
cd JazariFresnelSim

# Launch GUI
java -jar JazariFresnelSim.jar

# Or launch directly in CLI mode (no GUI)
java -jar JazariFresnelSim.jar --cli
```

You will see this menu:

```
================================================================
  JazariFresnelSim — Validation & Optimization Test Suite v2.2
  Paper: Rapid Optical-Thermal Design of LFR Systems
  Journal: Solar Energy (Elsevier), 2026
================================================================

========== MAIN MENU ==========
  --- Tabular results (manuscript order) ---
  [1]  Extreme-Angle Error Analysis      — Table 9
  [2]  Mirror Count Scaling              — Table 13
  [3]  Metaheuristic Optimization        — Tables 14-15
  [4]  Temporal Discretization           — Table 16
  --- Figure data export (CSV + Python) ---
  [5]  Spacing Sweep Export              — Fig. 6
  [6]  Height Sweep Export               — Fig. 7
  [7]  Daily Efficiency Profile Export   — Fig. 8
  [8]  Convergence Data Export           — Fig. 9
  ---
  [9]  Run ALL Tests
  [0]  Exit
================================
```

### Option C: Build from source

```bash
git clone https://github.com/hakmesyo/JazariFresnelSim.git
cd JazariFresnelSim
```

Open the project in **NetBeans** (or any Java IDE), set `jazarifresnelsim.JazariLauncher` as the main class, and run. Alternatively, build the JAR and run:

```bash
java -jar dist/JazariFresnelSim.jar
```

---

## ✨ Features

### Analytical Optical–Thermal Engine

| Component | Method | Accuracy |
|-----------|--------|----------|
| Solar position | Spencer 7-term Fourier series | RMSE 0.19° (vs. NREL SPA) |
| Mirror tracking | Bisector-based law of reflection | Max error 0.04° |
| Shading/blocking | 3D vector projection, all pairs | RMSE 0.6 pp (vs. SolTrace) |
| End losses | Bellos et al. analytical formulation | — |
| Spillage | First-order beam-width correction | σ_opt = 4.65 mrad |
| Thermal model | Churchill–Bernstein + radiative loss | Bare tube, 250°C |

### Computational Performance

| Mirrors (N) | Core time | Evaluation rate |
|-------------|-----------|-----------------|
| 6 | 0.18 ms | 60 Hz (with rendering) |
| 10 | 0.30 ms | 60 Hz (with rendering) |
| 48 | 1.31 ms | **700+ Hz (headless)** |

### Optimization Algorithms

Three metaheuristic algorithms with 5-parameter simultaneous optimization:

| Algorithm | Execution time | Best yield | Std. dev. |
|-----------|---------------|------------|-----------|
| Simulated Annealing | 0.52 s | 566.7 kW/m² | 49.1 |
| Genetic Algorithm | 2.44 s | 617.7 kW/m² | 15.9 |
| Particle Swarm (PSO) | 1.62 s | 602.6 kW/m² | 16.1 |

> Yield values correspond to the simplified thermal model (η_th = 0.70); relative algorithm rankings are independent of this assumption.

---

## 🔬 For Researchers

### Reproducing Paper Results

Every figure and table in the manuscript can be reproduced from this repository:

| Paper Reference | Menu Option | Output |
|----------------|-------------|--------|
| Table 9 (Extreme-angle analysis) | `[1]` | Console output |
| Table 13 (Mirror count scaling) | `[2]` | Console output |
| Tables 14–15 (Optimization results) | `[3]` | Console output |
| Table 16 (Temporal sensitivity) | `[4]` | Console output |
| Figure 6 (Spacing sweep) | `[5]` | `spacing_sweep.csv` → Python plot |
| Figure 7 (Height sweep) | `[6]` | `height_sweep.csv` → Python plot |
| Figure 8 (Daily efficiency profiles) | `[7]` | `daily_efficiency_profile.csv` → Python plot |
| Figure 9 (Convergence plot) | `[8]` | `convergence_*.csv` → Python plot |

**To reproduce Figures 6–9:**

```bash
# Step 1: Generate data
java -jar JazariFresnelSim.jar
# Select the relevant menu option, e.g. [8] for convergence data (~2.5 min)

# Step 2: Plot with Python
pip install matplotlib numpy
python scripts/plot_figure6.py   # Fig. 6 — shading loss vs p/w
python scripts/plot_figure7.py   # Fig. 7 — energy vs receiver height
python scripts/plot_figure8.py   # Fig. 8 — daily efficiency profiles
python scripts/plot_convergence.py  # Fig. 9 — convergence histories
```

The archived CSV files (`convergence_SA.csv`, `convergence_GA.csv`, `convergence_PSO.csv`) are included in the `data/` directory for immediate plotting without re-running the optimization.

### Dimensionless Design Rules

The framework derives two sizing rules validated across Diyarbakır (37.96°N), Berlin (52.52°N), and Jeddah (21.49°N):

| Rule | Formula | Meaning |
|------|---------|---------|
| **Rule 1** | p/w > 3.0 | Daily-averaged shading losses fall below 2% |
| **Rule 2** | N_opt ≈ 0.6 · W_f/p | Optimal mirror count for a given field width |

> **Receiver height guidance:** No universal optimal H_r/W_f ratio exists across different field geometries. A practical upper bound of H_r < 1.5 × W_f is recommended to avoid the region of diminishing optical returns at elevated structural cost.

### Using as a Library

You can integrate the analytical engine into your own Java projects:

```java
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.domain.MirrorTracker;
import jazarifresnelsim.domain.ShadingDetector;
import jazarifresnelsim.models.*;

// 1. Calculate solar position for any location and time
SolarCalculator calc = new SolarCalculator(37.96, 40.25, 0);  // lat, lon, alt
SolarPosition sunPos = calc.calculateSolarPosition(
    LocalDateTime.of(2024, 6, 21, 12, 0));

// 2. Compute optimal mirror angles
MirrorTracker tracker = new MirrorTracker();
double angle = tracker.calculateOptimalMirrorAngle(
    mirrorX, sunPos, simulationState);

// 3. Evaluate shading losses
ShadingDetector shading = new ShadingDetector();
double efficiency = shading.calculateBlockingAndShadingLoss(
    mirror, allMirrors, state, sunPos);

// 4. Run full optimization
FresnelDesignProblem problem = new FresnelDesignProblem(
    37.96, 40.25, evaluationTimes);
ParticleSwarm pso = new ParticleSwarm();
DesignSolution best = pso.optimize(problem, initialParams, constraints);
```

### Customizing Parameters

All design parameters can be adjusted programmatically:

```java
DesignParameters params = new DesignParameters(
    130.0,   // receiverHeight (cm)   — range: [30, 300]
    16.0,    // receiverDiameter (cm) — range: [10, 50]
    20.0,    // mirrorWidth (cm)      — range: [5, 30]
    30.0,    // mirrorSpacing (cm)    — range: [15, 80]
    6        // numberOfMirrors       — range: [2, 10]
);
```

---

## ✅ Validation

The framework is validated through a 5-level hierarchy:

```
Level 1: Solar Position   ──→  NREL SPA            ──→  RMSE < 0.25°
Level 2: Mirror Tracking  ──→  Closed-form          ──→  Error < 10⁻⁶°
Level 3: Mirror Angles    ──→  Barbón et al.        ──→  Max error 0.04°
Level 4: Intercept Factor ──→  Santos et al. MCRT   ──→  Agreement to 55°
Level 5: System Optical   ──→  SolTrace (5 geom.)   ──→  RMSE 2.1 pp
```

---

## 📁 Project Structure

```
JazariFresnelSim/
├── src/jazarifresnelsim/
│   ├── core/               # Simulation controller & interface
│   ├── domain/             # Analytical engine (stateless, O(N))
│   │   ├── SolarCalculator.java
│   │   ├── MirrorTracker.java
│   │   └── ShadingDetector.java
│   ├── models/             # Immutable data representations
│   ├── optimization/       # PSO, GA, SA + evaluation framework
│   │   ├── algorithms/     # Algorithm implementations
│   │   ├── evaluation/     # Multi-metric design evaluator
│   │   └── problem/        # Design parameter space definition
│   ├── test/               # Validation benchmarks
│   └── ui/                 # Processing-based 3D renderer
├── scripts/
│   ├── plot_figure6.py     # Fig. 6 — shading loss vs p/w ratio
│   ├── plot_figure7.py     # Fig. 7 — energy vs receiver height
│   ├── plot_figure8.py     # Fig. 8 — daily optical efficiency profiles
│   └── plot_convergence.py # Fig. 9 — convergence histories
├── data/
│   ├── spacing_sweep.csv           # Fig. 6 data
│   ├── height_sweep.csv            # Fig. 7 data
│   ├── daily_efficiency_profile.csv # Fig. 8 data
│   ├── convergence_SA.csv          # Fig. 9 data — Simulated Annealing
│   ├── convergence_GA.csv          # Fig. 9 data — Genetic Algorithm
│   └── convergence_PSO.csv         # Fig. 9 data — Particle Swarm
├── docs/                   # Documentation and screenshots
└── README.md
```

---

## 🛠️ Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| Java | 17 | 21 |
| RAM | 512 MB | 4 GB |
| GPU | Any (OpenGL 2.0) | Dedicated GPU |
| OS | Windows / macOS / Linux | Any |
| Python (for plots only) | 3.8+ | 3.10+ |

---

## 📖 Citation

If you use JazariFresnelSim in your research, please cite:

```bibtex
@article{demirtas2026jazari,
  author  = {Demirta{\c{s}}, Yunus and Ata{\c{s}}, Musa},
  title   = {Rapid Optical--Thermal Design of Linear Fresnel Reflectors:
             An Open-Source Analytical Framework and Dimensionless Sizing Rules},
  journal = {Solar Energy},
  year    = {2026},
  note    = {Submitted},
  url     = {https://github.com/hakmesyo/JazariFresnelSim}
}
```

---

## 📜 License

This project is licensed under the [MIT License](LICENSE) — free for academic and commercial use.

---

## 🤝 Contributing

Contributions are welcome! Areas where help is particularly appreciated:

- **Gaussian optical error model** — replacing the collimated-ray assumption with sunshape convolution
- **Secondary optics (CPC)** — analytical acceptance-angle model for compound parabolic concentrators
- **Non-uniform mirror spacing** — exposing per-mirror positions as optimization variables
- **Python API wrapper** — enabling integration with the Python scientific computing ecosystem

Please open an issue or pull request on GitHub.

---

## 📧 Contact

- **Musa Ataş** (Corresponding Author) — [musa.atas@siirt.edu.tr](mailto:musa.atas@siirt.edu.tr)
- **Yunus Demirtaş** — [yunusdemirtas@siirt.edu.tr](mailto:yunusdemirtas@siirt.edu.tr)

Department of Computer Engineering & Mechanical Engineering, Siirt University, Turkey