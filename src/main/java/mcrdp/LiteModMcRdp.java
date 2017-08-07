package mcrdp;

import static org.lwjgl.opengl.GL11.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.INetHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.propero.rdp.OrderSurface;

import org.lwjgl.BufferUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mumfrey.liteloader.LiteMod;
import com.mumfrey.liteloader.PacketHandler;
import com.mumfrey.liteloader.PlayerClickListener;
import com.mumfrey.liteloader.PlayerInteractionListener.MouseButton;
import com.mumfrey.liteloader.PreRenderListener;

public class LiteModMcRdp implements LiteMod, PlayerClickListener, PacketHandler, PreRenderListener {
	private final Minecraft minecraft = Minecraft.getMinecraft();
	private int textureID = -1;

	@Override
	public String getName() {
		return "mcrdp";
	}

	@Override
	public String getVersion() {
		return "0.1";
	}

	@Override
	public void init(File configPath) {
		instances.put("pi", RDPInstance.create("pi", "pi", "", 800, 600));
		textureID = glGenTextures();
	}

	@Override
	public void upgradeSettings(String version, File configPath,
			File oldConfigPath) {

	}

	@Override
	public List<Class<? extends Packet<?>>> getHandledPackets() {
		List<Class<? extends Packet<?>>> packets = new ArrayList<Class<? extends Packet<?>>>();
		packets.add(SPacketChunkData.class);
		packets.add(SPacketUpdateTileEntity.class);
		return packets;
	}

	@Override
	public boolean handlePacket(INetHandler netHandler, Packet<?> packet) {
		if (packet instanceof SPacketChunkData) {
			SPacketChunkData cpacket = (SPacketChunkData) packet;
			for (NBTTagCompound tag : cpacket.getTileEntityTags()) {
				if (tag.getString("id").toLowerCase().contains("sign")) {
					handleNewTE(new BlockPos(tag.getInteger("x"), tag.getInteger("y"),
							tag.getInteger("z")), tag);
				}
			}
		} else if (packet instanceof SPacketUpdateTileEntity) {
			SPacketUpdateTileEntity cpacket = (SPacketUpdateTileEntity) packet;
			if (cpacket.getTileEntityType() == 9) {
				handleNewTE(cpacket.getPos(), cpacket.getNbtCompound());
			}
		}
		return true;
	}

	/**
	 * Contains information about an RDP sign.
	 */
	public class RDPInfo {
		public RDPInfo(BlockPos pos, EnumFacing facing, String[] lines) throws InvalidRDPException {
			if (!lines[0].contains("mcrdp")) {
				throw new InvalidRDPException(pos, "First line '" + lines[0] + "' doesn't contain 'mcrdp'");
			}
			if (lines[1].matches("\\d+x\\d+")) {
				String[] part = lines[1].split("x");
				width = Integer.parseInt(part[0]);
				height = Integer.parseInt(part[1]);
			} else if (lines[1].isEmpty()) {
				width = 8;
				height = 6;
			} else {
				throw new InvalidRDPException(pos, "Second line '" + lines[1] + "' should be empty or [width]x[height]");
			}
			if (instances.containsKey(lines[3])) {
				server = lines[3];
			} else {
				throw new InvalidRDPException(pos, "Fourth line '" + lines[3] + "' does not contain a known RDP server");
			}
			this.pos = pos;
			this.facing = facing;
		}

		public final BlockPos pos;
		public final int width, height;
		public final String server;
		public final EnumFacing facing;
	}

	private Map<String, RDPInstance> instances = Maps.newHashMap();
	private List<RDPInfo> infos = Lists.newArrayList();

	private void handleNewTE(BlockPos pos, NBTTagCompound tag) {
		IBlockState state = minecraft.world.getBlockState(pos);
		if (state.getBlock() != Blocks.WALL_SIGN) {
			return;
		}

		String[] lines = new String[4];
		for (int i = 0; i < 4; i++) {
			lines[i] = ITextComponent.Serializer.fromJsonLenient(tag.getString("Text" + (i + 1))).getUnformattedText();
		}
		if (!lines[0].contains("mcrdp")) {
			// Nothing at all that can be wrong.
			return;
		}
		try {
			infos.add(new RDPInfo(pos, state.getValue(BlockWallSign.FACING), lines));
			minecraft.ingameGUI.getChatGUI().printChatMessage(new TextComponentString("Test"));
		} catch (InvalidRDPException ex) {
			ITextComponent component = new TextComponentString(ex.getMessage());
			component.getStyle().setColor(TextFormatting.RED);
			minecraft.ingameGUI.getChatGUI().printChatMessage(component);
		}
	}

	@Override
	public boolean onMouseClicked(EntityPlayerSP player, MouseButton button) {
		return true;
	}

	@Override
	public boolean onMouseHeld(EntityPlayerSP player, MouseButton button) {
		return true;
	}

	@Override
	public void onSetupCameraTransform(float partialTicks, int pass,
			long timeSlice) {
		if (minecraft.world == null) {
			// Unload world check
			minecraft.ingameGUI.getChatGUI().printChatMessage(new TextComponentString("Removed all " + this.infos.size() + " infos"));
			return;
		}
		{
			// Unloaded info check
			int oldSize = this.infos.size();
			boolean removedAny =
					this.infos.removeIf(info -> minecraft.world.getBlockState(info.pos).getBlock() != Blocks.WALL_SIGN);
			int newSize = this.infos.size();
	
			if (removedAny) {
				minecraft.ingameGUI.getChatGUI().printChatMessage(new TextComponentString("Removed " + (oldSize - newSize) + " unloaded infos"));
			}
		}

		for (RDPInstance instance : instances.values()) {
			if (instance.canvas == null) {
				if (!this.infos.isEmpty()) {
					int oldSize = this.infos.size();
					boolean removedAny =
							this.infos.removeIf(info -> info.server.equals(instance.server));
					int newSize = this.infos.size();
					if (removedAny) {
						minecraft.ingameGUI.getChatGUI()
								.printChatMessage(
										new TextComponentString("Purged "
												+ (oldSize - newSize)
												+ " entries for dead instance "
												+ instance.server));
					}
				}
				continue;
			}

			bindImage(instance.canvas);
	
			EntityPlayerSP player = minecraft.player;
			double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTicks;
			double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTicks;
			double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTicks;

			glPushMatrix();
			glTranslated(-x, -y, -z);

			infos.stream().forEach(this::drawInfo);

			glPopMatrix();
		}
	}

	private void bindImage(OrderSurface image) {
		// http://www.java-gaming.org/index.php?topic=25516.0
		int[] pixels = image.getImage(0, 0, image.getWidth(), image.getHeight());

		ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * 4); //4 for RGBA, 3 for RGB

		for(int y = 0; y < image.getHeight(); y++){
			for(int x = 0; x < image.getWidth(); x++){
				int pixel = pixels[y * image.getWidth() + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
				buffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
				buffer.put((byte) (pixel & 0xFF));               // Blue component
				buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
			}
		}

		buffer.flip(); //FOR THE LOVE OF GOD DO NOT FORGET THIS

		// You now have a ByteBuffer filled with the color data of each pixel.
		// Now just create a texture ID and bind it. Then you can load it using 
		// whatever OpenGL method you want, for example:

		// Do the drawing
		glEnable(GL_TEXTURE_2D);

		glBindTexture(GL_TEXTURE_2D, textureID); //Bind texture ID
		glTexParameteri (GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
	}

	/**
	 * Draws a RDP info, assuming the texture already is bound.
	 */
	private void drawInfo(RDPInfo info) {
		try {
			glPushMatrix();
			glTranslatef(info.pos.getX(), info.pos.getY(), info.pos.getZ());
			switch (info.facing) {
			case NORTH:
				glRotatef(180, 0, 1, 0);
				glTranslatef(-1, 0, -1);
				break;
			case EAST:
				glRotatef(90, 0, 1, 0);
				glTranslatef(-1, 0, 0);
				break;
			case SOUTH:
				// Noop
				glRotatef(0, 0, 1, 0);
				glTranslatef(0, 0, 0);
				break;
			case WEST:
				glRotatef(270, 0, 1, 0);
				glTranslatef(0, 0, -1);
				break;
			default:
				// Unexpected values (up, down)
				return;
			}
			drawImage(info.width, info.height);
			glPopMatrix();
		} catch (RuntimeException ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	/**
	 * Draws the given image at the current location, using the given width and height values.
	 * @param width The width in blocks (NOT the width of the image)
	 * @param height The height in blocks (NOT the height of the image)
	 */
	private void drawImage(int width, int height) {
		glBegin(GL_QUADS);
		{
			// This needs to be flipped vertically for some reason...
			final float Z_PUSH = 2/16f; // To avoid z-fighting: slightly larger than a sign
			glTexCoord2f(0, 1);
			glVertex3f(0, 0, Z_PUSH);

			glTexCoord2f(1, 1);
			glVertex3f(width, 0, Z_PUSH);

			glTexCoord2f(1, 0);
			glVertex3f(width, height, Z_PUSH);

			glTexCoord2f(0, 0);
			glVertex3f(0, height, Z_PUSH);
		}
		glEnd();
	}

	@Override
	public void onRenderWorld(float partialTicks) {
	}

	@Override
	public void onRenderSky(float partialTicks, int pass) {
	}

	@Override
	public void onRenderClouds(float partialTicks, int pass,
			RenderGlobal renderGlobal) {
	}

	@Override
	public void onRenderTerrain(float partialTicks, int pass) {

	}
}
