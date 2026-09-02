# The stack learns to leave home

**This Week in Modus №24** · 15–21 August 2026 · 136 commits · 23 repos

<https://modus-lisp.github.io/issues/2026-08-22/>

Two repositories appeared — one that fires the whole workspace into a container, one that puts it in a native window. The Common Lisp stack ran a REPL on real Pi Zero silicon. And half a dozen libraries stopped assuming whose home directory they were living in, which is the same story told small.

| Commits | Repos moved | New repos | Source files | Lines added |
|---|---|---|---|---|
| 136 | 23 | 3 | 236 | +27k |

---

*kiln / 21 commits, all of them its first*

## A desktop you can install

On Tuesday a repository appeared containing one idea: take the forty-odd checkouts that make up this workspace and fire them into a single image that boots a desktop. By Friday it could do that on Docker, on podman, on Apple’s container runtime, out of a plain folder with no container at all, and on Windows.

How it builds is the most modus-lisp thing in the week, and it is only possible because of what the last five issues built. Five repositories arrive as tarballs — cairn and its closure, which between them depend on nothing third-party — and then cairn, the from-scratch git of [№ 19](https://modus-lisp.github.io/issues/2026-07-18/), clones the other thirty-three out of GitHub over seal’s TLS from [№ 18](https://modus-lisp.github.io/issues/2026-07-11/). There is no git binary in the image. The lock file pins every commit and refreshes through cairn’s own ref discovery, which needs no API and no token.

The rest of the week is the difference between a demo and a thing you hand somebody. The browser became the default surface and VNC the opt-in. A published port lands on loopback unless you ask otherwise. The box configures over ssh, menuconfig style. The agent runs inside fences, and the mount list *is* the fence.

And on Thursday a second new repository, glass-sdl, gave the desktop a native window — verified 13/13 on real Windows hardware with a real video driver, then saved as a standalone executable. Which is a small irony worth naming: [№ 18](https://modus-lisp.github.io/issues/2026-07-11/) replaced loom’s SDL binding library by hand and [№ 19](https://modus-lisp.github.io/issues/2026-07-18/) retired SDL as the everyday driver in favour of VNC. SDL returns here on purpose, isolated in one repository and one file, as the single place this workspace admits that putting pixels on somebody else’s screen means asking somebody else’s window server.

---

*modus / 37 commits / #209, #160, #266*

## Four rungs, and a collector that never forwarded anything

Modus has been on Raspberry Pi hardware since [№ 3](https://modus-lisp.github.io/issues/2026-03-07/). What it has never had there is the Common Lisp stack — the one that runs the real language, the one [№ 6](https://modus-lisp.github.io/issues/2026-04-18/) onward has been grinding against the ANSI suite. Four rungs went up in a day: a bare-metal Pi 3B image on that stack, then fetching an archive over real HTTP, then installing, loading and calling the library inside it, and then the whole thing on real Pi Zero 2 W silicon.

*Real Pi Zero 2 W hardware — #209 rung 4, ebd1e04*

```
## Starting application at 0x00300000 ...
BOOT
MODUS-CL
Modus CL REPL (bare metal).  EVAL = MVM-EVAL.
> (mapcar (lambda (x) (* x x)) (list 1 2 3 4 5))
(1 4 9 16 25)
> (fact 20)
2432902008176640000
```

Getting there needed the mini UART rather than the other serial device for chain-loaded images, a loader protocol that carries the load address on the wire, and the exception vector set for the privilege level the image actually runs at.

### Two bugs inside the garbage collector

Turning on allocation bitmaps for bare-metal aarch64 found a collector that had never actually forwarded anything.

> `(mem-ref A :u64)` does not return the machine word at `A`. The loaded word is left untagged, so every later Lisp operation reads it as a tagged value: the integer handed back is *word/2*.

> — gc.lisp — 6c1b027, proven against physical memory with gdb

The docstring blamed i386 and claimed the other architectures used native trampolines instead. That was false for aarch64, whose trampoline is a shim that calls this same Lisp collector — so every bare-metal Pi image was running it. Its sibling is almost funnier: all four bitmap helpers wrote their mask with a *variable* shift count, and a variable shift routes through a runtime bignum operation, which conses. Each of them runs inside the collector, where the allocation pointer is already at the limit, so a single cons re-trips the check and re-enters the collector. They had never been exercised, because the targets with bitmaps on used a different path.

That is the same shape as [№ 22](https://modus-lisp.github.io/issues/2026-08-08/)’s baked heap addresses and the roots in [№ 14](https://modus-lisp.github.io/issues/2026-06-13/): code that is correct everywhere it is exercised and wrong in the one configuration nobody ran.

### A fork nobody had noticed

#266 cut 871 lines by making the bare-metal Pi build a thin tail over the shared assembly instead of a private fork. Twenty-seven of its thirty-eight variables were verbatim copies; among the duplicated code was a complete x64 JIT baked into an aarch64 image, dead only because its flag was off. The commit names the cost plainly — this is the shape of the console and USB bugs from the same week, a fix landing in one assembly and not the other — which is exactly the argument [№ 23](https://modus-lisp.github.io/issues/2026-08-15/) made a week earlier when three CLIs became one, and which that convergence proved by fixing four defects for free.

---

*seal cl-nostr cairn / 8 commits*

## One HTTP client, at last

Four hand-rolled HTTP clients had grown here, because there was nowhere for one to live: cairn’s 108 lines, just enough for git’s smart transport; weft’s 527, the mature one, reachable only by depending on a browser engine; skep’s; and cl-nostr, which had given up and reached for a third-party library — which is cl+ssl, which is OpenSSL through a foreign interface, in a workspace that [№ 18](https://modus-lisp.github.io/issues/2026-07-11/) spent a week removing OpenSSL from.

So HTTP/1.1 moved into seal, on seal’s own TLS: URLs with bracketed IPv6 authorities, chunked and length-delimited bodies, read-to-close, plain HTTP over a raw socket as well as TLS, and redirect following where the awkward status codes degrade to GET without a body, which is what clients actually do. A WebSocket client followed it out of modus. cairn stopped being a fourth client; cl-nostr moved its relays, name lookups and blob uploads across and is now free of foreign code.

One thing that fell out of the move deserves its own paragraph. secp256k1-fast — eleven times faster since [№ 15](https://modus-lisp.github.io/issues/2026-06-20/) and constant-time since [№ 17](https://modus-lisp.github.io/issues/2026-07-04/) — keeps mutable state across a scalar multiplication, so two threads signing at once do not merely contend: they corrupt each other. Of eighty signatures taken from four threads, seventy-seven signalled and the three that returned were invalid. Silently invalid is the dangerous half — the event looks complete, and the relay that rejects it reports a bad signature for a key you know is good. A pool fans out and an uploader fans out, so the lock went underneath everybody rather than into a note asking each caller to remember.

---

*glass webrtc-data / 27 commits*

## A link a person has to spend

Handing somebody a desktop over a direct message means handing them a code, and a code that can be used twice is not much of a code. So an enrolment became a record: revoking marks it, the box says when it runs out, and it tells the client before the client is surprised. A login code is traded exactly once, and cancelled when it turns out not to be needed.

Then the interesting failure. Single use gives a real property — a successful redemption means nobody else got there first — but only if a code cannot be spent without a person. These links arrive in a message, and preview bots fetch them; some render the page in a headless browser. One of those would connect, bind the code to its own throwaway key, and the human’s tap would be refused *as a leak*: a false alarm that locks the owner out, caused by the defence.

> So the gate sits on redemption, not on connecting. A returning terminal has an enrolment and spends nothing, so it opens straight away; only a load actually carrying a live code waits for a finger.

> — glass-webrtc — 78b4b63

Around it: a single control socket that answers even the form it cannot read, two seats onto one real desktop with a gate that proves it — the seats [№ 23](https://modus-lisp.github.io/issues/2026-08-15/) introduced, now with a second person actually on one — seats bound to loopback unless somebody asks otherwise, and a terminal window whose shell is a Lisp listener. And the box writes down which route a session took and how it ended, so “it didn’t connect” has an answer.

---

*mill stave chord stencil / 20 commits*

## An oracle for pictures, and a licence for a voice

stencil’s self-test asked stencil whether stencil agreed with stencil. Twenty-three checks, all green, and underneath them patterns rendered nothing at all: the paint server was not implemented, the reference resolved to no paint, and the shape was silently dropped. Nothing in a self-referential suite knows what the pixels were supposed to be. It surfaced when a wallpaper named for its patterns came out with no patterns in it.

The grader is now Chromium — each test rendered as a top-level document, stencil rendering the same file into the same box, both composited onto the same white and compared. That is the fifth reference implementation this workspace has pointed itself at, after Bitcoin Core, `ttx`, real git and libvpx, and the lesson each time is the one [№ 17](https://modus-lisp.github.io/issues/2026-07-04/) made explicit: build the oracle first, because the thing you cannot see is exactly the thing your own tests will agree with you about.

mill learned to read a model file and convert it once, to handle subgraphs, and to pack a matmul operand and pool its workers. stave got its second half — beam search, a lexicon, streaming, and a word error rate to be judged by. And chord spent three commits on a licence, which a weekly does not usually report. The voice model’s card says MIT; that covers the toolkit’s repository, not the speech it was trained on, and the dataset licence it links to grants research use only and forbids redistribution. Weights trained on that data are a derived work of it. So the voice cannot ship, the README says how to fetch and convert one yourself, and it says why the pronunciation dictionary *does* ship, being BSD. A dataset licence and a model licence are routinely different, and the dataset is the one that binds.

---

*everywhere / one line at a time*

## Nobody lives in /home/claude

The quiet theme of the week, and the reason for its title. Following the ten-repository sweep in [№ 23](https://modus-lisp.github.io/issues/2026-08-15/), the last of the hard-coded paths went:

- weft takes its test checkout from an environment variable.

- loom takes its browser, its Quicklisp and its test suite from `$HOME`.

- warp and spool resolve the workspace from the script that is running, and glass from the file asking for it.

- glass derives every system definition from the file being loaded, and finds Quicklisp rather than assuming a home it owns.

- webrtc-data finds its bundler under `$HOME`, and its deploy scripts say what to do instead of how to do it.

None of these is interesting on its own. Together they are the difference between a workspace that runs where it was written and one that runs in a container, in a folder, on Windows, or on a Pi — which is exactly what the rest of the week was about.

---

**Method.** Commits by *author* date, 15 to 21 August 2026, across the 42 git repositories in the modus-lisp workspace. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps. Produced with `bin/week 2026-08-22 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
