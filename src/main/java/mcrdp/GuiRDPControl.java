package mcrdp;

import static org.lwjgl.opengl.GL11.*;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.propero.rdp.Input;

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
		boolean smooth = dispScaleFactor > mcScaleFactor;
		if (smooth) {
			GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
			GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		}
		GlStateManager.scale(1.0/dispScaleFactor, 1.0/dispScaleFactor, 1);
		drawModalRectWithCustomSizedTexture(dispX * dispScaleFactor, dispY * dispScaleFactor, 0, 0, dispWidth * dispScaleFactor, dispHeight * dispScaleFactor, instance.width, instance.height);
		if (smooth) {
			GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			GlStateManager.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		}
		GlStateManager.popMatrix();

		this.fontRenderer.drawString("MCRDP - " + instance.server, windowX + 8, windowY + 6, 0x404040);

		if (rdpFocused) {
			instance.input.moveMouse(x, y);
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
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

	/**
	 * Maps LWJGL keycodes to key event VKEY codes.
	 */
	private static final Int2IntMap KEYBOARD_TO_VKEY = new Int2IntArrayMap();
	static {
		// In order of declaration in Keyboard
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_ESCAPE, KeyEvent.VK_ESCAPE);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_BACK, KeyEvent.VK_BACK_SPACE);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_TAB, KeyEvent.VK_TAB);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_RETURN, KeyEvent.VK_ENTER);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_LCONTROL, KeyEvent.VK_CONTROL);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_LSHIFT, KeyEvent.VK_SHIFT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_RSHIFT, KeyEvent.VK_SHIFT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_LMENU, KeyEvent.VK_ALT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_CAPITAL, KeyEvent.VK_CAPS_LOCK);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F1, KeyEvent.VK_F1);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F2, KeyEvent.VK_F2);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F3, KeyEvent.VK_F3);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F4, KeyEvent.VK_F4);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F5, KeyEvent.VK_F5);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F6, KeyEvent.VK_F6);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F7, KeyEvent.VK_F7);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F8, KeyEvent.VK_F8);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F9, KeyEvent.VK_F9);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F10, KeyEvent.VK_F10);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_NUMLOCK, KeyEvent.VK_NUM_LOCK);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_SCROLL, KeyEvent.VK_SCROLL_LOCK);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F11, KeyEvent.VK_F11);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F12, KeyEvent.VK_F12);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F13, KeyEvent.VK_F13);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F14, KeyEvent.VK_F14);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F15, KeyEvent.VK_F15);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F16, KeyEvent.VK_F16);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F17, KeyEvent.VK_F17);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F18, KeyEvent.VK_F18);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_KANA, KeyEvent.VK_KANA);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_F19, KeyEvent.VK_F19);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_CONVERT, KeyEvent.VK_CONVERT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_NOCONVERT, KeyEvent.VK_NONCONVERT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_CIRCUMFLEX, KeyEvent.VK_CIRCUMFLEX);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_NUMPADENTER, KeyEvent.VK_ENTER);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_RCONTROL, KeyEvent.VK_CONTROL);
		//KEYBOARD_TO_VKEY.put(Keyboard.KEY_SYSRQ, KeyEvent.VK_SYSRQ);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_RMENU, KeyEvent.VK_ALT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_PAUSE, KeyEvent.VK_PAUSE);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_HOME, KeyEvent.VK_HOME);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_UP, KeyEvent.VK_UP);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_PRIOR, KeyEvent.VK_PAGE_UP);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_LEFT, KeyEvent.VK_LEFT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_RIGHT, KeyEvent.VK_RIGHT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_END, KeyEvent.VK_END);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_DOWN, KeyEvent.VK_DOWN);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_NEXT, KeyEvent.VK_PAGE_DOWN);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_INSERT, KeyEvent.VK_INSERT);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_DELETE, KeyEvent.VK_DELETE);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_CLEAR, KeyEvent.VK_CLEAR);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_LMETA, KeyEvent.VK_META);
		KEYBOARD_TO_VKEY.put(Keyboard.KEY_RMETA, KeyEvent.VK_META);
	}

	@Override
	public void handleKeyboardInput() throws IOException {
		if (this.rdpFocused) {
			if (Keyboard.isRepeatEvent()) {
				// Repeat events should be handled on the other machine
				return;
			}
			char typedChar = Keyboard.getEventCharacter();

			if (typedChar >= ' ') {
				// Send characters with unicode when possible, as it means we don't
				// have to deal with the VKEY.
				instance.input.sendUnicode(typedChar, !Keyboard.getEventKeyState());
			} else {
				int keyCode = Keyboard.getEventKey();
				if (keyCode == Keyboard.KEY_NONE) {
					LOGGER.warn("Unknown key pressed with neither an acceptable keyCode nor char");
					return;
				}
				if (!KEYBOARD_TO_VKEY.containsKey(keyCode)) {
					LOGGER.warn("Key with keyCode {} does not have a vkey in the map", keyCode);
					return;
				}

				int keyEventCode = KEYBOARD_TO_VKEY.get(keyCode);

				// This is a bit of a mess; try to emulate a java event
				instance.input.modifiersValid = true;
				long time = Input.getTime();

				boolean press = Keyboard.getEventKeyState();
				int eventType = press ? KeyEvent.KEY_PRESSED : KeyEvent.KEY_RELEASED;
				int keyEventModifiers = 0;
				if (isCtrlKeyDown()) {
					keyEventModifiers |= KeyEvent.CTRL_DOWN_MASK;
				}
				if (isAltKeyDown()) {
					keyEventModifiers |= KeyEvent.ALT_DOWN_MASK;
				}
				if (isShiftKeyDown()) {
					keyEventModifiers |= KeyEvent.SHIFT_DOWN_MASK;
				}
				KeyEvent event = new KeyEvent(DummyComponent.INSTANCE, eventType, time, keyEventModifiers, keyEventCode, KeyEvent.CHAR_UNDEFINED);

				instance.input.lastKeyEvent = event;

				if (!instance.input.handleSpecialKeys(time, event, press)) {
					instance.input.sendKeyPresses(instance.input.newKeyMapper.getKeyStrokes(event));
				}
			}
		} else {
			super.handleKeyboardInput();
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

	private static class DummyComponent extends Component {
		private static final long serialVersionUID = 1L;
		public static final DummyComponent INSTANCE = new DummyComponent();
	}
}
