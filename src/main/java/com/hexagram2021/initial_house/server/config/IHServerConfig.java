package com.hexagram2021.initial_house.server.config;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.io.File;
import java.util.List;

import static com.hexagram2021.initial_house.InitialHouse.MODID;

public class IHServerConfig {
	public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	public static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.ConfigValue<Boolean> shouldUseStructurePosition;
	public static final ForgeConfigSpec.ConfigValue<Integer> generatedStructureXPosition;
	public static final ForgeConfigSpec.ConfigValue<Integer> generatedStructureYPosition;
	public static final ForgeConfigSpec.ConfigValue<Integer> generatedStructureZPosition;


	static {
		BUILDER.push("initial_house-server-config");
		shouldUseStructurePosition = BUILDER.comment("should generate structure")
					.define("shouldUseStructurePosition", true);
		generatedStructureXPosition = BUILDER.comment("X-pivot of the initial house. Recommend: x-size / 2.")
					.defineInRange("generatedStructureXPosition", 0, -10000, 10000);
		generatedStructureYPosition = BUILDER.comment("Y-pivot of the initial house, depends on the thickness of the floor.")
					.defineInRange("generatedStructureYPosition", 50, 0, 255);
		generatedStructureZPosition = BUILDER.comment("Z-pivot of the initial house. Recommend: z-size / 2")
					.defineInRange("generatedStructureZPosition", 0, -10000, 10000);

		BUILDER.pop();
		SPEC = BUILDER.build();
	}

	public static void loadConfig(ForgeConfigSpec config, String path) {
		final CommentedFileConfig file = CommentedFileConfig.builder(new File(path))
				.sync()
				.autosave()
				.writingMode(WritingMode.REPLACE)
				.build();
		file.load();
		config.setConfig(file);
	}
}
