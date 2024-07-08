package io.wispforest.lavender;

import com.mojang.logging.LogUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.lavender.book.LavenderBookItem;
import io.wispforest.lavender.client.LavenderClient;
import io.wispforest.owo.extras.ServerPlayConnectionEvents;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.GameData;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

import java.util.UUID;

@Mod(Lavender.MOD_ID)
public class Lavender {

    public static final Logger LOGGER = LogUtils.getLogger();
    public static final String MOD_ID = "lavender";
    public static final SoundEvent ITEM_BOOK_OPEN = SoundEvent.of(id("item.book.open"));

    public static final Identifier WORLD_ID_CHANNEL = Lavender.id("world_id_channel");


    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(MOD_ID);

    public Lavender(IEventBus modBus) {
        ITEMS.register(modBus);
        DATA_COMPONENTS.register(modBus);

        // trigger clinit.
        // TODO: make this better
        var unused = LavenderBookItem.BOOK_ID;

        modBus.addListener(RegisterEvent.class, event -> {
            event.register(RegistryKeys.SOUND_EVENT, ITEM_BOOK_OPEN.getId(), () -> ITEM_BOOK_OPEN);
        });

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, event -> {
            LavenderCommands.register(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
        });

        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            event.registrar("1")
                .optional()
                .playToClient(WorldUUIDPayload.ID, CodecUtils.toPacketCodec(WorldUUIDPayload.ENDEC), LavenderClient::handleCurrentWorldPacket);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            sender.accept(new WorldUUIDPayload(server.getOverworld().getPersistentStateManager().getOrCreate(WorldUUIDState.TYPE, "lavender_world_id").id));
        });
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static class WorldUUIDState extends PersistentState {

        public static final PersistentState.Type<WorldUUIDState> TYPE = new Type<>(() -> {
            var state = new WorldUUIDState(UUID.randomUUID());
            state.markDirty();
            return state;
        }, WorldUUIDState::read, DataFixTypes.LEVEL);

        public final UUID id;

        private WorldUUIDState(UUID id) {
            this.id = id;
        }

        @Override
        public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
            nbt.putUuid("UUID", id);
            return nbt;
        }

        public static WorldUUIDState read(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
            return new WorldUUIDState(nbt.contains("UUID", NbtElement.INT_ARRAY_TYPE) ? nbt.getUuid("UUID") : null);
        }
    }

    public record WorldUUIDPayload(UUID worldUuid) implements CustomPayload {
        public static final CustomPayload.Id<WorldUUIDPayload> ID = new CustomPayload.Id<>(Lavender.id("world_uuid"));
        public static final Endec<WorldUUIDPayload> ENDEC = StructEndecBuilder.of(
                BuiltInEndecs.UUID.fieldOf("world_uuid", WorldUUIDPayload::worldUuid),
                WorldUUIDPayload::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
