package net.joekit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

class NaiveKmlFileWriter {
  ListFileReader lfr;

  NaiveKmlFileWriter(ParameterSet params) {
    SimpleDateFormat dateFormat = new SimpleDateFormat();
    dateFormat.applyPattern("yyyy-MM-dd");
    try {
      String outputFilename = params.outputFile.getAbsolutePath();
      if(params.isKmzRatherThanKml){
        outputFilename = outputFilename.concat(".temp");
      }
      File tempOutputFile = new File (outputFilename);
      OutputStreamWriter bw =
        new OutputStreamWriter(new FileOutputStream(tempOutputFile.getAbsolutePath()),"UTF-8");
      bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
      // Missing some gibberish that identifies kml standards.
      bw.write("<kml>\n");
      bw.write("<Document>\n");
      bw.write("\t<name>"+params.inputFile.getName()+"</name>\n");
      
      // Legend
			if(params.legendShouldBeDisplayed){
			  writeLegendFile(params);
			  bw.write("<Folder>\n");
	      bw.write("<name>" + "Legend" + "</name>\n");
        bw.write("<ScreenOverlay>" + "\n" + 
            "\t<name>Legend</name>" + "\n" +
            "\t<drawOrder>99</drawOrder>" + "\n" + 
            "\t<Icon>\n" +
            "\t\t<href>files/legend.png</href>" + "\n" +
            "\t</Icon>" + "\n");
        bw.write("\t<overlayXY x=\"0\" y=\"0\" xunits=\"fraction\" yunits=\"fraction\"/>" + "\n");
        bw.write("\t<screenXY x=\"0\" y=\"25\" xunits=\"fraction\" yunits=\"pixels\"/>" + "\n");
        bw.write("\t<rotationXY x=\"0.5\" y=\"0.5\" xunits=\"fraction\" yunits=\"fraction\"/>" + "\n");
        bw.write("\t<size x=\"-1\" y=\"-1\" xunits=\"pixels\" yunits=\"pixels\"/>" + "\n");
        bw.write("</ScreenOverlay>\n");
        bw.write("</Folder>\n");
      }
			
      // Folder.
      bw.write("\t<Folder>\n");
      bw.write("\t\t<name>" + "Points" + "</name>\n");

      // NOW BEGIN LOOP ADDING PLACEMARKS.
      String dateString;
      for(String[] newLine: params.outputDataSet) {
        GeoPoint gp = new GeoPoint(newLine, params);
        if (gp.valid) {
          try{
            //placemark\
            bw.write("\t\t<Placemark>\n");
            if (params.animate) {
              bw.write("\t\t\t<TimeSpan>\n");
              dateString = dateFormat.format(gp.startDate);
              bw.write("\t\t\t\t<begin>" + dateString + "</begin>\n");
              dateString = dateFormat.format(gp.endDate);
              bw.write("\t\t\t\t<end>" + dateString + "</end>\n");
              bw.write("\t\t\t</TimeSpan>\n");
            }
            bw.write("\t\t\t<name>" + xmlCharReplace(gp.label) + "</name>\n");
            bw.write("\t\t\t<description>");
            if (gp.description != null){
              bw.write(xmlCharReplace(gp.description));
            }
            bw.write("</description>\n");
            
            bw.write("\t\t\t<Style>\n");
            bw.write("\t\t\t\t<IconStyle>\n");
            bw.write("\t\t\t\t\t<color>" + reverseRGBString(Integer.toHexString(gp.color.getRGB())) 
                     + "</color>\n");
            bw.write("\t\t\t\t\t<scale>" + Double.toString(gp.size) + "</scale>\n");			
            bw.write("\t\t\t\t\t<Icon>\n");
            bw.write("\t\t\t\t\t\t<href>" + params.iconFileName + "</href>\n");
            bw.write("\t\t\t\t\t</Icon>\n");
            bw.write("\t\t\t\t</IconStyle>\n");
  					
            //label size and color
            bw.write("\t\t\t\t<LabelStyle>\n");
            bw.write("\t\t\t\t\t\t<color>" + reverseRGBString(Integer.toHexString(params.labelColor.getRGB())) + 
                     "</color>\n");
            bw.write("\t\t\t\t\t\t<scale>" + Double.toString(params.labelSize) + "</scale>\n");
            bw.write("\t\t\t\t</LabelStyle>\n");
            bw.write("\t\t\t</Style>\n");
            bw.write("\t\t\t<Point>\n");
            bw.write("\t\t\t\t<altitudeMode>" + "clampedToGround" +"</altitudeMode>\n");
            bw.write("\t\t\t\t<coordinates>" + //"-" +
                     Double.toString(gp.longitude) + "," + Double.toString(gp.latitude) + 
                     "</coordinates>\n");
            bw.write("\t\t\t</Point>\n");
  						
            bw.write("\t\t</Placemark>\n");
          }catch(Exception e){
            System.out.println("NaiveKmlFileWriter didn't like point ");
            for(String s: newLine){
              System.out.print(s + "\t");
            }
          }
        }
      }
    
      	
      //STOP LOOPING
			
      //unfolder
      bw.write("\t</Folder>\n");
		
      bw.write("</Document>\n");
      bw.write("</kml>");
      bw.flush();
      bw.close();
      /* Now to make it into a .kmz file.  A .kmz is a compressed (zipped) file
  		 * with the extension .kmz rather than .zip (to tell the OS that it's a Google
  		 * Earth file).  The zipped file contains a single kml file, called doc.kml by 
  		 * convention.  The zipped file also contains other files such as images; this 
  		 * is how we are able to include custom icons, legends, and so forth.  The 
  		 * doc.kml file refers to other files within the zipped .kmz file.  The 
  		 * convention is that there is a subfolder in the .kmz called "files" which
  		 * contains all of the stuff (images and so forth) referred to by doc.kml.
      */
      if(params.isKmzRatherThanKml){
        byte[] buf = new byte[1024];
        
        // Set the name of the .kmz file
        int extensionIndex = params.outputFile.getAbsolutePath().lastIndexOf(".");
        if (extensionIndex == 0) {
          extensionIndex = params.inputFile.getAbsolutePath().length();
        }
        String kmzNameString = params.outputFile.getAbsolutePath().substring(0, extensionIndex)
            + ".kmz";
        try {
          ZipOutputStream kmzOut = new ZipOutputStream(new BufferedOutputStream(
              new FileOutputStream(kmzNameString)));
          
          // write the doc.kml file into the root of the .kmz zipfile structure
          FileInputStream inputFileStream = new FileInputStream(tempOutputFile.getAbsolutePath());
          kmzOut.putNextEntry(new ZipEntry("doc.kml"));
          int inputFileLen;
          while ((inputFileLen = inputFileStream.read(buf)) > 0){
            kmzOut.write(buf, 0, inputFileLen);
          }
          kmzOut.closeEntry();
          inputFileStream.close();
          
          // write the legend file in a files folder within the .kmz zipfile structure
          if(params.legendShouldBeDisplayed){
            FileInputStream legendFileStream = new FileInputStream(params.legendFile.getAbsolutePath());      
            kmzOut.putNextEntry(new ZipEntry("files" + File.separatorChar + params.legendFile.getName()));
            int legendFileLen;
            while((legendFileLen = legendFileStream.read(buf)) > 0){
              kmzOut.write(buf, 0, legendFileLen);
            }
            kmzOut.closeEntry();
            legendFileStream.close();
          }
          
          // write the iconfile in a files folder within the .kmz zipfile structure
          if(params.iconIsImageFile){
            FileInputStream iconFileStream = new FileInputStream(params.iconFile.getAbsolutePath());      
            kmzOut.putNextEntry(new ZipEntry("files" + File.separatorChar + params.iconFile.getName()));
            int iconFileLen;
            while((iconFileLen = iconFileStream.read(buf)) > 0){
              kmzOut.write(buf, 0, iconFileLen);
            }
            kmzOut.closeEntry();
            iconFileStream.close();
          }
          
          kmzOut.close();
        } catch (Exception e) {
          e.printStackTrace();
        }
        
        /* Delete the .kml file, leaving only the .kmz. The .kml file won't work if there's
         * a custom icon, because the reference to the icon file is relative, looking for a 
         * folder called files in the .kmz zipfile structure)
         */
        tempOutputFile.delete();
      }
    } catch (IOException e) {
      System.out.println("there has been some kind of problem opening a filewriter");
      e.printStackTrace();
    }
  }

  String reverseRGBString(String reversible){
    String reversedRGBString = reversible.substring(0, 2) + reversible.substring(6, 8) 
      + reversible.substring(4,6) + reversible.substring(2,4);
    return reversedRGBString;
  }
  
  void writeLegendFile(ParameterSet params){
    System.out.println("Attempting to write a legend file");
    try {
      int pathLength = (int) params.outputFile.getAbsolutePath().length() -
          params.outputFile.getName().length();
      String sep = System.getProperty("file.separator");
      String path = params.outputFile.getAbsolutePath().substring(0, pathLength) +
          sep + "files" + sep + "legend.png";
      System.out.println("path: " + path);
      params.legendFile = new File(path);
   // grab file's parent directory structure
      File destinationParent = params.legendFile.getParentFile();
      // create the parent directory structure if needed
      destinationParent.mkdirs();
      ImageIO.write(params.legendImage, "png", params.legendFile);
      System.out.println("Created a legend file at" + params.legendFile.getAbsolutePath());
      params.legendShouldBeDisplayed = true;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  String xmlCharReplace(String toReplace) {
    String string = toReplace;
    try{
      string = string.replaceAll("<", "&lt;");
      string = string.replaceAll(">", "&gt;");
      string = string.replaceAll("&", "&amp;");
      string = string.replaceAll("'", "&apos;");
      string = string.replaceAll("\"", "&quot;");
    }catch(Exception e){
    	System.out.println("Something was wacky with the xml character replacements " +
    			"for: " + string);
    }
    return string;
  }
}
