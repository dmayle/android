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

package com.android.tools.sherpa.structure;

import com.android.tools.sherpa.drawing.ConnectionDraw;
import com.android.tools.sherpa.drawing.ViewTransform;
import com.android.tools.sherpa.drawing.decorator.WidgetDecorator;
import com.android.tools.sherpa.interaction.ConnectionCandidate;
import com.android.tools.sherpa.interaction.ResizeHandle;
import com.android.tools.sherpa.interaction.WidgetInteractionTargets;
import android.support.constraint.solver.widgets.*;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

/**
 * Represent a list of widgets and the associated operations
 */
public class WidgetsScene {

    private HashMap<Object, ConstraintWidget> mWidgets = new HashMap<>();
    private WidgetContainer mRoot;
    private Selection mSelection;

    /**
     * Clear the scene
     */
    public void clear() {
        mRoot = null;
        if (mSelection != null) {
            mSelection.clear();
        }
        mWidgets.clear();
    }

    /**
     * Clear all constraints
     */
    public void clearAllConstraints() {
        for (ConstraintWidget widget : getWidgets()) {
            widget.resetAllConstraints();
        }
        mSelection.clear();
        mSelection.setSelectedAnchor(null);
    }

    /**
     * Accessor to the list of widgets
     *
     * @return
     */
    public Collection<ConstraintWidget> getWidgets() {
        return mWidgets.values();
    }

    /**
     * Set the current list of widgets
     *
     * @param widgets
     */
    public void setWidgets(HashMap<Object, ConstraintWidget> widgets) {
        mWidgets = widgets;
        for (ConstraintWidget widget : mWidgets.values()) {
            if (widget.isRoot()) {
                mRoot = (WidgetContainer) widget;
            }
        }
    }

    /**
     * Create and insert a new group from a given list of widgets
     *
     * @param widgets list of widgets to put in the group
     */
    public void createGroupFromWidgets(ArrayList<ConstraintWidget> widgets) {
        ConstraintWidgetContainer container = new ConstraintWidgetContainer();
        container.setCompanionWidget(WidgetCompanion.create(container));
        createContainerFromWidgets(widgets, container, createContainerName("group"));
    }

    /**
     * Transform the selected table to a normal container
     */
    public void transformTableToContainer(ConstraintTableLayout table) {
        ConstraintWidgetContainer container = new ConstraintWidgetContainer();
        container.setDebugName(createContainerName("container"));
        transformContainerToContainer(table, container);
    }

    /**
     * Remove container and move its children to the same level
     *
     * @param container
     */
    public void removeContainer(ConstraintWidgetContainer container) {
        ConstraintWidgetContainer parent = (ConstraintWidgetContainer) container.getParent();
        if (parent == null) {
            return;
        }
        for (ConstraintWidget widget : mWidgets.values()) {
            widget.disconnectWidget(container);
        }
        ArrayList<ConstraintWidget> children =
          new ArrayList<>(container.getChildren());
        for (ConstraintWidget child : children) {
            parent.add(child);
            child.resetAnchors();
            child.setX(child.getX() + container.getX());
            child.setY(child.getY() + container.getY());
        }
        parent.remove(container);
        mWidgets.remove(getTag(container));
    }

    /**
     * Utility method to return the tag associated with the widget
     * It will return either the tag set in the companion, or the debugName
     *
     * @param widget the widget
     * @return the widget's tag
     */
    private static Object getTag(ConstraintWidget widget) {
        WidgetCompanion companion = (WidgetCompanion) widget.getCompanionWidget();
        Object tag = companion.getWidgetTag();
        if (tag != null) {
            return tag;
        }
        return widget.getDebugName();
    }

    /**
     * Remove a widget from the tree, breaking any connections to it
     *
     * @param widget the widget we are removing
     */
    public void removeWidget(ConstraintWidget widget) {
        if (widget == null) {
            return;
        }
        if (widget instanceof ConstraintWidgetContainer) {
            ConstraintWidgetContainer container = (ConstraintWidgetContainer) widget;
            ArrayList<ConstraintWidget> children = new ArrayList<>(container.getChildren());
            for (ConstraintWidget w : children) {
                removeWidget(w);
            }
        }
        for (ConstraintWidget w : mWidgets.values()) {
            w.disconnectWidget(widget);
        }
        WidgetContainer parent = (WidgetContainer) widget.getParent();
        if (parent != null) {
            parent.remove(widget);
        }
        mWidgets.remove(getTag(widget));
        if (mRoot == widget) {
            mRoot = null;
        }
    }

    /**
     * Flatten the hierachy -- remove all existing containers children of the given container
     *
     * @param root the root container we start from
     */
    public void flattenHierarchy(ConstraintWidgetContainer root) {
        ArrayList<ConstraintWidgetContainer> containers = gatherContainers(root);
        while (!containers.isEmpty()) {
            for (ConstraintWidgetContainer container : containers) {
                removeContainer(container);
            }
            containers = gatherContainers(root);
        }
    }

    /**
     * Find a widget at the coordinate (x, y) in the current selection,
     * taking the decorator visibility in account
     *
     * @param x x position
     * @param y y position
     * @return a widget if found, null otherwise
     */
    public ConstraintWidget findWidgetInSelection(float x, float y) {
        ConstraintWidget found = null;
        ArrayList<ConstraintWidget> selection = mSelection.getWidgets();
        for (ConstraintWidget widget : selection) {
            WidgetCompanion companion = (WidgetCompanion) widget.getCompanionWidget();
            WidgetDecorator decorator =
                    companion.getWidgetDecorator(WidgetDecorator.BLUEPRINT_STYLE);
            if (!decorator.isVisible()) {
                continue;
            }
            if (widget instanceof ConstraintWidgetContainer) {
                ConstraintWidget f = findWidget((ConstraintWidgetContainer) widget, x, y);
                if (f != null) {
                    found = f;
                }
            } else {
                int l = widget.getDrawX();
                int t = widget.getDrawY();
                int r = l + widget.getWidth();
                int b = t + widget.getHeight();
                if (x >= l && x <= r && y >= t && y <= b) {
                    found = widget;
                }
            }
        }
        return found;
    }

    /**
     * Find a widget at the coordinate (x, y), taking the decorator visibility in account
     *
     * @param x x position
     * @param y y position
     * @return a widget if found, null otherwise
     */
    public ConstraintWidget findWidget(ConstraintWidgetContainer container, float x, float y) {
        WidgetCompanion companion = (WidgetCompanion) container.getCompanionWidget();
        WidgetDecorator containerDecorator =
                companion.getWidgetDecorator(WidgetDecorator.BLUEPRINT_STYLE);
        if (!containerDecorator.isVisible()) {
            return null;
        }

        ConstraintWidget found = null;
        if (container == getRoot()) {
            // First, check the current selection
            found = findWidgetInSelection(x, y);
            if (found != null) {
                return found;
            }
        }
        int l = container.getDrawX();
        int t = container.getDrawY();
        int r = l + container.getWidth();
        int b = t + container.getHeight();
        if (x >= l && x <= r && y >= t && y <= b) {
            found = container;
        }
        for (ConstraintWidget widget : container.getChildren()) {
            WidgetCompanion widgetCompanion = (WidgetCompanion) widget.getCompanionWidget();
            WidgetDecorator widgetDecorator =
                    widgetCompanion.getWidgetDecorator(WidgetDecorator.BLUEPRINT_STYLE);
            if (!widgetDecorator.isVisible()) {
                continue;
            }
            if (widget instanceof ConstraintWidgetContainer) {
                ConstraintWidget f = findWidget((ConstraintWidgetContainer) widget, x, y);
                if (f != null) {
                    found = f;
                }
            } else {
                l = widget.getDrawX();
                t = widget.getDrawY();
                r = l + widget.getWidth();
                b = t + widget.getHeight();
                if (x >= l && x <= r && y >= t && y <= b) {
                    found = widget;
                }
            }
        }
        return found;
    }

    /**
     * Gather all the widgets contained in the area specified and return them as an array,
     * taking the decorator visibility in account
     *
     * @param x      x position of the selection area
     * @param y      y position of the selection area
     * @param width  width of the selection area
     * @param height height of the selection area
     * @return an array containing the widgets inside the selection area
     */
    public ArrayList<ConstraintWidget> findWidgets(WidgetContainer container,
            int x, int y, int width, int height) {
        ArrayList<ConstraintWidget> found = new ArrayList<>();
        Rectangle area = new Rectangle(x, y, width, height);
        for (ConstraintWidget widget : container.getChildren()) {
            WidgetCompanion companion = (WidgetCompanion) widget.getCompanionWidget();
            WidgetDecorator decorator =
                    companion.getWidgetDecorator(WidgetDecorator.BLUEPRINT_STYLE);
            if (!decorator.isVisible()) {
                continue;
            }
            Rectangle bounds = new Rectangle(widget.getDrawX(), widget.getDrawY(),
                    widget.getWidth(), widget.getHeight());
            if (area.intersects(bounds)) {
                found.add(widget);
            }
        }
        return found;
    }

    /**
     * Find which ResizeHandle is close to the (x, y) coordinates
     *
     * @param widget the widget we are checking
     * @param x      x coordinate
     * @param y      y coordinate
     * @return the ResizeHandle close to (x, y), or null if none are close enough
     */
    private static ResizeHandle findResizeHandleInWidget(ConstraintWidget widget,
            float x, float y, ViewTransform transform) {
        WidgetCompanion companion = (WidgetCompanion) widget.getCompanionWidget();
        WidgetDecorator decorator = companion.getWidgetDecorator(WidgetDecorator.BLUEPRINT_STYLE);
        if (!decorator.isVisible()) {
            return null;
        }
        WidgetInteractionTargets widgetInteraction = companion.getWidgetInteractionTargets();
        widgetInteraction.updatePosition(transform);
        ResizeHandle handle = widgetInteraction.findResizeHandle(x, y);
        if (handle != null) {
            return handle;
        }
        return null;
    }

    /**
     * Find which ResizeHandle is close to the (x, y) coordinates
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return the ResizeHandle close to (x, y), or null if none are close enough
     */
    public ResizeHandle findResizeHandle(float x, float y, ViewTransform transform) {
        for (Selection.Element element : mSelection.getElements()) {
            ConstraintWidget widget = element.widget;
            ResizeHandle handle = findResizeHandleInWidget(widget, x, y, transform);
            if (handle != null) {
                return handle;
            }
        }
        for (ConstraintWidget widget : mWidgets.values()) {
            if (widget.isRootContainer()) {
                continue;
            }
            ResizeHandle handle = findResizeHandleInWidget(widget, x, y, transform);
            if (handle != null) {
                return handle;
            }
        }
        return null;
    }

    /**
     * Find which ConstraintAnchor is close to the (x, y) coordinates in the current selection
     *
     * @param x               x coordinate
     * @param y               y coordinate
     * @param checkGuidelines if true, we will check for guidelines to connect to
     * @param mousePress      pass true on mouse press
     * @param viewTransform   the view transform
     * @return the ConstraintAnchor close to (x, y), or null if none are close enough
     */
    public ConstraintAnchor findAnchorInSelection(float x, float y, boolean checkGuidelines,
            boolean mousePress, ViewTransform viewTransform) {
        ConnectionCandidate candidate = new ConnectionCandidate();
        float dist =
                (ConnectionDraw.CONNECTION_ANCHOR_SIZE + ConnectionDraw.CONNECTION_ANCHOR_SIZE) /
                        viewTransform.getScale();
        candidate.distance = viewTransform.getSwingDimensionF(30);
        // We first try to find an anchor in the current selection
        for (Selection.Element element : mSelection.getElements()) {
            ConstraintWidget widget = element.widget;
            if (!checkGuidelines && (widget instanceof Guideline)) {
                continue;
            }
            WidgetCompanion companion = (WidgetCompanion) widget.getCompanionWidget();
            WidgetInteractionTargets widgetInteraction = companion.getWidgetInteractionTargets();
            widgetInteraction.updatePosition(viewTransform);
            widgetInteraction.findClosestConnection(viewTransform, x, y, candidate, mousePress);
        }

        float slope = (dist * dist);
        if (candidate.anchorTarget != null
                && candidate.distance < slope) {
            // allow some slope if we picked an anchor from the selection
            candidate.distance = 0;
        } else {
            candidate.anchorTarget = null;
        }
        return candidate.anchorTarget;
    }

    /**
     * Find which ConstraintAnchor is close to the (x, y) coordinates
     *
     * @param x               x coordinate
     * @param y               y coordinate
     * @param checkGuidelines if true, we will check for guidelines to connect to
     * @param mousePress      pass true on mouse press
     * @param viewTransform   the view transform
     * @return the ConstraintAnchor close to (x, y), or null if none are close enough
     */
    public ConstraintAnchor findAnchor(float x, float y, boolean checkGuidelines,
            boolean mousePress,
            ViewTransform viewTransform) {
        ConnectionCandidate candidate = new ConnectionCandidate();
        float dist =
                (ConnectionDraw.CONNECTION_ANCHOR_SIZE + ConnectionDraw.CONNECTION_ANCHOR_SIZE) /
                        viewTransform.getScale();
        candidate.distance =
                ConnectionDraw.CONNECTION_ANCHOR_SIZE * ConnectionDraw.CONNECTION_ANCHOR_SIZE;
        // We first try to find an anchor in the current selection
        for (Selection.Element element : mSelection.getElements()) {
            ConstraintWidget widget = element.widget;
            if (!checkGuidelines && (widget instanceof Guideline)) {
                continue;
            }
            WidgetCompanion companion = (WidgetCompanion) widget.getCompanionWidget();
            WidgetInteractionTargets widgetInteraction = companion.getWidgetInteractionTargets();
            widgetInteraction.updatePosition(viewTransform);
            widgetInteraction.findClosestConnection(viewTransform, x, y, candidate, mousePress);
        }

        float slope = (dist * dist);
        if (candidate.anchorTarget != null
                && candidate.distance < slope) {
            // allow some slope if we picked an anchor from the selection
            candidate.distance = 0;
        } else {
            candidate.anchorTarget = null;
        }
        for (ConstraintWidget widget : mWidgets.values()) {
            if (!checkGuidelines && (widget instanceof Guideline)) {
                continue;
            }
            WidgetCompanion companion = (WidgetCompanion) widget.getCompanionWidget();
            WidgetDecorator decorator =
                    companion.getWidgetDecorator(WidgetDecorator.BLUEPRINT_STYLE);
            if (!decorator.isVisible()) {
                continue;
            }
            WidgetInteractionTargets widgetInteraction = companion.getWidgetInteractionTargets();
            widgetInteraction.updatePosition(viewTransform);
            widgetInteraction.findClosestConnection(viewTransform, x, y, candidate, mousePress);
        }
        return candidate.anchorTarget;
    }

    /*-----------------------------------------------------------------------*/
    // Private functions
    /*-----------------------------------------------------------------------*/

    /**
     * Utility function returning a new unique name for a container
     *
     * @param name the prefix used
     * @return new container name
     */
    public String createContainerName(String name) {
        boolean valid = false;
        int counter = 1;
        while (!valid) {
            String candidate = name + counter;
            boolean exists = false;
            for (ConstraintWidget widget : mWidgets.values()) {
                if (widget.getDebugName().equalsIgnoreCase(candidate)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                valid = true;
                name = candidate;
            } else {
                counter++;
            }
        }
        return name;
    }

    /**
     * Gather a list of containers that are children of the given container
     *
     * @param container the container we start from
     * @return a list of containers
     */
    private static ArrayList<ConstraintWidgetContainer> gatherContainers(
            ConstraintWidgetContainer container) {
        ArrayList<ConstraintWidgetContainer> containers = new ArrayList<>();
        for (ConstraintWidget widget : container.getChildren()) {
            if (widget instanceof ConstraintWidgetContainer) {
                containers.add((ConstraintWidgetContainer) widget);
            }
        }
        return containers;
    }

    /**
     * Move the content of an old container to a new container
     *
     * @param oldContainer
     * @param newContainer
     */
    public void transformContainerToContainer(WidgetContainer oldContainer,
            ConstraintWidgetContainer newContainer) {
        WidgetContainer parent = (WidgetContainer) oldContainer.getParent();

        if (newContainer.getCompanionWidget() == null) {
            newContainer.setCompanionWidget(oldContainer.getCompanionWidget());
        }
        newContainer.setOrigin(oldContainer.getX(), oldContainer.getY());
        newContainer.setDimension(oldContainer.getWidth(), oldContainer.getHeight());
        newContainer
                .setHorizontalDimensionBehaviour(oldContainer.getHorizontalDimensionBehaviour());
        newContainer.setVerticalDimensionBehaviour(oldContainer.getVerticalDimensionBehaviour());
        ArrayList<ConstraintWidget> children =
          new ArrayList<>(oldContainer.getChildren());
        for (ConstraintWidget child : children) {
            newContainer.add(child);
        }
        for (ConstraintAnchor anchor : oldContainer.getAnchors()) {
            if (anchor.isConnected()) {
                newContainer.getAnchor(anchor.getType())
                        .connect(anchor.getTarget(), anchor.getMargin(),
                                anchor.getStrength(), anchor.getConnectionCreator());
            }
        }
        for (ConstraintWidget child : newContainer.getChildren()) {
            // make sure the child anchors are reset
            child.resetAnchors();
        }
        if (parent != null) {
            parent.remove(oldContainer);
            parent.add(newContainer);
        } else {
            removeWidget(oldContainer);
        }
        mWidgets.remove(getTag(oldContainer));
        setWidget(newContainer);
        if (mRoot != null) {
            mRoot.layout();
        }
    }

    /**
     * Insert a new ConstraintWidgetContainer in place, from
     * a list of widgets. The widgets will be cleared of their current
     * constraints and put as children of the new container.
     *
     * @param widgets           widgets we want to group into the container
     * @param containerInstance the container that will be the parent of the widget
     */
    public void createContainerFromWidgets(ArrayList<ConstraintWidget> widgets,
            ConstraintWidgetContainer containerInstance, String name) {
        Collections.sort(widgets, (o1, o2) -> {
            if (o1.getY() + o1.getHeight() < o2.getY()) {
                return -1;
            }
            if (o2.getY() + o2.getHeight() < o1.getY()) {
                return 1;
            }
            return Integer.compare(o1.getX(), o2.getX());
        });

        if (widgets.isEmpty()) {
            return;
        }
        for (ConstraintWidget w : mWidgets.values()) {
            for (ConstraintWidget widget : widgets) {
                w.disconnectWidget(widget);
                widget.resetAnchors();
                widget.setHorizontalBiasPercent(0.5f);
                widget.setVerticalBiasPercent(0.5f);
            }
        }
        WidgetContainer parent = (WidgetContainer) widgets.get(0).getParent();
        if (parent == null) {
            parent = mRoot;
        }
        ConstraintWidgetContainer container =
                ConstraintWidgetContainer.createContainer(containerInstance, name, widgets, 8);
        if (container != null) {
            if (container.getCompanionWidget() == null) {
                container.setCompanionWidget(WidgetCompanion.create(container));
            }
            parent.add(container);
            setWidget(container);
            mRoot.layout();
        }
    }

    /**
     * Adapt the table's dimensions and columns or rows to its content
     *
     * @param table
     */
    public static void adaptTable(ConstraintTableLayout table) {
        // We do that by first setting the table to wrap_content...
        int width = table.getWidth();
        int height = table.getHeight();
        ConstraintWidget.DimensionBehaviour horizontalBehaviour =
                table.getHorizontalDimensionBehaviour();
        ConstraintWidget.DimensionBehaviour verticalBehaviour =
                table.getVerticalDimensionBehaviour();
        table.setHorizontalDimensionBehaviour(
                ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        table.setVerticalDimensionBehaviour(
                ConstraintWidget.DimensionBehaviour.WRAP_CONTENT);
        table.layout();
        // FIXME, 2nd pass should not be necessary
        table.layout();
        // then getting the computed size, and use it as minimum size
        table.setMinWidth(table.getWidth());
        table.setMinHeight(table.getHeight());
        table.computeGuidelinesPercentPositions();
        // then put back the table to fixed size
        table.setHorizontalDimensionBehaviour(horizontalBehaviour);
        table.setVerticalDimensionBehaviour(verticalBehaviour);
        table.setWidth(width < table.getMinWidth() ? table.getMinWidth() : width);
        table.setHeight(height < table.getMinHeight() ? table.getMinHeight() : height);
        table.layout();
    }

    public ConstraintWidget getWidget(Object key) {
        return mWidgets.get(key);
    }

    public void setRoot(ConstraintWidgetContainer root) {
        mRoot = root;
    }

    public WidgetContainer getRoot() {
        if (mRoot == null) {
            for (ConstraintWidget widget : mWidgets.values()) {
                if (widget instanceof WidgetContainer) {
                    WidgetContainer lastRoot = (WidgetContainer) widget;
                    WidgetContainer root = lastRoot;
                    while (root.getParent() != null) {
                        root = (WidgetContainer) root.getParent();
                        if (root instanceof WidgetContainer) {
                            lastRoot = root;
                        }
                    }
                    mRoot = lastRoot;
                    break;
                }
            }
        }
        return mRoot;
    }

    /**
     * Set the widget in the scene
     *
     * @param widget widget to add to the scene
     */
    public void setWidget(ConstraintWidget widget) {
        mWidgets.put(getTag(widget), widget);
    }

    public void addWidget(ConstraintWidget widget) {
        if (widget instanceof ConstraintWidgetContainer && widget.getParent() == null) {
            mRoot = (WidgetContainer) widget;
        }
        setWidget(widget);
    }

    public void setSelection(Selection selection) {
        mSelection = selection;
    }

    public int size() {
        return mWidgets.size();
    }

    /**
     * Utility function to return the closest horizontal anchor
     *
     * @param widget     widget we start from
     * @param searchLeft if true, we are searching on our left side
     * @return the closest ConstraintAnchor
     */
    public ConstraintAnchor getClosestHorizontalWidgetAnchor(ConstraintWidget widget,
            boolean searchLeft) {
        ConstraintWidgetContainer parent = (ConstraintWidgetContainer) widget.getParent();
        ArrayList<ConstraintWidget> children = parent.getChildren();
        int pos = widget.getDrawX();
        if (!searchLeft) {
            pos = widget.getDrawRight();
        }
        int min = Integer.MAX_VALUE;
        ConstraintWidget found = null;
        for (ConstraintWidget child : children) {
            // check if it intersects
            int maxTop = Math.max(child.getDrawY(), widget.getDrawY());
            int minBottom = Math.min(child.getDrawBottom(), widget.getDrawBottom());
            if (maxTop > minBottom) {
                // we don't intersect
                continue;
            }
            int delta = pos - child.getDrawRight();
            if (!searchLeft) {
                delta = child.getDrawX() - pos;
            }
            if (delta >= 0 && delta < min) {
                found = child;
                min = delta;
            }
        }
        if (found == null) {
            if (searchLeft) {
                return parent.getAnchor(ConstraintAnchor.Type.LEFT);
            } else {
                return parent.getAnchor(ConstraintAnchor.Type.RIGHT);
            }
        }
        if (searchLeft) {
            return found.getAnchor(ConstraintAnchor.Type.RIGHT);
        } else {
            return found.getAnchor(ConstraintAnchor.Type.LEFT);
        }
    }

    /**
     * Utility function to return the closest vertical anchor
     *
     * @param widget    widget we start from
     * @param searchTop if true, we are searching above us
     * @return the closest ConstraintAnchor
     */
    public ConstraintAnchor getClosestVerticalWidgetAnchor(ConstraintWidget widget,
            boolean searchTop) {
        ConstraintWidgetContainer parent = (ConstraintWidgetContainer) widget.getParent();
        ArrayList<ConstraintWidget> children = parent.getChildren();
        int pos = widget.getDrawY();
        if (!searchTop) {
            pos = widget.getDrawBottom();
        }
        int min = Integer.MAX_VALUE;
        ConstraintWidget found = null;
        for (int i = 0; i < children.size(); i++) {
            ConstraintWidget child = children.get(i);
            // check if it intersects
            int maxLeft = Math.max(child.getDrawX(), widget.getDrawX());
            int minRight = Math.min(child.getDrawRight(), widget.getDrawRight());
            if (maxLeft > minRight) {
                // we don't intersect
                continue;
            }
            int delta = pos - child.getDrawBottom();
            if (!searchTop) {
                delta = child.getDrawY() - pos;
            }
            if (delta >= 0 && delta < min) {
                found = child;
                min = delta;
            }
        }
        if (found == null) {
            if (searchTop) {
                return parent.getAnchor(ConstraintAnchor.Type.TOP);
            } else {
                return parent.getAnchor(ConstraintAnchor.Type.BOTTOM);
            }
        }
        if (searchTop) {
            return found.getAnchor(ConstraintAnchor.Type.BOTTOM);
        } else {
            return found.getAnchor(ConstraintAnchor.Type.TOP);
        }
    }

    /**
     * center the given widget horizontally
     *
     * @param widget the widget to center
     */
    public void centerHorizontally(ConstraintWidget widget) {
        ConstraintAnchor left = getClosestHorizontalWidgetAnchor(widget, true);
        ConstraintAnchor right = getClosestHorizontalWidgetAnchor(widget, false);
        widget.connect(widget.getAnchor(ConstraintAnchor.Type.LEFT), left, 0);
        widget.connect(widget.getAnchor(ConstraintAnchor.Type.RIGHT), right, 0);
    }

    /**
     * center the given widget vertically
     *
     * @param widget the widget to center
     */
    public void centerVertically(ConstraintWidget widget) {
        ConstraintAnchor top = getClosestVerticalWidgetAnchor(widget, true);
        ConstraintAnchor bottom = getClosestVerticalWidgetAnchor(widget, false);
        widget.connect(widget.getAnchor(ConstraintAnchor.Type.TOP), top, 0);
        widget.connect(widget.getAnchor(ConstraintAnchor.Type.BOTTOM), bottom, 0);
    }

    /**
     * Make sure the positions of the interaction targets are correctly updated
     *
     * @param viewTransform the view transform
     */
    public void updatePositions(ViewTransform viewTransform) {
        for (ConstraintWidget widget : mWidgets.values()) {
            widget.updateDrawPosition();
            WidgetCompanion companion = (WidgetCompanion) widget.getCompanionWidget();
            WidgetInteractionTargets widgetInteraction = companion.getWidgetInteractionTargets();
            widgetInteraction.updatePosition(viewTransform);
        }
    }

    /**
     * For the given widget, return true if a majority of its constraints were created
     * by the user (and so in "lock" mode)
     *
     * @param widget the widget we are looking at
     * @return USER_CREATOR if the majority of the constraints hvae been created by the user,
     * AUTO_CONSTRAINT_CREATOR otherwise. If no constraints are set, returns -1
     */
    public int getMainConstraintsCreator(ConstraintWidget widget) {
        int numAuto = 0;
        int numUser = 0;
        for (ConstraintAnchor anchor : widget.getAnchors()) {
            if (anchor.isConnected()) {
                if (anchor.getConnectionCreator() == ConstraintAnchor.USER_CREATOR) {
                    numUser++;
                } else {
                    numAuto++;
                }
            }
        }
        if (numAuto == 0 && numUser == 0) {
            return -1;
        }
        if (numUser > numAuto) {
            return ConstraintAnchor.USER_CREATOR;
        }
        return ConstraintAnchor.AUTO_CONSTRAINT_CREATOR;
    }

    /**
     * Set the connected constraints creator to USER_CREATOR or AUTO_CONSTRAINT_CREATOR
     *
     * @param widget  the widget we operate on
     * @param creator the creator
     */
    public void setConstraintsCreator(ConstraintWidget widget, int creator) {
        for (ConstraintAnchor anchor : widget.getAnchors()) {
            if (anchor.isConnected()) {
                anchor.setConnectionCreator(creator);
            }
        }
    }

    /**
     * Toggle the constraints of the given widget
     *
     * @param widget the widget to toggle the constraints' creator status
     */
    public void toggleLockConstraints(ConstraintWidget widget) {
        int constraintsCreator = getMainConstraintsCreator(widget);
        if (constraintsCreator == ConstraintAnchor.USER_CREATOR) {
            setConstraintsCreator(widget, ConstraintAnchor.AUTO_CONSTRAINT_CREATOR);
        } else if (constraintsCreator == ConstraintAnchor.AUTO_CONSTRAINT_CREATOR) {
            setConstraintsCreator(widget, ConstraintAnchor.USER_CREATOR);
        }
        mSelection.selectionHasChanged();
    }
}
