package mcrdp;

import java.awt.Cursor;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import javax.annotation.Nullable;

import net.propero.rdp.ConnectionException;
import net.propero.rdp.DisconnectInfo;
import net.propero.rdp.Input;
import net.propero.rdp.Options;
import net.propero.rdp.OrderSurface;
import net.propero.rdp.Rdesktop;
import net.propero.rdp.RdesktopException;
import net.propero.rdp.Rdp;
import net.propero.rdp.Version;
import net.propero.rdp.DisconnectInfo.Reason;
import net.propero.rdp.api.InitState;
import net.propero.rdp.api.RdesktopCallback;
import net.propero.rdp.keymapping.KeyCode_FileBased;
import net.propero.rdp.rdp5.VChannels;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** A connection through RDP */
public class RDPInstance implements RdesktopCallback {
	public final String server;
	private final String username;
	private final String password;
	public int width, height;
	private final Logger LOGGER;

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
		new Thread(instance::connect, "RDP thread - " + server).start();
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

	@Override
	public void error(Exception ex, Rdp rdp) {
		LOGGER.warn("Ex!", ex);
	}

	@Override
	public void markDirty(int x, int y, int width, int height) {

	}

	@Override
	public void movePointer(int x, int y) {

	}

	@Override
	public void registerSurface(OrderSurface canvas) {
		this.canvas = canvas;
	}

	@Override
	public Cursor createCursor(int x, int y, int w, int h, byte[] andmask,
			byte[] xormask, int cache_idx) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Cursor getCursor() {
		return null;
	}

	@Override
	public void setCursor(Cursor cursor) {
		// TODO Auto-generated method stub
	}

	@Override
	public void sizeChanged(int width, int height) {
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
