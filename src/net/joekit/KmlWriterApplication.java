package net.joekit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.ImageIcon;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JProgressBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Main class for the application.
 * 
 * @author Ivan
 */
public class KmlWriterApplication extends JFrame 
  implements WindowListener, ActionListener{

  static {
    // Disable custom L&F.  This is a workaround for the joekit.jar
    // being unsigned, which causes a classpath loading problem on
    // MacOS.  TODO(Pablo): change build.xml jar rule to sign the jar?
    try {  
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());  
    } catch (Exception e) {  
      e.printStackTrace();  
    }
  }
  int width = 600;
  int height = 600;
  int displayPanelWidth;
  static ConsolePanel cp;
  DataDisplayPanel ddp;
  JProgressBar pb;
  JDialog dialog;
  JColorChooser colorChooser;
  
  JMenuItem credits;
  JMenuItem manual;

  KmlWriterApplication(){
    super("Joekit line list to KML conversion utility");
    if(System.getProperty("os.name").contains("Mac")){
      System.setProperty("apple.laf.useScreenMenuBar", "true");
    }
    
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int screenWidth = dim.width;
    displayPanelWidth = ((width * 2) > (screenWidth - 10))
      ? (screenWidth - width) - 10
      : height;
    makeGUI();
    
    
    JMenuBar menuBar = new JMenuBar();
    menuBar.setBackground(Color.gray);
    // Add the menubar to the frame
    setJMenuBar(menuBar);
    // Define and add two drop down menus to the menubar
    JMenu helpMenu = new JMenu("Help");
    helpMenu.setBackground(Color.gray);
    JMenu aboutMenu = new JMenu("About");
    aboutMenu.setBackground(Color.gray);
    menuBar.add(helpMenu);
    menuBar.add(aboutMenu);
    
    // Put some stuff in the menus
    credits = new JMenuItem("Credits");
    credits.addActionListener(this);
    aboutMenu.add(credits);
    manual = new JMenuItem("Joekit Manual");
    manual.addActionListener(this);
    helpMenu.add(manual);
    
    
    setLocation(0, 0);
    pack();
    setResizable(false);
    setVisible(true);
    addWindowListener(this);
  }
	
  void makeGUI(){
    Container cont = getContentPane();
    cp = new ConsolePanel(this, width, height);     
    cont.add(cp, "Center");
    ddp = new DataDisplayPanel(this, displayPanelWidth, height);
    cont.add(ddp, "East");
    pack();
    ddp.setVisible(true);
  }
	
  void createReportPopup(String reportString){
    Container cont = getContentPane();
    JOptionPane.showMessageDialog(cont, reportString, 
                                  "Something interesting", 
                                  JOptionPane.PLAIN_MESSAGE);
  }
  
  void createProgressBar(){
    pb = new JProgressBar(0, 100);
    pb.setString("Working...");
    JLabel lb = new JLabel("Progress: ");
    JPanel newPanel = new JPanel();
    newPanel.add(lb);
    newPanel.add(pb);
    dialog = new JDialog(this, "Please wait while I consider your request.");
    dialog.setPreferredSize(new Dimension(400,80));
    pb.setPreferredSize(new Dimension(300,20));
    dialog.getContentPane().add(newPanel, BorderLayout.CENTER);
    dialog.pack();
    dialog.setLocation(100,300); 
    dialog.setVisible(true);
    dialog.toFront(); // raise above other java windows
  }
  
  void setProgressBar(int i){
    pb.setValue(i);
  }
  
  void destroyProgressBar(){
    dialog.dispose();
  }
	
  void createReportPopupWithTextField(String reportString){
    Container cont = getContentPane();
    JTextArea tA = new JTextArea();
    tA.setText(reportString);
    JScrollPane sP = new JScrollPane(tA);
    sP.setPreferredSize(new Dimension(400,400));	
    sP.setLocation(100,300);
    JOptionPane.showMessageDialog(this, sP, 
                                  "Names that didn't result in points",
                                  JOptionPane.PLAIN_MESSAGE);
    cont.setVisible(true);
  }
  
  void createPopupWithLegend(ParameterSet params){
    Container cont = getContentPane();
    ImageIcon legendImageIcon = new ImageIcon(params.legendImage);
    JLabel legendLabel = new JLabel(legendImageIcon);
    legendLabel.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        int catIndex = (evt.getY()-35)/20;
        System.out.println("x: " + evt.getX() + ", y: " + evt.getY() + ", index: " + catIndex);
        // TODO make this do something about it
      }
    });
    JScrollPane sP = new JScrollPane(legendLabel);
    sP.setPreferredSize(new Dimension(legendImageIcon.getIconWidth() + 40,
        legendImageIcon.getIconHeight() > 500 ? 540 : 
          legendImageIcon.getIconHeight() + 40));   
    sP.setLocation(0,200);
    JOptionPane.showMessageDialog(this, sP, 
                                  "Categories",
                                  JOptionPane.PLAIN_MESSAGE);
    cont.setVisible(true);
  }
	
  public static void main(String [] args) {
    try {
      // Set System L&F
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (UnsupportedLookAndFeelException e) {
      // TODO(pablo)
    } catch (ClassNotFoundException e) {
    } catch (InstantiationException e) {
    } catch (IllegalAccessException e) {
    }
    new KmlWriterApplication();
  }

  private static final long serialVersionUID = 1L;

  @Override
  public void windowActivated(WindowEvent e) {  
    ddp.paintPanel();
  }

  @Override
  public void windowClosed(WindowEvent e) { 
  }

  @Override
  public void windowClosing(WindowEvent e) {   
    System.exit(0);
  }

  @Override
  public void windowDeactivated(WindowEvent e) { 
  }

  @Override
  public void windowDeiconified(WindowEvent e) {    
  }

  @Override
  public void windowIconified(WindowEvent e) {    
    ddp.paintPanel();
  }

  @Override
  public void windowOpened(WindowEvent e) {    
  }

  @Override
  public void actionPerformed(ActionEvent ea) {
    if(ea.getSource() == credits){
      createReportPopup("Joekit is brought to you by:\n\n" + 
          "Ivan Gayton \nPablo Marygundter \nRuby Siddiqui \nLudovic Dupuis\n" +
          "and the efforts, advice, and assistance of many others.\n" +
          "\n\nJoekit is free, open source software, originally intended to facilitate \n" +
          "humanitarian work.  You are free to use it for whatever you like.");
    }
    if(ea.getSource() == manual){
      createReportPopup("A manual and set of example files can be downloaded from \n\n"+
          "code.google.com/p/downloads/list");
    }
    
  }
}
