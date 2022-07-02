package at.ac.meduniwien.trackmate.cell_impulse_response;

import at.ac.meduniwien.trackmate.cell_impulse_response.TSAnInfo;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.axis.CalibratedAxis;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.meta.axis.DefaultLinearAxis;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.gradient.PartialDerivative;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
//import org.joml.Math;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import ij.ImagePlus;
import ij.ImageStack;
import fiji.plugin.trackmate.FeatureModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.io.IOException;

public class TimeSeriesAnalysis{
	private DatasetService datasetService;
	private TSAnInfo info;
	private Model model;
	private String condition;
	private int conditionID;
	
	private int nFrames;
	private int nTracks;
	private Set<Integer> trackIDset;
	private ArrayList<Integer> trackIDArray;
	private int nChannelsImage;
	private int nChannels;
	private ArrayList<String> availableChannels;
	private HashMap<String,TSAnInfo.CompositeChannelInfo> compositeChannelsInfo;
	private String targetChannelLabel;
	private Integer targetChannelIdx;
	private double sigma;
	private double charactTime;
	private int nFeats;
	private int nDescriptors;

	private ArrayList<String> featsTracks = new ArrayList<String>();	
	private ArrayList<String> featsTM = new ArrayList<String>();	
	private ArrayList<String> featsExpanded = new ArrayList<String>();
	private ArrayList<String> feats	= new ArrayList<String>();	
	private ArrayList<String> featsRaw	= new ArrayList<String>();	
	private ArrayList<String> descriptors = new ArrayList<String>(); //38  
	
	private long[] dims;
	private float[][] dataArray;
	
	private Dataset tsImage;
	private Dataset tsaImage;
	private Dataset tssImage;
	private Dataset tspImage;
	private Img<FloatType> tsStack;
	private Img<FloatType> analStack;
	private Img<FloatType> syncStack;
	private Img<FloatType> projStack;
	private HashMap<String, Float> mapMaximum;
	private ImagePlus tsImagePlus;
	private ImagePlus tssImagePlus;
	private ImagePlus tspImagePlus;
	private ImagePlus tsaImagePlus;
	private HashMap<String, Float> mapMinimum;
	private Dataset tsDataset;
	
	public TimeSeriesAnalysis(String condition, Settings settings, Model model){
		this.condition = condition;
		this.conditionID = condition.hashCode();
		this.info = info;
		this.model = model;
		this.nFrames = settings.nframes;
		this.nTracks = model.getTrackModel().nTracks(true);
		this.trackIDset = model.getTrackModel().trackIDs(true);
		this.trackIDArray = new ArrayList<Integer>();
		this.trackIDArray.addAll(trackIDset);
		this.nChannelsImage = settings.imp.getNChannels(); // intensity channels from the image file
		this.availableChannels = new ArrayList<String>(Arrays.asList(this.info.tsParams.availableChannels)); // intensity channels, including the new composites
		Collections.sort(availableChannels); // convenient to parse COMP channels in order, as late ones may depend on former ones
		this.nChannels = availableChannels.size();
		this.targetChannelLabel = this.info.tsParams.targetChannel;
		this.compositeChannelsInfo = this.info.tsParams.compositeChannelsInfo;
		this.charactTime = this.info.tsParams.characteristicTFrames;
		this.sigma = this.info.tsParams.tsSigma;
		for (FeatsTracks ft: FeatsTracks.values()) {
			featsTracks.add(ft.name());
		}
		for (FeatsTrackMate ft: FeatsTrackMate.values()) {
			featsTM.add(ft.name());
		}

		for (FeatsExpanded ft: FeatsExpanded.values()) {
			featsExpanded.add(ft.name());
		}
		for (FeatsDescriptors ft: FeatsDescriptors.values()) {
			descriptors.add(ft.name());
		}
		feats.add("TRACK_FEATURES");
		feats.addAll(featsTM);
		feats.addAll(featsExpanded);
		for (String intensCh: availableChannels) {
			String icx = "INTENSITY_CH_"+intensCh;
			feats.add(icx);
			descriptors.add("avChannel"+intensCh);
		}
		this.nFeats = feats.size();//trackInfo, plus spotID, X,Y,Z, R, estim.Diameter,Q,SNR,Contrast, and intensity for each channel, including composites,plus featsExpanded:mask,dx,dy,dz,v,v_s,a,i,i_s,di_s,ddi_s,fft,hiState
		this.nDescriptors = descriptors.size();
		this.targetChannelIdx = feats.lastIndexOf("INTENSITY_CH_"+targetChannelLabel);
		
		featsRaw.addAll(featsTM);
		for (int i = 0; i < nChannelsImage; i++) {
			featsRaw.add("MEAN_INTENSITY0" + (i+1)); //this is the label that trackmate has for its intensity values
		}
		
		long[] dims = {nFrames,nTracks,nFeats};
		this.dims = dims;
		
		info.tsParams.feats = feats.toArray(new String[feats.size()]);
		info.tsParams.featsExpanded = featsExpanded.toArray(new String[featsExpanded.size()]); // the last 
		info.tsParams.featsTracks = featsTracks.toArray(new String[featsTracks.size()]);
		info.tsParams.featsDescriptors = descriptors.toArray(new String[descriptors.size()]);
	}
	
	public void generateArray() {		
		//double[][][] data = new double[nFeats+1][nTracks][nFrames];
		float[][] data = new float[nFeats][nTracks*nFrames]; // the array is already sized for the new composite channels and expanded features, but is populated only for the raw channels
		for(Integer tr : trackIDArray) {		
			int trackIdx = trackIDArray.indexOf(tr);
			FeatureModel fm = model.getFeatureModel();
			data[0][trackIdx*nFrames+featsTracks.indexOf("COND_ID")] = (float) conditionID; //first column contains only the TrackMatetrackID
			data[0][trackIdx*nFrames+featsTracks.indexOf("TRACK_ID")] = (float) tr; //first column contains only the TrackMatetrackID
			data[0][trackIdx*nFrames+featsTracks.indexOf("TRACK_DURATION")] = (float) (double) fm.getTrackFeature(tr, "TRACK_DURATION");
			data[0][trackIdx*nFrames+featsTracks.indexOf("TRACK_START")] = (float) (double) fm.getTrackFeature(tr, "TRACK_START");
			data[0][trackIdx*nFrames+featsTracks.indexOf("TRACK_STOP")] = (float) (double) fm.getTrackFeature(tr, "TRACK_STOP");
			data[0][trackIdx*nFrames+featsTracks.indexOf("TRACK_DISPLACEMENT")] = (float) (double) fm.getTrackFeature(tr, "TRACK_DISPLACEMENT");
			for(Spot spot : model.getTrackModel().trackSpots(tr)) {
				Map<String,Double> spotFeats = spot.getFeatures();
				// finally, the X dimension of the new array
				int frameIdx = (int) (double) spotFeats.get("FRAME");
				//System.out.println(" --- "+frameIdx);
				for(String feat:featsRaw) {
					int featIdx = featsRaw.indexOf(feat) + 1;
					if (featIdx == 0) { //case of non-multichannel tm result or any other reason
						continue;
					} else if (featIdx == 1) {
						data[featIdx][trackIdx*nFrames+frameIdx] = (float) spot.ID();
					} else {
						data[featIdx][trackIdx*nFrames+frameIdx] = (float) (double) spotFeats.get(feat);
						//System.out.println(" ------ "+feat+"  "+trackIdx+"  "+frameIdx+ " -> " + spotFeats.get(feat));
					}								
				}
			}
		}	
		// proceed with adding the composite channels
		for (String el: availableChannels) { // this way it access the array in an ordered manner, as later comp channels can be derived from former comp channels
			if (!el.startsWith("COMP")) {
				int rawChIdx = 1 + featsRaw.indexOf("MEAN_INTENSITY0"+el); // feats raw + track duration already included
				int chIdx = feats.indexOf("INTENSITY_CH_"+el);
				data[chIdx]= data[rawChIdx].clone();			
			} else {
				int idxCompCh = feats.indexOf("INTENSITY_CH_"+el);
				String cA = compositeChannelsInfo.get(el).channelA;
				int idxCA = feats.indexOf("INTENSITY_CH_"+cA);
				String cB = (String) compositeChannelsInfo.get(el).channelB;
				int idxCB = feats.indexOf("INTENSITY_CH_"+cB);
				String op = (String) compositeChannelsInfo.get(el).operation; 
				Double kA = (Double) compositeChannelsInfo.get(el).kA;
				Double kB = (Double) compositeChannelsInfo.get(el).kB;
				Double offset = (Double) compositeChannelsInfo.get(el).offset;
				
				float[] arrA = data[idxCA];
				float[] arrB = data[idxCB];
				float[] arrResult = data[idxCB].clone();
				float kAf = (float) (double) kA;
				float kBf = (float) (double) kB;
				float offsetf = (float) (double) offset;
				switch(op) {
				case "Add":
					for (int i=0;i<arrA.length;i++) {
						arrResult[i] = offsetf + (arrA[i] * kAf ) + (arrB[i] * kBf);
					}
					break;
				case "Subtract":
					for (int i=0;i<arrA.length;i++) {
						arrResult[i] = offsetf + (arrA[i] * kAf ) - (arrB[i] * kBf);
					}
					break;
				case "Multiply":
					for (int i=0;i<arrA.length;i++) {
						arrResult[i] = offsetf + (arrA[i] * kAf ) * (arrB[i] * kBf);
					}
					break;
				case "Divide":
					for (int i=0;i<arrA.length;i++) {
						if ((arrB[i] * kBf) == 0) { 
							arrResult[i] = 0f;
						} else {
							arrResult[i] = offsetf + (arrA[i] * kAf ) / (arrB[i] * kBf);
						}
					}
					break;
				default:
					for (int i=0;i<arrA.length;i++) {
						arrResult[i] = 0f;
					}
					break;
				}
				data[idxCompCh] = arrResult;
			}							
		}
		dataArray = data;
		info.tsParams.feats = feats.toArray(new String[feats.size()]);
	}
	
	public void generateTSImg() {
		AxisType[] axes = {Axes.X, Axes.Y, Axes.CHANNEL};		
		tsImage = datasetService.create(dims,"track time-series",axes,32,true,true);
		for(int i=0;i<nFeats;i++) {
			tsImage.setPlane(i, dataArray[i]);
		}
		@SuppressWarnings("unchecked")	
		Img<FloatType> rawStack = (Img<FloatType>) tsImage.getImgPlus();
		this.tsStack = rawStack;
	}
	
	public void expandArray(){		
		// random access for each image
		RandomAccess<FloatType> rats = tsStack.randomAccess();

		// basic ops: dx, dy, dz, velocity, fill x, y, z, intens
		int[] pos = new int[3];

		int idxRadius = feats.indexOf("RADIUS");
		int idxEstimatedDiameter = feats.indexOf("ESTIMATED_DIAMETER");
		int idxPositionX = feats.indexOf("POSITION_X");
		int idxPositionY = feats.indexOf("POSITION_Y");
		int idxPositionZ = feats.indexOf("POSITION_Z");
		int idxDifX = feats.indexOf("DIF_X");
		int idxDifY = feats.indexOf("DIF_Y");
		int idxDifZ = feats.indexOf("DIF_Z");
		int idxVelocity = feats.indexOf("VELOCITY"); 
		int idxVelocitySmooth = feats.indexOf("VELOCITY_SMOOTH");
		int idxAcceleration = feats.indexOf("ACCELERATION");
		int idxIntensity = feats.indexOf("INTENSITY");
		int idxIntensitySmooth = feats.indexOf("INTENSITY_SMOOTH");
		int idxDifIntensitySmooth  = feats.indexOf("DIF_INTENSITY_SMOOTH");
		int idxDifDifIntensitySmooth  = feats.indexOf("DIF_DIF_INTENSITY_SMOOTH");
		int idxFFT = feats.indexOf("FFT_INTENSITY_SMOOTH");
		int idxMask = feats.indexOf("MASK");

		int idxFirstIntensityChannel = 1 + featsTM.size() + featsExpanded.size();
		int idxTargetChannel = targetChannelIdx;
		// translate intensity planes to end
		
		
		//for each track j ...
		for (int j = 0; j < dims[1]; j++) {	
			// reset all features
			float estimDiam = 0f;
			float r = 0f;
			float mask = 0f;
			float x = 0f;
			float y = 0f;
			float z = 0f;
			float dx = 0f;
			float dy = 0f;
			float dz = 0f;
			float v = 0f;
			float intens = 0f;
			float[] chIntens = new float[nChannels];
			float estimDiamPre = 0f;
			float maskPre = 0f;
			float xPre = 0f;
			float yPre = 0f;
			float zPre = 0f;
			float estimDiamPost = 0f;
			float rPost = 0f;
			float maskPost = 0f;
			float xPost = 0f;
			float yPost = 0f;
			float zPost = 0f;
			float[] chIntensPre = new float[nChannels];
			float[] chIntensPost = new float[nChannels];
			
			
			//for each time-frame i ...
			for (int i = 0; i < dims[0]; i++) { 
				pos[0]=i; //frame
				pos[1]=j; //track
				pos[2]=0; 
				rats.setPosition(pos);				
				
				//1. normalize radius to strict mask, duplicate to mask, and fill with previous value (fill forward)
				rats.setPosition(idxRadius,2);	
				r = rats.get().get();
				r = (r==0f) ? 0f : 1f;
				rats.get().set(r);
				mask = (r==0f) ? maskPre : 1f;				
				rats.setPosition(idxMask,2);
				rats.get().set(mask);
				maskPre = mask;
				
				//2. in-situ fill forward NA(0) values with previous values for: shape,x,y,z,intensityChannel
				
				rats.setPosition(idxEstimatedDiameter,2);
				estimDiam = rats.get().get();
				estimDiam = (estimDiam==0f) ? estimDiamPre : estimDiam;
				rats.get().set(estimDiam);
				estimDiamPre = estimDiam;
				
				rats.setPosition(idxPositionX,2);
				x = rats.get().get();
				x = (x==0f) ? xPre : x;
				rats.get().set(x);
				xPre = x;
				
				rats.setPosition(idxPositionY,2);
				y = rats.get().get();
				y = (y==0f) ? yPre : y;
				rats.get().set(y);
				yPre = y;
				
				rats.setPosition(idxPositionZ,2);
				z = rats.get().get();	
				z = (z==0f) ? zPre : z;					
				rats.get().set(z);
				zPre = z;
				
				for (int k = 0;k < nChannels ; k++) {
					rats.setPosition(k + idxFirstIntensityChannel,2);
					float chInt = rats.get().get();
					chInt = (chInt == 0f) ? chIntensPre[k] : chInt;
					chIntens[k] = chInt;
					chIntensPre[k] = chIntens[k];
					rats.get().set(chInt);
				}						
			}		
		
			// 3. fill backwards nulls
			for (int i = (int) dims[0]-1; i>=0 ; i--) {
				//3.1 read later value
				pos[0]=i;
				pos[1]=j;
				pos[2]=0; 
				rats.setPosition(pos);
				
				rats.setPosition(idxEstimatedDiameter,2);
				estimDiam = rats.get().get();
				estimDiam = (estimDiam==0f) ? estimDiamPost : estimDiam;
				rats.get().set(estimDiam);
				estimDiamPost = estimDiam;
				
				
				rats.setPosition(idxPositionX,2);
				x = rats.get().get();
				x = (x==0f) ? xPost : x;
				rats.get().set(x);
				xPost = x;
				
				rats.setPosition(idxPositionY,2);
				y = rats.get().get();
				y = (y==0f) ? yPost : y;
				rats.get().set(y);
				yPost = y;
				
				rats.setPosition(idxPositionZ,2);
				z = rats.get().get();	
				z = (z==0f) ? zPost : z;	
				rats.get().set(z);
				zPost = z;
				
				for (int k = 0;k < nChannels ; k++) {
					rats.setPosition(k + idxFirstIntensityChannel,2);
					float chInt = rats.get().get();
					chInt = (chInt == 0f) ? chIntensPost[k] : chInt;
					rats.get().set(chInt);
					chIntens[k] = chInt;					
					chIntensPost[k] = chIntens[k];
				}
				
				// 3.1 Duplicate intensity target channel to intensity
				rats.setPosition(idxTargetChannel,2);	
				intens = rats.get().get();
				rats.setPosition(idxIntensity,2);
				rats.get().set(intens);
				
				// 3.2 Mask compare to strict mask, where does it end 
				rats.setPosition(idxRadius,2);
				r = rats.get().get();
				rats.setPosition(idxMask,2);
				mask = rats.get().get();
				r = (r == 0f) ? rPost : r;
				rPost = r;
				mask = r * mask; // sorts of AND operation with float 
				rats.get().set(mask);				
			}	
			
			// reset values
			x = 0f;
			y = 0f;
			z = 0f;
			xPre = 0f;
			yPre = 0f;
			zPre = 0f;
			xPost = 0f;
			yPost = 0f;
			zPost = 0f;

			// 4. differential applications dx, dy, dz, velocity, acceleration
			for (int i = 0; i < dims[0]; i++) {
				pos[0]=i;
				pos[1]=j;
				pos[2]=0; 
				rats.setPosition(pos);
				rats.setPosition(idxPositionX,2);
				x = rats.get().get();
				dx = (xPre == 0f) ? 0f : x - xPre;
				rats.setPosition(idxDifX,2);
				rats.get().set(dx);
				xPre = x;
				
				rats.setPosition(idxPositionY,2);
				y = rats.get().get();
				dy = (yPre == 0f) ? 0f : y - yPre;
				rats.setPosition(idxDifY,2);
				rats.get().set(dy);
				yPre = y;
				
				rats.setPosition(idxPositionZ,2);
				z = rats.get().get();
				dz = (zPre == 0f) ? 0f : z - zPre;
				rats.setPosition(idxDifZ,2);
				rats.get().set(dz);
				zPre = z;						
			
				v = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);
				rats.setPosition(idxVelocity,2);
				rats.get().set(v);
			}
		}
		
		// gauss 1d of velocity and acceleration
		double[] sigma1D = new double[] {sigma,0};
		RandomAccessibleInterval<FloatType> planeV = Views.hyperSlice(tsStack,2,idxVelocity);
		RandomAccessibleInterval<FloatType> planeVs = Views.hyperSlice(tsStack,2,idxVelocitySmooth);
		Gauss3.gauss(sigma1D, Views.extendMirrorSingle( planeV ), planeVs);
		
		RandomAccessibleInterval<FloatType> planeAccel = Views.hyperSlice(tsStack,2,idxAcceleration);
		PartialDerivative.gradientBackwardDifference(Views.extendMirrorSingle( planeVs ), planeAccel, 0);

		// intensity gauss 1d, diff intensity and diff*diff intensity (==LoG). 
		// As gaussian and first derivative are anyway calculated, there is no advantage in creating a kernel and convolving
		RandomAccessibleInterval<FloatType> planeIntens = Views.hyperSlice(tsStack,2,idxIntensity);
		RandomAccessibleInterval<FloatType> planeIntensS = Views.hyperSlice(tsStack,2,idxIntensitySmooth);
		Gauss3.gauss(sigma1D, Views.extendMirrorSingle( planeIntens ), planeIntensS);
		
		RandomAccessibleInterval<FloatType> planeRateIntens = Views.hyperSlice(tsStack,2,idxDifIntensitySmooth);
		PartialDerivative.gradientBackwardDifference(Views.extendMirrorSingle( planeIntensS ), planeRateIntens, 0);
		
		RandomAccessibleInterval<FloatType> planeDivIntens = Views.hyperSlice(tsStack,2,idxDifDifIntensitySmooth);
		PartialDerivative.gradientBackwardDifference(Views.extendMirrorSingle( planeRateIntens ), planeDivIntens, 0);

		info.tsParams.featsExpanded = featsExpanded.toArray(new String[featsExpanded.size()]);
		
	}
	
	public void tsAnalysis() throws Exception {
		// retrieve info from rawStack
		int numDims = tsStack.numDimensions();
		long[] dims = new long[numDims];
		tsStack.dimensions(dims); // loads the size of each dimension into the dims array. Bit verbose, only want 1 dimension actually, the number of tracks (1)	
		int descriptors = nDescriptors;
		int[] dimsTSA = {descriptors,(int) dims[1]};
		ImgFactory<FloatType> imgFactory = new ArrayImgFactory<>(new FloatType());
		Img<FloatType> analStack = imgFactory.create(dimsTSA);

		// random access for each image
		RandomAccess<FloatType> rats = tsStack.randomAccess();
		RandomAccess<FloatType> raAnal = analStack.randomAccess();
		
		// time-series image indexes
		int idxCondID = featsTracks.indexOf("COND_ID");
		int idxTrackID = featsTracks.indexOf("TRACK_ID");
		int idxTrackDuration = featsTracks.indexOf("TRACK_DURATION");
		int idxTrackStart = featsTracks.indexOf("TRACK_START");
		int idxTrackStop = featsTracks.indexOf("TRACK_STOP");
		int idxTrackDisplacement = featsTracks.indexOf("TRACK_DISPLACEMENT");;
		
		int idxTrackFeats = feats.indexOf("TRACK_FEATURES");
		int idxEstimatedDiameter = feats.indexOf("ESTIMATED_DIAMETER");
		int idxPositionX = feats.indexOf("POSITION_X");
		int idxPositionY = feats.indexOf("POSITION_Y");
		int idxPositionZ = feats.indexOf("POSITION_Z");
		int idxVelocitySmooth = feats.indexOf("VELOCITY_SMOOTH");
		int idxIntensitySmooth = feats.indexOf("INTENSITY_SMOOTH");
		int idxDifIntensitySmooth  = feats.indexOf("DIF_INTENSITY_SMOOTH");
		int idxFirstIntensityChannel = 1 + featsTM.size() + featsExpanded.size();
		
		//TODO automatically create with FeatsDescriptors, as a hashmap? but perhaps slows down performance and anyway these feats need to be instantiated properly, no automatization here
		float condID;
		float trackID;
		float trackDuration; //length
		float trackStart; // start
		float trackStop; // end
		float trackDisplacement; 
		float burst; // highest change in intensity
		float trackBurst; // frame of burst
		float peak; //peak-max
		float trackPeak; //frame of peak maximum
		float framesBefore;
		float framesAfter;
		float startX;
		float startY;
		float startZ;
		float burstX; //x-burst
		float burstY; //y-burst
		float burstZ; //z-burst
		float stopX;
		float stopY;
		float stopZ;
		float velocityTotalMin;
		float velocityTotalAv;
		float velocityBeforeAv;
		float velocityAfterAv;
		float displTotal;
		float displBefore;
		float displAfter;
		float totalAv;
		float total25;
		float total50;
		float total75;
		float beforeAv;
		float before25;
		float before50;
		float before75;
		float afterAv;
		float after25;
		float after50;
		float after75;
		float earlyAv;
		float early25;
		float early50;
		float early75;
		float lateAv;
		float late25;
		float late50;
		float late75;
		float diameterAv;
		float diameterBeforeAv;
		float diameterAfterAv;
		
		float[] otherChAv = new float[nChannels];
		
		
		//for each track i...
		for (int i = 0; i< dims[1]; i++) { 		
			// gets trackIDs, duration, track start, track stop
			int[] pos = new int[] {0,i,idxTrackFeats};
			rats.setPosition(pos); // go to track and first plane
			rats.setPosition(idxCondID,0);
			condID = rats.get().get();
			rats.setPosition(idxTrackID,0);			
			trackID = rats.get().get();
			rats.setPosition(idxTrackDuration,0);
			trackDuration = rats.get().get();
			rats.setPosition(idxTrackStart,0);
			trackStart = rats.get().get();
			rats.setPosition(idxTrackStop,0);
			trackStop = rats.get().get();
			rats.setPosition(idxTrackDisplacement,0);
			trackDisplacement = rats.get().get();
			
			// find burst, aka max dif intensity smooth
			pos[0] = (int) trackStart;
			pos[2] = idxDifIntensitySmooth;
			rats.setPosition(pos);
			burst = rats.get().get();
			trackBurst = rats.getIntPosition(0);
			for (float j = trackStart ; j <= trackStop; j++) {		
				pos[0] = (int) j;
				rats.setPosition(pos);
				float val = rats.get().get(); ///
				if ( val > burst ) {
					trackBurst = j;
					burst = val;
				}

			}
			
			framesBefore = trackBurst - trackStart;
			framesAfter = trackStop - trackBurst;
			
			//position in critical points start, burst and stop
			pos[0] = (int) trackStart;
			pos[2] = idxPositionX; 
			rats.setPosition(pos);
			startX = rats.get().get(); 
			pos[2] = idxPositionY; 
			rats.setPosition(pos);
			startY = rats.get().get();
			pos[2] = idxPositionZ; // 
			rats.setPosition(pos);
			startZ = rats.get().get();
			
			pos[0] = (int) trackBurst;
			pos[2] = idxPositionX;
			rats.setPosition(pos);
			burstX = rats.get().get(); 
			pos[2] = idxPositionY;
			rats.setPosition(pos);
			burstY = rats.get().get();
			pos[2] = idxPositionZ;
			rats.setPosition(pos);			
			burstZ = rats.get().get();

			pos[0] = (int) trackStop;
			pos[2] = idxPositionX;
			rats.setPosition(pos);
			stopX = rats.get().get(); 
			pos[2] = idxPositionY;
			rats.setPosition(pos);
			stopY = rats.get().get();
			pos[2] = idxPositionZ;
			rats.setPosition(pos);			
			stopZ = rats.get().get();
			
			double dispXtotal = stopX - startX;
			double dispYtotal = stopY - startY;
			double dispZtotal = stopZ - startZ;
			double dispXbefore = burstX - startX;
			double dispYbefore = burstY - startY;
			double dispZbefore = burstZ - startZ;
			double dispXafter = stopX - burstX;
			double dispYafter = stopY - burstY;
			double dispZafter = stopZ - burstZ;
			// would there be a Math method to calculate distance?
			displTotal = (float) Math.sqrt(dispXtotal*dispXtotal + dispYtotal*dispYtotal + dispZtotal*dispZtotal);
			displBefore = (float) Math.sqrt(dispXbefore*dispXbefore + dispYbefore*dispYbefore + dispZbefore*dispZbefore);
			displAfter = (float) Math.sqrt(dispXafter*dispXafter + dispYafter*dispYafter + dispZafter*dispZafter);
//
//			float descriptor;
//			if ((trackBurst - trackEnd) < charactTime) {
//				descriptor = 0;
//			} else if ((trackBurst - trackEnd) < 3*charactTime) {
//				descriptor = 1;
//			} else if ((trackBurst - trackEnd) >= 3*charactTime) {
//				descriptor = 2;
//			} else {
//				descriptor = 3;
//			}
			
			// for indexing we use int here because position will be set wit with setPosition() and not fwd()
			int trStart = (int) trackStart;
			int trStop = (int) trackStop;
			ArrayList<Float> diameter = new ArrayList<Float>();	
			ArrayList<Float> diameterBefore = new ArrayList<Float>();	
			ArrayList<Float> diameterAfter = new ArrayList<Float>();	
			ArrayList<Float> velocity = new ArrayList<Float>();				
			ArrayList<Float> velocityBefore = new ArrayList<Float>();			
			ArrayList<Float> velocityAfter = new ArrayList<Float>();	
			ArrayList<Float> intensityTotal = new ArrayList<Float>();
			ArrayList<Float> intensityBefore =new ArrayList<Float>();
			ArrayList<Float> intensityAfter = new ArrayList<Float>();
			ArrayList<Float> intensityEarly = new ArrayList<Float>();
			ArrayList<Float> intensityLate =  new ArrayList<Float>();
 			
			// intensity block (idxIntensitySmooth)
 			pos[0] = (int) trStart;
 			pos[2] = idxIntensitySmooth; 
			rats.setPosition(pos);
			peak = rats.get().get();
			trackPeak = rats.getIntPosition(0);
			for (int j = trStart ; j <= trStop; j++) {
				rats.setPosition(j,0);
				float val = rats.get().get();		
				intensityTotal.add(val);
				if (j <= trackBurst) {
					intensityBefore.add(val);
				}
				if (j > trackBurst) {
					intensityAfter.add(val);
					if (j <= (trackBurst + charactTime)) {
						intensityEarly.add(val);
					} else if (j > (trackBurst + 2*charactTime) && j <= (trackBurst + 3*charactTime)) {
						intensityLate.add(val);
					}
				}
				if ( val > peak ) {
					peak = val;
					trackPeak = (float) j;					
				}
			}	

			// shape block (estimatedDiameter)
			pos[2] = idxEstimatedDiameter;
			for (int j = trStart ; j <= trStop; j++) {
				pos[0] = j;
				rats.setPosition(pos);
				float val = rats.get().get();
				diameter.add(val);	
				if (j <= trackBurst) {
					diameterBefore.add(val);
				}
				if (j > trackBurst) {
					diameterAfter.add(val);
				}
			}
			
			// velocity block (velocitySmooth)
			pos[2] = idxVelocitySmooth;
			for (int j = trStart ; j <= trStop; j++) {
				pos[0] = j;
				rats.setPosition(pos);
				float val = rats.get().get();
				velocity.add(val);
				if (j <= trackBurst) {
					velocityBefore.add(val);
				}
				if (j > trackBurst) {
					velocityAfter.add(val);
				}
			}
			
			Collections.sort(intensityTotal);
			Collections.sort(intensityBefore);
			Collections.sort(intensityAfter);
			Collections.sort(intensityEarly);
			Collections.sort(intensityLate);
			Collections.sort(velocity);
			Collections.sort(velocityBefore);
			Collections.sort(velocityAfter);
			Collections.sort(diameter);
			Collections.sort(diameterBefore);
			Collections.sort(diameterAfter);

			totalAv = averageList(intensityTotal);
			total25 = percentile(intensityTotal,0.25f);
			total50 = percentile(intensityTotal,0.5f);
			total75 = percentile(intensityTotal,0.75f);
			beforeAv = averageList(intensityBefore);
			before25 = percentile(intensityBefore,0.25f);
			before50 = percentile(intensityBefore,0.5f);
			before75 = percentile(intensityBefore,0.75f);
			afterAv = averageList(intensityAfter);
			after25 = percentile(intensityAfter,0.25f);
			after50 = percentile(intensityAfter,0.5f);
			after75 = percentile(intensityAfter,0.75f);			
			earlyAv = averageList(intensityEarly);
			early25 = percentile(intensityEarly,0.25f);
			early50 = percentile(intensityEarly,0.5f);
			early75 = percentile(intensityEarly,0.75f);
			lateAv = averageList(intensityLate);
			late25 = percentile(intensityLate,0.25f);
			late50 = percentile(intensityLate,0.5f);
			late75 = percentile(intensityLate,0.75f);
			velocityTotalMin = velocity.get(0);
			velocityTotalAv = averageList(velocity);
			velocityBeforeAv = averageList(velocityBefore);
			velocityAfterAv = averageList(velocityAfter);
			diameterAv = averageList(diameter);
			diameterBeforeAv = averageList(diameterBefore);
			diameterAfterAv = averageList(diameterAfter);
			
			for (int c = 0; c < nChannels ; c++) {
				ArrayList<Float> ch = new ArrayList<Float>();
				pos[2] = c + idxFirstIntensityChannel;
				for (int j = trStart ; j <= trStop; j++) {
					pos[0] = j;
					rats.setPosition(pos);		
					ch.add(rats.get().get());
				}
				otherChAv[c] = averageList(ch);
			}
						
			float[] desc = new float[] {condID,
					trackID,
					trackDuration,
					trackStart,
					trackStop,
					trackDisplacement,
					trackBurst,
					trackPeak,
					framesBefore,
					framesAfter,
					startX,
					startY,
					startZ,
					burstX,
					burstY,
					burstZ,
					stopX,
					stopY,
					stopZ,
					velocityTotalMin,
					velocityTotalAv,
					velocityBeforeAv,
					velocityAfterAv,
					displTotal,
					displBefore,
					displAfter,
					diameterAv,
					diameterBeforeAv,
					diameterAfterAv,
					totalAv,
					total25,
					total50,
					total75,
					beforeAv,
					before25,
					before50,
					before75,
					afterAv,
					after25,
					after50,
					after75,
					earlyAv,
					early25,
					early50,
					early75,
					lateAv,
					late25,
					late50,
					late75};
			
			if ((desc.length + otherChAv.length) != this.nDescriptors) {
				throw new Exception("the descriptors created dont fit the number of descriptors started. ");
			}
			
			raAnal.setPosition(new int[] {0,i});
			for (float el:desc) {
				raAnal.get().set(el);
				raAnal.fwd(0);
			}
			for (float el: otherChAv) {
				raAnal.get().set(el);
				raAnal.fwd(0);
			}
		} // end for loop for tracks
		this.analStack = analStack;
		info.tsParams.featsDescriptors = this.descriptors.toArray(new String[this.descriptors.size()]);
		mapMaximum();
		mapMinimum();
	}
	
	/*
	 * necessary to establish the range to make the .fcs file and later kde analysis
	 */
	private void mapMaximum() {  
		HashMap<String,Float> mapMaximum = new HashMap<String,Float>();
		RandomAccess<FloatType> raAnal = analStack.randomAccess();
		long[] dims = new long[2];
		analStack.dimensions(dims);
		for (int i = 0; i < dims[0]; i++) {
			DescriptiveStatistics stats = new DescriptiveStatistics();
			for (int j = 0; j < dims[1]; j++) {
				int[] pos = {i,j};
				raAnal.setPosition(pos);
				stats.addValue((double) raAnal.get().get());
			}
			Float paramMax =(float) stats.getMax();
			if (paramMax.isNaN()) {
				paramMax = 0f;
			}
			mapMaximum.put(descriptors.get(i),paramMax);
		}
		this.mapMaximum = mapMaximum;
	}
	
	private void mapMinimum() {  
		HashMap<String,Float> mapMinimum = new HashMap<String,Float>();
		RandomAccess<FloatType> raAnal = analStack.randomAccess();
		long[] dims = new long[2];
		analStack.dimensions(dims);
		for (int i = 0; i < dims[0]; i++) {
			DescriptiveStatistics stats = new DescriptiveStatistics();
			for (int j = 0; j < dims[1]; j++) {
				int[] pos = {i,j};
				raAnal.setPosition(pos);
				stats.addValue((double) raAnal.get().get());
			}
			Float paramMin =(float) stats.getMin();
			if (paramMin.isNaN()) {
				paramMin = 0f;
			}
			mapMinimum.put(descriptors.get(i),paramMin);
		}
		this.mapMinimum = mapMinimum;
	}
	
	public void syncArray() {
		int numDims = tsStack.numDimensions();
		long[] dims = new long[numDims];
		tsStack.dimensions(dims); // loads the size of each dimension into the dims array. Bit verbose, only want 1 dimension actually, the number of tracks (1)		

		int dimSync = (int) dims[0]*2 + 1;
		ImgFactory<FloatType> imgFactory = new ArrayImgFactory<>(new FloatType());
		Img<FloatType> syncStack = imgFactory.create(new int[] {dimSync,(int) dims[1]});

		// random access for each image
		RandomAccess<FloatType> raSync = syncStack.randomAccess();
		RandomAccess<FloatType> rats = tsStack.randomAccess();
		RandomAccess<FloatType> raAnal = analStack.randomAccess();
		
		int idxIntensSmooth = feats.indexOf("INTENSITY_SMOOTH");
		int idxMask = feats.indexOf("MASK");
		int idxTrackBurst = descriptors.indexOf("trackBurst");
		// for each track i...
		for (int i = 0; i < dims[1]; i++) {
			int[] analPos = {idxTrackBurst,i};  //track burst
			raAnal.setPosition(analPos);
			int offset = (int) raAnal.get().get(); 
			offset = (int) dims[0] - offset;
			int[] elabPos = {0,i,idxIntensSmooth};
			int[] syncPos = {offset,i};
			rats.setPosition(elabPos);
			raSync.setPosition(syncPos);
			for (int j = 0 ; j < (int) dims[0] ; j++) {
				rats.setPosition(idxIntensSmooth,2);
				float val = rats.get().get();
				rats.setPosition(idxMask,2);
				float mask = rats.get().get();
				raSync.get().set(val * mask);
				rats.fwd(0);
				raSync.fwd(0);
			}
		}
		this.syncStack = syncStack;
	}
	
	public void syncArrayProjection() {
		long[] dims = new long[2];
		long[] dimsProj = new long[2];
		syncStack.dimensions(dims); 
		syncStack.dimensions(dimsProj); // loads the size of each dimension into the dims array. Bit verbose, only want 1 dimension actually, the number of tracks (1)		
		dimsProj[1] = 6;
		
		ImgFactory<FloatType> imgFactory = new ArrayImgFactory<>(new FloatType());
		Img<FloatType> projStack = imgFactory.create(dimsProj);

		// random access for each image
		RandomAccess<FloatType> raSync = syncStack.randomAccess();
		RandomAccess<FloatType> raProj = projStack.randomAccess();
		
		for (int i = 0; i < dims[0]; i++) {
			ArrayList<Float> frameIntensitiesList = new ArrayList<Float>(); //track burst
			int[] pos = new int[]{i,0};
			raSync.setPosition(pos);
			for (int j = 0 ; j < (int) dims[1] ; j++){
				float val = (float) raSync.get().get();
				if (val!=0f) {
					frameIntensitiesList.add(val);
				}
				raSync.fwd(1);
			}
			float av = averageList(frameIntensitiesList);
			float median = percentile(frameIntensitiesList,0.2f);
			float std = stdList(frameIntensitiesList);
			float max = maxList(frameIntensitiesList);
			float min = minList(frameIntensitiesList);
			float counts = frameIntensitiesList.size();
			raProj.setPosition( pos );
			raProj.get().set(av);
			raProj.fwd(1);			
			raProj.get().set(median);
			raProj.fwd(1);
			raProj.get().set(std);
			raProj.fwd(1);
			raProj.get().set(max);
			raProj.fwd(1);
			raProj.get().set(min);
			raProj.fwd(1);
			raProj.get().set(counts);		
		}
		this.projStack = projStack;
	}

	private float averageList(ArrayList<Float>  avList) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Float value : avList) {
			stats.addValue((double) (float) value);
		}
		Double av = stats.getMean();
		if (Double.isNaN(av)) {
			av = 0d;
		}
		return (float) (double) av;
	}
	
	private float stdList(ArrayList<Float> stdList) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Float value : stdList) {
			stats.addValue((double) (float) value);
		}
		return (float) stats.getStandardDeviation();
	}
	
	private float maxList(ArrayList<Float>  avList) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Float value : avList) {
			stats.addValue((double) (float) value);
		}
		return (float) stats.getMax();
	}
	
	private float minList(ArrayList<Float>  avList) {
		DescriptiveStatistics stats = new DescriptiveStatistics();
		for (Float value : avList) {
			stats.addValue((double) (float) value);
		}
		return (float) stats.getMin();
	}	
	
	// issues with apache.commons3 DescriptiveStatistics while dealing with small sized samples
	private float percentile(ArrayList<Float> prList, float prc) {
		if(prList.size()<1) {
			return 0f;
		}
		int pos = (int) (prc * prList.size());		
		if (pos < 1) {
			return (float) prList.get(0);
		}
		return (float) prList.get(pos-1);
	}

	public void generateTSImage() {
		tsImagePlus = ImageJFunctions.wrapFloat(tsStack,"ts-image");
		int imageStackSize = tsImagePlus.getImageStackSize();
		if (imageStackSize == feats.size()) {
			System.out.println("adding ts slice labels");
			for (int i = 0; i < imageStackSize ; i++) {				
				tsImagePlus.getImageStack().setSliceLabel(feats.get(i),i);
			}
		}
	}	
	
	public void generateTSAImage() {
		tsaImage = datasetService.create(analStack);
		tsaImage.axis(0).setType(Axes.X);
		tsaImage.axis(1).setType(Axes.Y);
		tsaImagePlus = ImageJFunctions.wrapFloat(analStack,"tse-image");
		int imageStackSize = tsaImagePlus.getImageStackSize();
		if (imageStackSize == descriptors.size()) {
			System.out.println("adding tsa slice labels");
			for (int i = 0; i < imageStackSize ; i++) {				
				tsaImagePlus.getImageStack().setSliceLabel(descriptors.get(i),i);
			}
		}
	}	
		
	public void generateTSSImage() {
		tssImage = datasetService.create(syncStack);
		tssImage.axis(0).setType(Axes.X);
		tssImage.axis(1).setType(Axes.Y);		
		tssImagePlus = ImageJFunctions.wrapFloat(syncStack,"tss-image");
	}
	
	public void generateTSPImage() {
		tspImage = datasetService.create(projStack);	
		tspImage.axis(0).setType(Axes.X);
		tspImage.axis(1).setType(Axes.Y);
		tspImagePlus = ImageJFunctions.wrapFloat(projStack,"tsp-image");
	}
	
	public void saveAnalysisAsCsv(File output) {
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(output.getAbsoluteFile()));
			ArrayList<String> header = descriptors;
			String fullHeader = String.join(";", header);
			br.write(fullHeader);
			br.newLine();
			long[] dims = new long[2];
			analStack.dimensions(dims);
			RandomAccess<FloatType> ra = analStack.randomAccess();
			for (int i = 0;i< dims[1];i++) {
				List<String> sb = new ArrayList<String>();
				for (int j = 0; j < dims[0]; j++) {
					int[] pos = {j,i};
					ra.setPosition(pos);
					float val = ra.get().get();
					sb.add(Float.toString(val));
				}				
				br.write(String.join(";", sb));
				br.newLine();
			}
			br.close();			
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void saveProjectionAsCsv(File output) {
		try {
			BufferedWriter br = new BufferedWriter(new FileWriter(output.getAbsoluteFile()));
			String fullHeader = "frameRelativeToBurst;average;median;std;max;min;counts";
			br.write(fullHeader);
			br.newLine();
			long[] dims = new long[2];
			projStack.dimensions(dims);
			int offset = (int) dims[0]/2;
			RandomAccess<FloatType> raproj = projStack.randomAccess();
			for (int i = 0;i< dims[0];i++) {
				List<String> sb = new ArrayList<String>();
				sb.add(String.valueOf(i-offset));
				for (int j = 0; j < dims[1]; j++) {
					int[] pos = {i,j};
					raproj.setPosition(pos);
					sb.add(Float.toString(raproj.get().get()));
				}				
				br.write(String.join(";", sb));
				br.newLine();
			}
			br.close();			
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public void saveAnalysisAsFcs(File output) {
		try {
			long[] imgDims = new long[2];
			analStack.dimensions(imgDims);
			float[] imageArray = new float[(int) analStack.size()];
			Cursor<FloatType> cursor = analStack.cursor();
			int i = 0;
			while(cursor.hasNext()) {
				imageArray[i] = cursor.next().get();
				i++;
			}			
			FCSWriter.writeFcsFile(output, imageArray, imgDims, info.tsParams.featsDescriptors, mapMaximum);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public ArrayList<String> getFeatsTM() {
		return featsTM;
	}
	
	public ArrayList<String> getFeats() {
		return feats;
	}
	
	public ArrayList<String> getFeatsExpanded() {
		return featsExpanded;
	}	
	
	public ArrayList<String> getDescriptors() {
		return descriptors;
	}
	
	public int getTargetChannel() {
		return targetChannelIdx;
	}
	
	public Dataset getTSImage() {
		return tsImage;
	}
	
	public Dataset getTSAImage() {
		return tsaImage;
	}
	
	public Dataset getTSSImage() {
		return tssImage;
	}
	
	public Dataset getTSPImage() {
		return tspImage;
	}
	
	public ImagePlus getTSImagePlus() {
		return tsImagePlus;
	}
	
	public ImagePlus getTSAImagePlus() {
		return tsaImagePlus;
	}
	
	public ImagePlus getTSSImagePlus() {
		return tssImagePlus;
	}
	
	public ImagePlus getTSPImagePlus() {
		return tspImagePlus;
	}
	
	public Integer getNFrames() {
		return nFrames;
	}
	public Integer getNFeats() {
		return nFeats;
	}
	public Integer getNTracks() {
		return nTracks;
	}
	public HashMap<String, Float> getMapMaximum() {
		return this.mapMaximum;
	}
	public HashMap<String, Float> getMapMinimum() {
		return this.mapMinimum;
	}
}


//  Play with PlanarImg and other low-level images
//	List<FloatArray> ll = new ArrayList<FloatArray>();
//	for (float[] el:data) {
//		FloatArray ff = new FloatArray(el);
//		ll.add(ff);
//	}
//	PlanarImg<FloatType,FloatArray> pim = new PlanarImg<FloatType,FloatArray>(ll,dims,new Fraction());
//	FloatArray fff = pim.getPlane(1);
//	ImgPlus<FloatType> pimp = new ImgPlus<FloatType>(pim, "raw", axes);
//	Map<String, Object> props = pimp.getProperties();		
//	FloatArray result = pim.getPlane(2);
/*
 * original imports, perhaps useful some other time
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.Axis;
import net.imagej.axis.AxisType;
import net.imagej.Dataset;
import net.imagej.DatasetService;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.img.imageplus.FloatImagePlus;
import net.imglib2.img.planar.PlanarCursor;
import net.imglib2.img.planar.PlanarImg;
import net.imglib2.img.planar.PlanarImgFactory;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Fraction;
import net.imglib2.view.Views;
import net.imglib2.algorithm.convolution.kernel.Kernel1D;
import net.imglib2.algorithm.convolution.kernel.KernelConvolverFactory;
import net.imglib2.algorithm.gauss3.Gauss3;
import net.imglib2.algorithm.gradient.PartialDerivative;

import org.scijava.log.LogService;
import io.scif.services.DatasetIOService;
import io.scif.codec.CodecService;
import io.scif.formats.qt.QTJavaService;
import io.scif.formats.tiff.TiffService;
import io.scif.services.TranslatorService;
import io.scif.services.LocationService;
import io.scif.img.ImgUtilityService;
import io.scif.services.JAIIIOService;
import io.scif.img.ImgSaver;
import net.imagej.ops.OpService;
import org.scijava.plugin.PluginService;
import org.scijava.plugin.SciJavaPlugin;

import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.command.ContextCommand;
import org.joml.Math;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.app.StatusService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.ui.UIService;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.FeatureModel;
import fiji.plugin.trackmate.TrackMate;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.DialogListener;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;
*/
