package mcrdp;

import static org.lwjgl.opengl.GL11.*;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.input.Cursor;

import net.minecraft.client.Minecraft;
import net.propero.rdp.ConnectionException;
import net.propero.rdp.DisconnectInfo;
import net.propero.rdp.DisconnectInfo.Reason;
import net.propero.rdp.Input;
import net.propero.rdp.Options;
import net.propero.rdp.OrderSurface;
import net.propero.rdp.Rdesktop;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.Version;
import net.propero.rdp.api.InitState;
import net.propero.rdp.api.RdesktopCallback;
import net.propero.rdp.keymapping.KeyCode_FileBased;
import net.propero.rdp.rdp5.VChannels;

/** A connection through RDP */
public class RDPInstance implements RdesktopCallback {
	public final String server;
	private final String username;
	private final String password;
	public int width, height;
	private final Logger LOGGER;
	public final int glId = glGenTextures();
	/**
	 * The cursor currently being used.  Null indicates system cursor
	 */
	@Nullable
	private Cursor cursor = null;

	// XXX this shouldn't need to be exposed
	public Input input;

	public RDPInstance(String server, String username, String password, int width, int height) {
		if (server.equalsIgnoreCase("localhost")) {
			// TODO: Is this even needed?
			server = "127.0.0.1";
		}

		this.server = server;
		this.username = username;
		this.password = password;
		this.width = width;
		this.height = height;
		LOGGER = LogManager.getLogger("MCRDP - " + server);
	}

	/**
	 * Constructs an instance and starts a new thread for it. The instance will
	 * not necessarilly be fully connected by the time this returns.
	 */
	public static RDPInstance create(String server, @Nullable String username, @Nullable String password, int width, int height) {
		RDPInstance instance = new RDPInstance(server, username, password, width, height);
		Thread thread = new Thread(instance::connect, "RDP thread - " + server);
		thread.setUncaughtExceptionHandler((t, e) -> instance.LOGGER.fatal("Unhandled exception in " + t.getName(), e));
		thread.start();
		return instance;
	}

	/**
	 * Connects to the server, and maintains the connection.  This method doesn't return unless an error occurs!
	 *
	 * From {@link Rdesktop#main(String[])}.
	 */
	private void connect() {
		// String mapFile = "en-us";
		int logonflags = Rdp.RDP_LOGON_NORMAL;

		Options options = new Options();

		options.username = username;
		options.password = password;
		options.width = width;
		options.height = height;

		// ... skip option parsing ...

		LOGGER.info("properJavaRDP version " + Version.version);

		String java = System.getProperty("java.specification.version");
		LOGGER.info("Java version is " + java);

		String os = System.getProperty("os.name");
		String osvers = System.getProperty("os.version");

		if (os.equals("Windows 2000") || os.equals("Windows XP")) {
			options.built_in_licence = true;
		}

		LOGGER.info("Operating System is " + os + " version " + osvers);

		if (os.startsWith("Linux")) {
			options.os = Options.OS.LINUX;
		} else if (os.startsWith("Windows")) {
			options.os = Options.OS.WINDOWS;
		} else if (os.startsWith("Mac")) {
			options.os = Options.OS.MAC;
		}

		if (options.os == Options.OS.MAC) {
			options.caps_sends_up_and_down = false;
		}

		Rdp RdpLayer;

		// Configure a keyboard layout
		/*KeyCode_FileBased keyMap;
		try {
			// logger.info("looking for: " + "/" + keyMapPath + mapFile);
			InputStream istr = Rdesktop.class.getResourceAsStream("/"
					+ keyMapPath + mapFile);
			// logger.info("istr = " + istr);
			if (istr == null) {
				LOGGER.debug("Loading keymap from filename");
				keyMap = new KeyCode_FileBased(options, keyMapPath + mapFile);
			} else {
				LOGGER.debug("Loading keymap from InputStream");
				keyMap = new KeyCode_FileBased(options, istr);
			}
			if (istr != null) {
				istr.close();
			}
			options.keylayout = keyMap.getMapCode();
		} catch (Exception kmEx) {
			LOGGER.warn("Unexpected keymap exception: ", kmEx);
			String[] msg = { (kmEx.getClass() + ": " + kmEx.getMessage()) };
			window.showErrorDialog(msg);
			Rdesktop.exit(0, null, null, true);
			return;
		}

		LOGGER.debug("Registering keyboard...");
		window.registerKeyboard(keyMap);*/

		LOGGER.debug("Initialising RDP layer...");
		RdpLayer = new Rdp(options);
		LOGGER.debug("Registering drawing surface...");
		RdpLayer.registerDrawingSurface(this);
		LOGGER.debug("Registering comms layer...");
		input = new Input(options, RdpLayer, (KeyCode_FileBased) null); // XXX HACK should be sent from RDP to this
		LOGGER.info("Connecting to " + server + ":" + options.port
				+ " ...");

		// Attempt to connect to server on port options.port
		try {
			RdpLayer.connect(options.username, InetAddress
					.getByName(server), logonflags, options.domain,
					options.password, options.command,
					options.directory);

			LOGGER.info("Connection successful");
			// now show window after licence negotiation
			DisconnectInfo info = RdpLayer.mainLoop();

			LOGGER.info("Disconnect: " + info);

			if (info.wasCleanDisconnect()) {
				/* clean disconnect */
				Rdesktop.exit(0, RdpLayer, null, true);
				// return 0;
			} else {
				if (info.getReason() == Reason.RPC_INITIATED_DISCONNECT
						|| info.getReason() == Reason.RPC_INITIATED_DISCONNECT) {
					/*
					 * not so clean disconnect, but nothing to worry
					 * about
					 */
					Rdesktop.exit(0, RdpLayer, null, true);
					// return 0;
				} else {
					String reason = info.toString();
					String msg[] = { "Connection terminated",
							reason };
					//window.showErrorDialog(msg);
					LOGGER.warn("Connection terminated: " + reason);
					Rdesktop.exit(0, RdpLayer, null, true);
				}

			}

			if (RdpLayer.getState() != InitState.READY_TO_SEND) {
				// maybe the licence server was having a comms
				// problem, retry?
				String msg1 = "The terminal server disconnected before licence negotiation completed.";
				String msg2 = "Possible cause: terminal server could not issue a licence.";
				String[] msg = { msg1, msg2 };
				LOGGER.warn(msg1);
				LOGGER.warn(msg2);
				//window.showErrorDialog(msg);
			}
		} catch (ConnectionException e) {
			LOGGER.warn("Connection exception", e);
			String msg[] = { "Connection Exception", e.getMessage() };
			//window.showErrorDialog(msg);
			Rdesktop.exit(0, RdpLayer, null, true);
		} catch (UnknownHostException e) {
			LOGGER.warn("Unknown host exception", e);
			Rdesktop.error(e, RdpLayer, null, true);
		} catch (SocketException s) {
			LOGGER.warn("Socket exception", s);
			if (RdpLayer.isConnected()) {
				LOGGER.fatal(s.getClass().getName() + " "
						+ s.getMessage());
				Rdesktop.error(s, RdpLayer, null, true);
				Rdesktop.exit(0, RdpLayer, null, true);
			}
		} catch (RdesktopException e) {
			String msg1 = e.getClass().getName();
			String msg2 = e.getMessage();
			LOGGER.fatal(msg1 + ": " + msg2, e);

			if (RdpLayer.getState() != InitState.READY_TO_SEND) {
				// maybe the licence server was having a comms
				// problem, retry?
				String msg[] = {
						"The terminal server reset connection before licence negotiation completed.",
						"Possible cause: terminal server could not connect to licence server." };
				LOGGER.warn(msg1);
				LOGGER.warn(msg2);
				//window.showErrorDialog(msg);
				Rdesktop.exit(0, RdpLayer, null, true);
			} else {
				String msg[] = { e.getMessage() };
				//window.showErrorDialog(msg);
				Rdesktop.exit(0, RdpLayer, null, true);
			}
		} catch (Exception e) {
			LOGGER.warn("Other unhandled exception: " + e.getClass().getName() + " " + e.getMessage(), e);
			Rdesktop.error(e, RdpLayer, null, true);
		}
		Rdesktop.exit(0, RdpLayer, null, true);
	}

	@Nullable
	public OrderSurface canvas;
	@Nullable
	private InitState state;

	private ByteBuffer makeBuf(int[] pixels) {
		ByteBuffer buffer = BufferUtils.createByteBuffer(pixels.length * 4);
	
		for (int pixel : pixels) {
			buffer.put((byte) ((pixel >> 16) & 0xFF));  // Red
			buffer.put((byte) ((pixel >> 8) & 0xFF));   // Green
			buffer.put((byte) (pixel & 0xFF));          // Blue
			buffer.put((byte) ((pixel >> 24) & 0xFF));  // Alpha
		}
	
		buffer.flip();

		return buffer;
	}

	@Override
	public void error(Exception ex, Rdp rdp) {
		LOGGER.warn("Ex!", ex);
	}

	@Override
	public void markDirty(int x, int y, int width, int height) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			glBindTexture(GL_TEXTURE_2D, this.glId);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	
			int[] pixels = canvas.getImage(x, y, width, height);
			glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, width, height, GL_RGBA, GL_UNSIGNED_BYTE, makeBuf(pixels));
		});
	}

	@Override
	public void movePointer(int x, int y) {

	}

	@Override
	public void registerSurface(OrderSurface canvas) {
		this.canvas = canvas;

		Minecraft.getMinecraft().addScheduledTask(() -> {
			glBindTexture(GL_TEXTURE_2D, this.glId);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	
			int width = canvas.getWidth();
			int height = canvas.getHeight();
			int[] pixels = canvas.getImage(0, 0, width, height);
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, makeBuf(pixels));
		});
	}

	@Override
	public Cursor createCursor(int hotspotX, int hotspotY, int width, int height, byte[] andmask,
			byte[] xormask) {
		// Implementation is the same as RdesktopFrame.createCursor except that andIndex
		// and xorIndex are not flipped
		final int size = width * height;
		final int scanline = width / 8;
		boolean[] mask = new boolean[size];
		int[] cursor = new int[size];

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < scanline; x++) {
				int andIndex = y * scanline + x;

				for (int bit = 0; bit < 8; bit++) {
					int maskIndex = ((y * scanline) + x) * 8 + bit;
					int bitmask = 0x80 >> bit;

					if ((andmask[andIndex] & bitmask) != 0) {
						mask[maskIndex] = true;
					} else {
						mask[maskIndex] = false;
					}
				}
			}
		}

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int xorIndex = (y * width + x) * 3;
				int cursorIndex = y * width + x;
				cursor[cursorIndex] = ((xormask[xorIndex + 2] << 16) & 0x00ff0000)
						| ((xormask[xorIndex + 1] << 8) & 0x0000ff00)
						| (xormask[xorIndex] & 0x000000ff);
			}

		}

		for (int i = 0; i < size; i++) {
			if ((mask[i] == true) && (cursor[i] != 0)) {
				cursor[i] = ~(cursor[i]);
				cursor[i] |= 0xff000000;
			} else if ((mask[i] == false) || (cursor[i] != 0)) {
				cursor[i] |= 0xff000000;
			}
		}

		IntBuffer buffer = BufferUtils.createIntBuffer(cursor.length);
		for (int pixel : cursor) {
			buffer.put(pixel);
		}

		buffer.flip();

		try {
			return new Cursor(width, height, hotspotX, hotspotY, 1, buffer, null);
		} catch (LWJGLException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setCursor(Object cursor) {
		assert cursor instanceof Cursor : "Unexpected object " + cursor + " (" + (cursor != null ? cursor.getClass() : null) + ")";
		this.cursor = (Cursor) cursor;
	}

	public Cursor getCursor() {
		return this.cursor;
	}

	@Override
	public void sizeChanged(int width, int height) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			glBindTexture(GL_TEXTURE_2D, this.glId);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
			int[] pixels = new int[width * height]; // TODO
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, makeBuf(pixels));
		});

		this.width = width;
		this.height = height;
		LOGGER.info("Resized!  Size is now {} by {}", width, height);
	}

	@Override
	public void registerChannels(VChannels channels) {
		// TODO: clipchannel
	}

	@Override
	public void stateChanged(InitState state) {
		LOGGER.info("State is now {}", state);
		this.state = state;
	}

	@Nullable
	public InitState getState() {
		return state;
	}
}
