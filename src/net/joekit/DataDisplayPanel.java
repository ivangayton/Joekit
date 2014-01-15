package net.joekit;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Right hand side of the UI, shows a basic preview of the map data.
 * 
 * @author Ivan
 */
public class DataDisplayPanel extends JPanel {
	
  KmlWriterApplication kwa;
	
  int pWidth;
  int pHeight;
  int newWidth;
  int newHeight;
  double windowFactor;
	
  Graphics mapGraphics;
  BufferedImage mapImage = null;
  ParameterSet params;
  BufferedImage icon;
	
  DataDisplayPanel(KmlWriterApplication kwa, int width, int height){
    pWidth=width;
    pHeight=height;
    setBackground(Color.black);
    setPreferredSize(new Dimension(pWidth,pHeight));
  }
	
	
  @SuppressWarnings("unchecked")
  void setup(ParameterSet pms){
    params = pms;
    if (mapImage == null) {
      mapImage = new BufferedImage(pWidth, pHeight, BufferedImage.TYPE_INT_ARGB);
      mapGraphics = mapImage.getGraphics();
      if (mapImage == null) {
        System.out.println("mapImage is null");
        return;
      } else {
        mapGraphics = mapImage.getGraphics();
      }
    }
    
    /* TODO turn the loading of the icon into a class so that it can be rewritten; 
     * what a fucking mess!  It works differently in Windows vs. OSX, depending 
     * whether it's in Eclipse or in a runnable JAR file... */
    if(params.iconIsImageFile){
      try {
        /* TODO this works when you first select the image, but when refreshing 
         * it runs out of heap space if the icon image file is huge.  Temporarily
         * solved by the kludge below, which simply forestalls large images.  Still,
         * that's not ideal because some people may want to use large images.
         */
        icon = ImageIO.read(params.iconFile); 
        // Check that it's not too big (say 200 pixels), and scale it down if it is
        int iconLargestDim = icon.getWidth() > icon.getHeight() ? 
            icon.getWidth() : icon.getHeight();
        double largestWeWant = 200;
        if(iconLargestDim > largestWeWant){
          double scaleFactor = largestWeWant / iconLargestDim;
          int newWidth = (int) (icon.getWidth() * scaleFactor);
          int newHeight = (int) (icon.getHeight() * scaleFactor);
          BufferedImage resized = new BufferedImage
              (newWidth, newHeight, icon.getType());
          Graphics2D g = resized.createGraphics();
          g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
          g.drawImage(icon, 0, 0, newWidth, newHeight, 0, 0, icon.getWidth(), icon.getHeight(), null);
          g.dispose();
          icon = resized;
        }
      } catch (IOException e) {
        System.out.println("The local image file " + params.iconFile.getAbsolutePath()
            + " did not load");
      }
    }else{
      IconImageLoader iconImageLoader = new IconImageLoader(params);
      icon = iconImageLoader.loadIconImage(params);
  }

    @SuppressWarnings("rawtypes")
    final java.util.Map hints = new java.util.HashMap();
    hints.put(java.awt.RenderingHints.KEY_ANTIALIASING,
              java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    ((Graphics2D) mapGraphics).setRenderingHints(hints);
    mapGraphics.setColor(Color.black);
    mapGraphics.fillRect(0,0,pWidth,pHeight);
		
    double heightFactor = pHeight / Math.abs(params.latMax - params.latMin);
    double widthFactor = pWidth / Math.abs(params.longMax - params.longMin);
    if (widthFactor > heightFactor){
      windowFactor = heightFactor;
    } else {
      windowFactor = widthFactor;
    }
  }
	
  public void paintPanel(){
    Graphics g;
		
    try {
      g = getGraphics();
      if ((g != null) && (mapImage != null)) {
        g.drawImage(mapImage, 0, 0, null);
      }
      g.dispose();
    } catch (Exception e) {
      System.out.println("Graphics context error: " + e);  
    }
  }
	
  public void reset(int crns, double chsmlt) {
    newWidth = this.getWidth();
    newHeight = this.getHeight();
  }
	
  public void displayPoint(GeoPoint gp) throws IOException {
    double marginBuffer = 0.2;
    double sx = (Math.abs(gp.longitude - params.longMin) / (1 + marginBuffer) *
        windowFactor + pWidth * marginBuffer / 2);
    double sy = pHeight - (Math.abs(gp.latitude - params.latMin) / (1 + marginBuffer) *
         windowFactor + pHeight * marginBuffer / 2);
    int x = (int) (sx);
    int y = (int) (sy);
    double iconSize = gp.size;
    
    /* Recolor the actual image that paints to the panel.  We take whatever color is
     * selected by the user, and apply it subtractively to the icon color.  So if the
     * user chooses green, and the icon image file is white, it shows as green.  However,
     * if the icon file is red, and the user chooses green, it shows as black.  This is
     * as close as I could come in a quick hack to mimicking the behaviour of Google 
     * Earth with custom icons; it's a reasonable approximation of what you'll see in 
     * GE.  Slightly messy algorism, but there you go.  Might have problems with some
     * icon files (I don't know what happens if you try to load something with no Alpha
     * channel...  */
    // TODO this takes too long.  Would be way better with OpenGL...
    if(icon != null){
      int w = icon.getWidth();
      int h = icon.getHeight();
      BufferedImage iconToDraw = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      int[] rgbArray = new int[w * h];
      icon.getRGB(0, 0, w, h, rgbArray, 0, w);
      for(int i = 0; i < rgbArray.length; i++){
        Color col = new Color(rgbArray[i], true);
        int oldR = col.getRed();
        int oldG = col.getGreen();
        int oldB = col.getBlue();
        int oldA = col.getAlpha();
        int newR = (oldR > gp.color.getRed()) ? gp.color.getRed() : oldR;
        int newG = (oldG > gp.color.getGreen()) ? gp.color.getGreen() : oldG;
        int newB = (oldB > gp.color.getBlue()) ? gp.color.getBlue() : oldB;
        int newA = (oldA > gp.color.getAlpha()) ? gp.color.getAlpha() : oldA;
        col = new Color(newR, newG, newB, newA);
        rgbArray[i] = col.getRGB();
      }
      
      mapGraphics.setColor(gp.color);
      iconToDraw.setRGB(0, 0, w, h, rgbArray, 0, w);
      /* Custom icons (pictures) in Google Earth seem show up about 45 pixels wide 
       * onscreen when the icon size value is 1. Let's use 10 pixels as our default 
       * value, since the Joekit preview window is maybe 1/4 the screen */
      int iconHalfWidth = (int)(iconSize * 5);
      int iconHalfHeight = (int)(iconSize * 5 * h / w);
      int iconWidth = (iconHalfWidth < 1) ? 1 : iconHalfWidth * 2;
      int iconHeight = (iconHalfHeight < 1) ? 1 : iconHalfHeight * 2;
      mapGraphics.drawImage(iconToDraw, x - iconHalfWidth, y - iconHalfHeight,
          iconWidth, iconHeight, null);
    }else{
      mapGraphics.setColor(gp.color);
      int iconScreenSize = (int)(iconSize * 8);
      mapGraphics.fillOval(x, y, iconScreenSize, iconScreenSize);
      // TODO display an error to the user concerning their unworkable image file
    }
  }

  /**
   * Dummy value to quiet serialization checks.  This class should
   * not be serialized.
   */
  private static final long serialVersionUID = 1L;
}
