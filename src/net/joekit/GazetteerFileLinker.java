package net.joekit;

import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/* Loads a gazetteer file and adds lat/long columns to every line in the dataset.
 * For efficiency, it first sorts the main dataset, so it only has to read through 
 * the gazetteer once per placename.
 * 
 * If the gazetteer contains polygons rather than points, this class assigns a random
 * position within the polygon to each individual line.*/

class GazetteerFileLinker {
  ParameterSet params;
  ArrayList<String[]> gazetteerDataSet;
  
  String fnm;
  int line;		
  String separator = "\t"; // default separator for an unknown file type will be tab
  String inputFileExtension;
  String splitHeader[];
  
  boolean foundOne; 
  boolean isPolygons;
  String foundString;
  public String orphanList;
	
  int dataRows,dataColumns,row;
  
  boolean isXlsx = false, isXls = false, isCsv = false, 
      isTxt = false, isVib = false, isKml = false, isKmz = false;
  
  // This list duplicates in a few places
  String[] latColumnNames = {"lat","latitude", "north", "northing", "ycoord"};
  String[] longColumnNames = {"long", "lon", "longitude", "east", "easting", "xcoord"};
  String[] nameColumnNames = {"name", "full_name", "full_name_",
      "id", "place name", "placename", "address", "location", 
      "village", "address or village", "town", "city", "place", 
      "ward", "adm1", "adm2", "adm3", "geo id", "geo_id"};

  BufferedReader br;
  FileInputStream fileInputStream;
  Workbook workbook;
  Sheet worksheet;
	
  int gazLatC, gazLongC, gazNameC;
  
//Create a comparator that ignores case and whitespace. 
  // TODO: also ignore special characters
  Comparator<String> stringMatcher = new Comparator<String>(){
    @Override
    public int compare(String string1, String string2) {
      return string1.replaceAll("\\s+", "").compareToIgnoreCase
          (string2.replaceAll("\\s+", ""));
    }
  };
  
  GazetteerFileLinker(ParameterSet pms) {
    params = pms;
    gazetteerDataSet = new ArrayList<String[]>();
    fnm = params.gazetteerFile.getAbsolutePath();
    String shortFilename = params.gazetteerFile.getName().toLowerCase();
    int dotAt = shortFilename.lastIndexOf(".");
    inputFileExtension = shortFilename.substring(dotAt+1);
    if (Pattern.matches("kml", inputFileExtension.toLowerCase())) {
      isKml = true;
      loadKmlGazetteer();
    } 
    if (Pattern.matches("kmz", inputFileExtension.toLowerCase())) {
      isKmz = true;
      loadKmzGazetteer();
    } 
    if (Pattern.matches("xls", inputFileExtension)) {
      isXls = true;
      loadExcelGazetteer();
    }
    if (Pattern.matches("xlsx", inputFileExtension)) {
      isXlsx = true;
      loadExcelGazetteer();
    }
    if (Pattern.matches("txt", inputFileExtension)) {
      separator = "\t";
      isTxt = true;
      loadTextGazetteer();
    }
    if (Pattern.matches("csv", inputFileExtension)) {
      separator = ",";
      isCsv = true;
      loadTextGazetteer();
    }
    if(isXls == false && isXlsx == false && isTxt == false && isCsv == false
        && isKml ==false && isKmz == false){
      System.out.println("unknown file type, attempting to open it as a tab-separated text file");
      loadTextGazetteer();
    }
  }
  
  void loadKmlGazetteer() {
    isPolygons = false;
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      SAXParser saxParser = factory.newSAXParser();

      DefaultHandler handler = new DefaultHandler() {
          boolean tName = false;
          boolean tCoordinates = false;
          boolean tKml = false;
          boolean tPolygon = false;
          String currentName;
          String[] splitString;
          public void startElement(String uri, String localName, String qName, 
                                   Attributes attributes) throws SAXException {        
            if (qName.equalsIgnoreCase("KML")) {
              tKml = true;
            }
            if (qName.equalsIgnoreCase("NAME")) {
              tName = true;
            }
            if (qName.equalsIgnoreCase("COORDINATES")) {
              tCoordinates = true;
            }
            if (qName.equalsIgnoreCase("POLYGON")) {
              tPolygon = true;
            }
            foundString = "";
          }
   
          public void endElement(String uri, String localName, String qName) throws SAXException {
            if (tKml) {
              tKml = false;
            }
            if (tName) {
              currentName = new String(foundString);
              tName = false;
            }
            if (tCoordinates) {
              if(tPolygon){
                isPolygons = true;
                splitString = new String[3];
                splitString[0] = currentName;
                splitString[1] = "Polygon";
                splitString[2] = foundString.trim();
              }else{
                String toSplit = ",".concat(foundString);// one blank space for the name before coords
                splitString = toSplit.split(",");
                splitString[0] = currentName;
              }
              gazetteerDataSet.add(splitString);
              tCoordinates = false;
            }
          }
   
          public void characters(char ch[], int start, int length) throws SAXException {
            String toConc = new String(ch, start, length);
            if (toConc != null) {
              foundString = foundString.concat(toConc);
            }
          }
        };
   
      saxParser.parse(params.gazetteerFile, handler);
        
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  void loadKmzGazetteer(){
    // TODO extract the doc.kml and then use the same logic as the preceding method

  }

  void loadExcelGazetteer() {
    try {
      fileInputStream = new FileInputStream(params.gazetteerFile);
      try {
        workbook = WorkbookFactory.create(fileInputStream);
      } catch (InvalidFormatException e1) {
        e1.printStackTrace();
      }
      FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
      worksheet = workbook.getSheetAt(workbook.getActiveSheetIndex());
      Row headerRow = worksheet.getRow(0);
      dataColumns = headerRow.getLastCellNum();
      splitHeader = new String [dataColumns];
      Cell currCell;
      for (int clm = 0; clm < dataColumns; clm++) {
        currCell = headerRow.getCell(clm);
        splitHeader[clm] = this.readCellAsString(currCell, evaluator);
      }
      getColumnNumbers(splitHeader);
      dataRows = worksheet.getLastRowNum() + 1;
      for (int row = 1; row < dataRows; row++){
        String[] splitString = new String[dataColumns];
        try {
          Row nextRow = worksheet.getRow(row);
          Cell dataCell;
          for (int clm = 0; clm < dataColumns; clm++) {
            if (nextRow.getCell(clm) != null){
              dataCell = nextRow.getCell(clm);
              splitString[clm] = readCellAsString(dataCell, evaluator);
            } else {
              splitString[clm] = "";
            }
          }
          gazetteerDataSet.add(splitString);
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("got stuck on a line in a gazetteer, yo!");
        }
      }
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  void loadTextGazetteer() {
    try {
      br = new BufferedReader(new InputStreamReader(new FileInputStream
          (params.gazetteerFile), "UTF8"));
      String header = br.readLine();
      splitHeader = header.split(separator);
      dataColumns = splitHeader.length;
      getColumnNumbers(splitHeader);
      String strLine = "";
      while ((strLine = br.readLine()) != null) {
        String[] splitString = strLine.split(separator);
        gazetteerDataSet.add(splitString);
      }
      br.close();
    } catch (IOException e) {
      e.printStackTrace();
      System.out.println("Error reading file: " + fnm);
    }
  }
  
  void link(){
    if(isKmz || isKml){
      linkToKmlGazetteer();
    }else{
      linkToTextGazetteer();
    }
  }
  
  @SuppressWarnings("unchecked")
  void linkToTextGazetteer(){
    orphanList = "";
    params.linkedDataSet = new ArrayList<String[]>();
    foundOne = false;
    int strLen = params.inputHeaders.length + 2;
    String[] newHeaders = Arrays.copyOf(params.inputHeaders, strLen);
    params.inputLatC = strLen - 2;
    params.inputLongC = strLen - 1;
    newHeaders[params.inputLatC] = "Lat";
    newHeaders[params.inputLongC] = "Long";
    int dataLine = 0;
    for(String[] strs: params.inputDataSet){
      try{
        String str = strs[params.inputNameC];
        if(str.length() == 0){
          orphanList = orphanList.concat(Integer.toString(dataLine).concat
              (" The name column seems to be empty.\n"));
          dataLine++;
        }else{
          boolean foundAMatch = false;
          for(String[] gazStrs: gazetteerDataSet){
            foundAMatch = false;
            for(String s: gazStrs){
              if(stringMatcher.compare(s, str)== 0){
                String[] newString = new String[strLen];
                for(int t = 0; t < (strLen -2); t++){
                  try{
                    newString[t] = strs[t];
                  }catch(Exception e1){
                    newString[t] = "";
                  }
                }
                newString[strLen - 2] = gazStrs[gazLatC];
                newString[strLen - 1] = gazStrs[gazLongC];
                /*for(String sing: newString){
                  System.out.print(sing + "\t");
                }
                System.out.println();*/
                params.linkedDataSet.add(newString);
                foundAMatch = true; // Only confirming this now that we've actually added a linked line
                break; // If we've found a match and added linked line, one is enough (first find only)
              }
            }
            if(foundAMatch){
              foundOne = true; // indicates that there was at least one match in the sets
              break; // Don't search the rest of the gazetteer file (first match is enough)
            }
          }
          if(!foundAMatch){
            try{
              orphanList = orphanList.concat(Integer.toString(dataLine)
                  .concat(" ".concat(strs[params.inputNameC]).concat("\n")));
            }catch(Exception e){
              orphanList = orphanList.concat(Integer.toString(dataLine).concat
                  (" Something fishy with this row...\n"));
            }
          }
          dataLine++;
        }
      }catch(Exception e){
        orphanList = orphanList.concat(Integer.toString(dataLine).concat
            (" Something fishy with this row.\n"));
        dataLine++;
      }
    }
    if(foundOne){
      // Reset the headers to match the new dataset columns (add Lat and Long columns)
      int newHeaderLength = params.inputHeaders.length + 2;
      String[] tempHeader = Arrays.copyOf(params.inputHeaders, newHeaderLength);
      tempHeader[newHeaderLength - 2] = "Lat";
      tempHeader[newHeaderLength - 1] = "Long";
      params.linkedHeaders = Arrays.copyOf(tempHeader, tempHeader.length);
      params.latColumnExists = true; //TODO is this necessary?
      params.longColumnExists = true;//TODO is this necessary?
      params.dataSetLinked = true;
      params.outputDataSet = (ArrayList<String[]>) params.linkedDataSet.clone();
      params.outputHeaders = Arrays.copyOf(params.linkedHeaders, params.linkedHeaders.length);
    }   
  }
  
  @SuppressWarnings("unchecked")
  /* iterate through the raw input dataset, link each line to a lat
   * and long in the kml or kmz gazetteer, an create a linked dataset
   * including lat and long columns.*/
  void linkToKmlGazetteer(){
    orphanList = "";
    params.linkedDataSet = new ArrayList<String[]>();
    foundOne = false;
    gazLatC = 2; 
    gazLongC = 1;
    int strLen = params.inputHeaders.length + 2;
    String[] newHeaders = Arrays.copyOf(params.inputHeaders, strLen);
    params.inputLatC = strLen - 2;
    params.inputLongC = strLen - 1;
    newHeaders[params.inputLatC] = "Lat";
    newHeaders[params.inputLongC] = "Long";
    int dataLine = 0;
    for(String[] strs: params.inputDataSet){
      if(params.inputNameC > strs.length){ // Move on if a row isn't long enough to have a name column
        orphanList = orphanList.concat(Integer.toString(dataLine).concat
            (" This row seems to be missing columns.\n"));
        dataLine++;
      }else{
        try{
          String str = strs[params.inputNameC];
          if(str.length() == 0){
            orphanList = orphanList.concat(Integer.toString(dataLine).concat
                (" The name column seems to be empty.\n"));
            dataLine++;
          }else{
            boolean foundAMatch = false;
            for(String[] gazStrs: gazetteerDataSet){
              foundAMatch = false; 
              for(String s: gazStrs){
                if(stringMatcher.compare(s, str) == 0){
                  String[] newString;
                  /* in case a line in the input file is longer than the header, truncate it here*/
                  if(strs.length > strLen){
                    newString = new String[strLen];
                    for(int t = 0; t < (strLen -2); t++){
                      newString[t] = strs[t];
                    }
                    /* if no need to truncate, just copy the line*/
                  }else{
                    newString = Arrays.copyOf(strs, strLen);
                  }try{
                    // We've found a name match, now add the appropriate lat and long to the linked line
                    if(isPolygons){
                      String ring[] = gazStrs[2].split("\\s+");
                      Polygon polygon = new Polygon();
                      Random generator = new Random();
                      int points = ring.length;
                      for (int point = 0; point < points; point++) {
                        String ringPtCoord[] = ring[point].split(",");
                        double ptLong = Double.parseDouble(ringPtCoord[0]) * 10000000;
                        double ptLat = Double.parseDouble(ringPtCoord[1]) * 10000000;
                        int pointLong = (int) ptLong;
                        int pointLat = (int) ptLat;
                        polygon.addPoint(pointLong, pointLat);
                      }
                      Rectangle2D bounds = polygon.getBounds2D();
                      int rightBound = (int) bounds.getMaxX();
                      int leftBound = (int) bounds.getMinX();
                      int topBound = (int) bounds.getMaxY();
                      int bottomBound = (int) bounds.getMinY();
                      boolean foundAPointInThePolygon = false;
                      int randomX = 0; 
                      int randomY = 0;
                      while (!foundAPointInThePolygon) {
                        randomX = generator.nextInt(rightBound-leftBound) + leftBound;
                        randomY = generator.nextInt(topBound-bottomBound) + bottomBound;
                        if (polygon.contains(randomX, randomY)) {
                          foundAPointInThePolygon = true;
                        }
                      }
                      double randX = randomX;
                      double randY = randomY;
                      randX /= 10000000;
                      randY /= 10000000;
                      newString[strLen - 2] = Double.toString(randY);
                      newString[strLen - 1] = Double.toString(randX);
                    }else{
                      newString[strLen - 2] = gazStrs[gazLatC];
                      newString[strLen - 1] = gazStrs[gazLongC];
                    }
                    params.linkedDataSet.add(newString);
                    foundAMatch = true; // Only confirming this now that we've actually added a linked line
                    break; // If we've found a match and added linked line, one is enough (first find only)
                  }catch(Exception e){
                    // The gazetteer line with the name match didn't seem to have lat and long columns, so move on
                  }
                }
              }
              if(foundAMatch){
                foundOne = true; // indicates that there was at least one match in the sets
                break; // Don't search the rest of the gazetteer file (first match is enough)
              }
            }
            if(!foundAMatch){
              try{
                orphanList = orphanList.concat(Integer.toString(dataLine)
                    .concat(" ".concat(strs[params.inputNameC]).concat("\n")));
              }catch(Exception e){
                orphanList = orphanList.concat(Integer.toString(dataLine).concat
                    (" Something fishy with this row...\n"));
              }
            }
            dataLine++;
          }
        }catch(Exception e){
          orphanList = orphanList.concat(Integer.toString(dataLine).concat
              (" Something fishy with this row.\n"));
          dataLine++;
        }
      }
    }
    
    if(foundOne){
      // Reset the headers to match the new dataset columns (add Lat and Long columns)
      int newHeaderLength = params.inputHeaders.length + 2;
      String[] tempHeader = Arrays.copyOf(params.inputHeaders, newHeaderLength);
      tempHeader[newHeaderLength - 2] = "Lat";
      tempHeader[newHeaderLength - 1] = "Long";
      params.linkedHeaders = Arrays.copyOf(tempHeader, tempHeader.length);
      params.latColumnExists = true; //TODO is this necessary?
      params.longColumnExists = true;//TODO is this necessary?
      params.dataSetLinked = true;
      params.outputDataSet = (ArrayList<String[]>) params.linkedDataSet.clone();
      params.outputHeaders = Arrays.copyOf(params.linkedHeaders, params.linkedHeaders.length);
    }   
  }
  
 
  String readCellAsString(Cell cell, FormulaEvaluator eval){
    String toReturn = null;
    int cellType = cell.getCellType();
    switch(cellType) {
    case Cell.CELL_TYPE_NUMERIC: {
      double num = cell.getNumericCellValue();
      if(Math.floor(num) == num){
        int numInt = (int)num;
        toReturn = Integer.toString(numInt);
      }else{
        toReturn = Double.toString(cell.getNumericCellValue());
      }
      break;
      }
      case Cell.CELL_TYPE_STRING: {
        toReturn = cell.getStringCellValue();
        break;
      }
      case Cell.CELL_TYPE_FORMULA: {
        toReturn = Double.toString(eval.evaluate(cell).getNumberValue());
        break;
      }
      case Cell.CELL_TYPE_BLANK: {
        toReturn = "";
        break;
      }
    }
    return toReturn;
  }

  /*Figure out number of columns, which is which (getColumnNumbers),
   * populate array of max and min values.
   */
  
  /* reads through the header String[] looking for standard names, and assigns
   * the appropriate values to the latC, longC, and nameC variables; this allows
   * Joekit to know where to find the coordinates.  Contrary to an input file, a
   * gazetteer file MUST have correct column names for lat and long, otherwise it
   * won't link to the right columns.
   */
  // TODO(pablo): simplify
  void getColumnNumbers(String[] splitHeader) {
    gazLatC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<latColumnNames.length;iter++){
        if (Pattern.matches(latColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (gazLatC==0){
            gazLatC = it;
          }
          else{System.out.println("It seems that more than one column represents " +
          		"latitude in the gazetteer file");}
        }
      }
    }
	      
    gazLongC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<latColumnNames.length;iter++){
        if (Pattern.matches(longColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (gazLongC==0){
            gazLongC = it;
          }
          else{System.out.println("It seems that more than one column represents " +
          		"longitude in the gazetteer file");}
        }
      }
    }
  }
}