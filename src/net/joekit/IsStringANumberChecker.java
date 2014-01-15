package net.joekit;

/**
 * Just what the name suggests.  
 * TODO add some hints if a number seems to be a date or some 
 * coordinates.
 * 
 * @author Ivan
 */
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IsStringANumberChecker {
  boolean check(String isIt) {  
    boolean isValid = false;   
    String expression = "[-+]?[0-9]*\\.?[0-9]+$";  
    CharSequence inputStr = isIt;  
    Pattern pattern = Pattern.compile(expression); 
    try{
      Matcher matcher = pattern.matcher(inputStr);  
    if (matcher.matches()) {  
      isValid = true;  
    }  
    }catch(Exception e){
      isValid = false;
    }
    
    return isValid; 
  }
}
