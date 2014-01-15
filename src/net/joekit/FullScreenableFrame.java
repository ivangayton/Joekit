package net.joekit;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import javax.swing.JFrame;

/**
 * Utility class for creating a full-screen frame.  From:
 *
 *   http://code.google.com/p/freality
 *
 * @author Pablo Mayrgundter
 */
public class FullScreenableFrame extends JFrame {

  /* TODO I've temporarily dropped the default width to 700, since on my MSF laptop 
   * (lo-res screen) the console panel was  1015 px wide, squeezing the preview panel
   * completely out of the way.  I'm not sure what the System.getProperty("width"... 
   * is supposed to read; it doesn't seem to be the screen resolution and on my 
   * computer it returns null.  Ideally the logic should be that the console panel is
   * about 700 pixels wide (enough for all of the widgets as they're currently laid out)
   * and the preview panel gets the rest of the real estate?  I've probably just 
   * misunderstood something in the logic here, so until I catch on I'm using this 
   * silly workaround of dropping the default return on the getProperty call to 700  */
  static final int WIDTH = Integer.parseInt(System.getProperty("width", "680"));//"700"));
  static final int HEIGHT = Integer.parseInt(System.getProperty("height", "700"));//"768"));
  static final Boolean FULLSCREEN = Boolean.getBoolean("fs");

  protected int width, height;
  Graphics2D drawGraphics;

  public FullScreenableFrame(String title) {
    this(title, WIDTH, HEIGHT, FULLSCREEN);
  }

  public FullScreenableFrame(String title, int width, int height, boolean fullscreen) {
    super(title);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    if (fullscreen) {
      setFullScreenSize();
    } else {
      setRegularSize();
    }
  }

  void setRegularSize() {
    this.width = WIDTH;
    this.height = HEIGHT;
    setSize(width, height);
  }

  void setFullScreenSize() {
    setUndecorated(true);
    final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    final GraphicsDevice device = env.getDefaultScreenDevice();
    final GraphicsConfiguration config = device.getDefaultConfiguration();
    device.setFullScreenWindow(this);
    this.width = (int)config.getBounds().getWidth();
    this.height = (int)config.getBounds().getHeight();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public Graphics2D getDrawGraphics() {
    return getDrawGraphics(Color.BLACK);
  }

  @SuppressWarnings(value="unchecked")
  public Graphics2D getDrawGraphics(final Color bgColor) {
    if (drawGraphics == null) {
      drawGraphics = (Graphics2D) getContentPane().getGraphics();
      @SuppressWarnings("rawtypes")
      final java.util.Map hints = new java.util.HashMap();
      hints.put(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
      drawGraphics.setRenderingHints(hints);
      drawGraphics.setColor(bgColor);
      drawGraphics.fillRect(0, 0, width, height);
    }
    return drawGraphics;
  }

  public String toString() {
    return String.format("{%s@%d: width: %d, height: %d}",
                         this.getClass().getName(),
                         System.identityHashCode(this), width, height);
  }

  /**
   * Dummy value to quiet serialization checks.  This class should
   * not be serialized.
   */
  private static final long serialVersionUID = 1L;
}
