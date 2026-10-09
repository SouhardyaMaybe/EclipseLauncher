/**
 * Minimal OpenGL declarations for this project.
 *
 * Defines only the base types, constants, and functions that the native
 * sources in this repository actually use. No extension definitions are
 * provided; nothing in this codebase needs them.
 */
#ifndef __gl_h_
#define __gl_h_

#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Base types
 */
typedef unsigned int    GLenum;
typedef unsigned char   GLboolean;
typedef unsigned int    GLbitfield;
typedef void            GLvoid;
typedef signed char     GLbyte;
typedef short           GLshort;
typedef int             GLint;
typedef unsigned char   GLubyte;
typedef unsigned short  GLushort;
typedef unsigned int    GLuint;
typedef int             GLsizei;
typedef float           GLfloat;
typedef float           GLclampf;
typedef double          GLdouble;
typedef double          GLclampd;
typedef char            GLchar;
typedef intptr_t        GLintptr;
typedef ptrdiff_t       GLsizeiptr;

/*
 * Constants used by this codebase
 */
#define GL_VENDOR               0x1F00
#define GL_RENDERER             0x1F01
#define GL_UNSIGNED_BYTE        0x1401
#define GL_INT                  0x1404
#define GL_RGB                  0x1907
#define GL_RGBA                 0x1908
#define GL_COLOR_BUFFER_BIT     0x00004000

/*
 * Functions used by this codebase (called directly or resolved at runtime
 * by the context-bridge loader)
 */
const GLubyte *glGetString(GLenum name);
void glFinish(void);
void glClearColor(GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
void glClear(GLbitfield mask);
void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height,
                  GLenum format, GLenum type, GLvoid *pixels);
void glReadBuffer(GLenum mode);

#ifdef __cplusplus
}
#endif

#endif /* __gl_h_ */
