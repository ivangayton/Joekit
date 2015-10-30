package net.joekit;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Random;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.DateUtil;

/* This class contains information about the input file iteslf, as well as
 * all of the user-selectable parameters for the output file.*/
  
/* At some point there should be a saveable parameter file (.jkt?) so that 
 * a Joekit user can just load up their favorite parameters rather than 
 * clicking all over the place.  Obvious usage: a spreadsheet that you update 
 * each week and make a new map; you'd want to keep the same settings and not
 * re-enter them every time you update your map.*/

class ParameterSet {
  // Strings/files (user-definable)
  File inputFile, outputFile, iconFile, gazetteerFile, legendFile;
  
  // Column numbers (user-definable)
  int inputLatC, inputLongC, inputNameC, inputLabelC, inputSizeC, inputColorC,
    inputStartDateC, inputEndDateC, inputEpiWeekC;
  
  // Suggested column header names
  // TODO This needs to be done with regex to match partials
  String[] latColumnNames = {"lat","latitude", "north", "northing", "ycoord"};
  String[] longColumnNames = {"long", "lon", "longitude", "east", "easting", "xcoord"};
  String[] nameColumnNames = {"name", "full_name", "full_name_",
      "id", "place name", "placename", "address", "location", 
      "village", "address or village", "town", "city", "place", 
      "ward", "adm1", "adm2", "adm3", "geo id", "geo_id"};
  String[] labelColumnNames = {"name", "id", "label", "place name", "placename",
      "full_name", "full_name_", "town", "city", "place"};
  String[] sizeColumnNames = {"size"};
  String[] colorColumnNames = {"color", "colour"};
  String[] startDateColumnNames = {"date", "start date", "startdate", "start", "begin", "time", 
    "week", "date of admission"};
  String[] endDateColumnNames = {"end date", "enddate", "finish", "end"};
  String[] epiWeekColumnNames = {"epi week", "epi_week", "ew"};
  
  // Other numerical parameters (user-definable)
  double defaultIconSize = 0.6, labelSize = 0.75, iconSizeFactor = 1.0;
  int daysPerEvent = 1;
  double colorMax = 0, colorMid = 0, colorMin = 0; 
  
  // Switches (user-definable)
  boolean variableIconSize = false, variableIconColor = false , 
    animate = false, endDateExists = false, gazetteerExists = false, 
    logarithmicSize = false, instancesCount = false, 
    sizeByInstances = false, colorByInstances = false, 
    isKmzRatherThanKml = false, colorCategories = false;
  
  // Colors (user-definable)
  Color defaultIconColor = Color.RED;
  Color middleIconColor = Color.YELLOW;
  Color maxIconColor = Color.GREEN;
  Color labelColor = Color.WHITE;
  
  // Column minima and maxima (not user-definable, simply are what they are)
  Double[] columnMaxes, columnMins, columnMids;
  int latC, longC, nameC, labelC, sizeC, colorC, startDateC, endDateC, instancesC;
  double latMax, latMin,longMax, longMin,sizeMax, sizeMin;
  
  // Internal tracking switches (not user-definable)
  boolean iconIsImageFile = false;
  boolean iconIsShadedDot = true;
  boolean latColumnExists, longColumnExists;
  boolean hasEpiWeek;
  
  // Internal stuff dealing with the icon and legend
  BufferedImage legendImage;
  BufferedImage iconImage;
  String iconFileName;
  
  // The input file loaded into memory, basically a raw dataset
  ArrayList<String[]> inputDataSet;
  String[] inputHeaders;
  
  /* the data as it will be read for display. If there is no preprocessing   
   * (simplest scenario whereby we simply read the input dataset line by line
   * to create one point per line, no gazetteer or instance counting), this
   * will be a clone (shallow copy or reference) to avoid using more memory.
   * If we need to link a gazetteer, sort the data, or do instance counts, then
   * this becomes a copy which is manipulated while the inputDataSet object 
   * remains unmolested.*/
  ArrayList<String[]> linkedDataSet; 
  String[] linkedHeaders;
  boolean dataSetLinked;
  
  /* Dataset containing only lines with either unique names, or if "animate" is 
   * checked, lines with unique combinations of name and date.*/
  ArrayList<String[]> instanceDataSet;
  String[] instanceHeaders = {"Name", "Lat", "Long", "Instances", "Date", "Data"};
  boolean instanceCountCreated;
  
  /*  Dataset to be read by the display and output methods and classes.  This
   * should never be an independent structure in memory, just a pointer (using 
   * clone() to create a "shallow copy) referencing whichever of the three other
   * datasets is the right one to use (the raw input dataset, the one linked to 
   * a gazetteer, or the instance count*/
  ArrayList<String[]> outputDataSet;
  String[] outputHeaders;
  
  // List of categories for colors by categories
  String[] colorList = new String[]{"0xFF0000", "0x00FF00", "0x0000FF", "0xFFFF00", 
      "0xFF00FF", "0x00FFFF", "0xFF6600", "0xCC99FF", "0x800000", "0x99CC00",
      "0x3366FF", "0xFF99CC", "0x808080", "0x008000", "0xFFFFCC", "0x333333", 
      "0x333399", "0x993300", "0xFFCC00", "0x993366"};
  ArrayList<String[]> categoriesForColors;
  
  // In case we've made a legend
  boolean legendShouldBeDisplayed;
  
  // the constructor doesn't do much and has no parameters...
  ParameterSet(){
    iconFileName = "http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png";
  }
  
  void setColumnNumbersToInputColumnNumbers(){
    latC = inputLatC;
    longC = inputLongC;
    nameC = inputNameC;
    labelC =inputLabelC;
    sizeC = inputSizeC;
    colorC = inputColorC;
    startDateC = inputStartDateC;
    endDateC = inputEndDateC;
  }
  
  @SuppressWarnings("unchecked")
  void setOutputFileToLinkedDataSet(){
    outputDataSet = (ArrayList<String[]>) linkedDataSet.clone();
    setColumnNumbersToInputColumnNumbers();
  }
  
  @SuppressWarnings("unchecked")
  void setOutputFileToInputDataSet(){
    outputDataSet = (ArrayList<String[]>) inputDataSet.clone();
    setColumnNumbersToInputColumnNumbers();
  }
  
  @SuppressWarnings("unchecked")
  void setOutputFileToInstanceDataSet(){
    outputDataSet = (ArrayList<String[]>) instanceDataSet.clone();
  }
  
  void setUpNonNumericalCategories(){
    // set up a temporary ArrayString to hold the strings that represent categories
    ArrayList<String> categoryList = new ArrayList<String>();
    for(String[] strings: outputDataSet){
      categoryList.add(strings[colorC]);
    }
    // create a comparator that ignores case and whitespace.  
    Comparator<String> categorizer = new Comparator<String>(){
      @Override
      public int compare(String string1, String string2) {
        return string1.replaceAll("\\s+", "").compareToIgnoreCase
            (string2.replaceAll("\\s+", ""));
      }
    };
    
    // create a list without duplicates
    Collection<String> noDupsTemp = new LinkedHashSet<String>();
    for(String s: categoryList){
      noDupsTemp.add(s.toLowerCase().replaceAll("\\s+", ""));
    }
    
    // Restore the spaces and capitalization of the category list (uses first instance)
    Collection<String> noDups = new LinkedHashSet<String>();
    for(String s: noDupsTemp){
      for(String st: categoryList){
        if(categorizer.compare(s, st) == 0){
          noDups.add(st);
          break;
        }
      }
    }
    
    // Assign colors to the categories (from the list)
    int colorIndex = 0;
    Random random = new Random();
    categoriesForColors = new ArrayList<String[]>();
    for(String s: noDups){
      String[] catAndColorSet = new String[2];
      catAndColorSet[0] = s;
      catAndColorSet[1] = (colorIndex < colorList.length) ? 
          colorList[colorIndex] : "0x" + Integer.toHexString(random.nextInt(0xffffff));
      categoriesForColors.add(catAndColorSet);
      colorIndex++;
    }
  }
  
  void createInstanceCount(){
    
    /* First ensure that we're using the right list.  For example, if we
     * have already loaded the instances without animating, and now we're
     * here because we've decided to animate, the outputDataSet (which we 
     * are using to create the temporary dataset for sorting to create the 
     * instances) will be a clone of the already-created instance list.  
     * We need to go back to the original data.  But is it the input dataset,
     * or the linked dataset after a gazetteer has been used? */
    ArrayList<String[]> tempDataSet = new ArrayList<String[]>();
    if(dataSetLinked){
      setOutputFileToLinkedDataSet();
    }else{
      setOutputFileToInputDataSet();
    }
    
    // Now load the list into a temporary ArrayList<String[]> for sorting
    for(String[] strCop: outputDataSet){
      tempDataSet.add(strCop);
    }
    
    // A comparator that sorts by the name column, case-insensitive
    Comparator<String[]> comper = new Comparator<String[]>(){
      @Override
      public int compare(String[] string1, String[] string2) {
        return string1[inputNameC].replaceAll("\\s+", "").compareToIgnoreCase
            (string2[inputNameC].replaceAll("\\s+", ""));
      }
    };
    
    // A comparator that sorts by the date column, using the date value, not the string
    // TODO not sure how it will behave with an unparseable date... 
    Comparator<String[]> compDate = new Comparator<String[]>(){
      @Override
      public int compare(String[] string1, String[] string2) {
        DateStringParser dateParser = new DateStringParser();
        Date date1 = dateParser.parseDate(string1[inputStartDateC]);
        Date date2 = dateParser.parseDate(string2[inputStartDateC]);
        return date1.compareTo(date2);
      }
    };
    
    // Sort by name
    Collections.sort(tempDataSet, comper);
    
    // If we are animating, sort by date
    if(this.animate){
      formatDateColumn(tempDataSet);
      Collections.sort(tempDataSet, compDate);
    }
    
    instanceDataSet = new ArrayList<String[]>();
    boolean stillIterating = true;
    int elmt = 0;
    while(stillIterating){
      if(elmt >= tempDataSet.size()){
        stillIterating = false;
      }
      // TODO make sure this string array doesn't throw up null references (too short)
      String[] crntElmt = tempDataSet.get(elmt);
      boolean foundNewName = false;
      int instancesOfThisElmt = 1;
      while(!foundNewName){
        String[] toAdd = new String[6];// name, lat, long, instances, date, description
        if(elmt + instancesOfThisElmt == tempDataSet.size()){
          toAdd[0] = crntElmt[inputNameC];
          toAdd[1] = crntElmt[inputLatC];
          toAdd[2] = crntElmt[inputLongC];
          toAdd[3] = Integer.toString(instancesOfThisElmt);
          toAdd[4] = crntElmt[inputStartDateC];
          toAdd[5] = ""; 
          // TODO add the correct column for description.
          instanceDataSet.add(toAdd);
          stillIterating = false;
          break;
        }
        String[] checkElmt = tempDataSet.get(elmt + instancesOfThisElmt);
        int nameCompare = comper.compare(crntElmt, checkElmt);
        int dateCompare = animate ? compDate.compare(crntElmt, checkElmt) : 0;
        if(nameCompare == 0 && dateCompare == 0){
          instancesOfThisElmt++;
        }else{
        toAdd[0] = crntElmt[inputNameC];
        toAdd[1] = crntElmt[inputLatC];
        toAdd[2] = crntElmt[inputLongC];
        toAdd[3] = Integer.toString(instancesOfThisElmt);
        toAdd[4] = crntElmt[inputStartDateC];
        toAdd[5] = ""; 
        
        instanceDataSet.add(toAdd);
        foundNewName = true;
        elmt += instancesOfThisElmt;
      }
      }
    }
    setOutputFileToInstanceDataSet();
    outputHeaders = instanceHeaders;
    latC = 1;
    longC = 2;
    instancesC = 3;
    if(sizeByInstances){
      sizeC = instancesC;
    }
    if(colorByInstances){
      colorC = instancesC;
    }
    startDateC = 4;
    
    setMaxMins();
    instanceCountCreated = true;
  }
  
  /* TODO  Check for American date format (look for 13th months). If we look through
   * the whole file, we should be able to determine whether they're European or 
   * American dates. Then we can put them in "proper" format yyyy-MM-dd. */
  void formatDateColumn(ArrayList<String[]> dataSet){ 
    for(String[] sp: dataSet){
      DateStringParser dateParser = new DateStringParser();
      sp[inputStartDateC] = dateParser.dateStringToCorrectedString(sp[inputStartDateC]);
    }
  }
  
  void setMaxMins(){
    System.out.println("Now trying to set maxima and minima for all "
        + outputHeaders.length + " columns");
    columnMaxes = new Double[outputHeaders.length];
    columnMins =  new Double[outputHeaders.length];
    columnMids = new Double[outputHeaders.length];
    IsStringANumberChecker numChecker = new IsStringANumberChecker();
    LatLongCoordParser coordParser = new LatLongCoordParser();
    DateStringParser dateParser = new DateStringParser();
    boolean[] foundFirstNumberInThisColumn = new boolean[outputHeaders.length];
    for(String[] str: outputDataSet){
      int columnIndexer = 0;
      for(String toTest: str){
        System.out.println(columnIndexer + ", " + outputHeaders[columnIndexer]);
        double tester = 0;
        boolean foundAValue = false;
        // if it's a number, store it
        if(numChecker.check(toTest)){
          tester = Double.parseDouble(toTest);
          foundAValue = true;
        }else{
          // if it's not a number, maybe it's a coordinate. If so, store it.
          if(coordParser.parseCoord(toTest) != 0){
            tester = coordParser.parseCoord(toTest);
            foundAValue = true;
          }else{
            // Ok, if it's not a number or a coordinate, maybe it's a date
            if(dateParser.parseDate(toTest) != null){
              Date dateHere = dateParser.parseDate(toTest);
              tester = DateUtil.getExcelDate(dateHere);
              foundAValue = true;
            }
          }
        }
        if(foundAValue){
          if(!foundFirstNumberInThisColumn[columnIndexer]){ 
            columnMaxes[columnIndexer] = tester;
            columnMins[columnIndexer] = tester;
            foundFirstNumberInThisColumn[columnIndexer] = true;
          }
          if(columnMaxes[columnIndexer] != null){
            if(tester > columnMaxes[columnIndexer]){
              columnMaxes[columnIndexer] = tester;
            }
            if(tester < columnMins[columnIndexer]){
                columnMins[columnIndexer] = tester;
              }
          }
          
        }
        columnIndexer++;
      }
    }
    for(int indexer2 = 0; indexer2 < inputHeaders.length; indexer2++){
      try{
        columnMids[indexer2] = (columnMaxes[indexer2] + columnMins[indexer2]) / 2;
      }catch(Exception e){
        // TODO basically ignore this?
      }
      
    }
    if(columnMaxes[latC] != null && columnMaxes[longC] != null && columnMins[latC] != null && columnMins[longC] != null){
      latMax = columnMaxes[latC];
      latMin = columnMins[latC];
      longMax = columnMaxes[longC];
      longMin = columnMins[longC];
    }
  }
  
  void loadIconImage(File iconImageFile){
    try {
      iconImage = ImageIO.read(new File(iconImageFile.getAbsolutePath()));
  } catch (IOException e) {
    e.printStackTrace();
  }
  }
  
  void recolorIcon(Color color){
    int w = iconImage.getWidth();
    int h = iconImage.getHeight();
    int[] rgbArray = new int[w * h];
    iconImage.getRGB(0, 0, w, h, rgbArray, 0, w);
    for(int i = 0; i < rgbArray.length; i++){
      Color col = new Color(rgbArray[i], true);
      int oldR = col.getRed();
      int oldG = col.getGreen();
      int oldB = col.getBlue();
      int oldA = col.getAlpha();
      int newR = (oldR > color.getRed()) ? color.getRed() : oldR;
      int newG = (oldG > color.getGreen()) ? color.getGreen() : oldG;
      int newB = (oldB > color.getBlue()) ? color.getBlue() : oldB;
      int newA = (oldA > color.getAlpha()) ? color.getAlpha() : oldA;
      col = new Color(newR, newG, newB, newA);
      rgbArray[i] = col.getRGB();
    }
    iconImage.setRGB(0, 0, w, h, rgbArray, 0, w);
  }
  
  void resetParameters(){
    inputFile = null;
    outputFile = null;
    iconFile = null;
    gazetteerFile = null;
    iconFileName = "http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png";
    latC = 0; longC = 0; nameC = 0; labelC = 0; sizeC = 0; colorC = 0; startDateC = 0;
    endDateC = 0;
    defaultIconSize = 0.6;
    labelSize = 0.75;
    iconSizeFactor = 1.0;
    variableIconSize = false; 
    variableIconColor = false;
    endDateExists = false;
    gazetteerExists = false;
    sizeByInstances = false;
    colorByInstances = false;
    animate = false;
    daysPerEvent = 1;
    longColumnExists = false;
    latColumnExists = false;
    setMaxMins();
  }
}
