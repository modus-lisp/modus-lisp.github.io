# The Pi Zero answers the doorbell

**This Week in Modus №25** · 22–29 August 2026 · 97 commits · 7 repos

<https://modus-lisp.github.io/issues/2026-08-29/>

A real OpenSSH client logged in to the bare-metal Common Lisp image on Pi Zero silicon, over a USB-ethernet driver written this month, and then fetched a library off the network and called it. Meanwhile the desktop above it learned its own name, its own resolution, and how to tell you where to find it.

| Commits | Repos moved | Source files | Lines added |
|---|---|---|---|
| 97 | 7 | 69 | +7.9k |

---

*modus / 37 commits, 4 merges / #275*

## The Pi Zero answers the doorbell

The week opened with the least glamorous work there is: distrusting the bootloader. A new boot preamble sanitises whatever cache and memory-management state the loader left behind, then brings the unit up on Modus’s own terms — an identity page table with a cached CPU and coherent DMA, which is the precondition for touching a USB controller at all.

On top of that went a from-scratch RTL8153 USB-ethernet driver. Getting frames *in* took reading the vendor registers back over the serial REPL, and the answer was that U-Boot had already enabled the receiver for its own network boot and left it enabled through the port reset. The previous five-write enable sequence was re-writing a control register under the configuration unlock for no reason, and that was exactly what wedged the chip, run after run. The working enable is two idempotent writes. ARP resolved. Ping answered.

### Then a real OpenSSH client logged in

[№ 3](https://modus-lisp.github.io/issues/2026-03-07/) had an SSH server on a Pi Zero in March, over the USB gadget driver and on the pre-Lisp stack. [№ 24](https://modus-lisp.github.io/issues/2026-08-22/) got the Common Lisp image booting to a REPL on the same silicon. This is the two of them meeting: curve25519 key exchange, an Ed25519 host key, chacha20-poly1305, authentication, channel exec, result on stdout, exit status 0 — against the CL image, over the driver written three days earlier.

*Verified on the netboot rig — 24 & 25 August*

```
$ ssh test@10.0.0.2 '(+ 2 3)'
= 5

$ ssh test@10.0.0.2 '(net-install-and-call "http://10.0.0.1:8099/alexandria.tar")'
= T

$ ssh test@10.0.0.2 '(alexandria:flatten (list 1 (list 2 3)))'
= (1 2 3)
```

Three root causes stood between “incorrect signature” and a login prompt. `ssh-receive-version` used a legacy zero-as-false idiom; in this image `0` is truthy, so it reported success on its first pass without parsing anything, the client version string stayed empty, and the server’s exchange hash covered an empty banner while OpenSSH hashed its real one. Every cryptographically valid signature failed. Separately, the receive path re-armed the next bulk transfer into the same single buffer the moment one completed, so back-to-back segments overwrote each other before the caller could copy a frame out.

That first one is worth pausing on. [№ 12](https://modus-lisp.github.io/issues/2026-05-30/) found that NIL and the fixnum zero shared a bit pattern on AArch64, making `:count 0` and `:end 0` ambiguous. This is the mirror image — code written for a Lisp where zero is false, running in one where it is not — and it is the same lesson from the other side: a shared network stack written for lenient legacy semantics, ported into a conformant image, will fail silently at exactly the place nobody rechecks.

### One bug wearing four costumes

The second milestone — the JIT running on the Pi and a library being fetched, untarred, parsed and loaded over the network, all eighteen files — was gated by a single allocator bug that had been producing unrelated-looking failures for weeks.

> The GC check compares the allocation pointer *before* the allocation, so an object that starts just under the limit writes its header and zero-fill straight past it. On the Pi the upper semispace ends where the collector’s configuration page begins.

> — modus — bfca1db, watchpoint-proven

A large buffer allocated inside the aarch64 translator was zeroing a field on that page, and the next collection then ran on garbage geometry. The corrupted-characters mystery, a wild-pointer fault deep in a library, and a recursive exception storm when the allocation bitmap was on were all this. The fix is an 8 MB guard band — which is the *third* time this exact remedy has appeared in this archive, after the 16 MB heap guard in [№ 14](https://modus-lisp.github.io/issues/2026-06-13/) and the i386 arena guard in [№ 23](https://modus-lisp.github.io/issues/2026-08-15/). A check that tests the pointer before the write is a bug you get to have once per architecture.

---

*modus / compiler, runtime, conformance*

## Climbing the Quicklisp ladder

Loading real libraries keeps finding places where Modus is not yet Common Lisp — the same method [№ 14](https://modus-lisp.github.io/issues/2026-06-13/) introduced by pointing ASDF at it. This week: package-local nicknames, conditions that keep their identity across an interpreter-state boundary, a real `with-open-file`, runtime default initargs, CLOS accessor generic functions, unbound-slot dispatch, globbing `directory`, a system-definition searcher hook, cross-unit loop `return`, and the monetary FORMAT directive.

The JIT’s constant vector went from a diagnostic ring buffer and a debug printout to a thunk-scoped, collector-updated vector with a coverage guard, and full scope flipped from an opt-in to the default once a dead-page read was understood. Cross-unit `go` now dispatches through a runtime catch frame. And the merge criteria stopped being folklore: an acceptance-gate script now encodes the gate, with 8 GB of dynamic space for the runner builds — another rule that was in somebody’s head becoming a rule the build enforces.

### An honest entry in the ledger

`concatenate` got four commits in two days. First it learned to validate every input up front and name the offender instead of reading a header blindly. Then a fast path that skips the reject scan and copies string codes raw made it three times faster. Then the fast path was found to be testing the wrong string predicate. Then the whole raw copy was reverted, because it breaks strings with a fill pointer. The validation stayed; the speedup will come back when it is correct. In the same batch, `eql` against a constant stopped calling the runtime, and compile-time safety levels landed as a sketch with a first consumer.

---

*kiln glass glass-sdl webrtc-data / 54 commits*

## A desktop that knows its own name

The upper half of the stack spent the week becoming something you can hand to a person. A session now has a name, and the desktop writes it in the corner without painting over a name somebody chose. It also has its own identity: one key per session, named after itself, so resume and detach mean something and there is no host key to fall back on. The container gets that identity too, and locks that are real locks.

Which makes one command the whole of “start a desktop and let me in from my phone”:

*kiln — a012b5c*

```
$ kiln local --detach --vnc-socket --nostr=ynniv@ynniv.com
$ kiln run --nostr=npub1…
```

The argument takes a public key, raw hex, or a domain-verified name, and answers two questions that happen to have the same answer: who may connect, and who gets told the session is up. They stay two variables on purpose — an allowlist can hold people nobody is messaging. The name lookup is resolved where a failure is visible rather than swallowed at boot, which is a rule this repository says it learned the hard way about that lookup specifically.

### Resize, all the way down

Making a window draggable turned out to touch every layer at once. glass now advertises the resize extension without waiting to be asked for pixels — otherwise no client ever asks for a size — and a client that does ask resizes the seat it is looking at; the wallpaper became a picture the seat re-cuts rather than a fixed bitmap, which is what the per-seat rasterisation in [№ 23](https://modus-lisp.github.io/issues/2026-08-15/) was for. glass-sdl stopped predicting the size its window would get and started asking the window what it actually got, with filtering that isn’t nearest and a pointer that lands where you clicked — then learned to keep drawing *during* the drag, because macOS blocks the event loop until you let go. webrtc-data’s browser payload notices a resolution change without being told, and stops the desktop hiding underneath its own control buttons.

### Ears, and a menu worth opening

The voice flags are now checked *before* the desktop starts, and a subcommand fetches what they need. The podcast client and the listening window were both in the checkout and in no image — the engines and the windows are separate systems, so loading the recogniser without its window gives you a working ear with no way to reach it, and a menu entry that vanishes silently. Both now build into the core and report through the same missing-systems path as everything else.

And the root menu finally offers what this project wrote. A desktop carrying a browser, a file manager, a podcast client, a speech synthesiser and a recogniser was offering a calculator and a gadget demo, because every one of those applications ships a register function and nothing ever called one. They are now discovered by name — glass must not depend on loom, warren or spool, since those depend on glass — so a first-party application is found exactly the way an out-of-tree one would be, an entry appears only when the image has the code behind it, and a menu line can never break a build.

### Operations

- Nothing was ever sweeping the oldest generation. Now something is, and the housekeeping lives in the core rather than in one file’s boot path.

- SBCL’s heap has to be smaller than the container it runs in — obvious in hindsight, invisible until the OOM killer explains it.

- Quicklisp archives are fetched with curl during the build, because its own client hangs in this environment.

- The framebuffer’s two locks now have different names, so a deadlock report says which one, and the compositor no longer waits on a surface’s framebuffer.

---

*elsewhere / 2 commits*

## Two small ones

- secp256k1-fast — on x86-64, `MUL` is a one-operand instruction and its operand size has to be stated. Pinned into kiln’s lock file the same night, which is what having a lock file is for.

- operandi-gui — a command that pings the model before it keeps it, so a typo fails at the prompt instead of at the next message.

---

*no commits this week*

## Quiet in the workshop

Thirty-five of the workspace’s forty-two repositories didn’t move — including weft, scribe and loom, which carry most of the desktop’s text and layout and have been stable since mid-August.

- brotli-pure

- cabinet

- cairn

- chord

- cl-consensus

- cl-frpc

- cl-nostr

- cl-tor

- cl-transport

- conch

- cram

- dist

- folio

- gesso

- glass-mcclim

- loom

- mill

- natrium

- operandi

- pagetree

- pigment

- reed

- scribe

- seal

- shuttle

- skep

- spool

- stave

- stencil

- warp

- warren

- webp-pure

- webrtc-media

- weft

- zstd-pure

---

**Method.** Commits by *author* date — when the work was done, not when a rebase replayed it — from 22 to 29 August inclusive, across the 42 git repositories in the modus-lisp workspace. Eight days rather than the usual seven, because this issue was published on the Saturday that closes it and swept that morning's work in. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps. Produced with `bin/week 2026-08-29 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
