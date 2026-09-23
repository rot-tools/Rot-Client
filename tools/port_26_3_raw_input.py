#!/usr/bin/env python3
from pathlib import Path

changed = []

def apply(path_str, transform):
    path = Path(path_str)
    old = path.read_text(encoding="utf-8")
    new = transform(old)
    if new != old:
        path.write_text(new, encoding="utf-8")
        changed.append(path_str)


def custom_cursor(text):
    text = text.replace("import org.lwjgl.glfw.GLFW;\n", "")
    text = text.replace(
        "    private static int lastCursorMode = Integer.MIN_VALUE;\n",
        "    private static Boolean lastCursorVisible;\n",
    )
    old = '''        int desired = hide
                ? GLFW.GLFW_CURSOR_HIDDEN
                : (menuOpen ? GLFW.GLFW_CURSOR_NORMAL : Integer.MIN_VALUE);
        if (hide) {
            GLFW.glfwSetInputMode(
                    client.getWindow().handle(),
                    GLFW.GLFW_CURSOR,
                    GLFW.GLFW_CURSOR_HIDDEN);
            lastCursorMode = desired;
        } else if (menuOpen) {
            if (desired != lastCursorMode) {
                GLFW.glfwSetInputMode(
                        client.getWindow().handle(),
                        GLFW.GLFW_CURSOR,
                        GLFW.GLFW_CURSOR_NORMAL);
                lastCursorMode = desired;
            }
        }
'''
    new = '''        if (hide) {
            if (!Boolean.FALSE.equals(lastCursorVisible)) {
                PlatformInputRuntime.setCursorVisible(false);
                lastCursorVisible = false;
            }
        } else if (menuOpen && !Boolean.TRUE.equals(lastCursorVisible)) {
            PlatformInputRuntime.setCursorVisible(true);
            lastCursorVisible = true;
        }
'''
    if old not in text:
        raise SystemExit("CustomCursorRuntime cursor-mode block did not match")
    text = text.replace(old, new)
    old = '''        boolean pressed = GLFW.glfwGetMouseButton(
                client.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS;
'''
    new = '''        boolean pressed = QolInputRuntime.isBoundDown(
                client.getWindow().handle(), "LMB");
'''
    if old not in text:
        raise SystemExit("CustomCursorRuntime mouse polling block did not match")
    text = text.replace(old, new)
    old = '''        GLFW.glfwSetCursorPos(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
'''
    new = '''        PlatformInputRuntime.warpCursor(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
'''
    if old not in text:
        raise SystemExit("CustomCursorRuntime warp block did not match")
    return text


def stall(text):
    text = text.replace("import org.lwjgl.glfw.GLFW;\n", "")
    old = '''        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
'''
    new = '''        return QolInputRuntime.isKeyDown(
                        window, QolInputRuntime.resolveGlfwKey("LEFT_CONTROL", ""))
                || QolInputRuntime.isKeyDown(
                        window, QolInputRuntime.resolveGlfwKey("RIGHT_CONTROL", ""));
'''
    if old not in text:
        raise SystemExit("StallMarketRuntime ctrl block did not match")
    return text.replace(old, new)


def gui_cursor(text):
    text = text.replace("import org.lwjgl.glfw.GLFW;\n", "")
    if "import fi.rotclient.PlatformInputRuntime;\n" not in text:
        text = text.replace(
            "import fi.rotclient.NoCursorResetPolicy;\n",
            "import fi.rotclient.NoCursorResetPolicy;\nimport fi.rotclient.PlatformInputRuntime;\n",
        )
    old = '''        GLFW.glfwSetCursorPos(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
'''
    new = '''        PlatformInputRuntime.warpCursor(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
'''
    if old not in text:
        raise SystemExit("GuiNoCursorResetMixin warp block did not match")
    return text.replace(old, new)


def client(text):
    text = text.replace(
        "import org.lwjgl.glfw.GLFW;\n",
        "import com.mojang.blaze3d.platform.InputConstants;\n",
    )
    # Deduplicate if an InputConstants import was already introduced.
    lines = text.splitlines()
    seen = set()
    out = []
    for line in lines:
        if line.startswith("import ") and line in seen:
            continue
        if line.startswith("import "):
            seen.add(line)
        out.append(line)
    text = "\n".join(out) + "\n"
    text = text.replace("GLFW.GLFW_MOUSE_BUTTON_LEFT", "InputConstants.MOUSE_BUTTON_LEFT")
    text = text.replace("GLFW.GLFW_KEY_LEFT_SHIFT", "InputConstants.KEY_LSHIFT")
    text = text.replace("GLFW.GLFW_KEY_RIGHT_SHIFT", "InputConstants.KEY_RSHIFT")
    old = '''                                        boolean shift = GLFW.glfwGetKey(
                                                client.getWindow().handle(),
                                                InputConstants.KEY_LSHIFT)
                                                == GLFW.GLFW_PRESS
                                                || GLFW.glfwGetKey(
                                                client.getWindow().handle(),
                                                InputConstants.KEY_RSHIFT)
                                                == GLFW.GLFW_PRESS;
'''
    new = '''                                        boolean shift = QolInputRuntime.isKeyDown(
                                                client.getWindow().handle(), InputConstants.KEY_LSHIFT)
                                                || QolInputRuntime.isKeyDown(
                                                client.getWindow().handle(), InputConstants.KEY_RSHIFT);
'''
    if old not in text:
        raise SystemExit("RotClientClient shift polling block did not match")
    text = text.replace(old, new)
    old = '''        GLFW.glfwSetCursorPos(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
'''
    new = '''        PlatformInputRuntime.warpCursor(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
'''
    if old not in text:
        raise SystemExit("RotClientClient warp block did not match")
    text = text.replace(old, new)
    return text

apply("src/client/java/fi/rotclient/CustomCursorRuntime.java", custom_cursor)
apply("src/client/java/fi/rotclient/StallMarketRuntime.java", stall)
apply("src/client/java/fi/rotclient/mixin/GuiNoCursorResetMixin.java", gui_cursor)
apply("src/client/java/fi/rotclient/RotClientClient.java", client)

print(f"Updated {len(changed)} raw-input files")
for path in changed:
    print(path)
