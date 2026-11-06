package slimeknights.tconstruct.smeltery.network;

import io.github.fabricators_of_create.porting_lib.fluids.FluidStack;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.smeltery.block.entity.FaucetBlockEntity;

/**
 * Sent to clients to activate the faucet animation clientside.
 * TODO(fabric): Mantle 1.11 removed BlockEntityPacket, so this packet looks up the block entity itself.
 */
@RequiredArgsConstructor
@ToString
public class FaucetActivationPacket implements IThreadsafePacket {
  protected final BlockPos pos;
  protected final FluidStack fluid;
  private final boolean isPouring;

  public FaucetActivationPacket(FriendlyByteBuf buffer) {
    this.pos = buffer.readBlockPos();
    this.fluid = FluidStack.readFromPacket(buffer);
    this.isPouring = buffer.readBoolean();
  }

  @Override
  public void encode(FriendlyByteBuf buffer) {
    buffer.writeBlockPos(pos);
    fluid.writeToPacket(buffer);
    buffer.writeBoolean(isPouring);
  }

  @Override
  public void handleThreadsafe(Context context) {
    HandleClient.handle(this);
  }

  /** Safely runs client side only code in a method only called on client */
  private static class HandleClient {
    private static void handle(FaucetActivationPacket packet) {
      assert Minecraft.getInstance().level != null;
      BlockEntity te = Minecraft.getInstance().level.getBlockEntity(packet.pos);
      if (te instanceof FaucetBlockEntity) {
        ((FaucetBlockEntity) te).onActivationPacket(packet.fluid, packet.isPouring);
      }
    }
  }
}
