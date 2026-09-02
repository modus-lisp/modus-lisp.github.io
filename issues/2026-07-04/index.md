# Build the oracle first

**This Week in Modus №17** · 27 June – 3 July 2026 · 437 commits · 14 repos

<https://modus-lisp.github.io/issues/2026-07-04/>

Ten repositories appeared this week, among them a web engine that finished at Acid3 100/100, a JavaScript engine that finished at 88% of test262, and a text rasterizer with hinting. All three began the same way, and it is the way the last three months taught this project to begin.

| Commits | Repos moved | New repos | Lines of code | Lines of test vectors |
|---|---|---|---|---|
| 437 | 14 | 10 | +93k | 156k |

---

*the method / 28–29 June*

## Build the oracle first

Here are the opening commits of two separate repositories, written a day apart, by a project that has spent three months learning what happens when you grade your own work.

*weft — 28 June, and scribe — 29 June*

```
weft: start a self-sovereign web engine — P0 WHATWG URL parser
P0 encoding: kernel + UTF-8 decoder + differential oracle + worker stubs
P0 encoding: 7 decoders built by a parallel agent swarm
P0 encoding complete: 11/11 charsets

scribe: scaffold + gamma-correct linear-light compositing
scribe: adopt SWARM — sfnt kernel + ttx oracle + W1 table wave
```

In both cases the second or third commit is not a feature. It is the thing that will decide whether the features are right: a differential oracle for character encodings in one, the `ttx` font dumper in the other. Only then do the workers fan out, each taking one decoder or one table, looping against the oracle until it stops improving.

That order is the accumulated lesson of this dispatch so far. [№ 5](https://modus-lisp.github.io/issues/2026-04-11/) adopted somebody else’s test suite so the implementation could not grade itself. [№ 6](https://modus-lisp.github.io/issues/2026-04-18/) discovered that a suite is worthless if the harness counting it can lose 17,587 tests without saying so. [№ 8](https://modus-lisp.github.io/issues/2026-05-02/) built a fuzzer as an instrument rather than chase a bug that moved when observed. [№ 14](https://modus-lisp.github.io/issues/2026-06-13/) pointed a real library at the implementation and used the form it died on as the measure. [№ 15](https://modus-lisp.github.io/issues/2026-06-20/) called into Bitcoin Core’s own compiled library and compared verdicts.

What is new here is that the oracle now comes before the code, and that it makes fanning work out possible at all. Nobody can supervise seven parallel character-set decoders by reading them. An oracle can.

---

*weft / 181 commits, from nothing*

## A URL parser on Sunday, Acid3 on Friday

The first commit is a WHATWG URL parser at 97.2% conformance. The second raises it to 97.7% by handling spaces in an opaque path before the query and fragment. Then character encoding — kernel, oracle, workers — reaching all 36 decoders the specification lists, including the Chinese, Japanese and Korean sets that need real tables. Then the HTML tokenizer, which lands at 100% of the html5lib suite in a single commit.

Tree construction is where the week’s texture shows. HTML’s parsing algorithm is a pile of special cases accumulated over thirty years, and there is no way to be clever about it — only to implement each rule and watch the number move:

*weft — the tree builder, in order*

```
DOM node kernel + core tree construction
raw-text/RCDATA reentrancy (script/style/title/textarea)   173 → 187
stray </p> synthesizes empty <p>; dd/dt auto-close          187 → 190
TABLE insertion modes + foster parenting                   190 → 212
scope bounds, foster setf bug, pre/textarea newline        224 → 246
frameset modes + form pointer + button scope               246 → 261
adoption agency algorithm + active formatting elements     261 → 281
```

That is the same ratchet the ANSI suite produced in [№ 5](https://modus-lisp.github.io/issues/2026-04-11/) and the same per-commit accounting as [№ 8](https://modus-lisp.github.io/issues/2026-05-02/), arriving in a brand-new repository on its second day, because by now it is simply how work is done here.

From there the week runs through the CSS cascade, layout and painting into scripting: the DOM bound to a JavaScript engine, hit testing, box-tree rendering, viewport scrolling and event dispatch, canvas gradients with alpha compositing, and SVG in an `<img>`. The last Acid3 cluster closes on Friday — a `NodeIterator` mutation case and scripts inside XHTML frames — taking it from 98 to 100. And one commit purely for taste: the Big5 table rewritten as a data literal instead of fourteen thousand assignment statements.

---

*shuttle / 80 commits, from nothing*

## 12.4%, then 88%

shuttle starts on Tuesday as a skeleton — a value model, a bytecode virtual machine, and the seam through which a host binds its own objects. Control flow and a test262 runner follow the same day, which produces the first honest number: 12.4% of 47,058 tests. It closes the week at 41,404, or 87.99%.

The commits in between record where the count stood, and those history rows are kept in the repository rather than remembered — the same instinct as the status lines in [№ 14](https://modus-lisp.github.io/issues/2026-06-13/), and for the same reason: a number that only exists in somebody’s memory cannot be checked and cannot be regressed against.

Most of that climb is not the language but the library around it: property introspection and the abstract operations the specification is written in, `Symbol`, regular expressions, BigInt, and — because test262 does not grade on charm — the internationalisation collators, segmenters and display names. The last commits of the week turn back toward weft, exporting the host-binding predicates and a microtask drain, which is what a DOM needs to run scripts on a real event loop.

---

*scribe gesso stencil loom / 68 commits, four new repos*

## Everything under the pixels

scribe opens with gamma-correct linear-light compositing, which is the right thing to get right first: blending in the wrong colour space is invisible until it is everywhere. By that evening it has the metric, naming and character-map tables green against `ttx`, outlines from the TrueType tables, an analytic rasterizer, and cubic charstrings with flex from the Compact Font Format.

What is striking is how the later commits describe themselves. Shaping is “= HarfBuzz”. Variable fonts and advance-width variations are “= fontTools instancer”. The claim is not that a feature exists but that it agrees with the reference implementation, which is exactly the form of [№ 15](https://modus-lisp.github.io/issues/2026-06-20/)’s zero divergence against Bitcoin Core. By Friday: WOFF and WOFF2 decompression that is outline-exact, hinting, stem darkening, a high-resolution demo comparing one-times and two-times rendering, and bounded glyph caches.

Friday also brings the three pieces that turn a rasterizer into a browser. gesso is 2D vector graphics standing on scribe’s coverage contract, and its second commit fixes stroke joins whose winding was cancelling them out under a non-zero fill. stencil parses SVG and renders it through gesso. loom is a window — an SDL shell over weft with DOM input, launched with a four-gigabyte heap and a large control stack because, as the commit says, the JavaScript engine recurses deep.

---

*modus / 66 commits / WS3*

## Production eval changes hands

While all that was being built, Modus quietly changed how it runs Lisp. It has had two evaluators since [№ 6](https://modus-lisp.github.io/issues/2026-04-18/) built a tree-walking interpreter as one of its layers, alongside the path that compiles to bytecode and interprets that. Two evaluators means every semantic fix lands twice, and the one that misses it is the one somebody hits.

This week the bytecode path reached parity and became the default. Getting there is a list of the places parity was missing: `catch` and `throw` through the interpreter’s own jump mechanism, closures that capture and mutate, secondary values across the runtime-call bridge, higher-order calls with a function object, `defstruct`, `handler-case` catching a signalled condition, CLOS initarg validation and compiler macros, and top-level `defun` persistence — the keystone, because ASDF needs it and [№ 14](https://modus-lisp.github.io/issues/2026-06-13/) had made ASDF the frontier.

Two pieces of method matter more than the fixes. An in-image differential gate runs the same form through both evaluators and compares, so parity is measured rather than asserted. And the flip itself is reversible behind a switch that is byte-identical when off, so the default can move and move back. Compile caching, worth about twenty times on repeated forms, is what makes running production evaluation through a compiler affordable at all.

---

*also this week / four more new repositories*

## Compression, Tor, Nostr, and a timing probe

- brotli-pure and zstd-pure both appear on Sunday and both reach full codecs the same day. Brotli is not decoration: it is what WOFF2 web fonts arrive compressed with, which is to say scribe needs it, which is to say weft needs it. The dependency was created three days before the thing that required it.

- cl-tor goes from a phased plan to three-hop circuits, then streams and a SOCKS proxy — curl through Tor works — then consensus signature validation with weighted exit-aware path selection, flow control, guard persistence, and relay-family and subnet path constraints. Six phases in one day.

- secp256k1-fast, eleven times faster since [№ 15](https://modus-lisp.github.io/issues/2026-06-20/), made its secret operations constant-time — and then tested that *property* rather than the results, with an operation-count gate and a timing probe. Both cl-nostr, new on Sunday, and cl-consensus recorded the change by rewriting their security disclaimers, which is the honest way to benefit from somebody else’s work.

- cl-consensus also vendored secp256k1-fast and pagetree as submodules, and bumped pagetree to pick up a fix for a quadratic read overflow — the first time in this archive that a bug in one repository is fixed on behalf of another.

---

**Method.** Commits by *author* date, 27 June to 3 July 2026, across the 42 git repositories in the modus-lisp workspace. Of the week's 248,840 added lines, 156,058 are vendored conformance corpora under `inspect/vectors/` and `inspect/test262/` — tokenizer vectors, URL test data, character-map tables dumped from the `ttx` oracle — checked in so the gates run offline; the tally above reports the remainder separately. Line counts also exclude generated acceptance-gate artefacts and committed scratch dumps. Produced with `bin/week 2026-07-04 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
