# NEI Filter Drag-and-Drop Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add NEI drag-and-drop support to all item-based filter text fields in the pattern generator GUI and convert dropped stacks into the existing explicit filter syntax.

**Architecture:** Reuse ModularUI's existing NEI drag bridge instead of adding another global NEI handler. Introduce a drag-aware text field widget that implements `IDragAndDropHandler`, plus a small formatter utility that maps `ItemStack` data to `[id]`, `[id:meta]`, `(ore)`, or `{display}` tokens.

**Tech Stack:** Java 8, JUnit 4, GTNH ModularUI, NotEnoughItems, Minecraft 1.7.10 mod code

---

### Task 1: Add failing formatter tests

**Files:**
- Create: `D:/CODE/AE2PatternGen/src/test/java/com/github/ae2patterngen/gui/ExplicitFilterDropFormatterTest.java`
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/gui/ExplicitFilterDropFormatter.java`

**Step 1: Write the failing test**

Add tests for:
- formatting `[id:meta]` when meta is meaningful
- formatting `[id]` when meta should not be included
- falling back to the first ore dictionary entry
- falling back to escaped display-name token
- escaping special characters in ore/display tokens

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.gui.ExplicitFilterDropFormatterTest`

Expected: FAIL because the formatter class does not exist yet.

**Step 3: Write minimal implementation**

Implement the formatter helper with safe id, meta, ore, and display-name handling.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.github.ae2patterngen.gui.ExplicitFilterDropFormatterTest`

Expected: PASS

### Task 2: Add failing drag-aware text field tests

**Files:**
- Create: `D:/CODE/AE2PatternGen/src/test/java/com/github/ae2patterngen/gui/FilterTextFieldWidgetTest.java`
- Create: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/gui/FilterTextFieldWidget.java`

**Step 1: Write the failing test**

Add tests for:
- `handleDragAndDrop` replaces the text with the formatted token
- null stack returns `false`
- successful drop returns `true`

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests com.github.ae2patterngen.gui.FilterTextFieldWidgetTest`

Expected: FAIL because the widget class does not exist yet.

**Step 3: Write minimal implementation**

Implement a `TextFieldWidget` subclass that also implements `IDragAndDropHandler` and delegates stack-to-text conversion to the formatter helper.

**Step 4: Run test to verify it passes**

Run: `./gradlew test --tests com.github.ae2patterngen.gui.FilterTextFieldWidgetTest`

Expected: PASS

### Task 3: Integrate the new widget into the pattern generator GUI

**Files:**
- Modify: `D:/CODE/AE2PatternGen/src/main/java/com/github/ae2patterngen/gui/GuiPatternGen.java`

**Step 1: Write or extend a failing test if practical**

If a direct GUI construction test is practical, add one. If not, rely on the widget-level tests and keep GUI integration minimal.

**Step 2: Write minimal implementation**

Replace these five `TextFieldWidget` instances with the new drag-aware widget:
- output filter
- input filter
- NC item filter
- blacklist input
- blacklist output

Keep existing geometry, colors, background, alignment, and saved-field loading behavior unchanged.

**Step 3: Run focused tests**

Run: `./gradlew test --tests com.github.ae2patterngen.gui.ExplicitFilterDropFormatterTest --tests com.github.ae2patterngen.gui.FilterTextFieldWidgetTest`

Expected: PASS

### Task 4: Run project verification

**Files:**
- No code changes expected

**Step 1: Run focused verification**

Run: `./gradlew test --tests com.github.ae2patterngen.gui.ExplicitFilterDropFormatterTest --tests com.github.ae2patterngen.gui.FilterTextFieldWidgetTest`

Expected: PASS

**Step 2: Run broader verification**

Run: `./gradlew test`

Expected: PASS

**Step 3: Review diff**

Run: `git diff --stat`

Expected: only the new formatter/widget, `GuiPatternGen`, tests, and docs are included.
