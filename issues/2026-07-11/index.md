# Two things deleted

**This Week in Modus №18** · 4–10 July 2026 · 233 commits · 11 repos

<https://modus-lisp.github.io/issues/2026-07-11/>

Modus removed the tree-walking evaluator it wrote in April, having proved with an instrumented build that nothing was using it. And OpenSSL left the stack, five days after the library that replaces it existed at all.

| Commits | Repos moved | New repos | Lines removed |
|---|---|---|---|
| 233 | 11 | 3 | −15k |

---

*modus / 9–10 July / WS3*

## One evaluator

[№ 6](https://modus-lisp.github.io/issues/2026-04-18/) built a tree-walking interpreter as one of the layers of a Common Lisp, in a week that also produced the reader, the printer, the conditions and CLOS. [№ 17](https://modus-lisp.github.io/issues/2026-07-04/) made the bytecode path the default for production evaluation, behind a switch that could be flipped back. This week the walker was removed, and the way it was removed is the point.

It was first extracted into a file only the fork builds loaded. Then *quarantined* rather than deleted — the fallback restored, and a counter added to record every time anything reached for it. Only when that counter had stayed at zero across the full ANSI corpus and the library gauntlet was the file dropped.

> Every consumer ported: the x64-Linux gate, the gauntlet build, bare-metal x64, Linux/AArch64 verified natively on a Pi 5, and bare-metal AArch64. The deletion is census-gated — zero walker fallbacks, instrumented, across everything.

> — modus — 2257554

That is the same discipline as the pre-stamps in [№ 10](https://modus-lisp.github.io/issues/2026-05-16/), where a range of tests could only stop being marked failed once the wedge inside it had been root-caused: do not remove the bandage because you believe it is unnecessary, remove it because you measured that it is. Two regressions surfaced on the way, both about what an unresolved name emits — a function reference that could not be resolved was emitting a live but misaligned code pointer, and a duplicate `defun` re-triggered a whole crash class.

### Two more roots the collector could not see

[№ 14](https://modus-lisp.github.io/issues/2026-06-13/) found four things the garbage collector was not scanning — the keyword and package tables, the multiple-value area, and two bounds bugs. This week found two more of the same family, and they are worth stating together because the shape recurs:

- The multiple-value region and the closure environment register were being stored as raw machine words. The collector cannot distinguish a raw word from an integer, so it does not trace it — and anything only reachable through them moved out from under the program.

- The pre-interned symbols the signalling machinery uses lived in unscanned slots, so condition routing collapsed silently after a collection.

With one evaluator, the CLOS and ASDF work could finally proceed against a single target: initialisation methods honouring `&allow-other-keys`, accessor methods on generic functions, runtime `defvar` specials, CLOS class objects usable as type specifiers, and handler frames transparent to non-local exit. The register allocator learned to spill on overflow rather than fail, and the four bare-metal and hosted ANSI builds were re-forked from a common ancestor so a fix in one is a fix in all.

---

*seal cl-tor cl-consensus / 38 commits*

## OpenSSL leaves the building

On Saturday seal was created: a pure Common Lisp TLS 1.3 client with fail-closed certificate chain validation across RSA, ECDSA and Ed25519. By Tuesday it had a TLS 1.2 fallback for servers that decline 1.3, post-handshake key updates, and signature verification using Jacobian coordinates and Shamir’s trick — the same two optimisations that made secp256k1-fast eleven times faster in [№ 15](https://modus-lisp.github.io/issues/2026-06-20/), arriving in a second implementation because the curve is different.

By Thursday cl-tor had used seal to delete `cl+ssl` — which is OpenSSL behind a foreign-function interface — from its link layer, and cl-consensus had vendored seal to follow. Five days from “this library does not exist” to “the C dependency is gone”. That is what the no-FFI claim costs and what it buys.

The rest of cl-tor’s week is an onion service built from the bottom up: ed25519 group arithmetic and key blinding, the directory hash ring with its timing inputs and index formulas, fetching and decrypting a descriptor over a circuit, the rendezvous handshake, and an end-to-end connection to somebody else’s `.onion`. Then the same thing from the other side — building, signing and publishing a descriptor, establishing introduction points, accepting inbound connections. The cross-certificate work names its own acceptance criterion: descriptors valid for *stock* Tor clients, not merely for ours. Which is the differential-oracle instinct again, applied to a protocol rather than a test suite.

cl-consensus put it to work immediately — outbound peers routed through cl-transport, a v3 onion peer directory collected and persisted, its own `.onion` for inbound connections, and self-advertisement. A Bitcoin node reachable without a public address, four weeks after the node itself existed.

---

*loom weft / 123 commits*

## The Opera Mini approach

The neatest idea of the week is about 180 lines. The engine renders a page to a full-height PNG on the server; a minimal HTML client shows that raster in an image element so the browser scrolls it natively, and posts clicks back as coordinates. The server hit-tests, dispatches the DOM click or follows the link through the page’s navigation hook, re-renders, and serves the new raster. Click-through navigation verified over real TLS — which is to say over seal, four days old.

From there it became a service: a supervisor, a thread per connection so one crash cannot take the server down, rasters cached per generation, rendering at each request’s own viewport width so the result is readable on a phone, navigation driven from the URL hash so Back and Forward work, and an inspector panel streaming fine-grained navigation phases with the network broken down by resource kind. Two robustness rules arrived that any real browser needs: a script error degrades to a render instead of blanking the page, and if scripts collapse the render entirely, fall back to the static markup.

In weft, web fonts landed — `@font-face` downloaded and rendered, plus a replay of the Web Font Loader convention that a great many real sites still use — along with script itemization for font fallback, area-averaged downscaling, inline background painting, `object-fit` and `calc()`. scribe supported that from underneath with a comprehensive Noto set and Unicode-block script fallback, and lifted two gamma transfers out of a per-pixel exponentiation into tables.

---

*also this week*

## Two more repositories, and one fewer binding

- webp-pure arrived on Tuesday and was a VP8 key-frame decoder by the end of the day: the container, the boolean decoder and the specification’s tables, then the inverse transforms, then the full pixel pipeline through prediction, the loop filter and colour conversion.

- cl-transport was extracted so that “dial direct, through SOCKS5, or through Tor” is one interface with a pluggable backend, rather than a decision every caller makes for itself. cl-tor and cl-consensus both moved onto it the same week.

- loom replaced its SDL binding library with fifteen hand-written declarations. The reasoning is the same as everywhere else here — the binding generator needs a C header parser you build from source, which is a great deal of machinery to put a rectangle of pixels on a screen — and it is also the last week in this archive where SDL is the everyday way to look at a page.

---

**Method.** Commits by *author* date, 4 to 10 July 2026, across the 42 git repositories in the modus-lisp workspace. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps. Produced with `bin/week 2026-07-11 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
