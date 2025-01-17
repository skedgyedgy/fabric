/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.test.structure;

import java.util.Optional;
import java.util.Random;

import com.mojang.serialization.Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.block.Blocks;
import net.minecraft.class_6908;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.ShiftableStructurePiece;
import net.minecraft.structure.StructureGeneratorFactory;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.structure.StructurePiecesGenerator;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.pool.StructurePoolElement;
import net.minecraft.structure.processor.StructureProcessorLists;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;

import net.fabricmc.fabric.api.structure.v1.FabricStructureBuilder;
import net.fabricmc.fabric.api.structure.v1.StructurePoolAddCallback;

public class StructureTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(StructureTest.class);

	public static final StructureFeature<DefaultFeatureConfig> STRUCTURE = new TestStructureFeature(DefaultFeatureConfig.CODEC);
	public static final RegistryEntry<ConfiguredStructureFeature<?, ?>> CONFIGURED_STRUCTURE = BuiltinRegistries.add(
			BuiltinRegistries.CONFIGURED_STRUCTURE_FEATURE,
			new Identifier("fabric", "test_structure"),
			STRUCTURE.configure(new DefaultFeatureConfig(), class_6908.DESERT_PYRAMID_HAS_STRUCTURE)
	);
	public static final StructurePieceType.Simple PIECE = TestStructureGenerator::new;

	static {
		LOGGER.info("Registering test structure");
		FabricStructureBuilder.create(new Identifier("fabric", "test_structure"), STRUCTURE)
				.step(GenerationStep.Feature.SURFACE_STRUCTURES)
				.defaultConfig(32, 8, 12345)
				.enableSuperflat()
				.adjustsSurface()
				.register();
		Registry.register(Registry.STRUCTURE_PIECE, new Identifier("fabric", "test_structure_piece"), PIECE);

		//Basic Test of Callback
		StructurePoolAddCallback.EVENT.register(structurePool -> LOGGER.info("Structure pool {} added", structurePool.getId()));

		//The ideal usage of this callback is to add structures to a Village. Here, I constructed a Cactus Farm, which will be added to the house pool for deserts. For testing purposes, we will make it very common, and use a plains-style log outline so it is clear that it doesn't belong.
		StructurePoolAddCallback.EVENT.register(structurePool -> {
			if (structurePool.getId().equals(new Identifier("minecraft:village/desert/houses"))) {
				structurePool.addStructurePoolElement(StructurePoolElement.ofProcessedLegacySingle("fabric:cactus_farm", StructureProcessorLists.FARM_PLAINS).apply(StructurePool.Projection.RIGID));
			}
		});
	}

	public static class TestStructureFeature extends StructureFeature<DefaultFeatureConfig> {
		public TestStructureFeature(Codec<DefaultFeatureConfig> codec) {
			super(codec, TestStructureFeature::createGenerator);
		}

		private static Optional<StructurePiecesGenerator<DefaultFeatureConfig>> createGenerator(StructureGeneratorFactory.Context<DefaultFeatureConfig> context) {
			return Optional.of((structurePiecesCollector, ctx) -> {
				int blockX = ctx.chunkPos().getStartX();
				int blockZ = ctx.chunkPos().getStartZ();
				int blockY = ctx.chunkGenerator().getHeight(blockX, blockZ, Heightmap.Type.WORLD_SURFACE_WG, ctx.world());

				TestStructureGenerator generator = new TestStructureGenerator(ctx.random(), blockX, blockY, blockZ);
				structurePiecesCollector.addPiece(generator);
			});
		}
	}

	public static class TestStructureGenerator extends ShiftableStructurePiece {
		public TestStructureGenerator(Random random, int x, int y, int z) {
			super(PIECE, x, y, z, 0, 48, 16, getRandomHorizontalDirection(random));
		}

		public TestStructureGenerator(NbtCompound nbtCompound) {
			super(PIECE, nbtCompound);
		}

		@Override
		public void generate(StructureWorldAccess structureWorldAccess, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox boundingBox, ChunkPos chunkPos, BlockPos blockPos) {
			for (int x = 0; x < 48; x++) {
				for (int z = 0; z < 48; z++) {
					for (int y = 0; y < 16; y++) {
						this.addBlock(structureWorldAccess, Blocks.DIAMOND_BLOCK.getDefaultState(), x, y, z, boundingBox);
					}
				}
			}
		}
	}
}
