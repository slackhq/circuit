Counter – Mosaic
================

A [Mosiac](https://github.com/JakeWharton/mosaic) counter circuit implementation.

Usage:

```bash
$ ./gradlew :samples:counter:mosaic:installDist
$ ./samples/counter/mosaic/build/install/mosaic/bin/mosaic
```

Currently a Circuit implementation isn't fully working
until [Mosaic supports effects](https://github.com/JakeWharton/mosaic/issues/3), so for now
this sample has a partial implementation and defaults to a circuit-less implementation. This is here
as a demo + toe-hold for the future once the mentioned issue is fixed.

To force use of a Circuit implementation, add `--use-circuit` to the second executable command
above.
