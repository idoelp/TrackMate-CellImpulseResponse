package at.ac.meduniwien.trackmate.cell_impulse_response;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ExperimentInfo {
	public static final HashMap<String,String> outputExtensions;
	public static final String DEFAULT_FILE_EXTENSION = ".ts.zip";	

	public static final String EXPERIMENT_INFO = "experiment-info";	
	public static final String SPECIMEN_INFO = "specimen-info";	
	public static final String TIME_SERIES_COMPLEX = "time-series-complex";	
	public static final String TIME_SERIES_COMPLEX_SYNC= "time-series-complex-sync";	
	public static final String ANALYSIS_COMPLEX = "analysis-complex";	
	public static final String ANALYSIS_COMPLEX_FLOW = "analysis-complex-flow";	
	public static final String ANALYSIS_COMPLEX_TABLE = "analysis-complex-table";	
	public static final String REPORT_PDF = "report-pdf";	
	public static final String REPORT_SVG= "report-svg";	
	public static final String COMPLEX_ZIP = "complex-zip";	
	public static final String REPORT_FOLDER = "reports-folder";	
	public static final String TEMP_FOLDER = "temp-folder";	
	static {
		HashMap<String,String> oe = new HashMap<String,String>();
		oe.put(EXPERIMENT_INFO, "experiment.speck.json");
		oe.put(SPECIMEN_INFO, "specimen.speck.json");
		oe.put(TIME_SERIES_COMPLEX, ".speckts.tif");
		oe.put(TIME_SERIES_COMPLEX_SYNC, ".specktss.tif");
		oe.put(ANALYSIS_COMPLEX, ".speck.tif");
		oe.put(ANALYSIS_COMPLEX_FLOW, ".speck.fcs");
		oe.put(ANALYSIS_COMPLEX_TABLE, ".speck.csv");
		oe.put(REPORT_PDF, ".speck.pdf");
		oe.put(REPORT_SVG, ".speck.svg");
		oe.put(COMPLEX_ZIP, ".speck.zip");
		oe.put(TEMP_FOLDER, "temp");
		oe.put(REPORT_FOLDER, "reports");
		outputExtensions = oe;
	}
	
	private static final Date dateNow = new Date();
	private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");	
	public static final String DEFAULT_DATE = formatter.format(dateNow);
	
	private static ArrayList<String> descriptors = new ArrayList<String>();
	private static ArrayList<String> descriptorsComplex = new ArrayList<String>();
		
	private static final String DEFAULT_KEYWORD = "1";
	public static final String DEFAULT_MASTER_CTRL_FILENAME = "_MasterCtrl";
	private static final String DEFAULT_PATH_INPUT = new java.io.File(".").getAbsolutePath();
	private static final String DEFAULT_PATH_OUTPUT = new java.io.File(".").getAbsolutePath();
	public static final String[] AVAILABLE_CHANNELS = {"1"};
	private static final String DEFAULT_TARGET_CHANNEL = "1";
	private static final String DEFAULT_REF_PARAM_NORM = "before50";
	private static final String DEFAULT_REF_PARAM_CLASSIF = "late75";
	private static final Boolean DEFAULT_HAS_CALIBRATION_FILE = false;
	
	private static final String DEFAULT_UNITS_SPATIAL = "um";
	private static final String DEFAULT_UNITS_TIME = "s";
	private static final Double DEFAULT_CALIBRATION_SPATIAL = 1d;
	private static final Double DEFAULT_CALIBRATION_TIME = 1d;
	private static final Integer DEFAULT_CHARACTERISTIC_T_FRAMES = 2;
	private static final Double DEFAULT_CHARACTERISTIC_TIME = DEFAULT_CHARACTERISTIC_T_FRAMES * DEFAULT_CALIBRATION_TIME;
	private static final Double DEFAULT_TS_SIGMA = 1d;
	private static final Double DEFAULT_KDE_PRECISION = 1d;
	private static final Double DEFAULT_NORMALIZATION_FACTOR = 1d;
	private static final Double DEFAULT_CLASSIFIER_VALUE = 1d;
	public static final Double DEFAULT_OUTLIER_MARGIN = 0.025d;
	public static final Double DEFAULT_STD_FACTOR = 2d;
	
		
	public Sources sources = new Sources();
	public Params params = new Params();
	
	public static class Sources {
		static {
			for (FeatsDescriptors ft: FeatsDescriptors.values()) {
				descriptors.add(ft.name());
			}
			for (FeatsDescriptorsComplex ft: FeatsDescriptorsComplex.values()) {
				descriptorsComplex.add(ft.name());
			}
		}
		// GUI: Init -> Controller -> [Calibration Get -> ExpParams] -> FileAssign -> Gates -> [Pool -> Norm -> Class]
		// INI defined at Initialization
		public String date = DEFAULT_DATE;
		public String[] featsDescriptors = descriptors.toArray(new String[descriptors.size()]);
		public String[] featsDescriptorsComplex = descriptorsComplex.toArray(new String[descriptorsComplex.size()]);
		public String[] featsDescriptorsFull = descriptors.toArray(new String[descriptors.size()]);
		
		// CO: controller; CA: Calibration; EP: Experiment Params; FA: File Assignation; GA: Gating; PO: Pooling; NO: Normalization; CL: Classification
		// CO defined at Controller
		public String experimentKeyword = DEFAULT_KEYWORD;
		public String pathInput = DEFAULT_PATH_INPUT;
		public String pathOutput = DEFAULT_PATH_OUTPUT;	
		public String fileExtension = DEFAULT_FILE_EXTENSION;
		// CO + PO
		public String[] conditions = new String[1]; 
		// CA defined at Read from calibration file		
		public String otherInfo = "";
		public Boolean hasCalibrationFile = DEFAULT_HAS_CALIBRATION_FILE;
		public String calibrationFile = "";
		// EP defined at Experiment Params
		public Integer numSpecimens = 1;
		public String[] specimens = new String[numSpecimens];
		public Integer numSpecimenVariables = 1;
		public String[] variables = new String[numSpecimenVariables];
		public HashMap<String,Boolean> specimenVariables = new HashMap<String,Boolean>();
		// EP + PO + GA + NO
		public HashMap<String,SpecimenInfo> specimensInfo = new HashMap<String,SpecimenInfo>();
		// FA defined at File Assignation + PO
		public String[] selectedConditions = new String[1]; 
		public Integer numSelectedConditions = 0;
		public HashMap<String,ConditionContextInfo> conditionsInfo = new HashMap<String,ConditionContextInfo>();
		public String[] negCtrlConditions = new String[1];
		// PO defined at Pooling neg ctrls
		public String masterNegCtrl = "";
		public String[] masterNegCtrlPool = new String[1];
	}
	
	public static class Params {
		
		// CO
		public String imageSettingsFile = "";
		public String targetChannel = DEFAULT_TARGET_CHANNEL;
		public String unitsSpatial = DEFAULT_UNITS_SPATIAL;
		public String unitsTime = DEFAULT_UNITS_TIME;
		public Double calibrationSpatial = DEFAULT_CALIBRATION_SPATIAL;
		public Double calibrationTime = DEFAULT_CALIBRATION_TIME;
		public Integer characteristicTFrames = DEFAULT_CHARACTERISTIC_T_FRAMES;
		public Double characteristicTime = DEFAULT_CHARACTERISTIC_TIME;
		public Double tsSigma = DEFAULT_TS_SIGMA;	
		
		// GA defined at Filters==Gating
		public Gate[] gates = new Gate[1];
		public Integer numGates = 0;
		public Double kdePrecision = DEFAULT_KDE_PRECISION;
		// NO defined at Normalization
		public String refParamNorm = DEFAULT_REF_PARAM_NORM;
		public Double normalizationFactor = DEFAULT_NORMALIZATION_FACTOR;
		public NormalizationFactorDetermination normFDet = new NormalizationFactorDetermination();
		// CL defined at Classif
		public String refParamClassif = DEFAULT_REF_PARAM_CLASSIF;
		public Double classifierValue = DEFAULT_CLASSIFIER_VALUE;
		public Double classifierValueNorm =1d;
		public ClassificationValueDetermination classValueDet = new ClassificationValueDetermination();
		

	}
	
	public static class Gate {
		public String param = "";
		public Double threshold = 0d;
		public Boolean excludeValsUnderThreshold = true;
	}

	public static class NormalizationFactorDetermination {
		public String param = "";
		public String negCtrl = "";
		public Boolean gated = false;
		public String scale = "log";
		public String method = "average";
		public Double outlierExcludeUp = DEFAULT_OUTLIER_MARGIN;
		public Double outlierExcludeLo = DEFAULT_OUTLIER_MARGIN;
	}
	
	public static class ClassificationValueDetermination {
		public String param = "";
		public String negCtrl = "";
		public Boolean gated = false;
		public String scale = "log";
		public String method = "std";
		public Double stdFactor = DEFAULT_STD_FACTOR;
		public Double outlierExcludeUp = DEFAULT_OUTLIER_MARGIN;
		public Double outlierExcludeLo = DEFAULT_OUTLIER_MARGIN;
	}
	
	public static class ConditionContextInfo {
		public String condition = "";
		public Integer conditionID = 0;
		public String conditionPath = "";
		public String specimen = "1";
		public Integer specimenID = 0;
		public Boolean isNegCtrl = false;
		public String negCtrl = "";
		public HashMap<String,String> variableValues = new HashMap<String,String>();
	}
	
	public static class SpecimenInfo {
		public String specimenName;
		public Integer specimenID;
		public String[] conditions;
		public String negCtrl;
	}
}

