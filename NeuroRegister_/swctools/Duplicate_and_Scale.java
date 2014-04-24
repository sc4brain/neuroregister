package swctools;

import ij.plugin.PlugIn;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.process.ImageProcessor;
 
/** Duplicate and scale the current image. */
public class Duplicate_and_Scale implements PlugIn {
 
    /** Ask for parameters and then execute.*/
    public void run(String arg) {
        // 1 - Obtain the currently active image:
        ImagePlus imp = IJ.getImage();
        if (null == imp) return;
 
        // 2 - Ask for parameters:
        GenericDialog gd = new GenericDialog("Scale");
        gd.addNumericField("Image Size : ", 512, 0, 5, "pixel");
        gd.addNumericField("Image Height : ", 512, 0, 5, "pixel");
        gd.addNumericField("Image Depth : ", 256, 0, 5, "pixel");

        gd.addNumericField("Pixel Width : ", 2.0, 2, 4, "um");
        gd.addNumericField("Pixel Height : ", 2.0, 2, 4, "um");
        gd.addNumericField("Pixel Depth : ", 2.0, 2, 4, "um");


        //gd.addStringField("name:", imp.getTitle());
        gd.showDialog();
        if (gd.wasCanceled()) return;
 
        // 3 - Retrieve parameters from the dialog
        int width = (int)gd.getNextNumber();
        int height = (int)gd.getNextNumber();
	/*
        String name = gd.getNextString();
 
        // 4 - Execute!
        Object[] result = exec(imp, name, width, height);
 
        // 5 - If all went well, show the image:
        if (null != result) {
            ImagePlus scaled = (ImagePlus) result[1];
            scaled.show();
        }
	*/
    }
 
    /**
     * Execute the plugin functionality: duplicate and scale the given image.
     * @return an Object[] array with the name and the scaled ImagePlus.
     * Does NOT show the new, scaled image; just returns it.
     */
    public Object[] exec(ImagePlus imp, String newName, int width, int height) {
        // 0 - Check validity of parameters
        if (null == imp) return null;
        if (width <= 0 || height <= 0) return null;
        if (null == newName) newName = imp.getTitle();
 
        // 1 - Perform the duplication and resizing
        ImagePlus scaled = duplicateAndScale(imp, newName, width, height);
 
        // 2 - Return the new name and the scaled image
        return new Object[]{newName, scaled};
    }
 
    /**
     * Execute the plugin functionality: duplicate and scale the given image, without validation.
     * @return the scaled ImagePlus.
     * Does NOT show the new, scaled image; just returns it.
     */
    public static ImagePlus duplicateAndScale(ImagePlus imp, String newName, int width, int height) {
        ImageProcessor ip = imp.getProcessor().resize(width, height);
        return new ImagePlus(newName, ip);
    }
}

