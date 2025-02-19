package net.shadowmage.ancientwarfare.automation.gui;

import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.shadowmage.ancientwarfare.automation.AncientWarfareAutomation;
import net.shadowmage.ancientwarfare.automation.container.ContainerWarehouseControl;
import net.shadowmage.ancientwarfare.core.container.ContainerBase;
import net.shadowmage.ancientwarfare.core.gui.GuiContainerBase;
import net.shadowmage.ancientwarfare.core.gui.Listener;
import net.shadowmage.ancientwarfare.core.gui.elements.Button;
import net.shadowmage.ancientwarfare.core.gui.elements.Checkbox;
import net.shadowmage.ancientwarfare.core.gui.elements.CompositeItemSlots;
import net.shadowmage.ancientwarfare.core.gui.elements.CompositeScrolled;
import net.shadowmage.ancientwarfare.core.gui.elements.GuiElement;
import net.shadowmage.ancientwarfare.core.gui.elements.ItemSlot;
import net.shadowmage.ancientwarfare.core.gui.elements.Label;
import net.shadowmage.ancientwarfare.core.gui.elements.Text;
import net.shadowmage.ancientwarfare.core.inventory.ItemHashEntry;
import net.shadowmage.ancientwarfare.core.network.NetworkHandler;
import net.shadowmage.ancientwarfare.core.util.InventoryTools.ComparatorItemHashEntry;
import net.shadowmage.ancientwarfare.core.util.InventoryTools.ComparatorItemHashEntry.SortOrder;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GuiWarehouseControl extends GuiContainerBase<ContainerWarehouseControl> {

	private static final String TRANSL_GUI_AUTOMATION = "guistrings.automation.";
	private CompositeScrolled area;
	private Text input;
	private final ComparatorItemHashEntry sorter;
	private Label storedLabel;

	public GuiWarehouseControl(ContainerBase par1Container) {
		super(par1Container, 178, 240);
		sorter = new ComparatorItemHashEntry(getContainer().itemMap, getContainer().getSortType(), getContainer().getSortOrder());
	}

	@Override
	public void initElements() {
		Button sortChange = new Button(8, 8, 110, 12, TRANSL_GUI_AUTOMATION + getContainer().getSortType().toString()) {
			@Override
			protected void onPressed() {
				getContainer().setSortType(getContainer().getSortType().next());
				setText(TRANSL_GUI_AUTOMATION + getContainer().getSortType().toString());
				refreshGui();
			}
		};
		addGuiElement(sortChange);

		Checkbox sortOrderBox = new Checkbox(8 + 55 + 55 + 4, 6, 16, 16, "guistrings.automation.descending") {
			@Override
			public void onToggled() {
				super.onToggled();
				getContainer().setSortOrder(checked() ? SortOrder.ASCENDING : SortOrder.DESCENDING);
				String name = getContainer().getSortOrder().name().toLowerCase(Locale.ENGLISH);
				label = I18n.format(TRANSL_GUI_AUTOMATION + name);
				refreshGui();
			}
		};
		sortOrderBox.setChecked(getContainer().getSortOrder() == SortOrder.ASCENDING);
		addGuiElement(sortOrderBox);

		input = new Text(8, 8 + 12 + 4, 178 - 16, "", this) {
			@Override
			public void onTextUpdated(String oldText, String newText) {
				if (!oldText.equals(newText)) {
					refreshGui();
				}
			}

		};
		addGuiElement(input);

		area = new CompositeItemSlots(this, 0, 8 + 12 + 4 + 12 + 2, 178, 96, this);

		Listener l = new Listener(Listener.MOUSE_DOWN) {
			@Override
			public boolean onEvent(GuiElement widget, ActivationEvent evt) {
				if (evt.mButton == 0 && widget.isMouseOverElement(evt.mx, evt.my) && !area.isMouseOverSubElement(evt.mx, evt.my)) {
					getContainer().handleClientRequestSpecific(null, 0, isShiftKeyDown(), false);
				}
				return true;
			}
		};
		area.addNewListener(l);
		addGuiElement(area);

		Button b = new Button(8, 240 - 8 - 12, 40, 12, "guistrings.automation.adjust_bounds") {
			@Override
			protected void onPressed() {
				NetworkHandler.INSTANCE.openGui(player, NetworkHandler.GUI_WORKSITE_BOUNDS, getContainer().tileEntity.getPos());
			}
		};
		addGuiElement(b);

		storedLabel = new Label(8 + 40 + 4, 240 - 8 - 11, I18n.format("guistrings.warehouse.storage", getContainer().currentStored, getContainer().maxStorage));
		addGuiElement(storedLabel);
	}

	@Override
	public void setupElements() {
		area.clearElements();
		addInventoryViewElements();
		storedLabel.setText(I18n.format("guistrings.warehouse.storage", getContainer().currentStored, getContainer().maxStorage));
		storedLabel.setColor(getContainer().isWarehouseFull() ? Color.RED.getRGB() : 4210752);
	}

	private void addInventoryViewElements() {
		ItemStack stack;
		List<ItemHashEntry> displayEntries = new ArrayList<>();

		for (ItemHashEntry entry : getContainer().itemMap.keySet()) {
			stack = entry.getItemStack().copy();
			if (matchesSearch(stack, entry.getNameAndTooltip())) {
				displayEntries.add(entry);
			}
		}

		sortItems(displayEntries);

		int x = 0;
		int y = 0;
		int totalSize = 22;
		for (ItemHashEntry entry : displayEntries) {
			if (x >= 9) {
				x = 0;
				y++;
				totalSize += 18;
			}

			ItemStack displayStack = entry.getItemStack().copy();
			int count = getContainer().itemMap.getCount(entry);
			displayStack.setCount(count);

			ItemSlot slot = new ItemSlot(4 + x * 18, 4 + y * 18, displayStack, this) {
				@Override
				public void onSlotClicked(ItemStack stack, boolean rightClicked) {
					getContainer().handleClientRequestSpecific(entry.writeToNBT(), count, isShiftKeyDown(), rightClicked);
				}
			};
			area.addGuiElement(slot);
			x++;
		}
		area.setAreaSize(totalSize);
	}

	private boolean matchesSearch(ItemStack stack, String nameAndTooltip) {
		String searchInput = input.getText().toLowerCase(Locale.ENGLISH);
		if (searchInput.isEmpty()) {
			return true;
		}

		if (searchInput.startsWith("@")) {
			String[] searchStrings = searchInput.split(" ");
			String modName = searchStrings[0].substring(1);
			ResourceLocation registryName = stack.getItem().getRegistryName();
			if (registryName == null || !registryName.getResourceDomain().contains(modName)) {
				return false;
			} else if (searchStrings.length <= 1) {
				return true;
			}
			searchInput = String.join(" ", Arrays.copyOfRange(searchStrings, 1, searchStrings.length));
		}

		return nameAndTooltip.contains(searchInput);
	}

	private void sortItems(List<ItemHashEntry> items) {
		sorter.setSortType(getContainer().getSortType());
		sorter.setSortOrder(getContainer().getSortOrder());
		try {
			items.sort(sorter);
		}
		catch (IllegalArgumentException ex) {
			AncientWarfareAutomation.LOG.error("Error sorting warehouse items: ", ex);
		}
	}

}
