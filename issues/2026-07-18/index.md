# Ten repositories in seven days

**This Week in Modus №19** · 11–17 July 2026 · 291 commits · 19 repos

<https://modus-lisp.github.io/issues/2026-07-18/>

A crypto suite on Sunday. An SSH client on Tuesday. A git implementation on Wednesday. A filesystem, a compression codec, a package distribution, an agent and a VNC desktop on Thursday. What makes it a week rather than a list is that by Friday they are standing on each other — and on a B-tree written a month ago for something else entirely.

| Commits | Repos moved | New repos | Source files | Lines added |
|---|---|---|---|---|
| 291 | 19 | 10 | 366 | +55k |

---

*the workspace / 10 first commits*

## Each one needs the last

The order is the story. natrium lands on Sunday as a hashing floor, then a random number generator, then an authenticated cipher, then key agreement, then signatures — the whole NaCl primitive set by that evening, made constant-time on Monday. conch appears on Tuesday and takes its crypto from natrium rather than writing its own; by Thursday it has public-key authentication, command execution, file transfer and a server. And seal, eight days old, deletes its own symmetric and curve implementations in favour of natrium’s the same week.

Then the chain that has been four weeks in the building. [№ 16](https://modus-lisp.github.io/issues/2026-06-27/) introduced pagetree, a copy-on-write B+tree, and made it the Bitcoin node’s UTXO store within four days. This week cabinet becomes a filesystem — inodes, directories and file contents in one keyspace — on top of that tree. And cairn, the from-scratch git, learns to keep an entire repository inside a cabinet.

> Route cairn’s git store through a small backend protocol, so the same cairn can keep a repository on the host filesystem or inside a cabinet, which lives in a single pagetree file. This is what lets cairn run where there is no host filesystem at all.

> — cairn — bc06808

That last clause is the project’s whole argument in one line, and it is the reason a B-tree written for a Bitcoin node matters to a version control system. Every library here is written as though the operating system underneath might not be there, because the destination — the bare-metal machine of [№ 3](https://modus-lisp.github.io/issues/2026-03-07/) — is a place where it is not.

A related discipline shows up in a one-line cairn commit that drops a shell-out to `nproc`: nothing in the core chain may spawn a process. On a hosted Linux that is a stylistic preference. On the target it is the difference between running and not.

---

*glass / 35 commits, most of them on one day*

## A framebuffer on Thursday, a desktop on Friday

It begins as “a framebuffer and a VNC/RFB server in pure Common Lisp” under a different name, gains dirty-region tracking, a lossless tile encoding about eighty-eight times smaller than raw, and a compressed encoding riding cram’s stream — which was written the same morning. Then it is renamed glass. Then Friday happens.

A McCLIM backend, modelled on the X framebuffer one: CLIM applications render into a glass framebuffer and are served over the wire, with keyboard and pointer coming back from the VNC client — no X, no foreign code. Resize in both directions, stock CLIM applications running unmodified, a compositor for menus and pop-ups, real keyboard focus so the Listener evaluates over the network, and a small OPEN LOOK window manager. Then the window manager stops using McCLIM for its own decoration, because glass now owns text through scribe.

And then a terminal emulator, which arrives essentially complete: a real pty, ANSI escapes, a scribe glyph grid, UTF-8 with wide characters and fallback fonts, colour emoji through scribe’s layered-glyph support, a hardened VT with 256 colours and the alternate screen so that `htop` and `vim` work, mouse reporting with a controlling terminal, tabs, and inline bitmap graphics. The root menu offers a browser window, an image viewer, a terminal, a calculator, and — from the Lisp machine lineage this project keeps reaching for — an object inspector and a graphical debugger.

![A VNC session titled glass-mcclim showing overlapping windows: a loom browser window rendering a page with headings v0, v1 and v2, a McCLIM gadget test window with sliders, and a glass splash window behind them.](https://modus-lisp.github.io/assets/img/desktop-2026-07-18.png)

***The desktop, over the wire.** loom rendering its own demo page — a live layout blitted to SDL, link navigation, and a `setTimeout` counter running — beside a stock McCLIM gadget window and glass’s own splash. *“i’m just opening apps that don’t have close boxes and dragging windows that don’t focus around and browsing poorly rendered websites at 2 fps BECAUSE I CAN.”* nostr — c7b032e2, 2026-07-18 03:19:38Z*

![An object inspector window titled Inspector: #<WINSP::LISPM>. It lists direct slots: NAME is Genera, YEAR is 1988, AUTHORS is a list of symbolics, lmi and ti, SPECS is a hash table, WORDS is a vector, and CPU expands inline into a nested object whose NAME is Ivory and BITS is 40.](https://modus-lisp.github.io/assets/img/inspector-2026-07-17.jpg)

***The inspector, inspecting the thing it descends from.** A live object browser with nested slots expanded in place; the object on the table is a `LISPM` whose name is *Genera*, year 1988, CPU *Ivory*, 40 bits. *“can’t claim genera heritage without a live object inspector.”* nostr — 1f2672d1, 2026-07-17 22:18:24Z*

![A terminal window inside the Lisp desktop running Claude Code: a release-notes banner, a warning that three MCP servers need authentication, a promotional notice, and a prompt at the bottom reading manual mode on.](https://modus-lisp.github.io/assets/img/claude-in-lisp-term-2026-07-18.png)

***The tool, running inside its own output.** Claude Code in a terminal emulator it wrote, using a rasterizer it wrote, under a window manager it wrote, on a framebuffer it wrote, served by a VNC server it wrote. The author’s note lists that chain and then adds the sting: *“do you have any idea how much easier this was to build than the common lisp operating system that claude code hasn’t finished yet?”* nostr — 71dc3b4d, 2026-07-18 03:31:31Z · cropped to remove the account banner*

---

*cairn / 27 commits over two days*

## git, read then written then served

Wednesday: read a repository — hashes, compression, loose objects, refs, log — then packfiles with delta-compressed objects, then clone over the smart HTTP transport, then checkout, then the write side, at which point real git accepts the commits it produces. Then status and diff matching git’s output, push over SSH, fetch and fast-forward pull, a three-way merge with a pluggable conflict resolver, recursive merge over multiple merge bases for criss-cross histories, delta compression in written packs within a few percent of git’s own, and repositories using the newer hash function, git-validated.

The acceptance criterion in almost every one of those subjects is agreement with the real implementation rather than an internal test passing — the same standard [№ 15](https://modus-lisp.github.io/issues/2026-06-20/) held itself to against Bitcoin Core and [№ 17](https://modus-lisp.github.io/issues/2026-07-04/) against `ttx` and HarfBuzz.

Thursday is the part that matters for a machine with no Unix underneath: the storage backend seam, a worktree backend so a full working tree lives in a cabinet, a direct pagetree backend where a commit and a push are one transaction, and cairn’s own git server speaking the upload and receive protocols over SSH — over conch, two days old. Two performance fixes on the way: streaming pack indexing for bounded memory, which fixed cloning large repositories out of memory, and a parallel resolve about eight times faster.

---

*modus / 43 commits / WS4, WS5*

## A CLI that compiles without SBCL

Modus has been self-hosting in one sense since [№ 2](https://modus-lisp.github.io/issues/2026-02-28/), where a running image compiled its successor from source embedded in its own binary, and [№ 4](https://modus-lisp.github.io/issues/2026-03-14/) proved the compiler leaves no fingerprint of the machine that ran it. But the toolchain that produced those images still started from a host Lisp. This week a `modus` binary appeared with the entire x64 build pipeline baked into it, compiling Lisp to a native Linux executable in-image, with SBCL needed only to seed the CLI once.

What stood between that and true self-hosting was reading. The compiler could not read its own sources inside the image — a lenient recovery for missing package qualifiers took the skipped forms from 204 to zero. A compiler that cannot parse itself is not self-hosting no matter how good its code generation is, which is a thing that only becomes obvious when you try.

In parallel, the runtime JIT came up in five staged pieces, each landing inert before anything called it: the translator baked into the evaluator image as dead code, real forms compiled end to end against an interpreter oracle, native call relocation for calls leaving a module, a constant pool with literal patching that survives collection, and finally the wiring into production evaluation behind a flag. The oracle in step two is the same in-image differential gate [№ 17](https://modus-lisp.github.io/issues/2026-07-04/) used to flip evaluators.

---

*weft loom / 123 commits*

## Tables, baselines, and a browser with tabs

weft spent 105 commits in the least glamorous corners of CSS, which is where the difference between a renderer and a browser lives: table height distributed across rows, a cell that never shrinks below its content, mixed-unit column widths, flex item margins on both axes and in both directions, minimum and maximum clamping on flex, grid and table containers, `position: sticky` with the right containing block, and a run of line-box work — text painted on the baseline, atomic inlines aligned to it, the strut descent kept under a tall one.

loom turned into something usable: per-tab sessions with distinct browsing contexts, a per-context cookie jar threaded through the render, a tab bar with a pinned engine-status tab, a thread per connection so one render cannot freeze the service, and each navigation bound to the tab that began it. Bounded waits for web fonts and image prefetching, so a slow asset degrades a page rather than stalling it. And by Friday loom had a glass backend — the browser driven over VNC, with no SDL and no foreign code anywhere in the path, one week after [№ 18](https://modus-lisp.github.io/issues/2026-07-11/) replaced its SDL bindings by hand.

---

*also this week*

## The rest of it

- operandi arrived as a Lisp-native agent loop and grew an interactive REPL with streaming and tool rendering the same day. Its launcher went from about sixteen seconds to sixty milliseconds by dumping a core.

- cram was a compressor on Thursday morning and a full codec by the afternoon. cairn dropped its two third-party compression libraries for it before the week ended, and glass’s encoder was built on it from the start.

- webrtc-data went from a signalling and NAT-traversal foundation to a working data channel in a day, each rung verified against an established implementation rather than against itself.

- dist began publishing the workspace as a Quicklisp distribution, which is how any of this becomes installable by anyone else.

- cl-frpc appeared to reverse-proxy through cl-transport’s new inbound side — the first answer to “how does a machine behind NAT get reached at all”, which is a question a desktop you can connect to is about to need.

---

**Method.** Commits by *author* date, 11 to 17 July 2026, across the 42 git repositories in the modus-lisp workspace. Line counts exclude generated acceptance-gate artefacts and committed scratch dumps. Produced with `bin/week 2026-07-18 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
