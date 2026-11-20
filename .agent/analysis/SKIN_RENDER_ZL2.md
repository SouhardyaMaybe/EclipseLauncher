# ZL2 PetLib Rendering Analysis

## 1. ZL2 Commit & Source
- **Repo**: `/tmp/opencode/zalith/zl2` (commit `0c10282`, upstream `50f08ca`)
- **Core module**: `PetLib` (Rust software rasterizer + JNI Kotlin wrapper)
- **Key files**:
  - `PetLib/rust/src/render.rs` — software rasterizer with per-pixel z-buffer
  - `PetLib/rust/src/skin.rs` — skin/cape texture parsing, arm model detection
  - `PetLib/rust/src/lighting.rs` — Lambertian shading + sRGB LUT
  - `PetLib/rust/src/pose.rs` — skeleton, animation, gaze, smooth transitions
  - `PetLib/rust/src/render.rs:456-676` — `draw_box` triangle rasterization with z-buffer
  - `PetLib/src/main/java/com/movtery/petlib/PetController.kt` — JNI wrapper, frame loop

## 2. Rendering Technique
| Aspect | ZL2 PetLib |
|--------|------------|
| **Technique** | Software rasterizer with **per-pixel z-buffer** (depth buffer) |
| **Language** | Rust (PetLib/rust) + JNI Kotlin wrapper |
| **Projection** | Weak perspective (orthographic with depth for sorting) |
| **Coordinate system** | Origin at feet, +Y up, model faces +Z (GLTF basis rotated 180°Y) |
| **Geometry** | Box model (6 faces per box), 13 parts: head, body, 2 arms, 2 legs, cape; overlays as inflated duplicates |
| **Skin layout** | 64×64 standard, 64×32 legacy auto-detected; HD multiples up to 1024×1024 |
| **Cape** | 16×10×1 box at shoulder y=24, tilted 6°, 22×17 UV unwrap (or HD multiples) |
| **Slim arms** | 3px vs classic 4px, auto-detected from transparent arm strips |
| **Overlay (outer layer)** | Inflated boxes: hat +1px, others +0.5px; drawn after base layer |
| **Culling** | Base layer: back-face culled (inflate==0); outer layer: no culling, sorted by camera-space depth |
| **Sorting** | Parts sorted: base layer first (inflate==0), then outer layer (inflate>0) by camera-space depth (far→near) |
| **Rasterization** | Per-pixel z-buffer (depth buffer), triangle barycentric, edge functions with 256× fixed-point edge bias |
| **Texture filtering** | Nearest-neighbor (no bilinear) |
| **Lighting** | Directional Lambertian, sRGB-accurate LUT (128 levels), pre-multiplied alpha blending |
| **Texture format** | Pre-multiplied RGBA, alpha cutoff <16 skips, base layer forces opaque on core regions |
| **Alpha handling** | Pre-multiplied RGBA, alpha cutoff <16 skips, base layer forces opaque on core regions |
| **Camera** | Weak perspective: `width/44` scale, camera at (30,23,70) normalized, model centered vertically at y=16 |
| **HD support** | Skin up to 1024×1024 (64× multiples), cape up to 1024×512 (64×32 base × scale) |
| **Texture filtering** | Nearest-neighbor (point sampling) |
| **Lighting** | Lambertian + sRGB LUT (128 levels), pre-multiplied alpha blending |
| **Cape tilt** | 6° (0.1047 rad), attached at y=24, mesh offset (0,-8,+0.5) |
| **Skeleton/Animation** | Full skeleton with idle, walking, sub-anims, interact, squash, swing; cross-fade 0.2s |
| **Output** | Pre-multiplied RGBA, box-filter downsample to target size |

## 3. ZL2 vs Ours — Row-by-Row Comparison

| # | Aspect | ZL2 PetLib | Our Current (SkinRig.kt / SkinModelView.kt) | Difference | Glitch Risk |
|---|--------|------------|---------------------------------------------|------------|-------------|
| 1 | **Depth handling** | **Per-pixel z-buffer** (f32 depth buffer, per-pixel test) | Painter's algorithm (sort by `paintRank`, `boxDepth`, `layer`) | **Critical** — Painter's algorithm cannot handle interpenetrating geometry (cape/head, body/leg) | ✅ Cape tiles, stacked slabs |
| 2 | **Culling** | Base layer: back-face cull; overlay: no cull, sorted by camera-space depth | Both sides drawn for all faces (cull only for reporting) | Overdraw, wrong occlusion | ✅ Cape tiles |
| 3 | **Part sorting** | Base layer first (inflate=0), then outer layer by camera-space depth (far→near) | `paintRank` groups (head/cape/body), then `boxDepth`, `layer` | Cape vs head/body order wrong at some angles | ✅ Cape tiles |
| 4 | **Projection** | Weak perspective (z for depth), camera at (30,23,70) | Orthographic, `cx=64`, `cy=0.46*h`, `scale=min(w/46,h/30)` | Different depth values, different occlusion | ✅ Depth ordering |
| 4a | Camera position | Camera at (30,23,70) normalized | Implicit orthographic, no explicit camera | Different view angles | ✅ |
| 5 | **Texture filtering** | Nearest-neighbor (point sampling) | Nearest-neighbor (`isFilterBitmap=false`) | ✅ Match | — |
| 6 | **Lighting** | Lambertian + sRGB LUT (128 levels), pre-multiplied alpha | No lighting (flat) | Flat look, no depth cues | Minor |
| 7 | **Alpha / Base layer** | Pre-multiplied RGBA, base layer forces opaque on core regions, alpha<16 skips | Pre-multiplied RGBA, alpha<255 blended, no forced opaque | Transparent base layer shows through | Minor |
| 8 | **Cape geometry** | 16×10×1 box at y=24, tilted 6°, offset (0,-8,+0.5), 22×17 UV | Same dimensions, but cape tilt applied in transform | Same, but rendering order differs | ✅ Cape tiles |
| 9 | **Cape UV unwrap** | 22×17 unwrap, outer/inner/left/right/top/bottom | Same (CapeGeometry.kt) | ✅ Match | — |
| 10 | **Slim arms** | 3px vs 4px, auto-detected from transparent arm strips | 3px vs 4px, passed via `skinVariant` | ✅ Match | — |
| 11 | **Overlay inflation** | Hat +1px, others +0.5px; drawn after base, sorted by camera depth | Hat 0.5px (y0=24.5), others 0.25px; `layer` key then `boxDepth` | Different inflation, different sort | ✅ Neck band |
| 12 | **Culling (overlay)** | No culling for inflate>0, sorted by camera depth | Culling applied to all faces | Over-culling of outer layer | ✅ Neck band |
| 13 | **Part sorting (outer)** | Outer layer sorted by camera-space depth (far→near) | `boxDepth` then `layer` | Wrong order at intersecting angles | ✅ Neck band |
| 14 | **HD textures** | Up to 1024×1024 (skin), 1024×512 (cape) | 64×64 only (validation rejects HD) | HD skins rejected | Minor |
| 15 | **Animation/Idle** | Full skeleton with cross-fade, sub-anims, gaze, squash, swing | Simple idle (sway, breath, head glances) | Visual difference, not a glitch | Minor |
| 16 | **Coordinate system** | Origin at feet, +Y up, model faces +Z, GLTF 180°Y rotated | Same (SkinRig.kt) | ✅ Match | — |
| 16a | Model origin | Feet at y=0, hips at y=16, shoulders at y=23, neck at y=24 | Same (SkinRig.kt) | ✅ Match | — |
| 17 | Cape attachment | Box origin (0,12,-2) → model (0,24,-2), mesh offset (0,-8,+0.5), rotation.y = π | Same (CapeGeometry.kt, SkinRig.kt) | ✅ Match | — |
| 18 | Output format | Pre-multiplied RGBA, box-filter downsample | Pre-multiplied RGBA, Canvas drawBitmap | ✅ Match | — |

## 4. Glitch Root Causes (from table)

| Glitch | Root Cause (ZL2 vs Ours diff) |
|--------|-------------------------------|
| **Dark slab across lower cape** | (1) No z-buffer: cape outer face drawn behind head/body when it should occlude; (3) Cape sorted by `paintRank` not camera depth |
| **Stacked white slabs at feet** | (1) No z-buffer: leg/body intersections resolved by arbitrary sort order; (2) No back-face culling on base layer |
| **Neck band (hat/jacket vs head/body)** | (11) Different inflation (1px vs 0.5px); (12) Overlay culled incorrectly; (13) Outer layer sorted by boxDepth not camera depth |

## 5. License & Integration Strategy

| Item | Detail |
|------|--------|
| **ZL2 License** | GPL-3.0 (PetController.kt header, root COPYING) |
| **PetLib Rust** | GPL-3.0 (same repo) |
| **Integration Options** | |
| A. Re-implement in Kotlin | ✅ Preferred: same algorithm, our own code, no license conflict |
| B. Use Rust via JNI | ❌ GPL-3.0 → entire app must be GPL-3.0 (source availability, notices) |
| C. Dynamic load Rust .so | ❌ Still GPL-3.0 derivative work |

**Decision**: Re-implement ZL2's software rasterizer with per-pixel z-buffer in pure Kotlin (JVM), matching constants and behavior exactly. No GPL code copied.

## 6. Implementation Plan

### Phase 1: Core Rasterizer (JVM)
1. Create `SoftwareRasterizer.kt` with:
   - Per-pixel depth buffer (`FloatArray` width×height, initialized to `-INFINITY`)
   - Triangle rasterization with barycentric coords + 256× fixed-point edge bias
   - Pre-multiplied RGBA blending with alpha cutoff (16)
   - Pre-multiplied alpha blending (Lambertian shade × LUT)
   - Box-filter downsample to target size

### Phase 2: Geometry & Constants
1. Port ZL2 box geometry exactly (SkinRig.kt → new `Zl2Geometry.kt`):
   - Box sizes, UV coords, inflation values
   - Cape: 16×10×1, y=24, tilt 6°, offset (0,-8,+0.5), rotation.y=π
   - Slim arms: 3px, classic 4px
   - Overlay inflation: hat +1, others +0.5

### Phase 3: Camera & Projection
1. Weak perspective matching ZL2:
   - Camera forward = (30,23,70).normalize()
   - Scale = width / 44
   - Center at (width/2, height/2) with vertical offset

### Phase 4: Lighting (optional, can defer)
1. Lambertian + sRGB LUT (128 levels) — can start with flat shading

### Phase 5: Integration
1. Replace `SkinModelView` Canvas drawing with `SoftwareRasterizer` output
2. Keep `SkinModelView` API (drag, zoom, full-screen) unchanged
3. Feed rasterizer output Bitmap to Canvas via `drawBitmap`

### Phase 6: Tests
1. Geometry/UV test: assert constants match ZL2 numbers
2. Reference sweep: rasterize at 128×128, compare painter vs z-buffer over 360°×360° (7° steps)
2. Target: zero mismatch for default skin ±cape; tiny isolated pixels for others

## 7. Performance Estimate

| Config | Pixels | Triangles | Est. Cost (arm64) |
|--------|--------|-----------|-------------------|
| 128×128 | 16,384 | ~78 (13 parts × 6 faces / 2) | ~0.5 ms/frame |
| 256×256 | 65,536 | ~78 | ~2 ms/frame |
| 512×512 | 262,144 | ~78 | ~8 ms/frame |

- Single-threaded Rust reference: ~1-2ms at 150×170 on mobile
- Kotlin ~3-5× slower → 3-10ms at 256×256
- **Acceptable** for 60fps if ≤16ms/frame budget
- Can add multi-threaded bands (ZL2 does row bands) if needed

## 8. Decision Log

- **2026-11-16**: Analysis complete. Re-implement in Kotlin. License GPL-3.0 prevents JNI use. Target: zero painter/z-buffer mismatch for default skin ±cape.