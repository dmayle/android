/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.idea.assistant.view;

import com.android.tools.idea.assistant.datamodel.StepData;
import com.android.tools.idea.assistant.datamodel.StepElementData;
import com.android.tools.idea.structure.services.DeveloperService;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Renders a single step inside of a tutorial.
 *
 * TODO: Move render properties to a form.
 */
public class TutorialStep extends JPanel {
  private static final Logger logger = Logger.getLogger(TutorialStep.class.getName());

  private final int myIndex;
  private final StepData myStep;
  private final JPanel myContents;
  private final Project myProject;

  TutorialStep(StepData step, int index, ActionListener listener, DeveloperService service) {
    myIndex = index;
    myStep = step;
    myProject = service.getModule().getProject();
    myContents = new JPanel();
    setOpaque(false);
    setLayout(new GridBagLayout());

    // TODO: Consider the setup being in the ctors of customer inner classes.
    initStepNumber();
    initLabel();
    initStepContentsContainer();

    for (StepElementData element : step.getStepElements()) {
      // element is a wrapping node to preserve order in a heterogeneous list,
      // hence switching over type.
      switch (element.getType()) {
        case SECTION:
          // TODO: Make a custom inner class to handle this.
          JTextPane section = new JTextPane();
          section.setOpaque(false);
          section.setBorder(BorderFactory.createEmptyBorder());
          UIUtils.setHtml(section, element.getSection());
          myContents.add(section);
          break;
        case ACTION:
          myContents.add(new StatefulButton(element.getAction(), listener, service));
          break;
        case CODE:
          CodePane code = new CodePane(element);
          myContents.add(code);
          break;
        default:
          logger.log(Level.SEVERE, "Found a StepElement of unknown type. " + element.toString());
      }
      // Add 10px spacing between elements.
      myContents.add(Box.createRigidArea(new Dimension(0, 10)));
    }
    // NOTE: Due to some calculation issues with html rendered content, we're forcing the element to do a relayout.
    // TODO: SwingUtilities.updateComponentTreeUI(this) reverted some overridden properties and is no longer in use even though it might
    // generally have been the better choice.Investigate the root cause and determine the best path forward.
    invalidate();
    validate();
    repaint();
  }

  /**
   * Create and add the step label.
   */
  private void initLabel() {
    JLabel label = new JLabel(myStep.getLabel());
    Font font = label.getFont();
    Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
    label.setFont(boldFont);

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 1;
    c.gridy = 0;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.insets = new Insets(7, 0, 10, 5);

    add(label, c);
  }

  /**
   * Configure and add the container holding the set of step elements.
   */
  private void initStepContentsContainer() {
    myContents.setLayout(new BoxLayout(myContents, BoxLayout.Y_AXIS));
    myContents.setOpaque(false);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 1;
    c.gridy = 1;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.NORTHWEST;
    c.insets = new Insets(0, 0, 0, 5);

    add(myContents, c);
  }

  /**
   * Create and add the step number indicator. Note that this is a custom
   * display that surrounds the number with a circle thus has some tricky
   * display characteristics. It's unclear if a form can be leveraged for this.
   */
  private void initStepNumber() {
    // Get standard label font.
    Font font = new JLabel().getFont();
    JTextPane stepNumber = new JTextPane();
    stepNumber.setEditable(false);
    stepNumber.setText(myIndex + "");
    Font boldFont = new Font(font.getFontName(), Font.BOLD, 11);
    stepNumber.setFont(boldFont);
    stepNumber.setOpaque(false);
    stepNumber.setForeground(UIUtils.getLinkColor());
    stepNumber.setBorder(new NumberBorder());
    Dimension size = new Dimension(21, 21);
    stepNumber.setSize(size);
    stepNumber.setPreferredSize(size);
    stepNumber.setMinimumSize(size);
    stepNumber.setMaximumSize(size);

    StyledDocument doc = stepNumber.getStyledDocument();
    SimpleAttributeSet center = new SimpleAttributeSet();
    StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
    doc.setParagraphAttributes(0, doc.getLength(), center, false);

    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    c.gridy = 0;
    c.weightx = 0;
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.CENTER;
    c.insets = new Insets(5, 5, 5, 5);

    add(stepNumber, c);
  }

  /**
   * A custom border used to create a circle around a specifically sized step number.
   * TODO: Adjust values further to match specs.
   */
  class NumberBorder extends AbstractBorder {
    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Graphics2D g2 = (Graphics2D)g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      // Shrink the diameter by 1px as attempting to use full bounds results in clipping.
      int d = height - 1;
      g2.setColor(UIUtils.getLinkColor());
      g2.drawOval(x, y, d, d);
      g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
      return new Insets(2, 2, 2, 2);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
      insets.left = insets.right = 2;
      insets.top = insets.bottom = 2;
      return insets;
    }
  }

  /**
   * A read-only code editor designed to display code samples, this should live inside a
   * {@code NaturalHeightScrollPane} to render properly.
   *
   * TODO: Add a listener to select all code when clicked. Potentially do a hover listener instead that surfaces a button that copies all
   * content to your clipboard.
   * TODO(b/28357327): Try to reduce the number of hacks and fragile code paths.
   */
  private class CodePane extends EditorTextField {

    private static final int PAD = 5;
    private static final int MAX_HEIGHT = 500;

    // Scrollbar height, used for calculating preferred height when the horizontal scrollbar is present. This is somewhat of a hack in that
    // the value is set as a side effect of the scroll pane being instantiated. Unfortunately the pane is released before we can get access
    // so we cache the value (which should be the same across instantiations) each time the scrollpane is created.
    private int myScrollBarHeight = 0;

    public CodePane(StepElementData element) {
      // TODO: Use the file type hint from the element when supported.
      super(element.getCode(), myProject, StdFileTypes.JAVA);
      // NOTE: Monospace must be used or the preferred width will be inaccurate (most likely due to line length calculations based on the
      // width of a sample character.
      setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
      ensureWillComputePreferredSize();
      Document doc = getDocument();
      getDocument().setReadOnly(true);

      // NO-OP to ensure that the editor is created, which has the side effect of instantiating the scroll pane.
      getPreferredSize();

      int height = Math.min(MAX_HEIGHT, getActualPreferredHeight() + myScrollBarHeight);
      // Preferred height is ignored for some reason, setting the the desired final height via minimum.
      setMinimumSize(new Dimension(1, height));

      setPreferredSize(new Dimension(getActualPreferredWidth(), height));
    }

    /**
     * Gets the actual preferred width, accounting for padding added to internal borders.
     */
    private int getActualPreferredWidth() {
      return (int)getPreferredSize().getWidth() + (2 * PAD);
    }

    /**
     * Gets the actual preferred height by calculating internal content heights and accounting for borders.
     *
     * HACK ALERT: EditorTextField does not return a reasonable preferred height and creating the editor without a file appears to leave
     * the internal editor instance null. As the internal editor would have been the best place to get the height, we fall back to
     * calculating the height of the contents by finding the line height and multiplying by the number of lines.
     */
    private int getActualPreferredHeight() {
      return (getFontMetrics(getFont()).getHeight() * getDocument().getLineCount()) + (2 * PAD);
    }

    /**
     * HACK ALERT: The editor is not set after this class is instantiated, being released after it's created. Any necessary overrides to
     * the scroll pane (which resides in the editor) must be made while the scroll pane exists... so the override is placed in this method
     * which is called each time the editor is created.
     *
     * TODO: Only do this on the final editor creation, but ensure that we've got the track height when setting the preffered height in
     * the constructor.
     */
    @Override
    protected EditorEx createEditor() {
      EditorEx editor = super.createEditor();

      JScrollPane scroll = editor.getScrollPane();

      // Escape early, should not occur when we're doing the final render.
      if (scroll == null) {
        return editor;
      }

      // Set margins on the code scroll pane.
      scroll.setViewportBorder(BorderFactory.createEmptyBorder(PAD, PAD, PAD, PAD));

      // Code typically overflows width, causing a horizontal scrollbar, and we need to account for the additional height so as to not
      // occlude the last line in the code sample. Value is used in the constructor so this method _must_ be triggered at least once prior
      // to setting minimum and preferred heights.
      myScrollBarHeight = scroll.getHorizontalScrollBar().getPreferredSize().height;

      // Set the scrollbars to show if the content overflows.
      // TODO(b/28357327): Why isn't this the default...
      scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

      // Due to some unidentified race condition in calculations, we default to being partially scrolled. Reset the scrollbars.
      JScrollBar verticalScrollBar = scroll.getVerticalScrollBar();
      JScrollBar horizontalScrollBar = scroll.getHorizontalScrollBar();
      verticalScrollBar.setValue(verticalScrollBar.getMinimum());
      horizontalScrollBar.setValue(horizontalScrollBar.getMinimum());

      return editor;
    }
  }
}
