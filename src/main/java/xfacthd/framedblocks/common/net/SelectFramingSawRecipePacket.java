package xfacthd.framedblocks.common.net;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import xfacthd.framedblocks.api.util.Utils;
import xfacthd.framedblocks.common.menu.IFramingSawMenu;

/**
 * Serverbound: selects a recipe in the currently open framing saw menu
 */
public record SelectFramingSawRecipePacket(int containerId, int recipeIdx) implements FabricPacket
{
    public static final PacketType<SelectFramingSawRecipePacket> TYPE = PacketType.create(
            Utils.rl("select_framing_saw_recipe"), SelectFramingSawRecipePacket::new
    );

    public SelectFramingSawRecipePacket(FriendlyByteBuf buf)
    {
        this(buf.readInt(), buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf)
    {
        buf.writeInt(containerId);
        buf.writeInt(recipeIdx);
    }

    @Override
    public PacketType<?> getType()
    {
        return TYPE;
    }

    /**
     * Called on the server thread
     */
    public void handle(ServerPlayer player, PacketSender sender)
    {
        AbstractContainerMenu menu = player.containerMenu;
        if (menu.containerId == containerId && menu instanceof IFramingSawMenu)
        {
            menu.clickMenuButton(player, recipeIdx);
        }
    }
}
