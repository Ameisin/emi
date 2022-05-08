package dev.emi.emi.screen;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mojang.blaze3d.systems.RenderSystem;

import org.apache.commons.compress.utils.Lists;
import org.lwjgl.glfw.GLFW;

import dev.emi.emi.EmiConfig;
import dev.emi.emi.EmiRecipeFiller;
import dev.emi.emi.EmiRecipes;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.bom.MaterialNode;
import dev.emi.emi.mixin.accessor.ScreenAccessor;
import dev.emi.emi.screen.widget.ArrowButtonWidget;
import dev.emi.emi.widget.RecipeBackground;
import dev.emi.emi.widget.RecipeDefaultButtonWidget;
import dev.emi.emi.widget.RecipeFillButtonWidget;
import dev.emi.emi.widget.RecipeTreeButtonWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class RecipeScreen extends Screen implements EmiScreen {
	private static final Identifier TEXTURE = new Identifier("emi", "textures/gui/background.png");
	private static final int RECIPE_PADDING = 9;
	public static MaterialNode resolve = null;
	private Map<EmiRecipeCategory, List<EmiRecipe>> recipes;
	public HandledScreen<?> old;
	private List<RecipeTab> tabs = Lists.newArrayList();
	private int tabPageSize = 6;
	private int tabPage = 0, tab = 0, page = 0;
	private List<ArrowButtonWidget> arrows;
	private List<WidgetGroup> currentPage = Lists.newArrayList();
	private int tabOff = 0;
	int backgroundWidth = 176;
	int backgroundHeight = 200;
	int x = (this.width - backgroundWidth) / 2;
	int y = (this.height - backgroundHeight) / 2;

	public RecipeScreen(HandledScreen<?> old) {
		super(new TranslatableText("screen.emi.recipe"));
		this.old = old;
		arrows = List.of(
			new ArrowButtonWidget(x + 2, y - 18, 12, 12, 0, 64,
				() -> tabs.size() > tabPageSize, w -> setPage(tabPage - 1, tab, page)),
			new ArrowButtonWidget(x + backgroundWidth - 14, y - 18, 12, 12, 12, 64,
				() -> tabs.size() > tabPageSize, w -> setPage(tabPage + 1, tab, page)),
			new ArrowButtonWidget(x + 5, y + 5, 12, 12, 0, 64,
				() -> tabs.size() > 1, w -> setPage(tabPage, tab - 1, page)),
			new ArrowButtonWidget(x + backgroundWidth - 17, y + 5, 12, 12, 12, 64,
				() -> tabs.size() > 1, w -> setPage(tabPage, tab + 1, page)),
			new ArrowButtonWidget(x + 5, y + 18, 12, 12, 0, 64,
				() -> tabs.get(tab).recipes.size() > 1, w -> setPage(tabPage, tab, page - 1)),
			new ArrowButtonWidget(x + backgroundWidth - 17, y + 18, 12, 12, 12, 64,
				() -> tabs.get(tab).recipes.size() > 1, w -> setPage(tabPage, tab, page + 1))
		);
	}

	@Override
	protected void init() {
		super.init();
		this.client.keyboard.setRepeatEvents(true);
		backgroundHeight = height - 52 - EmiConfig.verticalPadding;
		x = (this.width - backgroundWidth) / 2;
		y = (this.height - backgroundHeight) / 2 + 1;
		
		for (ArrowButtonWidget widget : arrows) {
			addDrawableChild(widget);
		}
		EmiScreenManager.search.x = (this.width - 176) / 2 + (176 - EmiScreenManager.search.getWidth()) / 2;
		EmiScreenManager.search.y = height - 22;
		EmiScreenManager.search.setTextFieldFocused(false);
		addSelectableChild(EmiScreenManager.search);
		if (recipes != null) {
			setPages(recipes);
		}
		setPage(tabPage, tab, page);
		setRecipePageWidth(backgroundWidth);
	}

	private void setRecipePageWidth(int width) {
		this.backgroundWidth = width;
		this.x = (this.width - backgroundWidth) / 2;
		this.tabOff = (backgroundWidth - 176) / 2;
		this.arrows.get(0).x = this.x + 2;
		this.arrows.get(1).x = this.x + this.backgroundWidth - 14;
		this.arrows.get(2).x = this.x + 5;
		this.arrows.get(3).x = this.x + this.backgroundWidth - 17;
		this.arrows.get(4).x = this.x + 5;
		this.arrows.get(5).x = this.x + this.backgroundWidth - 17;

		this.arrows.get(0).y = this.y - 18;
		this.arrows.get(1).y = this.y - 18;
		this.arrows.get(2).y = this.y + 5;
		this.arrows.get(3).y = this.y + 5;
		this.arrows.get(4).y = this.y + 18;
		this.arrows.get(5).y = this.y + 18;
	}

	@Override
	public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		this.renderBackground(matrices);
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.setShaderTexture(0, TEXTURE);
		EmiRenderHelper.drawNinePatch(matrices, x, y, backgroundWidth, backgroundHeight, 0, 0, 4, 1);

		int tp = tabPage * tabPageSize;
		int off = 0;
		for (int i = tp; i < tabs.size() && i < tp + tabPageSize; i++) {
			RecipeTab tab = tabs.get(i);
			RenderSystem.setShaderTexture(0, TEXTURE);
			int sOff = (i == this.tab ? 2 : 0);
			EmiRenderHelper.drawNinePatch(matrices, x + tabOff + off * 24 + 16, y - 24 - sOff, 24, 27 + sOff,
				i == this.tab ? 9 : 18, 0, 4, 1);
			tab.category.render(matrices, x + tabOff + off++ * 24 + 20, y - 20 - (i == this.tab ? 2 : 0), delta);
		}
		fillGradient(matrices, x + 19, y + 5, x + backgroundWidth - 19, y + 5 + 12, 0xff999999, 0xff999999);
		fillGradient(matrices, x + 19, y + 18, x + backgroundWidth - 19, y + 18 + 12, 0xff999999, 0xff999999);

		RecipeTab tab = tabs.get(this.tab);
		drawCenteredText(matrices, textRenderer, new TranslatableText(EmiUtil.translateId("emi.category.", tab.category.getId())),
			x + backgroundWidth / 2, y + 7, 0xffffff);
		drawCenteredText(matrices, textRenderer, new TranslatableText("emi.page", this.page + 1, tab.recipes.size()),
			x + backgroundWidth / 2, y + 20, 0xffffff);

		List<EmiIngredient> workstations = EmiRecipes.workstations.getOrDefault(tab.category, List.of());
		if (!workstations.isEmpty()) {
			int size = Math.min(workstations.size(), (backgroundHeight - 30) / 18);
			RenderSystem.setShaderTexture(0, TEXTURE);
			EmiRenderHelper.drawNinePatch(matrices, x - 21, y + 7, 24, 6 + 18 * size, 27, 0, 3, 1);
		}
		for (WidgetGroup group : currentPage) {
			int mx = mouseX - group.x();
			int my = mouseY - group.y();
			MatrixStack view = RenderSystem.getModelViewStack();
			view.push();
			view.translate(group.x(), group.y(), 0);
			RenderSystem.applyModelViewMatrix();
			for (Widget widget : group.widgets) {
				widget.render(matrices, mx, my, delta);
			}
			view.pop();
			RenderSystem.applyModelViewMatrix();
		}
		for (WidgetGroup group : currentPage) {
			int mx = mouseX - group.x();
			int my = mouseY - group.y();
			for (Widget widget : group.widgets) {
				if (widget.getBounds().contains(mx, my)) {
					((ScreenAccessor) this).invokeRenderTooltipFromComponents(matrices, widget.getTooltip(), mouseX, Math.max(16, mouseY));
				}
			}
		}

		super.render(matrices, mouseX, mouseY, delta);
		EmiScreenManager.render(matrices, mouseX, mouseY, delta);
		if (mouseX >= x + 16 + tabOff && mouseX < x + backgroundWidth && mouseY >= y - 24 && mouseY < y) {
			int n = (mouseX - x - 16 - tabOff) / 24 + tabPage * tabPageSize;
			if (n < tabs.size() && n >= tabPage * tabPageSize && n < (tabPage + 1) * tabPageSize) {
				RecipeTab t = tabs.get(n);
				List<Text> list = Lists.newArrayList();
				list.add(new TranslatableText(EmiUtil.translateId("emi.category.", t.category.getId())));
				list.add(new LiteralText(EmiUtil.getModName(t.category.getId().getNamespace())).formatted(Formatting.BLUE));
				this.renderTooltip(matrices, list, mouseX, Math.max(16, mouseY));
			}
		}
	}

	public void focusRecipe(EmiRecipe recipe) {
		for (int i = 0; i < tabs.size(); i++) {
			RecipeTab tab = tabs.get(i);
			for (int j = 0; j < tab.recipes.size(); j++) {
				for (EmiRecipe r : tab.recipes.get(j)) {
					if (r == recipe) {
						setPage(tabPage, i, j);
						return;
					}
				}
			}
		}
	}

	public void setPage(int tp, int t, int p) {
		currentPage.clear();
		if (tabs.isEmpty()) {
			return;
		}
		boolean snapTabPage = tp == tabPage && t != tab;
		tab = t;
		if (tab >= tabs.size()) {
			tab = 0;
		} else if (tab < 0) {
			tab = tabs.size() - 1;
		}
		if (snapTabPage) {
			tp = (tab) / tabPageSize;
		}
		tabPage = tp;
		if (tabPage >= (tabs.size() - 1) / tabPageSize + 1) {
			tabPage = (tabs.size() - 1) / tabPageSize;
		} else if (tabPage < 0) {
			tabPage = 0;
		}
		List<List<EmiRecipe>> recipes = tabs.get(tab).recipes;
		page = p;
		if (page >= recipes.size()) {
			page = 0;
		} else if (page < 0) {
			page = recipes.size() - 1;
		}
		if (page < recipes.size()) {
			int width = 160;
			for (List<EmiRecipe> list : recipes) {
				for (EmiRecipe r : list) {
					int w = r.getDisplayWidth();
					if (r.supportsRecipeTree() || !EmiRecipeFiller.RECIPE_HANDLERS.getOrDefault(r.getCategory(), Set.of()).isEmpty()) {
						w += 26;
					}
					width = Math.max(width, w);
				}
			}
			setRecipePageWidth(width + 16);
			int off = 0;
			for (EmiRecipe r : recipes.get(page)) {
				List<Widget> widgets = Lists.newArrayList();
				int xOff = (backgroundWidth - r.getDisplayWidth()) / 2;
				int wx = x + xOff;
				int wy = y + 36 + off;
				final int recipeHeight = Math.min(backgroundHeight - 52, r.getDisplayHeight());
				widgets.add(new RecipeBackground(-4, -4, r.getDisplayWidth() + 8, recipeHeight + 8));
				WidgetHolder holder = new WidgetHolder() {
		
					public int getWidth() {
						return r.getDisplayWidth();
					}
		
					public int getHeight() {
						return recipeHeight;
					}
		
					public <T extends Widget> T add(T widget) {
						widgets.add(widget);
						return widget;
					}
				};
				r.addWidgets(holder);
				int by = recipeHeight - 12;
				if (recipeHeight <= 18) {
					by += 4;
				}
				int button = 0;
				if (!EmiRecipeFiller.RECIPE_HANDLERS.getOrDefault(r.getCategory(), Set.of()).isEmpty()) {
					if (EmiConfig.recipeFillButton) {
						widgets.add(new RecipeFillButtonWidget(r.getDisplayWidth() + 5, by + 14 * button++, r));
					}
				}
				if (r.supportsRecipeTree()) {
					if (EmiConfig.recipeTreeButton) {
						widgets.add(new RecipeTreeButtonWidget(r.getDisplayWidth() + 5, by - 14 * button++, r));
					}
					if (EmiConfig.recipeDefaultButton) {
						widgets.add(new RecipeDefaultButtonWidget(r.getDisplayWidth() + 5, by - 14 * button++, r));
					}
				}
				off += recipeHeight + RECIPE_PADDING;
				currentPage.add(new WidgetGroup(widgets, wx, wy));
			}
			List<EmiIngredient> workstations = EmiRecipes.workstations.getOrDefault(tabs.get(tab).category, List.of());
			if (!workstations.isEmpty()) {
				List<Widget> widgets = Lists.newArrayList();
				for (int i = 0; i < workstations.size() && i < (backgroundHeight - 30) / 18; i++) {
					widgets.add(new SlotWidget(workstations.get(i), x - 18, y + 10 + i * 18));
				}
				currentPage.add(new WidgetGroup(widgets, 0, 0));
			}
		}
	}

	public void setPages(Map<EmiRecipeCategory, List<EmiRecipe>> recipes) {
		resolve = null;
		this.recipes = recipes;
		if (!recipes.isEmpty()) {
			EmiRecipeCategory current = null;
			if (tab < tabs.size()) {
				current = tabs.get(tab).category;
			}
			tabs.clear();
			for (Map.Entry<EmiRecipeCategory, List<EmiRecipe>> entry : recipes.entrySet().stream()
					.sorted((a, b) -> EmiRecipes.categories.indexOf(a.getKey()) - EmiRecipes.categories.indexOf(b.getKey())).toList()) {
				List<EmiRecipe> set = entry.getValue();
				if (!set.isEmpty()) {
					tabs.add(new RecipeTab(entry.getKey(), set));
				}
			}

			int newTab = 0;
			for (int i = 0; i < tabs.size(); i++) {
				if (tabs.get(i).category == current) {
					newTab = i;
					break;
				}
			}
			
			// Force tabPage adjustment
			tab = -1;
			setPage(tabPage, newTab, 0);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int mx = (int) mouseX;
		int my = (int) mouseY;
		for (WidgetGroup group : currentPage) {
			int ox = mx - group.x();
			int oy = my - group.y();
			for (Widget widget : group.widgets) {
				if (widget.getBounds().contains(ox, oy)) {
					if (widget.mouseClicked(ox, oy, button)) {
						return true;
					}
				}
			}
		}
		if (EmiScreenManager.mouseClicked(mouseX, mouseY, button)) {
			return true;
		} else if (mx >= x + 16 + tabOff && mx < x + backgroundWidth && my >= y - 24 && my < y) {
			int n = (mx - x - 16 - tabOff) / 24 + tabPage * tabPageSize;
			if (n < tabs.size() && n >= tabPage * tabPageSize && n < (tabPage + 1) * tabPageSize) {
				setPage(tabPage, n, 0);
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (EmiScreenManager.mouseScrolled(mouseX, mouseY, amount)) {
			return true;
		} else if (mouseX > x && mouseX < x + backgroundWidth && mouseY < x + backgroundHeight) {
			if (EmiUtil.isShiftDown()) {
				setPage(tabPage, (int) (tab - amount), page);
			} else {
				setPage(tabPage, tab, (int) (page - amount));
			}
		}
		return super.mouseScrolled(mouseX, mouseY, amount);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
			this.close();
			return true;
		} else if (EmiScreenManager.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		} else if (this.client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
			this.close();
			return true;
		}

		for (WidgetGroup group : currentPage) {
			int mx = EmiScreenManager.lastMouseX - group.x();
			int my = EmiScreenManager.lastMouseY - group.y();
			for (Widget widget : group.widgets) {
				if (widget.getBounds().contains(mx, my)) {
					if (widget.keyPressed(keyCode, scanCode, modifiers)) {
						return true;
					}
				}
			}
		}
		if (keyCode == GLFW.GLFW_KEY_LEFT) {
			setPage(tabPage - 1, tab, page);
		} else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
			setPage(tabPage + 1, tab, page);
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public void close() {
		this.client.setScreen(old);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public int emi$getLeft() {
		return x - 20;
	}

	@Override
	public int emi$getRight() {
		return x + backgroundWidth;
	}

	private class RecipeTab {
		private final EmiRecipeCategory category;
		private final List<List<EmiRecipe>> recipes;

		public RecipeTab(EmiRecipeCategory category, List<EmiRecipe> recipes) {
			this.category = category;
			this.recipes = getPages(recipes, backgroundHeight - 44);
		}

		private List<List<EmiRecipe>> getPages(List<EmiRecipe> recipes, int height) {
			List<List<EmiRecipe>> list = Lists.newArrayList();
			List<EmiRecipe> current = Lists.newArrayList();
			int h = 0;
			for (EmiRecipe recipe : recipes) {
				int rh = recipe.getDisplayHeight();
				if (!current.isEmpty() && h + rh > height) {
					list.add(current);
					current = Lists.newArrayList();
					h = 0;
				}
				h += rh + RECIPE_PADDING;
				current.add(recipe);
			}
			if (!current.isEmpty()) {
				list.add(current);
			}
			return list;
		}
	}

	private static record WidgetGroup(List<Widget> widgets, int x, int y) {
	}
}
