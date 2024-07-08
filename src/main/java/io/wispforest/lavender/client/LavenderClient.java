package io.wispforest.lavender.client;

import io.wispforest.lavender.Lavender;
import io.wispforest.lavender.LavenderCommands;
import io.wispforest.lavender.book.Book;
import io.wispforest.lavender.book.BookContentLoader;
import io.wispforest.lavender.book.BookLoader;
import io.wispforest.lavender.book.LavenderBookItem;
import io.wispforest.lavender.md.ItemListComponent;
import io.wispforest.lavender.structure.LavenderStructures;
import io.wispforest.owo.extras.ClientPlayConnectionEvents;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.hud.Hud;
import io.wispforest.owo.ui.parsing.UIParsing;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Mod(value = Lavender.MOD_ID, dist = Dist.CLIENT)
public class LavenderClient {

    public static final BlitCutoutProgram BLIT_CUTOUT_PROGRAM = new BlitCutoutProgram();
    public static final BlitAlphaProgram BLIT_ALPHA_PROGRAM = new BlitAlphaProgram();

    private static final Int2ObjectMap<Size> TEXTURE_SIZES = new Int2ObjectOpenHashMap<>();
    private static final Identifier ENTRY_HUD_ID = Lavender.id("entry_hud");

    private static UUID currentWorldId = null;

    public LavenderClient(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class, event -> {
            LavenderCommands.Client.register(event.getDispatcher(), event.getBuildContext());
        });

        // TODO: figure out what the fuck.

//        ModelLoadingPlugin.register(pluginContext -> {
//            pluginContext.resolveModel().register(context -> {
//                if (!context.id().equals(Lavender.id("item/dynamic_book"))) return null;
//                return new BookBakedModel.Unbaked();
//            });
//        });

        StructureOverlayRenderer.initialize();
        OffhandBookRenderer.initialize();

        LavenderStructures.initialize(modBus);
        BookLoader.initialize(modBus);
        BookContentLoader.initialize(modBus);

        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingIn.class, event -> {
            BookLoader.reload(MinecraftClient.getInstance().getResourceManager());
            BookContentLoader.reloadContents(MinecraftClient.getInstance().getResourceManager());
        });

        Hud.add(ENTRY_HUD_ID, () -> Containers.horizontalFlow(Sizing.content(), Sizing.content()).gap(5).positioning(Positioning.across(50, 52)));
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.world == null || !(Hud.getComponent(ENTRY_HUD_ID) instanceof FlowLayout hudComponent)) return;

            hudComponent.<FlowLayout>configure(container -> {
                container.clearChildren();

                Book book = LavenderBookItem.bookOf(client.player.getMainHandStack());
                if (book == null) book = LavenderBookItem.bookOf(client.player.getOffHandStack());
                if (book == null) return;

                if (!(client.crosshairTarget instanceof BlockHitResult hitResult)) return;
                var item = client.world.getBlockState(hitResult.getBlockPos()).getBlock().asItem();
                if (item == Items.AIR) return;

                var associatedEntry = book.entryByAssociatedItem(item.getDefaultStack());
                if (associatedEntry == null || !associatedEntry.canPlayerView(client.player)) return;

                container.child(Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(associatedEntry.iconFactory().apply(Sizing.fixed(16)).margins(Insets.of(0, 1, 0, 1)))
                        .child(Components.item(LavenderBookItem.itemOf(book)).sizing(Sizing.fixed(8)).positioning(Positioning.absolute(9, 9)).zIndex(50)));
                container.child(Containers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(Components.label(Text.literal(associatedEntry.title())).shadow(true))
                        .child(Components.label(Text.translatable(client.player.isSneaking() ? "text.lavender.entry_hud.click_to_view" : "text.lavender.entry_hud.sneak_to_view"))));
            });
        });

        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, event -> {
            var stack = event.getEntity().getStackInHand(event.getHand());
            if (!event.getEntity().isSneaking()) return;

            var book = LavenderBookItem.bookOf(stack);
            if (book == null) return;

            var item = event.getLevel().getBlockState(event.getHitVec().getBlockPos()).getBlock().asItem();
            if (item == Items.AIR) return;

            var associatedEntry = book.entryByAssociatedItem(item.getDefaultStack());
            if (associatedEntry == null || !associatedEntry.canPlayerView((ClientPlayerEntity) event.getEntity())) {
                return;
            }

            LavenderBookScreen.pushEntry(book, associatedEntry);
            MinecraftClient.getInstance().setScreen(new LavenderBookScreen(book));

            event.getEntity().swingHand(event.getHand());
            event.setCanceled(true);
            event.setCancellationResult(ActionResult.FAIL);
        });

        UIParsing.registerFactory(Lavender.id("ingredient"), element -> {
            Lavender.LOGGER.warn("Deprecated <ingredient> element used, migrate to <item-list> instead");
            return new ItemListComponent();
        });

        UIParsing.registerFactory(Lavender.id("item-list"), element -> new ItemListComponent());
    }

    public static void handleCurrentWorldPacket(Lavender.WorldUUIDPayload payload, IPayloadContext ctx) {
        currentWorldId = payload.worldUuid();
    }

    public static UUID currentWorldId() {
        return currentWorldId;
    }

    public static void registerTextureSize(int textureId, int width, int height) {
        TEXTURE_SIZES.put(textureId, Size.of(width, height));
    }

    public static @Nullable Size getTextureSize(Identifier texture) {
        return TEXTURE_SIZES.get(MinecraftClient.getInstance().getTextureManager().getTexture(texture).getGlId());
    }
}
