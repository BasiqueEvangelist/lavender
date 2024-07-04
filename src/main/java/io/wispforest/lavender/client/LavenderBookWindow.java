package io.wispforest.lavender.client;

import io.wispforest.lavender.book.Book;
import io.wispforest.lavender.book.LavenderBookItem;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.window.OwoWindow;
import io.wispforest.owo.ui.window.WindowIcon;
import io.wispforest.owo.ui.window.context.CurrentWindowContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class LavenderBookWindow extends OwoWindow<FlowLayout> {
    private Book book;
    private LavenderBookScreen screen;

    public LavenderBookWindow(Book book) {
        this.book = book;

        title(LavenderBookItem.itemOf(book).getName().getString());
        icon(WindowIcon.fromResources(Identifier.of("lavender:textures/item/red_book.png")));
        windowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, 1);
        windowHint(GLFW.GLFW_DECORATED, 0);
        windowHint(GLFW.GLFW_FLOATING, 1);
    }

    @Override
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    void resetScreen(Book book) {
        if (this.screen != null) screen.removed();

        var client = MinecraftClient.getInstance();
        this.book = book;
        this.screen = new LavenderBookScreen(book, false, this);
        try (var ignored = CurrentWindowContext.setCurrent(this)) {
            this.screen.init(client, scaledWidth(), scaledHeight());
        }
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        resetScreen(this.book);

        framebufferResized().subscribe((newWidth, newHeight) -> {
            var client = MinecraftClient.getInstance();
            this.screen.init(client, scaledWidth(), scaledHeight());
        });

        rootComponent.child(new BaseComponent() {
            @Override
            public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float partialTicks, float delta) {
                screen.render(context, mouseX, mouseY, delta);
            }

            @Override
            public boolean onMouseDown(double mouseX, double mouseY, int button) {
                return super.onMouseDown(mouseX, mouseY, button) | screen.mouseClicked(mouseX, mouseY, button);
            }

            @Override
            public boolean onMouseUp(double mouseX, double mouseY, int button) {
                return super.onMouseUp(mouseX, mouseY, button) | screen.mouseReleased(mouseX, mouseY, button);
            }

            @Override
            public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
                return super.onKeyPress(keyCode, scanCode, modifiers) | screen.keyPressed(keyCode, scanCode, modifiers);
            }

            @Override
            public boolean onCharTyped(char chr, int modifiers) {
                return super.onCharTyped(chr, modifiers) | screen.charTyped(chr, modifiers);
            }

            @Override
            public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
                return super.onMouseScroll(mouseX, mouseY, amount) | screen.mouseScrolled(mouseX, mouseY, 0, amount);
            }

            @Override
            public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
                return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button) | screen.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
            }

            @Override
            public boolean canFocus(FocusSource source) {
                return true;
            }
        }.sizing(Sizing.fill(100)));
    }
}
