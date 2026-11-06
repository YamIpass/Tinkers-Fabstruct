#!/usr/bin/perl
# Adds missing imports for the Fabric/Porting Lib symbols the port introduces.
# usage: addimports.pl <file> [<file> ...]
use strict; use warnings;

my @map = (
  ['Storage',            'net.fabricmc.fabric.api.transfer.v1.storage.Storage'],
  ['StorageView',        'net.fabricmc.fabric.api.transfer.v1.storage.StorageView'],
  ['StorageUtil',        'net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil'],
  ['SingleSlotStorage',  'net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage'],
  ['SlottedStorage',     'net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage'],
  ['FluidVariant',       'net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant'],
  ['FluidStorage',       'net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage'],
  ['ItemVariant',        'net.fabricmc.fabric.api.transfer.v1.item.ItemVariant'],
  ['ContainerItemContext','net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext'],
  ['Transaction',        'net.fabricmc.fabric.api.transfer.v1.transaction.Transaction'],
  ['TransactionContext', 'net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext'],
  ['FluidValues',        'slimeknights.tconstruct.library.recipe.FluidValues'],
  ['FluidStack',         'io.github.fabricators_of_create.porting_lib.fluids.FluidStack'],
  ['LazyOptional',       'io.github.fabricators_of_create.porting_lib.util.LazyOptional'],
  ['RegistryObject',     'io.github.fabricators_of_create.porting_lib.util.RegistryObject'],
  ['ItemHandlerHelper',  'io.github.fabricators_of_create.porting_lib.transfer.item.ItemHandlerHelper'],
  ['ItemStackHandler',   'io.github.fabricators_of_create.porting_lib.transfer.item.ItemStackHandler'],
  ['SlottedStackStorage','io.github.fabricators_of_create.porting_lib.transfer.item.SlottedStackStorage'],
  ['FluidTank',          'io.github.fabricators_of_create.porting_lib.transfer.fluid.FluidTank'],
  ['AbstractBlock',      'net.minecraft.block.AbstractBlock'],
  ['VoxelShapes',        'net.minecraft.util.shape.VoxelShapes'],
  ['VoxelShape',         'net.minecraft.util.shape.VoxelShape'],
  ['ShapeContext',       'net.minecraft.block.ShapeContext'],
  ['StateManager',       'net.minecraft.state.StateManager'],
  ['MatrixStack',        'net.minecraft.client.util.math.MatrixStack'],
  ['TooltipContext',     'net.minecraft.client.item.TooltipContext'],
  ['BooleanBiFunction',  'net.minecraft.util.function.BooleanBiFunction'],
  ['BlockEntityProvider','net.minecraft.block.BlockEntityProvider'],
  ['BlockRotation',      'net.minecraft.util.BlockRotation'],
  ['BlockMirror',        'net.minecraft.util.BlockMirror'],
  ['NbtHelper',          'net.minecraft.nbt.NbtHelper'],
  ['Formatting',         'net.minecraft.util.Formatting'],
  ['DrawContext',        'net.minecraft.client.gui.DrawContext'],
  ['VertexConsumerProvider','net.minecraft.client.render.VertexConsumerProvider'],
  ['HandledScreen',      'net.minecraft.client.gui.screen.ingame.HandledScreen'],
  ['Inventory',          'net.minecraft.inventory.Inventory'],
  ['PropertyDelegate',   'net.minecraft.screen.PropertyDelegate'],
  ['ScreenHandler',      'net.minecraft.screen.ScreenHandler'],
  ['NamedScreenHandlerFactory','net.minecraft.screen.NamedScreenHandlerFactory'],
  ['BlockEntityRendererFactory','net.minecraft.client.render.block.entity.BlockEntityRendererFactory'],
  ['NavigationType',     'net.minecraft.entity.ai.pathing.NavigationType'],
  ['FluidAction',        'net.minecraftforge.fluids.capability.IFluidHandler.FluidAction'],
);

for my $file (@ARGV) {
  open(my $fh, '<:encoding(UTF-8)', $file) or die "open $file: $!";
  local $/; my $src = <$fh>; close $fh;
  my @add;
  for my $e (@map) {
    my ($name, $fqn) = @$e;
    next if $src =~ /^import\s+\Q$fqn\E;/m;
    # skip the FluidAction import: the port drops it
    next if $name eq 'FluidAction';
    next unless $src =~ /(?<![\w."])$name(?![\w"])/;
    push @add, "import $fqn;";
  }
  next unless @add;
  $src =~ s/^(package\s+[\w.]+;\n)/$1\n@{[ join("\n", @add) ]}\n/m;
  open(my $oh, '>:encoding(UTF-8)', $file) or die "write $file: $!";
  print $oh $src; close $oh;
}
