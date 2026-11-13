package com.volcanicruins.thirst;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Drain rates that can be changed in-game with /thirst drainrate.
 * Saved to config/volcanic_thirst.json so they survive server restarts.
 *
 * Rates are expressed as "minutes for a full meter to empty". 0 = no drain.
 */
public final class ThirstSettings {
	private ThirstSettings() {}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("volcanic_thirst.json");

	private static double defaultMinutes = ThirstConfig.DEFAULT_FULL_METER_MINUTES;
	/** Biome id (e.g. "minecraft:desert") -> minutes for a full meter. */
	private static final Map<String, Double> BIOME_MINUTES = new TreeMap<>();

	/** Shape of the JSON file. */
	private static final class Data {
		double defaultFullMeterMinutes = ThirstConfig.DEFAULT_FULL_METER_MINUTES;
		Map<String, Double> biomeFullMeterMinutes = new TreeMap<>();
	}

	public static void load() {
		if (!Files.exists(FILE)) {
			save(); // write a starter file so admins can see the format
			return;
		}
		try (Reader reader = Files.newBufferedReader(FILE)) {
			Data data = GSON.fromJson(reader, Data.class);
			if (data != null) {
				defaultMinutes = Math.max(0, data.defaultFullMeterMinutes);
				BIOME_MINUTES.clear();
				if (data.biomeFullMeterMinutes != null) {
					data.biomeFullMeterMinutes.forEach((biome, minutes) -> {
						if (biome != null && minutes != null) {
							BIOME_MINUTES.put(biome, Math.max(0, minutes));
						}
					});
				}
			}
			VolcanicThirst.LOGGER.info("Loaded thirst drain rates from {}", FILE);
		} catch (Exception e) {
			VolcanicThirst.LOGGER.error("Could not read {}, using defaults", FILE, e);
		}
	}

	public static void save() {
		Data data = new Data();
		data.defaultFullMeterMinutes = defaultMinutes;
		data.biomeFullMeterMinutes = new TreeMap<>(BIOME_MINUTES);
		try {
			Files.createDirectories(FILE.getParent());
			try (Writer writer = Files.newBufferedWriter(FILE)) {
				GSON.toJson(data, writer);
			}
		} catch (IOException e) {
			VolcanicThirst.LOGGER.error("Could not save {}", FILE, e);
		}
	}

	/** Minutes for a full meter in the given biome (falls back to the default if not overridden). */
	public static double minutesFor(String biomeId) {
		if (biomeId != null) {
			Double override = BIOME_MINUTES.get(biomeId);
			if (override != null) {
				return override;
			}
		}
		return defaultMinutes;
	}

	public static double getDefaultMinutes() {
		return defaultMinutes;
	}

	public static void setDefaultMinutes(double minutes) {
		defaultMinutes = Math.max(0, minutes);
		save();
	}

	public static void setBiomeMinutes(String biomeId, double minutes) {
		BIOME_MINUTES.put(biomeId, Math.max(0, minutes));
		save();
	}

	/** @return true if the biome had an override */
	public static boolean clearBiome(String biomeId) {
		boolean removed = BIOME_MINUTES.remove(biomeId) != null;
		if (removed) {
			save();
		}
		return removed;
	}

	public static Map<String, Double> biomeOverrides() {
		return Collections.unmodifiableMap(BIOME_MINUTES);
	}
}
