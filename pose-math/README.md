# :pose-math

Pure Kotlin/JVM pose math for the Minecraft player rig. No Android types, no
renderer types, no dependencies.

## Coordinate convention (single source of truth)

- Right-handed coordinate system.
- `+Y` is up.
- `+X` is viewer-right (the character's anatomical left).
- `+Z` points toward the viewer (the character faces the viewer in rest pose).
- Column vectors: a point is transformed as `p' = M * p`, never `p * M`.
- `M = T * R` means apply `R` first, then `T`.
- Units are skin pixels: the model is 32 px tall, feet at `y = 0`, head top at
  `y = 32`. No model-unit conversion lives in this module.
- Positive rotations follow the right-hand rule:
  - `Rx(+θ)` maps `+Y` to `+Z` (and `+Z` to `-Y`).
  - `Ry(+θ)` maps `+Z` to `+X` (and `+X` to `-Z`).
  - `Rz(+θ)` maps `+X` to `+Y` (and `+Y` to `-X`).
- Euler order is yaw (Y), then pitch (X), then roll (Z): `q = q_roll * q_pitch *
  q_yaw`, applied to a column vector as `R_roll * R_pitch * R_yaw * v`.

## Rest-pose pivots (skin pixels, model space)

| Joint | Pivot |
| --- | --- |
| body | `(0, 12, 0)` |
| head | `(0, 24, 0)` |
| left arm (viewer right, `+X`) | `(5, 22, 0)` |
| right arm (viewer left, `-X`) | `(-5, 22, 0)` |
| left leg (`+X`) | `(2, 12, 0)` |
| right leg (`-X`) | `(-2, 12, 0)` |

Slim (Alex) skins do **not** change pivots. The narrower arm geometry and its
inward offset live in `MeshMapping`, which is tested separately. Renderer
offsets must never leak back into `Skeleton.pivots`.

Arm abduction is mirrored: `+roll` raises the `+X` (left) arm outward, so the
left arm allows `0..90°` and the right arm allows `-90..0°`.

## Forward kinematics

For the root joint:

```
M_root = T(p_root) * R_root * T(-p_root)
```

For every child joint:

```
M_child = M_parent * T(p_child) * R_child * T(-p_child)
p_world  = M_child * p_rest
```

`T(p) * R * T(-p)` rotates around the rest-pose pivot: a point `x` maps to
`p + R * (x - p)`. The pivot itself is a fixed point of its own rotation.

## Angles are the source of truth

`JointAngles` stores yaw, pitch and roll in radians. `Skeleton.clamp` clamps
the stored angles directly. Quaternions and matrices are derived from the
angles only when needed. There is deliberately no `Quaternion -> Euler`
conversion in this module: clamping through a quaternion round-trip breaks at
singularities.

## Layout

- `Vec3`, `Quaternion`, `Mat4` — math primitives.
- `Skeleton`, `SkeletonPose`, `JointAngles`, `JointLimits` — rig and FK.
- `MeshMapping` — skin-pixel box geometry relative to each pivot.
- `Ray`, `OrientedBox`, `RayBoxPicker` — ray/box picking.
