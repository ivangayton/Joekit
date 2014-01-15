package net.joekit;

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

/*
 * The GeoPoint class stores a coordinate and related information used
 * to populate a KML file.
 * 
 * The constructor accepts a string as input, and uses the column numbers 
 * in the parameter set to figure out which column is which.
 */
class GeoPoint {
  IsStringANumberChecker numChecker = new IsStringANumberChecker();
  static final SimpleDateFormat dateFormat = new SimpleDateFormat();

  double latitude = 0, longitude = 0;
  double size;
  String name, label, description;
  double colorNum;
  Color color;
  boolean valid = false;
  Date startDate, endDate;
  int instances = 1;

  GeoPoint(String[] nL, ParameterSet params) {
    dateFormat.applyPattern("yyyy-MM-dd");
    DateStringParser dateParser = new DateStringParser();
    if (params.animate) {
      try{
        if(params.hasEpiWeek){
          startDate = dateParser.parseEpiWeek(nL[params.inputEpiWeekC]);
        }else{
          try{
            startDate = dateParser.parseDate(nL[params.startDateC]);
            // throw out the point if bad date
            if (startDate == null) {
              valid = false;
            }
            if (!params.endDateExists) {
              Calendar calendar = new GregorianCalendar();
              calendar.setTime(startDate);// TODO crashed here null pointer
              calendar.add(Calendar.DAY_OF_YEAR, params.daysPerEvent);
              endDate = calendar.getTime();
            } else {
              endDate = dateParser.parseDate(nL[params.endDateC]);
              if(endDate == null){
                valid = false;
              }
            }
          }catch (Exception e){
            e.printStackTrace();
            valid = false;
          }
        }
        
      }catch (Exception e){
        e.printStackTrace();
        valid = false;
      }
    }

    try {
      if (nL.length > params.nameC) {
        name = nL[params.nameC];
      } else {
        name = "";
      }
    } catch (Exception e) {
      e.printStackTrace();
      valid = false;
    }

    try{
      // Deleted section that used a gazetteer linker to find lat and long
      LatLongCoordParser coordParser = new LatLongCoordParser();
      if(nL.length > params.latC && params.latColumnExists){
        latitude = coordParser.parseCoord(nL[params.latC]);
      }
      if(nL.length > params.longC && params.longColumnExists){
        longitude = coordParser.parseCoord(nL[params.longC]);
      }
      
      if (latitude != 0 && longitude != 0) {
        valid = true;
      } else {
        valid = false;
      }
    } catch (Exception e) {
      e.printStackTrace();
      valid = false;
    }
		
    try{
      if (nL.length > params.labelC) {
        label = nL[params.labelC];
      } else {
        label = name;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      if (params.variableIconSize) {
        if (numChecker.check(nL[params.sizeC])) {
          if(params.logarithmicSize){
            size = Math.log(Double.parseDouble(nL[params.sizeC]))
                           * params.iconSizeFactor;
          }else{
            size = Math.sqrt(Double.parseDouble(nL[params.sizeC]) 
                           * params.iconSizeFactor);
          }
          if(size < params.defaultIconSize){
            size = params.defaultIconSize;
          }
        } else {
          size = 0;
        }
      
      } else {
        size = params.defaultIconSize;
      }
    } catch (Exception e) {
      e.printStackTrace();
      valid = false;
    }
		
    color = params.defaultIconColor;
    try {
      if (params.variableIconColor) {
        if(params.colorCategories){
          // create a comparator that ignores case and whitespace.  
          Comparator<String> categorizer = new Comparator<String>(){
            @Override
            public int compare(String string1, String string2) {
              return string1.replaceAll("\\s+", "").compareToIgnoreCase
                  (string2.replaceAll("\\s+", ""));
            }
          };
          for(String[] s: params.categoriesForColors){
            if(categorizer.compare(s[0], nL[params.colorC]) == 0){
              color = Color.decode(s[1]);
            }
          }
        }else{
          if (nL.length > params.colorC) {
            if (numChecker.check(nL[params.colorC])) {
              colorNum = Double.parseDouble(nL[params.colorC]);
              color = figureOutColor(colorNum, params);
            }
          }
        }
        
      } else {
        color = params.defaultIconColor;
      }
    } catch (Exception e) {
      e.printStackTrace();
      valid = false;
    }
		
    try {
      description = "";
      try{
        int clm = 0;
        for (String str : params.outputHeaders) {
          description = description.concat(str);
          description = description.concat(": ");
          if (nL.length > clm) {
            if(nL[clm] != null){
              description = description.concat(nL[clm]);
              description = description.concat("\n");
            }
          }
          clm++;
        }
      }catch(Exception e){
        e.printStackTrace();
        valid = false;
      }
      
      if(params.animate){
        try{
          if(params.endDateExists){
            String endDateString = "End date: " + dateFormat.format(endDate) + "\n";
            description = endDateString.concat(description);
          }
          String dateString = "Date: " + dateFormat.format(startDate) + "\n";// TODO crashed here null pointer
          description = dateString.concat(description);
        } catch(Exception e){
          e.printStackTrace();
          valid = false;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      valid = false;
    }
  }
	
  Color figureOutColor(Double colorNumber, ParameterSet params) {
    int red;
    int green;
    int blue;
		
    if (colorNumber > params.colorMid) {
      double range = params.colorMax - params.colorMid;
      double distance = (colorNumber - params.colorMid)/range;
      red = (int) (params.middleIconColor.getRed()
                   + (params.maxIconColor.getRed()
                      - params.middleIconColor.getRed())
                   * distance);
      green = (int) (params.middleIconColor.getGreen()
                     + (params.maxIconColor.getGreen()
                        - params.middleIconColor.getGreen())
                     * distance);
      blue = (int) (params.middleIconColor.getBlue()
                    + (params.maxIconColor.getBlue()
                       - params.middleIconColor.getBlue())
                    * distance);
    } else {
      double range = params.colorMid - params.colorMin;
      double distance = (colorNumber - params.colorMin)/range;
      red = (int) (params.defaultIconColor.getRed()
                   + (params.middleIconColor.getRed()
                      - params.defaultIconColor.getRed())
                   * distance);
      green = (int) (params.defaultIconColor.getGreen()
                     + (params.middleIconColor.getGreen()
                        - params.defaultIconColor.getGreen())
                     * distance);
      blue = (int) (params.defaultIconColor.getBlue()
                    + (params.middleIconColor.getBlue()
                       - params.defaultIconColor.getBlue())
                    * distance);

    }
    // TODO(pablo): simplify.
    if (red > 255) {
      red = 255;
    }
    if (red < 0) {
      red = 0;
    }

    if (green > 255) {
      green = 255;
    }
    if (green < 0) {
      green = 0;
    }

    if (blue > 255) {
      blue = 255;
    }
    if (blue < 0) {
      blue = 0;
    }

    Color colorize = new Color(red, green, blue);
    return colorize;
  }
}
