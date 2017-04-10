package mcrdp;

import net.minecraft.util.math.BlockPos;

public class InvalidRDPException extends Exception {
	private static final long serialVersionUID = -2476292485604881933L;

	public InvalidRDPException(BlockPos pos, String message) {
		super("For RDP sign at " + pos.getX() + "," + pos.getY() + "," + pos.getZ() + ": " + message);
	}
}
