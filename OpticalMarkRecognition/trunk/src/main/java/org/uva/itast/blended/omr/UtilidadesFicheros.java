/*
 * UtilidadesFicheros.java
 *
 * Creado en Febrero-Mayo de 2009
 *
 */

package org.uva.itast.blended.omr;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Hashtable;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.uva.itast.blended.omr.pages.PageImage;
import org.uva.itast.blended.omr.pages.SubImage;
import org.uva.itast.blended.omr.scanners.BarcodeScanner;
import org.uva.itast.blended.omr.scanners.MarkScannerException;
import org.uva.itast.blended.omr.scanners.ScanResult;
import org.uva.itast.blended.omr.scanners.SolidCircleMarkScanner;

import com.sun.pdfview.PDFFile;

/**
 * @author Juan Pablo de Castro
 * @author Jes�s Rodilana
 */
public class UtilidadesFicheros
{
	/**
	 * Logger for this class
	 */
	private static final Log	logger					= LogFactory
																.getLog(UtilidadesFicheros.class);

	public static final String	USERID_FIELDNAME		= "USERID";
	public static final String	ACTIVITYCODE_FIELDNAME	= "ACTIVITYCODE";
	public static final String	IMAGE_TYPE				= "png";

	/**
	 * M�todo que lee una imagen y la transforma en un objeto de tipo
	 * BufferedImage reescalado
	 * 
	 * @param filename
	 * @param outputdir
	 * @return imagenSalida
	 * @throws IOException
	 */
	
	

	/**
	 * M�todo que salva un objeto tipo imagen en un archivo f�sico de extensi�n
	 * png
	 * 
	 * @param imagen
	 * @param filename
	 * @param imageFormat
	 * @throws IOException
	 */
	public static void salvarImagen(Image imagen, String filename,
			String imageFormat) throws IOException
	{
		File rasterImageFile = new File(filename);
		ImageIO.write((RenderedImage) imagen, imageFormat, rasterImageFile);
	}

	/**
	 * M�todo que a partir de un fichero pdf de entrada devuelve el n�mero su
	 * p�ginas
	 * 
	 * @param inputdir
	 * @return pdffile.getNumpages();
	 * @throws IOException
	 */
	public static int getNumpagesPDF(String inputdir) throws IOException
	{
		RandomAccessFile raf = new RandomAccessFile(inputdir, "r"); // se carga
																	// la imagen
																	// pdf para
																	// leerla
		FileChannel channel = raf.getChannel();
		ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel
				.size());
		PDFFile pdffile = new PDFFile(buf); // se crea un objeto de tipo PDFFile
											// para almacenar las p�ginas
		return pdffile.getNumPages(); // se obtiene el n�mero de paginas
	}



	/**
	 * M�todo que procesa una im�gen dada por el inputpath y que llama a los
	 * m�todos que har�n posible el procesado de los datos que contenga
	 * 
	 * @param inputpath
	 * @param align
	 * @param medianfilter
	 * @param outputdir
	 * @param plantilla
	 * @throws IOException
	 */
	public static void procesarImagenes(PageImage page, boolean align,
			boolean medianfilter, String outputdir, PlantillaOMR plantilla, String acticode, String userid)
			throws IOException
	{
		

			long taskStart = System.currentTimeMillis();
			
			
			processPageAndSaveResults(align, medianfilter,outputdir, plantilla, page, acticode, userid);
			logger.debug("Page  ("+page.getFileName()+") processed in (ms)"+(System.currentTimeMillis()-taskStart)); //$NON-NLS-1$
			
		

	}

	/**
	 * @param inputpath
	 * @param align
	 * @param medianfilter
	 * @param outputdir
	 * @param plantilla
	 * @param pageImage
	 * @throws FileNotFoundException
	 */
	public static void processPageAndSaveResults(boolean align,
			boolean medianfilter, String outputdir, PlantillaOMR plantilla,
			PageImage pageImage, String acticode, String userid) throws FileNotFoundException
	{
		
		procesarPagina(pageImage, align, medianfilter, outputdir, plantilla); // se
																			// procesa
																			// la
																			// p�gina
		
		saveOMRResults(pageImage.getFileName(), outputdir, plantilla, acticode, userid);// se salvan
																	// los
																	// resultados
																	// en
																	// archivo
	}

	/**
	 * M�todo que procesa una p�gina a partir de un BufferedImage invocando a
	 * los m�todos que buscar�n las marcas
	 * 
	 * @param pageImage
	 * @param align
	 * @param outputdir
	 * @param plantilla in/out devuelve los valores reconocidos
	 * @throws FileNotFoundException
	 */
	public static void procesarPagina(PageImage pageImage, boolean align,
			boolean medianfilter, String outputdir, PlantillaOMR plantilla)
			throws FileNotFoundException
	{
		
		if (align)
			{
			
			pageImage.align(); //encapsula procesamiento y representaci�n
			
			}
		
		
		long taskStart = System.currentTimeMillis();
		
		searchMarks(outputdir, plantilla, pageImage, medianfilter); // se
																				// buscan
																				// las
																				// marcas
		logger.debug("\tMarks scanned in (ms)" + (System.currentTimeMillis() - taskStart)); //$NON-NLS-1$
		
	}

	/**
	 * @param grayimage
	 */
	/* TODO: Implementar alineamiento con BufferedImage
	 * 
	private static void alignImage(Gray8Image grayimage)
	{
		ImageManipulation imageManipulator = new ImageManipulation(grayimage); // se
																				// crea
																				// un
																				// objeto
																				// image
																				// que
																				// nos
																				// permita
																				// trabajar
																				// con
																				// la
																				// imagen
		imageManipulator.locateConcentricCircles(); // se alinea si esta marcada
													// la bandera de alineaci�n
	}
*/
	/**
	 * M�todo para buscar las marcas dentro de un objeto tipo Gray8Image
	 * 
	 * @param outputdir
	 * @param plantilla
	 * @param pageImage 
	 * @param imagen
	 * @param medianfilter
	 * @return plantilla
	 * @throws FileNotFoundException
	 */
	public static PlantillaOMR searchMarks(String outputdir,
			PlantillaOMR plantilla,  PageImage pageImage, 
			boolean medianfilter) throws FileNotFoundException
	{
		

		for (int i = 0; i < plantilla.getNumPaginas(); i++)
		{
			// se recorren todas las marcas de una p�gina determinada
			Hashtable<String, Field> campos = plantilla.getPagina(i + 1)
					.getCampos(); // Hastable para almacenar los campos que
									// leemos del fichero de definici�n de
									// marcas
			Collection<Field> campos_val = campos.values();
			for (Field campo : campos_val)
			{
				// vamos a buscar en los campos le�dos, en marcas[] est�n
				// almacenadas las keys
				int tipo = campo.getTipo(); // se almacena el tipo para separar
											// entre si es un barcode o un
											// circle
				if (tipo == Field.CIRCLE)
					buscarMarcaCircle(i, pageImage , campo, medianfilter);
				else if (tipo == Field.CODEBAR)
					{
					buscarMarcaCodebar(pageImage, campo, medianfilter);
					
					}
			}
			
			pageImage.labelPageAsProcessed();
 			
		}
		return plantilla;
	}

	// XXX pasar medianfilter

	/**
	 * M�todo que busca marcas de tipo codebar en un objeto tipo BufferedImage
	 * 
	 * @param pageImage
	 * @param campo
	 * @param medianfilter
	 */
	private static void buscarMarcaCodebar(PageImage pageImage, Field campo,boolean medianFilter)
	{

	
		BarcodeScanner barcodeScanner=new BarcodeScanner(pageImage,medianFilter);
		try
		{
			campo.setValue( barcodeScanner.getParsedCode(campo) );
			barcodeScanner.markBarcode(campo);
		}
		catch (MarkScannerException e)
		{
			campo.setValue(null);
			campo.setValid(false);
		}
		
		//barcodeManipulator.markBarcode(campo);
	}

	/**
	 * M�todo que busca marcas de tipo circle en un objeto tipo Gray8Image
	 * 
	 * @param i
	 *
	 * @param campo 
	 * @param markedImage 
	 * @param mark
	 * @param markedImage
	 * @param campo
	 * @param medianfilter
	 */
	private static void buscarMarcaCircle(int i, 
			PageImage pageImage, Field campo, boolean medianfilter)
	{
		Rectangle2D bbox=campo.getBBox();//milimeters
		Rectangle bboxPx = pageImage.toPixels(bbox);
		// center of the mark
		Point2D center=new Point();
		center.setLocation(bbox.getCenterX(),bbox.getCenterY());
	
		// leemos la anchura de las marcas en mil�metros
		double markWidth = Math.max(1, bbox.getWidth());
		double markHeight = Math.max(1,bbox.getHeight());
		SolidCircleMarkScanner markScanner = new SolidCircleMarkScanner(pageImage,markWidth,markHeight,medianfilter);

		if (logger.isDebugEnabled())
		{
			logger.debug("buscarMarcaCircle - campo=" + campo); //$NON-NLS-1$
		}
		try
		{
			ScanResult res=markScanner.scanField(campo);
			
			if ( (Boolean)res.getResult() ) // se busca la marca que se desea encontrar
			{
				if (logger.isDebugEnabled())
				{
					logger.debug("buscarMarcaCircle - >>>>>>>Found mark at " + bbox + " (mm) :" + campo); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				}
				campo.setValue("true");
													// si se ha encontrado la marca
				markScanner.putCircleMarkOnImage(pageImage);
			} else
			{
				campo.setValue("false");
			}
			campo.setValid(true);
		}
		catch (MarkScannerException e)
		{
			campo.setValid(false);
		}
	}

	/**
	 * M�todo para guardar los resultados del proceso de reconocimiento de
	 * marcas
	 * 
	 * @param outputdir
	 * @param inputpath
	 * @param plantilla
	 * @throws FileNotFoundException
	 */
	public static void saveOMRResults(String inputpath, String outputdir,
			PlantillaOMR plantilla, String acticode, String userid) throws FileNotFoundException
	{
		Hashtable<String, Field> campos = plantilla.getPagina(1).getCampos();
		Field acticodeField = campos.get(acticode);
		Field useridField = campos.get(userid);
		try
		{
			
			
			int useridInt = Integer.parseInt(useridField.getValue()); // evita
																		// inyecci�n
																		// de path
																		// en el
																		// c�digo
			int acticodeInt = Integer.parseInt(acticodeField.getValue()); // evita
																			// inyecci�n
																			// de
																			// path
																			// en el
																			// c�digo

			File dir = new File(outputdir); // que venga de parametro
			File outputFile = new File(dir, "omr_result_" + useridInt + "_"
					+ acticodeInt + ".txt");

			PrintWriter out = new PrintWriter(new FileOutputStream(outputFile,true));
			for (int i = 0; i < plantilla.getNumPaginas(); i++)
			{
				out.println("Filename=" + inputpath);
				out.println("[Page" + plantilla.getPagina(i + 1).getNumPagina()
						+ "]");
				for (int k = 0; k < plantilla.getPagina(i + 1).getMarcas().size(); k++)
				{
					Field campo2 = campos.get(plantilla.getPagina(i + 1)
							.getMarcas().elementAt(k));
					out.println(campo2.getNombre() + "=" + campo2.getValue());
				}
			}
			out.close();
			
		}
		catch (NumberFormatException e)
		{
			// TODO Auto-generated catch block
			logger.error("saveOMRResults:  Can't obtain "+acticode+"="+acticodeField.getValue()+" and "+userid+"="+useridField.getValue()+" for outputting report."); //$NON-NLS-1$
			
		}
		return;
	}

	/**
	 * @param subImage
	 */
	public static void logSubImage(String textId, BufferedImage subImage)
	{
		if (logger.isDebugEnabled()&& true)
		{
			long start=System.currentTimeMillis();
			
			
			
			try
			{
				URL url=UtilidadesFicheros.class.getClassLoader().getResource("Doc1.pdf");
				File testPath=new File(new File(url.toURI()).getParentFile(),"output");
				File imgFile=new File(testPath,"debug_"+textId+System.currentTimeMillis()+".png");
				UtilidadesFicheros.salvarImagen(subImage, imgFile.getAbsolutePath(), "PNG");
				logger.debug("Dumped "+textId+" in (ms) :"+(System.currentTimeMillis()-start));
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			catch (URISyntaxException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
	}
	public static void logSubImage(SubImage subImage)
	{
		
		logSubImage("subimage", subImage);
	}

	/**
	 * @param prefix
	 * @param medianed
	 */
	public static void logSubImage(String prefix, SubImage subImage)
	{
		Rectangle2D markArea=subImage.getBoundingBox();
		logger.debug("Dumped subimage  "+markArea);
		logSubImage(prefix,(BufferedImage)subImage);
		
	}
}
