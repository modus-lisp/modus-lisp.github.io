# The Fixpoint of Theseus

**This Week in Modus №4** · 7–13 March 2026 · 3 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-03-14/>

If a compiler is portable, the machine it ran on should not be detectable in what it produced. This week Modus proved that with five SHA-256 comparisons across three architectures — and, having proved it, deleted the foundation it started on.

| Commits | Source files | Lines added | Lines removed |
|---|---|---|---|
| 3 | 392 | +17k | −84k |

---

*modus / 7 March / b3c2cbb*

## The compiler comes down to the metal

The MVM compiler had been running on a host Lisp. It now runs on bare metal: an x86-64 image with no operating system compiles Lisp source to MVM bytecode, and the checksum of what it produces matches the SBCL reference exactly. Seven test functions, including recursion and control flow.

That single fact makes a much stranger claim testable. If the compiler is genuinely portable, then a program compiled by an x64 image and a program compiled by an AArch64 image should be the *same bytes* — the host architecture should leave no fingerprint. So: SBCL builds generation zero for x64, generation zero builds generation one for AArch64, generation one builds generation two back on x64. The checksum of generation two matches the SBCL reference, and generation one matches generation three byte for byte.

Getting there needed a real compiler bug fixed first, and it is a good one: the MVM compiler mishandled push and pop in the crypto and SSH code, so function calls had to be hoisted out of loop arithmetic and computed beforehand. The cross-compiled AArch64 image then booted as an SSH server with a working REPL — E1000, IP, TCP, DHCP, ChaCha20-Poly1305, Ed25519 and SSH-2, none of it recompiled by a host.

---

*modus / 8–9 March / c97356b, 276870a*

## Five checks, three architectures

Sunday added i386 as a complete platform — Multiboot1 boot, a native translator, a serial REPL, an NE2000 ISA network card, and an SSH server with full crypto done in pairs of words, because a 32-bit target has 30-bit fixnums and the ciphers do not care about your word size. Monday used it to close the proof.

*modus — run-fixpoint-i386.sh, eight QEMU steps*

```
SHA256(Gen1)      == SHA256(Gen3)       x64 ↔ AArch64 fixpoint
SHA256(i386-A)    == SHA256(i386-B)     i386 translator determinism
SHA256(i386-A)    == SHA256(i386-C)     i386 self-hosting fixpoint
SHA256(x64-D)     == SHA256(Gen2)       i386 → x64 cross-compilation
SHA256(AArch64-E) == SHA256(Gen1)       i386 → AArch64 cross-compilation
```

A 32-bit machine compiling a 64-bit image that is bit-identical to one a 64-bit machine produced is the strongest statement of portability available, and it is checked by a script rather than believed.

### What it cost

The fixes underneath are a catalogue of everywhere 32 bits leaks. Values that fit comfortably in a 64-bit fixnum — function-table hashes, the kernel-main hash, magic bytes — overflow i386’s 230 range, so they are copied as raw bytes rather than computed as integers; the x64 boot preamble’s control-register constants are emitted one byte at a time for the same reason. Deeply nested functions had to be split into shallower helpers to fit within a seven-slot spill budget. And two bugs are the uninitialised-memory hazard from [№ 3](https://modus-lisp.github.io/issues/2026-03-07/) wearing a new costume: a spill-tracking array reported every register unspilled because bare-metal `make-array` ignores `:initial-element`, and every keyword parameter was removed from the AArch64 instruction encoders because uninitialised optional arguments behaved differently on different architectures.

The rest of the commit is subtraction. The tree was consolidated, the 64-bit work moved to the root, and **Movitz was removed** — which is the eighty-four thousand deleted lines in the tally above, and the end of the thread [№ 1](https://modus-lisp.github.io/issues/2025-03-15/) started. A feature matrix and a suite of nineteen QEMU tests went in where it had been.

---

**Method.** Commits by *author* date, 7 to 13 March 2026, across the git repositories in the modus-lisp workspace — at this point, one. Line counts exclude generated build artefacts. Produced with `bin/week 2026-03-14 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
