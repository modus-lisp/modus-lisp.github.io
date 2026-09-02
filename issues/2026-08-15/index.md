# Two packages, one variable

**This Week in Modus №23** · 8–14 August 2026 · 273 commits · 13 repos

<https://modus-lisp.github.io/issues/2026-08-15/>

Symbols learned which package they belong to back in June. This week it emerged that nine of the tables *keyed* by those symbols had never been told — so `da::*v*` and `db::*v*` were the same entry, and one of the nine was dispatching the wrong generic function in silence.

| Commits | Repos moved | Source files | Lines added | of 230 passing |
|---|---|---|---|---|
| 273 | 13 | 356 | +48k | 230 |

---

*modus / nine registries*

## Two packages, one variable

[№ 13](https://modus-lisp.github.io/issues/2026-06-06/) gave symbols a home package, which is the half of the problem everybody thinks of: two symbols with the same name in different packages must be different objects. This week is the other half. A Lisp keeps a lot of tables — what is a generic function, what is a macro, what is a type name, what is a special variable, which class a condition belongs to, what a `setf` of this name expands into — and nine of them were keyed by name alone.

The generic-function registry is worth dwelling on, because its primary lookup was already package-exact. The blindness lived entirely in a secondary hash pass that exists to bridge two representations of the same symbol, and that pass compares a hash of the plain name. Probed directly: the hash of `ma::zz` and the hash of `mb::zz` are the same number. A generic function defined in one package could be dispatched from another, silently, with no error anywhere.

The same shape in `deftype`, symbol macros, `setf` expanders, compiler macros, `defstruct`, method combinations, the CLOS class and condition registries — which fixed cross-package `handler-case` — the runtime macro expanders, and special variables, where a global *write* had to start keying by name rather than by the symbol object. Each got a battery covering both directions, and the family was declared closed.

This is the third time in this archive that a hash has been mistaken for an identity: the subtag that could not tell a one-slot object from a three-slot one in [№ 7](https://modus-lisp.github.io/issues/2026-04-25/), the intern table that collided on case in [№ 14](https://modus-lisp.github.io/issues/2026-06-13/), and now nine registries at once. A hash is a way to find something quickly. It is not what the something is.

---

*modus / #242, #245, #248, #251, #252*

## The build starts checking itself

Modus has a standing limitation: a `defvar`’s initialiser only runs if this image’s entry point calls the routine that runs them. Readers of [№ 20](https://modus-lisp.github.io/issues/2026-07-25/) have already watched it cost a week — that is the uninitialised counter that held up the self-hosting cycle. The commit that lands this week’s fix says it has produced five separate production bugs, each found the hard way. So it became a build check that fails the build and names the variable.

> The unit of analysis is the assembled blob one build script hands to the image builder, not a source file — because the last bug was a function that existed and was correct in every file, and that three of ten build scripts simply never called. A per-file check would have passed.

> — modus — 09355d2

It immediately found a live one: the shipping AArch64 CLI never called the routine at all, leaving 150 globals unbound. Its siblings arrived over the next three days — a sweep of every first-party file for read errors, which found two unescaped quotes that had made two build scripts unbuildable; a check for `let` of an unregistered special, which found seventeen live bugs; and a compiler-warning histogram with a ratchet, so the number can only go down.

Reproducibility got the same treatment. The compiler was drawing generated symbol names from the host’s counter, which made the host’s state an input to the image; it now has its own, collision-proof by construction rather than by luck, and the three pins that had been holding the old behaviour were dropped.

Turning a rule people are remembering into a rule the build enforces is the same move as the register-invariant check in [№ 21](https://modus-lisp.github.io/issues/2026-08-01/), and it is the natural end of the road this project has been on since it stopped trusting its own test harness in [№ 6](https://modus-lisp.github.io/issues/2026-04-18/).

---

*modus / three CLIs become one*

## One lineage, three architectures

The i386 CLI was a third build lineage. It loaded the loader directly and re-derived the entire source set by hand — its own scanners, emitters, overrides and boot sequence, all running parallel to the shared ones. The consequence was not stylistic: it baked zero library surface, so the twenty-two-library ladder and alexandria’s own test suite could not run on 32 bits at all. Converged onto the shared assembly, it went from 2,145 lines to 384. The aarch64 CLI went the same way the day before — and that convergence *fixed four defects* by itself, because the fixes had only ever landed in one of the two copies.

With all three architectures finally measurable, i386 got x64’s 16 MB collector guard band and twelve of its thirteen faults became clean exits; aarch64 got the same invariant asserted at build time rather than relied on. A zero-initialisation hypothesis for the remaining i386 fault was tested and *disproved*, and the disproof recorded rather than dropped.

### And the ladder

Underneath it, a long list of conformance defects, each found by a real library refusing to load. `check-type` was a no-op at runtime. `with-standard-io-syntax` was silently a `progn` in clean images. `read-sequence` and `write-sequence` parsed their keyword arguments positionally. `catch`/`throw` and cross-unit `return-from` truncated to a single value. A stale multiple-value count leaked out of a short-circuit tail as phantom extra values. `assert` was not a macro, so it evaluated its places. `do` dropped a step form that was literally NIL. `char-code-limit` did not exist. The little-endian feature was not advertised, which is why babel raised a program error.

A package symbol table became an O(1) index, recovering three reader tests above U+00FF and taking one gate shard from 139 seconds to 21. And the week closed with alexandria’s own 230 tests all running to completion and passing — the skip list kept but empty, because an empty list is the honest inventory of what does not terminate. That is the same instinct as the pre-stamps in [№ 10](https://modus-lisp.github.io/issues/2026-05-16/): keep the shape of the admission even when it is no longer needed, so nobody has to rediscover why it was there.

---

*glass warp / 44 commits*

## A seat is who is watching

A glass port had been two things wearing one name: the session — the applications, their windows, the pixels in them — and the single person watching it, with a screen, a pointer, a keyboard, a focus, an open menu and an arrangement of windows. Nothing forced them apart while there was one viewer, and everything about a second one is the second half.

So a seat is now its own thing, holding what a second watcher must not get a second copy of: its own framebuffer and size, its own wallpaper rasterised *at* that size (a phone and a desktop want the same picture, not the same pixels), its own pointer, its own focus. A second person can be on the same windows, arranged their own way. The menu belongs to its window, on whichever screen is watching. My mix is not yours, and my microphone is mine.

The same idea arrived in warp from the other end, and the commit that records it is unusually candid about having been wrong first.

> The boundary was one level too high. Sharing a projection necessarily shares scroll *and* window size, which makes “independent scroll over one projection” unimplementable. The projection should hold the query; present, layout, diff and encode belong to the consumer.

> — warp rule 8 — d8a00f5

The forcing argument is that a DOM consumer lays out in a browser with its own viewport, while a token consumer has no extents at all — which is the same “macroblocks and tokens are peer encodings” claim warp made for itself in [№ 21](https://modus-lisp.github.io/issues/2026-08-01/), now meeting the code and correcting it. With that settled, warp grew a third encoding — a browser’s DOM, over its own protocol and nothing else — moved the protocol into a core that loads without glass, and reached rule 9: an app is a bundle of facets and the consumer chooses.

---

*webrtc-data warren operandi / 38 commits*

## The box answers its own door

The phone client stopped being a demo. The box got an identity of its own and refuses to run without one; admission became the desktop’s answer, with the gateway merely asking for it. A terminal list and then a file browser arrived on the connection the phone already had. A small stable shell lives on a static host and pulls the rest of the client from the box, so the part that must never break is the part that almost never changes. And a pile of real-world corrections: iOS restores a tab without ever firing a visibility change; the page says whether the link is still there; a reconnect stays in the corner; a feature nobody has used yet does not get to take the desktop down.

warren moved the box’s identity and its message bot out of the gateway and into the desktop, stopped listening on every interface, and fixed the banner that went on saying `0.0.0.0` after the bind had moved to loopback. And operandi grew a headless mode over encrypted direct messages — greeting the operator on startup if asked, holding a fixed admission floor so nothing arrives out of order, splitting a long reply rather than truncating it — with sessions becoming an append-only tree whose state is derived by replay, and an engine that stopped fabricating cost numbers and started surfacing provider errors.

---

*across the workspace*

## One DEFLATE, and ten repos that stopped guessing

- scribe dropped its own compression and reads web fonts through cram, vendored as a submodule. cram had just learned gzip and checksums — and fixed stored blocks over 64 KB, which were being silently truncated. That is the fourth library to shed a codec for the workspace’s own since [№ 19](https://modus-lisp.github.io/issues/2026-07-18/).

- On 12 August, ten repositories committed the same one-line change: find sibling repos relative to the script, not to a home directory that happens to exist on one machine. A workspace that runs only where it was written is not a workspace.

- glass stopped calling `setsid` unconditionally, which does not exist on macOS or the BSDs.

---

**Method.** Commits by *author* date, 8 to 14 August 2026, across the 42 git repositories in the modus-lisp workspace. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps — of which there were 380,000 lines this week, swept in by a `git add -u` and untracked again two days later. Produced with `bin/week 2026-08-15 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
