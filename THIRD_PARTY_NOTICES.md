# Third-party notices

## Swiss Ephemeris

This distribution includes:

- `libs/swisseph-2.00.00-01.jar`, derived from Thomas Mack's Java port of
  Swiss Ephemeris Free Edition 2.00.00.
- `third_party/swisseph/swisseph-2.00.00-java-src-01.zip`, the corresponding
  unmodified source archive published alongside that JAR by Thomas Mack.
- Swiss Ephemeris data files in `ephe/`.

The bundled JAR was verified byte-for-byte against the file currently
published by Thomas Mack:

```text
SHA-256  8699b7385be4fb95278421673b4417039d7428d2fbb5b13855f0cf85aaf9d64d
```

The corresponding source archive has this checksum:

```text
SHA-256  29f0d758659351cacb8cbf18f337c6272dcef8b26c7b80ddc95fd048f464cc6d
```

The source archive's `README` documents the required precompilation step. For
the full Java SE build represented by the ready-to-use JAR, transit routines
are enabled and JPL support is retained:

```text
javac Precompile.java
java Precompile -qfc -iswesrc -iprogs -DTRANSITS
```

Swiss Ephemeris copyright:

> Copyright (C) 1997–2008 Astrodienst AG, Switzerland. All rights reserved.

Authors of Swiss Ephemeris: Dieter Koch and Alois Treindl.

The bundled historical version was made available under a dual-licensing
model:

1. GNU General Public License version 2 or any later version; or
2. Swiss Ephemeris Professional License.

For this distribution, the free-software option is selected. OpenAstroAPI is
therefore licensed under the compatible GNU Affero General Public License,
version 3 or any later version.

Swiss Ephemeris is provided without warranty. The names of its authors and
copyright holder must not be used to promote derived products without written
permission. The original historical licensing notice previously stored as
this project's `LICENSE` is preserved below.

---

Swiss Ephemeris is distributed with NO WARRANTY OF ANY KIND. No author or
distributor accepts any responsibility for the consequences of using it, or
for whether it serves any particular purpose or works at all, unless stated
otherwise in writing.

The software developer using any part of Swiss Ephemeris must choose between
the GNU public license and the Swiss Ephemeris Professional License before
distributing software containing Swiss Ephemeris or activating a public
service using it.

The license grants the right to use, copy, modify, and redistribute Swiss
Ephemeris only under the applicable license conditions. Copyright and license
notices must be preserved on all copies.

The authors of Swiss Ephemeris have no control over derived works or services
created by other programmers using Swiss Ephemeris functions.

The trademarks “Swiss Ephemeris” and “Swiss Ephemeris inside” may be used for
promoting software, products, or services containing Swiss Ephemeris.

Project and licensing information:

- https://www.astro.com/swisseph/
- http://www.th-mack.de/international/download/
- https://www.gnu.org/licenses/old-licenses/gpl-2.0.html
