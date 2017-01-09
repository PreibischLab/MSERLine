package spim.fiji.spimdata.explorer.popup;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import mpicbg.spim.data.SpimData;
import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.sequence.AbstractSequenceDescription;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.io.IOFunctions;
import spim.fiji.plugin.Specify_Calibration;
import spim.fiji.plugin.Specify_Calibration.Cal;
import spim.fiji.spimdata.explorer.ViewSetupExplorerPanel;

public class SpecifyCalibrationPopup extends JMenuItem implements ViewExplorerSetable
{
	private static final long serialVersionUID = 5234649267634013390L;
	public static boolean showWarning = true;

	ViewSetupExplorerPanel< ? extends AbstractSpimData< ? extends AbstractSequenceDescription< ?, ?, ? > >, ? > panel;

	public SpecifyCalibrationPopup()
	{
		super( "Specify Calibration ..." );

		this.addActionListener( new MyActionListener() );
	}

	@Override
	public JMenuItem setViewExplorer( final ViewSetupExplorerPanel< ? extends AbstractSpimData< ? extends AbstractSequenceDescription< ?, ?, ? > >, ? > panel )
	{
		this.panel = panel;
		return this;
	}

	public class MyActionListener implements ActionListener
	{
		@Override
		public void actionPerformed( final ActionEvent e )
		{
			if ( panel == null )
			{
				IOFunctions.println( "Panel not set for " + this.getClass().getSimpleName() );
				return;
			}

			final List< ViewId > viewIds = panel.selectedRowsViewId();

			final ArrayList< Cal > calibrations = Specify_Calibration.findCalibrations( panel.getSpimData(), viewIds );

			final Cal maxCal = Specify_Calibration.mostPresentCal( calibrations );

			if ( !Specify_Calibration.queryNewCal( calibrations, maxCal ) )
				return;

			if ( SpimData.class.isInstance( panel.getSpimData() ) )
			{
				Specify_Calibration.applyCal( maxCal, (SpimData)panel.getSpimData(), viewIds );
	
				if ( showWarning )
				{
					JOptionPane.showMessageDialog(
							null,
							"The calibration was set, but this is not reflected in the transformations yet (Click 'Info' Button). If you want to\n"
							+ "do so, please call 'Apply Transformation' and use the image calibration as basis for transformations." );
	
					showWarning = false;
				}
			}
			else
			{
				JOptionPane.showMessageDialog(
						null,
						"Applying the calibration is not supported for '" + panel.getSpimData().getClass().getSimpleName() + "', needs to extend SpimData." );
			}
		}
	}
}
