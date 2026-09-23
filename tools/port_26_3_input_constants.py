#!/usr/bin/env python3
from pathlib import Path

ROOTS = [Path("src/main/java"), Path("src/client/java"), Path("src/test/java")]

# These files call GLFW functions directly. They need semantic SDL/vanilla-input
# changes rather than a blind constant rename and are handled separately.
SKIP = {
    Path("src/main/java/fi/rotclient/QolKeybindNames.java"),
    Path("src/client/java/fi/rotclient/CustomCursorRuntime.java"),
    Path("src/client/java/fi/rotclient/StallMarketRuntime.java"),
    Path("src/client/java/fi/rotclient/RotClientClient.java"),
    Path("src/client/java/fi/rotclient/mixin/GuiNoCursorResetMixin.java"),
}

KEY_MAP = {
    "GLFW_KEY_UNKNOWN": "UNKNOWN.getValue()",
    "GLFW_KEY_LEFT_SHIFT": "KEY_LSHIFT",
    "GLFW_KEY_RIGHT_SHIFT": "KEY_RSHIFT",
    "GLFW_KEY_LEFT_CONTROL": "KEY_LCONTROL",
    "GLFW_KEY_RIGHT_CONTROL": "KEY_RCONTROL",
    "GLFW_KEY_LEFT_ALT": "KEY_LALT",
    "GLFW_KEY_RIGHT_ALT": "KEY_RALT",
    "GLFW_KEY_LEFT_SUPER": "KEY_LGUI",
    "GLFW_KEY_RIGHT_SUPER": "KEY_RGUI",
    "GLFW_KEY_ESCAPE": "KEY_ESCAPE",
    "GLFW_KEY_TAB": "KEY_TAB",
    "GLFW_KEY_SPACE": "KEY_SPACE",
    "GLFW_KEY_ENTER": "KEY_RETURN",
    "GLFW_KEY_BACKSPACE": "KEY_BACKSPACE",
    "GLFW_KEY_INSERT": "KEY_INSERT",
    "GLFW_KEY_DELETE": "KEY_DELETE",
    "GLFW_KEY_HOME": "KEY_HOME",
    "GLFW_KEY_END": "KEY_END",
    "GLFW_KEY_PAGE_UP": "KEY_PAGEUP",
    "GLFW_KEY_PAGE_DOWN": "KEY_PAGEDOWN",
    "GLFW_KEY_UP": "KEY_UP",
    "GLFW_KEY_DOWN": "KEY_DOWN",
    "GLFW_KEY_LEFT": "KEY_LEFT",
    "GLFW_KEY_RIGHT": "KEY_RIGHT",
    "GLFW_KEY_MINUS": "KEY_MINUS",
    "GLFW_KEY_EQUAL": "KEY_EQUALS",
    "GLFW_KEY_CAPS_LOCK": "KEY_CAPSLOCK",
    "GLFW_KEY_LEFT_BRACKET": "KEY_LBRACKET",
    "GLFW_KEY_RIGHT_BRACKET": "KEY_RBRACKET",
    "GLFW_KEY_KP_ENTER": "KEY_NUMPADENTER",
    "GLFW_KEY_KP_0": "KEY_NUMPAD0",
    "GLFW_KEY_KP_1": "KEY_NUMPAD1",
    "GLFW_KEY_KP_2": "KEY_NUMPAD2",
    "GLFW_KEY_KP_3": "KEY_NUMPAD3",
    "GLFW_KEY_KP_4": "KEY_NUMPAD4",
    "GLFW_KEY_KP_5": "KEY_NUMPAD5",
    "GLFW_KEY_KP_6": "KEY_NUMPAD6",
    "GLFW_KEY_KP_7": "KEY_NUMPAD7",
    "GLFW_KEY_KP_8": "KEY_NUMPAD8",
    "GLFW_KEY_KP_9": "KEY_NUMPAD9",
    "GLFW_MOUSE_BUTTON_LEFT": "MOUSE_BUTTON_LEFT",
    "GLFW_MOUSE_BUTTON_RIGHT": "MOUSE_BUTTON_RIGHT",
    "GLFW_MOUSE_BUTTON_MIDDLE": "MOUSE_BUTTON_MIDDLE",
    "GLFW_MOUSE_BUTTON_4": "MOUSE_BUTTON_4",
    "GLFW_MOUSE_BUTTON_5": "MOUSE_BUTTON_5",
    "GLFW_PRESS": "PRESS",
    "GLFW_RELEASE": "RELEASE",
    "GLFW_REPEAT": "REPEAT",
}

# Alpha, number-row and function-key names are unchanged apart from the prefix.
for c in "ABCDEFGHIJKLMNOPQRSTUVWXYZ":
    KEY_MAP[f"GLFW_KEY_{c}"] = f"KEY_{c}"
for n in range(10):
    KEY_MAP[f"GLFW_KEY_{n}"] = f"KEY_{n}"
for n in range(1, 25):
    KEY_MAP[f"GLFW_KEY_F{n}"] = f"KEY_F{n}"

changed_files = []
for root in ROOTS:
    if not root.exists():
        continue
    for path in root.rglob("*.java"):
        if path in SKIP:
            continue
        text = path.read_text(encoding="utf-8")
        original = text

        # Longest tokens first so KP/number/function variants cannot be partly replaced.
        for old, new in sorted(KEY_MAP.items(), key=lambda item: len(item[0]), reverse=True):
            text = text.replace(f"GLFW.{old}", f"InputConstants.{new}")

        # Some call sites used the fully-qualified GLFW class inline.
        text = text.replace("org.lwjgl.glfw.InputConstants.", "InputConstants.")

        if text != original:
            text = text.replace(
                "import org.lwjgl.glfw.GLFW;",
                "import com.mojang.blaze3d.platform.InputConstants;",
            )
            if "InputConstants." in text and "import com.mojang.blaze3d.platform.InputConstants;" not in text:
                package_end = text.find(";\n", text.find("package "))
                if package_end >= 0:
                    insert_at = package_end + 2
                    text = text[:insert_at] + "\nimport com.mojang.blaze3d.platform.InputConstants;" + text[insert_at:]
            path.write_text(text, encoding="utf-8")
            changed_files.append(str(path))

print(f"Migrated {len(changed_files)} files to InputConstants")
for path in changed_files:
    print(path)
