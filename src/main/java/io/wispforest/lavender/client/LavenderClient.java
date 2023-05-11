package io.wispforest.lavender.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.wispforest.lavender.book.BookLoader;
import io.wispforest.lavender.book.BookContentLoader;
import io.wispforest.lavender.md.MarkdownProcessor;
import io.wispforest.lavender.structure.LavenderStructures;
import io.wispforest.owo.ui.core.Size;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class LavenderClient implements ClientModInitializer {

    private static final Int2ObjectMap<Size> TEXTURE_SIZES = new Int2ObjectOpenHashMap<>();

    private static final SimpleCommandExceptionType NO_SUCH_STRUCTURE = new SimpleCommandExceptionType(Text.literal("No such structure is loaded"));
    private static final SuggestionProvider<FabricClientCommandSource> STRUCTURE_INFO = (context, builder) ->
            CommandSource.suggestMatching(LavenderStructures.loadedStructures().stream().map(Identifier::toString), builder);

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("parse-md").then(argument("md", StringArgumentType.greedyString()).executes(context -> {
                context.getSource().sendFeedback(MarkdownProcessor.TEXT.process(StringArgumentType.getString(context, "md")));
                return 0;
            })));

            dispatcher.register(literal("edit-md").executes(context -> {
                MinecraftClient.getInstance().setScreen(new EditMdScreen());
                return 0;
            }));

            dispatcher.register(literal("structure-overlay")
                    .then(literal("clear-all").executes(context -> {
                        StructureOverlayRenderer.clearOverlays();
                        return 0;
                    }))
                    .then(literal("add")
                            .then(argument("structure", IdentifierArgumentType.identifier()).suggests(STRUCTURE_INFO).executes(context -> {
                                var structureId = context.getArgument("structure", Identifier.class);
                                if (LavenderStructures.get(structureId) == null) throw NO_SUCH_STRUCTURE.create();

                                StructureOverlayRenderer.addPendingOverlay(structureId);
                                return 0;
                            }))));
        });

        StructureOverlayRenderer.initialize();
        LavenderStructures.initialize();
        BookLoader.initialize();
        BookContentLoader.initialize();
    }

    public static void registerTextureSize(int textureId, int width, int height) {
        TEXTURE_SIZES.put(textureId, Size.of(width, height));
    }

    public static @Nullable Size getTextureSize(Identifier texture) {
        return TEXTURE_SIZES.get(MinecraftClient.getInstance().getTextureManager().getTexture(texture).getGlId());
    }
}
