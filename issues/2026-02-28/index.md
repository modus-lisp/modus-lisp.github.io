# Nine architectures in four commits

**This Week in Modus №2** · 21–27 February 2026 · 4 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-02-28/>

Work resumes after eleven months, and not where it stopped. Modus is rewritten as a native 64-bit Lisp cross-compiled from SBCL — then, over four days, gains an actor system, symmetric multiprocessing, the ability to recompile itself from source it carries inside its own binary, and a portable instruction set with nine backends.

| Commits | Source files | Lines added | Architectures |
|---|---|---|---|
| 4 | 77 | +54k | 9 |

---

*modus / 23 February / 89f6713*

## Monday: a 64-bit Lisp with actors

Movitz is gone. The new image is cross-compiled from SBCL to x86-64 and boots to a Lisp REPL carrying a runtime native compiler, a Cheney copying collector, an E1000 driver, an SSH server, and Erlang-style actors with a heap each. Preemptive multitasking comes from reduction counting and the local APIC timer, and it is stable on eight cores.

Three of those are unusual to find in a first commit of a rewrite. A per-actor heap means collection is per-actor too, so one process’s garbage is not everybody’s pause. Reduction counting — interrupting a process after it has done a fixed amount of work rather than after a fixed amount of time — is the scheduling idea Erlang uses, and it is what makes a language runtime able to preempt itself without help from a kernel. And SMP on eight cores on day one sets the constraint that every later data structure has to live with.

---

*modus / 25 February / c73b202*

## Wednesday: the kernel recompiles itself

Generation zero is built by SBCL. Generation one is built by generation zero, at runtime, from source the kernel is carrying: the whole thing is serialized with symbols replaced by integer identifiers and embedded in the binary, so a running image has everything it needs to produce its successor without a host Lisp anywhere.

The pipeline for proving it is worth a sentence, because it is the shape of every gate this project will build later. Boot generation zero under QEMU, call `(build-image)`, then extract the result *out of the emulator’s memory* over the QEMU monitor protocol, and boot that. Not “the compiler ran without error” — the artefact is taken out and started.

The commit also mentions, in one clause, switching to actor one’s stack before entering the REPL so that the collector scans the right stack. That is the first appearance of a theme this dispatch will keep returning to: almost every hard bug in Modus is a disagreement about what the garbage collector can see.

![A terminal showing a table: Gen0 to Gen1 build, 13,239,158 bytes; Gen1 tests, 15 of 15 pass; Gen1 to Gen2 build, 13,239,158 bytes, identical size; Gen2 tests, 15 of 15 pass. Below it, key achievements and a root-cause note about rt-compile-defun.](https://modus-lisp.github.io/assets/img/selfhost-2026-02-24.png)

***The round trip closing, the day before it was committed.** Generation one and generation two come out at 13,239,158 bytes each — the same number twice, which is what reproducible means here — and both pass 15 of 15. The root cause underneath it is the kind this dispatch keeps finding: `rt-compile-defun` took `(car rest2)` and so compiled only the *first* body form of a multi-form `defun`, silently dropping the rest. Nothing had objected, because nothing was checking. nostr — 923e329f, 2026-02-24 · c73b202 is authored the following day. The author’s own account of the stretch: a fifteen-hour run, a short check-in, then another eight or so — about twenty-five hours end to end.*

---

*modus / 26 February / cd4eda7, 34a4eb5*

## Thursday: one bytecode, nine targets

The Modus Virtual Machine is a portable instruction set of about fifty opcodes that sits between the Lisp compiler and the machine. Source compiles to MVM bytecode; a per-target translator turns that into native code. The compiler stops knowing what a register is, and porting to a new processor stops meaning porting a compiler.

*modus — cd4eda7, all nine verified in QEMU*

```
x86-64   i386     AArch64
RISC-V64 PPC64    PPC32
ARM32    ARMv7    Motorola 68k

all produce correct serial output: factorial = 3628800
```

The self-hosting pipeline from Wednesday now runs through it — SBCL to MVM to x64 for generation zero, then generation zero compiles generation one from its own source, and generation one boots to a REPL with arithmetic, loops, factorial and a real-time clock read off the CMOS. And the same day, AArch64 stopped being a list entry: the image boots on QEMU virt *and* on a Raspberry Pi, with the SSH server and the crypto that go with it.

Four commits, one week, and the shape of the whole project is now fixed. A Lisp that owns every layer beneath it, a virtual instruction set so that owning it is affordable across machines, actors so that isolation is a language feature rather than a kernel feature, and a self-hosting cycle so that none of it depends on somebody else’s compiler for long.

---

**Method.** Commits by *author* date, 21 to 27 February 2026, across the git repositories in the modus-lisp workspace — at this point, one. Line counts exclude generated build artefacts. Produced with `bin/week 2026-02-28 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
