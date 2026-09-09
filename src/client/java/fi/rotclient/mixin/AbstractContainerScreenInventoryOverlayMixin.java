package fi.rotclient.mixin;

import fi.rotclient.ClientBoundaryGuard;
import fi.rotclient.CustomTooltipRuntime;
import fi.rotclient.DungeonLeapOverlayRuntime;
import fi.rotclient.DungeonPolicy;
import fi.rotclient.PrizeSpinRuntime;
import fi.rotclient.DungeonRuntime;
import fi.rotclient.DungeonTerminalClickRuntime;
import fi.rotclient.ExperimentSolverRuntime;
import fi.rotclient.InventoryChromeRuntime;
import fi.rotclient.InventoryButtonsRuntime;
import fi.rotclient.ItemRarityRuntime;
import fi.rotclient.IotaKuudraRuntime;
import fi.rotclient.MenuKeybindRuntime;
import fi.rotclient.MissingEnchantsRuntime;
import fi.rotclient.QolClientFlavorSupport;
import fi.rotclient.QolVisualRuntime;
import fi.rotclient.RotClientUiDraw;
import fi.rotclient.SkyBlockMenuHighlightRuntime;
import fi.rotclient.SkyBlockTooltipRuntime;
import fi.rotclient.StallMarketRuntime;
import fi.rotclient.SlayerRuntime;
import fi.rotclient.SlotBindsRuntime;
import fi.rotclient.StorageOverlayRuntime;
import fi.rotclient.TermSimRuntime;
import fi.rotclient.TermSimScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenInventoryOverlayMixin {
    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    @Shadow
    protected int imageWidth;

    @Shadow
    protected int imageHeight;

    @Shadow
    protected Slot hoveredSlot;

    @Inject(method = "extractContents", at = @At("HEAD"), cancellable = true)
    private void rotclient$cancelWardrobeRender(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        // 26.2 declares extractBackground on Screen / ContainerScreen, not here.
        InventoryChromeRuntime.extractInventoryBackground(
                screen,
                graphics,
                leftPos,
                topPos,
                imageWidth,
                imageHeight);
        if (MenuKeybindRuntime.shouldCancelContainerRender(screen)) {
            ci.cancel();
            return;
        }
        if (Boolean.TRUE.equals(ClientBoundaryGuard.call(
                "STORAGE_OVERLAY_GATE",
                () -> StorageOverlayRuntime.shouldReplaceVanilla(screen),
                false))) {
            ClientBoundaryGuard.run("STORAGE_OVERLAY_RENDER", () ->
                    rotclient$renderStorageReplacement(screen, graphics, mouseX, mouseY));
            ci.cancel();
            return;
        }
        if (DungeonLeapOverlayRuntime.active(screen)) {
            DungeonLeapOverlayRuntime.render(screen, graphics, mouseX, mouseY);
            ci.cancel();
        }
    }

    @Inject(method = "extractContents", at = @At("RETURN"))
    private void rotclient$inventoryChrome(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (Boolean.TRUE.equals(ClientBoundaryGuard.call(
                "STORAGE_OVERLAY_GATE",
                () -> StorageOverlayRuntime.shouldReplaceVanilla(screen),
                false))) {
            return;
        }
        PrizeSpinRuntime.observe(screen);
        PrizeSpinRuntime.renderChest(screen, graphics);
        InventoryChromeRuntime.afterContainerContents(
                screen,
                graphics,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                mouseX,
                mouseY);
        DungeonTerminalClickRuntime.render(screen, graphics);
        DungeonRuntime.renderMenuExtras(screen, graphics, leftPos, topPos);
        ClientBoundaryGuard.run("STORAGE_OVERLAY_RENDER", () ->
                StorageOverlayRuntime.render(
                screen,
                graphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY));
        ClientBoundaryGuard.run("INVENTORY_BUTTONS_RENDER", () ->
                InventoryButtonsRuntime.render(
                screen,
                graphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY));
    }

    @Unique
    private void rotclient$renderStorageReplacement(
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY) {
        ClientBoundaryGuard.run("STORAGE_OVERLAY_RENDER", () ->
                StorageOverlayRuntime.render(
                screen,
                graphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY));
        ClientBoundaryGuard.call(
                "STORAGE_OVERLAY_HOVER",
                () -> {
                    hoveredSlot = StorageOverlayRuntime.hoveredSlot(
                            screen, leftPos, topPos, mouseX, mouseY);
                    return true;
                },
                false);
        ClientBoundaryGuard.run("INVENTORY_BUTTONS_RENDER", () ->
                InventoryButtonsRuntime.render(
                screen,
                graphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY));
        InventoryChromeRuntime.renderColorEditor(
                screen, graphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
    }

    @Inject(method = "extractLabels", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideVanillaStorageLabels(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (Boolean.TRUE.equals(ClientBoundaryGuard.call(
                "STORAGE_OVERLAY_GATE",
                () -> StorageOverlayRuntime.shouldReplaceVanilla(screen),
                false))) {
            ci.cancel();
            return;
        }
        if (DungeonRuntime.shouldHideTerminalHeader(screen)) {
            ci.cancel();
        }
    }

    @Inject(method = "hasClickedOutside", at = @At("HEAD"), cancellable = true)
    private void rotclient$keepStorageOverlayClicksInside(
            double mouseX,
            double mouseY,
            int left,
            int top,
            CallbackInfoReturnable<Boolean> cir) {
        if (StorageOverlayRuntime.shouldSuppressOutsideClick(mouseX, mouseY)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "onClose", at = @At("HEAD"))
    private void rotclient$storageOverlayUserExit(CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (StorageOverlayRuntime.shouldReplaceVanilla(screen)) {
            StorageOverlayRuntime.markUserExiting();
        }
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void rotclient$customTooltipBegin(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (ExperimentSolverRuntime.shouldHideTooltip(screen)
                || DungeonRuntime.shouldHideTerminalTooltip(screen)) {
            ci.cancel();
            return;
        }
        ItemStack hovered = hoveredSlot == null ? ItemStack.EMPTY : hoveredSlot.getItem();
        CustomTooltipRuntime.beforeTooltip(hovered);
        if (!hovered.isEmpty()
                && QolVisualRuntime.shouldHideEmptyTooltip(
                        hovered.getHoverName().getString(),
                        ((AbstractContainerScreen<?>) (Object) this).getTitle().getString())) {
            ci.cancel();
            return;
        }
        if (CustomTooltipRuntime.onlyName() && !hovered.isEmpty()) {
            Minecraft client = Minecraft.getInstance();
            if (client != null) {
                RotClientUiDraw.text(graphics, client.font,
                        hovered.getHoverName().getString(),
                        CustomTooltipRuntime.shiftedX(mouseX + 12),
                        CustomTooltipRuntime.shiftedY(mouseY - 12),
                        0xFFFFFFFF,
                        true);
            }
            ci.cancel();
        }
    }

    @Inject(method = "extractTooltip", at = @At("RETURN"))
    private void rotclient$inventoryChromeTooltip(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        InventoryChromeRuntime.afterContainerTooltip(
                (AbstractContainerScreen<?>) (Object) this,
                graphics,
                leftPos,
                topPos,
                imageWidth,
                mouseX,
                mouseY);
        SlotBindsRuntime.render(
                (AbstractContainerScreen<?>) (Object) this,
                graphics,
                leftPos,
                topPos,
                mouseX,
                mouseY,
                hoveredSlot);
        MissingEnchantsRuntime.afterTooltip(
                (AbstractContainerScreen<?>) (Object) this,
                graphics,
                mouseX,
                mouseY,
                hoveredSlot);
        StorageOverlayRuntime.applyValueTooltip(graphics, mouseX, mouseY);
        InventoryChromeRuntime.applyValueTooltip(graphics, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void rotclient$openStatsFromInventory(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (hoveredSlot != null) {
            MissingEnchantsRuntime.noteCtrlClick(
                    hoveredSlot.getItem(), event.hasControlDown(), event.button());
        }
        if (QolClientFlavorSupport.hooks().autoExperimentsBlockMouse()) {
            cir.setReturnValue(true);
            return;
        }
        if (PrizeSpinRuntime.swallowClicks((AbstractContainerScreen<?>) (Object) this)) {
            cir.setReturnValue(true);
            return;
        }
        if (InventoryChromeRuntime.handleInventoryClick(
                (AbstractContainerScreen<?>) (Object) this,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                (int) Math.round(event.x()),
                (int) Math.round(event.y()),
                event.button(),
                event.hasControlDown(),
                event.hasShiftDown())) {
            cir.setReturnValue(true);
            return;
        }
        if (Boolean.TRUE.equals(ClientBoundaryGuard.call(
                "STORAGE_OVERLAY_CLICK",
                () -> StorageOverlayRuntime.click(
                (AbstractContainerScreen<?>) (Object) this,
                leftPos,
                topPos,
                (int) Math.round(event.x()), (int) Math.round(event.y()),
                event.button(),
                event.hasShiftDown()),
                false))
                || Boolean.TRUE.equals(ClientBoundaryGuard.call(
                "INVENTORY_BUTTONS_CLICK",
                () -> InventoryButtonsRuntime.click(
                (AbstractContainerScreen<?>) (Object) this,
                leftPos, topPos, imageWidth, imageHeight,
                (int) Math.round(event.x()), (int) Math.round(event.y()), event.button(),
                event.hasShiftDown()),
                false))) {
            cir.setReturnValue(true);
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        DungeonTerminalClickRuntime.record(
                screen,
                (int) Math.round(event.x()),
                (int) Math.round(event.y()),
                event.button());
        if (DungeonLeapOverlayRuntime.click(
                screen,
                (int) Math.round(event.x()),
                (int) Math.round(event.y()))) {
            cir.setReturnValue(true);
            return;
        }
        if (hoveredSlot != null) {
            java.util.ArrayList<ItemStack> stacks = new java.util.ArrayList<>();
            for (Slot slot : screen.getMenu().slots) {
                stacks.add(slot.getItem());
            }
            SkyBlockTooltipRuntime.rememberPurchase(
                    screen.getTitle().getString(),
                    hoveredSlot.getItem(),
                    stacks);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void rotclient$dragInventoryPet(
            MouseButtonEvent event,
            double dragX,
            double dragY,
            CallbackInfoReturnable<Boolean> cir) {
        if (InventoryChromeRuntime.handleInventoryDrag(
                (AbstractContainerScreen<?>) (Object) this,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                (int) Math.round(event.x()),
                (int) Math.round(event.y()),
                event.button())
                || Boolean.TRUE.equals(ClientBoundaryGuard.call(
                "STORAGE_OVERLAY_DRAG",
                () -> StorageOverlayRuntime.drag((int) Math.round(event.y())),
                false))
                || Boolean.TRUE.equals(ClientBoundaryGuard.call(
                "INVENTORY_BUTTONS_DRAG",
                () -> InventoryButtonsRuntime.drag(
                (AbstractContainerScreen<?>) (Object) this,
                leftPos,
                topPos,
                imageWidth,
                imageHeight,
                (int) Math.round(event.x()),
                (int) Math.round(event.y()),
                event.button()),
                false))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void rotclient$releaseInventoryPet(
            MouseButtonEvent event,
            CallbackInfoReturnable<Boolean> cir) {
        if (QolClientFlavorSupport.hooks().autoExperimentsBlockMouse()) {
            cir.setReturnValue(true);
            return;
        }
        if (InventoryChromeRuntime.handleInventoryRelease(event.button())
                || StorageOverlayRuntime.mouseReleased()
                || InventoryButtonsRuntime.release(event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideOffhandSlot(
            GuiGraphicsExtractor graphics,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        if (InventoryChromeRuntime.shouldHideOffhandSlot(
                (AbstractContainerScreen<?>) (Object) this,
                slot)
                || ExperimentSolverRuntime.shouldHideWrongSlot(
                        (AbstractContainerScreen<?>) (Object) this, slot)
                || DungeonRuntime.shouldHideTerminalSlot(
                        (AbstractContainerScreen<?>) (Object) this, slot)) {
            ci.cancel();
            return;
        }
        if (slot != null && (slot.x <= -1000 || slot.y <= -1000)
                && StorageOverlayRuntime.shouldReplaceVanilla(
                        (AbstractContainerScreen<?>) (Object) this)) {
            ci.cancel();
            return;
        }
        if (slot != null && !slot.getItem().isEmpty()) {
            ItemRarityRuntime.paintSlotBackground(
                    graphics,
                    slot.x,
                    slot.y,
                    slot.getItem());
        }
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void rotclient$maskCooldownOverlay(
            GuiGraphicsExtractor graphics,
            Slot slot,
            int mouseX,
            int mouseY,
            CallbackInfo ci) {
        if (slot != null && !slot.getItem().isEmpty()) {
            DungeonRuntime.paintMaskOverlay(graphics, slot.x, slot.y, slot.getItem());
        }
    }

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void rotclient$blockHiddenOffhand(
            Slot slot,
            int slotId,
            int button,
            ContainerInput input,
            CallbackInfo ci) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (Boolean.TRUE.equals(ClientBoundaryGuard.call("STORAGE_SLOT_INPUT", () ->
                StorageOverlayRuntime.shouldBlockSlotInput(screen, slot, slotId, input), true))) {
            ci.cancel();
            return;
        }
        if (slot != null && StallMarketRuntime.shouldBlockClick(
                screen, slot, button, StallMarketRuntime.controlHeld())) {
            ci.cancel();
            return;
        }
        if (QolClientFlavorSupport.hooks().inventoryWalkBlocksClick()) {
            ci.cancel();
            return;
        }
        if (InventoryChromeRuntime.shouldHideOffhandSlot(screen, slot)) {
            ci.cancel();
            return;
        }
        if (ExperimentSolverRuntime.shouldBlockClick(screen, slotId)) {
            ci.cancel();
            return;
        }
        if (screen instanceof TermSimScreen termSim
                && TermSimRuntime.onSlotClicked(termSim, slot, button)) {
            ci.cancel();
            return;
        }
        if (slot != null
                && DungeonPolicy.detectTerminal(
                        screen.getTitle() == null
                                ? ""
                                : screen.getTitle().getString())
                        != DungeonPolicy.Terminal.NONE) {
            if (QolClientFlavorSupport.hooks().shouldCancelTerminalSlot(screen, slot.index)) {
                ci.cancel();
                return;
            }
            DungeonRuntime.noteTerminalSlotClick(screen, slot.index);
            if (DungeonRuntime.enqueueTerminalClick(slot.index, button)) {
                ci.cancel();
                return;
            }
        }
        if (slot != null && SlayerRuntime.shouldBlockMaddoxClick(screen, slot.getItem())) {
            ci.cancel();
            return;
        }
        ExperimentSolverRuntime.onSlotClicked(screen, slotId);
        if (slot != null) {
            IotaKuudraRuntime.onChestSlotClicked(screen, slot);
        }
    }
}
