package com.hexagram2021.initial_house;

import com.hexagram2021.initial_house.server.IHContent;
import com.hexagram2021.initial_house.server.IHSavedData;
import com.hexagram2021.initial_house.server.config.IHServerConfig;
import com.hexagram2021.initial_house.server.util.IHLogger;
import com.hexagram2021.initial_house.server.util.ModCommon;
import com.hexagram2021.initial_house.server.util.SCEutil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.util.Objects;

import static net.minecraft.world.level.Level.OVERWORLD;

@Mod(InitialHouse.MODID)
public class InitialHouse {
	public static final String MODID = "initial_house";

	public InitialHouse() {
		//ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, IHServerConfig.getConfig());
		IHServerConfig.loadConfig(IHServerConfig.SPEC, FMLPaths.CONFIGDIR.get().resolve(MODID + "default.toml").toString());
		ModCommon.init();
		IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
		IHContent.modConstruct(bus);

		//MinecraftForge.EVENT_BUS.addListener(this::onPlayerRespawn);
		MinecraftForge.EVENT_BUS.addListener(this::onEntityJoin);
		//MinecraftForge.EVENT_BUS.addListener(this::onOverworldLoad);
		MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
		//MinecraftForge.EVENT_BUS.addListener(this::onServerClose);
		MinecraftForge.EVENT_BUS.register(this);
	}

	/*private static void teleportPlayerToSpawnPoint(ServerPlayer serverPlayer) {
		BlockPos sharedSpawnPos = serverPlayer.level().getSharedSpawnPos();
		serverPlayer.teleportTo(
				sharedSpawnPos.getX() + IHServerConfig.SPAWN_POINT_SHIFT_X.get() + 0.5D,
				sharedSpawnPos.getY() + IHServerConfig.SPAWN_POINT_SHIFT_Y.get(),
				sharedSpawnPos.getZ() + IHServerConfig.SPAWN_POINT_SHIFT_Z.get() + 0.5D
		);
	}*/

	/*private void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		Player player = event.getEntity();
		if(!player.level().isClientSide && player instanceof ServerPlayer serverPlayer && IHServerConfig.DISABLE_SPAWN_POINT_RANDOM_SHIFTING.get()) {
			ServerLevel serverLevel = Objects.requireNonNull(serverPlayer.level().getServer()).getLevel(serverPlayer.getRespawnDimension());
			if (serverPlayer.getRespawnPosition() == null || serverLevel == null || !hasRespawnPosition(serverLevel, serverPlayer.getRespawnPosition())) {
				teleportPlayerToSpawnPoint(serverPlayer);
			}
		}
	}*/

	private void onEntityJoin(EntityJoinLevelEvent e) {
		//if(e.getLevel() instanceof ServerLevel world && world.dimension().toString().equals("heaven:sky")){
			if(!e.getLevel().isClientSide && e.getEntity() instanceof ServerPlayer serverPlayer && !IHSavedData.containsPlayer(serverPlayer.getUUID())) {
				IHSavedData.addPlayer(serverPlayer.getUUID());
				SCEutil.generateSchematic((ServerLevel) e.getLevel());
				//teleportPlayerToSpawnPoint(serverPlayer);
			}
		//}
	}

	/*public void onOverworldLoad(LevelEvent.Load event) {
		if(event.getLevel() instanceof ServerLevel world && world.dimension().toString().equals("heaven:sky")) {
			BlockPos spawnPoint = world.getChunkSource().randomState().sampler().findSpawnPosition();
			IHLogger.debug("Spawn Point is (%d, %d, %d).".formatted(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ()));
			//SpawnPointOnlyPlacement.setCache(new ChunkPos(spawnPoint));
		}
	}*/

	//public static final ResourceKey<Level> HEAVEN = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("heaven", "sky"));
	public void onServerStarted(ServerStartedEvent event) {
		ServerLevel world = event.getServer().getLevel(OVERWORLD);
		assert world != null;
		if (!world.isClientSide) {
			IHSavedData worldData = world.getDataStorage().computeIfAbsent(IHSavedData::new, IHSavedData::new, IHSavedData.SAVED_DATA_NAME);
			IHSavedData.setInstance(worldData);
		}
	}

	/*private void onServerClose(ServerStoppedEvent event) {
		SpawnPointOnlyPlacement.clearCache();
	}*/

	private static boolean hasRespawnPosition(ServerLevel serverLevel, BlockPos blockPos) {
		BlockState blockstate = serverLevel.getBlockState(blockPos);
		Block block = blockstate.getBlock();
		if (block instanceof RespawnAnchorBlock && blockstate.getValue(RespawnAnchorBlock.CHARGE) > 0 && RespawnAnchorBlock.canSetSpawn(serverLevel)) {
			return RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, serverLevel, blockPos).isPresent();
		}
		if (block instanceof BedBlock && BedBlock.canSetSpawn(serverLevel)) {
			return BedBlock.findStandUpPosition(EntityType.PLAYER, serverLevel, blockPos, blockstate.getValue(BedBlock.FACING), 1.0F).isPresent();
		}
		return blockstate.getRespawnPosition(EntityType.PLAYER, serverLevel, blockPos, 1.0F, null).isPresent();
	}
}
