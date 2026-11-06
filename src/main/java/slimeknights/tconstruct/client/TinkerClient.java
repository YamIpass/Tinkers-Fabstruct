package slimeknights.tconstruct.client;

import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint for Tinkers' Construct.
 *
 * <p>{@code fabric.mod.json} can only list classes that exist in every environment it loads in, so this thin class is
 * the client entrypoint and the real work stays in {@link slimeknights.tconstruct.shared.TinkerClient}, which must only
 * ever be touched from the client side.</p>
 */
public class TinkerClient implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    slimeknights.tconstruct.shared.TinkerClient.onConstruct();
    slimeknights.tconstruct.FabricEvents.initClient();
  }
}
