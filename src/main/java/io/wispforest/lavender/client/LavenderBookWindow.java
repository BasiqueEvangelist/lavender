package io.wispforest.lavender.client;

import io.wispforest.lavender.book.Book;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.window.OwoWindow;
import io.wispforest.owo.ui.window.context.CurrentWindowContext;
import net.minecraft.client.MinecraftClient;

public class LavenderBookWindow extends OwoWindow<FlowLayout> {
    private final LavenderBookScreen screen;

    public LavenderBookWindow(Book book) {
        super(640, 480, book.id().toString(), MinecraftClient.getInstance().getWindow().getHandle());

        this.screen = new LavenderBookScreen(book, false);

        try (var ignored = CurrentWindowContext.setCurrent(this)) {
            this.screen.init(client, scaledWidth(), scaledHeight());
        }

        framebufferResized().subscribe((newWidth, newHeight) -> {
            this.screen.init(client, scaledWidth(), scaledHeight());
        });
    }

    @Override
    protected OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
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
        }.sizing(Sizing.fill(100)));
    }
}
