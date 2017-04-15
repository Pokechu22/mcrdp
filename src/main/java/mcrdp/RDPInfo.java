package mcrdp;

import net.minecraft.util.math.BlockPos;

public class RDPInfo {
	public RDPInfo(BlockPos pos, String[] lines) throws InvalidRDPException {
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
		this.pos = pos;
	}

	public final BlockPos pos;
	public final int width, height;
}
