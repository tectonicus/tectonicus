# Headless rendering

By default Tectonicus creates its OpenGL context through a hidden window, which needs a desktop session
(X11 or Wayland on Linux). On a server, in a container, or anywhere else without a display, use one of the
options below. Both are set on the `<rasteriser>` element (or with the matching command line flag).

```xml
<rasteriser type="lwjgl" useEGL="true"/>
```

## EGL (recommended, Linux only)

`useEGL="true"` / `--useEGL` creates the context with EGL and picks a display in this order:

1. The default display, if an X11 or Wayland session is available.
2. The first EGL device, which renders on a GPU without any window system (the usual setup on headless
   NVIDIA or AMD servers).
3. Mesa's surfaceless platform, which renders on the CPU with llvmpipe when there is no GPU.

On machines with more than one GPU, set `eglDevice="N"` / `--eglDevice N` to choose one. The devices found are
listed in the debug log (`--logLevel DEBUG`), along with the display that was used.

EGL needs the system's EGL library, for example `libegl1` and `libegl-mesa0` on Debian/Ubuntu or `mesa-egl` on
Alpine. EGL is not available on Windows or macOS; there `useEGL` is ignored with a warning and the normal
offscreen context is used.

## OSMesa (compatibility fallback)

`useOSMesa="true"` / `--useOSMesa` renders entirely on the CPU through Mesa's OSMesa library, without any window
system. It uses the same llvmpipe software renderer as EGL's fallback, so it is not faster and produces the same
images. Use it only when EGL doesn't work:

- EGL is missing or won't initialize, for example on an older distribution or in a minimal image that has
  `libOSMesa` but no EGL library.
- You want CPU rendering on a machine that has a GPU, to get identical output on different machines or to rule
  out a graphics driver problem. EGL always prefers the GPU when one is available.
- Windows or macOS, as a last resort, if you supply an OSMesa build (`osmesa.dll` or `libOSMesa.8.dylib`). This
  is untested.

Mesa removed OSMesa in version 25.1, so newer distributions only ship it as a compatibility package
(`libosmesa6` on Debian/Ubuntu, `mesa-compat-libOSMesa` on Fedora) or not at all. To use a library that isn't
on the default search path, pass `-Dorg.lwjgl.opengl.libname=/path/to/libOSMesa.so.8` to Java.

If both `useEGL` and `useOSMesa` are set, EGL is used.

## Docker

[Dockerfile](Dockerfile) builds an Alpine image that renders with EGL. Without a GPU passed to the container
it renders on the CPU. The comments at the top of the file show how to build and run it. Paths in the config
must be the paths inside the container, and `minecraftJar` must be set because Tectonicus can't find the
Minecraft launcher's files there.
