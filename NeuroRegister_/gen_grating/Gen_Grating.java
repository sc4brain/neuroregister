package gen_grating;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;


public class Gen_Grating implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		if (IJ.versionLessThan("1.37j"))
			return DONE;
		else	
			return DOES_ALL+DOES_STACKS+SUPPORTS_MASKING;
	}

	public void run(ImageProcessor ip) {
	    Rectangle r = ip.getRoi();
	    int div_x = r.width / 10;
	    int div_y = r.height / 10;
	    
	    for (int y=r.y; y<(r.y+r.height); y++){
		for (int x=r.x; x<(r.x+r.width); x++){
		    if (x % div_x == 0 || y%div_y == 0){
			ip.set(x, y, 255);
		    }
		}
	    }

	}

}
