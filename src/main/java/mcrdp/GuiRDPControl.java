package mcrdp;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
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

	// Border (window) values
	private int windowWidth, windowHeight, windowX, windowY;
	// Display info
	private int dispWidth, dispHeight, dispX, dispY;

	private static final Logger LOGGER = LogManager.getLogger();
	private final Cursor oldCursor;
	private Cursor cursor;

	private final RDPInstance instance;
	public GuiRDPControl(RDPInstance instance) {
		this.instance = instance;
		oldCursor = Mouse.getNativeCursor();
		System.out.println(oldCursor);
	}

	@Override
	public void initGui() {
		try {
			this.cursor = this.instance.getCursor();
			Mouse.setNativeCursor(this.instance.getCursor());
		} catch (LWJGLException e) {
			LOGGER.warn("Failed to set cursor", e);
		}

		addButton(new GuiButton(0, width / 2 - 200 / 2, height - 40, I18n.format("gui.done")));

		windowWidth = Math.max(Math.min(instance.width + 18, this.width - 200), 100);
		windowHeight = Math.max(Math.min(instance.height + 27, this.height - 80), 100);
		windowX = this.width / 2 - windowWidth / 2;
		windowY = 20;
		dispWidth = windowWidth - 18;
		dispHeight = windowHeight - 27;
		dispX = windowX + 9;
		dispY = windowY + 18;
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();

		GlStateManager.enableBlend();

		mc.getTextureManager().bindTexture(WINDOW);
		drawTexturedModalRect(windowX, windowY, 0, 0, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE);
		drawScaledCustomSizeModalRect(windowX + WINDOW_EDGE_SIZE, windowY, WINDOW_EDGE_SIZE, 0, WINDOW_HORIZ_WIDTH, WINDOW_EDGE_SIZE, windowWidth - 2 * WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, 256, 256);
		drawTexturedModalRect(windowX + windowWidth - WINDOW_EDGE_SIZE, windowY, WINDOW_LOWER_U, 0, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE);
		drawScaledCustomSizeModalRect(windowX, windowY + WINDOW_EDGE_SIZE, 0, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, WINDOW_VERT_HEIGHT, WINDOW_EDGE_SIZE, windowHeight - 2 * WINDOW_EDGE_SIZE, 256, 256);
		drawScaledCustomSizeModalRect(windowX + windowWidth - WINDOW_EDGE_SIZE, windowY + WINDOW_EDGE_SIZE, WINDOW_LOWER_U, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, WINDOW_VERT_HEIGHT, WINDOW_EDGE_SIZE, windowHeight - 2 * WINDOW_EDGE_SIZE, 256, 256);
		drawTexturedModalRect(windowX, windowY + windowHeight - WINDOW_EDGE_SIZE, 0, WINDOW_LOWER_V, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE);
		drawScaledCustomSizeModalRect(windowX + WINDOW_EDGE_SIZE, windowY + windowHeight - WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, WINDOW_LOWER_V, WINDOW_HORIZ_WIDTH, WINDOW_EDGE_SIZE, windowWidth - 2 * WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE, 256, 256);
		drawTexturedModalRect(windowX + windowWidth - WINDOW_EDGE_SIZE, windowY + windowHeight - WINDOW_EDGE_SIZE, WINDOW_LOWER_U, WINDOW_LOWER_V, WINDOW_EDGE_SIZE, WINDOW_EDGE_SIZE);

		GlStateManager.bindTexture(instance.glId);
		drawModalRectWithCustomSizedTexture(dispX, dispY, 0, 0, dispWidth, dispHeight, instance.width, instance.height);

		this.fontRenderer.drawString("MCRDP - " + instance.server, windowX + 8, windowY + 6, 0x404040);

		int x = mouseX - dispX;
		int y = mouseY - dispY;
		if (x >= 0 && x < dispWidth && y >= 0 && y < dispHeight) {
			instance.input.moveMouse(x, y);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		super.keyTyped(typedChar, keyCode);
		if (typedChar != 0) {
			instance.input.sendUnicode(typedChar, false);
			instance.input.sendUnicode(typedChar, true);
		}
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton)
			throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);

		int x = mouseX - dispX;
		int y = mouseY - dispY;
		if (x >= 0 && x < dispWidth && y >= 0 && y < dispHeight) {
			if (instance.input.canSendButton(mouseButton + 1)) {
				instance.input.mouseButton(mouseButton + 1, true, x, y);
			}
		}
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();
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

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);
		int x = mouseX - dispX;
		int y = mouseY - dispY;
		if (x >= 0 && x < dispWidth && y >= 0 && y < dispHeight) {
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
		if (this.cursor != instance.getCursor()) {
			this.cursor = instance.getCursor();
			try {
				Mouse.setNativeCursor(this.instance.getCursor());
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
