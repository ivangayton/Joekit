package net.joekit;

import java.awt.Dimension;
import java.util.Vector; 

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
 
/**
 * got this workaround from the following bug: 
 *      http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4618607
 */
@SuppressWarnings("rawtypes")
public class WideComboBox extends JComboBox {

  private static final long serialVersionUID = 1L;
  private boolean layingOut = false;

  public WideComboBox() {
  }

  public WideComboBox(final Object items[]) {
    super(items);
  }

  public WideComboBox(Vector items) {
    super(items);
  }
 
  public WideComboBox(ComboBoxModel aModel) {
    super(aModel);
  }

  @Override
  public void doLayout() {
    try {
      layingOut = true;
      super.doLayout();
    } finally {
      layingOut = false;
    } 
  } 

  @Override
  public Dimension getSize() {
    Dimension dim = super.getSize(); 
    if(!layingOut) {
      dim.width = Math.max(dim.width, getPreferredSize().width);
    }
    return dim; 
  } 
}
