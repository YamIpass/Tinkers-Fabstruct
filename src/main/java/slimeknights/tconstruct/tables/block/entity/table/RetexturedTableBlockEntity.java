package slimeknights.tconstruct.tables.block.entity.table;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import slimeknights.mantle.block.entity.IRetexturedBlockEntity;
import slimeknights.mantle.client.model.ModelData;
import slimeknights.mantle.util.RetexturedHelper;
import slimeknights.tconstruct.shared.block.entity.TableBlockEntity;

import javax.annotation.Nonnull;

public abstract class RetexturedTableBlockEntity extends TableBlockEntity implements IRetexturedBlockEntity {
  private static final String TAG_TEXTURE = "texture";

  @Nonnull @Getter
  protected Block texture = Blocks.AIR;
  public RetexturedTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Component name, int size) {
    super(type, pos, state, name, size);
  }
  // TODO(fabric): Forge's BlockEntity#getRenderBoundingBox() has no Fabric counterpart
  public AABB getRenderBoundingBox() {
    return new AABB(worldPosition, worldPosition.offset(1, 2, 1));
  }


  /* Textures */

  @Nonnull
  @Override
  public ModelData getRenderData() {
    return RetexturedHelper.getModelData(texture);
  }

  @Override
  public String getTextureName() {
    return RetexturedHelper.getTextureName(texture);
  }

  private void textureUpdated() {
    // update the texture in BE data
    if (level != null && level.isClientSide) {
      // TODO(fabric): Forge's requestModelDataUpdate() has no Fabric counterpart, Mantle re-sends the block update
      RetexturedHelper.onTextureUpdated(this);
    }
  }

  @Override
  public void updateTexture(String name) {
    Block oldTexture = texture;
    texture = RetexturedHelper.getBlock(name);
    if (oldTexture != texture) {
      setChangedFast();
      textureUpdated();
    }
  }

  @Override
  public void saveSynced(CompoundTag tags) {
    super.saveSynced(tags);
    if (texture != Blocks.AIR) {
      tags.putString(TAG_TEXTURE, getTextureName());
    }
  }

  @Override
  public void load(CompoundTag tags) {
    super.load(tags);
    if (tags.contains(TAG_TEXTURE, Tag.TAG_STRING)) {
      texture = RetexturedHelper.getBlock(tags.getString(TAG_TEXTURE));
      textureUpdated();
    }
  }
}
