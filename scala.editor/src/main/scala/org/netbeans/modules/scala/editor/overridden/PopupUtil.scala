/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.scala.editor.overridden

import java.awt.AWTEvent
import java.awt.Component
import java.awt.Container
import java.awt.Frame
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.event.AWTEventListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowStateListener
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.KeyStroke
import javax.swing.SwingUtilities
import org.openide.windows.WindowManager

/**
 *
 * @author phrebejk
 */
object PopupUtil  {
    
  // private static MyFocusListener mfl = new MyFocusListener();
    
  private val CLOSE_KEY = "CloseKey"; //NOI18N
  private val CLOSE_ACTION = new CloseAction
  private val ESC_KEY_STROKE = KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 )
        
  private val POPUP_NAME = "popupComponent" //NOI18N
  private var popupWindow: JDialog = _
  private val hideListener = new HideAWTListener
    
  def showPopup(content: JComponent, title: String) {
    showPopup(content, title, -1, -1, false)
  }
    
  def showPopup(content: JComponent, title: String, x: Int, y: Int, undecorated: Boolean) {
    showPopup(content, title, x, y, false, -1)
  }

  def showPopup(content: JComponent, title: String, x: Int, y: Int, undecorated: Boolean, altHeight: Int) {
    if (popupWindow ne null ) {
      return // Content already showing
    }
                           
    Toolkit.getDefaultToolkit.addAWTEventListener(hideListener, AWTEvent.MOUSE_EVENT_MASK)
        
    // NOT using PopupFactory
    // 1. on linux, creates mediumweight popup taht doesn't refresh behind visible glasspane
    // 2. on mac, needs an owner frame otherwise hiding tooltip also hides the popup. (linux requires no owner frame to force heavyweight)
    // 3. the created window is not focusable window
        
    popupWindow = new JDialog(getMainWindow)
    popupWindow.setName(POPUP_NAME)
    popupWindow.setUndecorated(undecorated);
    popupWindow.getRootPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( ESC_KEY_STROKE, CLOSE_KEY)
    popupWindow.getRootPane.getActionMap.put(CLOSE_KEY, CLOSE_ACTION)
	
    //set a11y
    val a11yName = content.getAccessibleContext.getAccessibleName
    if ((a11yName ne null) && a11yName != "")
      popupWindow.getAccessibleContext().setAccessibleName(a11yName)
    val a11yDesc = content.getAccessibleContext.getAccessibleDescription
    if ((a11yDesc ne null) && a11yDesc != "")
      popupWindow.getAccessibleContext.setAccessibleDescription(a11yDesc)
	    
    if ( title ne null ) {
      // popupWindow.setTitle( title );
    }
    // popupWindow.setAlwaysOnTop( true );
    popupWindow.getContentPane.add(content)
    // popupWindow.addFocusListener( mfl );
    // content.addFocusListener( mfl );
                
    WindowManager.getDefault.getMainWindow.addWindowStateListener(hideListener)
    WindowManager.getDefault.getMainWindow.addComponentListener(hideListener)
    resizePopup
        
    if (x != (-1)) {
      val p = fitToScreen(x, y, altHeight)
      popupWindow.setLocation(p.x, p.y)
            
    }
        
    popupWindow.setVisible(true)
    // System.out.println("     RFIW ==" + popupWindow.requestFocusInWindow() );
    content.requestFocus
    content.requestFocusInWindow
//        System.out.println("     has focus =" + content.hasFocus());
//        System.out.println("     has focus =" + popupWindow.hasFocus());
//        System.out.println("     window focusable=" + popupWindow.isFocusableWindow());
  }
    
  def hidePopup {
    if (popupWindow ne null) {
//            popupWindow.getContentPane().removeAll();
      Toolkit.getDefaultToolkit.removeAWTEventListener(hideListener)
            
      popupWindow.setVisible(false)
      popupWindow.dispose
    }
    WindowManager.getDefault.getMainWindow.removeWindowStateListener(hideListener)
    WindowManager.getDefault.getMainWindow.removeComponentListener(hideListener)
    popupWindow = null
  }

    
  private def resizePopup {
    popupWindow.pack
    val point = new Point(0,0)
    SwingUtilities.convertPointToScreen(point, getMainWindow)
    popupWindow.setLocation(point.x + (getMainWindow.getWidth  - popupWindow.getWidth)  / 2,
                            point.y + (getMainWindow.getHeight - popupWindow.getHeight) / 3)
  }
    
  private val X_INSET = 10
  private val Y_INSET = X_INSET
    
  private def fitToScreen(x: Int, y: Int, altHeight: Int): Point = {
        
    val screen = org.openide.util.Utilities.getUsableScreenBounds
                
    val p = new Point( x, y )
        
    // * Adjust the x postition if necessary
    if (p.x + popupWindow.getWidth > screen.x + screen.width - X_INSET) {
      p.x = screen.x + screen.width - X_INSET - popupWindow.getWidth
    }
        
    // * Adjust the y position if necessary
    if (p.y + popupWindow.getHeight > screen.y + screen.height - X_INSET) {
      p.y = p.y - popupWindow.getHeight - altHeight
    }
        
    p
  }

    
  private def getMainWindow: Frame = {
    WindowManager.getDefault.getMainWindow
  }
    
  // Innerclasses ------------------------------------------------------------
    
  private class HideAWTListener extends ComponentAdapter with  AWTEventListener with WindowStateListener {
        
    def eventDispatched(aWTEvent: java.awt.AWTEvent) {
      aWTEvent match {
        case mv: MouseEvent =>
          if (mv.getID == MouseEvent.MOUSE_CLICKED && mv.getClickCount > 0) {
            aWTEvent.getSource match {
              case comp: Component =>
                val par = SwingUtilities.getAncestorNamed(POPUP_NAME, comp) //NOI18N
                // Container barpar = SwingUtilities.getAncestorOfClass(PopupUtil.class, comp);
                // if (par eq null && barpar eq null) {
                if (par eq null) {
                  hidePopup
                }
              case _ => hidePopup; return
            }
          }
        case _ =>
      }
    }

    def windowStateChanged(windowEvent: WindowEvent) {
      if (popupWindow ne null ) {
        val oldState = windowEvent.getOldState
        val newState = windowEvent.getNewState
            
        if (((oldState & Frame.ICONIFIED) == 0) &&
            ((newState & Frame.ICONIFIED) == Frame.ICONIFIED)) {
          hidePopup
//                } else if (((oldState & Frame.ICONIFIED) == Frame.ICONIFIED) && 
//                           ((newState & Frame.ICONIFIED) == 0 )) {
//                    //TODO remember we showed before and show again? I guess not worth the efford, not part of spec.
        }
      }
    }
        
    override def componentResized(evt: ComponentEvent) {
      if (popupWindow ne null) {
        resizePopup
      }
    }
        
    override def componentMoved(evt: ComponentEvent) {
      if (popupWindow ne null) {
        resizePopup
      }
    }
        
  }
    
  private class MyFocusListener extends FocusListener {
        
    def focusLost(e: java.awt.event.FocusEvent) {
      println(e)
    }

    def focusGained(e: java.awt.event.FocusEvent) {
      println(e)
    }
                        
  }
    
  private class CloseAction extends AbstractAction {
        
    def actionPerformed(e: java.awt.event.ActionEvent) {
      hidePopup
    }                
  }
    
}
