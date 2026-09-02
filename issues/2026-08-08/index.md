# Speech, both ways

**This Week in Modus №22** · 1–7 August 2026 · 218 commits · 15 repos

<https://modus-lisp.github.io/issues/2026-08-08/>

In one week the workspace got a voice and something to hear with: a text-to-speech engine, a speech recognizer, a tensor engine underneath both, and a desktop that wires a phone’s microphone through to whichever window has focus. Underneath, the JIT stopped falling back to the interpreter.

| Commits | Repos moved | New repos | Lines of code | Lines of phonetics |
|---|---|---|---|---|
| 218 | 15 | 4 | +51k | 488k |

---

*chord mill stave / 25 commits, three new repos*

## Speech, both ways

On Thursday a repository called chord appeared containing text to speech in pure Common Lisp: a neural graph interpreter with no runtime under it, a pronunciation dictionary, and letter-to-sound rules learned from that dictionary for the words it does not contain. Those two data files are 488,000 of the week’s lines, which is why the tally above separates code from phonetics — the same treatment [№ 17](https://modus-lisp.github.io/issues/2026-07-04/)’s test corpora got, and for the same reason: shipped content is not the same achievement as written code, and pretending otherwise makes both numbers meaningless.

The rest of Thursday is making it fast enough to be a desktop’s voice rather than a demonstration, and the commits read like a profile being walked down.

> MatMul was 22% of a synthesis; now it is 0.8%. Twenty-six nodes out of 2,755, and a third of a second of every four-second sentence.

> — chord — fde2002

The loop order was already right. The operation simply ran at default safety with untyped index arithmetic, so every offset went through generic addition and every element through the bounds-checking accessor: the arithmetic was the small part of what the arithmetic cost. A typed core with declared indices, and the rows fanned across a worker pool that already existed. The convolutions got the same two levers plus a five-operation vector layer, a scatter became an accumulate once the output row was de-interleaved, and dead buffers started being recycled instead of asking the allocator for fresh pages.

By Friday the tensor engine had moved out into mill — “chord is what is about speech” — taking the graph interpreter, subgraphs, and the operations a model needs. And stave arrived on the other side of the conversation: a transducer engine proven against all three of its graph types, a front end of audio decoding, spectral analysis and a filterbank, then “it transcribes”, then transcribing compressed audio without leaving Lisp — through reed, written the week before in [№ 21](https://modus-lisp.github.io/issues/2026-08-01/).

---

*glass warren loom / 32 commits*

## A desktop with a mixer

A session got a sound, then more than one listener for it, then the mix learned to leave the process that makes it. A drifting sink is corrected one frame at a time rather than in a lump, which is the difference between audio that slides and audio that clicks. On top of that came the desktop’s own voice, a window to type into when you want it to say something by hand, and a selection menu that appears where the words are and asks the *window* what is selected rather than the clipboard.

Then the ear, and the neatest trick of the week: dictation needed no new input path at all.

> The key injector already existed, for pasting into applications that do not read a clipboard. An injected key takes the identical route a client’s keypress takes — the window manager’s focus rule, the pty write, the event queue — and nothing downstream can tell the difference.

> — glass — 0f70ef5

So speech reaches an application that has never heard of an ear, the phone’s microphone reaches the desktop instead of the floor, and stopping the ear stops dictation with a button that says so. loom made selecting the same as copying, with the selection owned by the window rather than the browser in general; warren gave the desktop a voice and then removed its own two speech menu items once the window manager had them.

---

*modus / #222, #227, #220, #221*

## The JIT stops falling back

The runtime JIT came up in stages in [№ 19](https://modus-lisp.github.io/issues/2026-07-18/) and was flipped on and then off again in [№ 21](https://modus-lisp.github.io/issues/2026-08-01/). This week closes the gap that had been keeping it honest-but-useless. Every runtime `defun` was being installed as a heap closure that re-enters the interpreter, so a later form calling it failed the native-callee check and the whole calling module fell back to interpretation. That single gap was the JIT’s only remaining fallback reason — and the reason a runtime-defined function could never be an actor entry point, and the reason a loadable ANSI corpus ran fully interpreted.

Its counterpart is found because somebody made the “survives GC” probes actually collect. A runtime macro’s expander stopped working after any collection: the function still existed, but its quoted-symbol constants had gone stale, so an expansion came back with an unprintable object where a name should be. A macro defined after a collection worked until the next one.

The cause is not about macros. A compiled page bakes absolute addresses into immediates in three places, and two of them could be *heap* addresses — and heap objects move. This is the same family that [№ 14](https://modus-lisp.github.io/issues/2026-06-13/) found four members of and [№ 18](https://modus-lisp.github.io/issues/2026-07-11/) found two more: something holding a reference the collector does not know to update. Four classes of stale-address corruption closed at once.

### One scratch register, six wrong answers

On aarch64, the helper that materialises a source operand hands back the scratch register whenever a value has been spilled. Several instruction sequences were using that same scratch as working space while still needing the source it stood for.

*modus — #220, what `(mod x y)` emitted*

```
SDIV x16, pa, pb      ; x16 = quotient
MUL  x16, x16, pb     ; x16 = q * divisor
SUB  pd, pa, x16      ; remainder = dividend - q*divisor

; at any site whose dividend is spilled, pa IS x16 —
; so the last line degenerates to SUB pd, x16, x16
```

`(mod x y)` returned a hard zero. A sweep of all 102 sites found five more of the same shape: `(consp nil)` answering true, `(atom nil)` answering false, a wild store, an atomic exchange whose address had been destroyed, and a per-CPU set clobbering its own value. Two resolution bugs went with them — an unresolved call landing at offset zero instead of a safe stub, which turned out to be the SSH images’ long-standing hang.

---

*modus / 32 bits, and 36 build scripts*

## i386, from `(eval 42)` upward

Saturday’s i386 commits continue [№ 21](https://modus-lisp.github.io/issues/2026-08-01/)’s bring-up and read as a log: `(eval 42)` evaluates — `consp` said true for NIL and `make-array` was missing. The macro and backquote bootstrap, real file I/O, and `load`. The interpreter’s canonical NIL was a bignum. And then the one that explains a whole column of symptoms above it:

> `(eval '(quote X))` returned NIL for every X, because x86-32 masks a shift count to five bits. A runtime `defmacro` was handed NIL and registered nothing — silently, because the key lookup just answers NIL.

> — modus — 571a19d

Floats were the other wall, and the diagnosis draws a distinction worth keeping: i386 floats were a *representation* problem, not a code-generation gap. The layout became width-neutral — four slots of tagged chunks — and then hardware float instructions landed, taking the translator gap from thirteen down to one.

And the build matrix stopped being folklore. Thirty-six build scripts were audited by actually building them, then consolidated to twenty-eight, with one build file covering architecture by mode by board and one launcher replacing about fourteen. Seven of twenty-eight output paths in the table turned out to be wrong and were transcribed from the scripts themselves. Also, and less gloriously, 960 MB of accidentally committed build binaries were untracked.

---

*webrtc-media webrtc-data / 38 commits*

## A drag is not a screen full of new content

Dragging a window over a slow link produced a long wait and then the window at its final position in one step. The cost model was pricing a translation as fresh content: an inter frame was estimated as dirty macroblocks times a price per coefficient, but a macroblock that merely moved codes no coefficients at all. It spends a mode and a vector reference — about a byte, and the same byte at every quality setting. A drag dirties most of the screen and translates almost all of what it dirties, so the estimate was wrong by orders of magnitude exactly when it mattered.

Out of that came a bandwidth ladder where everything derives from one knob: a rung names a cadence and not only a rate, and the coarseness a frame may *settle* at is kept separate from the coarseness it may *move* at. Resolution follows damage. An idle frame may not hold the link, and a picture nobody can see is not a picture that stopped. On the phone side, the client picks a rung and sees what it bought, survives backgrounding, reconnects on its own, and stops answering ghosts.

---

*also this week*

## Three smaller things

- spool, a podcast client for the glass desktop, arrived on Wednesday — feeds through weft, audio through reed, which is four repositories composing without anyone writing glue for the occasion.

- reed learned to play a file rather than only convert one, to open over bytes already in memory, and to start an `.m4a` where its audio starts rather than where its bitstream does.

- seal added TLS 1.2 key exchange over the NIST curves, with a gate that runs it.

---

**Method.** Commits by *author* date, 1 to 7 August 2026, across the 42 git repositories in the modus-lisp workspace. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps, but *not* chord’s 488,000 lines of pronunciation data, which are shipped content and are broken out separately above. Produced with `bin/week 2026-08-08 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
