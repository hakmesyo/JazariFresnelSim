<p align="center">
  <img src="docs/banner.png" alt="JazariFresnelSim" width="800"/>
</p>

<h1 align="center">JazariFresnelSim</h1>

<p align="center">
  <b>An Open-Source, Ray-Tracing-Validated Analytical Engine for Linear Fresnel Reflector Field Design</b>
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

JazariFresnelSim is an **open-source, purely-optical analytical simulation framework** for Linear Fresnel Reflector (LFR) field design. It is the reference implementation for the accompanying manuscript, which shows that a purely optical objective has no interior optimum in mirror count, and that a minimum receiver concentration ratio is what actually makes the field-sizing problem well posed.

- **Instant parametric exploration** — sweep hundreds of geometries in seconds
- **Metaheuristic optimization** — PSO, GA and SA under a hard concentration-ratio constraint (C_g ≥ 20)
- **Three-layer verification** — solar position vs. NREL SPA, mirror tracking vs. the law of reflection, and system optical efficiency vs. SolTrace ray-tracing
- **Interactive 3D visualization** — real-time mirror tracking and ray paths
- **In-app manuscript figures** — the four figure-producing tests render the corresponding plot inline, next to the numeric output

<p align="center">
  <img src="docs/screenshots/gui_screenshot.png" alt="Interactive 3D Environment" width="700"/>
</p>

> **Accompanying paper:** *"The Limits of Purely-Optical Field Sizing in Linear Fresnel Reflectors: A Ray-Tracing-Validated Analytical Study"* — submitted to **Solar Energy**.

> **Scope note:** This is a purely optical pre-screening tool. It does not model receiver thermal losses, secondary optics, or economics — see Section 1.2 of the manuscript for the scope discussion.

---

## 🚀 Quick Start (2 minutes)

### Prerequisites

- **Java 17 or later** — [Download from Oracle](https://www.oracle.com/tr/java/technologies/downloads/)
- Verify installation: open a terminal and type `java -version`

### Option A: Download and Run (easiest — no IDE needed)

1. **Download** the latest release: [**⬇ JazariFresnelSim.zip**](https://github.com/hakmesyo/JazariFresnelSim/releases/latest/download/JazariFresnelSim.zip)
2. **Extract** the ZIP to any folder
3. **Double-click** `JazariFresnelSim.jar` inside the extracted folder
4. The launcher window opens — choose **Interactive 3D Simulator** or **Design Fresnel System**

> **Important:** Do not move the JAR file out of its folder. The `lib/` and `natives/` folders must stay next to it.

<p align="center">
  <img src="docs/screenshots/launcher_screenshot.png" alt="JazariFresnelSim Launcher" width="500"/>
</p>

### Option B: Run from terminal

```bash
unzip JazariFresnelSim.zip
cd JazariFresnelSim
java -jar JazariFresnelSim.jar
```

This opens the launcher window. Select **Design Fresnel System**, then the **Manuscript Validation** tab. Nine buttons reproduce the manuscript's tables and figures directly, in ascending order (tables first, then figures):

```
Test 1 · Solar Position         — Table 1, Fig. 2
Test 2 · Tracking Solver        — Table 2
Test 3 · G1–G5 vs SolTrace      — Table 3-4
Test 4 · Mirror Count Scaling   — Table 6
Test 5 · p/w Sweep              — Table 7, Fig. 3
Test 6 · Metaheuristic Opt.     — Table 10 (Cg ≥ 20)
Test 7 · Temporal Discret.      — Table 11
Test 8 · Hr/Wf Scaling          — Table 8, Fig. 4
Test 9 · Well-posedness         — Fig. 5
Run All Tests                   — Tests 1–9 in sequence
```

Tests 1, 5, 8 and 9 render the corresponding manuscript figure inline, next to the console output, using a dependency-free in-app chart renderer — no external plotting step is required. Table 5 (three-layer verification summary) and Table 9 (metaheuristic hyperparameters) have no dedicated button because neither is an independent computation: Table 5 summarizes Tests 1–3, and Table 9 documents the fixed PSO/GA/SA settings already visible in the source.

> There is no `--cli` flag in the current build; `JazariLauncher` always opens the graphical launcher.

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

### Analytical Optical Engine

Implements Eqs. (1)–(16) of the manuscript. Every row below is directly reproducible from the Manuscript Validation panel (button noted in parentheses):

| Component | Method | Verified accuracy |
|-----------|--------|--------------------|
| Solar position | Spencer (1971) 7-term Fourier series | RMSE 0.117° vs. NREL SPA (Test 1) |
| Mirror tracking | Bisector law of reflection | Residual ≈ 10⁻¹³° (Test 2) |
| Shading/blocking | 3D vector projection, all mirror pairs | — |
| End losses | Slant-distance lever arm (Eq. 11) | — |
| Spillage | Closed-form Gaussian beam-intercept (Eq. 14–15), σ_opt = 2.325 mrad | — |
| System optical efficiency | Full chain vs. SolTrace MCRT, 5 geometries | RMSE 0.25 pp (Test 3) |

No thermal, secondary-optics, or economic model is included — see the scope note above.

**Performance:** solar position is O(1) per time step and tracking is O(N), but pairwise shading/blocking (Eq. 12) is O(N²) and dominates for the field sizes considered. One complete field evaluation takes **17.5 μs at N=17** on an Intel Core i7-10700 @ 2.90 GHz (manuscript, Sec. 3.5), which is what makes the metaheuristic searches in Test 6 practical.

**Speed vs. ray tracing:** for the five G1–G5 geometries of Table 3-4 at solar noon, the analytical engine reproduces SolTrace's optical efficiency to **RMSE 0.25 pp** (matching Table 4 exactly) while taking **142 μs total** against SolTrace's **20.1 s** for the same five geometries at 10⁶ target ray-receiver intersections each (~90.6 million rays generated, 4 CPU threads) — a measured **~141,000×** speedup for this comparison. This is why the framework is useful for design-space exploration: it replaces a statistically-converged Monte Carlo estimate with an exact closed-form evaluation of the same physics.

### Optimization

Three metaheuristics (GA, PSO, SA) search a 4-parameter space (H_r, w, p, N) under a **hard** concentration-ratio constraint C_g = N·w/D_r ≥ 20 (Eq. 18), matching the manuscript exactly. Designs that violate the constraint are rejected outright, not softly penalized — this is what makes the optimum sit reproducibly on the constraint boundary, as reported in Table 10.

| Parameter | Range |
|-----------|-------|
| Receiver height H_r | 50 – 600 cm |
| Mirror width w | 5 – 40 cm |
| Mirror spacing p | 15 – 150 cm |
| Mirror count N | 4 – 50 |
| Receiver diameter D_r | 10 cm (fixed) |

---

## 🔬 For Researchers

### Reproducing Paper Results

| Manuscript reference | Button | Output |
|---|---|---|
| Table 1, Fig. 2 (solar position vs. NREL SPA) | Test 1 | Console + inline chart |
| Table 2 (tracking solver vs. law of reflection) | Test 2 | Console |
| Table 3-4 (G1–G5 vs. SolTrace) | Test 3 | Console |
| Table 6 (mirror count scaling) | Test 4 | Console |
| Table 7, Fig. 3 (p/w sweep) | Test 5 | Console + inline chart |
| Table 10 (constrained optimization) | Test 6 | Console |
| Table 11 (temporal resolution sensitivity) | Test 7 | Console |
| Table 8, Fig. 4 (H_r/W_f scaling) | Test 8 | Console + inline chart |
| Fig. 5 (well-posedness) | Test 9 | Console + inline chart |

Raw sweep data is also written to CSV files in the working directory for external plotting or cross-checking, alongside the console output and in-app charts.

### Central Result

A purely optical objective (Fig. 5a) is monotone in mirror count and drives the optimizer to a boundary — it cannot size a field on its own. Imposing the receiver's minimum concentration ratio (C_g ≥ 20, Fig. 5b) produces a genuine interior optimum. The constrained field-sizing results (Table 10) place the optimum at C_g ≈ 20.0–20.1 at all three tested sites (Jeddah, Diyarbakır, Berlin), confirming the constraint is active, not incidental.

### Using as a Library

```java
import jazarifresnelsim.domain.SolarCalculator;
import jazarifresnelsim.domain.MirrorTracker;
import jazarifresnelsim.domain.ShadingDetector;
import jazarifresnelsim.optimization.problem.DesignParameters;
import jazarifresnelsim.optimization.problem.FresnelDesignProblem;
import jazarifresnelsim.models.*;

// 1. Calculate solar position for any location and time
SolarCalculator calc = new SolarCalculator(37.91, 40.24, 0);  // lat, lon, alt
SolarPosition sunPos = calc.calculateSolarPosition(
    LocalDateTime.of(2024, 6, 21, 12, 0));

// 2. Define a design (Hr, w, p, N -- Dr and L are fixed via config)
DesignParameters params = new DesignParameters(216.3, 11.1, 15.0, 17);

// 3. Evaluate the constrained objective J [Wh/m2 of land], Eq. (18)
FresnelDesignProblem problem = new FresnelDesignProblem(
    "Diyarbakir", 37.91, 40.24);   // TMY constructor, real DNI data
double J = problem.evaluateDesign(params);
```

---

## ✅ Validation

Three independent layers, each checked against a reference the model itself was not fit to (Sec. 4 of the manuscript):

```
Layer 1: Solar Position      ──→  NREL SPA (pvlib)     ──→  RMSE 0.117°       (Test 1)
Layer 2: Mirror Tracking     ──→  Law of reflection      ──→  Residual ~1e-13°  (Test 2)
Layer 3: System Optical Eff. ──→  SolTrace MCRT, 5 geom. ──→  RMSE 0.25 pp      (Test 3)
```

Table 5 of the manuscript summarizes all three layers in one place; it is not re-computed separately since it draws directly on Tests 1–3.

### Independent Cross-Check: Run the SolTrace Side Yourself

Layer 3 above is not something you have to take on faith. Below is the exact SolTrace LK script for the G1–G5 geometries of Table 3 (June 21 solar noon, Diyarbakır). Running it independently and comparing against Test 3's console output is a direct, external check of the manuscript's central accuracy claim — no part of this script was written or touched by the analytical engine it is validating against.

Save it as `soltrace_g1_g5_validation.lk`, open it in SolTrace's script editor, and run it. It prints `eta_opt_pct` for each of the five geometries; compare these against the `eta_JFS` column printed by **Test 3** in the app. On the machine this was verified on, the two agree to **RMSE 0.25 pp**, and SolTrace takes on the order of 20 seconds for all five geometries (≈9×10⁷ rays at 10⁶ target intersections each) against ≈142 μs total for the analytical engine — consistent with the ≈141,000× figure quoted above.

<details>
<summary><b>soltrace_g1_g5_validation.lk</b> (click to expand)</summary>

```javascript
// ============================================================================
// FINAL SOLTRACE VALIDATION - G1..G5
// Run this once. It replaces every earlier SolTrace run.
//
// WHAT WAS WRONG BEFORE, AND WHAT IS FIXED
// ----------------------------------------
// 1. include_sunshape defaulted to 0, so every earlier trace ran as a point
//    source. Measured: the beam left the mirror at 0.2890 mm standard
//    deviation and arrived 5 m later at 0.2890 mm - no spread at all.
//    Now set to 1. Measured with it on: 2.333 mrad.
//
// 2. The declared 4.65 mrad is a HALF-ANGLE, not a standard deviation.
//    Measured projection: 2.333 mrad against the 2.325 predicted for a
//    pillbox of half-angle 4.65. The analytical model must therefore be fed
//        SIGMA_OPT = 2.325e-3
//    not 4.65e-3.
//
// 3. optical_errors defaulted to 0, but the optic defaults are non-zero
//    (errslope 0.95 mrad, errspec 0.2 mrad). Measured contribution with the
//    switch on: 1.895 mrad, against sqrt((2*0.95)^2 + 0.2^2) = 1.911. The
//    factor two on slope error is confirmed. Both are set to zero here so
//    that the sun is the only angular contribution and the two codes model
//    the same physics.
//
// 4. The receiver is a flat downward-facing strip of width Dr, which presents
//    only Dr*cos(psi) to a ray arriving at angle psi. The analytical model
//    previously assumed a tube, presenting Dr from every direction; that
//    mismatch accounted for essentially all of the apparent spillage in the
//    earlier comparison. The strip is kept here and the analytical model is
//    corrected to match, so both codes describe the same absorber.
//
// GEOMETRY: w = 10 cm, L = 10 m, Dr = 10 cm, Hs = 30 cm, rho_m = 0.92
// ============================================================================

deg2rad     = 3.14159265 / 180.0;
target_hits = 1000000;
rho_m       = 0.92;
HALFANG     = 4.65;      // sun half-angle [mrad]

L  = 10.00;
Dr = 0.10;
Hs = 0.30;

alt = 74.9;
azi = 161.6;
sx = -cos(alt*deg2rad) * sin(azi*deg2rad);
sy =  sin(alt*deg2rad);
sz = -cos(alt*deg2rad) * cos(azi*deg2rad);

names = ["G1_Compact", "G2_Standard", "G3_WideSpaced",
         "G4_HighFocus", "G5_LargeField"];
Ns  = [4,    6,    6,    8,     16  ];
ws  = [0.10, 0.10, 0.10, 0.10,  0.10];
ps  = [0.10, 0.15, 0.25, 0.175, 0.20];
Hrs = [1.00, 1.30, 1.30, 2.00,  2.50];

outln("=== FINAL VALIDATION | sunshape ON, mirror errors OFF, flat absorber ===");
outln("CSV,name,N,p,Hr,recv_hits,sun_nrays,A_sun,A_field,eta_opt_pct");

for (g = 0; g < 5; g++) {

    N  = Ns[g];
    w  = ws[g];
    p  = ps[g];
    Hr = Hrs[g];
    A_field = N * w * L;

    clear_project();
    clearoptics();
    clearstages();

    // slope and specularity forced to zero: the sun is the only spread
    addoptic("M");
    opticopt("M", 1, { 'refl': rho_m, 'errslope': 0.0, 'errspec': 0.0 });
    addoptic("T");
    opticopt("T", 1, { 'refl': 0.0, 'errslope': 0.0, 'errspec': 0.0 });

    sunopt({ 'x': sx, 'y': sy, 'z': sz,
             'ptsrc': false, 'shape': 'p',
             'sigma': HALFANG, 'halfwidth': HALFANG });

    // ---- stage 0: mirrors ----
    addstage("Mirrors");
    clearelements();
    addelement(N);

    for (i = 0; i < N; i++) {

        if (i < N/2) { off = -(i + 0.5); } else { off = (i - N/2 + 0.5); }
        xm = off * p;

        tx = -xm;
        ty = Hr - Hs;
        tm = sqrt(tx*tx + ty*ty);
        tx = tx / tm;
        ty = ty / tm;

        sm  = sqrt(sx*sx + sy*sy);
        sxu = sx / sm;
        syu = sy / sm;

        nx = sxu + tx;
        ny = syu + ty;
        nm = sqrt(nx*nx + ny*ny);
        nx = nx / nm;
        ny = ny / nm;

        elementopt(i, { 'x': xm, 'y': Hs, 'z': 0,
                        'ax': xm + nx, 'ay': Hs + ny, 'az': 0,
                        'zrot': 90,
                        'aper': ['r', w, L],
                        'surf': ['f'], 'interact': 2, 'optic': 'M' });
    }

    // ---- stage 1: flat absorber strip, facing down ----
    // A cylinder would be the physical receiver, but the LK surface syntax
    // for one is not reliable here. A flat strip is used instead, and the
    // analytical model is given the matching cos(psi_t) projection so that
    // both codes describe the same absorber.
    addstage("Receiver");
    clearelements();
    addelement(1);
    elementopt(0, { 'x': 0, 'y': Hr, 'z': 0,
                    'ax': 0, 'ay': Hs, 'az': 0,
                    'zrot': 90,
                    'aper': ['r', L, Dr],
                    'surf': ['f'],
                    'interact': 2, 'optic': 'T' });

    traceopt({ 'rays': target_hits, 'maxrays': 2000000000,
               'cpus': 4, 'seed': 42,
               'include_sunshape': 1, 'optical_errors': 1 });
    trace();

    sd    = sundata();
    A_sun = (sd.xmax - sd.xmin) * (sd.ymax - sd.ymin);
    rec   = nintersect(1, 0);

    eta = (rec / sd.nrays) * A_sun / A_field * 100.0;

    outln("--- " + names[g] + " | N=" + N + " p=" + p + " Hr=" + Hr + " ---");
    outln("    sun rays      = " + sd.nrays);
    outln("    A_sun         = " + A_sun + " m2   A_field = " + A_field + " m2");
    outln("    receiver hits = " + rec);
    outln("    eta_opt       = " + eta + " %");
    outln("CSV," + names[g] + "," + N + "," + p + "," + Hr + ","
          + rec + "," + sd.nrays + "," + A_sun + "," + A_field + "," + eta);
}

outln("=== DONE ===");
```

</details>

> **Note on repository hygiene:** an earlier script (`soltrace_FINAL_G1_G5.lk`, despite its name) used a different sunshape parameterization and did not zero out the optics' default slope/specularity errors; it produces a noticeably worse match (RMSE ≈1.5 pp) and should not be used for validation. The script above (`soltrace_g1_g5_validation.lk`) is the one confirmed, independently, to reproduce Table 4's RMSE of 0.25 pp.

---

## 📁 Project Structure

```
JazariFresnelSim/
├── src/jazarifresnelsim/
│   ├── core/               # Simulation controller & interface
│   ├── domain/              # Analytical engine (stateless, O(N))
│   │   ├── SolarCalculator.java
│   │   ├── MirrorTracker.java
│   │   ├── ShadingDetector.java
│   │   ├── ConfigManager.java
│   │   └── SolarData.java   # TMY-style DNI data (Jeddah, Diyarbakir, Berlin)
│   ├── models/              # Immutable data representations
│   ├── optimization/        # PSO, GA, SA + evaluation framework, and
│   │   │                    #   TestOptimization.java (all 9 validation tests)
│   │   ├── algorithms/
│   │   ├── evaluation/
│   │   └── problem/          # FresnelDesignProblem, DesignParameters
│   └── ui/
│       ├── DesignFresnelSystemFrame.java  # Manuscript Validation panel
│       ├── ChartPanel.java                # dependency-free XY chart renderer
│       └── DualChartPanel.java            # side-by-side (a)/(b) figure layout
├── docs/                    # Documentation and screenshots
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

---

## 📖 Citation

If you use JazariFresnelSim in your research, please cite:

```bibtex
@article{atas2026limits,
  author  = {Ata{\c{s}}, Musa and Demirta{\c{s}}, Yunus},
  title   = {The Limits of Purely-Optical Field Sizing in Linear Fresnel
             Reflectors: A Ray-Tracing-Validated Analytical Study},
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

- **Gaussian optical error model** — extending the sunshape convolution beyond the collimated-plus-Gaussian-spread approximation used here
- **Secondary optics (CPC)** — analytical acceptance-angle model for compound parabolic concentrators
- **Non-uniform mirror spacing** — exposing per-mirror positions as optimization variables
- **Utility-scale validation** — extending the SolTrace cross-check to commercial-scale geometries (see manuscript Sec. 6.2 for the scale-limitation discussion)

Please open an issue or pull request on GitHub.

---

## 📧 Contact

- **Musa Ataş** (Corresponding Author) — [musa.atas@siirt.edu.tr](mailto:musa.atas@siirt.edu.tr)
- **Yunus Demirtaş** — [yunusdemirtas@siirt.edu.tr](mailto:yunusdemirtas@siirt.edu.tr)

Department of Computer Engineering & Mechanical Engineering, Siirt University, Turkey