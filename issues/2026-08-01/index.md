# 629 bytes of desktop

**This Week in Modus №21** · 25–31 July 2026 · 294 commits · 16 repos

<https://modus-lisp.github.io/issues/2026-08-01/>

On Monday there was a boolean entropy coder. By Tuesday night Safari was decoding frames of a live Lisp desktop from a VP8 encoder written out of the RFC. Five file formats were implemented from scratch this week, and every one of them was graded against the reference decoder rather than against itself.

| Commits | Repos moved | New repos | Source files | Lines added |
|---|---|---|---|---|
| 294 | 16 | 5 | 378 | +55k |

---

*webrtc-media webrtc-data / 72 commits, one new repo*

## 629 bytes of desktop

The commit sequence is the story, so here it is in order. Monday: secure RTP, the framing layer and a telephony codec, then full-duplex audio — the box beeps, the browser’s microphone comes back. Monday night: the boolean entropy coder of RFC 6386, the first piece of a codec. Then the transform and quantisation. Then a keyframe’s uncompressed header, byte-exact against ffmpeg. Then the first decodable frame — a solid grey keyframe, verified by libvpx. Then an alternating pattern confirmed on a real device: Safari decodes our frames.

Every rung there is judged by somebody else’s decoder, which by now is simply how this workspace builds a format — against `ttx` and HarfBuzz in [№ 17](https://modus-lisp.github.io/issues/2026-07-04/), against real git in [№ 19](https://modus-lisp.github.io/issues/2026-07-18/), against Bitcoin Core in [№ 15](https://modus-lisp.github.io/issues/2026-06-20/). A bitstream parser was even built on the decoder purely as a debug oracle, so that when real images encoded wrongly the fault could be localised to entropy coding rather than guessed at.

Tuesday was correctness and then colour: the end-of-block node and the second-order quantiser made exact, then chroma. And then the number in the headline.

> Inter frames built from exactly one mode — a zero motion vector against the last reconstruction. The prediction is the co-located pixels of the previous frame, with no motion search and nothing on the wire, so an unchanged macroblock collapses to a single skip bit. A static desktop costs 629 bytes instead of 76 kilobytes.

> — webrtc-media — 8c6bbad

What follows sharpens the same idea: use what the desktop already knows. The damage rectangles glass has been computing since [№ 20](https://modus-lisp.github.io/issues/2026-07-25/) feed the encoder directly, so a static frame encodes in a millisecond. A per-frame size bound rather than only an average rate. An adaptive quantiser with an idle refinement pass. And on Friday, one global motion vector per frame that a macroblock may choose instead of the zero one — so a scroll costs a mode bit rather than a re-encode of everything it touched, which is what had been producing seconds of mush after every scroll.

### And on the other end, a phone on a cellular network

The client side spent the week on things that only appear in the field: two-finger scroll-lock, natural touch scrolling, mid-session quality switching, a cursor ring with a real outline so it shows on light backgrounds, video that does not swallow touches. Enrolled devices renew themselves for a day, credentials expire visibly, and the device file — not process memory — is the source of truth.

The best-diagnosed failure was a connection that never formed on cellular, from a real session where the box *was* advertising a relay and the phone offered three of its own. Gathering was installing a permission for every remote candidate, and each call blocks. A modern browser on cellular offered sixty-six candidates from a handful of addresses — roughly 132 blocking round trips before anyone could answer. Permissions are now installed per unique address.

---

*folio gesso loom / 33 commits, one new repo*

## A PDF renderer, in a day

On Sunday folio went from nothing to a PDF renderer in twenty commits that read as a milestone list: the parser, the content-stream interpreter drawing through gesso, text and the standard fonts with glyphs from scribe, embedded and composite fonts, form and image objects — and then a conformance harness graded against pdfium, at which point the milestones stop and the real material begins.

Because what comes after the harness is what actual PDFs are made of: a fax decoder for scanned pages, exact Type1 charstring outlines, transparency with soft masks and blend modes, axial and radial shadings with colour functions, tiling patterns, form field recognition, a JBIG2 codec with its arithmetic decoder and symbol dictionaries, predefined character maps for the four Adobe CJK collections, and Type3 fonts whose glyphs are procedures.

gesso grew exactly what folio needed and no more — a per-pixel colour source across a clip, affine blits, offscreen group compositing, soft masks, the four non-separable blend modes — and loom wired it up, so a PDF now opens inline in the browser on a scrollable canvas. Three repositories, one afternoon, no new dependency.

---

*reed / 11 commits, one new repo*

## Three decoders in three days

An MPEG Layer III decoder on Tuesday, verified against a reference implementation at 24 kHz once an obscure scale-factor flag was applied. AAC with its two container formats on Wednesday. Then Opus, in pieces that each stand alone: the range decoder and packet framing, a conformance-verified CELT decoder, a SILK decoder covering three bandwidths in mono and stereo, hybrid mode with mode-switch redundancy, and Ogg demuxing to finish. Which is to say the whole of RFC 6716, in a week where two other codecs were also being written.

---

*modus / 104 commits / WS4, WS5*

## A third architecture, and three honest verdicts

i386 has been a bare-metal platform since [№ 4](https://modus-lisp.github.io/issues/2026-03-14/), where it was the ninth architecture and half of the fixpoint proof. What it has never had is the Common Lisp stack, and this week brought that up from nothing: seventeen missing opcodes, a hosted 32-bit target, then an image that boots and evaluates.

Most of the work is width. The numeric width now follows the target rather than assuming 64 bits, the bignum half-limb width stopped being hardcoded, and a register invariant that had been a convention became a mechanised check — which immediately found fifteen violating opcodes and then reported an empty list. That is the same move as the build checks this project keeps reaching for: turn a rule people are remembering into a rule the build enforces.

Three results are worth more than the fixes, and all three are things the project decided *not* to believe:

- A ChaCha20 failure was localised to bignum arithmetic above 32 bits — and then retracted. The bignum band was not broken and the cipher was correct.

- An register-clobber hypothesis was pronounced dead and the investigation stopped, rather than continuing to chase it.

- The collector was found never to have been wired on i386 at all — no arm in the cross-compiler, and a collect path that is an invalid instruction on a hosted system. The bulk of the crashes were arena exhaustion. “Proven twice over rather than argued.”

And when a ported collector ran but corrupted memory, it was gated off with the honest crash restored rather than shipped half-working. [№ 16](https://modus-lisp.github.io/issues/2026-06-27/)’s note to default away from blaming the garbage collector has clearly been read.

On aarch64, meanwhile, the runtime JIT was flipped on by default after a call-relocation bug that had been halving addresses — and flipped back off two days later, because it re-executes top-level forms. The reversal is its own commit rather than a quiet amendment. Underneath it all, the hosted CLI got real sockets, a resolver, block storage with durability, and an HTTP client — the first time modus fetched anything from the network itself.

---

*warp warren weft / 59 commits, two new repos*

## A delta protocol, a file browser, and Acid3

warren arrived first: a Miller-column file browser drawn over glass, with image previews through pigment decoded on a worker thread with a loading state and a stale guard, and an embedding mode so it can live in a framebuffer somebody else owns.

warp arrived the same day and spent the week deciding what it is:

> The core is a delta protocol, not a renderer: keyed, budgeted, staleness-annotated deltas over typed projections, with encodings per consumer. *Present* is a compression decision — which slice travels toward a consumer with finite intake — so macroblocks and tokens are peer encodings and neither is privileged.

> — warp — 52b2c13

A context window is a viewport; attention is priced per token the way frames are priced per macroblock. Written in the same week as the VP8 rate control at the top of this issue, which is presumably not a coincidence. The first practical client was a live pipeline monitor, and by Thursday warp implemented glass’s surface contract and could be a desktop window itself.

And weft took the forms remainder in one wave — interactive controls with focus, typing and painted state, one selection rather than two, and what a `<select>` pick means. Block-in-inline hoisting cut one page’s box error from 12,302 to 1,338, and Acid3 finished the week scripted at 100/100. The roadmap was corrected from being two phases behind reality; it still claimed “no JavaScript”.

---

**Method.** Commits by *author* date, 25 to 31 July 2026, across the 42 git repositories in the modus-lisp workspace. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps. Produced with `bin/week 2026-08-01 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
