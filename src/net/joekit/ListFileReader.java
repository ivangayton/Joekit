package net.joekit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;


import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/* opens an input file, and bungs the contents into params.dataSet, which
 * is an ArrayList<String[]>  The first row of the file is stored in 
 * params.headers, a String[], and the dataset starts with the contents 
 * of the second row.  
 * 
 * This class doesn't really care what's in the file, it just stores the 
 * contents as strings.  The only thing it checks is for header rows; it
 * checks column names (in the header row) against a standard list, so if the 
 * input file has some standard column names it will automatically recognize 
 * them and store them in the ParameterSet, with the upshot that the user
 * doesn't have to manually tell Joekit which column represents latitude,
 * longitude, placename, etc.  This also helps align a gazetteer, since the 
 * gazetteer file linker checks for a match to the contents of a single column.
 */

class ListFileReader {
  File inputFile;
  ParameterSet params;
  String fnm;
  String separator = "\t";  // default separator for uknown file type is tab
  String inputFileExtension;
  String splitHeader[];
	
  int dataRows,dataColumns,row;
  
  boolean isXlsx = false, isXls = false, isCsv = false, isTxt = false, isVib = false;
  
  boolean hasEpiWeek = false;
  
  // This needs to be done with regex to match partials
  String[] latColumnNames = {"lat","latitude", "north", "northing", "ycoord"};
  String[] longColumnNames = {"long", "longitude", "east", "easting", "xcoord"};
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

  double[] columnMinValue, columnMaxValue;
  BufferedReader br;
  FileInputStream fileInputStream;
  Workbook workbook;
  Sheet worksheet;
	
  int latC, longC, nameC, labelC, sizeC, colorC, startDateC, endDateC, epiWeekC;
  
  @SuppressWarnings("unchecked")
  ListFileReader(File inptFl, ParameterSet pms) {
    inputFile = inptFl;
    params = pms;
    fnm = inputFile.getAbsolutePath();
    String shortFilename = inputFile.getName().toLowerCase();
    int dotAt = shortFilename.lastIndexOf(".");
    inputFileExtension = shortFilename.substring(dotAt+1);
    if (Pattern.matches("xls", inputFileExtension)) {
      isXls = true;
      initializeExcelFile(inptFl);
    }
    if (Pattern.matches("xlsx", inputFileExtension)) {
      isXlsx = true;
      initializeExcelFile(inptFl);
    }
    if (Pattern.matches("txt", inputFileExtension)) {
      separator = "\t";
      isTxt = true;
      initializeTextFile(inptFl);
    }
    if (Pattern.matches("csv", inputFileExtension)) {
      separator = ",";
      isCsv = true;
      initializeTextFile(inptFl);
    }
    if(Pattern.matches("vib", inputFileExtension)) {
      separator = ",";
      isVib = true;
      initializeVibFile(inptFl);
    }
    if(isXls == false && isXlsx == false && isTxt == false && isCsv == false
        && isVib == false){
      System.out.println
      ("unknown file type, attempting to open it as a tab-separated text file");
      initializeTextFile(inptFl);
    }

    /* Create a reference (shallow copy, or clone) of the dataset.  This doesn't
     * double up the memory required, because the outputDataSet is, unless we do
     * something to establish otherwise, just a reference to the same list.*/
    params.outputDataSet = (ArrayList<String[]>) params.inputDataSet.clone();
    params.inputHeaders = splitHeader;
    params.outputHeaders = params.inputHeaders;
  }
	
  void initializeVibFile(File inptFl) {
    /* this sets things up specifically for the export of an MSF-OCA
     * cholera epidemiological data tool. It's an inelegant mess,
     * because the exports were never really meant for this...*/
	try{
	  dataColumns = 14;
      splitHeader = new String[dataColumns];
      br = new BufferedReader(new FileReader(inputFile));
      String stringLine;
      int line = 1;
      /* Crash through the preamble of the export file, the end of which is 
       * signalled by a line reading "Main data".  For our purposes there
       * is nothing interesting in the preamble.*/
      while((stringLine = br.readLine()) != null) {
    	String s = (stringLine.replaceAll("\\[", "")).replaceAll("\\]", "");
        if(Pattern.matches("Main data", s)){
          break;
        }
        line++;
      }
      /* Now we're into the actual data, which in this export file is two
       * lines of text per data point.  We have to run through two lines at
       * a time, combining each two lines into one String[] of the ArrayList*/
      params.inputDataSet = new ArrayList<String[]>();
      String strLine = "";
      while((stringLine = br.readLine()) != null){
        String[] splitString = null;
        try {
          if ((strLine=br.readLine())!=null){
            String secondLine;
            if ((secondLine = br.readLine())!=null){
              strLine = strLine.concat("," + secondLine);
            }
            splitString = strLine.split(separator);
            for (int t = 0;t<splitString.length; t++){
              splitString[t] = (splitString[t].replaceAll("\"", "")).replaceAll("\\#", "");
            }
            params.inputDataSet.add(splitString);
          }
        } catch (IOException e) {
          System.out.println("Hi, something went wrong reading a line in the vib file");
        }
      }
      
      /* Set the paramaters based on what we know to be the setup of the cholera
       * tool export file. If something changes we're screwed, we have to trust
       * the epi data tool to provide a consistent column order.*/
      columnMinValue = new double[dataColumns];
      columnMaxValue = new double[dataColumns];
      splitHeader[0] = "CTC Name";
      splitHeader[1] = "Patient ID no";
      splitHeader[2] = "Date of Admission";
      splitHeader[3] = "Days since first symptoms";
      splitHeader[4] = "Age in years";
      splitHeader[5] = "Age in months";
      splitHeader[6] = "Sex (1 = M, 2 = F)";
      splitHeader[7] = "Address or village";
      splitHeader[8] = "Dehydration (1 = A, 2 = B, 3 = C)";
      splitHeader[9] = "Litres of ORS";
      splitHeader[10] = "Litres of Ringers";
      splitHeader[11] = "Outcome (1 = cured, 2 = died, 3 = LTF, 4 = trfd)";
      splitHeader[12] = "Date of exit";
      splitHeader[13] = "Time between admission and death";
      nameC = 7;
      startDateC = 2;
      labelC = 1;
      colorC = 8;
      dataRows = line;
	} catch(Exception e){
	  e.printStackTrace();
	}
  }
  
  void initializeExcelFile(File inptFl) {
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.applyPattern("yyyy-MM-dd");
    try {
      fileInputStream = new FileInputStream(inptFl);
      params.inputDataSet = new ArrayList<String[]>();
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
        int cellType = currCell.getCellType();
        switch(cellType) {
        case Cell.CELL_TYPE_NUMERIC: {
          splitHeader[clm] = Double.toString(currCell.getNumericCellValue());
          break;
        }
        case Cell.CELL_TYPE_STRING: {
          splitHeader[clm] = currCell.getStringCellValue();
          break;
        }
        case Cell.CELL_TYPE_FORMULA: {
          splitHeader[clm] = Double.toString(evaluator.evaluate(currCell).getNumberValue());
          break;
        }
        case Cell.CELL_TYPE_BLANK: {
          splitHeader[clm] = null;
          break;
        }
        }
      }
      getColumnNumbers(splitHeader);
      columnMinValue = new double[dataColumns];
      columnMaxValue = new double[dataColumns];
      dataRows = worksheet.getLastRowNum() + 1;
      LatLongCoordParser coordParser = new LatLongCoordParser();
      for (int row = 1; row < dataRows; row++){
        String[] splitString = new String[dataColumns];
        try {
          Row nextRow = worksheet.getRow(row);
          Cell dataCell;
          for (int clm = 0; clm < dataColumns; clm++) {
            if (nextRow.getCell(clm) != null){
              dataCell = nextRow.getCell(clm);
              int cellType = dataCell.getCellType();
              switch(cellType) {
              case Cell.CELL_TYPE_NUMERIC: {
                double num = dataCell.getNumericCellValue();
                if(HSSFDateUtil.isCellDateFormatted(dataCell)){
                  Date date = dataCell.getDateCellValue();
                  splitString[clm] = dateFormat.format(date);
                }else{
                  if(Math.floor(num) == num){
                    int numInt = (int)num;
                    splitString[clm] = Integer.toString(numInt);
                  }else{
                    splitString[clm] = Double.toString(dataCell.getNumericCellValue());
                  }
                }
                
                break;
                }
                case Cell.CELL_TYPE_STRING: {
                  splitString[clm] = dataCell.getStringCellValue();
                  break;
                }
                case Cell.CELL_TYPE_FORMULA: {
                  splitString[clm] = Double.toString(evaluator.evaluate(dataCell).getNumberValue());
                  break;
                }
                case Cell.CELL_TYPE_BLANK: {
                  splitString[clm] = "";
                  break;
                }
              }
            } else {
              splitString[clm] = "";
            }
          }
          params.inputDataSet.add(splitString);
        } catch (Exception e) {
          System.out.println("got stuck on a line!");
        } 				    	
        double currNum = 0;
        for (int clm = 0; clm < dataColumns; clm++) {
          if (clm == latC || clm == longC) {
            currNum = coordParser.parseCoord(splitString[clm]);
          } else {
            IsStringANumberChecker isIt = new IsStringANumberChecker();
            currNum = (isIt.check(splitString[clm]))
              ? Double.parseDouble(splitString[clm])
              : 0;
          }
			        	  
          if (row == 1) {
            columnMinValue[clm] = currNum;
            columnMaxValue[clm] = currNum;
          }
          if (currNum != 0) {
            if (currNum < columnMinValue[clm]) {
              columnMinValue[clm] = currNum;
            }
            if (currNum > columnMaxValue[clm]) {
              columnMaxValue[clm] = currNum;
            }
          }
        }
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Figure out number of columns, which is which (getColumnNumbers),
   * populate array of max and min values.
   */
  void initializeTextFile(File inptFl) {
    try {
      br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), "UTF8"));
      String header = br.readLine();
      splitHeader = header.split(separator);

      dataColumns = splitHeader.length;
	      
      getColumnNumbers(splitHeader);
	      
      columnMinValue = new double[dataColumns];
      columnMaxValue = new double[dataColumns];
      row = 1;
      params.inputDataSet = new ArrayList<String[]>();
      String strLine = "";
      LatLongCoordParser coordParser = new LatLongCoordParser();
      while ((strLine=br.readLine()) != null) {
        String[] splitString = strLine.split(separator);
        int columnsInThisLine = (dataColumns >= splitString.length)
          ? splitString.length
          : dataColumns;
        if (splitString.length > latC && splitString.length > longC) {
          double currNum = 0;
          for (int clm = 0; clm < columnsInThisLine; clm++) {
            if (clm == latC || clm == longC){
              currNum = coordParser.parseCoord(splitString[clm]);
            } else {
              IsStringANumberChecker isIt = new IsStringANumberChecker();
              currNum = (isIt.check(splitString[clm]))
                ? Double.parseDouble(splitString[clm])
                : 0;
            }
		        	  
            if (row == 1) {
              columnMinValue[clm] = currNum;
              columnMaxValue[clm] = currNum;
            }
            if (currNum != 0) {
              if (currNum < columnMinValue[clm]) {
                columnMinValue[clm] = currNum;
              }
              if (currNum > columnMaxValue[clm]) {
                columnMaxValue[clm] = currNum;
              }
            }
		        	  
          }
        }
        params.inputDataSet.add(splitString);
        row++;
      }
      dataRows = row;
      br.close();

    } catch (IOException e) {
      System.out.println("Error reading file: " + fnm);
    }
  }

  // TODO(pablo): simplify
  void getColumnNumbers(String[] splitHeader) {
    latC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<latColumnNames.length;iter++){
        if (Pattern.matches(latColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (latC==0){
            latC = it;
          }
          else{System.out.println("It seems that more than one column represents latitude");}
        }
      }
    }
	      
    longC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<latColumnNames.length;iter++){
        if (Pattern.matches(longColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (longC==0){
            longC = it;
          }
          else{System.out.println("It seems that more than one column represents longitude");}
        }
      }
    }
    nameC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<nameColumnNames.length;iter++){
        if (Pattern.matches(nameColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (nameC==0){
            nameC = it;
          }
          else{System.out.println("It seems that more than one column represents name");}
        }
      }
    }
    labelC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<labelColumnNames.length;iter++){
        if (Pattern.matches(labelColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (labelC==0){
            labelC = it;
          }
          else{System.out.println("It seems that more than one column represents name");}
        }
      }
    }
    sizeC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<sizeColumnNames.length;iter++){
        if (Pattern.matches(sizeColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (sizeC==0){
            sizeC = it;
          }
          else{System.out.println("It seems that more than one column represents size");}
        }
      }
    }
    colorC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<colorColumnNames.length;iter++){
        if (Pattern.matches(colorColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (colorC==0){
            colorC = it;
          }
          else{System.out.println("It seems that more than one column represents color");}
        }
      }
    }
    startDateC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<startDateColumnNames.length;iter++){
        if (Pattern.matches(startDateColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (startDateC==0){
            startDateC = it;
          }
          else{System.out.println("It seems that more than one column represents start date");}
        }
      }
    }
    endDateC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<endDateColumnNames.length;iter++){
        if (Pattern.matches(endDateColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          if (endDateC==0){
            endDateC = it;
          }
          else{System.out.println("It seems that more than one column represents end date");}
        }
      }
    }
    epiWeekC=0;
    for (int it=0;it<dataColumns;it++){
      for (int iter=0;iter<epiWeekColumnNames.length;iter++){
        if (Pattern.matches(epiWeekColumnNames[iter].toLowerCase().
            replaceAll("\\s+", ""), splitHeader[it].toLowerCase().replaceAll("\\s+", ""))){
          hasEpiWeek = true;
          if (epiWeekC==0){
            epiWeekC = it;
          }
          else{System.out.println("It seems that more than one column represents epi week");}
        }
      }
    }   
  }
}