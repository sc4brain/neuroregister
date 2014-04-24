package swctools;

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.frame.*;


public class Swc_Tools implements PlugIn {

    private SwcContainer swc;
    public void run(String arg) {
		//IJ.showMessage("SwcTools","Select SWC file.");
		swc = new SwcContainer();
		if(swc.loadCheck() == false){
			return;
		}
		//swc = new SwcContainer(512, 512, 256, 2.0, 2.0, 2.0);
		//swc.open();
		//swc.printInfo();
		swc.loadAll();
		swc.printInfo();
		//swctool.printRecord();
		swc.show();
	
    }
}


