# PLAN: ZL2-Exact 3D Skin/Cape Renderer

## IN SCOPE
- Replace painter's algorithm with per-pixel z-buffer software rasterizer (matching ZL2 PetLib)
- Match ZL2 geometry constants, UV layouts, overlay inflation, cape geometry, camera/projection
- Keep existing SkinModelView API (drag, zoom, full-screen, Microsoft view-only, debug overlay)
- Keep all existing tests green; adapt only if internals changed (with written justification)
- Pure JVM implementation (no JNI, no GPL code)

## OUT OF SCOPE
- Lighting (start with flat shading, add Lambertian later if needed)
- HD texture support (beyond 64×64)
- Animation system (keep existing idle sway/breath)
- Visual redesign

## PHASES

- [ ] Phase 0: Analysis & constants verification (DONE - SKIN_RENDER_ZL2.md)
- [ ] Phase 1: Create JVM SoftwareRasterizer with per-pixel z-buffer
  - [ ] Triangle rasterization with barycentric coords + 256× edge bias
  - [ ] Per-pixel depth buffer (FloatArray[width×height])
  - [ ] Pre-multiplied RGBA blending with alpha cutoff (16)
  - [ ] Box-filter downsample to target size
  - [ ] Back-face culling for base layer (inflate==0)
- [ ] Phase 2: Port ZL2 geometry constants exactly
  - [ ] Box sizes, UV coords, inflation values (Zl2Geometry.kt)
  - [ ] Cape: 16×10×1, y=24, tilt 6°, offset (0,-8,+0.5), rotation.y=π
  - [ ] Slim arms: 3px vs 4px
  - [ ] Overlay inflation: hat +1, others +0.5
  - [ ] UV layouts matching CapeGeometry.kt and SkinRig.kt
- [ ] Phase 3: Camera & projection matching ZL2
  - [ ] Weak perspective: camera forward=(30,23,70).normalize(), scale=width/44
  - [ ] Model transform: body_chain, hip, head pivot, arm/leg pivots
  - [ ] Cape transform: body_chain * translation(0,12,-2) * rotation(cape.x, π, cape.y) * translation(0,-8,+0.5)
  - [ ] Anchor squash at hips, global yaw
- [ ] Phase 4: Sorting & culling matching ZL2
  - [ ] Parts list: base layer (inflate=0) first, then outer layer (inflate>0) sorted by camera-space depth
  - [ ] Base layer: back-face culling
  - [ ] Outer layer: no culling, sorted by camera-space depth (far→near)
- [ ] Phase 5: Integration with SkinModelView
  - [ ] Replace Canvas drawing loop with SoftwareRasterizer
  - [ ] Feed rasterizer output Bitmap to Canvas via drawBitmap
  - [ ] Keep drag/zoom/full-screen/Microsoft mode/debug overlay unchanged
- [ ] Phase 6: Reference sweep test
  - [ ] Rasterize at 128×128, painter vs z-buffer over 360°×360° (7° steps)
  - [ ] Configs: default skin, full-overlay, slim, each ±cape
  - [ ] Report mismatch fraction per angle, worst 10 angles
  - [ ] Target: zero mismatch for default skin ±cape; tiny isolated pixels for others
- [ ] Phase 7: Regression tests
  - [ ] All existing cape/rotation/dialog/account tests stay green
  - [ ] Adapt test assertions only if internals changed (with justification)
- [ ] Phase 8: CI, APK, report
  - [ ] Push, wait for green CI
  - [ ] Collect APK/test-report links
  - [ ] Report before/after mismatch numbers, ZL2-vs-ours table result, claims table

## CI Budget
- Max 6 CI runs

## Key Files to Create/Modify
- `app/src/main/java/me/shadow/eclipselauncher/ui/character/SoftwareRasterizer.kt` (NEW)
- `app/src/main/java/me/shadow/eclipselauncher/ui/character/Zl2Geometry.kt` (NEW)
- `app/src/main/java/me/shadow/eclipselauncher/ui/character/SkinRig.kt` (MODIFY - replace projection/sorting)
- `app/src/main/java/me/shadow/eclipselauncher/ui/character/SkinModelView.kt` (MODIFY - use rasterizer)
- `app/src/test/java/me/shadow/eclipselauncher/ui/character/RenderOrderSweep.kt` (MODIFY - new sweep)