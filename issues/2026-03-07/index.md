# A Pi with no SD card

**This Week in Modus №3** · 28 February – 6 March 2026 · 3 commits · 1 repo

<https://modus-lisp.github.io/issues/2026-03-07/>

Modus left the emulator. It runs on real Pi Zero 2 W silicon, booted over USB with no card in the slot, and is reached over SSH across a USB Ethernet gadget it implements itself — while the MVM compiler learns to compile its own 759 functions.

| Commits | Source files | Lines added | Functions self-compiled |
|---|---|---|---|
| 3 | 64 | +12.6k | 759 |

---

*modus / 2 March / bc14258*

## USB, from both ends

A Raspberry Pi has no serial port you can casually plug into and no Ethernet jack on the small ones, so reaching one means USB — and USB is not a thing an operating system gets for free. This week Modus wrote both halves of it.

As a *host*, on an emulated Pi 3B: a DWC2 host controller driver, device enumeration, hub support, and CDC Ethernet, with the SSH server running over that same network stack. Then USB HID over the boot protocol — keyboard, mouse and tablet — with the keyboard driving the REPL directly. As a *device*, on a real Pi Zero 2 W: the same controller in gadget mode presenting CDC-ECM Ethernet to the machine it is plugged into, so the Pi appears as a network interface on your desk.

Around it, the peripherals a board actually needs: the BCM2835 system timer, a GPIO LED, the GPU framebuffer, and the mini UART at `0x3F215040` with 32-bit stores and TX-ready polling. And a serial bootloader, which is the difference between an afternoon and a week: a permanent kernel on the SD card receives a new kernel over the wire at 115,200 baud and jumps to it at `0x300000`, driven by a host-side script that resets the board over GPIO17 first.

### Where the emulator stops telling the truth

Two fixes in this commit exist only because the work moved to real silicon. The MVM `YIELD` opcode had been compiled to a bare `WFE` — wait for event — which is correct under emulation and stalls a real Cortex-A53 outright; it now pairs with an `SEV`. And the hardware random number generator on the BCM2710A1 crashes, so entropy comes from the timer instead. There is also a persistent bulk-IN channel fix for a reconnection bug in QEMU’s DWC2, which is the mirror image of the same problem: the emulator lying in the other direction.

### Actors as isolation

The AArch64 side gained the cooperative actor system — spawn, yield, mailbox send and receive, a scheduler, and real context switching through save and restore of the machine context — with a four-megabyte heap per actor and soft allocation that can fail rather than trap. The arrangement it enables is described in the commit as Qubes-like, and it is the first statement of a principle the project keeps: **a net-domain actor owns the hardware, and SSH handlers reach it only through typed mailbox messages, with the boundary drawn at the TCP byte stream.** Isolation is a property of the language runtime here, not of a kernel that has to be trusted underneath one.

---

*modus / 5 March / 133df85*

## Real hardware, over a wire it made itself

Three days later the whole stack runs on the real board: DWC2 in gadget mode with interrupt-driven receive through a four-slot ring buffer serviced by an ISR at `0x1000`, an SSH server with Ed25519, X25519 and ChaCha20, an HTTP client with DNS resolution and an HTTP server on port 80, and actors scheduling all of it. It boots over `rpiboot` with no SD card at all, and redeploys over the UART bootloader.

![A photograph of a monitor showing a colour test pattern: red, green and blue rectangles across the top, a white bar across the middle, and yellow, magenta and cyan rectangles below, all on a dark blue background. A Raspberry Pi Zero 2 W is held up in front of the screen, wired over USB.](https://modus-lisp.github.io/assets/img/colors-2026-03-02.jpg)

***Six colours and a background, out of a GPU nobody documented for this.** A 640×480 32-bit framebuffer obtained through the VideoCore mailbox property tags, with the pixel order corrected to BGR — driven from bare-metal Lisp on the board being held up in front of it. The exchange, in full: *“How do the colors look now?”* — *“boom! red green blue, background bar, white bar, yellow magenta cyan, dark blue background.”* nostr — 43e4cd6a, 2026-03-02 04:54:49Z*

Two of the fixes name a constraint that only exists when a Lisp is the network stack. USB Ethernet has a five-second watchdog; a cold boot that computes its crypto keys when first asked will blow through it, so the keys are pre-computed at boot — and the long crypto loops (X25519, Ed25519, field squaring) poll USB inside themselves, because there is no kernel to do it for them while the CPU is busy multiplying.

And one line in the changelog is going to come back for months:

> Uninitialized RAM fixes throughout — `make-array` doesn’t zero on real hardware.

> — modus — 133df85

Under an emulator, fresh memory tends to be zeros, so code that forgets to initialise something works perfectly. On a real board it is whatever was there before. This is a hazard class rather than a bug, and the repository will be finding members of it for a long time.

### The compiler starts compiling itself

The other half of the commit is the MVM compiler compiling its own 759 functions to architecture-independent bytecode. Making that possible meant giving the bytecode a Lisp big enough to express a compiler in: a prelude with MVM-compilable hash tables, `sort`, `mapcar`, `gensym` and `symbol-value`; new opcodes for `funcall`, `multiple-value-bind` and `flet`; macros for `ldb`, `list`, `vector` and multi-place `setf`. All seven translators were updated in step, which is the tax the portable-ISA design charges and the reason it keeps being worth paying.

The week’s third commit is a slide deck for a talk at ATL BitLab, with an empty message. Somebody stood up and showed people this.

---

**Method.** Commits by *author* date, 28 February to 6 March 2026, across the git repositories in the modus-lisp workspace — at this point, one. Line counts exclude generated build artefacts. Produced with `bin/week 2026-03-07 --log`.

[← All issues](https://modus-lisp.github.io/issues/) · [crier](https://modus-lisp.github.io/)
