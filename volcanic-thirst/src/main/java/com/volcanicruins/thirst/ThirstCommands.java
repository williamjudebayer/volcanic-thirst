package com.volcanicruins.thirst;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;

/**
 * Operator-only (permission level 2, same as /gamemode) commands.
 *
 *   /thirst set [players] <0-100>
 *   /thirst add [players] <-100..100>
 *   /thirst get [player]
 *   /thirst drainrate                                   show current rates
 *   /thirst drainrate <minutes>                         full meter lasts <minutes> (default for all biomes)
 *   /thirst drainrate reset                             default back to 30 minutes
 *   /thirst drainrate biome <biome> <minutes>           rate for one biome
 *   /thirst drainrate biome <biome> default             remove that biome's rate (use the default again)
 *   /thirst biome                                       show the biome you're standing in and its rate
 *
 * Shortcuts kept from before: /setthirst, /addthirst, /getthirst.
 * Minutes can be decimals (e.g. 7.5). 0 means no drain.
 */
public final class ThirstCommands {
	private ThirstCommands() {}

	private static final double MAX_MINUTES = 10080; // one week

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("thirst")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(setNode("set"))
				.then(addNode("add"))
				.then(getNode("get"))
				.then(drainRateNode())
				.then(Commands.literal("biome").executes(ctx -> showHere(ctx.getSource()))));

		// Shortcuts
		dispatcher.register(setNode("setthirst").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)));
		dispatcher.register(addNode("addthirst").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)));
		dispatcher.register(getNode("getthirst").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)));
	}

	// ---------------------------------------------------------------------------------------------
	// set / add / get
	// ---------------------------------------------------------------------------------------------

	private static LiteralArgumentBuilder<CommandSourceStack> setNode(String name) {
		int max = ThirstConfig.MAX_THIRST;
		return Commands.literal(name)
				.then(Commands.argument("value", IntegerArgumentType.integer(0, max))
						.executes(ctx -> set(ctx.getSource(),
								List.of(ctx.getSource().getPlayerOrException()),
								IntegerArgumentType.getInteger(ctx, "value"))))
				.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("value", IntegerArgumentType.integer(0, max))
								.executes(ctx -> set(ctx.getSource(),
										EntityArgument.getPlayers(ctx, "targets"),
										IntegerArgumentType.getInteger(ctx, "value")))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> addNode(String name) {
		int max = ThirstConfig.MAX_THIRST;
		return Commands.literal(name)
				.then(Commands.argument("amount", IntegerArgumentType.integer(-max, max))
						.executes(ctx -> add(ctx.getSource(),
								List.of(ctx.getSource().getPlayerOrException()),
								IntegerArgumentType.getInteger(ctx, "amount"))))
				.then(Commands.argument("targets", EntityArgument.players())
						.then(Commands.argument("amount", IntegerArgumentType.integer(-max, max))
								.executes(ctx -> add(ctx.getSource(),
										EntityArgument.getPlayers(ctx, "targets"),
										IntegerArgumentType.getInteger(ctx, "amount")))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> getNode(String name) {
		return Commands.literal(name)
				.executes(ctx -> get(ctx.getSource(), ctx.getSource().getPlayerOrException()))
				.then(Commands.argument("target", EntityArgument.player())
						.executes(ctx -> get(ctx.getSource(), EntityArgument.getPlayer(ctx, "target"))));
	}

	private static int set(CommandSourceStack source, Collection<ServerPlayer> targets, int value) {
		for (ServerPlayer player : targets) {
			ThirstHandler.setThirst(player, value);
		}
		source.sendSuccess(() -> Component.literal("Set thirst to " + value + "% for " + describe(targets)), true);
		return targets.size();
	}

	private static int add(CommandSourceStack source, Collection<ServerPlayer> targets, int amount) {
		for (ServerPlayer player : targets) {
			ThirstHandler.addThirst(player, amount);
		}
		String verb = amount >= 0 ? "Added " + amount : "Removed " + (-amount);
		if (targets.size() == 1) {
			int now = ThirstHandler.getThirst(targets.iterator().next());
			source.sendSuccess(() -> Component.literal(verb + "% thirst for " + describe(targets) + " (now " + now + "%)"), true);
		} else {
			source.sendSuccess(() -> Component.literal(verb + "% thirst for " + describe(targets)), true);
		}
		return targets.size();
	}

	private static int get(CommandSourceStack source, ServerPlayer player) throws CommandSyntaxException {
		int thirst = ThirstHandler.getThirst(player);
		source.sendSuccess(() -> Component.literal(player.getName().getString() + "'s thirst: " + thirst + "%"), false);
		return thirst;
	}

	private static String describe(Collection<ServerPlayer> targets) {
		return targets.size() == 1
				? targets.iterator().next().getName().getString()
				: targets.size() + " players";
	}

	// ---------------------------------------------------------------------------------------------
	// drainrate
	// ---------------------------------------------------------------------------------------------

	private static LiteralArgumentBuilder<CommandSourceStack> drainRateNode() {
		return Commands.literal("drainrate")
				.executes(ctx -> showRates(ctx.getSource()))
				.then(Commands.literal("reset")
						.executes(ctx -> setDefault(ctx.getSource(), ThirstConfig.DEFAULT_FULL_METER_MINUTES)))
				.then(Commands.argument("minutes", DoubleArgumentType.doubleArg(0, MAX_MINUTES))
						.executes(ctx -> setDefault(ctx.getSource(), DoubleArgumentType.getDouble(ctx, "minutes"))))
				.then(Commands.literal("biome")
						.then(Commands.argument("biome", IdentifierArgument.id())
								.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
										biomeRegistry(ctx.getSource()).keySet().stream().map(Identifier::toString), builder))
								.then(Commands.literal("default")
										.executes(ctx -> clearBiome(ctx)))
								.then(Commands.argument("minutes", DoubleArgumentType.doubleArg(0, MAX_MINUTES))
										.executes(ctx -> setBiome(ctx, DoubleArgumentType.getDouble(ctx, "minutes"))))));
	}

	private static Registry<Biome> biomeRegistry(CommandSourceStack source) {
		return source.getServer().registryAccess().lookup(Registries.BIOME).orElseThrow();
	}

	private static int setDefault(CommandSourceStack source, double minutes) {
		ThirstSettings.setDefaultMinutes(minutes);
		source.sendSuccess(() -> Component.literal("Default thirst drain: " + rateText(minutes)), true);
		return 1;
	}

	private static int setBiome(CommandContext<CommandSourceStack> ctx, double minutes) {
		Identifier biome = IdentifierArgument.getId(ctx, "biome");
		if (!biomeRegistry(ctx.getSource()).keySet().contains(biome)) {
			ctx.getSource().sendFailure(Component.literal("Unknown biome: " + biome));
			return 0;
		}
		ThirstSettings.setBiomeMinutes(biome.toString(), minutes);
		ctx.getSource().sendSuccess(() -> Component.literal("Thirst drain in " + biome + ": " + rateText(minutes)), true);
		return 1;
	}

	private static int clearBiome(CommandContext<CommandSourceStack> ctx) {
		String biome = IdentifierArgument.getId(ctx, "biome").toString();
		if (!ThirstSettings.clearBiome(biome)) {
			ctx.getSource().sendFailure(Component.literal(biome + " has no custom drain rate"));
			return 0;
		}
		ctx.getSource().sendSuccess(() -> Component.literal(biome + " now uses the default drain ("
				+ rateText(ThirstSettings.getDefaultMinutes()) + ")"), true);
		return 1;
	}

	private static int showRates(CommandSourceStack source) {
		source.sendSuccess(() -> Component.literal("Default: " + rateText(ThirstSettings.getDefaultMinutes())), false);
		Map<String, Double> overrides = ThirstSettings.biomeOverrides();
		if (overrides.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No biome-specific rates set."), false);
		} else {
			overrides.forEach((biome, minutes) ->
					source.sendSuccess(() -> Component.literal("  " + biome + ": " + rateText(minutes)), false));
		}
		return overrides.size();
	}

	private static int showHere(CommandSourceStack source) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		String biome = ThirstHandler.currentBiomeId(source.getLevel(), player);
		boolean custom = biome != null && ThirstSettings.biomeOverrides().containsKey(biome);
		double minutes = ThirstSettings.minutesFor(biome);
		source.sendSuccess(() -> Component.literal("You are in " + biome + " — "
				+ rateText(minutes) + (custom ? " (biome-specific)" : " (default)")), false);
		return 1;
	}

	/** "full meter lasts 30 min (1% every 18s)" or "no drain" */
	private static String rateText(double minutes) {
		if (minutes <= 0) {
			return "no drain";
		}
		double secondsPerPercent = minutes * 60.0 / ThirstConfig.MAX_THIRST;
		return "full meter lasts " + trim(minutes) + " min (1% every " + trim(secondsPerPercent) + "s)";
	}

	private static String trim(double value) {
		return value == Math.rint(value)
				? String.valueOf((long) value)
				: String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
	}
}
