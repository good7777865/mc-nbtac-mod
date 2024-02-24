package com.mt1006.nbt_ac.mixin.suggestions.selectors;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Either;
import com.mt1006.nbt_ac.autocomplete.NbtSuggestionManager;
import com.mt1006.nbt_ac.utils.RegistryUtils;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Mixin(ItemParser.class)
public class ItemParserMixin
{
	@Shadow @Final private StringReader reader;
	@Shadow private Either<Holder<Item>, HolderSet<Item>> result;
	@Shadow @Nullable private CompoundTag nbt;
	@Shadow private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions;

	@Inject(at = @At(value = "HEAD"), method = "readNbt", cancellable = true)
	protected void atReadNbt(CallbackInfo ci) throws CommandSyntaxException
	{
		ci.cancel();
		int cursorPos = reader.getCursor();

		try
		{
			nbt = (new TagParser(reader)).readStruct();
		}
		catch (CommandSyntaxException exception)
		{
			reader.setCursor(cursorPos);
			suggestions = this::suggestNbt;
			throw exception;
		}
	}

	@Unique private CompletableFuture<Suggestions> suggestNbt(SuggestionsBuilder suggestionsBuilder)
	{
		Holder<Item> itemHolder = result.left().orElse(null);
		if (itemHolder == null) { return Suggestions.empty(); }
		Item item = itemHolder.get();
		ResourceLocation resourceLocation = RegistryUtils.ITEM.getKey(item);

		String name = resourceLocation.toString();
		String tag = suggestionsBuilder.getRemaining();

		return NbtSuggestionManager.loadFromName("item/" + name, tag, suggestionsBuilder, false);
	}
}
