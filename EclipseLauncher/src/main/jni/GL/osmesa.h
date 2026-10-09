/**
 * Minimal OSMesa declarations for this project.
 *
 * Defines the context handle type and the format / pixel-store constants
 * referenced by this repository's native sources. The OSMesa entry points
 * themselves are resolved at runtime through dlsym by the bridge loader,
 * so no function prototypes are needed here.
 */
#ifndef OSMESA_H
#define OSMESA_H

#include <GL/gl.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Opaque handle to an off-screen rendering context.
 */
typedef struct osmesa_context *OSMesaContext;

/* Format argument for OSMesaCreateContext */
#define OSMESA_RGBA             GL_RGBA

/* pnames accepted by OSMesaPixelStore */
#define OSMESA_ROW_LENGTH       0x10
#define OSMESA_Y_UP             0x11

#ifdef __cplusplus
}
#endif

#endif /* OSMESA_H */
