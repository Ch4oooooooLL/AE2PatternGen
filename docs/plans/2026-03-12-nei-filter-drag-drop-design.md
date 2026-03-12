# NEI Filter Drag-and-Drop Design

**Date:** 2026-03-12

## Goal

Allow every item-based generation filter field in the pattern generator GUI to accept items dragged directly from NEI and convert the dropped stack into the existing explicit filter syntax.

## Scope

Apply the behavior to these five fields in `GuiPatternGen`:

1. Output filter
2. Input filter
3. NC item filter
4. Blacklist input
5. Blacklist output

Do not change the recipe-map field or tier selector.

## Desired Behavior

When the player drags an item from NEI onto one of the supported filter text boxes, the target field is replaced with a formatted explicit token chosen by this priority:

1. If the stack has an item id and a meaningful meta value, write `[id:meta]`
2. If the stack only has an item id, write `[id]`
3. Otherwise, if the stack exposes ore dictionary names, write the first ore name as `(escapedOreName)`
4. Otherwise, write the stack display name as `{escapedDisplayName}`

The formatting must be compatible with the existing shared explicit matcher syntax.

## Design

### 1. Reuse ModularUI's NEI bridge

Do not register a second standalone NEI GUI handler for this feature.

`ModularUI` already implements `INEIGuiHandler` in `ModularGui` and forwards NEI drag-and-drop events to the currently hovered widget when that widget implements `IDragAndDropHandler`.

That means the clean integration point is a dedicated text field widget subclass, not a GUI-coordinate hook and not a second global NEI registration path.

### 2. Add a drag-aware filter text field

Create a small widget class that:

- extends `TextFieldWidget`
- implements `IDragAndDropHandler`
- accepts an `ItemStack`
- formats the stack into explicit filter text
- updates the field text immediately when a drag succeeds

This keeps the behavior local to the supported widgets and lets `GuiPatternGen` opt in field-by-field.

### 3. Extract explicit token formatting into a helper

Create a helper responsible only for converting an `ItemStack` into explicit filter syntax.

Responsibilities:

- read item id and meta safely
- treat wildcard metadata as "id only"
- choose the first usable ore dictionary name when id fallback is not appropriate
- fall back to a safe display name
- escape regex-significant characters for ore/display tokens so the dropped item becomes an exact literal match

This helper should not know about GUI classes.

### 4. Replace the target fields in `GuiPatternGen`

Swap the five supported `TextFieldWidget` instances for the new drag-aware widget.

No other GUI flow changes are required:

- preview still reads field text directly
- generate still reads field text directly
- save-on-close still sends the current text

## Edge Cases

- Wildcard metadata should format as `[id]`, not `[id:32767]`
- Empty ore dictionary names should be ignored
- Formatting codes must not leak into display-name tokens
- If a dropped stack is null or invalid, the handler should return `false`
- The handler should overwrite the current field contents, matching NEI search-field behavior

## Testing

1. Add helper tests for:
   - `[id:meta]` formatting
   - `[id]` formatting
   - ore fallback
   - display-name fallback
   - escaping special characters
2. Add widget-level drag handler tests for:
   - successful text replacement
   - rejecting null stacks
3. Run the targeted tests and then the full Gradle test suite.

## Acceptance Criteria

- Dragging an NEI item onto any supported filter field fills that field immediately
- `[id:meta]` is used when meta is meaningful
- `[id]` is used when only id should be preserved
- `(escapedOreName)` is used when id formatting is unavailable
- `{escapedDisplayName}` is used as the final fallback
- Existing explicit filter syntax and filter matching behavior remain unchanged
