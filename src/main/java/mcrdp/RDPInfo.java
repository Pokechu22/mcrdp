package mcrdp;

import net.minecraft.util.math.BlockPos;

public class RDPInfo {
	public RDPInfo(BlockPos pos, String[] lines) throws InvalidRDPException {
		if (!lines[0].contains("mcrpd")) {
			throw new InvalidRDPException(pos, "First line '" + lines[0] + "' doesn't contain 'mcrdp'");
		}
	}
}
