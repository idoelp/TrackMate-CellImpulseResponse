package at.ac.meduniwien.trackmate.cell_impulse_response;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class TSAnInfo {
	public static final String SETTINGS = "settings";
	public static final String CONDITION_INFO = "condition-info";
	public static final String TIME_SERIES = "time-series";
	public static final String TIME_SERIES_EXPANDED = "time-series-expanded";
	public static final String TIME_SERIES_SYNC = "time-series-sync";
	public static final String ANALYSIS_IMAGE = "analysis-image";
	public static final String ANALYSIS_FLOW = "analysis-flow";
	public static final String ANALYSIS_TABLE = "analysis-table";
	public static final String TRACE_IMAGE = "trace-image";
	public static final String TRACE_TABLE = "trace-table";
	public static final String ZIP = "zip";
	public static final String IMAGE_STACK = "image-stack";
	public static final String TRACKMATE_TRACKS = "trackmate-tracks";
	public static final String FOLDER_EXTRAS = "folder-extras";
	public static final HashMap<String,String> outputExtensions;
	static {
		HashMap<String,String> oe = new HashMap<String,String>();
		oe.put(SETTINGS,"settings.ts.json");
		oe.put(CONDITION_INFO, "condition.ts.json");
		oe.put(TIME_SERIES, ".ts.tif");
		oe.put(TIME_SERIES_EXPANDED, ".tse.tif");
		oe.put(TIME_SERIES_SYNC, ".tss.tif");
		oe.put(ANALYSIS_IMAGE, ".tsa.tif");
		oe.put(ANALYSIS_FLOW, ".tsa.fcs");
		oe.put(ANALYSIS_TABLE, ".tsa.csv");
		oe.put(TRACE_IMAGE, ".trace.tif");
		oe.put(TRACE_TABLE, ".trace.csv");
		oe.put(ZIP, ".ts.zip");
		oe.put(IMAGE_STACK, ".ome.tif");
		oe.put(TRACKMATE_TRACKS, ".tm.xml");
		oe.put(FOLDER_EXTRAS, "cellIntenTSAn_extras");
		outputExtensions = oe;
	}
	private static final Date dateNow = new Date();
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");	
	public static final String DEFAULT_DATE = formatter.format(dateNow);
	public static final String[] EXECUTION_MODES = {"manual", "automatic"};
	private static final String DEFAULT_EXECUTION_MODE = EXECUTION_MODES[0];
	public static final String[] PROCESS_MODES = {"single file", "batch"};
	private static final String DEFAULT_PROCESS_MODE = PROCESS_MODES[1];
	public static final String[] EXTENT_OPTIONS = {"track + tsa", "only tsa","obsolete"};
	private static final String DEFAULT_EXTENT = EXTENT_OPTIONS[0];
	private static final String DEFAULT_PARAMS_SOURCE = "";
	private static final String DEFAULT_PATH_INPUT = new java.io.File(".").getAbsolutePath();
	private static final String DEFAULT_PATH_OUTPUT = new java.io.File(".").getAbsolutePath();
	private static final String DEFAULT_FILE_EXTENSION = "ome.tif";;
	private static final Boolean DEFAULT_DISPLAY_OVERLAY = false;

	public static final String[] DIMENSION_ORDER_OPTIONS = {"XYZCT","XYZTC","XYCZT","XYCTZ","XYTZC","XYTCZ"};
	private static final String DEFAULT_DIMENSION_ORDER = "XYCZT";
	private static final Integer DEFAULT_SIZE_X = 1;
	private static final Integer DEFAULT_SIZE_Y = 1;
	private static final Integer DEFAULT_SIZE_Z = 1;
	private static final Integer DEFAULT_SIZE_C = 1;
	private static final Integer DEFAULT_SIZE_T = 1;
	private static final String DEFAULT_UNITS_X = "um";
	private static final String DEFAULT_UNITS_Y = "um";
	private static final String DEFAULT_UNITS_Z = "um";
	private static final String DEFAULT_UNITS_T = "s";
	private static final Double DEFAULT_CALIBRATION_X = 1d;
	private static final Double DEFAULT_CALIBRATION_Y = 1d;
	private static final Double DEFAULT_CALIBRATION_Z = 1d;
	private static final Double DEFAULT_CALIBRATION_T = 1d;
	private static final Integer DEFAULT_MINIMUM_T = 5;
	
	public static final Integer[] DETECTION_CHANNELS = {1};
	private static final Integer DEFAULT_DETECTION_CHANNEL = 1;
	public static final String[] DETECTORS = {"DOG_DETECTOR", "LOG_DETECTOR"};
	private static final String DEFAULT_DETECTOR = "LOG_DETECTOR";
	private static final Double DEFAULT_RADIUS = 7.0;
	private static final Boolean DEFAULT_DO_MEDIAN_FILTERING = false;
	private static final Boolean DEFAULT_DO_SUBPIXEL_LOCALIZATION = false;
	private static final Double DEFAULT_THRESHOLD = 100d;
	private static final Double DEFAULT_INITIAL_SPOT_FILTER_VALUE = DEFAULT_THRESHOLD;
	public static final String[] TRACKERS = {"SIMPLE_LAP_TRACKER","SIMPLE_FAST_LAP_TRACKER","SPARSE_LAP_TRACKER"};
	private static final String DEFAULT_TRACKER = "SPARSE_LAP_TRACKER";
	private static final Double DEFAULT_LINKING_MAX_DISTANCE = 25d;
	private static final Double DEFAULT_GAP_CLOSING_MAX_DISTANCE = 25d;
	private static final Integer DEFAULT_GAP_CLOSING_MAX_FRAME_GAP = 2;
	private static final Boolean DEFAULT_ALLOW_TRACK_SPLITTING = false;
	private static final Boolean DEFAULT_ALLOW_TRACK_MERGING = false;
	private static final Double DEFAULT_FILTER_TRACK_LENGTH = Double.valueOf(DEFAULT_MINIMUM_T);
	private static final Boolean DEFAULT_DO_ROLLING_BALL_BACKGROUND_SUBTRACTION = false;
	private static final Double DEFAULT_ROLLING_BALL_RADIUS = DEFAULT_RADIUS;
	
	private static final String DEFAULT_OPS_DESCRIPTION = " ((kA*chA) [ + | - | * | / ] (kB*chB)) + offset";
	public static final String[] ARRAY_OPS_OPTIONS = {"Add",
			"Subtract",
			"Multiply",
			"Divide"};
	public static final String[] AVAILABLE_CHANNELS = {"1"};
	private static final String DEFAULT_TARGET_CHANNEL = "1";
	private static final Integer DEFAULT_CHARACTERISTIC_T_FRAMES = 2;
	private static final Double DEFAULT_TS_SIGMA = 1d;
	
	public Sources sources = new Sources();
	public ImageParams imageParams = new ImageParams();
	public TMParams tmParams = new TMParams();
	public TSParams tsParams = new TSParams();
	public ObsoleteInfo obsoleteInfo = new ObsoleteInfo();
	
	public static class Sources {
		public String date = DEFAULT_DATE;
		public String fileExtension = DEFAULT_FILE_EXTENSION;
		public String pathInput = DEFAULT_PATH_INPUT;
		public String pathOutput = DEFAULT_PATH_OUTPUT;
		public String paramsSource = DEFAULT_PARAMS_SOURCE;
		public String[] executionModes = EXECUTION_MODES;
		public String[] processModes = PROCESS_MODES;
		public String[] extentOptions = EXTENT_OPTIONS;
		public String executionMode = DEFAULT_EXECUTION_MODE;
		public String processMode = DEFAULT_PROCESS_MODE;
		public String extent = DEFAULT_EXTENT;
		public Boolean displayOverlay = DEFAULT_DISPLAY_OVERLAY;
		public static HashMap<String,String> outputExtensions = new HashMap<String,String>();
		
		
	}
	
	public static class ImageParams {
		public String[] dimensionOrderOptions = DIMENSION_ORDER_OPTIONS;
		public String dimensionOrder = DEFAULT_DIMENSION_ORDER;
		public String[] analysedFiles;
		public Integer sizeX = DEFAULT_SIZE_X;
		public Integer sizeY = DEFAULT_SIZE_Y;
		public Integer sizeZ = DEFAULT_SIZE_Z;
		public Integer sizeC = DEFAULT_SIZE_C;
		public Integer sizeT = DEFAULT_SIZE_T;
		public Integer minimumT = DEFAULT_MINIMUM_T;
		public Double calibrationX = DEFAULT_CALIBRATION_X;
		public Double calibrationY = DEFAULT_CALIBRATION_Y;
		public Double calibrationZ = DEFAULT_CALIBRATION_Z;
		public Double calibrationT = DEFAULT_CALIBRATION_T;
		public String unitsX = DEFAULT_UNITS_X;
		public String unitsY = DEFAULT_UNITS_Y;
		public String unitsZ = DEFAULT_UNITS_Z;
		public String unitsT = DEFAULT_UNITS_T;
		public HashMap<String,AnalysedFileInfo> analysedFilesInfo;
	}
	
	public static class TMParams {
		public Integer[] detectionChannels = DETECTION_CHANNELS;
		public Integer detectionChannel = DEFAULT_DETECTION_CHANNEL;		
		public Integer sizeX = DEFAULT_SIZE_X;
		public Integer sizeY = DEFAULT_SIZE_Y;
		public Integer sizeZ = DEFAULT_SIZE_Z;
		public Integer sizeC = DEFAULT_SIZE_C;
		public Integer sizeT = DEFAULT_SIZE_T;

		public String[] detectors = DETECTORS;
		public String detector = DEFAULT_DETECTOR;
		public Double radius = DEFAULT_RADIUS;
		public Double threshold = DEFAULT_THRESHOLD;
		public Double initialSpotFilterValue = DEFAULT_INITIAL_SPOT_FILTER_VALUE;
		public Boolean doSubpixelLocalization = DEFAULT_DO_SUBPIXEL_LOCALIZATION;
		public Boolean doMedianFiltering = DEFAULT_DO_MEDIAN_FILTERING;

		public String[] trackers = TRACKERS;
		public String tracker = DEFAULT_TRACKER;
		public Double linkingMaxDistance = DEFAULT_LINKING_MAX_DISTANCE;
		public Double gapClosingMaxDistance = DEFAULT_GAP_CLOSING_MAX_DISTANCE;
		public Integer gapClosingMaxFrameGap = DEFAULT_GAP_CLOSING_MAX_FRAME_GAP;
		public Boolean allowTrackSplitting = DEFAULT_ALLOW_TRACK_SPLITTING;
		public Boolean allowTrackMerging = DEFAULT_ALLOW_TRACK_MERGING;
		
		public Double filterTrackLength = DEFAULT_FILTER_TRACK_LENGTH;
		
		public Boolean doRollingBallBackgroundSubtraction = DEFAULT_DO_ROLLING_BALL_BACKGROUND_SUBTRACTION;
		public Double rollingBallRadius = DEFAULT_ROLLING_BALL_RADIUS;
	}
	
	public static class TSParams {
		public String[] availableChannels = AVAILABLE_CHANNELS;
		public String targetChannel = DEFAULT_TARGET_CHANNEL;
		public Integer characteristicTFrames = DEFAULT_CHARACTERISTIC_T_FRAMES;
		public Double tsSigma = DEFAULT_TS_SIGMA;
		public String[] arrayOps = ARRAY_OPS_OPTIONS;
		public HashMap<String,CompositeChannelInfo> compositeChannelsInfo;
		public String[] feats;
		public String[] featsTracks;		
		public String[] featsExpanded;
		public String[] featsDescriptors;
		public String operationDescription = DEFAULT_OPS_DESCRIPTION;
	}
	
	public static class AnalysedFileInfo {
		public String fileName;
		public String path; // the zip file path
		public String pathInfoJson; //inside the zip file...
		public String pathTSImage;
		public String pathTSSImage;
		public String pathTSPImage;
		public String pathTraceCsv;
		public String pathTSAImage;
		public String pathTSAFcs;
		public String pathTSACsv;
		public Integer sizeX;
		public Integer sizeY;
		public Integer sizeZ;
		public Integer sizeC;
		public Integer sizeT;
		public Double calibrationX;
		public Double calibrationY;
		public Double calibrationZ;
		public Double calibrationT;
		public String unitsX;
		public String unitsY;
		public String unitsZ;
		public String unitsT;
		public String[] slicesLabels;
		public Integer nTracks;
		public Integer nFrames;
		public Integer nFeats;
		public HashMap<String, Float> mapMaximum;
		public HashMap<String, Float> mapMinimum;
	}
	
	public static class CompositeChannelInfo {
		public String compCh;
		public String channelA;
		public String channelB;
		public String operation;
		public Double kA;
		public Double kB;
		public Double offset;		
	}
	
	public static class ObsoleteInfo {
		public Integer nFrames;
		public Integer nTracks;
		public Float[] trackIDArray;
		public Integer nChannelsAcquired;
		public String[] channelsAcquired;
		public Integer nAvailableChannels;	
		public String[] availableChannels;
		public String targetChannel;
		public Integer characteristicTFrames;
		public Double tsSigma;
		public HashMap<String,CompositeChannelInfo> compositeChannelsInfo;
		public String[] fileHeader;
		public String[] feats;
		public String[] featsTracks;
		public String[] featsExpanded;
		public String[] descriptors;
		public HashMap<String, Float> mapMaximum;	
	}
}

