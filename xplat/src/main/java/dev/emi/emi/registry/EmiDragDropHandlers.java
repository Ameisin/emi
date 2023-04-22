package dev.emi.emi.registry;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;

public class EmiDragDropHandlers {
	public static Map<Class<?>, List<EmiDragDropHandler<?>>> fromClass = Maps.newHashMap();
	public static List<EmiDragDropHandler<?>> generic = Lists.newArrayList();

	public static void clear() {
		fromClass.clear();
		generic.clear();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static void render(Screen screen, EmiIngredient stack, MatrixStack matrices, int mouseX, int mouseY, float delta) {
		if (fromClass.containsKey(screen.getClass())) {
			for (EmiDragDropHandler handler : fromClass.get(screen.getClass())) {
				handler.render(screen, stack, matrices, mouseX, mouseY, delta);
			}
		}
		for (EmiDragDropHandler handler : generic) {
			handler.render(screen, stack, matrices, mouseX, mouseY, delta);
		}
	}
	
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static boolean dropStack(Screen screen, EmiIngredient stack, int x, int y) {
		if (fromClass.containsKey(screen.getClass())) {
			for (EmiDragDropHandler handler : fromClass.get(screen.getClass())) {
				if (handler.dropStack(screen, stack, x, y)) {
					return true;
				}
			}
		}
		for (EmiDragDropHandler handler : generic) {
			if (handler.dropStack(screen, stack, x, y)) {
				return true;
			}
		}
		return false;
	}
}
