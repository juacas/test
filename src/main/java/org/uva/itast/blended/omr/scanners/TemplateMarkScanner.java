package org.uva.itast.blended.omr.scanners;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Dimension2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.uva.itast.blended.omr.BufferedImageUtil;
import org.uva.itast.blended.omr.Field;
import org.uva.itast.blended.omr.OMRProcessor;
import org.uva.itast.blended.omr.OMRUtils;
import org.uva.itast.blended.omr.pages.PageImage;
import org.uva.itast.blended.omr.pages.SubImage;

public abstract class TemplateMarkScanner extends MarkScanner
{
	/**
	 * Expansion percentage of the clipped area
	 */
	double EXTRASIZEFACTOR=1.5;
	/**
	 * 
	 */
	private static final double	SIMILARITY_PERCENT	=0.17d*3/4; // experimental adjustment
	
	/**
	 * 
	 */
	private static final int	SCAN_DELTA_DIVISOR	=8;
	/**
	 *  porcentaje del radio para el barrido que se da para buscar la marca
	 */
	private static final double	SCAN_PERCENT	=0.6d;
	/**
	 * Logger for this class
	 */
	protected static final Log	logger	=LogFactory.getLog(TemplateMarkScanner.class);
	public double	markWidth;
	public double	markHeight;
	protected double	approxXscale;
	protected double	approxYscale;
	protected BufferedImage	template;
	double	maxsim;
	int	maxsimX;
	int	maxsimY;
	protected double	autoSimilarity;
	private boolean	dump	=false;

//	public TemplateMarkScanner()
//	{
//		super();
//	}

	public TemplateMarkScanner(OMRProcessor omr,PageImage pageimage,double markWidth,double markHeight,  boolean medianfilter)
	{
		super(omr, pageimage, medianfilter);
		this.approxXscale = pageimage.getHorizontalResolution();
		this.approxYscale = pageimage.getVerticalResolution();
		this.markHeight = markWidth;
		this.markWidth = markHeight;
		Dimension2D dims= pageimage.sizeInPixels(new Size(markWidth, markHeight));
		
		int effectiveWidth=(int) (dims.getWidth() * EXTRASIZEFACTOR) + 1;
		int effectiveHeight=(int) (dims.getHeight() * EXTRASIZEFACTOR) + 1;
		
		if (logger.isTraceEnabled())
		{
			logger.trace("TemplateMarkScanner(PageImage, double, double, boolean) - markWidth : " + markWidth); //$NON-NLS-1$
			logger.trace("TemplateMarkScanner(PageImage, double, double, boolean) - markHeight : " + markHeight); //$NON-NLS-1$
			logger.trace("TemplateMarkScanner(PageImage, double, double, boolean) - dims.x : " + effectiveWidth); //$NON-NLS-1$
			logger.trace("TemplateMarkScanner(PageImage, double, double, boolean) - dims.y : " + effectiveHeight); //$NON-NLS-1$
		}
		
		template = new BufferedImage(effectiveWidth,effectiveHeight,BufferedImage.TYPE_BYTE_BINARY);
		
		this.autoSimilarity=fillTemplate(template, (int) dims.getWidth() , (int) dims.getHeight());
		
	
	}
/**
 * Draws a  template pattern centered in image and calculates the autosimilarity of the template
 * @param template2 {@link BufferedImage} to receive the template
 * @param width in pixels of the feature
 * @param height
 * @return autosimilarity measured as the percentage of whites over the total number of pixels
 */
	protected abstract double fillTemplate(BufferedImage template2, int width, int height);

	/**
	 * When comparing with an empty space there are a minimum similarity due to
	 * empty space around the mark
	 * 
	 * @return
	 */
	private double getAutoSimilarity()
	{
	
		return autoSimilarity;
	}

	

	

	/**
	 * @param markCenter position in milimeters relative to actual page
	 * @param b
	 * @return
	 * @throws MarkScannerException 
	 */
	protected MarkDetectionStatus isMark(Rectangle2D markArea, boolean dump) throws MarkScannerException
	{
		try
		{
			maxsim = -1;
			maxsimX = 0;
			maxsimY = 0;
			
			
			// [JPC] this loop was refactored to start the analysis from the center
			
			
			//Gets a subimage from  x-maxDeltaX-template.getWidth(),y-maxDeltaY ->  x+maxDeltaY,y+maxDeltaY
			// stores the offsetX and offsetY to use original images's coordinates
			markArea=getExpandedArea(markArea);
			
			long start=System.currentTimeMillis();
			SubImage subImage=this.pageImage.getSubimage(markArea, BufferedImage.TYPE_INT_RGB);
			logger.trace("isMark(Point2D, boolean) - Subimage extracted in (ms)=" + (System.currentTimeMillis()-start)); //$NON-NLS-1$
			BufferedImage img=subImage;
			
			//if (logger.isDebugEnabled())
			//	OMRUtils.logSubImage(subImage);
			
			if(medianfilter == true)
			 {
				start=System.currentTimeMillis();
				img= medianFilter(subImage);
				logger.trace("scanAreaForMark(MedianFilter area=" + subImage.getWidth()+"x"+subImage.getHeight() + ") In (ms) "+(System.currentTimeMillis()-start)); //$NON-NLS-1$ //$NON-NLS-2$
				 
				 if (logger.isTraceEnabled())
					 OMRUtils.logSubImage(omr,"debug_median",img);
			 }
			/**
			 * Try XZing binarizer
			 */
//			BufferedImageUtil.binarizeWithZxing(subImage);
			
			float medLum[]=BufferedImageUtil.statsLuminance(subImage, 1);
			float maxLumin=medLum[2];
			float minLumin=medLum[1];
			float medLumin=medLum[0];
			float lumThreshold=maxLumin - (maxLumin-minLumin)/3;
			BufferedImageUtil.threshold(subImage, lumThreshold);
			
			
			  
			if (logger.isDebugEnabled())
			{
				OMRUtils.logSubImage(omr, subImage);
				OMRUtils.logFrame(pageImage,markArea, Color.GRAY,"");
			}
			return sampleAndLoopArea2(markArea, dump, img);
		}
		catch (RasterFormatException e)
		{
			throw new MarkScannerException(e);
		}
	
	}

	/**
	 * @param markArea
	 * @param dump
	 * @param img
	 * @return
	 */
//	private boolean sampleAndLoopArea(Rectangle2D markArea, boolean dump, BufferedImage img)
//	{
//		long start;
//		// Start processing in pixels
//		Point2D markCenter=new Point();
//		markCenter.setLocation(markArea.getCenterX(),markArea.getCenterY());
//		
//		int templateWidth=template.getWidth();
//		int templateHeight=template.getHeight();
//		Point markCenterPx=pageImage.toPixels(markCenter);
//		Rectangle markAreaPx=pageImage.toPixels(markArea);
//
//		double maxDeltaX = markWidth * SCAN_PERCENT;
//		double maxDeltaY = markHeight * SCAN_PERCENT;
//		
//		int maxDeltaXpx = (int) (maxDeltaX*pageImage.getAllignmentInfo().getScaleX());
//		int maxDeltaYpx = (int) (maxDeltaY*pageImage.getAllignmentInfo().getScaleY());
//		
//		int deltaXYpx = (int) (Math.max(1, markWidth / SCAN_DELTA_DIVISOR)*pageImage.getAllignmentInfo().getScaleX());
//		
//		boolean markpoint = true;// for debugging the position of the templates.
//		start=System.currentTimeMillis();
//		int offsetX=markAreaPx.x;
//		int offsetY=markAreaPx.y;
//		
//		for (int xTemplate = markCenterPx.x; xTemplate <= markCenterPx.x + maxDeltaXpx; xTemplate += deltaXYpx)
//		{
//
//			for (int yTemplate = markCenterPx.y; yTemplate <= markCenterPx.y + maxDeltaYpx; yTemplate += deltaXYpx)
//			{
//				int displacementX,displacementY;
//			// one XOR for each quadrant
//				//lower right this is the Reference
//				displacementX=xTemplate;
//				displacementY=yTemplate;
//			double similarity = 1.0 - 
//				BufferedImageUtil.templateXOR(img, 
//						(displacementX  - templateWidth / 2 -offsetX), 
//						(displacementY  - templateHeight / 2 -offsetY), 
//						template, dump);
//				if (markpoint)
//					OMRUtils.markPointInImage(pageImage,displacementX,displacementY);
//				
//				if (maxsim == -1 || maxsim < similarity)
//				{
//					maxsim = similarity;
//					maxsimX = displacementX;
//					maxsimY = displacementY;
//				}
//				
//				// upper left
//				displacementX = 2 *  markCenterPx.x - xTemplate - deltaXYpx;
//				displacementY = 2 *  markCenterPx.y - yTemplate - deltaXYpx;
//				similarity = 1.0 - 
//					BufferedImageUtil.templateXOR(
//						img, 
//						displacementX - templateWidth / 2 -offsetX, 
//						displacementY - templateHeight / 2 -offsetY,
//						template, dump);
//				if (markpoint)
//					OMRUtils.markPointInImage(pageImage,displacementX, displacementY);
//				if (maxsim == -1 || maxsim < similarity)
//				{
//					maxsim = similarity;
//					maxsimX = displacementX;
//					maxsimY = displacementY;
//				}
//				// upper right
//				 displacementX=xTemplate;// +deltaXYpx;
//				 displacementY= 2 *  markCenterPx.y - yTemplate -deltaXYpx;
//				
//				similarity = 1.0 - 
//					BufferedImageUtil.templateXOR(
//							img, 
//							xTemplate - templateWidth / 2 -offsetX,
//							displacementY - templateHeight / 2-offsetY,
//							template, dump);
//				if (markpoint)
//					OMRUtils.markPointInImage(pageImage,displacementX, displacementY);
//				if (maxsim == -1 || maxsim < similarity)
//				{
//					maxsim = similarity;
//					maxsimX = displacementX;
//					maxsimY = displacementY;
//				}
//
//				// bottom left
//				 displacementX=2 *  markCenterPx.x - xTemplate -deltaXYpx;
//				 displacementY= yTemplate;// + deltaXYpx;
//				
//				similarity = 1.0 - BufferedImageUtil.templateXOR(
//						img, 
//						displacementX  - templateWidth / 2 -offsetX,
//						displacementY - templateHeight/ 2-offsetY,
//						template, dump);
//
//				if (markpoint)
//					OMRUtils.markPointInImage(pageImage,displacementX, displacementY);
//				if (maxsim == -1 || maxsim < similarity)
//				{
//					maxsim = similarity;
//					maxsimX = displacementX;
//					maxsimY = displacementY;
//				}
//			}
//		}
//		//TODO apply this condition in the loop 
//		double threshold = getAutoSimilarity() * (1 + SIMILARITY_PERCENT);
//		double doubtThreshold = getAutoSimilarity() * (1 + SIMILARITY_PERCENT/2);
//		if (logger.isDebugEnabled())
//		{
//			logger.debug("isMark-->(ms)"+(System.currentTimeMillis()-start)+" Simil:" + maxsim + " (threshold)" + threshold + ":" + maxsimX + "," + maxsimY + " supposed to be at->" + markCenterPx ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
//		}
//		if (maxsim > threshold)
//			return true;
//		else
//			return false;
//	}
	/**
	 * @param markArea
	 * @param dump
	 * @param img
	 * @return
	 */
	private MarkDetectionStatus sampleAndLoopArea2(Rectangle2D markArea, boolean dump, BufferedImage img)
	{
		long start;
		// Start processing in pixels
		Point2D markCenter=new Point();
		markCenter.setLocation(markArea.getCenterX(),markArea.getCenterY());
		
		int templateWidth=template.getWidth();
		int templateHeight=template.getHeight();
		Point markCenterPx=pageImage.toPixels(markCenter);
		Rectangle markAreaPx=pageImage.toPixels(markArea);

		double maxDeltaX = markWidth * SCAN_PERCENT;
		double maxDeltaY = markHeight * SCAN_PERCENT;
		
		int maxDeltaXpx = (int) (maxDeltaX*pageImage.getAllignmentInfo().getScaleX());
		int maxDeltaYpx = (int) (maxDeltaY*pageImage.getAllignmentInfo().getScaleY());
		
		int deltaXYpx = (int) (Math.max(1, markWidth / SCAN_DELTA_DIVISOR)*pageImage.getAllignmentInfo().getScaleX());
		
		boolean markpoint = true;// for debugging the position of the templates.
		start=System.currentTimeMillis();
		int offsetX=markAreaPx.x;
		int offsetY=markAreaPx.y;
		
		for (int xTemplate = markCenterPx.x- maxDeltaXpx; xTemplate <= markCenterPx.x + maxDeltaXpx; xTemplate += deltaXYpx)
		{

			for (int yTemplate = markCenterPx.y-maxDeltaYpx; yTemplate <= markCenterPx.y + maxDeltaYpx; yTemplate += deltaXYpx)
			{
				int displacementX,displacementY;
		
				displacementX=xTemplate;
				displacementY=yTemplate;
			double similarity = 1.0 - 
				BufferedImageUtil.templateXOR(img, 
						(displacementX  - templateWidth / 2 -offsetX), 
						(displacementY  - templateHeight / 2 -offsetY), 
						template, dump);
				if (logger.isDebugEnabled())
					OMRUtils.markPointInImage(pageImage,displacementX,displacementY);
				
				if (maxsim == -1 || maxsim < similarity)
				{
					maxsim = similarity;
					maxsimX = displacementX;
					maxsimY = displacementY;
				}
			}
		}
		//TODO apply this condition in the loop 
		double threshold = getAutoSimilarity() * (1 + SIMILARITY_PERCENT);
		double doubtThreshold = getAutoSimilarity() * (1 + SIMILARITY_PERCENT*3/4);
		if (logger.isDebugEnabled())
		{
			logger.debug("isMark-->(ms)"+(System.currentTimeMillis()-start)+" Simil:" + maxsim + " (threshold)" + threshold + ":" + maxsimX + "," + maxsimY + " supposed to be at->" + markCenterPx ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		if (maxsim > threshold)
			return MarkDetectionStatus.MARK;
		else
		if (maxsim> doubtThreshold)
			return MarkDetectionStatus.DOUBT_MARK;
		else
			return MarkDetectionStatus.NO_MARK;
	}
	@Override
	protected Rectangle2D getExpandedArea(Rectangle2D rect, double scanPercent)
	{
		Dimension2D templateDim = pageImage.sizeToMilimeters(new Size(template.getWidth(),  template.getHeight()));
		
		double maxDeltaX = markWidth * scanPercent;
		double maxDeltaY = markHeight * scanPercent;
		
		double marginX = maxDeltaX+templateDim.getWidth();
		double marginY = maxDeltaY+templateDim.getHeight();
		double startX = rect.getCenterX()- marginX/2;
		double startY = rect.getCenterY()- marginY/2;
		
		Rectangle2D markArea=new Rectangle2D.Double(startX, startY, marginX, marginY);
		
		return markArea;
	}

	/* (non-Javadoc)
	 * @see org.uva.itast.blended.omr.scanners.MarkScanner#getExpandedArea(java.awt.geom.Rectangle2D)
	 */
	@Override
	protected Rectangle2D getExpandedArea(Rectangle2D coords)
	{
		return getExpandedArea(coords, SCAN_PERCENT);
	}
	@Override
	public String getParsedCode(Field campo) throws MarkScannerException
	{
		
		return ((Boolean)scanField(campo).getResult()).toString();
	}

	@Override
	public ScanResult scanAreaForFieldData(Rectangle2D coords) throws MarkScannerException
	{
		
		MarkDetectionStatus result=isMark(coords, dump);
		ScanResult res=new ScanResult(this.getType(),result);
		return res;
	}
/**
 * 
 * @return String with textual typing
 */
	public abstract String getType();

}