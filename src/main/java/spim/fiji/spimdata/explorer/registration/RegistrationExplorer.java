package spim.fiji.spimdata.explorer.registration;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;

import mpicbg.spim.data.generic.AbstractSpimData;
import mpicbg.spim.data.generic.XmlIoAbstractSpimData;
import mpicbg.spim.data.generic.sequence.BasicViewDescription;
import mpicbg.spim.data.generic.sequence.BasicViewSetup;
import spim.fiji.spimdata.explorer.SelectedViewDescriptionListener;
import spim.fiji.spimdata.explorer.ViewSetupExplorer;

public class RegistrationExplorer< AS extends AbstractSpimData< ? >, X extends XmlIoAbstractSpimData< ?, AS > >
	implements SelectedViewDescriptionListener< AS >
{
	final String xml;
	final JFrame frame;
	final RegistrationExplorerPanel panel;
	final ViewSetupExplorer< AS, X > viewSetupExplorer;
	
	public RegistrationExplorer( final String xml, final X io, final ViewSetupExplorer< AS, X > viewSetupExplorer )
	{
		this.xml = xml;
		this.viewSetupExplorer = viewSetupExplorer;

		frame = new JFrame( "Registration Explorer" );
		panel = new RegistrationExplorerPanel( viewSetupExplorer.getPanel().getSpimData().getViewRegistrations(), this );
		frame.add( panel, BorderLayout.CENTER );

		frame.setSize( panel.getPreferredSize() );

		frame.pack();
		frame.setVisible( true );
		
		// Get the size of the screen
		final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

		// Move the window
		frame.setLocation( ( dim.width - frame.getSize().width ) / 2, ( dim.height - frame.getSize().height ) * 3 / 4  );

		// this call also triggers the first update of the registration table
		viewSetupExplorer.addListener( this );
	}

	public JFrame frame() { return frame; }

	@Override
	public void save() {}

	@Override
	public void seletedViewDescription( final BasicViewDescription<? extends BasicViewSetup> viewDescription )
	{
		panel.updateViewDescription( viewDescription );
	}

	@Override
	public void quit()
	{
		frame.setVisible( false );
		frame.dispose();
	}

	public RegistrationExplorerPanel panel() { return panel; }

	@Override
	public void updateContent( final AS data )
	{
		panel.getTableModel().update( data.getViewRegistrations() );
		panel.getTableModel().fireTableDataChanged();
	}
}
