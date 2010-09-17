/*
 * BarcodeManipulation.java
 *
 * Creado en Abril-Mayo de 2009
 *
 */

package org.uva.itast.blended.omr.scanners;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.uva.itast.blended.omr.Field;
import org.uva.itast.blended.omr.UtilidadesFicheros;
import org.uva.itast.blended.omr.pages.PageImage;
import org.uva.itast.blended.omr.pages.SubImage;

import com.google.zxing.MonochromeBitmapSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageMonochromeBitmapSource;
import com.google.zxing.client.result.ParsedResult;
import com.google.zxing.client.result.ResultParser;

/**
 * 
 * @author Juan Pablo de Castro
 * @author Jes�s Rodilana
 *
 */
public final class BarcodeScanner extends MarkScanner
{
	/**
	 * Logger for this class
	 */
	static final Log	logger	= LogFactory.getLog(BarcodeScanner.class);

	static final double	BARCODE_AREA_PERCENT	= 0.5d;

	
	

	private BufferedImage	subimage;

	public BarcodeScanner(PageImage imagen, boolean medianfilter)
	{
		super(imagen,medianfilter);
	}
	/**
	 * 
	 */
	public String getParsedCode(Field campo) throws MarkScannerException
	{
		return getParsedCode(scanField(campo));
	}
	
	/**
	 * @param result
	 * @return
	 */
	public String getParsedCode(ScanResult result)
	{
		String barcode;
		if (result!=null)
		{
		ParsedResult parsedResult = ResultParser.parseResult((Result) result.getResult());
	      
	      //System.out.println(imagen.toString() + " (format: " + result.getBarcodeFormat() +", type: " + parsedResult.getType() + "):\nRaw result:\n" + result.getText() +"\nParsed result:\n" + parsedResult.getDisplayResult());
	    barcode = parsedResult.getDisplayResult();
		}
		else
		barcode=null;
		
		return barcode;
	}
	/**
	 * Generates an expanded boundingbox in milimeters
	 * 
	 * @see {@link #BARCODE_AREA_PERCENT}
	 * @see {@value #BARCODE_AREA_PERCENT}
	 * @param rect
	 * @return milimeteres
	 */
	protected Rectangle2D getExpandedArea(Rectangle2D rect)
	{
		Rectangle expandedRect=new Rectangle();
		expandedRect.setFrame((rect.getX()-rect.getWidth()*(BARCODE_AREA_PERCENT)/2),
						(rect.getY()-rect.getHeight()*(BARCODE_AREA_PERCENT)/2),
						(rect.getWidth()*(1+BARCODE_AREA_PERCENT)),
						(rect.getHeight()*(1+BARCODE_AREA_PERCENT)));
		return expandedRect;
	}
	/**
	 * @param rect area in milimeters to be scanned
	 * @return
	 * @throws ReaderException
	 */
	public ScanResult scanAreaForFieldData(Rectangle2D rect)	throws MarkScannerException
	{
		//[JPC] Need to be TYPE_BYTE_GRAY 
		  // BufferedImageMonochromeBitmapSource seems to work bad with TYPE_BYTERGB
		  
		 SubImage subimage = pageImage.getSubimage(rect, BufferedImage.TYPE_BYTE_GRAY);		//se coge la subimagen, x,y,w,h (en p�xeles)
		 
		 //TODO
			//se coge la subimage
			//BufferedImage subimage = getImagen().getSubimage(rect.x,rect.y,rect.width,rect.height);
			
			File rasterImageFile = new File("C:\\Documents and Settings\\Administrador\\Escritorio\\jj\\cb.png");
	    	try {
				ImageIO.write(subimage,"png", rasterImageFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//TODO
	
		  if (subimage == null)
			{
			  logger.error("leerBarcode(Campo) - " + pageImage.toString() + ": No es posible cargar la imagen", null); //$NON-NLS-1$ //$NON-NLS-2$
			  //TODO: Lanzar otra Excepcion
			  throw new RuntimeException("Can't extract subimage from page.");
			}
		//if (logger.isDebugEnabled())
		//	UtilidadesFicheros.logSubImage(subimage);  
		
	    MonochromeBitmapSource source = new BufferedImageMonochromeBitmapSource(subimage);
	    Result result=null;
		try
		{
			result = new MultiFormatReader().decode(source,null);
		}
		catch (ReaderException e)
		{
			//retry after filtering
		if(medianfilter == true)
			 {
				 UtilidadesFicheros.logSubImage(subimage);
	
				long start=System.currentTimeMillis();
				BufferedImage medianed= medianFilter(subimage);
				logger.debug("scanAreaForBarcode(MedianFilter area=" + subimage.getWidth()+"x"+subimage.getHeight() + ") In (ms) "+(System.currentTimeMillis()-start)); //$NON-NLS-1$ //$NON-NLS-2$
				 
				 if (logger.isDebugEnabled())
					 UtilidadesFicheros.logSubImage("debug_median",medianed);
				 
				 source = new BufferedImageMonochromeBitmapSource(medianed);
				 try
				{
					result = new MultiFormatReader().decode(source, null);
				}
				catch (ReaderException e1)
				{
					throw new MarkScannerException(e);
				}
				
				 //subimage=medianed; // store medianed image for reporting
				
			 }
		else
			throw new MarkScannerException(e); // re-throw exception to notify caller 
		}
		ScanResult scanResult=new ScanResult("Barcode",result);
		return scanResult;
	}

	/**
	 * @param campo
	 */
	public void markBarcode(Field campo)
	{
		//get bbox in pixels
		Rectangle rect=pageImage.toPixels(campo.getBBox());
		// expand the area for some tolerance
		Rectangle2D expandedArea = getExpandedArea(campo.getBBox());
		Rectangle expandedRect = pageImage.toPixels(expandedArea);
		
		Graphics2D g = pageImage.getReportingGraphics();
		AffineTransform t=g.getTransform();
		g.setStroke(new BasicStroke(1,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_ROUND,1,new float[]{(float) (3/t.getScaleX()),(float) (6/t.getScaleY())},0));
		if (lastResult!=null)
			g.setColor(Color.BLUE);
		else 
			g.setColor(Color.RED);
		
		
		g.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 3, 3);
		g.drawRoundRect(expandedRect.x, expandedRect.y, expandedRect.width, expandedRect.height, 3, 3);
		
		
		g.setFont(new Font("Arial",Font.PLAIN,(int) (12/t.getScaleX())));
		if (lastResult!=null)
			g.drawString(((Result)lastResult.getResult()).getBarcodeFormat().toString()+"="+getParsedCode(lastResult), rect.x, rect.y);
		
	}
}