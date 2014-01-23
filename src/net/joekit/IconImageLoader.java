package net.joekit;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class IconImageLoader{
  public BufferedImage icon;
  public BufferedImage coloredIcon;

  public IconImageLoader(ParameterSet params) {
    //loadIconImage(params);
  }
  
  BufferedImage loadIconImage(ParameterSet params){
    BufferedImage icon = null;
    String iconFileString;
    if(params.iconIsImageFile){
      // so it's a user-chosen file in the local system. Just load it.
      try {
        icon = ImageIO.read(params.iconFile);
      } catch (IOException e) {
        System.out.println("The local image file " + params.iconFile.getAbsolutePath()
            + " did not load");
      }
    }else{
      // Load it from the jar, no matter what OS we're in
      iconFileString = "/files/shaded_dot.png";
      try{
        InputStream stream = this.getClass().getResourceAsStream(iconFileString);
        Image image = ImageIO.read(stream);
        if(image.getWidth(null) > 0){
          icon = new BufferedImage(image.getWidth(null), image.getHeight(null)
              , BufferedImage.TYPE_INT_ARGB);
          Graphics gConv = icon.getGraphics();
          gConv.drawImage(image, 0, 0, null);
          gConv.dispose();
        }else{
          System.out.println(iconFileString  + " didn't consist of much.");
        }
      }catch(Exception e){
        System.out.println("tried to load " + iconFileString);
        e.printStackTrace();
      }
    }
    return icon;
  }
  
  void colorIcon(Color color){
    if(icon != null){
      int w = icon.getWidth();
      int h = icon.getHeight();
      coloredIcon = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      int[] rgbArray = new int[w * h];
      icon.getRGB(0, 0, w, h, rgbArray, 0, w);
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
      coloredIcon.setRGB(0, 0, w, h, rgbArray, 0, w);
    }
  }

}
