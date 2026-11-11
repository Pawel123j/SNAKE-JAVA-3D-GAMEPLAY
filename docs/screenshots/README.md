# Screenshots

This directory has no images yet. `ScreenshotTest` captures the running JavaFX
scene and only saves a gameplay shot after checking that `SCENE3D` is available
and the rendered world is not a uniform background. The software Prism
pipeline used by basic Xvfb smoke tests cannot draw this game's 3D scene.

Run the capture test on a machine with a JavaFX 3D-capable renderer:

```bash
mvn -B test -Dtest=ScreenshotTest -Dsnake.screenshots=true \
  -Dsnake.screenshots.dir=docs/screenshots -DfailIfNoTests=false
```

It drives the real application and writes menu, gameplay, pause, board-mode,
and game-over images. A failed renderer check is not a screenshot result.
