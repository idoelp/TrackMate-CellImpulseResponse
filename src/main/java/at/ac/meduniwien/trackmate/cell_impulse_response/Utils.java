package at.ac.meduniwien.trackmate.cell_impulse_response;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FileUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.service.Service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

import fiji.plugin.trackmate.Model;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.HyperStackConverter;
import ij.plugin.filter.BackgroundSubtracter;
//import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import fiji.plugin.trackmate.features.FeatureFilter;
import fiji.plugin.trackmate.io.TmXmlReader;

@Plugin(type=Service.class)

public class Utils implements Service { 
	public static String getCurrentDate() {
		Date dateNow = new Date();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");	
		return formatter.format(dateNow);
	}
	
	
	public static Model readTmXmlModel(File tmXml) {
		TmXmlReader reader = new TmXmlReader(tmXml);
		if (!reader.isReadingOk()) {
			System.out.println(reader.getErrorMessage());
			return null;
		}
		return reader.getModel();
	}

	/**
	 * Changes the file extension of the original file for a new extension. 
	 * If the original file has no extension, ie no \".\", new extension is just added
	 * @param fileInput original file name
	 * @param newExtension 
	 * @return File object with the new file extension
	 **/
	public static File removeFileNameExtension(File fileInput, String extension) {
		Pattern pattern = Pattern.compile(Pattern.quote(extension));	
		File parent = fileInput.getParentFile();
		String fnPre = fileInput.getName();
		Matcher matcher = pattern.matcher(fnPre);
		String fnPost = matcher.replaceAll("");
		if (parent==null) {
			return new File(fnPost);
		}
		return new File(parent,fnPost);
	}
	
	public static File createOutputFile(File fileInput, String newExtension) {
		String[] fnPre = fileInput.getAbsolutePath().split("[.]");
		ArrayList<String> compositeFn = new ArrayList<>(Arrays.asList(fnPre));
		if(compositeFn.size()>1) {
			compositeFn.remove(compositeFn.size()-1);
		}
		compositeFn.add(newExtension);
		String fn = String.join("", compositeFn );
		return new File(fn);
	}
	/**
	 * Changes the file extension of the original file for a new extension and changes the path to  
	 * If the original file has no extension, ie no *\".\"*, new extension is just added
	 * @param pathOutput new path for the file
	 * @param fileInput original file name
	 * @param newExtension new file extension
	 * @return File object with the new file extension and new path
	 **/
	public static File createOutputFile(File pathOutput, File fileInput, String newExtension) {
		String[] fnPre = fileInput.getName().split("[.]");
		ArrayList<String> compositeFn = new ArrayList<>(Arrays.asList(fnPre));
		if(compositeFn.size()>1) {
			compositeFn.remove(compositeFn.size()-1);
		}
		compositeFn.add(newExtension);
		String fn = String.join("", compositeFn );
		return new File(pathOutput,fn);
	}
	/**
	 * Changes the file extension of the original file for a new extension and changes the path to  
	 * If the original file has no extension, ie no *\".\"*, new extension is just added
	 * @param pathOutput new path for the file
	 * @param fileInput original file name
	 * @param newExtension new file extension
	 * @return File object with the new file extension and new path
	 **/
	public static File createOutputSubfolder(File pathOutput, File fileInput) {
		String[] fnPre = fileInput.getName().split("[.]");
		String subfolder = fnPre[0];
		return new File(pathOutput,subfolder);
	}	

	
	public static TSAnInfo readJsonTSAnInfo ( File jsonFile ) {
		try {
			FileInputStream fli = new FileInputStream(jsonFile.getAbsolutePath());
			BufferedReader bfr = new BufferedReader(new InputStreamReader(fli,Charset.forName("UTF-8")));
			Gson gson = new Gson();
			TSAnInfo info = gson.fromJson(bfr, TSAnInfo.class );
			bfr.close();
			return info;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}	

	public static TSAnInfo readJsonTSAnInfo ( InputStream fli ) {
		try {
			BufferedReader bfr = new BufferedReader(new InputStreamReader(fli,Charset.forName("UTF-8")));
			Gson gson = new Gson();
			TSAnInfo info = gson.fromJson(bfr, TSAnInfo.class );
			bfr.close();
			return info;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}	
	

	public static CondInfo readJsonCondInfo ( File jsonFile ) {
		try {
			FileInputStream fli = new FileInputStream(jsonFile.getAbsolutePath());
			BufferedReader bfr = new BufferedReader(new InputStreamReader(fli,Charset.forName("UTF-8")));
			Gson gson = new Gson();
			CondInfo info = gson.fromJson(bfr, CondInfo.class );
			bfr.close();
			return info;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}	

	public static CondInfo readJsonCondInfo ( InputStream fli ) {
		try {
			BufferedReader bfr = new BufferedReader(new InputStreamReader(fli,Charset.forName("UTF-8")));
			Gson gson = new Gson();
			CondInfo info = gson.fromJson(bfr, CondInfo.class );
			bfr.close();
			return info;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}	
	
	
	public static void writeJsonInfo ( Object info, String pathOutput ) {
		// TODO abstract info class or interface
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(info);
		try {
			FileOutputStream flo = new FileOutputStream(pathOutput);
			BufferedWriter bfw = new BufferedWriter(new OutputStreamWriter (flo,Charset.forName("UTF-8")));
			bfw.write(json);
			bfw.flush();
			bfw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static File[] parseFolder(File path, String fileExtension) {
		if (fileExtension.startsWith(".")) {
			fileExtension = fileExtension.substring(1);
		}
		String regexExtension = (fileExtension==null) ? ".+" : ".+\\."+fileExtension; //in case null, all files read
		Pattern pattern = Pattern.compile(regexExtension);	
		class FileExtensionFilter implements FilenameFilter{
			@Override
			public boolean accept (File dir,String fn) {
				if (fn.startsWith(".")) { // thumbnails off
					return false;
				}
				Matcher matcher = pattern.matcher(fn);
				return matcher.find();
			}
		}
		File[] listFiles = path.listFiles(new FileExtensionFilter());
		return listFiles;
	}
	


	
	public static TSAnInfo.AnalysedFileInfo readImageParams ( File imageFl ){
		try {
			TSAnInfo.AnalysedFileInfo imParams = new TSAnInfo.AnalysedFileInfo();
			ImagePlus image = IJ.openImage(imageFl.getAbsolutePath()); 
			imParams.sizeX = image.getWidth();
			imParams.sizeY = image.getHeight();
			imParams.sizeZ = image.getNSlices();
			imParams.sizeC = image.getNChannels();
			imParams.sizeT = image.getNFrames();
			imParams.calibrationX = image.getCalibration().pixelWidth;
			imParams.calibrationY = image.getCalibration().pixelHeight;
			imParams.calibrationZ = image.getCalibration().pixelDepth;
			imParams.calibrationT = image.getCalibration().frameInterval;
			imParams.unitsX = image.getCalibration().getXUnit();
			imParams.unitsY = image.getCalibration().getYUnit();
			imParams.unitsZ = image.getCalibration().getZUnit();
			imParams.unitsT = image.getCalibration().getTimeUnit();
			imParams.slicesLabels = image.getImageStack().getSliceLabels();
			image.flush();
			image.close();
			return imParams;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return null;
	}

	/**
	 * ImagePlus time-stacks are often read as Z-stacks; this method automatically switches these 2 dimensions.
	 * it checks if there is only one time frame; if so, checks if Z is over 1, which means it is very likely to be the same situation
	 * @param im to be checked and edited
	 * @return image with corrected T dimensions
	 */
	public ImagePlus swapDimensions(ImagePlus im) {
		Integer sl = im.getNSlices(), ch = im.getNChannels(), fr = im.getNFrames();
		//TODO make here a simple dialog YES_NO_CANCEL, with an "apply to all" boolean check
		if(fr<2 && sl >=2) {
			im.setDimensions(ch,fr,sl);
		}
		return im;
	}
	
	public static void dimensionShuffle(ImagePlus imp, String order) {
		HyperStackConverter hsc = new HyperStackConverter();		
		switch(order) {
		case "XYCZT":
			break;
		case "XYCTZ":
			hsc.shuffle(imp,1);
			imp.setDimensions(imp.getNChannels(), imp.getNFrames(), imp.getNSlices());
			break;
		case "XYZCT":
			hsc.shuffle(imp,2);
			imp.setDimensions(imp.getNSlices(), imp.getNChannels(), imp.getNFrames());
			break;
		case "XYZTC":
			hsc.shuffle(imp,3);
			imp.setDimensions(imp.getNSlices(), imp.getNFrames(), imp.getNChannels());
			break;
		case "XYTCZ":
			hsc.shuffle(imp,4);
			imp.setDimensions(imp.getNFrames(), imp.getNChannels(), imp.getNSlices());
			break;
		case "XYTZC":
			hsc.shuffle(imp,5);
			imp.setDimensions(imp.getNFrames(), imp.getNSlices(), imp.getNChannels());
			break;
		default:
			break;
		}
	}
	
	public static ImagePlus backgroundSubtraction(ImagePlus imp, Double rbRad) {
		BackgroundSubtracter bg = new BackgroundSubtracter();
		ImageStack imsOriginal = imp.getImageStack();
		ImageStack ims = imsOriginal.duplicate();
		for (int i = 1; i < (ims.getSize()+1);i++) {
			bg.rollingBallBackground(ims.getProcessor(i), rbRad, false,false,false,false,false);
		}
		
		ImagePlus simpleImp = new ImagePlus(imp.getTitle()+"_bgCor" ,ims);
		simpleImp.setDimensions(imp.getNChannels(),imp.getNSlices(),imp.getNFrames());
		return simpleImp;
	}


	public static ArrayList<Float[]> readCsvObsolete(File currentFile, char delim) {
		try {
			ArrayList<Float[]> fullData = new ArrayList<Float[]>();
			FileReader flr = new FileReader(currentFile.getAbsolutePath());
			CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(delim).withFirstRecordAsHeader();
			Iterable<CSVRecord> records = csvFormat.parse(flr);
			for (CSVRecord record: records) {
				Float[] row = new Float[record.size()];
				for (int i = 0 ; i < record.size(); i++) {
					row[i] = Float.valueOf(record.get(i));
				}
				fullData.add(row);
			}
			flr.close();
			return fullData;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static ArrayList<String[]> readCsv(File currentFile, char delim) {
		try {
			ArrayList<String[]> fullData = new ArrayList<String[]>();
			FileReader flr = new FileReader(currentFile.getAbsolutePath());
			CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(delim).withFirstRecordAsHeader();
			fullData.add(csvFormat.getHeader());
			Iterable<CSVRecord> records = csvFormat.parse(flr);
			for (CSVRecord record: records) {
				String[] row = new String[record.size()];
				for (int i = 0 ; i < record.size(); i++) {
					row[i] = String.valueOf(record.get(i));
				}
				fullData.add(row);
			}
			flr.close();
			return fullData;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}	 

	public static String[] readHeaderCsvObsolete(File currentFile, char delim) {
		try {
			FileReader flr = new FileReader(currentFile.getAbsolutePath());
			CSVFormat csvFormat = CSVFormat.DEFAULT.withDelimiter(delim).withFirstRecordAsHeader();
			CSVParser parser = CSVParser.parse(flr, csvFormat);
			List<String> ls = parser.getHeaderNames();
			String[] header = ls.toArray(new String[ls.size()]);
			flr.close();
			return header;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


	public static void createObsoleteInfoPrimer() {        
        TSAnInfo primer = new TSAnInfo();
        primer.sources.date = "20200723";
        primer.sources.fileExtension = "csv";
        primer.sources.paramsSource = "/Users/iago/Desktop/obsoleteParams.ts.json";
        primer.sources.pathInput = "/Users/iago/Desktop/tba";
        primer.sources.pathOutput = "/Users/iago/Desktop/tba";
        primer.sources.executionMode = "automatic";
        primer.sources.processMode = "batch";
        primer.sources.extent = "obsolete";
        
        primer.obsoleteInfo.channelsAcquired = new String[]{"340_MEAN","340COR_MEAN","340COR_MEDIAN","380_MEAN","380COR_MEAN"};
        primer.obsoleteInfo.availableChannels = new String[] {"340_MEAN","340COR_MEAN","340COR_MEDIAN","380_MEAN","380COR_MEAN","COMP_RATIO_COR_MEAN"};
        primer.obsoleteInfo.targetChannel = "COMP_RATIO_COR_MEAN";
        primer.obsoleteInfo.characteristicTFrames = 12;
        primer.obsoleteInfo.tsSigma = 1d;
        
        TSAnInfo.CompositeChannelInfo compRatio = new TSAnInfo.CompositeChannelInfo();
        compRatio.channelA = "340COR_MEAN";
        compRatio.channelB = "380COR_MEAN";
        compRatio.operation = "Divide";
        compRatio.kA = 100d;
        compRatio.kB = 1d;
        compRatio.offset = 0d;
        compRatio.compCh = "COMP_RATIO_COR_MEAN";

        primer.tsParams.compositeChannelsInfo = new HashMap<String,TSAnInfo.CompositeChannelInfo>();
        primer.tsParams.compositeChannelsInfo.put(compRatio.compCh,compRatio);
        primer.obsoleteInfo.compositeChannelsInfo = new HashMap<String,TSAnInfo.CompositeChannelInfo>();
        primer.obsoleteInfo.compositeChannelsInfo.put(compRatio.compCh,compRatio);
		
        Utils.writeJsonInfo(primer, primer.sources.paramsSource);
	}

	public static void packZip(File pathOutput, File pathOutputSub, String outputFile) throws IOException{
		File[] posFiles = pathOutputSub.listFiles();			
		File fl = new File(pathOutput,outputFile); // zip file output
        FileOutputStream fos = new FileOutputStream(fl); 
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (File fileToZip : posFiles) {
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
            zipOut.putNextEntry(zipEntry);	 
            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();
        fos.close();
		FileUtils.deleteDirectory(pathOutputSub);		
	}
	
	public static void deleteDirectory(File pathDelete) throws IOException {
		FileUtils.deleteDirectory(pathDelete);		
	}
	
	public static InputStream openFileInsideZip (File zipInput, File toExtract) throws IOException{
		ZipFile zipFile = new ZipFile(zipInput);
		Enumeration<? extends ZipEntry> entries = zipFile.entries(); 
	    while(entries.hasMoreElements()){
	        ZipEntry entry = entries.nextElement();
	        if (entry.getName().equals(toExtract.getName())){
	        	InputStream stream = zipFile.getInputStream(entry);
	        	return stream;
	        }
	    }
	    zipFile.close();
	    return null;
			
	}
	
	public static void extractFileInsideZip (File zipInput, File toExtract, File toSave) throws IOException{
		FileOutputStream fos = new FileOutputStream(toSave);
		ZipFile zipFile = new ZipFile(zipInput);
		Enumeration<? extends ZipEntry> entries = zipFile.entries(); 
	    while(entries.hasMoreElements()){
	        ZipEntry entry = entries.nextElement();
	        if (entry.getName().equals(toExtract.getName())){
	        	InputStream stream = zipFile.getInputStream(entry);
	        	byte[] buffer = new byte[1024];
	        	int length;
	        	while ((length = stream.read(buffer)) != -1) {
	        		fos.write(buffer,0,length);
	        	}
	        	fos.close();
	        	stream.close();
	        }		        
	    }
	    zipFile.close();

	}


	@Override
	public Context context() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Context getContext() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public double getPriority() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public void setPriority(double priority) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public PluginInfo<?> getInfo() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public void setInfo(PluginInfo<?> info) {
		// TODO Auto-generated method stub
		
	}

}


// obsolete code, useful for recycling
//public static HashMap <String,HashMap<String, Object >> readJsonParams ( File jsonFile ) {
//try {
//	FileInputStream fli = new FileInputStream(jsonFile.getAbsolutePath());
//	BufferedReader bfr = new BufferedReader(new InputStreamReader(fli,Charset.forName("UTF-8")));
//	Type complexHashMap = new TypeToken<HashMap <String,HashMap<String, Object>>>(){}.getType();
//	Gson gson = new Gson();
//	HashMap<String,HashMap<String,Object>> map = gson.fromJson(bfr,complexHashMap );
//	bfr.close();
//	@SuppressWarnings("unchecked")
//	Map<String,Map<String,Object>> afiMap = (Map<String,Map<String,Object>>) map.get("IMAGE_PARAMS").get("ANALYSED_FILES_INFO");
//	HashMap <String,HashMap<String, Object >> analysedFilesInfo = new HashMap <String,HashMap<String, Object >> ();
//	if (afiMap!=null) {
//		for (String key: afiMap.keySet()) {
//			HashMap<String,Object> subMap = new HashMap<String,Object>();
//			for (String subKey: afiMap.get(key).keySet()) {
//				subMap.put(subKey, afiMap.get(key).get(subKey));
//			}
//			analysedFilesInfo.put(key,subMap);
//		}
//	}
//	map.get("IMAGE_PARAMS").replace("ANALYSED_FILES_INFO",analysedFilesInfo);
//	@SuppressWarnings("unchecked")
//	Map<String,Map<String,Object>> cciMap = (Map<String,Map<String,Object>>) map.get("TS_PARAMS").get("COMPOSITE_CHANNELS_INFO");
//	HashMap <String,HashMap<String, Object >> compositeChannelsInfo = new HashMap <String,HashMap<String, Object >> ();
//	if (cciMap!=null) {
//		for (String key: cciMap.keySet()) {
//			HashMap<String,Object> subMap = new HashMap<String,Object>();
//			for (String subKey: cciMap.get(key).keySet()) {
//				subMap.put(subKey, cciMap.get(key).get(subKey));
//			}
//			compositeChannelsInfo.put(key,subMap);
//		}
//	}
//	map.get("TS_PARAMS").replace("COMPOSITE_CHANNELS_INFO",compositeChannelsInfo);
//	return map;
//} catch (FileNotFoundException e) {
//	e.printStackTrace();
//} catch (IOException e) {
//	// TODO Auto-generated catch block
//	e.printStackTrace();
//}
//return null;
//}

//public static void packZipOld(File pathOutput, File pathOutputSub) {
//	try {
//		ZipUtil.pack(pathOutputSub,new File(pathOutput,pathOutputSub.getName()+".zip"));
//		FileUtils.deleteDirectory(pathOutputSub);
//	} catch (IOException e) {
//		e.printStackTrace();
//	}		
//}