package at.ac.meduniwien.trackmate.cell_impulse_response;

import javax.swing.ImageIcon;

import org.scijava.plugin.Plugin;

import fiji.plugin.trackmate.TrackMatePlugIn;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.action.TrackMateActionFactory;
import ij.ImageJ;
import ij.ImagePlus;

@Plugin( type = TrackMateActionFactory.class )
public class CellImpulseResponseActionFactory implements TrackMateActionFactory
{

	private static final String INFO_TEXT = "<html>This action analyses the track from the perspective of an impulse response. Generates new Spot, Edge and Track Features and exports the data as .csv, .tif and .fcs.</html>";

	private static final String KEY = "CELL_IMPULSE_RESPONSE";

	private static final String NAME = "Cell impulse response";

	@Override
	public String getInfoText()
	{
		return INFO_TEXT;
	}

	@Override
	public ImageIcon getIcon()
	{
		return null; // No icon for this one.
	}

	@Override
	public String getKey()
	{
		return KEY;
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public TrackMateAction create()
	{
		return new CellImpulseResponseAction();
	}

	public static void main( final String[] args )
	{
		ImageJ.main( args );
		new ImagePlus( "samples/FakeTracks.tif" ).show();
		new TrackMatePlugIn().run( "" );
	}
}
