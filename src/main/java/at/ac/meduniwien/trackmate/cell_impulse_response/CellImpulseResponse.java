package at.ac.meduniwien.trackmate.cell_impulse_response;

//import net.imagej.ImageJ;

import net.imagej.Dataset;
import net.imagej.DatasetService;
import org.scijava.log.LogService;
//import io.scif.services.DatasetIOService;
//import net.imagej.ops.OpService;
import org.scijava.plugin.PluginService;
import org.scijava.command.ContextCommand;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.DialogPrompt;
import org.scijava.ui.DialogPrompt.Result;
import org.scijava.ui.UIService;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import ij.IJ;
import ij.ImagePlus;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.ImageIcon;

@Plugin(type = TrackMateActionFactory.class)
public class CellImpulseResponse implements TrackMateActionFactory,ActionListener {

	private Boolean ready = false;

	private SettingsController sc;
	private String fileExtension;	
	private File pathInput;	
	private File pathOutput;

	private File[] listFiles;
	private TSAnInfo info = new TSAnInfo();

	public void validateExistance() {
		if(executionMode.equals("automatic") && !pathToJson.exists()) {			
			cancel(" the *.json file provided is not existing. The plugin is finished");
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == "emptyFolder") {
			cancel(" no files to analyze with the extension: "+TSAnInfo.outputExtensions.get("zip")+"\n restart the plugin");
		} else if (e.getActionCommand() == "quit") {
			cancel(" plugin was quitted ");
		} else if (e.getActionCommand() == "finish"){
			//launchCellIntenTSAn(this.sc.getInfo(),true);
			ready = true;
			this.run();
		} else {
			cancel( "something failed ");
		}
	}
	
	
	@Override
	public void run(){		
		if (executionMode.equals("automatic")) {
			this.info = Utils.readJsonTSAnInfo(pathToJson);
			info.sources.date = Utils.getCurrentDate();
			info.sources.executionMode = executionMode;
			info.sources.extent = extent;
			info.sources.processMode = "batch";
			info.sources.paramsSource = pathToJson.getAbsolutePath();		
			if(pathToJson.exists()) {			
				launchCellIntenTSAn(info,false);
			}
			
		} else if (executionMode.equals("manual") && !ready) { // ugly like hell... but it works i guess
			info.sources.executionMode = executionMode;
			info.sources.extent = extent;
			this.sc = new SettingsController(this, info, pathToJson);
			
		} else if (executionMode.equals("manual") && ready){
			launchCellIntenTSAn(this.sc.getInfo(),true);
			
		} else {
			cancel(" execution mode: invalid choice ");
		}
		
	}
	
	public void launchCellIntenTSAn( TSAnInfo updatedInfo, Boolean isManualMode ) {
		try {
			/* Read files from folder */
			this.info = updatedInfo; //necessary in case of API access
			this.pathInput = new File(info.sources.pathInput);
			this.pathOutput = new File(info.sources.pathOutput);
			if (!pathOutput.exists()) {
				pathOutput.mkdirs();
			}
			this.fileExtension = info.sources.fileExtension;
			if (isManualMode) {
				String[] listFilePaths = info.imageParams.analysedFiles;
				listFiles = new File[listFilePaths.length];
				for (int i = 0; i < listFilePaths.length; i++) {
					listFiles[i] = new File(listFilePaths[i]);
				}
			} else if ( !isManualMode ) {								
				listFiles = Utils.parseFolder(pathInput,fileExtension);
				Utils.validateFolderContent(listFiles, this); // will stop command
				String[] listFilePaths = new String[listFiles.length];
				HashSet<Integer> divChannels = new HashSet<Integer>();
				info.imageParams.analysedFilesInfo = new HashMap<String,TSAnInfo.AnalysedFileInfo>();
				for (int i = 0; i < listFilePaths.length; i++) {
					String fp = listFiles[i].getAbsolutePath();
					if(info.sources.extent.equals("track + tsa")) {
						TSAnInfo.AnalysedFileInfo temp = Utils.readImageParams(listFiles[i]);
						if(temp.sizeT < info.imageParams.minimumT) {
							logService.error(String.format("%s file contains less than %d frames and is excluded from the analysis",fp,temp.sizeT));
						}
						info.imageParams.analysedFilesInfo.put(fp, temp);
						logService.info(String.format("    %s X:%d  Y:%d C:%d  Z:%%d  T:%d", fp,temp.sizeX,temp.sizeY,temp.sizeC, temp.sizeZ, temp.sizeT));
						divChannels.add(temp.sizeC);
						listFilePaths[i] = fp;
					} else if(info.sources.extent.equals("only tsa")) {
						TSAnInfo.TMParams temp = Utils.readTmXmlParams(listFiles[i]);
						if(temp.sizeT < info.imageParams.minimumT) {
							logService.error(String.format("%s file contains less than %d frames and is excluded from the analysis",fp,temp.sizeT));
						}
						TSAnInfo.AnalysedFileInfo dummy = new TSAnInfo.AnalysedFileInfo();
						dummy.fileName = listFiles[i].getName();
						dummy.sizeX = temp.sizeX;
						dummy.sizeY = temp.sizeY;
						dummy.sizeZ = temp.sizeZ;
						dummy.sizeC = temp.sizeC;
						dummy.sizeT = temp.sizeT;
						dummy.path = fp;
						info.imageParams.analysedFilesInfo.put(fp, dummy);
						logService.info(String.format("    %s X:%d  Y:%d C:%d  Z:%%d  T:%d", fp,temp.sizeX,temp.sizeY,temp.sizeC, temp.sizeZ, temp.sizeT));
						divChannels.add(temp.sizeC);
						listFilePaths[i] = fp;
					} else if(info.sources.extent.equals("obsolete")) {
						TSAnInfo.AnalysedFileInfo dummy = new TSAnInfo.AnalysedFileInfo();
						divChannels.add(1);
						listFilePaths[i] = fp;
						dummy.fileName = listFiles[i].getName();
						dummy.path = fp;
						info.imageParams.analysedFilesInfo.put(fp, dummy);
					}
				}
				if (divChannels.size() > 1 ) {
					cancel("the chosen files don't have the same number of channels. Plugin stops.");
				} else if (info.imageParams.analysedFilesInfo.keySet().size() < 1) {
					cancel("none of the indicated files meets the criteria to continue the analysis. Plugin stops");
				}
				info.imageParams.analysedFiles = listFilePaths;
			}
			
			
			logService.info(logService.getName());
			if(info.sources.processMode.equals("manual")) {
				// just a terrible way to forcibly open the fiji console and visualize stdout messages...
				logService.warn("this plugin is in beta distribution. Follow the instructions and be patient\n\n");
			}
			logService.info("Cell Intensity Time-Series Analysis - A TrackMate-run plugin \n");
			logService.info("************************************************************");
			logService.info(String.format("Analysis source:  \"%s\"",pathInput));
			logService.info(String.format("Results saved in folder \"%s\" \n",pathInput));
			logService.info("Files to be analyzed");
			for(File el:listFiles) {
				logService.info("    "+el.getName());
			}
			logService.info("***  Starting file processing  ***  ");
			
			
			for (File currentFile :listFiles){	
				logService.info("    ...processing file:   "+currentFile);
				if (info.sources.extent.equals("track + tsa")) {
					TrackMateAPIAccess tmResult = runTracking(currentFile);
					if (tmResult == null) {
						continue;
					}		
					Model modelResult = tmResult.getModel();
					Settings settingsResult = tmResult.getSettings();
					if (info.sources.displayOverlay) {
						logService.info("   displaying image overlay");
						tmResult.displayImageOverlay(); // careful when running batch, as the image is not closed after running it
					} else {
						tmResult.closeImp();
					}
					runAnalysis(currentFile,modelResult,settingsResult);
				} else if (info.sources.extent.equals("only tsa")) {
					Model modelResult = Utils.readTmXmlModel(currentFile);
					Settings settingsResult = Utils.readTmXmlSettings(currentFile, info);
					runAnalysis(currentFile,modelResult,settingsResult);
				} else if (info.sources.extent.equals("obsolete")) {
					//runAnalysisObsolete(currentFile);
				} else {					
					logService.warn("the \"extent of analysis\" parameter has not a valid option");
					continue;
				}
				/* create and save image of TimeSeries*/
								
			}
			logService.info("***  CellIntenTSAn finished successfully, Pfiat di! *** ");
			/* */
		} catch (final Exception exc) {
			logService.error(exc);
		}
	}		
	
	/*
	 * Auxiliary methods
	 */
	private TrackMateAPIAccess runTracking(File currentImageFile) throws IOException {
		// at this point
		File outputTmXml = Utils.createOutputFile(currentImageFile,TSAnInfo.outputExtensions.get("trackmate-tracks"));
		if (!datasetIOService.canOpen(currentImageFile.getAbsolutePath())) {
			logService.error(String.format("   !!! file   %s   could not be opened, skipped !!!",currentImageFile.getName()));
			return null;
		}
		// debugging gets stuck opening a file with datasetIOService, plus
		// problems convertService cannot convert dataset to ImagePlus (TrackMate demand)
		// Proceed by opening with ImageJ1 ij.IJ.openImage --> ImagePlus, anyway
		// trackmate requires old ImageJ1 ImagePlus; also convenient to shuffle and background subtraction.
		// the next commented stub is for future versions, working through with Dataset and ImgPlus, and not ImagePlus
		ImagePlus imp;
		Dataset currentImage = datasetIOService.open(currentImageFile.getAbsolutePath());
		Boolean check = convertService.supports(currentImage, ImagePlus.class);
		if (!check) {
			logService.error("    Cannot convert given dataset to ImagePlus, proceed with ImageJ1 ");
			imp = IJ.openImage(currentImageFile.getAbsolutePath());
		} else {
			imp = convertService.convert(currentImage, ImagePlus.class);
		}
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		imp.getCalibration().frameInterval = 1d;
		// TODO get metadata
		
		Utils.dimensionShuffle(imp, info.imageParams.dimensionOrder);	
		
		if (imp.getNFrames() < info.tmParams.filterTrackLength || imp.getNFrames() < info.imageParams.minimumT) {
			logService.error(String.format("    !!! file contains only %d frames, minimum required is %d. Analysis skipped !!!",imp.getNFrames(),info.imageParams.minimumT));
			return null;
		}
		
		if (info.tmParams.doRollingBallBackgroundSubtraction) {
			Double rbRad = info.tmParams.rollingBallRadius;
			imp = Utils.backgroundSubtraction(imp,rbRad);
		}
		imp.getCalibration().pixelWidth = 1d;
		imp.getCalibration().pixelHeight = 1d;
		imp.getCalibration().pixelDepth = 1d;
		imp.getCalibration().frameInterval = 1d;
		TrackMateAPIAccess tmResult = new TrackMateAPIAccess(this,info,imp);
		tmResult.run();
		tmResult.saveTmXml(outputTmXml);
		logService.info("   ...tracking saved as " + outputTmXml.getName());
		int nTracksFound = tmResult.getModel().getTrackModel().nTracks(true);
		logService.info("   Found " + String.valueOf(nTracksFound) + " tracks.");	
		if (nTracksFound < 1) {
			logService.error("   !!! no (valid) tracks found for this file. Analysis skipped !!!"); 
			if (info.sources.executionMode.equals("manual")) {
				Result dialogResult = uiService.showDialog("no tracks found; would you like to display the image?",DialogPrompt.MessageType.QUESTION_MESSAGE, DialogPrompt.OptionType.YES_NO_OPTION);
				if( dialogResult == DialogPrompt.Result.YES_OPTION ) {
					tmResult.displayImageOverlay();
				}
			}
			return null;
		}
		return tmResult;
	}
	
	private void runAnalysis(File currentFile, Model modelResult, Settings settingsResult) throws IOException {		
		try {
			TimeSeriesAnalysis tsAnal = new TimeSeriesAnalysis(this,info,currentFile.getName(),settingsResult,modelResult);
			tsAnal.generateArray(); // creates float array with x=T, y= track, c= nFeats (feats+intensCh)
			tsAnal.generateTSImg();
			tsAnal.expandArray();  // velocity and differentials, fft, etc
			tsAnal.generateTSImage();				
			tsAnal.tsAnalysis();   // detection of events on expand array, description of the track			
			tsAnal.generateTSAImage();
			tsAnal.syncArray();   //  target channel, tracks synchronized to burst
			tsAnal.generateTSSImage();
			tsAnal.syncArrayProjection(); // sync array projection, to represent a representative trace
			tsAnal.generateTSPImage();
			
			// prepare all the file system
			File fileNameSimple= Utils.removeFileNameExtension(currentFile,info.sources.fileExtension);
			
			File pathOutputZip = Utils.createOutputSubfolder(pathOutput,fileNameSimple); // this folder will be turned into a zip file
			String outputZipFile = pathOutputZip.getName() + TSAnInfo.outputExtensions.get("zip");
			if (!pathOutputZip.exists()) {
				pathOutputZip.mkdirs();
			}
			
			File pathOutputExtras = Utils.createOutputSubfolder(pathOutput,new File(TSAnInfo.outputExtensions.get("folder-extras")));
			if (!pathOutputExtras.exists()) {
				pathOutputExtras.mkdirs();
			}
			
			File outputTSImage = Utils.createOutputFile(pathOutputZip,fileNameSimple,TSAnInfo.outputExtensions.get("time-series"));
			File outputTSSImage = Utils.createOutputFile(pathOutputZip,fileNameSimple,TSAnInfo.outputExtensions.get("time-series-sync"));
			File outputTSPImage = Utils.createOutputFile(pathOutputZip,fileNameSimple,TSAnInfo.outputExtensions.get("trace-image"));		
			File outputTSPCsv = Utils.createOutputFile(pathOutputZip,fileNameSimple,TSAnInfo.outputExtensions.get("trace-table"));
			File outputTSPCsv2 = Utils.createOutputFile(pathOutputExtras,fileNameSimple,TSAnInfo.outputExtensions.get("trace-table"));
			File outputTSAImage = Utils.createOutputFile(pathOutputZip,fileNameSimple,TSAnInfo.outputExtensions.get("analysis-image"));		
			File outputTSACsv = Utils.createOutputFile(pathOutputZip,fileNameSimple,TSAnInfo.outputExtensions.get("analysis-table"));
			File outputTSAFcs = Utils.createOutputFile(pathOutputZip,fileNameSimple,TSAnInfo.outputExtensions.get("analysis-flow"));
			File outputTSAFcs2 = Utils.createOutputFile(pathOutputExtras,fileNameSimple,TSAnInfo.outputExtensions.get("analysis-flow"));
			File outputInfoJson = new File(pathOutputZip,TSAnInfo.outputExtensions.get("condition-info")); // condInfo file name does not contain the original name, just the generic "condition", to prevent crash if somebody manually changes the ts.zip file name
			
			// get images and save them. Access to the datasetIOservice from outside, ie from tsAnal, is somehow problematic and slow. From here works fine
			Dataset tsDataset = tsAnal.getTSImage();
			tsDataset.setName(fileNameSimple.getName()+" - track time-series");	
			
			Dataset tsaDataset = tsAnal.getTSAImage();
			tsaDataset.setName(fileNameSimple.getName()+" - time-series analysis");	
			
			Dataset tssDataset = tsAnal.getTSSImage();
			tssDataset.setName(fileNameSimple.getName()+" - track time-series synchronized to burst");	
						
			Dataset tspDataset = tsAnal.getTSPImage();
			tspDataset.setName(fileNameSimple.getName()+" - track time-series projection");	
						
			datasetIOService.save(tsDataset,outputTSImage.getAbsolutePath()); 
			datasetIOService.save(tsaDataset,outputTSAImage.getAbsolutePath()); 
			datasetIOService.save(tssDataset,outputTSSImage.getAbsolutePath()); 
			datasetIOService.save(tspDataset,outputTSPImage.getAbsolutePath()); 

			// export the analysis in text format track-descriptors and fcs file
			tsAnal.saveAnalysisAsCsv(outputTSACsv);
			tsAnal.saveProjectionAsCsv(outputTSPCsv);
			// copy for direct use, in the input folder
			tsAnal.saveProjectionAsCsv(outputTSPCsv2);
			// copy for the analysis
			tsAnal.saveAnalysisAsFcs(outputTSAFcs);
			// copy for direct use, in the input folder
			tsAnal.saveAnalysisAsFcs(outputTSAFcs2);
			
			// fill the meta-information
			TSAnInfo.AnalysedFileInfo afi = info.imageParams.analysedFilesInfo.get(currentFile.getName());
			afi.path = new File(pathOutput,outputZipFile).getAbsolutePath();
			afi.pathTSImage = outputTSImage.getName();
			afi.pathTSSImage = outputTSSImage.getName();
			afi.pathTSPImage = outputTSPImage.getName();
			afi.pathTraceCsv = outputTSPCsv.getName();
			afi.pathTSAImage = outputTSAImage.getName();
			afi.pathTSAFcs = outputTSAFcs.getName();
			afi.pathTSACsv = outputTSACsv.getName();
			afi.pathInfoJson = outputInfoJson.getName();
			afi.nFeats = tsAnal.getNFeats();
			afi.nFrames = tsAnal.getNFrames();
			afi.nTracks = tsAnal.getNTracks();
			afi.mapMaximum = tsAnal.getMapMaximum();
			afi.mapMinimum = tsAnal.getMapMinimum();
			CondInfo condInfo = new CondInfo();
			condInfo.condition = currentFile.getName();
			condInfo.info = info;
			Utils.writeJsonInfo(condInfo, outputInfoJson.getAbsolutePath());
			
			// get sub-folder contents, save it as zip, delete sub-folder
			Utils.packZip(pathOutput, pathOutputZip, outputZipFile);
						
			// alternative: save as IJ1.0
//			IJ.save(tsAnal.getTSImagePlus(),outputTSImage.getAbsolutePath());
//			tsAnal.getTSImagePlus().close();			
//			
//			IJ.save(tsAnal.getTSAImagePlus(),outputTSAImage.getAbsolutePath());
//			tsAnal.getTSAImagePlus().close();
//			
//			IJ.save(tsAnal.getTSSImagePlus(),outputTSSImage.getAbsolutePath());
//			tsAnal.getTSSImagePlus().close();
//	
//			IJ.save(tsAnal.getTSPImagePlus(),outputTSPImage.getAbsolutePath());
//			tsAnal.getTSPImagePlus().close();
//			
		} catch (Exception e) {
			e.getStackTrace();
		}
	}
	
//	private void runAnalysisObsolete(File currentFile) throws IOException {
//		File outputSimple= Utils.createOutputFile(currentFile,"");
//		TSAnInfo.ObsoleteInfo props = info.obsoleteInfo; 
//
//		ArrayList<Float[]> arrayObsolete = Utils.readCsvObsolete(currentFile,';');		
//		
//		props.fileHeader =  Utils.readHeaderCsvObsolete(currentFile,';');
//		
//		HashSet<Float> trackIDSet = new HashSet<Float>();
//		for (Float[] tr : arrayObsolete) {
//			trackIDSet.add(tr[0]);
//		}		
//		props.trackIDArray = trackIDSet.toArray(new Float[trackIDSet.size()]);
//		props.nTracks = trackIDSet.size();
//		
//		HashSet<Float> framesSet = new HashSet<Float>();
//		Float maxFrame = 0f;
//		for (Float[] tr : arrayObsolete) {
//			framesSet.add(tr[1]);
//			if (tr[1] > maxFrame) {
//				maxFrame = tr[1];
//			}
//		}	
//		props.nFrames = (int) (float) maxFrame;
//		
//		TimeSeriesAnalysisObsolete tsAnal = new TimeSeriesAnalysisObsolete(this,props,arrayObsolete);
//		tsAnal.generateArray(); // creates float array with x=T, y= track, c= nFeats (feats+intensCh)
//		tsAnal.generateTSImage();
//		tsAnal.expandArray();  // velocity and differentials, fft, etc
//		tsAnal.generateTSEImage();
//
//
//		File outputTSImage = Utils.createOutputFile(pathOutput,currentFile,"ts.tif");
//		Dataset tsImage = tsAnal.getTSImage();
//		tsImage.setName(outputSimple.getName()+" - trackTimeSeries_unprocessed");
//		
//		datasetIOService.save(tsImage,outputTSImage.getAbsolutePath());
//
//
//		File outputTSEImage = Utils.createOutputFile(pathOutput,currentFile,"tse.tif");
//		Dataset tseImage = tsAnal.getTSEImage();
//		tseImage.setName(outputSimple.getName()+" - trackTimeSeries_processed");
//		datasetIOService.save(tseImage,outputTSEImage.getAbsolutePath());
//
//		tsAnal.tsAnalysis();   // detection of events on expand array, description of the track
//		tsAnal.generateTSAImage();
//		tsAnal.syncArray();   //  target channel, tracks synchronized to burst
//		tsAnal.generateTSSImage();
//		
//		File outputTSAimage = Utils.createOutputFile(pathOutput,currentFile,"tsa.tif");
//		Dataset tsaImage = tsAnal.getTSAImage();
//		tsaImage.setName(outputSimple.getName()+" - trackTimeSeries_analysis");
//		datasetIOService.save(tsaImage,outputTSAimage.getAbsolutePath());
//
//		File outputTSSimage = Utils.createOutputFile(pathOutput,currentFile,"tss.tif");
//		Dataset tssImage = tsAnal.getTSSImage();
//		tssImage.setName(outputSimple.getName()+" - trackTimeSeries_synchronized burst");
//		datasetIOService.save(tssImage,outputTSSimage.getAbsolutePath());
//
//		
//		/* export the analysis in text format track-descriptors and fcs file; the cherry on the cake */
//		File outputTSAcsv = Utils.createOutputFile(pathOutput,currentFile,"tsa.csv");
//		tsAnal.saveAnalysisAsCsv(outputTSAcsv);
//		File outputTSAfcs = Utils.createOutputFile(currentFile,"tsa.fcs");
//		tsAnal.writeToFcs(outputTSAfcs);
//		File outputInfoJson = Utils.createOutputFile(pathOutput,currentFile,"ts.json");
//		Utils.writeJsonTSAnInfo(info, outputInfoJson.getAbsolutePath());
//	}
	/**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        
        ij.command().run(CellIntenTSAn.class, true);
    	}
@Override
public String getInfoText() {
	// TODO Auto-generated method stub
	return null;
}
@Override
public ImageIcon getIcon() {
	// TODO Auto-generated method stub
	return null;
}
@Override
public String getKey() {
	// TODO Auto-generated method stub
	return null;
}
@Override
public String getName() {
	// TODO Auto-generated method stub
	return null;
}
@Override
public TrackMateAction create() {
	// TODO Auto-generated method stub
	return null;
}
	}











		/* test shuffling
	 	*       //inside main(), after ij.ui().showUI() --->
	 	*         File fl = new File("/Users/iago/Desktop/_testShuffling/shuffleTestXYTCZ.tif");
        ImagePlus imp = IJ.openImage(fl.getAbsolutePath());
        imp.setDimensions(imp.getNSlices(),imp.getNFrames(),imp.getNChannels());
        System.out.println(String.format("C %d    Z %d     T %d", imp.getNChannels(),imp.getNSlices(),imp.getNFrames()));
        String dimOrder = "XYTCZ";
        Utils.dimensionShuffle(imp, dimOrder);
        System.out.println(String.format("C %d    Z %d     T %d", imp.getNChannels(),imp.getNSlices(),imp.getNFrames()));
        IJ.saveAs(imp,"tif", String.format("/Users/iago/Desktop/_testShuffling/shuffleTest_%s.tif",dimOrder));
        imp.close();
        */

/**
 *  checks that TrackMate is installed TODO check if this step is necessary with my imports
 */
//public void validateTMInstalled() {
//	PluginInfo<?> tm_ = pluginService.getPlugin("fiji.plugin.trackmate.TrackMateOptions");
//	if(tm_ == null) {
//		cancel("please install trackmate plugin, or use Fiji instead of ImageJ");
//	}
//}
//
///**
// * checks that TrackMate extras are installed TODO check if this is actually necessary
// */
//public void validateTMExtrasInstalled() {
//	PluginInfo<?> tm_ = pluginService.getPlugin("fiji.plugin.trackmate.extra.trackanalyzer.TrackMeanIntensityAnalyzer");
//	if(tm_ == null) {
//		cancel("please install trackmate-extras plugin");
//	}
//}
