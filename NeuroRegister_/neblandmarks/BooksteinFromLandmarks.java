/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package neblandmarks;

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.text.*;

import ij.io.FileInfo;
import ij.io.OpenDialog;

import ij.measure.Calibration;

import java.awt.Color;
import java.io.*;

import math3d.Bookstein;
import math3d.Point3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Comparator;
import java.util.Vector;

import vib.FastMatrix;
//import landmarks.NamedPointWorld;
import vib.oldregistration.RegistrationAlgorithm;

import util.OverlayRegistered;

//import swctools.SwcContainer;


public class BooksteinFromLandmarks extends RegistrationAlgorithm {
	double xSpacingTemplate;
	double xSpacingDomain;
	double ySpacingTemplate;
	double ySpacingDomain;
	double zSpacingTemplate;
	double zSpacingDomain;

	int templateWidth;
	int templateHeight;
	int templateDepth;

	int domainWidth;
	int domainHeight;
	int domainDepth;

	Bookstein templateToDomain;
	Bookstein domainToTemplate;

	Calibration templateCalibration;
	Calibration domainCalibration;

	public void generateTransformation( ) {

                NamedPointSet points0 = null;
                NamedPointSet points1 = null;

		try {
			points0 = NamedPointSet.forImage(sourceImages[0]);
		} catch( NamedPointSet.PointsFileException e ) {
                        throw new RuntimeException( "No corresponding .points file found "+
						    "for image: \""+sourceImages[0].getTitle()+"\"" );
                }

		try {
			points1 =  NamedPointSet.forImage(sourceImages[1]);
		} catch( NamedPointSet.PointsFileException e ) {
                        throw new RuntimeException( "No corresponding .points file found "+
						    "for image: \""+sourceImages[1].getTitle()+"\"" );
                }

		generateTransformation( points0, points1 );
	}

	public void generateTransformation( NamedPointSet points0, NamedPointSet points1 ) {

		if( sourceImages == null )
			throw new RuntimeException( "Bookstein_From_Landmarks: The source images must be set before calling generateTransformation()");
		if( sourceImages[0] == null )
			throw new RuntimeException( "Bookstein_From_Landmarks: The template image is null in generateTransformation()");
		if( sourceImages[1] == null )
			throw new RuntimeException( "Bookstein_From_Landmarks: The image to transform is null in generateTransformation()");

                ArrayList<String> commonPointNames = points0.namesSharedWith( points1, true );

                Point3d[] domainPoints=new Point3d[commonPointNames.size()];
                Point3d[] templatePoints=new Point3d[commonPointNames.size()];

                int i_index=0;
                for ( String s : commonPointNames ) {

			for( NamedPointWorld current : points0.pointsWorld ) {
                                if (s.equals(current.getName())) {
                                        Point3d p=new Point3d(current.x,
                                                              current.y,
                                                              current.z);
                                        templatePoints[i_index]=p;
                                        break;
                                }
			}

			for( NamedPointWorld current : points1.pointsWorld ) {
				if (s.equals(current.getName())) {
					Point3d p=new Point3d(current.x,
							      current.y,
							      current.z);
					domainPoints[i_index] = p;
					break;
				}

			}

                        ++i_index;
                }

		templateToDomain = new Bookstein( templatePoints, domainPoints );
		// nebula
		domainToTemplate = new Bookstein( domainPoints, templatePoints );

		ImagePlus template = sourceImages[0];
		ImagePlus domain = sourceImages[1];

		xSpacingTemplate = 1;
		ySpacingTemplate = 1;
		zSpacingTemplate = 1;
		templateCalibration = template.getCalibration();
		if( templateCalibration != null ) {
			xSpacingTemplate = templateCalibration.pixelWidth;
			ySpacingTemplate = templateCalibration.pixelHeight;
			zSpacingTemplate = templateCalibration.pixelDepth;
		}

		xSpacingDomain = 1;
		ySpacingDomain = 1;
		zSpacingDomain = 1;
		domainCalibration = domain.getCalibration();
		if( domainCalibration != null ) {
			xSpacingDomain = domainCalibration.pixelWidth;
			ySpacingDomain = domainCalibration.pixelHeight;
			zSpacingDomain = domainCalibration.pixelDepth;
		}

		templateWidth = template.getWidth();
		templateHeight = template.getHeight();
		templateDepth = template.getStackSize();

		domainWidth = domain.getWidth();
		domainHeight = domain.getHeight();
		domainDepth = domain.getStackSize();

		validateTransformation();
	}

	/* We really don't want to have to construct a new Point3d
	   each time we transform a point; obviously this will all go
	   horribly wrong if you're using this from multiple threads. */

	Point3d p = new Point3d();

	public void transformTemplateToDomainWorld( double x, double y, double z, Point3d result ) {
		if( ! isTransformationValid() )
			throw new RuntimeException( "Trying to use Bookstein_From_Landmarks.transformWorld() with an invalid transformation." );
		p.x = x; p.y = y; p.z = z;
		templateToDomain.apply( p );
		result.x = templateToDomain.x;
		result.y = templateToDomain.y;
		result.z = templateToDomain.z;
	}

	public void transformTemplateToDomain( int x, int y, int z, RegistrationAlgorithm.ImagePoint result ) {
		if( ! isTransformationValid() )
			throw new RuntimeException( "Trying to use Bookstein_From_Landmarks.transform() with an invalid transformation." );

		p.x = x * xSpacingTemplate;
		p.y = y * ySpacingTemplate;
		p.z = z * zSpacingTemplate;

		templateToDomain.apply( p );

		double dxd = templateToDomain.x / xSpacingDomain;
		double dyd = templateToDomain.y / ySpacingDomain;
		double dzd = templateToDomain.z / zSpacingDomain;

		result.x = (int)Math.round( dxd );
		result.y = (int)Math.round( dyd );
		result.z = (int)Math.round( dzd );
	}

	public void transformDomainToTemplateWorld( double x, double y, double z, Point3d result ) {
		if( ! isTransformationValid() )
			throw new RuntimeException( "Trying to use Bookstein_From_Landmarks.transformWorld() with an invalid transformation." );
		p.x = x; p.y = y; p.z = z;
		domainToTemplate.apply( p );
		result.x = domainToTemplate.x;
		result.y = domainToTemplate.y;
		result.z = domainToTemplate.z;
	}

	public void transformDomainToTemplate( int x, int y, int z, RegistrationAlgorithm.ImagePoint result ) {
		if( ! isTransformationValid() )
			throw new RuntimeException( "Trying to use Bookstein_From_Landmarks.transform() with an invalid transformation." );

		p.x = x * xSpacingDomain;
		p.y = y * ySpacingDomain;
		p.z = z * zSpacingDomain;

		domainToTemplate.apply( p );

		double dxd = domainToTemplate.x / xSpacingTemplate;
		double dyd = domainToTemplate.y / ySpacingTemplate;
		double dzd = domainToTemplate.z / zSpacingTemplate;

		result.x = (int)Math.round( dxd );
		result.y = (int)Math.round( dyd );
		result.z = (int)Math.round( dzd );
	}

	public ImagePlus register() {

                NamedPointSet points0 = null;
                NamedPointSet points1 = null;

		try {
			points0 = NamedPointSet.forImage(sourceImages[0]);
		} catch( NamedPointSet.PointsFileException e ) {
                        throw new RuntimeException( "No corresponding .points file found "+
						    "for image: \""+sourceImages[0].getTitle()+"\"" );
                }

		try {
			points1 =  NamedPointSet.forImage(sourceImages[1]);
		} catch( NamedPointSet.PointsFileException e ) {
                        throw new RuntimeException( "No corresponding .points file found "+
						    "for image: \""+sourceImages[1].getTitle()+"\"" );
                }

		return register( points0, points1 );
	}

	public ImagePlus register( NamedPointSet points0, NamedPointSet points1 ) {

		generateTransformation( points0, points1 );
		ImageStack newStack = new ImageStack( templateWidth, templateHeight );

		ImageStack domainStack = sourceImages[1].getStack();

		byte [][] domainPixels = new byte[domainDepth][];
		for( int z = 0; z < domainDepth; ++z )
			domainPixels[z] = ( byte[] ) domainStack.getPixels( z + 1 );

		RegistrationAlgorithm.ImagePoint result = new RegistrationAlgorithm.ImagePoint();

		IJ.showProgress( 0 );

		for( int z = 0; z < templateDepth; ++z ) {

			byte [] pixels = new byte[ templateWidth * templateHeight ];

			for( int y = 0; y < templateHeight; ++y )
				for( int x = 0; x < templateWidth; ++x ) {

					transformTemplateToDomain( x, y, z, result );

					int dx = result.x;
					int dy = result.y;
					int dz = result.z;

					if( dx < 0 || dy < 0 || dz < 0 ||
					    dx >= domainWidth ||
					    dy >= domainHeight ||
					    dz >= domainDepth )
						continue;

					pixels[y*templateWidth+x] =
						domainPixels[dz][dy*domainWidth+dx];
				}

			ByteProcessor bp = new ByteProcessor( templateWidth, templateHeight );
			bp.setPixels( pixels );
			newStack.addSlice( "", bp );

			IJ.showProgress( (z + 1) / (double)templateDepth );
		}

		IJ.showProgress( 1.0 );

		ImagePlus transformed = new ImagePlus( "Transformed", newStack );

		if( templateCalibration != null )
			transformed.setCalibration( templateCalibration );

		return transformed;
	}

	private String swc_header = "";
	private String swc_body = "";
	private String swc_body_inv = "";
	private FileReader swc_fr=null;
	private BufferedReader swc_br;
	private StreamTokenizer swc_st;
	private String swc_buf;
	private double swc_scale[] = new double[3];

	private int swc_index;
	private int swc_type;
	private double swc_pos[] = new double[3];
	private double swc_diam;
	private int swc_parent;
	private Vector<Point3d> swc_vec = new Vector<Point3d>(1000, 1000);

	private boolean openSwc()
	{
		File file;
		boolean readHeader = false;
		swc_scale[0] = swc_scale[1] = swc_scale[2] = 1.0;
		try{
			if(swc_fr == null){
				OpenDialog dialog = new OpenDialog("Open SWC file", null);
				if(dialog.getDirectory()==null){ return(false); }
				file = new File(dialog.getDirectory(), dialog.getFileName());
				swc_fr = new FileReader(file);
				swc_br = new BufferedReader(swc_fr);
				//swc_st = new StreamTokenizer(swc_br);
			}
			while((swc_buf = swc_br.readLine()) != null){
				// read header
				if(swc_buf.length()>=1){
					if(swc_buf.charAt(0)=='1'){
						parseSwc(swc_buf);
						readHeader=true;
						break;
					}else{
						if(swc_buf.startsWith("#SCALE ")==true){
							int start = 7, end=0;
							end = swc_buf.indexOf(' ', start);
							swc_scale[0] = Double.parseDouble(swc_buf.substring(start, end));
							start = end+1;
							end = swc_buf.indexOf(' ', start);
							swc_scale[1] = Double.parseDouble(swc_buf.substring(start, end));
							start = end+1;
							swc_scale[2] = Double.parseDouble(swc_buf.substring(start, swc_buf.length()));
							swc_header += "#SCALE 1.0 1.0 1.0\n";
						}else{
							swc_header += swc_buf + "\n";
						}
					}
				}
			}
			System.out.println("scale=("+swc_scale[0]+", "+swc_scale[1]+", "+swc_scale[2]+")");
			
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

	private void parseSwc(String buf)
	{
		int start=0, end;
		end = buf.indexOf(' ', start);
		swc_index = Integer.parseInt(buf.substring(start, end));
		start = end+1;
		end = buf.indexOf(' ', start);
		swc_type = Integer.parseInt(buf.substring(start, end));
		start = end+1;
		end = buf.indexOf(' ', start);
		swc_pos[0] = Double.parseDouble(buf.substring(start, end)) * swc_scale[0];
		start = end+1;
		end = buf.indexOf(' ', start);
		swc_pos[1] = Double.parseDouble(buf.substring(start, end)) * swc_scale[1];
		start = end+1;
		end = buf.indexOf(' ', start);
		swc_pos[2] = Double.parseDouble(buf.substring(start, end)) * swc_scale[2];
		start = end+1;
		end = buf.indexOf(' ', start);
		swc_diam = Double.parseDouble(buf.substring(start, end));
		start = end+1;
		end = buf.length();
		swc_parent = Integer.parseInt(buf.substring(start, end));
		return;
	}

	private boolean readSwc()
	{
		try{
			if((swc_buf = swc_br.readLine()) != null){
				parseSwc(swc_buf);
				return(true);
			}else{
				return(false);
			}
		}catch(Exception e){
			System.out.println("Exception: "+ e);
		}
		return(false);
	}
	private void closeSwc()
	{
		try{
			swc_br.close();
		}catch(Exception e){
			System.out.println("Exception: "+ e);
		}
		return;
	}

	public ImagePlus register( NamedPointSet points0, NamedPointSet points1, String filename, Boolean withInvert, Boolean overlayResult) 
	{
		generateTransformation( points0, points1 );
		//System.out.println("Width : " + xSpacingDomain + " " + xSpacingTemplate + " " +templateWidth +" "+ xSpacingTemplate*templateWidth);

		// start SWC Translation
		IJ.showMessage("start SWC Transform\n");
		if(openSwc()==true){
			int i=0;
			do{
				i+=1;
				Point3d w_result = new Point3d(255.0, 255.0, 255.0);
				Point3d w_result2 = new Point3d(255.0, 255.0, 255.0);
				transformDomainToTemplateWorld(swc_pos[0], swc_pos[1], swc_pos[2], w_result);
				swc_body += String.valueOf(swc_index) + " "
					+ String.valueOf(swc_type) + " "
					+ String.valueOf(w_result.x) + " "
					+ String.valueOf(w_result.y) + " "
					+ String.valueOf(w_result.z) + " "
					+ String.valueOf(swc_diam) + " "
					+ String.valueOf(swc_parent) + "\n";

				swc_body_inv += String.valueOf(swc_index) + " "
					+ String.valueOf(swc_type) + " "
					+ String.valueOf(templateWidth * xSpacingTemplate - w_result.x) + " "
					+ String.valueOf(w_result.y) + " "
					+ String.valueOf(w_result.z) + " "
					+ String.valueOf(swc_diam) + " "
					+ String.valueOf(swc_parent) + "\n";


				swc_vec.add(w_result);
				IJ.showProgress(i/100.0);
			}while(readSwc()==true);
			closeSwc();
		}



		// start normal process
		//IJ.showMessage("start Image Transform\n");

		ImageStack newStack = new ImageStack( templateWidth, templateHeight );
		ImageStack newStack2 = new ImageStack( templateWidth, templateHeight );
		ImageStack domainStack = sourceImages[1].getStack();
		byte [][] domainPixels = new byte[domainDepth][];
		for( int z = 0; z < domainDepth; ++z )
			domainPixels[z] = ( byte[] ) domainStack.getPixels( z + 1 );
		RegistrationAlgorithm.ImagePoint result = new RegistrationAlgorithm.ImagePoint();
		IJ.showProgress( 0 );
		int counter=0;
		for( int z = 0; z < templateDepth; ++z ) {
			byte [] pixels = new byte[ templateWidth * templateHeight ];
			byte [] pixels2 = new byte[ templateWidth * templateHeight ];
			for( int y = 0; y < templateHeight; ++y ){
				for( int x = 0; x < templateWidth; ++x ) {
					transformTemplateToDomain( x, y, z, result );
					int dx = result.x;
					int dy = result.y;
					int dz = result.z;
					if( dx < 0 || dy < 0 || dz < 0 ||
					    dx >= domainWidth ||
					    dy >= domainHeight ||
					    dz >= domainDepth )
						continue;
					pixels[y*templateWidth+x] =
						domainPixels[dz][dy*domainWidth+dx];
					//pixels[y*templateWidth+x] = (byte)0;
				}
			}
			// nebula
			Point3d tmp_p;
			for(int i=0; i<swc_vec.size(); i++){
				tmp_p = swc_vec.elementAt(i);
				//System.out.println(tmp_p.x + ", "+tmp_p.y+", "+tmp_p.z+" "
				//     +xSpacingTemplate+", "+ySpacingTemplate+", "+zSpacingTemplate);
				//if((int)(tmp_p.z/zSpacingTemplate)==z){
				if((double)z <= tmp_p.z/zSpacingTemplate && (double)(z+1) > tmp_p.z/zSpacingTemplate){
					counter++;
					//System.out.println("("+tmp_p.x+", "+tmp_p.y+", "+tmp_p.z+")");
					//System.out.println("hit :"+ z);
					//tmp_p.y = tmp_p.y / ySpacingTemplate;
					//tmp_p.x = tmp_p.x / xSpacingTemplate;
					//swc_vec.remove(i); i--;
					if(tmp_p.y >= 0 && tmp_p.x >= 0 
					   && tmp_p.y/ySpacingTemplate < templateHeight 
					   && tmp_p.x/xSpacingTemplate < templateWidth){
						pixels2[(int)(tmp_p.y/ySpacingTemplate)*templateWidth
							+(int)(tmp_p.x/xSpacingTemplate)] = (byte)200;
					}
				}
			}
			/*
			  for(int i=0; i<128; i++){
			  pixels[i*templateWidth+i+z]= (byte)255;
			  }
			*/

			ByteProcessor bp = new ByteProcessor( templateWidth, templateHeight );
			bp.setPixels( pixels );
			newStack.addSlice( "", bp );

			ByteProcessor bp2 = new ByteProcessor( templateWidth, templateHeight );
			bp2.setPixels( pixels2 );
			newStack2.addSlice( "", bp2 );
			IJ.showProgress( (z + 1) / (double)templateDepth );
		}
		System.out.println("counter : " +counter);
		IJ.showProgress( 1.0 );

		ImagePlus transformed = new ImagePlus( "Transformed", newStack );
		ImagePlus transformed2 = new ImagePlus( "Transformed2", newStack2 );

		if(overlayResult == true){

			ImagePlus merged = OverlayRegistered.overlayToImagePlus( transformed, transformed2 );
			merged.setTitle( "merge for check" );
			merged.show();
			//transformed = OverlayRegistered.overlayToImagePlus(transformed, transformed2);
		}

		Editor ed = new Editor();
		ed.setSize(500, 500);
		//ed.create("SWC data", swc_vec.toString());
		ed.create("SWC_data", swc_header + swc_body);

		if(withInvert == true){
			Editor ed2 = new Editor();
			ed2.setSize(500, 500);
			ed2.create("SWC_data_Inverted", swc_header + swc_body_inv);
		}


		if( templateCalibration != null )
			transformed.setCalibration( templateCalibration );


		return transformed;
	}

}

