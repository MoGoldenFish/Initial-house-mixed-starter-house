package com.hexagram2021.initial_house.server.util;

import com.hexagram2021.initial_house.InitialHouse;
import com.hexagram2021.initial_house.server.config.IHServerConfig;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
public class SCEutil {
    public static String getConfigDirectory() {
        return DataFunctions.getGameDirectory() + File.separator + "config";
    }
    private static final String dirpath = getConfigDirectory() + File.separator + InitialHouse.MODID;
    private static final File schematicDir = new File(dirpath + File.separator + "schematics");
    private static final File signDataDir = new File(dirpath + File.separator + "signdata");
    private static final Logger logger = LogUtils.getLogger();

    public static boolean initDirs() {
        if (!schematicDir.isDirectory()) {
            if (!schematicDir.mkdirs()) {
                return false;
            }
        }
        return true;
    }
    public static BlockPos generateSchematic(ServerLevel serverLevel) {
        if (!schematicDir.isDirectory()) {
            if (!initDirs()) {
                return null;
            }
        }

        List<File> listOfSchematicFiles = new ArrayList<File>();

        File[] listOfFiles = schematicDir.listFiles();
        for (File file : listOfFiles) {
            if (file.getName().endsWith(".schem") || file.getName().endsWith(".schematic") || file.getName().endsWith(".nbt")) {
                listOfSchematicFiles.add(file);
            }
        }

        if (listOfSchematicFiles.size() == 0) {
            return null;
        }

        File schematicFile = listOfSchematicFiles.get(GlobalVariables.random.nextInt(listOfSchematicFiles.size()));
        if (!schematicFile.isFile()) {
            logger.info("Unable to find starter structure file.");
            return null;
        }

        boolean automaticCenter = schematicFile.getName().endsWith(".nbt");

        BlockPos structurePos = serverLevel.getSharedSpawnPos();

        if (IHServerConfig.shouldUseStructurePosition.get()) {
            structurePos = new BlockPos(IHServerConfig.generatedStructureXPosition.get(), IHServerConfig.generatedStructureYPosition.get(), IHServerConfig.generatedStructureZPosition.get());
        }

        /*if (ConfigHandler.ignoreTreesDuringStructurePlacement && (!ConfigHandler.shouldUseStructurePosition || ConfigHandler.generatedStructureYPosition == 0)) {
            structurePos = getSpawnPos(serverLevel, BlockPosFunctions.getSurfaceBlockPos(serverLevel, structurePos.getX(), structurePos.getZ(), true), false, false);
        }*/

        /*if (ConfigHandler.shouldUseStructureOffset) {
            structurePos = structurePos.offset(ConfigHandler.generatedStructureXOffset, ConfigHandler.generatedStructureYOffset, ConfigHandler.generatedStructureZOffset).immutable();
        }*/


        ParsedSchematicObject parsedSchematicObject;
        try (FileInputStream fileInputStream = new FileInputStream(schematicFile)){
            parsedSchematicObject = ParseSchematicFile.getParsedSchematicObject(fileInputStream, serverLevel, structurePos, 0, false, automaticCenter);
        }
        catch (Exception ex) {
            logger.info("Exception while attempting to parse schematic file.");
            ex.printStackTrace();
            return null;
        }

        if (!parsedSchematicObject.parsedCorrectly) {
            logger.info("The starter structure object was not parsed correctly.");
            return null;
        }

        BlockPos finalStructurePos = structurePos;
        MinecraftServer minecraftServer = serverLevel.getServer();

        minecraftServer.execute(() -> {




            for (Pair<BlockPos, BlockState> blockPair : parsedSchematicObject.blocks) {
                BlockState blockState = blockPair.getSecond();
                Block block = blockState.getBlock();
                if (block instanceof JigsawBlock || block instanceof StructureBlock || block instanceof StructureVoidBlock) {

                        continue;

                }

                serverLevel.setBlock(blockPair.getFirst(), blockState, 3);


            }


            minecraftServer.execute(() -> {
                parsedSchematicObject.placeBlockEntitiesInWorld(serverLevel);

                Registry<EntityType<?>> entityTypeRegistry = serverLevel.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
                for (Pair<BlockPos, BlockEntity> blockEntityPair : parsedSchematicObject.getBlockEntities(serverLevel)) {
                    BlockPos blockPos = blockEntityPair.getFirst();
                    BlockEntity blockEntity = blockEntityPair.getSecond();
                    if (blockEntity instanceof SignBlockEntity) {
                        SignBlockEntity signBlockEntity = (SignBlockEntity)blockEntity;
                        List<String> signLines = SignFunctions.getSignText(signBlockEntity);

                        String firstLine = signLines.get(0);
                        signLines.remove(0);
                        String signContent = String.join("", signLines);

                        Entity newEntity = null;
                        if (firstLine.contains("[Mob]") || firstLine.contains("[Entity]")) {
                            EntityType<?> entityType = entityTypeRegistry.get(new ResourceLocation(signContent));
                            if (entityType != null) {
                                newEntity = entityType.create(serverLevel);
                            }
                        }
                        else if (firstLine.contains("[NBT]")) {
                            String nbtFilePath = signDataDir + File.separator + signContent + ".txt";

                            File nbtTextFile = new File(nbtFilePath);
                            if (nbtTextFile.isFile()) {
                                int n = 1;
                                while (n >= 0) {
                                    String rawNBT = "";
                                    try {
                                        rawNBT = new String(Files.readAllBytes(Paths.get(nbtFilePath)));

                                        CompoundTag entityCompoundTag = TagParser.parseTag(rawNBT);
                                        Optional<Entity> optionalNewEntity = EntityType.create(entityCompoundTag, serverLevel);
                                        if (optionalNewEntity.isPresent()) {
                                            if (n != 1) {
                                                //logger.info(logPrefix + "Unable to parse the " + signContent + ".txt entitydata file. Attempting automatic fix.");
                                            }

                                            newEntity = optionalNewEntity.get();
                                            n = -1;
                                        }
                                    } catch (Exception ex) {
                                        //logger.info(logPrefix + "Unable to parse the " + signContent + ".txt entitydata file. Attempting automatic fix.");
                                        try {
                                            attemptEntityDataFileFix(nbtFilePath, rawNBT);
                                        }
                                        catch (IOException ignored) { }
                                    }

                                    n-=1;
                                }
                            }
                        }
                        else {
                            continue;
                        }

                        if (newEntity != null) {
                            newEntity.getTags().add(InitialHouse.MODID + ".protected");
                            newEntity.setPos(blockPos.getX()+0.5, blockPos.getY(), blockPos.getZ()+0.5);
                            serverLevel.addFreshEntity(newEntity);
                            serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }



                minecraftServer.execute(() -> {
                    float spawnAngle = serverLevel.getSharedSpawnAngle();

                    if (!isSpawnablePos(serverLevel, finalStructurePos)) {
                        List<Integer> absoluteArray = Arrays.asList(-1, 1);

                        for (int i = 0; i <= 10; i++) {
                            for (BlockPos aroundPos : BlockPos.betweenClosed(finalStructurePos.getX()-i, finalStructurePos.getY()-i, finalStructurePos.getZ()-i, finalStructurePos.getX()+i, finalStructurePos.getY()+i, finalStructurePos.getZ()+i)) {
                                BlockPos upPos = aroundPos.above();
                                if (isSpawnablePos(serverLevel, aroundPos) && isSpawnablePos(serverLevel, upPos)) {
                                    serverLevel.setDefaultSpawnPos(aroundPos, spawnAngle);
                                    return;
                                }
                            }
                        }
                    }
                });
            });

        });

        return structurePos;
    }
    private static boolean isSpawnablePos(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock().equals(Blocks.AIR);
    }
    private static void attemptEntityDataFileFix(String nbtFilePath, String rawNBT) throws IOException {
        String[] rawSplit = rawNBT.split("\\{", 2);
        if (rawSplit.length > 1) {
            String prefix = rawSplit[0];
            String newRawNBT = rawSplit[1];

            String idValue = "";
            if (prefix.contains(":")) {
                String[] prefixSplit = prefix.split(" ");
                for (String word : prefixSplit) {
                    if (word.contains(":")) {
                        idValue = "id:\"" + word + "\",";
                    }
                }
            }

            if (!idValue.equals("")) {
                newRawNBT = "{" + idValue + newRawNBT;
                Files.write(Path.of(nbtFilePath), newRawNBT.getBytes());
            }
        }
    }
}
