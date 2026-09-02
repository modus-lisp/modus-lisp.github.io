# Modus compiles Modus

**This Week in Modus №20** · 18–24 July 2026 · 414 commits · 14 repos

<https://modus-lisp.github.io/issues/2026-07-25/>

A Modus image compiled the Modus sources into a second Modus image that boots and does it again, with no host Lisp anywhere in the loop. The last thing standing in the way was a counter that had never been set. Elsewhere: 256 spec sections in weft, and a VNC drag that went from 18 frames a second to 76.

| Commits | Repos moved | New repos | Source files | Lines added |
|---|---|---|---|---|
| 414 | 14 | 2 | 333 | +59k |

---

*modus / 21 July / #176*

## Modus compiles Modus

The smallest section in this issue is the one that matters most, and it needs a careful statement of what is new. [№ 2](https://modus-lisp.github.io/issues/2026-02-28/) had a running image build its own successor from source embedded in its binary. [№ 4](https://modus-lisp.github.io/issues/2026-03-14/) proved with five hash comparisons that the compiler leaves no trace of the machine that ran it. [№ 19](https://modus-lisp.github.io/issues/2026-07-18/) produced a `modus` binary carrying the whole build toolchain. But in every one of those, the first generation came out of SBCL. This week the loop closes with no host Lisp in it anywhere.

The last obstacle was not architectural.

> `(gensym)` returned `#:GNIL` every time, because `*gensym-counter*` was NIL — its `defvar` initialiser does not run at boot. Every other build image sets it explicitly; the self-host source did not. Colliding `GNIL` labels broke the native translator.

> — modus — 2fef2b2

That limitation is not new — [№ 13](https://modus-lisp.github.io/issues/2026-06-06/) had already made initialiser thunks fire at boot for one image family, without generalising it. A limitation that has been worked around correctly in four places and forgotten in the fifth is the most expensive kind, because every individual instance looks like an oversight rather than a pattern.

The rest of the week around it is clearing the path: the in-image self-compile’s collector corruption root-caused, a quadratic hash insertion in label emission removed — it was the “GC wall” a self-compile appeared to hit, which is to say it was misattributed to the collector, the way [№ 16](https://modus-lisp.github.io/issues/2026-06-27/) warned it would be — a 96 MB code-buffer over-allocation stopped, and the compile driver taught to report the failing form and its condition type instead of dying quietly.

---

*weft / 256 commits*

## A spec section per commit

Almost every one of weft’s 256 subject lines this week ends in a citation: grid subgrid on both axes, cascade layer ordering, container-query length units, mask images, path-based clipping, relative colour syntax, logical properties under vertical writing modes, collapsing-border table geometry, multi-layer background painting, viewport units in their small, large and dynamic forms, individual transform properties, drop-shadow filters, and lazy image loading measured from a live scroll offset.

Volume like that comes from the harness [№ 17](https://modus-lisp.github.io/issues/2026-07-04/) established, now grown a third stage. A per-unit oracle maps a feature to its real Web Platform Tests files and prints how many pass. A wave script drives a fleet of workers, each editing exactly one file in an isolated copy of weft and looping against the oracle. And a merge step re-verifies every result in the canonical tree and *keeps only what actually helped* — which is the part that makes the whole thing trustworthy rather than merely fast.

The isolation is proven rather than assumed: the source registry is pinned to a private tree with inherited configuration switched off, so a worker cannot silently fall through to the real checkout and score itself against code it did not write. Round two added diversified variants of each task and kept the best. That is the same instinct as the census before deletion in [№ 18](https://modus-lisp.github.io/issues/2026-07-11/) — the mechanism is only worth having if you have checked that it is telling the truth.

---

*glass / 48 commits*

## 18 frames a second, then 76

Dragging a window ran at 18 frames a second, and the profile exonerated everything obvious: compositing took under half a millisecond, and a drag frame is about 1.6 KB, which encodes in about one. Yet each frame took 56.

> That 50 milliseconds is pure TCP latency. The accept path never set `TCP_NODELAY`, so Nagle’s algorithm held every small interactive frame for about 40 ms waiting on the peer’s acknowledgement. A request-and-tiny-response protocol is Nagle’s worst case, which is why every real VNC server disables it.

> — glass — 2ce7ae5, drags 18 → 76 fps

The encoding work around it is a catalogue of the same discipline: a window move sending coordinates instead of pixels, per-region damage with region-limited diffing, the frame comparison skipped entirely when a generation counter says nothing changed, a tile encoding chosen to skip a serial compression pass, and then a stored-block variant that is a thirteen-fold encode win from one threshold. Plus a compliance fix worth its own line: those tiles are sixteen pixels, not sixty-four.

Then the drag that adapts, and it is the week’s best small idea. A big drag on a client without a copy-rectangle extension re-encodes the whole window every frame — measured at about 397 KB and 20 ms — while small moves are cheap and look perfect. So rather than pick a rule, the sender tracks how long the oldest unsent change has been waiting, smooths it, and switches: a drag starts opaque and falls back to a wireframe only once the link is demonstrably behind. The decision is driven off the measured backlog rather than off a clock, which is the same correction the ANSI harness made in [№ 6](https://modus-lisp.github.io/issues/2026-04-18/) when it stopped trusting what it had not observed.

Interoperability took the other half of the week, and each fix names the client that forced it: an older handshake version and its authentication scheme for macOS Screen Sharing; honouring the client’s requested pixel format, including for the cursor, for iOS clients; a different encoding for RealVNC, and then the cursor folded into the frame update, which fixed it properly. The authentication needed DES, which was written from scratch and promptly moved into seal where the ciphers live — the same consolidation reflex as natrium in [№ 19](https://modus-lisp.github.io/issues/2026-07-18/).

---

*webrtc-data cl-nostr / 41 commits*

## A desktop in a browser tab

The repository began the week by renaming itself. It implements the WebRTC data-channel profile and not media, so `cl-webrtc` overclaimed and `webrtc-data` does not — leaving the name webrtc-media well-defined for whatever fills it later. Naming a thing for what it actually does is cheap now and expensive in six months.

Then it carried a desktop. The transport became reliable and flow-controlled with round-trip estimation and message fragmentation; the connectivity layer gained server-reflexive candidates, full agent checks and a relay client, proven against a rootless NAT rig rather than asserted; the handshake switched its certificate to an elliptic-curve one, a smaller flight and about 540 ms faster. On top of that, glass over WebRTC: an off-the-shelf browser VNC client accepts a data channel directly as its transport, so the stack is a transparent pipe from a browser tab to a Lisp framebuffer.

The signalling is the part with no server in it. cl-nostr — which had been three weeks old and mostly a client — added encrypted direct messages, gift wrapping, a double ratchet, blob upload and static-site publishing; webrtc-data used them to exchange an offer and answer over direct messages, then to hand out one-time login links. Because a phone has no console, the page grew an on-screen diagnostics panel with a copy button, and the gateway learned to log *why* a login code was refused — which is the kind of thing you only build after standing in a room failing to log in.

---

*also this week*

## Five smaller things

- pigment was born by splitting the image codecs out of weft, and immediately switched its PNG decompression to cram. That is the third library this month to drop a third-party codec for the workspace’s own.

- skep was born on Friday: a Nostr agent workspace, proven against a real interoperating implementation rather than a mock.

- operandi grew a server for the protocol editors use to drive agents, validated against the official schema — which caught a `null` that should have been an empty object — and against a third-party client. It also got resumable sessions and an input line you can type into while the agent is still working.

- shuttle spent five commits on the JavaScript lexer’s oldest problem: whether a slash begins a regular expression or is a division. After a block, after a function declaration, after an `if` or `while` head, and — the other way — not after a postfix increment.

- glass’s terminal moved to 24-bit colour cells, picked up a programming font with scribe’s hinting on, and learned to keep its contents across a resize. There is also a demo that displays a JPEG in the terminal, for no reason anybody felt obliged to justify.

---

**Method.** Commits by *author* date, 18 to 24 July 2026, across the 42 git repositories in the modus-lisp workspace. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps. Produced with `bin/week 2026-07-25 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
