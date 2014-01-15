package net.joekit;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.poi.ss.usermodel.DateUtil;
import org.pojava.datetime.*;

/* TODO go through all Date columns looking for months over 13 (to catch
 * American date formats).  Assume that of the first two two-digit numbers
 * separated by a delimiter, the one that goes over 12 represents day and 
 * the user is American.  If they both go over 12, then assume that the user 
 * is George W. Bush, or possibly Kid Rock.
 */
public class DateStringParser {
  Date parseDate(String dateString){
    DateTimeConfig.globalEuropeanDateFormat();
    Date dateToReturn = new Date();
    IsStringANumberChecker numCheck = new IsStringANumberChecker();
    if(numCheck.check(dateString)){
      dateToReturn = DateUtil.getJavaDate(Double.parseDouble(dateString));
      return dateToReturn;
    }else{
      DateTime dateTime = new DateTime();
      try{
        dateTime = DateTime.parse(dateString);
        dateToReturn = dateTime.toDate();
      }catch(Exception e){
        dateToReturn = null;
      }
      return dateToReturn;
    }
  }

  String dateStringToCorrectedString(String dateString){
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.applyPattern("yyyy-MM-dd");
    DateTimeConfig.globalEuropeanDateFormat();
    
    // first, try a straight-up number (if it is one, assume an Excel date)
    IsStringANumberChecker numCheck = new IsStringANumberChecker();
    String dateStringToReturn = new String();
    if(numCheck.check(dateString) == true){
      Date dateToReturn = DateUtil.getJavaDate(Double.parseDouble(dateString));
      dateStringToReturn = dateFormat.format(dateToReturn);
    }else{
      /* Ok, so it's not a straight-up number, let's try a date string,*/
      try{
        DateTime dateTime = new DateTime();
        dateTime = DateTime.parse(dateString);
        dateStringToReturn = dateTime.toDate().toString();
      }catch(Exception e){
        // Ok, that crashed, so let's try epi weeks
        try{
          Date dateWeek = new Date();
          dateWeek = parseEpiWeek(dateString);
          dateStringToReturn = dateFormat.format(dateWeek);
        }catch(Exception ef){
          ef.printStackTrace();
        }
        
      }
    }
    //System.out.println(dateString + " becomes " + dateStringToReturn);
    return dateStringToReturn;
  }
  
  Date parseEpiWeek(String epiWeekString){
    Date dateToReturn = new Date();
    try {
      String[] splitEpiWeekString = epiWeekString.split("-");
      int year = Integer.parseInt(splitEpiWeekString[0]);
      int week = Integer.parseInt(splitEpiWeekString[1]);
      Calendar calendar = new GregorianCalendar();
      calendar.set(Calendar.YEAR, year);
      calendar.set(Calendar.WEEK_OF_YEAR, week);
      calendar.set(Calendar.DAY_OF_WEEK, 1);
      dateToReturn = calendar.getTime();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return dateToReturn;
  }
}
