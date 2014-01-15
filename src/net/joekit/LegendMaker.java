package net.joekit;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class LegendMaker {
  public BufferedImage icon;
  public BufferedImage coloredIcon;
  
  Graphics g;
  
  LegendMaker(ParameterSet params){
    System.out.println("Trying to make a legend");
    // TODO set the width from the maximum line length 
    int lWidth = 300; 
    int lHeight = 20 + params.categoriesForColors.size() * 20;
    params.legendImage = new BufferedImage(lWidth, lHeight, BufferedImage.TYPE_INT_ARGB);
    g = params.legendImage.getGraphics();
    g.setColor(Color.white);
    g.fillRect(0, 0, lWidth, lHeight);
    g.setColor(Color.gray);
    g.drawRect(0, 0, lWidth - 1,lHeight -1);
    IconImageLoader iconImageLoader = new IconImageLoader(params);
    icon = iconImageLoader.loadIconImage(params);
    int row = 1;
    for(String[] s: params.categoriesForColors){
      colorIcon(Color.decode(s[1]));
      g.drawImage(coloredIcon, 10, 20 * row - 10, 25, 25, null);
      g.setColor(Color.black);
      g.drawString(s[0], 35, 8 + 20 * row);
      row++;
    }
    g.dispose();
  }
    
  // TODO replace with call to IconImageLoader
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
