package g2c.gerber.contents;

import java.util.LinkedList;
import java.util.List;

public class Macro {
	public List<String>  cmds = new LinkedList<>();

    public void addCmd (String cmd) {
    	cmds.add(cmd);
    }
}