package dev.doctor4t.trainmurdermystery.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.doctor4t.trainmurdermystery.client.TrainMurderMysteryClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen implements ScreenHandlerProvider<T> {

    @Shadow
    protected int x;

    @Shadow
    protected int y;

    @Shadow
    @Nullable
    protected Slot focusedSlot;

    @Shadow
    @Final
    protected T handler;

    @Shadow
    protected abstract void drawSlot(DrawContext context, Slot slot);

    @Shadow
    protected abstract boolean isPointOverSlot(Slot slot, double pointX, double pointY);

    @Shadow
    public static void drawSlotHighlight(DrawContext context, int x, int y, int z) {
    }

    @Shadow
    protected abstract void drawForeground(DrawContext context, int mouseX, int mouseY);

    @Shadow
    private ItemStack touchDragStack;

    @Shadow
    private boolean touchIsRightClickDrag;

    @Shadow
    @Final
    protected Set<Slot> cursorDragSlots;

    @Shadow
    private int draggedStackRemainder;

    @Shadow
    protected boolean cursorDragging;

    @Shadow
    protected abstract void drawItem(DrawContext context, ItemStack stack, int x, int y, String amountText);

    @Shadow
    private ItemStack touchDropReturningStack;

    @Shadow
    private long touchDropTime;

    @Shadow
    private int touchDropX;

    @Shadow
    private int touchDropY;

    @Shadow
    private @Nullable Slot touchDropOriginSlot;

    protected HandledScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (TrainMurderMysteryClient.shouldRestrictPlayerOptions()) {
            int i = this.x;
            int j = this.y;
            super.render(context, mouseX, mouseY, delta);
            RenderSystem.disableDepthTest();
            context.getMatrices().push();
            context.getMatrices().translate((float) i, (float) j, 0.0F);
            this.focusedSlot = null;

            for (int k = 0; k < this.handler.slots.size(); k++) {
                Slot slot = this.handler.slots.get(k);
                if (slot.isEnabled()) {
                    this.drawSlot(context, slot);
                }

                if (this.isPointOverSlot(slot, mouseX, mouseY) && slot.isEnabled()) {
                    this.focusedSlot = slot;
                    int l = slot.x;
                    int m = slot.y;
                    if (this.focusedSlot.canBeHighlighted()) {
                        drawSlotHighlight(context, l, m, 0);
                    }
                }
            }

            this.drawForeground(context, mouseX, mouseY);
            ItemStack itemStack = this.touchDragStack.isEmpty() ? this.handler.getCursorStack() : this.touchDragStack;
            if (!itemStack.isEmpty()) {
                int n = 8;
                int l = this.touchDragStack.isEmpty() ? 8 : 16;
                String string = null;
                if (!this.touchDragStack.isEmpty() && this.touchIsRightClickDrag) {
                    itemStack = itemStack.copyWithCount(MathHelper.ceil((float) itemStack.getCount() / 2.0F));
                } else if (this.cursorDragging && this.cursorDragSlots.size() > 1) {
                    itemStack = itemStack.copyWithCount(this.draggedStackRemainder);
                    if (itemStack.isEmpty()) {
                        string = Formatting.YELLOW + "0";
                    }
                }

                this.drawItem(context, itemStack, mouseX - i - 8, mouseY - j - l, string);
            }

            if (!this.touchDropReturningStack.isEmpty()) {
                float f = (float) (Util.getMeasuringTimeMs() - this.touchDropTime) / 100.0F;
                if (f >= 1.0F) {
                    f = 1.0F;
                    this.touchDropReturningStack = ItemStack.EMPTY;
                }

                int l = this.touchDropOriginSlot.x - this.touchDropX;
                int m = this.touchDropOriginSlot.y - this.touchDropY;
                int o = this.touchDropX + (int) ((float) l * f);
                int p = this.touchDropY + (int) ((float) m * f);
                this.drawItem(context, this.touchDropReturningStack, o, p, null);
            }

            context.getMatrices().pop();
            RenderSystem.enableDepthTest();

            ci.cancel();
        }
    }

}
