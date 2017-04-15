package mcrdp;

import net.minecraft.util.math.BlockPos;

public class RDPInfo {
	public RDPInfo(BlockPos pos, String[] lines) throws InvalidRDPException {
		if (!lines[0].contains("mcrdp")) {
			throw new InvalidRDPException(pos, "First line '" + lines[0] + "' doesn't contain 'mcrdp'");
		}
		this.pos = pos;
	}

	public final BlockPos pos;
}
