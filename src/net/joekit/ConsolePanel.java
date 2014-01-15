package net.joekit;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Lefthand side of the UI.
 * Contains a lot of the program logic in the methods triggered
 * by the action listeners.
 * 
 * @author Ivan
 */
public class ConsolePanel extends JPanel implements ActionListener, ChangeListener {
    //Dummy value to quiet serialization checks.  
  private static final long serialVersionUID = 1L;

    // Allows access to the main class, including methods for launching dialogs
  private KmlWriterApplication 	kwa;
    // Ingests a file, stores it in memory, and identifies some file attributes
  public ListFileReader lfr;
    // Creates the actual KML file when the output button is pressed
  public NaiveKmlFileWriter kfw;
    // Basically a data structure with attributes of the input file and desired output
  public ParameterSet params;
    // GUI default dimensions
  private int pWidth, pHeight;

  /* Buttons to choose files the program will deal with: input, output, icon (pic), and
   * gazetteer. */
  protected JButton inputFilenameChooserButton, outputFilenameChooserButton,
    iconFilenameChooserButton, gazetteerFilenameChooserButton;

  /* Columns and their associated dropdown boxes (user can define
   * which columns control what attribute of the eventual output file). 
   * */
  protected WideComboBox latField, longField, labelField, nameField, sizeField, colorField,
    startDateField, endDateField;
  protected JLabel latL, longL, labelL, sizeL, colorL, startDateL, endDateL;
 
  /* Controls for user-selectable attributes of output file */
  protected JSpinner labelSizeSp, defaultIconSizeSp, iconSizeFactorSp, daysPerEventSp,
    maxIconColorValue, minIconColorValue, midIconColorValue;
  protected SpinnerNumberModel labelSizeModel, defaultIconSizeModel,
    iconSizeFactorModel, daysPerEventModel, maxIconColorValueModel,
    minIconColorValueModel, midIconColorValueModel;

  protected JCheckBox animateCB, variableIconSizeCB, variableIconColorCB,
    gazetteerExistsCB, noEndDateCB, labelCB, logarithmicIconSizeCB, categoriesCB,
    legendCB; 

  /* Labels and other informational or formatting stuff to organize
   * the GUI. */
  protected JLabel labelSizeL, defaultIconSizeL, iconSizeFactorL, maxIconColorValueL,
    minIconColorValueL, midIconColorValueL;
  protected JLabel identifyLatAndLongColumns;
  protected JTextField inputFilenameText, outputFilenameText, iconFilenameText,
    gazetteerFilenameText;
  protected JSeparator separator, separator1, separator2, separator3, separator4, separator5, 
    separator6, separator7;
	
  // File and color chooser dialogs
  protected JFileChooser fileChooser;
  protected JColorChooser defaultIconColorChooser;
  protected JColorChooser middleIconColorChooser;
  protected JColorChooser maxIconColorChooser;
  protected JColorChooser labelColorChooser;
	
    //buttons to trigger chooser diaogs
  protected JButton defaultIconColorChooserButton, middleIconColorChooserButton, 
    maxIconColorChooserButton, labelColorChooserButton,
    previewButton, createKmlButton, createKmzButton;
  
  protected GroupLayout layout;
	
  public ConsolePanel(KmlWriterApplication kmwr, int width, int height) {
    kwa = kmwr;
    // Initialize a ParameterSet
    params = new ParameterSet();
    // sets the dimensions and creates & shows the panel 
    pWidth = width;
    pHeight = height;
    setPreferredSize(new Dimension(pWidth, pHeight));
    setFocusable(true);
    requestFocus();
    
    // Calls the methods that create and lay out the widgets    
    createTheWidgetInstances();
    doTheLayout();
  }

    /* Listens for user input and responds thereto. When the required 
     * action is short, the response code is right in the listener method.
     * When the response needs to be more elaborate, the listener calls
     *  one of the methods below */
  @Override
  public void actionPerformed(ActionEvent e) {	
    if (e.getSource() == inputFilenameChooserButton) {
      setInputFile();
    }
    if (e.getSource() == outputFilenameChooserButton) {
      setOutputFile();
    }
    if (e.getSource() == iconFilenameChooserButton) {
      setIconFile();
    }
    if (e.getSource() == legendCB){
      params.legendShouldBeDisplayed = legendCB.isSelected();
      if(legendCB.isSelected()){
        @SuppressWarnings("unused")
        LegendMaker legendMaker = new LegendMaker(params);
        kwa.createPopupWithLegend(params);
      }
    }
    
    if (e.getSource() == categoriesCB){
      params.colorCategories = categoriesCB.isSelected();
      legendCB.setEnabled(params.colorCategories);
      if(legendCB.isSelected()){
        params.colorCategories = categoriesCB.isSelected();
      }
      if(categoriesCB.isSelected()){
        params.setUpNonNumericalCategories();
        @SuppressWarnings("unused")
        LegendMaker legendMaker = new LegendMaker(params);
        kwa.createPopupWithLegend(params);
        refreshDisplayPanel();
      }else{
        //TODO stop use of categories
      }
    }
    if (e.getSource() == defaultIconColorChooserButton) {
      params.defaultIconColor =
        JColorChooser.showDialog(defaultIconColorChooser, 
                                 "choose a color for the icon",
                                 params.defaultIconColor);
      defaultIconColorChooserButton.setForeground(params.defaultIconColor);
    }
    if (e.getSource() == middleIconColorChooserButton) {
      params.middleIconColor =
        JColorChooser.showDialog(defaultIconColorChooser, 
                                 "choose a color for the icon",
                                 params.middleIconColor);
      middleIconColorChooserButton.setForeground(params.middleIconColor);
    }
    if (e.getSource() == maxIconColorChooserButton) {
      params.maxIconColor =
        JColorChooser.showDialog(defaultIconColorChooser,
                                 "choose a color for the icon",
                                 params.maxIconColor);
      maxIconColorChooserButton.setForeground(params.maxIconColor);
    }
    if (e.getSource() == labelColorChooserButton) {
      params.labelColor =
        JColorChooser.showDialog(labelColorChooser, 
                                 "choose a color for the labels",
                                 params.labelColor);
      labelColorChooserButton.setForeground(params.labelColor);
    }
    
    if(e.getSource() == labelCB){
      if(labelCB.isSelected()){
        labelColorChooserButton.setEnabled(true);
        labelL.setEnabled(true);
        labelSizeL.setEnabled(true);
        labelSizeSp.setEnabled(true);
        params.labelSize = labelSizeModel.getNumber().doubleValue();
      }else{
        params.labelSize = 0;
        labelColorChooserButton.setEnabled(false);
        labelL.setEnabled(false);
        labelSizeL.setEnabled(false);
        labelSizeSp.setEnabled(false);
      }
    }
		
    if (e.getSource() == latField) {
      params.inputLatC = latField.getSelectedIndex();
      params.latColumnExists = true;
    }
    if (e.getSource() == longField) {
      params.inputLongC = longField.getSelectedIndex();
      params.longColumnExists = true;
    }
    if (e.getSource() == labelField) {
      params.inputLabelC = labelField.getSelectedIndex();
    }
    if (e.getSource() == nameField) {
      params.inputNameC = nameField.getSelectedIndex();
    }
    if (e.getSource() == startDateField) {
      params.inputStartDateC = startDateField.getSelectedIndex();
      if(startDateField.getSelectedItem() == "Epi week"){
        params.hasEpiWeek = true;
        daysPerEventModel.setValue(7);
        startDateField.setSelectedIndex(lfr.epiWeekC);
        params.inputEpiWeekC = lfr.epiWeekC;
      }else{
        params.hasEpiWeek = false;
      }
    }
    if (e.getSource() == endDateField) {
      params.inputEndDateC = endDateField.getSelectedIndex();
    }
    
    if (e.getSource() == colorField) {
      params.inputColorC = colorField.getSelectedIndex();
      if (colorField.getSelectedItem() == "Instances") {
        params.instancesCount = true;
        params.colorByInstances = true;
        if(!params.instanceCountCreated){
          params.createInstanceCount();
        }
      } else {
        if(!params.sizeByInstances){
          // then neither size nor color use instances, so:
          params.instancesCount = false; 
        }
        params.colorByInstances = false;
      }
      if(variableIconColorCB.isSelected()){
        setUpTheColors();
      }
      if(params.colorCategories){
        params.setUpNonNumericalCategories();
        @SuppressWarnings("unused")
        LegendMaker legendMaker = new LegendMaker(params);
        kwa.createPopupWithLegend(params);
        refreshDisplayPanel();
      }
      //refreshDisplayPanel(); // maybe we should do this?
    }
    
    if (e.getSource() == sizeField) {
      params.inputSizeC=sizeField.getSelectedIndex();
      if (sizeField.getSelectedItem() == "Instances") {
        params.instancesCount = true;
        params.sizeByInstances = true;
        if(!params.instanceCountCreated){
          params.createInstanceCount();
        }
        params.sizeC = params.instancesC;
      } else {
        if(!params.colorByInstances){
          // then neither size nor color use instances, so:
          params.instancesCount = false; 
        }
        params.sizeByInstances = false;
        params.sizeC = params.inputSizeC;
        //TODO set size column back to input size column?
      }
      //this.refreshDisplayPanel(); // maybe we should do this?
    }
		
    if (e.getSource() == animateCB) {
      labelCB.setSelected(false);
      params.labelSize = 0;
      labelColorChooserButton.setEnabled(false);
      labelL.setEnabled(false);
      labelSizeL.setEnabled(false);
      labelSizeSp.setEnabled(false);
      params.animate = animateCB.isSelected();
      startDateL.setEnabled(animateCB.isSelected());
      startDateField.setEnabled(animateCB.isSelected());
      endDateL.setEnabled((!animateCB.isSelected()) ? false : !noEndDateCB.isSelected());
      endDateField.setEnabled((!animateCB.isSelected()) ? false : !noEndDateCB.isSelected());
      noEndDateCB.setEnabled(animateCB.isSelected());
      daysPerEventSp.setEnabled((!animateCB.isSelected()) ? false : noEndDateCB.isSelected());
      
      // if we're doing instances and want to start animating we need to redo the instance list
      if(params.instancesCount){
        params.createInstanceCount();
        if(params.colorByInstances){
          setUpTheColors();
        }
      }
      refreshDisplayPanel();
    }
    
    if (e.getSource() == noEndDateCB) {
      endDateL.setEnabled(!noEndDateCB.isSelected());
      endDateField.setEnabled(!noEndDateCB.isSelected());
      daysPerEventSp.setEnabled(noEndDateCB.isSelected());
      params.endDateExists = !noEndDateCB.isSelected();
    }
    
    if (e.getSource() == variableIconSizeCB) {
      params.variableIconSize = variableIconSizeCB.isSelected();
      iconSizeFactorL.setEnabled(variableIconSizeCB.isSelected());
      iconSizeFactorSp.setEnabled(variableIconSizeCB.isSelected());
      logarithmicIconSizeCB.setEnabled(variableIconSizeCB.isSelected());
      sizeL.setEnabled(variableIconSizeCB.isSelected());
      sizeField.setEnabled(variableIconSizeCB.isSelected());
      
      if(variableIconSizeCB.isSelected() == false){
        params.sizeByInstances = false;
        if(params.colorByInstances == false){
          // then neither size nor color use instances, so:
          params.instancesCount = false;
        }
      }
      if(variableIconSizeCB.isSelected() == true && sizeField.getSelectedItem() == "Instances"){
        params.sizeByInstances = true;
        params.instancesCount = true;
      }
      if(params.instancesCount){
        params.createInstanceCount();
      }
    }
    
    if(e.getSource() == logarithmicIconSizeCB){
      params.logarithmicSize = logarithmicIconSizeCB.isSelected();
      refreshDisplayPanel();
    }
    
    if (e.getSource() == variableIconColorCB) {
      params.variableIconColor = variableIconColorCB.isSelected();
      if(categoriesCB.isSelected()){
        legendCB.setEnabled(params.variableIconColor);
      }
      if(legendCB.isSelected()){
        params.colorCategories = variableIconColorCB.isSelected();
      }
      middleIconColorChooserButton.setEnabled(variableIconColorCB.isSelected());
      maxIconColorChooserButton.setEnabled(variableIconColorCB.isSelected());
      colorL.setEnabled(variableIconColorCB.isSelected());
      colorField.setEnabled(variableIconColorCB.isSelected());
      categoriesCB.setEnabled(variableIconColorCB.isSelected());
      maxIconColorValueL.setEnabled(variableIconColorCB.isSelected());
      minIconColorValueL.setEnabled(variableIconColorCB.isSelected());
      midIconColorValueL.setEnabled(variableIconColorCB.isSelected());
      maxIconColorValue.setEnabled(variableIconColorCB.isSelected());
      minIconColorValue.setEnabled(variableIconColorCB.isSelected());
      midIconColorValue.setEnabled(variableIconColorCB.isSelected());
      
   // don't bother with instances if neither color nor size varies by it
      if(variableIconColorCB.isSelected() == false){
        params.colorByInstances = false;
        if(params.sizeByInstances = false){
          // then neither size nor color use instances, so:
          params.instancesCount = false;
        }
      }
      if(variableIconColorCB.isSelected() == true && colorField.getSelectedItem() == "Instances"){
        params.colorByInstances = true;
        params.instancesCount = true;
      }
      if(params.instancesCount){
        params.createInstanceCount();
      }
      if(this.variableIconColorCB.isSelected()){
        setUpTheColors();
      }
    }
    
    if (e.getSource() == gazetteerExistsCB) {
      gazetteerFilenameChooserButton.setEnabled(gazetteerExistsCB.isSelected());
      gazetteerFilenameText.setEnabled(gazetteerExistsCB.isSelected());
      if (!gazetteerExistsCB.isSelected()) {
        params.gazetteerExists = false;
        params.setOutputFileToInputDataSet();
      }else{
        setGazetteerFile();
      }
      identifyLatAndLongColumns.setEnabled(!gazetteerExistsCB.isSelected());
      latL.setEnabled(!gazetteerExistsCB.isSelected());
      longL.setEnabled(!gazetteerExistsCB.isSelected());
      latField.setEnabled(!gazetteerExistsCB.isSelected());
      longField.setEnabled(!gazetteerExistsCB.isSelected());
    }

    if (e.getSource()== gazetteerFilenameChooserButton) {
      setGazetteerFile();
    }

    if (e.getSource() == previewButton) {
      /*TODO what if the icon file string is a URL? 
      Assuming we want things to work offline, perhaps 
      in this case we should use a placeholder image?*/
      try{
        params.iconFile = new File(iconFilenameText.getText());
        if(params.iconFile.length() > 0){
          params.iconIsImageFile = true;
          params.iconFileName = params.iconFile.getAbsolutePath();
        }else{
          params.iconIsImageFile = false;
        }
      }catch(Exception exc){
        exc.printStackTrace();
      }
      kwa.createProgressBar();
      refreshDisplayPanel();
      kwa.destroyProgressBar();
    }

    if (e.getSource() == createKmlButton) {
      System.out.println("Is the icon an image file? " + params.iconIsImageFile);
      if(params.iconIsImageFile){
        params.iconFile = new File(iconFilenameText.getText());
        params.iconFileName = params.iconFile.getAbsolutePath();
      }
      kwa.createProgressBar();
      if(params.instancesCount && !params.instanceCountCreated){
        params.createInstanceCount();
      }
      if(!params.instancesCount){
        params.setColumnNumbersToInputColumnNumbers();
      }
      params.setMaxMins();
      params.isKmzRatherThanKml = false;
      params.outputFile = new File(outputFilenameText.getText());
      //params.iconFile = new File(iconFilenameText.getText());
      //params.iconFileName = iconFilenameText.getText();
      kfw = new NaiveKmlFileWriter(params);
      String toReport =
        "That should have created a .kml file in\n"
        + params.outputFile.getAbsolutePath()
        + "\n\nPlease check for your map file.";
      kwa.destroyProgressBar();
      kwa.createReportPopup(toReport);
    }
    
    if (e.getSource() == createKmzButton) {
      System.out.println("Is the icon an image file? " + params.iconIsImageFile);
      if(params.iconIsImageFile){
        params.iconFile = new File(iconFilenameText.getText());
        params.iconFileName = params.iconFile.getAbsolutePath();
      }
      kwa.createProgressBar();
      if(params.instancesCount && !params.instanceCountCreated){
        params.createInstanceCount();
      }
      if(!params.instancesCount){
        params.setColumnNumbersToInputColumnNumbers();
      }
      params.setMaxMins();
      // TODO change the extension of the output file to .kmz
      params.isKmzRatherThanKml = true;
      params.outputFile = new File(outputFilenameText.getText());
      //params.iconFile = new File(iconFilenameText.getText());
      //params.iconFileName = iconFilenameText.getText();
      kfw = new NaiveKmlFileWriter(params);
      String toReport =
        "That should have created a .kmz file in\n"
        + params.outputFile.getAbsolutePath()
        + "\n\nPlease check for your map file.";
      kwa.destroyProgressBar();
      kwa.createReportPopup(toReport);
    }
  }
	
  @Override
  public void stateChanged(ChangeEvent e) {
    if(e.getSource() == defaultIconSizeModel) {
      params.defaultIconSize = defaultIconSizeModel.getNumber().doubleValue();
    }
    if(e.getSource() == iconSizeFactorModel) {
      params.iconSizeFactor = iconSizeFactorModel.getNumber().doubleValue();
    }
    if(e.getSource() == labelSizeModel) {
      params.labelSize = labelSizeModel.getNumber().doubleValue();
    }
    if(e.getSource() == daysPerEventModel) {
      params.daysPerEvent = daysPerEventModel.getNumber().intValue();
    }
    if(e.getSource() == maxIconColorValueModel) {
      params.colorMax = maxIconColorValueModel.getNumber().doubleValue();
    }
    if(e.getSource() == minIconColorValueModel) {
      params.colorMin = minIconColorValueModel.getNumber().doubleValue();
    }
    if(e.getSource() == midIconColorValueModel) {
      params.colorMid = midIconColorValueModel.getNumber().doubleValue();
    }
  }
	
  @SuppressWarnings("unchecked")
  private void setInputFile() {
    int returnVal = fileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      kwa.createProgressBar();
      params = new ParameterSet(); // Resets all parameters 
      
      /*TODO Reset the GUI to match the newly reset parameters
       * In general Joekit reacts badly to restarting by hitting
       * the Input button and loading a new file (the old settings
       * persist in some parts of the GUI and the parameters.
       * We should either disable the input button, or really fix
       * the way in which it resets itself when a new input file 
       * is chosen.
       */
      
      params.inputFile = fileChooser.getSelectedFile();
      inputFilenameText.setText(params.inputFile.getAbsolutePath());
 
      //Set the output file by default to the same as the input file (can be changed)
      int extensionIndex = params.inputFile.getAbsolutePath().lastIndexOf(".");
      if (extensionIndex == 0) {
        extensionIndex = params.inputFile.getAbsolutePath().length();
      }
      outputFilenameText.setText(params.inputFile.getAbsolutePath().substring(0, extensionIndex)
                                 + " map" + ".kml");
      params.outputFile = new File(outputFilenameText.getText());

      lfr = new ListFileReader(params.inputFile, params);
      kwa.setProgressBar(50);

      /* Clear out the existing items in all of the dropdown boxes (will be the defaults
       * if this is the first time the user is choosing an inputfile*/
      latField.removeAllItems();
      longField.removeAllItems();
      labelField.removeAllItems();
      nameField.removeAllItems();
      sizeField.removeAllItems();
      colorField.removeAllItems();
      startDateField.removeAllItems();
      endDateField.removeAllItems();

      // Populate all of the dropdown boxes with the list of column headers of the inputfile 
      for (String item : params.inputHeaders) {
        latField.addItem(item);
        longField.addItem(item);
        labelField.addItem(item);
        nameField.addItem(item);
        sizeField.addItem(item);
        colorField.addItem(item);
        startDateField.addItem(item);
        endDateField.addItem(item);
      }
      // Add a final item to size and color dropdown boxes: instances
      sizeField.addItem("Instances");
      colorField.addItem("Instances");

      /* Sets the dropdown boxes to a guess at the right default values based on column header
       *names*/ 
      latField.setSelectedIndex(lfr.latC);
      longField.setSelectedIndex(lfr.longC);
      labelField.setSelectedIndex(lfr.labelC);
      nameField.setSelectedIndex(lfr.nameC);
      sizeField.setSelectedIndex(lfr.sizeC);
      colorField.setSelectedIndex(lfr.colorC);
      if(lfr.hasEpiWeek){
        System.out.println("thar be epi-weeks, matey");
        startDateField.setSelectedIndex(lfr.epiWeekC);
        params.hasEpiWeek = true;
        params.inputEpiWeekC = lfr.epiWeekC;
        this.daysPerEventModel.setValue(7);
      }else{
        startDateField.setSelectedIndex(lfr.startDateC);
      }
      
      // Enables some of the greyed-out stuff in the GUI now that there's an inputfile
      outputFilenameChooserButton.setEnabled(true);
      outputFilenameText.setEnabled(true);
      identifyLatAndLongColumns.setEnabled(true);
      latL.setEnabled(true);
      longL.setEnabled(true);
      latField.setEnabled(true);
      longField.setEnabled(true);
      labelL.setEnabled(true);
      labelField.setEnabled(true);
      defaultIconSizeL.setEnabled(true);
      defaultIconSizeSp.setEnabled(true);
      variableIconSizeCB.setEnabled(true);
      variableIconColorCB.setEnabled(true);
      defaultIconColorChooserButton.setEnabled(true);
      //legendCB.setEnabled(true);
      animateCB.setEnabled(true);
      labelColorChooserButton.setEnabled(true);
      labelCB.setEnabled(true);
      labelSizeL.setEnabled(true);
      labelSizeSp.setEnabled(true);
      gazetteerExistsCB.setEnabled(true);
      nameField.setEnabled(true);
      previewButton.setEnabled(true);
      iconFilenameChooserButton.setEnabled(true);
      iconFilenameText.setEnabled(true);
      createKmlButton.setEnabled(true);
      createKmzButton.setEnabled(true);
 
      params.inputStartDateC = startDateField.getSelectedIndex();
      
      /* Check to see if lat and long columns are set; will have happened
       * automatically if there are columns named appropriately for lat and long.
       * Don't know if this is necessary now...*/
      
      // TODO use booleans instead of this clumsy check
      
      if((lfr.latC>0 || lfr.longC>0) && lfr.latC != lfr.longC){
        params.longColumnExists = true;
        params.latColumnExists = true;
        //System.out.println("found lat and long columns");
      }else{
        params.longColumnExists = false;
        params.latColumnExists = false;
        //System.out.println("didn't find lat and long columns");
        }
      if(params.gazetteerExists){
        GazetteerFileLinker gzf = new GazetteerFileLinker(params);
        gzf.link();
        if(gzf.orphanList.length() > 0){
          kwa.createReportPopupWithTextField(gzf.orphanList);
        }
      }else{
        params.dataSetLinked = false;
      }
      refreshDisplayPanel();

      /* TODO create a progress tracking system that actually knows how
       * far into the input file we are. It should be possible to track 
       * where we are in the file, even if just by brute force - how many
       * bytes is the file and how many have we processed.  With the current
       * non-system (this progress bar doesn't actually connect to any 
       * quantity related to progress, users are sitting in front of an 
       * apparently frozen program.
       */
      kwa.setProgressBar(100);
      kwa.destroyProgressBar();
    } else {
      System.out.println("Open command cancelled by user.");
    }
  }

  private void setOutputFile() {
    /* Only needs to be done if the user doesn't want the default filename, which is
     * simply the input filename plus the word "map" with extension .kml instead of whatever
     * the input file's native extension was.*/
    FileNameExtensionFilter filter = new FileNameExtensionFilter("KML file","kml");
    fileChooser.setFileFilter(filter);
    File f = new File(params.outputFile.getAbsolutePath());
    fileChooser.setSelectedFile(f);
    int returnVal = fileChooser.showSaveDialog(this);
    fileChooser.removeChoosableFileFilter(filter);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      params.outputFile = fileChooser.getSelectedFile();
      String fnm = params.outputFile.getAbsolutePath();
      int dotAt = fnm.lastIndexOf(".");
      String fnmExtension = fnm.substring(dotAt+1);
      if(!fnmExtension.equalsIgnoreCase("kml")){
        params.outputFile = new File(fnm.concat(".kml"));
      }
      System.out.println("Saving: " + params.outputFile.getAbsolutePath());
      outputFilenameText.setText(params.outputFile.getAbsolutePath());
      outputFilenameText.setEnabled(true);
      createKmlButton.setEnabled(true);
      createKmzButton.setEnabled(true);
    } else {
      System.out.println("Open command cancelled by user.");
    }
  }

  private void setGazetteerFile() {
    int returnVal = fileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      kwa.createProgressBar();
      params.gazetteerFile = fileChooser.getSelectedFile();
      gazetteerFilenameText.setText(params.gazetteerFile.getAbsolutePath());
      params.gazetteerExists = true;
      GazetteerFileLinker gzf = new GazetteerFileLinker(params);
      gzf.link();
      if(gzf.orphanList.length() > 0){
        kwa.createReportPopupWithTextField(gzf.orphanList);
      }
      refreshDisplayPanel();
      kwa.setProgressBar(100);
      kwa.destroyProgressBar();
    } else {
      System.out.println("Open command cancelled by user.");
    }
  }

  private void setUpTheColors() {
    if (params.colorByInstances) {
      params.colorC = params.instancesC;
    }else{
      params.colorC = colorField.getSelectedIndex();
    }
    if(params.columnMaxes[params.colorC] == null){
      System.out.println("You are probably trying to set up the colors with a non-numerical column");
    }else{
      params.colorMax = params.columnMaxes[params.colorC];
      params.colorMin = params.columnMins[params.colorC];
      params.colorMid = params.columnMids[params.colorC]; 
    }
    maxIconColorValueModel.setValue(params.colorMax);
    minIconColorValueModel.setValue(params.colorMin);
    midIconColorValueModel.setValue(params.colorMid);
  }

  private void setIconFile() {
    int returnVal = fileChooser.showOpenDialog(this);
    if (returnVal == JFileChooser.APPROVE_OPTION) {
      params.iconFile = fileChooser.getSelectedFile();
      params.iconFileName = params.iconFile.getAbsolutePath();
      iconFilenameText.setText(params.iconFileName);
      params.iconIsImageFile = true;
      params.iconIsShadedDot = false;
      refreshDisplayPanel();
    }
  }
  
  private void refreshDisplayPanel() {
    if(params.instancesCount && !params.instanceCountCreated){
      params.createInstanceCount();
    }
    if(!params.instancesCount){
      params.setColumnNumbersToInputColumnNumbers();
    }
    params.setMaxMins();
    kwa.ddp.setup(params);
    String failedLinesList = "";
    boolean oneWorked = false;
    boolean oneFailed = false;
    try {
      int lineCheck = 2;
      for(String[] str: params.outputDataSet){
        GeoPoint gp = new GeoPoint(str, params);
        if(gp.valid){
          kwa.ddp.displayPoint(gp);
          oneWorked = true;
          lineCheck++;
        }else{
          /* If this is the first time an item with this name has
           * caused a problem, append the name and number of the
           * offending row to the orphanList string.*/
          oneFailed = true;
          try{
            if (!failedLinesList.contains(str[params.nameC])) { /*TODO This won't filter 
                                          out duplicates with different capitalization*/
              failedLinesList += "Row " + lineCheck + ": " + str[params.nameC] + "\n";
            }
          }catch(Exception e){
            failedLinesList += "Row " + lineCheck + 
                "Joekit couldn't find a name in the appropriate column!\n";
            //e.printStackTrace();
          }
          lineCheck++;
        }
      }
    } catch (IOException e) {
      // TODO(pablo)
      e.printStackTrace();
    }

    kwa.ddp.paintPanel();

    if (oneWorked) {
      if (oneFailed) {
        if(!params.instancesCount){
          kwa.createReportPopupWithTextField(failedLinesList);
        }
      }
    } else {
      if(!params.instancesCount){
        kwa.createReportPopup("None of the rows of this dataset " + 
                            "successfully generated a point." + 
                            "\nPlease check your lat/long columns, or" +
                            "\nconsider using a gazetteer!");
      }
    }
  }

  /* TODO create a better UI! 
   * This is crazy complicated; people are always asking for features
   * that already exist (they don't know how to find them amongs the
   * mess of buttons), and those just who want a basic map are stymied
   * by the complexity of the UI.
   */
  private void createTheWidgetInstances() {
    inputFilenameText = new JTextField("Please select an input file (.xls, .xlsx, .csv, or .txt)");
    Font font = new Font(inputFilenameText.getFont().getFontName(getLocale()), Font.PLAIN, 10);
    inputFilenameText.setFont(font);
    inputFilenameText.addActionListener(this);
    inputFilenameChooserButton = new JButton("Input");
    inputFilenameChooserButton.setFont(font);
    inputFilenameChooserButton.addActionListener(this);
    identifyLatAndLongColumns = new JLabel(
      "Please ensure that the correct columns are selected to represent latitude and longitude.");
    identifyLatAndLongColumns.setFont(font);
    identifyLatAndLongColumns.setEnabled(false);
    latL = new JLabel("Latitude:");
    latL.setFont(font);
    latL.setEnabled(false);
    latField = new WideComboBox();
    latField.setFont(font);
    latField.setEnabled(false);
    latField.addActionListener(this);
    longL = new JLabel("Longitude:");
    longL.setFont(font);
    longL.setEnabled(false);
    longField = new WideComboBox();
    longField.setFont(font);
    longField.setEnabled(false);
    longField.addActionListener(this);
    labelL = new JLabel("Choose the column whose contents will appear as the labels:");
    labelL.setFont(font);
    labelL.setEnabled(false);
    labelField = new WideComboBox();
    labelField.setFont(font);
    labelField.setEnabled(false);
    labelField.addActionListener(this);
    sizeL = new JLabel("Choose the column whose contents will determine icon size:");
    sizeL.setFont(font);
    sizeL.setEnabled(false);
    sizeField = new WideComboBox();
    sizeField.setFont(font);
    sizeField.setEnabled(false);
    sizeField.addActionListener(this);
    colorL = new JLabel("Choose the column whose contents will determine icon color:");
    colorL.setFont(font);
    colorL.setEnabled(false);
    colorField = new WideComboBox();
    colorField.setFont(font);
    colorField.setEnabled(false);
    colorField.addActionListener(this);
    startDateL = new JLabel("Start date column:");
    startDateL.setFont(font);
    startDateL.setEnabled(false);
    startDateField = new WideComboBox();
    startDateField.setFont(font);
    startDateField.setEnabled(false);
    startDateField.addActionListener(this);
    endDateL = new JLabel("End date column:");
    endDateL.setFont(font);
    endDateL.setEnabled(false);
    endDateField = new WideComboBox();
    endDateField.setFont(font);
    endDateField.setEnabled(false);
    endDateField.addActionListener(this);
    daysPerEventModel = new SpinnerNumberModel(params.daysPerEvent, 1, 365, 1.0);
    daysPerEventSp = new JSpinner(daysPerEventModel);
    ((JSpinner.DefaultEditor) daysPerEventSp.getEditor()).getTextField().setFont(font);
    daysPerEventSp.setEnabled(false);
    daysPerEventModel.addChangeListener(this);

    animateCB = new JCheckBox("Animate");
    animateCB.setFont(font);
    animateCB.addActionListener(this);
    animateCB.setHorizontalTextPosition(SwingConstants.LEFT);
    animateCB.setEnabled(false);
    noEndDateCB = new JCheckBox("No end dates, just this many days per item: ");
    noEndDateCB.setFont(font);
    noEndDateCB.setSelected(true);
    noEndDateCB.addActionListener(this);
    noEndDateCB.setEnabled(false);

    variableIconSizeCB = new JCheckBox("Variable icon sizes");
    variableIconSizeCB.setFont(font);
    variableIconSizeCB.setEnabled(false);
    variableIconSizeCB.addActionListener(this);
    variableIconSizeCB.setHorizontalTextPosition(SwingConstants.LEFT);
    logarithmicIconSizeCB = new JCheckBox("Log scale");
    logarithmicIconSizeCB.setFont(font);
    logarithmicIconSizeCB.setEnabled(false);
    logarithmicIconSizeCB.setHorizontalTextPosition(SwingConstants.LEFT);
    logarithmicIconSizeCB.addActionListener(this);
    
    variableIconColorCB = new JCheckBox("Variable icon colors");
    variableIconColorCB.setFont(font);
    variableIconColorCB.setEnabled(false);
    variableIconColorCB.addActionListener(this);
    categoriesCB = new JCheckBox("Categories");
    categoriesCB.setFont(font);
    categoriesCB.setEnabled(false);
    categoriesCB.addActionListener(this);
    legendCB = new JCheckBox("Legend");
    legendCB.setFont(font);
    legendCB.setEnabled(false);
    legendCB.addActionListener(this);
    
    
    maxIconColorValueL = new JLabel("Max icon color value");
    maxIconColorValueL.setFont(font);
    maxIconColorValueL.setEnabled(false);
    minIconColorValueL = new JLabel("Min icon color value");
    minIconColorValueL.setFont(font);
    minIconColorValueL.setEnabled(false);
    midIconColorValueL = new JLabel("Mid icon color value");
    midIconColorValueL.setFont(font);
    midIconColorValueL.setEnabled(false);
    maxIconColorValueModel = new SpinnerNumberModel(0, -1000000000, 1000000000, 1.0);
    maxIconColorValue = new JSpinner(maxIconColorValueModel);
    ((JSpinner.DefaultEditor) maxIconColorValue.getEditor()).getTextField().setFont(font);
    maxIconColorValueModel.addChangeListener(this);
    maxIconColorValue.setEnabled(false);
    minIconColorValueModel = new SpinnerNumberModel(0, -1000000000, 1000000000, 1.0);
    minIconColorValue = new JSpinner(minIconColorValueModel);
    ((JSpinner.DefaultEditor) minIconColorValue.getEditor()).getTextField().setFont(font);
    minIconColorValueModel.addChangeListener(this);
    minIconColorValue.setEnabled(false);
    midIconColorValueModel = new SpinnerNumberModel(0, -1000000000, 1000000000, 1.0);
    midIconColorValue = new JSpinner(midIconColorValueModel);
    ((JSpinner.DefaultEditor) midIconColorValue.getEditor()).getTextField().setFont(font);
    midIconColorValueModel.addChangeListener(this);
    midIconColorValue.setEnabled(false);

    gazetteerExistsCB = new JCheckBox("Use a gazetteer, match locations from names in column:");
    gazetteerExistsCB.setFont(font);
    gazetteerExistsCB.setEnabled(false);
    gazetteerExistsCB.addActionListener(this);
    gazetteerFilenameText = new JTextField(
      "Please choose a gazetteer with locations for the places listed in the input file");
    gazetteerFilenameText.setFont(font);
    gazetteerFilenameText.setEnabled(false);
    gazetteerFilenameChooserButton = new JButton("Gazetteer");
    gazetteerFilenameChooserButton.setFont(font);
    gazetteerFilenameChooserButton.addActionListener(this);
    gazetteerFilenameChooserButton.setEnabled(false);   
    nameField = new WideComboBox();
    nameField.setFont(font);
    nameField.setEnabled(false);
    nameField.addActionListener(this);

    outputFilenameText = new JTextField("Please choose a name for your .kml file");
    outputFilenameText.setFont(font);
    outputFilenameText.setEnabled(false);

    outputFilenameChooserButton = new JButton("Output");
    outputFilenameChooserButton.setFont(font);
    outputFilenameChooserButton.addActionListener(this);
    outputFilenameChooserButton.setEnabled(false);

    iconFilenameText = new JTextField("http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png");
    iconFilenameText.setFont(font);
    iconFilenameText.setEnabled(false);

    iconFilenameChooserButton = new JButton("Icon file");
    iconFilenameChooserButton.setFont(font);
    iconFilenameChooserButton.addActionListener(this);
    iconFilenameChooserButton.setEnabled(false);

    defaultIconColorChooserButton = new JButton("Default icon color");
    defaultIconColorChooserButton.setFont(font);
    defaultIconColorChooserButton.setForeground(params.defaultIconColor);
    defaultIconColorChooserButton.setEnabled(false);
    defaultIconColorChooserButton.addActionListener(this);
    middleIconColorChooserButton = new JButton("Mid icon color");
    middleIconColorChooserButton.setFont(font);
    middleIconColorChooserButton.setForeground(params.middleIconColor);
    middleIconColorChooserButton.setEnabled(false);
    middleIconColorChooserButton.addActionListener(this);
    maxIconColorChooserButton = new JButton("Max icon color");
    maxIconColorChooserButton.setFont(font);
    maxIconColorChooserButton.setForeground(params.maxIconColor);
    maxIconColorChooserButton.setEnabled(false);
    maxIconColorChooserButton.addActionListener(this);


    defaultIconSizeL = new JLabel("Default (minimum) icon size");
    defaultIconSizeL.setFont(font);
    defaultIconSizeL.setEnabled(false);
    iconSizeFactorL = new JLabel("Multiplication factor for icon size");
    iconSizeFactorL.setFont(font);
    iconSizeFactorL.setEnabled(false);
    defaultIconSizeModel = new SpinnerNumberModel(params.defaultIconSize, 0, 100, 0.05);
    defaultIconSizeSp = new JSpinner(defaultIconSizeModel);
    ((JSpinner.DefaultEditor) defaultIconSizeSp.getEditor()).getTextField().setFont(font);
    defaultIconSizeSp.setEnabled(false);
    defaultIconSizeModel.addChangeListener(this);
    iconSizeFactorModel = new SpinnerNumberModel(1, 0, 10000, 0.1);
    iconSizeFactorSp = new JSpinner(iconSizeFactorModel);
    ((JSpinner.DefaultEditor) iconSizeFactorSp.getEditor()).getTextField().setFont(font);
    JSpinner.NumberEditor editor = (JSpinner.NumberEditor)iconSizeFactorSp.getEditor();  
    DecimalFormat format = editor.getFormat();  
    format.setMinimumFractionDigits(4);
    iconSizeFactorModel.addChangeListener(this);
    iconSizeFactorSp.setEnabled(false);

    labelColorChooserButton = new JButton("Label color");
    labelColorChooserButton.setFont(font);
    labelColorChooserButton.setEnabled(false);
    labelColorChooserButton.addActionListener(this);

    labelCB = new JCheckBox("Label points");
    labelCB.setFont(font);
    labelCB.setEnabled(false);
    labelCB.setSelected(true);
    labelCB.addActionListener(this);
    labelSizeL = new JLabel("Label size");
    labelSizeL.setFont(font);
    labelSizeL.setEnabled(false);
    labelSizeModel = new SpinnerNumberModel(params.labelSize,0,10,.05);
    labelSizeSp = new JSpinner(labelSizeModel);
    ((JSpinner.DefaultEditor) labelSizeSp.getEditor()).getTextField().setFont(font);
    labelSizeSp.setEnabled(false);
    labelSizeModel.addChangeListener(this);

    createKmlButton = new JButton("Create KML");
    createKmlButton.setFont(font);
    createKmlButton.addActionListener(this);
    createKmlButton.setEnabled(false);
    
    createKmzButton = new JButton("Create KMZ");
    createKmzButton.setFont(font);
    createKmzButton.addActionListener(this);
    createKmzButton.setEnabled(false);
    
    previewButton = new JButton("Preview");
    previewButton.setFont(font);
    previewButton.addActionListener(this);
    previewButton.setEnabled(false);
    
    separator = new JSeparator(SwingConstants.HORIZONTAL);
    separator1 = new JSeparator(SwingConstants.HORIZONTAL);
    separator2 = new JSeparator(SwingConstants.HORIZONTAL);
    separator3 = new JSeparator(SwingConstants.HORIZONTAL);
    separator4 = new JSeparator(SwingConstants.HORIZONTAL);
    separator5 = new JSeparator(SwingConstants.HORIZONTAL);
    separator6 = new JSeparator(SwingConstants.HORIZONTAL);
    separator7 = new JSeparator(SwingConstants.HORIZONTAL);
    fileChooser = new JFileChooser();
    defaultIconColorChooser = new JColorChooser();
  }
	
  private void doTheLayout() {
    layout = new GroupLayout(this);
    setLayout(layout);
    layout.setAutoCreateGaps(false);
    layout.setAutoCreateContainerGaps(true);
    layout.linkSize(maxIconColorValueL, minIconColorValueL, midIconColorValueL);
    layout.linkSize(maxIconColorValue, minIconColorValue, midIconColorValue);
    layout.linkSize(defaultIconColorChooserButton, middleIconColorChooserButton, 
                    maxIconColorChooserButton);
    layout.setHorizontalGroup(layout.createSequentialGroup()
                              .addGroup(layout.createParallelGroup()
                                        .addComponent(separator)
                                        .addComponent(separator1)
                                        .addComponent(separator2)	
                                        .addComponent(separator3)
                                        .addComponent(separator4)
                                        .addComponent(separator5)
                                        .addComponent(separator6)
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(inputFilenameText)
                                                  .addComponent(inputFilenameChooserButton))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(identifyLatAndLongColumns))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(latL)
                                                  .addComponent(latField, 160,
                                                                GroupLayout.DEFAULT_SIZE, 160)
                                                  .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                  .addComponent(longL)
                                                  .addComponent(longField, 160,
                                                                GroupLayout.DEFAULT_SIZE, 160)
                                                                )
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(labelL)
                                                  .addComponent(labelField, 0, 160, 160))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(labelCB)
                                                  .addComponent(labelColorChooserButton)
                                                  .addComponent(labelSizeL)
                                                  .addComponent(labelSizeSp,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                70)
                                                                .addComponent(legendCB))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(iconFilenameText)
                                                  .addComponent(iconFilenameChooserButton))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(variableIconSizeCB)
                                                  .addComponent(defaultIconSizeL)
                                                  .addComponent(defaultIconSizeSp,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                70)
                                                  .addComponent(logarithmicIconSizeCB))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(sizeL)
                                                  .addComponent(sizeField, 0, 160, 160))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(iconSizeFactorL)
                                                  .addComponent(iconSizeFactorSp,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                70))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(variableIconColorCB))
                                        .addGroup(layout.createSequentialGroup() 
                                                  .addComponent(colorL)
                                                  .addComponent(colorField, 0, 160, 160)
                                                  .addComponent(categoriesCB))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(defaultIconColorChooserButton)
                                                  .addComponent(minIconColorValueL)
                                                  .addComponent(minIconColorValue))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(middleIconColorChooserButton)
                                                  .addComponent(midIconColorValueL)
                                                  .addComponent(midIconColorValue))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(maxIconColorChooserButton)
                                                  .addComponent(maxIconColorValueL)
                                                  .addComponent(maxIconColorValue, 0, 150, 150))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(animateCB)
                                                  .addComponent(startDateL)
                                                  .addComponent(startDateField, 0, 160, 160)
                                                  .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                                  .addComponent(endDateL)
                                                  .addComponent(endDateField, 0, 160, 160))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(noEndDateCB)
                                                  .addComponent(daysPerEventSp,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                GroupLayout.PREFERRED_SIZE,
                                                                70))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(gazetteerExistsCB)
                                                  .addComponent(nameField))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(gazetteerFilenameText)
                                                  .addComponent(gazetteerFilenameChooserButton))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addComponent(outputFilenameText)
                                                  .addComponent(outputFilenameChooserButton))
                                        .addGroup(layout.createSequentialGroup()
                                                  .addGap(300)// THIS IS GOING TO CREATE A PROBLEM
                                                  .addComponent(previewButton)
                                                  .addComponent(createKmlButton)
                                                  .addComponent(createKmzButton)
                                                  )
                                        .addGroup(layout.createParallelGroup()
                                                  .addGap(10))));

    layout.setVerticalGroup(layout.createSequentialGroup()
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(inputFilenameText)
                                      .addComponent(inputFilenameChooserButton))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(identifyLatAndLongColumns))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(latL)
                                      .addComponent(latField)
                                      .addComponent(longL)
                                      .addComponent(longField)
                                      )
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(labelL)
                                      .addComponent(labelField))
                            .addComponent(separator)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(gazetteerExistsCB)
                                      .addComponent(nameField))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(gazetteerFilenameText)
                                      .addComponent(gazetteerFilenameChooserButton))
                            .addComponent(separator1)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(labelCB)
                                      .addComponent(labelSizeL)
                                      .addComponent(labelSizeSp,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.PREFERRED_SIZE)
                                      .addComponent(labelColorChooserButton)
                                      .addComponent(legendCB))
                            .addComponent(separator2)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE) 
                                      .addComponent(iconFilenameText)
                                      .addComponent(iconFilenameChooserButton))
                            .addComponent(separator3)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(defaultIconSizeL)	
                                      .addComponent(defaultIconSizeSp,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.PREFERRED_SIZE)
                                      .addComponent(variableIconSizeCB)
                                      .addComponent(logarithmicIconSizeCB))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(sizeL)
                                      .addComponent(sizeField))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(iconSizeFactorL)
                                      .addComponent(iconSizeFactorSp,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.PREFERRED_SIZE))
                            .addComponent(separator4)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(variableIconColorCB))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(colorL)
                                      .addComponent(colorField)
                                      .addComponent(categoriesCB))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(defaultIconColorChooserButton)
                                      .addComponent(minIconColorValueL)
                                      .addComponent(minIconColorValue))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(middleIconColorChooserButton)
                                      .addComponent(midIconColorValueL)
                                      .addComponent(midIconColorValue))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE) 
                                      .addComponent(maxIconColorChooserButton)
                                      .addComponent(maxIconColorValueL)
                                      .addComponent(maxIconColorValue))
                            .addComponent(separator5)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(animateCB)
                                      .addComponent(startDateL)
                                      .addComponent(startDateField)
                                      .addComponent(endDateL)
                                      .addComponent(endDateField))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(noEndDateCB)
                                      .addComponent(daysPerEventSp,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.PREFERRED_SIZE,
                                                    GroupLayout.PREFERRED_SIZE))
                            .addComponent(separator6)
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(outputFilenameText)
                                      .addComponent(outputFilenameChooserButton))
                            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                      .addComponent(previewButton)
                                      .addComponent(createKmlButton)
                                      .addComponent(createKmzButton))
                                      );
      }
}