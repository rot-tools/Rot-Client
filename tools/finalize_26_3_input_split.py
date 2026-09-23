#!/usr/bin/env python3
from pathlib import Path


def write_if_changed(path: Path, text: str) -> bool:
    old = path.read_text(encoding="utf-8")
    if old == text:
        return False
    path.write_text(text, encoding="utf-8")
    return True

changed = []

# Live input calls belong to the client bridge. Keep the bridge itself using
# the common name normalizer.
for root_name in ("src/client/java", "src/plusClient/java"):
    root = Path(root_name)
    if not root.exists():
        continue
    for path in root.rglob("*.java"):
        if path.name == "QolInputRuntime.java":
            continue
        text = path.read_text(encoding="utf-8")
        new = text.replace("QolKeybindNames.", "QolInputRuntime.")
        # The earlier mechanical GLFW migration could introduce duplicate imports.
        lines = new.splitlines()
        seen_imports = set()
        out = []
        for line in lines:
            if line.startswith("import "):
                if line in seen_imports:
                    continue
                seen_imports.add(line)
            out.append(line)
        new = "\n".join(out) + ("\n" if new.endswith("\n") else "")
        if new != text:
            path.write_text(new, encoding="utf-8")
            changed.append(str(path))

# RingPolicy is common policy code; compare normalized configured names instead
# of Minecraft platform codes.
ring = Path("src/main/java/fi/rotclient/RingPolicy.java")
text = ring.read_text(encoding="utf-8")
text = text.replace("import com.mojang.blaze3d.platform.InputConstants;\n\n", "")
old_same = '''    public static boolean sameBind(String left, String right) {
        if (blank(left) || blank(right)) {
            return false;
        }
        Integer mouseLeft = QolKeybindNames.resolveMouseButton(left);
        Integer mouseRight = QolKeybindNames.resolveMouseButton(right);
        if (mouseLeft != null || mouseRight != null) {
            return mouseLeft != null && mouseLeft.equals(mouseRight);
        }
        int glfwLeft = QolKeybindNames.resolveGlfwKey(left, "");
        int glfwRight = QolKeybindNames.resolveGlfwKey(right, "");
        return glfwLeft != InputConstants.UNKNOWN.getValue() && glfwLeft == glfwRight;
    }
'''
new_same = '''    public static boolean sameBind(String left, String right) {
        return QolKeybindNames.sameBind(left, right);
    }
'''
if old_same not in text:
    raise SystemExit("RingPolicy sameBind block did not match expected source")
text = text.replace(old_same, new_same)
if write_if_changed(ring, text):
    changed.append(str(ring))

# Pure policy tests intentionally contain no client-only Minecraft classes.
menu_test = Path("src/test/java/fi/rotclient/MenuKeybindPolicyTest.java")
text = menu_test.read_text(encoding="utf-8")
text = text.replace("import com.mojang.blaze3d.platform.InputConstants;\n", "")
replacements = {
    "InputConstants.KEY_RIGHT": '"RIGHT"',
    "InputConstants.KEY_LEFT": '"LEFT"',
    "InputConstants.KEY_1": '"1"',
    "InputConstants.KEY_U": '"U"',
    "InputConstants.KEY_3": '"3"',
    "InputConstants.KEY_0": '"0"',
    "InputConstants.KEY_EQUALS": '"EQUAL"',
}
for old, new in replacements.items():
    text = text.replace(old, new)
start = '''    @Test
    void keyNameRoundTripForArrowsAndMouse() {
'''
if start in text:
    a = text.index(start)
    b = text.index("    }\n", a) + len("    }\n")
    replacement = '''    @Test
    void configuredKeyNamesNormalizeAcrossAliases() {
        assertEquals("RIGHT", QolKeybindNames.canonicalKeyName("right", ""));
        assertEquals("RIGHT_SHIFT", QolKeybindNames.canonicalKeyName("rshift", ""));
        assertEquals("LMB", QolKeybindNames.canonicalKeyName("mouse left", ""));
        assertEquals("RMB", QolKeybindNames.canonicalMouseName("MOUSE_2"));
        assertEquals("ESCAPE", QolKeybindNames.canonicalKeyName("esc", ""));
    }
'''
    text = text[:a] + replacement + text[b:]
if write_if_changed(menu_test, text):
    changed.append(str(menu_test))

click_test = Path("src/test/java/fi/rotclient/ClickGuiKeyPolicyTest.java")
text = click_test.read_text(encoding="utf-8")
text = text.replace("import com.mojang.blaze3d.platform.InputConstants;\n", "")
old = '''        assertEquals(
                InputConstants.KEY_RSHIFT,
                QolKeybindNames.resolveGlfwKey("", "RIGHT_SHIFT"));
        assertEquals(
                InputConstants.KEY_RSHIFT,
                QolKeybindNames.resolveGlfwKey("right shift", "RIGHT_SHIFT"));
'''
new = '''        assertEquals(
                "RIGHT_SHIFT",
                QolKeybindNames.canonicalKeyName("", "RIGHT_SHIFT"));
        assertEquals(
                "RIGHT_SHIFT",
                QolKeybindNames.canonicalKeyName("right shift", "RIGHT_SHIFT"));
'''
if old not in text:
    raise SystemExit("ClickGuiKeyPolicyTest expected block did not match")
text = text.replace(old, new)
if write_if_changed(click_test, text):
    changed.append(str(click_test))

# Track held mouse buttons from Minecraft's callback; no raw SDL/GLFW polling.
mouse_mixin = Path("src/client/java/fi/rotclient/mixin/MouseHandlerRingMixin.java")
text = mouse_mixin.read_text(encoding="utf-8")
needle = "        int button = buttonInfo == null ? -1 : buttonInfo.button();\n"
insert = needle + "        QolInputRuntime.updateMouseButton(button, action);\n"
if "QolInputRuntime.updateMouseButton" not in text:
    if needle not in text:
        raise SystemExit("MouseHandlerRingMixin button line did not match")
    text = text.replace(needle, insert)
    if "import fi.rotclient.QolInputRuntime;" not in text:
        text = text.replace(
            "import fi.rotclient.RingKeybindsRuntime;\n",
            "import fi.rotclient.QolInputRuntime;\nimport fi.rotclient.RingKeybindsRuntime;\n",
        )
if write_if_changed(mouse_mixin, text):
    changed.append(str(mouse_mixin))

print(f"Updated {len(changed)} files")
for path in changed:
    print(path)
