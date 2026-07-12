Implement the spiral-based destination candidate search as a separate step. Do not add biome selection yet.

Replace the current random destination-coordinate selection with a lazy, deterministic square-spiral sequence of transient search anchor coordinates centered on world coordinate `(0, 0)`.

Use the configured minimum distance `d` as the spacing between adjacent anchors. The default value of `d` should remain `10,000` blocks.

The sequence must begin exactly as follows:

```text
(0, 0)

(-d, 0)
(-d, d)
(0, d)
(d, d)
(d, 0)
(d, -d)
(0, -d)
(-d, -d)

(-2d, 0)
(-2d, d)
(-2d, 2d)
(-d, 2d)
(0, 2d)
...
```

Each orbit should trace the perimeter of the next larger square and finish at the southwest corner before beginning the next orbit at `(-(r + 1) * d, 0)`.

The anchors are only coordinates produced by an iterator or generator. Do not persist them, register them, generate chunks at them, or represent them as world objects.

At the start of each destination search:

1. Take a frozen snapshot of the region files that already exist.
2. Iterate through the spiral anchors in order.
3. Reject an anchor if it is less than `d` blocks from any region in that frozen snapshot.
4. Use the first anchor that satisfies the minimum-distance rule as the input to the existing landing-site and destination-portal placement flow.

The distance test must use the actual block-space bounds of each region file, not merely the region center. Preserve the distance metric already used by the project unless there is a clear correctness bug.

Region files created while this destination search is running must not affect the current search. Concurrent destination searches do not need special coordination.

Preserve the existing final validation of the actual landing position and destination portal anchor against the same frozen region snapshot. Passing the spiral-anchor check does not by itself guarantee that a later adjusted landing position is valid.

Remove or stop using any destination constraint based on proximity to existing registered portals. The relevant constraint is distance from the region files present when the search began.

Do not add biome-search code in this change. Do not change the portal registry format. Do not add registry migration logic; existing saved registry data does not need to be migrated.

Please add focused tests for the spiral iterator, including:

* the exact initial sequence shown above;
* correct completion of the first and second square orbits;
* no duplicate coordinates;
* every coordinate lying on the expected `d`-spaced grid;
* lazy production of coordinates without precomputing an unbounded list.

Also add or update tests for rejecting anchors that are too close to the frozen region snapshot and accepting the first eligible anchor in spiral order.
