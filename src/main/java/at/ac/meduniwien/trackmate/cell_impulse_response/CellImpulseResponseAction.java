package at.ac.meduniwien.trackmate.cell_impulse_response;

import java.awt.Frame;
import java.io.File;

import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import net.imagej.Dataset;

public class CellImpulseResponseAction implements TrackMateAction
{

	private Logger logger;

	@Override
	public void execute( final TrackMate trackmate, final SelectionModel selectionModel, final DisplaySettings displaySettings, final Frame parent )
	{
		logger.log( "Launching cell impulse response analysis..." );
		try {
			//TODO create info, get filename
			TimeSeriesAnalysis tsAnal = new TimeSeriesAnalysis(trackmate.getName(),trackmate.getSettings(),trackmate.getModel());
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
			
			TSAnInfo info = new TSAnInfo();
			// prepare all the file system
			File fileNameSimple= Utils.removeFileNameExtension(trackmate.getName(),info.sources.fileExtension);
			
			File pathOutputZip = Utils.createOutputSubfolder(info.pathOutput,fileNameSimple); // this folder will be turned into a zip file
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

	@Override
	public void setLogger( final Logger logger )
	{
		this.logger = logger;
	}
}
