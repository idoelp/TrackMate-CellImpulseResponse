package at.ac.meduniwien.trackmate.cell_impulse_response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

class FCSWriter {

	
	public static byte[] floatArray2ByteArray(float[] values){
	    ByteBuffer buffer = ByteBuffer.allocate(4 * values.length).order(ByteOrder.BIG_ENDIAN);

	    for (float value : values){
	        buffer.putFloat(value);
	    }

	    return buffer.array();
	}
	
	public static String buildTextStr(String[] descriptors,HashMap<String,Float> mapMaximum) {
		String defText = "|$BEGINDATA|%s|$ENDDATA|%s|$BEGINANALYSIS|0|$ENDANALYSIS|0|$BEGINSTEXT|0|$ENDSTEXT|0|"
				+ "$DATATYPE|F|$BYTEORD|4,3,2,1|$MODE|L|$NEXTDATA|0|$TOT|%s|$PAR|%s|";
		int paramNumber = 1;
		for (String el:descriptors) {
			Float paramRangeFloat =  1.5f * mapMaximum.get(el);
			int paramRange = (int) (float) paramRangeFloat;
			
			if (paramRange < 100) {
				paramRange = 100;
			}
			defText = defText + String.format("$P%dN|%s|$P%dB|32|$P%dE|0,0|$P%dR|%d|", paramNumber,el,paramNumber,paramNumber,paramNumber,paramRange);
			paramNumber++;
		}		
		return defText;
	}
	
	
	public static void writeFcsFile(File fl, float[] image, long[]imageDims, String[] descriptors, HashMap<String,Float> mapMaximum) throws IOException, RuntimeException {
		byte[] imageByteStream = floatArray2ByteArray(image);
		if (imageDims[0]!=descriptors.length) {
			throw new RuntimeException("the dimensions of the image and the parameters do not fit");
		}
				
		char[] bdArray = new char[] {'0','0','0','0','0','0','0','0'}; // to be added to stringText
		char[] edArray = new char[] {'0','0','0','0','0','0','0','0'}; // to be added to stringText
		char[] bdhArray = new char[]{' ',' ',' ',' ',' ',' ',' ',' '}; // to be added to header
		char[] edhArray = new char[]{' ',' ',' ',' ',' ',' ',' ',' '}; // to be added to header
		char[] ethArray = new char[]{' ',' ',' ',' ',' ',' ',' ',' '}; // to be added to header
		
		String bdStr = new String(bdArray);
		String edStr = new String(edArray);	
		String totStr = String.valueOf(imageDims[1]);
		String parStr = String.valueOf(imageDims[0]);
		String textTemplate = buildTextStr(descriptors,mapMaximum);
		
		String stringText = String.format(textTemplate,bdStr,edStr,totStr,parStr);

		//adapt beginData, endData, beginText, endText byte position
		int beginData = 58+stringText.getBytes().length;
		int endData = beginData + imageByteStream.length;
				
		String endTextStr = String.valueOf(beginData-1);
		String beginDataStr = String.valueOf(beginData);
		String endDataStr = String.valueOf(endData);

		for(int i = 0; i < endTextStr.length() ; i++) {
			ethArray[i+8-endTextStr.length()] = endTextStr.charAt(i);
		}
		for(int i = 0; i < beginDataStr.length() ; i++) {
			bdArray[i+8-beginDataStr.length()] = beginDataStr.charAt(i);
			bdhArray[i+8-beginDataStr.length()] = beginDataStr.charAt(i);
		}
		for(int i = 0; i < endDataStr.length() ; i++) {
			edArray[i+8-endDataStr.length()] = endDataStr.charAt(i);
			edhArray[i+8-endDataStr.length()] = endDataStr.charAt(i);
		}
				
		bdStr = new String(bdArray);
		edStr = new String(edArray);
		
		String textFinal = String.format(textTemplate,bdStr,edStr,totStr,parStr);		
		
		String headerStart = "FCS3.1    ";
		String headerBeginText = "      58";
		String headerEndText = new String(ethArray);
		String headerBeginData = new String(bdhArray);
		String headerEndData = new String(edhArray);
		String headerBeginAnalysis = "       0";
		String headerEndAnalysis = "       0";
		String header = headerStart + headerBeginText + headerEndText + headerBeginData + headerEndData + headerBeginAnalysis + headerEndAnalysis;
								
		FileOutputStream fos = new FileOutputStream(fl);
		try{
			fos.write(header.getBytes());
			fos.flush();
			fos.write(textFinal.getBytes());
			fos.flush();
			fos.write(imageByteStream);
			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		fos.close();

	}

}
