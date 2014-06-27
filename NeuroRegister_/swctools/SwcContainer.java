package swctools;
/*
  TODO
    OK : dialog :
    OK :  image size(x,y,z)
    OK :  image scale(x,y,z)

    OK : swc scale header
    color
    consider diameter 
    OK : draw line
*/


import java.io.*;
import java.util.Vector;
import java.awt.*;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.frame.*;
import ij.text.*;
import ij.io.FileInfo;
import ij.io.OpenDialog;

import ij.measure.*;

import math3d.Point3d;

class SwcHeader{
    public String body;
    public String originalsource;
    public double scale[] = new double[3];
}

class SwcRecord{
    public int index;
    public int type;
    public double pos[] = new double[3];
    public double diam;
    public int parent;
}

public class SwcContainer{
    private String filename;
    private String short_filename;
    private SwcHeader swcheader = new SwcHeader();
    //private String swc_header = "";
    //private double swc_header_scale[] = new double[3];
    //private String swc_body = "";
    
    private int swc_index;
    private int swc_type;
    private double swc_pos[] = new double[3];
    private double swc_diam;
    private int swc_parent;
    //private Vector<Point3d> swc_vec = new Vector<Point3d>(2000, 1000);
    private Vector<SwcRecord> swc_vec = new Vector<SwcRecord>(2000, 1000);

    private FileReader swc_fr=null;
    private BufferedReader swc_br;
    private StreamTokenizer swc_st;

    private int imageWidth, imageHeight, imageDepth;
    private double pixSize[] = new double[3];
    private String[] drawmode_label = {"Line", "Diam", "Linear", "Line2"};
    private int drawmode;
    private boolean loadOk;
    
    public SwcContainer()
    {
		this.init();
		OpenDialog dialog = new OpenDialog("Open SWC file", null);
		if(dialog.getDirectory()==null){ 
			IJ.showMessage("Error : file open.");
			return;
		}
		//file = new File(dialog.getDirectory(), dialog.getFileName());
		short_filename = dialog.getFileName();
		filename = dialog.getDirectory()+short_filename;
	
		this.setupDialog();

		if(this.open()==true){
			loadOk = true;
		}

    }

    public SwcContainer(String name)
    {
		this.init();
		filename = name;
		this.setupDialog();
		this.open();
    }

    public SwcContainer(int width, int height, int depth, double x_scale, double y_scale, double z_scale)
    {
		imageWidth = width;
		imageHeight = height;
		imageDepth = depth;

		pixSize[0] = x_scale;
		pixSize[1] = y_scale;
		pixSize[2] = z_scale;
		swcheader.scale[0] = swcheader.scale[1] = swcheader.scale[2] = 1.0;
		this.open();
    }

    public boolean loadCheck()
    {
		return(loadOk);
    }

    public void setupDialog()
    {
        GenericDialog gd = new GenericDialog("Scale");

        gd.addNumericField("ImageWidth : ", imageWidth, 0, 5, "pixel");
        gd.addNumericField("ImageHeight : ", imageHeight, 0, 5, "pixel");
        gd.addNumericField("ImageDepth : ", imageDepth, 0, 5, "pixel");

        gd.addNumericField("PixelWidth : ", pixSize[0], 2, 4, "um");
        gd.addNumericField("PixelHeight : ", pixSize[1], 2, 4, "um");
        gd.addNumericField("PixelDepth : ", pixSize[2], 2, 4, "um");

		gd.addChoice("DrawMode : ", drawmode_label, "Line");

        gd.showDialog();
        if (gd.wasCanceled()) return;

        imageWidth = (int)gd.getNextNumber();
        imageHeight = (int)gd.getNextNumber();
        imageDepth = (int)gd.getNextNumber();

        pixSize[0] = (double)gd.getNextNumber();
        pixSize[1] = (double)gd.getNextNumber();
        pixSize[2] = (double)gd.getNextNumber();

		Vector choices = gd.getChoices();
		Choice choice = (Choice)choices.elementAt(0);
		String item = choice.getSelectedItem();

		drawmode=0;
		/*
		  if(item.compareTo(drawmode_label[0])){
		  drawmode=0;
		  }else if(item.compareTo(drawmode_label[1])){
		  drawmode=1;
		  }else if(item.compareTo(drawmode_label[2])){
		  drawmode=2;
		  }
		*/

		System.out.println("(DEBUG) drawmode="+item);
    }

    public void init()
    {
		swcheader.scale[0] = swcheader.scale[1] = swcheader.scale[2] = 1.0;

		imageWidth = imageHeight = 1024;
		imageDepth = 512;

		pixSize[0] = pixSize[1] = pixSize[2] = 1.0;
		loadOk = false;
    }


    public boolean open()
    {
		File file;
		boolean readHeader = false;
		String swc_buf;
		try{
			if(swc_fr == null){
				//OpenDialog dialog = new OpenDialog("Open SWC file", null);
				//if(dialog.getDirectory()==null){ return(false); }
				file = new File(filename);
				swc_fr = new FileReader(file);
				swc_br = new BufferedReader(swc_fr);
				//swc_st = new StreamTokenizer(swc_br);
			}
			while((swc_buf = swc_br.readLine()) != null){
				if(swc_buf.length()>=1){
					if(swc_buf.charAt(0)!='1'){
						if(this.parseHeader(swc_buf)==false){
							IJ.error("Error : Wrong Header format.");
							return(false);
						}
						swcheader.body += swc_buf + "\n";
					}else{
						SwcRecord swcrecord0, swcrecord;
						swcrecord0 = new SwcRecord();
						swcrecord = this.parse(swc_buf);
						swc_vec.add(swcrecord0);
						swc_vec.add(swcrecord);
						readHeader=true;
						break;
					}
				}
			}
	    
		}catch(Exception e){
			IJ.showMessage("Error", "file not found.");
			System.out.println("Exception: "+ e);
			return(false);
		}
		if(readHeader==true){
			return(true);
		}else{
			return(false);
		}
    }

    public void loadAll()
    {
		SwcRecord swcrecord;
		while( (swcrecord = read())!=null){	    
			swc_vec.add(swcrecord);
		}

    }

    public void printInfo()
    {
		System.out.println("SWC Info");
		System.out.println(" * FILENAME : "+filename);
		System.out.println(" * num_nodes : "+(swc_vec.size()-1));
		System.out.println(" * Original Source : "+ swcheader.originalsource);
		System.out.println(" * SWC Scale : "+swcheader.scale[0]+", "+swcheader.scale[1]+", "+swcheader.scale[2]);
		System.out.println("");
		System.out.println("Image Info");
		System.out.println(" * Image Size [px] : "+imageWidth+" x "+imageHeight+" x "+imageDepth);
		System.out.println(" * Pixcel Size [um] : "+pixSize[0]+" x "+pixSize[1]+" x "+pixSize[2]);
		System.out.println(" * Draw Mode : line");
	
    }

    public void printRecord()
    {
		SwcRecord swcrecord;
		for(int i=0; i<swc_vec.size(); i++){
			swcrecord = swc_vec.elementAt(i);
			System.out.println(swcrecord.index + ", "+ swcrecord.type
							   + ", " + swcrecord.pos[0]
							   + ", " + swcrecord.pos[1]
							   + ", " + swcrecord.pos[2]
							   + ", " + swcrecord.diam
							   + ", " + swcrecord.parent
							   );


		}
    }

    private boolean parseHeader(String buf)
    {
		int start=7, end;
		try{
			if(buf.startsWith("#ORIGINAL_SOURCE")==true){
				swcheader.originalsource = buf.substring(17, buf.length());
			}else if(buf.startsWith("#SCALE ")==true){
				end = buf.indexOf(' ', start);
				//System.out.println("start="+start+"  end="+end);
				swcheader.scale[0] = Double.parseDouble(buf.substring(start, end));
				start = end+1;
				end = buf.indexOf(' ', start);
				swcheader.scale[1] = Double.parseDouble(buf.substring(start, end));
				start = end+1;
				//end = buf.indexOf(' ', start);
				swcheader.scale[2] = Double.parseDouble(buf.substring(start, buf.length()));
				//System.out.println("SCALE : "+swcheader.scale[0]+", "+swcheader.scale[1]+", "+swcheader.scale[2]);
			}else if(false){
				;
			}else{
				;
			}
		}catch(Exception e){
			System.out.println("Exception: "+ e);
			return(false);
		}

		return(true);
    }

    private SwcRecord parse(String buf)
    {
		int start=0, end;
		SwcRecord swcrecord = new SwcRecord();
		end = buf.indexOf(' ', start);
		swcrecord.index = Integer.parseInt(buf.substring(start, end));
		start = end+1;
		end = buf.indexOf(' ', start);
		swcrecord.type = Integer.parseInt(buf.substring(start, end));
		start = end+1;
		end = buf.indexOf(' ', start);
		swcrecord.pos[0] = Double.parseDouble(buf.substring(start, end))*swcheader.scale[0];
		start = end+1;
		end = buf.indexOf(' ', start);
		swcrecord.pos[1] = Double.parseDouble(buf.substring(start, end))*swcheader.scale[1];
		start = end+1;
		end = buf.indexOf(' ', start);
		swcrecord.pos[2] = Double.parseDouble(buf.substring(start, end))*swcheader.scale[2];
		start = end+1;
		end = buf.indexOf(' ', start);
		swcrecord.diam = Double.parseDouble(buf.substring(start, end));
		start = end+1;
		end = buf.length();
		swcrecord.parent = Integer.parseInt(buf.substring(start, end));
		return(swcrecord);
    }

    public SwcRecord read()
    {
		SwcRecord swcrecord;
		String swc_buf;
		try{
			if((swc_buf = swc_br.readLine()) != null){
				swcrecord = this.parse(swc_buf);
				return(swcrecord);
			}else{
				return(null);
			}
		}catch(Exception e){
			System.out.println("Exception: "+ e);
		}
		return(null);
    }

    public void showPoint()
    {
		ImageStack newStack = new ImageStack( imageWidth, imageHeight );
		SwcRecord swcrecord;
		int outside=0;

		for( int z = 0; z < imageDepth; z++ ) {
			byte [] pixels = new byte[ imageWidth * imageHeight ];

			for(int j=1; j<swc_vec.size(); j++){
				swcrecord = swc_vec.elementAt(j);
				if((double)z <= swcrecord.pos[2] && (double)(z+1) > swcrecord.pos[2]){
					if(swcrecord.pos[1] < imageHeight 
					   && swcrecord.pos[0] < imageWidth){
						pixels[(int)(swcrecord.pos[1])*imageWidth
							   +(int)(swcrecord.pos[0])] = (byte)255;
					}else{
						outside++;
					}
				}
			}
			ByteProcessor bp = new ByteProcessor( imageWidth, imageHeight );
			bp.setPixels( pixels );
			newStack.addSlice( "", bp );

		}
		ImagePlus swcimage = new ImagePlus( "swcimage", newStack );
		swcimage.show();
		System.out.println("outside_point = "+outside);

    }

    public void show()
    {
		ImageStack newStack = new ImageStack( imageWidth, imageHeight );
		SwcRecord swcrecord, swcparent;
		int outside=0, plotnum=0;
		double dir[] = new double[3];
		double pos[] = new double[3];
		double dist[] = new double[3];
		int start_pos[] = new int[3];
		int end_pos[] = new int[3];
		double length;

		boolean pixels[][] = new boolean[imageDepth][imageWidth*imageHeight];

		if(drawmode==0){
			for(int j=2; j<swc_vec.size(); j++){
				swcrecord = swc_vec.elementAt(j);
				swcparent = swc_vec.elementAt(swcrecord.parent);
				if(swcparent.index != swcrecord.parent){
					IJ.error("Error : SWC file has wrong index. ("+swcparent.index+")");
					return;
				}
				//System.out.println("Number of Plot : "+ plotnum);

				pos[0] = swcrecord.pos[0]; pos[1] = swcrecord.pos[1]; pos[2]= swcrecord.pos[2];
				dir[0] = swcparent.pos[0] - pos[0];
				dir[1] = swcparent.pos[1] - pos[1];
				dir[2] = swcparent.pos[2] - pos[2];
				length = Math.sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
				dir[0] = dir[0]/length/2.0*pixSize[0];
				dir[1] = dir[1]/length/2.0*pixSize[1];
				dir[2] = dir[2]/length/2.0*pixSize[2];

				// debug
				//System.out.println("swcrecord="+swcrecord.index + "->"+swcrecord.parent);
				//System.out.println("swcparent="+swcparent.pos[0]);
				//System.out.println("dir[0]="+dir[0]);

				do{
					if(pos[0] > 0.0
					   && pos[1] > 0.0
					   && pos[2] > 0.0
					   && pos[2] < imageDepth*pixSize[2]
					   && pos[1] < imageHeight*pixSize[1]
					   && pos[0] < imageWidth*pixSize[0]){

						pixels[(int)(pos[2]/pixSize[2])]
							[(int)(pos[1]/pixSize[1])*imageWidth
							 +(int)(pos[0]/pixSize[0])] = true;

						plotnum++;
					}else{
						outside++;
					}
					pos[0] += dir[0];
					pos[1] += dir[1];
					pos[2] += dir[2];
				}while((dir[0]>0.0 && pos[0] < swcparent.pos[0])
					   || (dir[0]<0.0 && pos[0] > swcparent.pos[0]));

			}

		}else if(drawmode==1){
			IJ.error("Error : this draw mode is under construction.");
			return;
		}else if(drawmode==2){
			IJ.error("Error : this draw mode is under construction.");
			return;
		}else if(drawmode==3){
			IJ.error("Error : this draw mode is under construction.");
			return;
		}

		System.out.println("(DEBUG) Number of Plot : "+ plotnum);


		for(int j=0; j<imageDepth; j++){
			ByteProcessor bp = new ByteProcessor( imageWidth, imageHeight );

			byte byte_pixels[] = new byte[imageWidth*imageHeight];
			for(int y=0; y<imageHeight; y++){
			    for(int x=0; x<imageWidth; x++){
				byte_pixels[y*imageWidth + x] = (byte)(pixels[j][y*imageWidth+x]?255:0);
			    }
			}
			

			bp.setPixels( byte_pixels );
			newStack.addSlice( "", bp );
		}


		Calibration cal;
		ImagePlus swcimage = new ImagePlus( short_filename, newStack );
		cal = swcimage.getCalibration();

		System.out.println("(DEBUG) " + cal.getUnit() +" \n");
		cal.setUnit("um");
		cal.pixelWidth = pixSize[0];
		cal.pixelHeight = pixSize[1];
		cal.pixelDepth = pixSize[2];

		swcimage.setCalibration(cal);

		swcimage.show();
		if(outside>0){
			System.out.println("Warning : outside_point = "+outside);
		}


	}


	public void close()
	{
		try{
			swc_br.close();
		}catch(Exception e){
			System.out.println("Exception: "+ e);
		}
		return;
	}

}
