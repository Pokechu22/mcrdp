package mcrdp;

import static org.lwjgl.opengl.GL11.*;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

public class GuiRDPControl extends GuiScreen {
	private static final ResourceLocation WINDOW = new ResourceLocation("textures/gui/advancements/window.png");

	// Constants
	private static final int WINDOW_EDGE_SIZE = 32;
	private static final int WINDOW_TEX_WIDTH = 252;
	private static final int WINDOW_TEX_HEIGHT = 140;
	private static final int WINDOW_HORIZ_WIDTH = WINDOW_TEX_WIDTH - 2 * WINDOW_EDGE_SIZE;
	private static final int WINDOW_VERT_HEIGHT = WINDOW_TEX_HEIGHT - 2 * WINDOW_EDGE_SIZE;
	private static final int WINDOW_LOWER_U = WINDOW_TEX_WIDTH - WINDOW_EDGE_SIZE;
	private static final int WINDOW_LOWER_V = WINDOW_TEX_HEIGHT - WINDOW_EDGE_SIZE;

	/**
	 * Amount of total display size to reserve.
	 */
	private static final int WIDTH_RESERVE = 200, HEIGHT_RESERVE = 100;
	/**
	 * Minimum resolution guaranteed given to the RDP screen.
	 */
	private static final int GUARANTEED_WIDTH = 96, GUARANTEED_HEIGHT = 72;

	// Display position info
	private int dispWidth, dispHeight, dispX, dispY;

	/**
	 * Width of a single gui pixel on MC (effectively, the ratio between display width and
	 * actual GUI width)
	 */
	private int mcScaleFactor;
	/**
	 * Width of a single gui pixel on the RDP screen (effectively, the ratio between display
	 * width (of the area occupied by the RDP screen) and RDP screen width
	 */
	private int dispScaleFactor;

	/**
	 * True if the RDP screen is currently focused.
	 */
	private boolean rdpFocused = false;
	/**
	 * A bitmask of mouse buttons that are currently held.
	 */
	private int mouseButtons = 0;

	private static final Logger LOGGER = LogManager.getLogger();
	/** The cursor that was being used when the GUI was opened. */
	private final Cursor oldCursor;
	/**
	 * The cursor that is currently being displayed (either the original cursor or
	 * the instance's cursor)
	 */
	private Cursor cursor;

	private final RDPInstance instance;
	public GuiRDPControl(RDPInstance instance) {
		this.instance = instance;
		oldCursor = Mouse.getNativeCursor();
	}

	@Override
	public void initGui() {
		addButton(new GuiButton(0, width / 2 - 200 / 2, height - 40, I18n.format("gui.done")));

		mcScaleFactor = new ScaledResolution(this.mc).getScaleFactor();

		int maxWidth = (this.width - WIDTH_RESERVE);
		int maxHeight = (this.height - HEIGHT_RESERVE);

		int instanceWidth = instance.width;
		int instanceHeight = instance.height;

		int dispScale = 1;
		while (instanceWidth / dispScale >= maxWidth
				&& instanceHeight / dispScale >= maxHeight) {
			dispScale++;
			if (instanceWidth / dispScale < GUARANTEED_WIDTH
					|| instanceHeight / dispScale < GUARANTEED_HEIGHT) {
				dispScale--;
				break;
			}
		}

		dispScaleFactor = dispScale;
		dispWidth = instanceWidth / dispScale;
		dispHeight = instanceHeight / dispScale;
		dispX = this.width / 2 - dispWidth / 2;
		dispY = 38;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();

		int windowWidth = dispWidth + 18;
		int windowHeight = dispHeight + 27;
		int windowX = dispX - 9;
		int windowY = dispY - 18;

		GlStateManager.enableBlend();

		mc.getTextureManager().bindTexture(WINDOW);
		int x = (mouseX - dispX) * dispScaleFactor;
		int y = (mouseY - dispY) * dispScaleFactor;
		if (mouseButtons == 0) {
			// Only re-evaluate focus when there are no mouse buttons held (to allow
			// dragging out of bounds) 
			this.rdpFocused = (x >= 0 && x < dispWidth * dispScaleFactor && y >= 0 && y < dispHeight * dispScaleFactor);
		}
		if (rdpFocused) {
			GlStateManager.color(.75f, .75f, .75f);
		}
		drawTexturedModalRect(windowX, windowY, 0, 0, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE);
		drawScaledCustomSizeModalRect(windowX + WINDOW_EDGE_SIZE, windowY, WINDOW_EDGE_SIZE, 0, WINDOW_HORIZ_WIDTH, WINDOW_EDGE_SIZE, windowWidth - 2 * WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, 256, 256);
		drawTexturedModalRect(windowX + windowWidth - WINDOW_EDGE_SIZE, windowY, WINDOW_LOWER_U, 0, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE);
		drawScaledCustomSizeModalRect(windowX, windowY + WINDOW_EDGE_SIZE, 0, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, WINDOW_VERT_HEIGHT, WINDOW_EDGE_SIZE, windowHeight - 2 * WINDOW_EDGE_SIZE, 256, 256);
		drawScaledCustomSizeModalRect(windowX + windowWidth - WINDOW_EDGE_SIZE, windowY + WINDOW_EDGE_SIZE, WINDOW_LOWER_U, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, WINDOW_VERT_HEIGHT, WINDOW_EDGE_SIZE, windowHeight - 2 * WINDOW_EDGE_SIZE, 256, 256);
		drawTexturedModalRect(windowX, windowY + windowHeight - WINDOW_EDGE_SIZE, 0, WINDOW_LOWER_V, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE);
		drawScaledCustomSizeModalRect(windowX + WINDOW_EDGE_SIZE, windowY + windowHeight - WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, WINDOW_LOWER_V, WINDOW_HORIZ_WIDTH, WINDOW_EDGE_SIZE, windowWidth - 2 * WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, 256, 256);
		drawTexturedModalRect(windowX + windowWidth - WINDOW_EDGE_SIZE, windowY + windowHeight - WINDOW_EDGE_SIZE, WINDOW_LOWER_U, WINDOW_LOWER_V, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE);

		GlStateManager.color(1, 1, 1);
		GlStateManager.bindTexture(instance.glId);

		GlStateManager.pushMatrix();
		GlStateManager.scale(1.0/dispScaleFactor, 1.0/dispScaleFactor, 1);
		drawModalRectWithCustomSizedTexture(dispX * dispScaleFactor, dispY * dispScaleFactor, 0, 0, dispWidth * dispScaleFactor, dispHeight * dispScaleFactor, instance.width, instance.height);
		GlStateManager.popMatrix();

		this.fontRenderer.drawString("MCRDP - " + instance.server, windowX + 8, windowY + 6, 0x404040);

		if (rdpFocused) {
			instance.input.moveMouse(x, y);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);
		if (rdpFocused) {
			if (typedChar != 0) {
				instance.input.sendUnicode(typedChar, false);
				instance.input.sendUnicode(typedChar, true);
			}
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
			throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);

		mouseButtons |= 1 << mouseButton;

		int x = (mouseX - dispX) * dispScaleFactor;
		int y = (mouseY - dispY) * dispScaleFactor;
		if (rdpFocused) {
			if (instance.input.canSendButton(mouseButton + 1)) {
				instance.input.mouseButton(mouseButton + 1, true, x, y);
			}
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
		if (rdpFocused) {
			int dwheel = Mouse.getEventDWheel() * 2; // * 2 because it feels "right"
			if (dwheel != 0) {
				int sig = (dwheel > 0 ? 1 : -1);
				while (Math.abs(dwheel) >= 256) {
					dwheel -= sig * 255;
					instance.input.scrollVertically(sig * 255);
				}
				instance.input.scrollVertically(dwheel);
			}
		}
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);

		mouseButtons &= ~(1 << state);

		if (rdpFocused) {
			int x = (mouseX - dispX) * dispScaleFactor;
			int y = (mouseY - dispY) * dispScaleFactor;
			if (instance.input.canSendButton(state + 1)) {
				instance.input.mouseButton(state + 1, false, x, y);
			}
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			mc.displayGuiScreen(null);
		}
	}

	@Override
	public void updateScreen() {
		Cursor expectedCursor = (rdpFocused ? instance.getCursor() : oldCursor);
		if (this.cursor != expectedCursor) {
			this.cursor = expectedCursor;
			try {
				Mouse.setNativeCursor(expectedCursor);
			} catch (LWJGLException e) {
				LOGGER.warn("Failed to update cursor", e);
			}
		}
	}

	@Override
	public void onGuiClosed() {
		try {
			Mouse.setNativeCursor(this.oldCursor);
		} catch (LWJGLException e) {
			LOGGER.warn("Failed to reset cursor", e);
		}
	}
}
