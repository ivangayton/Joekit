package net.joekit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LatLongCoordParser {
  double parseCoord(String coord) {
    try{
      double decimalCoord = 0;
      int norSou = 1;
      if (coord.length() == 0) {
        return decimalCoord;
      }
      if (coord.substring(0, 1).matches("-")) {
        norSou = -1;
      }
      Pattern pattern = Pattern.compile("[WS]");
      Matcher matcher = pattern.matcher(coord);
      if (matcher.find()) {
        norSou *= -1; 
      }
      String[] splitCoord = coord.split("[[^0-9\\.]+]");
      int parts = 0;
      for (String that:splitCoord) {
        if (!that.equalsIgnoreCase("")) {
          double aParsedDouble = 0;
          try {
            aParsedDouble = Double.parseDouble(that);
          } catch (Exception e) {
            return 0;
          }
          switch (parts) {
          case 0: {
            decimalCoord += aParsedDouble;
            parts += 1;
            break;
          }
          case 1: {
            decimalCoord += aParsedDouble/60;
            parts += 1;
            break;
          }
          case 2: {
            decimalCoord += aParsedDouble/3600;
            parts += 1;
            break;
          }
          }
        }
      }
      decimalCoord *= norSou;
      return decimalCoord;
    }catch(Exception e){
      return 0;
    }
    
  }
}
