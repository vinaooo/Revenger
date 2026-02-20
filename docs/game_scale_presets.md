# Game Screen Alignment Configuration

This document describes the new alignment system implemented in February 2026.
It replaces the previous direct use of `gs_inset_portrait` and
`gs_inset_landscape` with a simpler set of configuration options.

## Resources available in `game_scale.xml`

```xml
<string name="gs_align_horizontal">center</string>        <!-- left|center|right -->
<string name="gs_align_vertical">center</string>          <!-- top|center|bottom -->
<integer name="gs_camera_hole_pct">0</integer>           <!-- 0..99 percentage for camera margin -->
<integer name="gs_align_offset_pct">0</integer>           <!-- 0..99 percentage for alignment movement -->
```

### How it works

* **Camera margin** (`gs_camera_hole_pct`) is applied to the edge where the
  front camera is located:
  * portrait → top
  * landscape → left or right, depending on rotation

* **Horizontal/vertical alignment** requests that the viewport be pushed toward
  that edge within the remaining area. The same percentage value
  (`camera_hole_pct`) is used for the alignment; if the camera and alignment
  margins point to the same side the effect accumulates.

* If both alignments are `center`, the game fills the available area (even if
  a camera margin exists).

### Typical combination example

```xml
<string name="gs_align_vertical">top</string>
<integer name="gs_camera_hole_pct">8</integer>
```

In portrait the game will be moved down 8% to avoid the camera hole and will
also be "anchored" at the top. Rotating to landscape, the same configuration
will leave 8% spacing on whichever side the camera is on (right/left).

## Compatibility notes

Old APKs that still include `gs_inset_portrait`/`gs_inset_landscape` will
continue to work unchanged. New builds should migrate their templates to the
new tags. No additional client code adjustments are required.
