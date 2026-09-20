# A DVD plays, bit-exact

**This Week in Modus №27** · 5–11 September 2026 · 296 commits · 17 repos

<https://modus-lisp.github.io/issues/2026-09-12/>

On Saturday the workspace could decode one video codec. By Friday it could decode eight, in five containers, with four new audio codecs beside them — and every one of them is checked sample-for-sample against ffmpeg, a conformance suite, or the reference encoder’s own checksum. The decoder written on Sunday became the benchmark that drove Modus’s compiler from 3 to 60 frames a second on a Pi 5 by Thursday, after a profiler overturned the conclusion everyone had drawn about where the time went. And the browser client is now built by a JavaScript engine this project wrote.

| Commits | Repositories | New this week | fps, Pi 5 | Green, of 48 |
|---|---|---|---|---|
| 296 | 17 | 2 | 3→60 | 37 |

---

*reel, cassette, reed / 128 commits / 6–9 September*

## One codec to eight, against an oracle each

Two repositories were created on 7 September and between them account for forty per cent of the week. Neither is quite new work: *reel* gathers the VP8 encoder and decoder that had been living in webrtc-media and webp-pure, and *cassette* is webm-pure renamed, for a reason its first commit states plainly — it was about to read MP4 as well, and a repository named for its first format would then be lying. A cassette holds several tracks wound together and hands them back in step, which is what a container is. reel is the codecs; cassette is the boxes they arrive in; reed, already here, is sound.

Then the week happens to them. On Sunday reel has one video codec. Its road map, written on Monday, is candid about scope: H.264 High profile first because that is what x264 emits, MPEG-2 next because it is most of what any archive holds, and — in so many words — *“HEVC nowhere, because it is larger than all of H.264.”* By Friday:

*reel — what decodes, and what each was checked against*

```
H.264     Baseline, Main, High — intra, P, B, CABAC, weighted prediction, 8x8
          bit-exact against ffmpeg; the whole JVT conformance suite swept
VP9       all thirty-four libvpx conformance vectors, bit-exact
MPEG-1/2  bit-exact against ffmpeg
MPEG-4    Part 2, Simple and Advanced Simple — the DivX/XviD era — complete
Theora    frame decoder bit-exact against ffmpeg
FFV1      versions 0 and 1, both entropy coders
HEVC      intra pictures bit-exact; the first inter stream decodes exactly

cassette  MP4, Matroska/WebM, AVI, Ogg, MPEG program and transport streams
reed      AC-3, FLAC, Vorbis, MPEG audio Layer II, streaming AAC
```

“HEVC nowhere” lasted two days. That is not the road map being wrong; it is the road map being an honest estimate of cost, and the cost turning out to be payable.

The refrain in the commit subjects is *bit-exact*, and it is doing real work. Each decoder is graded against something with no stake in it: ffmpeg for most of the video, libvpx’s own test vectors for VP9, the JVT suite for H.264, the encoder’s embedded MD5 for FLAC, and for Vorbis a correlation of `1.000000` against libvorbis. Where the oracle disagrees with the specification the road map says so — one commit is titled *what the conformance suites say, including where it is wrong*. And when something does not decode it refuses by shape rather than guessing: *a video track that will not parse loses the picture, not the file*, and several refusals are kept in the suite as tests, so that a future decoder that stops refusing is noticed.

Put together, on Tuesday: *an MPEG program stream with MPEG-2 video and AC-3 audio plays with sound*. That is a DVD. The container, the picture and the sound were all written in the same seven days, and a DVD has been silent in this workspace for exactly as long as the workspace has existed.

![A phone screen showing the glass desktop through the browser: a Media window playing a WebM test vector with a timecode overlay reading 00:00:01.067 over colour bars, a track list of eight WebM files, mic and box level meters, and a row of Ctrl, Alt, Shift and Command keys.](https://modus-lisp.github.io/assets/img/webm-2026-09-07.png)

***WebM on a phone, through the desktop.** cassette demuxing a test vector, reel decoding it, glass’s Media window drawing it, glass-webrtc carrying the desktop to a phone’s browser — every layer of that in the workspace, and the two on top written this week. *“once you have vp8, webm isn’t that hard.”* nostr — b847bab4, 2026-09-07 00:17Z*

### The arrays were never fixnums

Halfway through, every decoder in reel got between 1.3 and 2.2 times faster with no algorithmic change and every sample unchanged. The commit that found it is worth reading for anyone who writes numeric Lisp:

> SBCL upgrades `(simple-array fixnum)` to `(signed-byte 64)` storage, so a value loaded from one is wider than a fixnum and the compiler cannot prove otherwise — every sum of two of them was an out-of-line call into generic arithmetic, and a video decoder is nothing but sums of values loaded from arrays. Declaring them thirty-two bits wide, which is what they hold, is fourteen per cent on its own and halves the memory they touch.

> — reel — 087edb3, *vp9: twice as fast, bit-identical — the arrays were never fixnums*

VP9 at 640×360 goes from 125 to 222 frames a second; 1080p from 13.7 to 26.8. H.264 had already gone from 20.6 to 118 fps on Monday, the first jump described as *“by telling the compiler what was already true.”* Two of the week’s largest speedups were declarations.

---

*modus / 73 commits / 5–10 September*

## The decoder that taught the compiler

The velocity note argued that the libraries compound and the floor does not — that the operating system underneath is the one layer where nothing written last week makes this week faster. This week is the clearest counterexample so far, and it runs the other way: a library written on Sunday made the floor twenty times faster by Thursday.

The benchmark is reel’s VP8 decoder, thirty frames of a small clip, running on Modus’s own compiler on a Raspberry Pi 5. On Wednesday it managed three frames a second. The commits that follow are a two-day argument with a profiler, and the docs record each step: 3, 4, 5, 30, 37, 48, 51, 56, 60. The interesting moment is in the middle, when the argument was being lost:

> What remains between 37 and 60 fps is uniform per-operation cost: every variable read is a frame load and every binary op is ~12 instructions. The next step is register allocation across a basic block, not another dispatch fix.

> — modus — docs/jit-fnaddr-rejection.md, as it stood at 37 fps

> That conclusion was wrong, and a sampling profile said so. `perf` on the Pi 5 put only ~45% of the decode in reel’s kernels. The rest was runtime library work the phase timers and op histograms could not see: every global-variable reference probing the globals table by hash, `expand-cl-loop`’s unbound termination-flag gensym turning every `loop while` exit into a *global* write, `FILL`’s generic element loop plus ~200 instructions of keyword parsing on each 16-element coefficient block, keyword literals re-interned on every evaluation, and rank-2 declared `aref`s through `APPLY`.

> — modus — bfd7af5a, the same file, one commit later

Every item on that list became a general fix in the compiler rather than a special case for the decoder: a runtime-compiled global read bakes its `(key . value)` cell as a constant and does `cdr` in place; the loop flag is LET-bound; `fill` and `replace` on a declared array compile to bounded typed loops; rank-2 `aref` gets fixed-arity primitives; `length` on a declared simple array stops paying a 30 ns dispatch. The instruction-count work everyone had been doing *changed nothing on the Pi*, and the doc says so under the heading “the useful negative result.”

The rank-2 probe also found a correctness bug that predates all of it: the compile-time `setf` expansion for `aref` kept only the *first* subscript, so `(setf (aref a i j) v)` inside a function stored at flat index `i`. Toplevel forms took a different path and were right, which is why nothing had noticed.

Sixty frames a second on 10 September, keyframe 37 ms, inter frames 15 ms, the same Y-plane checksum on x64 and aarch64. Then the first three commits of a SIMD plan: packed single-float vectors as a new heap subtag, native `fcvt` for element access, and single-float arithmetic emitting `fadd`/`fmul` directly.

### Three hosts, and a heap that survives

Two other threads land on `main`. Modus now bootstraps under Clozure CL and under ABCL — Armed Bear, on the JVM — as well as SBCL, and a script runs the self-hosting fixpoint across all three. The three host-built images differ by about 0.3%, which is the hosts’ own codegen showing through; but what each of those images then *compiles* is produced by Modus’s in-image compiler, and the script requires that output to be byte-identical across all three — *“the host washes out once Modus is compiling.”* Three independent Lisps agreeing to the byte on the same toolchain is the diverse double-compilation that Thompson’s *Reflections on Trusting Trust* asks for, and the compiler that produces the bare-metal image is no longer defined by one host’s idea of Common Lisp.

And *save-and-die*: a heap snapshot and restore, validated first under QEMU and then on a real Pi Zero 2 W, with alexandria restored from a saved core on the board. The follow-on is a turnkey SSH-REPL image for the Zero locked to a user’s Ed25519 key, and restored cores now come up with the JIT on by default. Last week’s [quickload over cabinet](https://modus-lisp.github.io/issues/2026-09-05/) put a library *onto* the machine; this week the machine can be put down and picked up again with the library still in it.

---

*shuttle, glass-webrtc / 39 commits*

## Nothing in this repo runs node any more

shuttle is the JavaScript engine. This week it grows the half of ECMAScript modules a bundler needs — the static graph — and then the runtime half: live bindings, cycles, namespaces, dynamic import, top-level await and the evaluation order it forces. The module portion of test262 goes from 543 to 561 of 566, and the commit names the four engine bugs behind the last eighteen.

On top of the module graph, a bundler: one script out of many modules, and *“esbuild is out of the deploy path.”* Then a minifier that deletes what carries no meaning and proves it over the whole test corpus. Then npm itself — a resolver that walks a dependency graph, verifies every byte, and lays out `node_modules`, with a semver implementation graded against the semver that npm resolves with. The grading is the reason for the week’s largest single file: 82,909 lines of `semver-cases.tsv`, which is an oracle and not code, and is why shuttle’s line count in the ledger below reads six figures.

glass-webrtc, the gateway that puts a glass desktop into a browser, is the first customer. It vendors its browser client’s dependencies, builds them with shuttle, and then:

> Both python builds are gone. The four artefacts are unchanged in kind, and both pages render IDENTICALLY to the esbuild output in headless Chromium, with zero page errors on either.

> — glass-webrtc — ce9efbb, *delete the python builds: nothing in this repo runs node any more*

One more detail from the same commit, because it is the kind this project notices: the gzipped payload is written with a zero mtime, so an unchanged payload is a byte-identical file. The gateway hashes what it reads, and *“a hash that moved because a clock moved would push a pointless transfer to every phone.”* cram grew the gzip *container* — not just the decoder — and the tar half of `.tar.gz`, this week, for exactly this. Then shuttle’s minifier halves what crosses the data channel.

shuttle’s test262 harness is also made honest this week, and that is its own story: negative tests are now scored on the error they *declare* rather than on whether anything went wrong, scripts get a module host, and the commit subjects say what that cost — *four bugs the honest harness uncovered*, *four more the harness finally showed*, and a full-corpus number recorded with a note on *why it is lower than the last one*. A conformance figure that went down because the grader got stricter is the right direction.

---

*webp-pure, webrtc-media, warren / 7 and 8 September*

## Three gates go red for a true reason

Last week thirty repositories got their first CI gate, and every failure that turned up was in the harness. This week three of those gates went red for a reason that is real:

*webp-pure, webrtc-media, warren — the newest run of each*

```
LOAD-ERR Component :REEL not found, required by #<SYSTEM "webp-pure">
Component "reel" not found, required by #<SYSTEM "webrtc-media">
Component :REEL not found, required by #<SYSTEM "webp-pure">   (via warren)
```

VP8 moved out of webp-pure and webrtc-media into reel on Sunday, and the three systems that now depend on reel say so — but the workflows that check out sibling repositories were not told, so a clean runner cannot find it. Anyone cloning webp-pure fresh this week would hit the same wall. The fix is one line in each `gates.yml`; the point is that a week ago nothing would have said anything at all.

The mirror image is less comfortable. reel and cassette, 119 commits between them and the week’s largest body of new code, have no gate. Neither does glass-webrtc. The org stands at 48 repositories, 37 green, 4 failing, 7 with no CI — and the four failures are the three above plus operandi, whose reverted gate still shows its last red run.

---

*the rest / warp, cl-transport, kiln, cl-payments*

## Also this week

**warp** gets a widget catalogue — twelve widgets, twenty types, *nothing undeclared* — photographed reproducibly by a script, which immediately caught three the client was not painting. The principle is stated in a subject line: *a widget is a declared cell layout, not a shape you guess from the data*. Its media player, a warp client, plays MP4 video now, and knows the difference between a picture that is missing and one that never started.

**cl-transport** absorbs the connectivity stack: STUN and TURN move in from webrtc-data, ICE follows them, and ICE *“stops knowing what SDP is,”* which is a layering fix disguised as a move. Two hex formatters go back the other way, because their only caller stayed behind.

**kiln** re-dumps its image four times in one day to keep up with reel — the allocation fixes, the speedup, Main profile, MP4 in the media player — and the default window becomes a Lisp listener rather than a shell on the host. **glass** gets a Media item in its root menu.

**cl-payments**, last week’s Lightning node, has a quiet week and its first pull request: `fsync` on durable writes, and a shachain reload that is a true inverse of the save.

---

**Method.** Commits by *author* date, 5 to 11 September 2026, across the 46 repositories of the modus-lisp organisation, counting each repository’s default branch as the union of the local branch and `origin/`. The organisation’s `.github` profile repository and this site are not counted. Produced with `bin/week 2026-09-12 --log`.

**Line counts.** The ledger’s ADDED column exaggerates three repositories this week and the reader should discount them: shuttle’s 89,049 includes 82,909 lines of a semver oracle (`inspect/semver-cases.tsv`), cassette’s 23,978 includes the 17,027-line text of RFC 6386 kept as a test vector, and glass-webrtc’s 30,358 is mostly vendored JavaScript under `vendor/`. None of those is code anyone here wrote. reel’s 36,822 is.

**The tool, again.** Two weeks ago `bin/week` counted whatever branch was checked out; last week it learned to count the default branch and to ask the organisation about repositories it could not see. This week it found the local modus clone 86 commits behind origin and kiln 20 behind — a quiet week that was not — so it now counts the local default branch and its `origin/` counterpart together, and prints which clones are behind so that anyone reading source to write an issue knows they are reading last week’s. Both new repositories were already cloned here, which is the first time in three weeks the ledger has not been missing one.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
