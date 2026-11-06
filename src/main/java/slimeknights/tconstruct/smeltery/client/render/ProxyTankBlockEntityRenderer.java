package slimeknights.tconstruct.smeltery.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import slimeknights.mantle.client.render.FluidCuboid;
import slimeknights.mantle.client.render.FluidRenderer;
import slimeknights.mantle.client.render.RenderItem;
import slimeknights.mantle.client.render.RenderingHelper;
import slimeknights.tconstruct.common.config.Config;
import slimeknights.tconstruct.smeltery.block.entity.ProxyTankBlockEntity;
import slimeknights.tconstruct.smeltery.block.entity.tank.ProxyItemTank;

import java.util.List;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

/** Renderer for {@link ProxyTankBlockEntity}. Unlike {@link TankInventoryBlockEntityRenderer}, does not use a {@link slimeknights.tconstruct.library.fluid.FluidTankAnimated} */
public class ProxyTankBlockEntityRenderer implements BlockEntityRenderer<ProxyTankBlockEntity> {
  @SuppressWarnings("unused")  // nicer lambda
  public ProxyTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

  @Override
  public void render(ProxyTankBlockEntity proxyTank, float partialTicks, PoseStack matrices, MultiBufferSource buffer, int light, int combinedOverlayIn) {
    BlockState state = proxyTank.getBlockState();
    List<FluidCuboid> fluids = Config.CLIENT.tankFluidModel.get() ? List.of() : FluidCuboid.REGISTRY.get(state, List.of());
    List<RenderItem> renderItems = RenderItem.STATE_REGISTRY.get(state, List.of());
    if (!fluids.isEmpty() || !renderItems.isEmpty()) {
      // rotate the matrix
      boolean isRotated = RenderingHelper.applyRotation(matrices, state.getValue(HORIZONTAL_FACING));

      // render fluids
      // TODO(fabric): Forge's IFluidHandlerItem exposed tank arrays; the Fabric storage exposes one slot
      ProxyItemTank<?> itemTank = proxyTank.getItemTank();
      FluidVariant stored = ((SingleSlotStorage<FluidVariant>) itemTank.getFluidStorage()).getResource();
      FluidStack fluid = stored.isBlank() ? FluidStack.EMPTY : new FluidStack(stored, itemTank.getAmount());
      if (!fluids.isEmpty()) {
        int capacity = (int) itemTank.getCapacity();
        for (FluidCuboid cube : fluids) {
          FluidRenderer.renderScaledCuboid(matrices, buffer, cube, fluid, 0, capacity, light, true);
        }
      }

      // render items
      for (int i = 0; i < renderItems.size(); i++) {
        RenderingHelper.renderItem(matrices, buffer, itemTank.getStack(), renderItems.get(i), light);
      }

      // pop back rotation
      if (isRotated) {
        matrices.popPose();
      }
    }
  }
}
