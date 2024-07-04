package io.wispforest.lavender.client;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.CursorStyle;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class MoveButtonComponent extends ButtonComponent {
    private LavenderBookWindow window;
    private boolean buttoning = false;

    public MoveButtonComponent(Text message, Consumer<ButtonComponent> onPress) {
        super(message, onPress);
        cursorStyle(CursorStyle.MOVE);
    }

    public MoveButtonComponent attachWindow(LavenderBookWindow window) {
        this.window = window;
        return this;
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) this.buttoning = true;

        return super.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseUp(double mouseX, double mouseY, int button) {
        buttoning = false;

        return super.onMouseUp(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (this.window != null && buttoning) {
            // TODO: add screen width and height getters to OwoWindow
            int[] windowWidth = new int[1];
            int[] windowHeight = new int[1];

            GLFW.glfwGetWindowSize(this.window.handle(), windowWidth, windowHeight);

            double factor = window.scaleFactor() * windowWidth[0] / window.framebufferWidth();

            int[] windowX = new int[1];
            int[] windowY = new int[1];

            GLFW.glfwGetWindowPos(this.window.handle(), windowX, windowY);
            GLFW.glfwSetWindowPos(this.window.handle(), windowX[0] + (int)(deltaX * factor), windowY[0] + (int)(deltaY * factor));
        }

        return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
    }
}
