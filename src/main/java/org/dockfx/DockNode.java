/**
 * @file DockNode.java
 * @brief Class implementing basic dock node with floating and styling.
 *
 * @section License
 *
 *          This file is a part of the DockFX Library. Copyright (C) 2015 Robert B. Colton
 *
 *          This program is free software: you can redistribute it and/or modify it under the terms
 *          of the GNU Lesser General Public License as published by the Free Software Foundation,
 *          either version 3 of the License, or (at your option) any later version.
 *
 *          This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *          WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *          PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 *          You should have received a copy of the GNU Lesser General Public License along with this
 *          program. If not, see <http://www.gnu.org/licenses/>.
 **/

package org.dockfx;

import java.util.List;

import org.dockfx.pane.ContentPane;
import org.dockfx.pane.ContentPane.Type;
import org.dockfx.pane.ContentSplitPane;
import org.dockfx.pane.ContentTabPane;
import org.dockfx.pane.DockNodeTab;
import org.dockfx.viewControllers.DockFXViewController;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

/**
 * Base class for a dock node that provides the layout of the content along with
 * a title bar and a styled border. The dock node can be detached and floated or
 * closed and removed from the layout. Dragging behavior is implemented through
 * the title bar.
 * 
 * @since DockFX 0.1
 */
public class DockNode extends VBox implements EventHandler<MouseEvent> {
	/**
	 * The style this dock node should use on its stage when set to floating.
	 */
	private StageStyle stageStyle = StageStyle.UNDECORATED;
	/**
	 * The stage that this dock node is currently using when floating.
	 */
	private Stage stage;

	/**
	 * The contents of the dock node, i.e. a TreeView or ListView.
	 */
	private Node contents;
	/**
	 * The title bar that implements our dragging and state manipulation.
	 */
	private DockTitleBar dockTitleBar;
	/**
	 * The border pane used when floating to provide a styled custom border.
	 */
	private BorderPane borderPane;

	/**
	 * The dock pane this dock node belongs to when not floating.
	 */
	private DockPane dockPane;

	/**
	 * View controller of node inside this DockNode
	 */
	private DockFXViewController viewController;

	/**
	 * Keep window state before its maximizing.
	 */
	private double widthBeforeMaximizing;
	private double heightBeforeMaximizing;
	private double xPosBeforeMaximizing;
	private double yPosBeforeMaximizing;

	/**
	 * CSS pseudo class selector representing whether this node is currently
	 * floating.
	 */
	private static final PseudoClass FLOATING_PSEUDO_CLASS = PseudoClass.getPseudoClass("floating");
	/**
	 * CSS pseudo class selector representing whether this node is currently
	 * docked.
	 */
	private static final PseudoClass DOCKED_PSEUDO_CLASS = PseudoClass.getPseudoClass("docked");
	/**
	 * CSS pseudo class selector representing whether this node is currently
	 * maximized.
	 */
	private static final PseudoClass MAXIMIZED_PSEUDO_CLASS = PseudoClass.getPseudoClass("maximized");

	/**
	 * Boolean property maintaining whether this node is currently maximized.
	 * 
	 * @defaultValue false
	 */
	private BooleanProperty maximizedProperty = new SimpleBooleanProperty(false) {

		@Override
		protected void invalidated() {
			DockNode.this.pseudoClassStateChanged(MAXIMIZED_PSEUDO_CLASS, get());
			if (borderPane != null) {
				borderPane.pseudoClassStateChanged(MAXIMIZED_PSEUDO_CLASS, get());
			}

			if (!get()) {
				stage.setX(xPosBeforeMaximizing);
				stage.setY(yPosBeforeMaximizing);
				stage.setWidth(widthBeforeMaximizing);
				stage.setHeight(heightBeforeMaximizing);
			} else {
				widthBeforeMaximizing = stage.getWidth();
				heightBeforeMaximizing = stage.getHeight();
				xPosBeforeMaximizing = stage.getX();
				yPosBeforeMaximizing = stage.getY();
			}

			// TODO: This is a work around to fill the screen bounds and not
			// overlap the task bar when
			// the window is undecorated as in Visual Studio. A similar work
			// around needs applied for
			// JFrame in Swing.
			// http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4737788
			// Bug report filed:
			// https://bugs.openjdk.java.net/browse/JDK-8133330
			if (this.get()) {
				Screen screen = Screen
						.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()).get(0);
				Rectangle2D bounds = screen.getVisualBounds();

				stage.setX(bounds.getMinX());
				stage.setY(bounds.getMinY());

				stage.setWidth(bounds.getWidth());
				stage.setHeight(bounds.getHeight());
			}
		}

		@Override
		public String getName() {
			return "maximized";
		}
	};

	public DockNode(Node contents, String title, Node graphic, DockFXViewController controller) {
		initializeDockNode(contents, title, graphic, controller);
	}

	/**
	 * Creates a default DockNode with a default title bar and layout.
	 * 
	 * @param contents
	 *            The contents of the dock node which may be a tree or another
	 *            scene graph node.
	 * @param title
	 *            The caption title of this dock node which maintains
	 *            bidirectional state with the title bar and stage.
	 * @param graphic
	 *            The caption graphic of this dock node which maintains
	 *            bidirectional state with the title bar and stage.
	 */
	public DockNode(Node contents, String title, Node graphic) {
		this(contents, title, graphic, null);
	}

	/**
	 * Creates a default DockNode with a default title bar and layout.
	 * 
	 * @param contents
	 *            The contents of the dock node which may be a tree or another
	 *            scene graph node.
	 * @param title
	 *            The caption title of this dock node which maintains
	 *            bidirectional state with the title bar and stage.
	 */
	public DockNode(Node contents, String title) {
		this(contents, title, null);
	}

	/**
	 * Creates a default DockNode with a default title bar and layout.
	 * 
	 * @param contents
	 *            The contents of the dock node which may be a tree or another
	 *            scene graph node.
	 */
	public DockNode(Node contents) {
		this(contents, null, null);
	}

	/**
	 *
	 * Creates a default DockNode with contents loaded from FXMLFile at provided
	 * path.
	 *
	 * @param FXMLPath
	 *            path to fxml file.
	 * @param title
	 *            The caption title of this dock node which maintains
	 *            bidirectional state with the title bar and stage.
	 * @param graphic
	 *            The caption title of this dock node which maintains
	 *            bidirectional state with the title bar and stage.
	 */
	public DockNode(String FXMLPath, String title, Node graphic) {
		FXMLLoader loader = loadNode(FXMLPath);
		initializeDockNode(loader.getRoot(), title, graphic, loader.getController());
	}

	/**
	 * Creates a default DockNode with contents loaded from FXMLFile at provided
	 * path.
	 *
	 * @param FXMLPath
	 *            path to fxml file.
	 * @param title
	 *            The caption title of this dock node which maintains
	 *            bidirectional state with the title bar and stage.
	 */
	public DockNode(String FXMLPath, String title) {
		this(FXMLPath, title, null);
	}

	/**
	 * Creates a default DockNode with contents loaded from FXMLFile at provided
	 * path with default title bar.
	 *
	 * @param FXMLPath
	 *            path to fxml file.
	 */
	public DockNode(String FXMLPath) {
		this(FXMLPath, null, null);
	}

	/**
	 * Loads Node from fxml file located at FXMLPath and returns it.
	 *
	 * @param FXMLPath
	 *            Path to fxml file.
	 * @return Node loaded from fxml file or StackPane with Label with error
	 *         message.
	 */
	private static FXMLLoader loadNode(String FXMLPath) {
		FXMLLoader loader = new FXMLLoader();
		try {
			loader.load(DockNode.class.getResourceAsStream(FXMLPath));
		} catch (Exception e) {
			e.printStackTrace();
			loader.setRoot(new StackPane(new Label("Could not load FXML file")));
		}
		return loader;
	}

	/**
	 * Sets DockNodes contents, title and title bar graphic
	 *
	 * @param contents
	 *            The contents of the dock node which may be a tree or another
	 *            scene graph node.
	 * @param title
	 *            The caption title of this dock node which maintains
	 *            bidirectional state with the title bar and stage.
	 * @param graphic
	 *            The caption title of this dock node which maintains
	 *            bidirectional state with the title bar and stage.
	 */
	private void initializeDockNode(Node contents, String title, Node graphic, DockFXViewController controller) {
		this.titleProperty.setValue(title);
		this.graphicProperty.setValue(graphic);
		this.contents = contents;
		this.viewController = controller;

		dockTitleBar = new DockTitleBar(this);
		if (viewController != null) {
			viewController.setDockTitleBar(dockTitleBar);
		}

		getChildren().addAll(dockTitleBar, contents);
		VBox.setVgrow(contents, Priority.ALWAYS);

		this.getStyleClass().add("dock-node");
	}

	ContentPane parent;

	void refreshLastPosition(ContentPane parent) {
		this.parent = parent;
		DockPos position;
		if (parent instanceof ContentSplitPane) {
			ContentSplitPane splitPane = (ContentSplitPane) parent;
			boolean isFirst = parent.getChildrenList().get(0) == this;

			if (splitPane.getOrientation() == Orientation.HORIZONTAL) {
				if (isFirst) {
					position = DockPos.LEFT;
				} else {
					position = DockPos.RIGHT;
				}
			} else {
				if (isFirst) {
					position = DockPos.TOP;
				} else {
					position = DockPos.BOTTOM;
				}
			}

		} else {
			position = DockPos.CENTER;
		}

		lastDockPos = position;
		if (parent.getChildrenList().size() == 0) {
			lastDockSibling = parent.getChildrenList().get(0);
		} else {
			lastDockSibling = parent.getChildrenList().stream().filter(n -> n != this).findFirst().orElse(this);
		}
	}

	/**
	 * The stage style that will be used when the dock node is floating. This
	 * must be set prior to setting the dock node to floating.
	 * 
	 * @param stageStyle
	 *            The stage style that will be used when the node is floating.
	 */
	public void setStageStyle(StageStyle stageStyle) {
		this.stageStyle = stageStyle;
	}

	/**
	 * Changes the contents of the dock node.
	 * 
	 * @param contents
	 *            The new contents of this dock node.
	 */
	public void setContents(Node contents) {
		this.getChildren().set(this.getChildren().indexOf(this.contents), contents);
		this.contents = contents;
	}

	/**
	 * Changes the title bar in the layout of this dock node. This can be used
	 * to remove the dock title bar from the dock node by passing null.
	 * 
	 * @param dockTitleBar
	 *            null The new title bar of this dock node, can be set null
	 *            indicating no title bar is used.
	 */
	public void setDockTitleBar(DockTitleBar dockTitleBar) {
		if (dockTitleBar != null) {
			if (this.dockTitleBar != null) {
				this.getChildren().set(this.getChildren().indexOf(this.dockTitleBar), dockTitleBar);
			} else {
				this.getChildren().add(0, dockTitleBar);
			}
		} else {
			this.getChildren().remove(this.dockTitleBar);
			setClosable(false);
			setFloatable(false);
		}

		this.dockTitleBar = dockTitleBar;
	}

	/**
	 * Whether the node is currently maximized.
	 * 
	 * @param maximized
	 *            Whether the node is currently maximized.
	 */
	public final void setMaximized(boolean maximized) {
		maximizedProperty.set(maximized);
	}

	/**
	 * Whether the node is currently floating.
	 * 
	 * @param floating
	 *            Whether the node is currently floating.
	 * @param translation
	 *            null The offset of the node after being set floating. Used for
	 *            aligning it with its layout bounds inside the dock pane when
	 *            it becomes detached. Can be null indicating no translation.
	 * @param newDockPane
	 *            the parent Dock Pane to associate with if not already set
	 */
	public void setFloating(boolean floating, Point2D translation, DockPane newDockPane) {
		if (floating && !this.isFloating()) {
			if (null == dockPane) {
				dockPane = newDockPane;
			}

			// position the new stage relative to the old scene offset
			Point2D floatScene = this.localToScene(0, 0);
			Point2D floatScreen = this.localToScreen(0, 0);

			// setup window stage
			dockTitleBar.setVisible(this.isCustomTitleBar());
			dockTitleBar.setManaged(this.isCustomTitleBar());

			if (this.isDocked()) {
				this.undock();
			}

			stage = new Stage();
			stage.setAlwaysOnTop(dockTitleBar.isAlwaysOnTop());

			stage.titleProperty().bind(titleProperty);

			stage.initStyle(stageStyle);

			// offset the new stage to cover exactly the area the dock was local
			// to the scene
			// this is useful for when the user presses the + sign and we have
			// no information
			// on where the mouse was clicked
			Point2D stagePosition;
			boolean translateToCenter = false;
			if (this.isDecorated() && stage.getOwner() != null) {
				Window owner = stage.getOwner();
				stagePosition = floatScene.add(new Point2D(owner.getX(), owner.getY()));
			} else if (floatScreen != null) {
				// using coordinates the component was previously in (if
				// available)
				stagePosition = floatScreen;
			} else {
				translateToCenter = true;

				if (null != dockPane) {
					Window rootWindow = dockPane.getScene().getWindow();
					double centerX = rootWindow.getX() + (rootWindow.getWidth() / 2);
					double centerY = rootWindow.getY() + (rootWindow.getHeight() / 2);
					stagePosition = new Point2D(centerX, centerY);
				} else {
					// using the center of the screen if no relative position is
					// available
					Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds();
					double centerX = (primScreenBounds.getWidth() - Math.max(getWidth(), getMinWidth())) / 2;
					double centerY = (primScreenBounds.getHeight() - Math.max(getHeight(), getMinHeight())) / 2;
					stagePosition = new Point2D(centerX, centerY);
				}
			}

			if (translation != null) {
				stagePosition = stagePosition.add(translation);
			}

			// the border pane allows the dock node to
			// have a drop shadow effect on the border
			// but also maintain the layout of contents
			// such as a tab that has no content
			borderPane = new BorderPane();
			borderPane.getStyleClass().add("dock-node-border");
			borderPane.setCenter(this);

			Scene scene = new Scene(borderPane);

			// apply the floating property so we can get its padding size
			// while it is floating to offset it by the drop shadow
			// this way it pops out above exactly where it was when docked
			this.floatingProperty.set(floating);
			// this.setMinimizable(floating);
			this.applyCss();

			// apply the border pane css so that we can get the insets and
			// position the stage properly
			borderPane.applyCss();
			Insets insetsDelta = borderPane.getInsets();

			double insetsWidth = insetsDelta.getLeft() + insetsDelta.getRight();
			double insetsHeight = insetsDelta.getTop() + insetsDelta.getBottom();

			stage.setScene(scene);

			stage.setMinWidth(borderPane.minWidth(this.getMinWidth()) + insetsWidth);
			stage.setMinHeight(borderPane.minHeight(this.getMinHeight()) + insetsHeight);
			borderPane.setPrefSize(this.getWidth() + insetsWidth, this.getHeight() + insetsHeight);

			if (translateToCenter) {
				// we are floating over the center of some parent, therefore
				// align our center with theirs
				stage.setX(stagePosition.getX() - insetsDelta.getLeft() - (borderPane.getPrefWidth() / 2.0));
				stage.setY(stagePosition.getY() - insetsDelta.getTop() - (borderPane.getPrefHeight() / 2.0));
			} else {
				stage.setX(stagePosition.getX() - insetsDelta.getLeft());
				stage.setY(stagePosition.getY() - insetsDelta.getTop());
			}

			if (stageStyle == StageStyle.TRANSPARENT) {
				scene.setFill(null);
			}

			stage.setResizable(this.isStageResizable());
			if (this.isStageResizable()) {
				stage.addEventFilter(MouseEvent.MOUSE_PRESSED, this);
				stage.addEventFilter(MouseEvent.MOUSE_MOVED, this);
				stage.addEventFilter(MouseEvent.MOUSE_DRAGGED, this);
			}

			// we want to set the client area size
			// without this it subtracts the native border sizes from the scene
			// size
			stage.sizeToScene();

			stage.show();
			stage.setOnCloseRequest(r -> close());
		} else if (!floating && this.isFloating()) {
			this.floatingProperty.set(floating);
			// this.setMinimizable(floating);

			stage.removeEventFilter(MouseEvent.MOUSE_PRESSED, this);
			stage.removeEventFilter(MouseEvent.MOUSE_MOVED, this);
			stage.removeEventFilter(MouseEvent.MOUSE_DRAGGED, this);

			stage.close();
		}
	}

	/**
	 * Whether the node is currently floating.
	 * 
	 * @param floating
	 *            Whether the node is currently floating.
	 * @return
	 */
	boolean setFloated;

	public DockNode setFloating(boolean floating) {
		setFloating(floating, null, dockPane);
		if (!setFloated) {
			setFloated = true;
			stage.setWidth(floatingWidth);
			stage.setHeight(floatingHeight);
		}
		return this;
	}

	/**
	 * The dock pane that was last associated with this dock node. Either the
	 * dock pane that it is currently docked to or the one it was detached from.
	 * Can be null if the node was never docked.
	 * 
	 * @return The dock pane that was last associated with this dock node.
	 */
	public final DockPane getDockPane() {
		return dockPane;
	}

	/**
	 * ViewController associated with this dock nodes contents, might be null
	 *
	 * @return ViewController associated with this dock nodes contents
	 */
	public final DockFXViewController getViewController() {
		return viewController;
	}

	/**
	 * The dock title bar associated with this dock node.
	 * 
	 * @return The dock title bar associated with this node.
	 */
	public final DockTitleBar getDockTitleBar() {
		return this.dockTitleBar;
	}

	/**
	 * The stage associated with this dock node. Can be null if the dock node
	 * was never set to floating.
	 * 
	 * @return The stage associated with this node.
	 */
	public final Stage getStage() {
		return stage;
	}

	/**
	 * The border pane used to parent this dock node when floating. Can be null
	 * if the dock node was never set to floating.
	 * 
	 * @return The stage associated with this node.
	 */
	public final BorderPane getBorderPane() {
		return borderPane;
	}

	/**
	 * The contents managed by this dock node.
	 * 
	 * @return The contents managed by this dock node.
	 */
	public final Node getContents() {
		return contents;
	}

	/**
	 * Object property maintaining bidirectional state of the caption graphic
	 * for this node with the dock title bar or stage.
	 * 
	 * @defaultValue null
	 */
	public final ObjectProperty<Node> graphicProperty() {
		return graphicProperty;
	}

	private ObjectProperty<Node> graphicProperty = new SimpleObjectProperty<Node>() {
		@Override
		public String getName() {
			return "graphic";
		}
	};

	public final Node getGraphic() {
		return graphicProperty.get();
	}

	public final DockNode setGraphic(Node graphic) {
		this.graphicProperty.setValue(graphic);
		return this;
	}

	/**
	 * Boolean property maintaining bidirectional state of the caption title for
	 * this node with the dock title bar or stage.
	 * 
	 * @defaultValue "Dock"
	 */
	public final StringProperty titleProperty() {
		return titleProperty;
	}

	private StringProperty titleProperty = new SimpleStringProperty("Dock") {
		@Override
		public String getName() {
			return "title";
		}
	};

	public final String getTitle() {
		return titleProperty.get();
	}

	public final DockNode setTitle(String title) {
		this.titleProperty.setValue(title);
		return this;
	}

	/**
	 * Boolean property maintaining bidirectional state of the name stored in
	 * the setting files
	 * 
	 * @defaultValue "Dock"
	 */
	private StringProperty settingNameProperty = new SimpleStringProperty("Dock");

	public final StringProperty settingNameProperty() {
		return settingNameProperty;
	}

	public final String getSettingName() {
		return settingNameProperty.get();
	}

	public final DockNode setSettingName(String settingName) {
		this.settingNameProperty.setValue(settingName);
		return this;
	}

	/**
	 * Boolean property maintaining whether this node is currently using a
	 * custom title bar. This can be used to force the default title bar to show
	 * when the dock node is set to floating instead of using native window
	 * borders.
	 * 
	 * @defaultValue true
	 */
	public final BooleanProperty customTitleBarProperty() {
		return customTitleBarProperty;
	}

	private BooleanProperty customTitleBarProperty = new SimpleBooleanProperty(true) {
		@Override
		public String getName() {
			return "customTitleBar";
		}
	};

	public final boolean isCustomTitleBar() {
		return customTitleBarProperty.get();
	}

	public final void setUseCustomTitleBar(boolean useCustomTitleBar) {
		if (this.isFloating()) {
			dockTitleBar.setVisible(useCustomTitleBar);
			dockTitleBar.setManaged(useCustomTitleBar);
		}
		this.customTitleBarProperty.set(useCustomTitleBar);
	}

	/**
	 * Boolean property maintaining whether this node is currently floating.
	 * 
	 * @defaultValue false
	 */
	public final BooleanProperty floatingProperty() {
		return floatingProperty;
	}

	private BooleanProperty floatingProperty = new SimpleBooleanProperty(false) {
		@Override
		protected void invalidated() {
			DockNode.this.pseudoClassStateChanged(FLOATING_PSEUDO_CLASS, get());
			if (borderPane != null) {
				borderPane.pseudoClassStateChanged(FLOATING_PSEUDO_CLASS, get());
			}
		}

		@Override
		public String getName() {
			return "floating";
		}
	};

	public final boolean isFloating() {
		return floatingProperty.get();
	}

	/**
	 * Boolean property maintaining whether this node is currently floatable.
	 * 
	 * @defaultValue true
	 */
	public final BooleanProperty floatableProperty() {
		return floatableProperty;
	}

	private BooleanProperty floatableProperty = new SimpleBooleanProperty(true) {
		@Override
		public String getName() {
			return "floatable";
		}
	};

	public final boolean isFloatable() {
		return floatableProperty.get();
	}

	public final void setFloatable(boolean floatable) {
		if (!floatable && this.isFloating()) {
			this.setFloating(false);
		}
		this.floatableProperty.set(floatable);
	}

	/**
	 * Boolean property maintaining whether this node is currently closable.
	 * 
	 * @defaultValue true
	 */
	public final BooleanProperty closableProperty() {
		return closableProperty;
	}

	private BooleanProperty closableProperty = new SimpleBooleanProperty(true) {
		@Override
		public String getName() {
			return "closable";
		}
	};

	public final boolean isClosable() {
		return closableProperty.get();
	}

	public final void setClosable(boolean closable) {
		this.closableProperty.set(closable);
	}

	private BooleanProperty minimizedProperty = new SimpleBooleanProperty(false) {
		@Override
		public String getName() {
			return "minimized";
		}
	};

	public final boolean isMinimized() {
		return minimizedProperty.get();
	}

	public final void setMinimized(boolean minimized) {
		if (null != stage) {
			stage.setIconified(minimized);
			this.minimizedProperty.set(minimized);
		}
	}

	/**
	 * Boolean property maintaining whether this node is minimizable.
	 * 
	 * @return
	 */
	public final BooleanProperty minimizableProperty() {
		return minimizableProperty;
	}

	private BooleanProperty minimizableProperty = new SimpleBooleanProperty(true) {
		@Override
		public String getName() {
			return "minimizable";
		}
	};

	public final boolean isMinimizable() {
		return minimizableProperty.get();
	}

	public final void setMinimizable(boolean minimizable) {
		this.minimizableProperty.set(minimizable);
	}

	/**
	 * Boolean property maintaining whether this node is currently resizable.
	 * 
	 * @defaultValue true
	 */
	public final BooleanProperty resizableProperty() {
		return stageResizableProperty;
	}

	private BooleanProperty stageResizableProperty = new SimpleBooleanProperty(true) {
		@Override
		public String getName() {
			return "resizable";
		}
	};

	public final boolean isStageResizable() {
		return stageResizableProperty.get();
	}

	public final void setStageResizable(boolean resizable) {
		stageResizableProperty.set(resizable);
	}

	/**
	 * Boolean property maintaining whether this node is currently docked. This
	 * is used by the dock pane to inform the dock node whether it is currently
	 * docked.
	 * 
	 * @defaultValue false
	 */
	public final BooleanProperty dockedProperty() {
		return dockedProperty;
	}

	private BooleanProperty dockedProperty = new SimpleBooleanProperty(false) {
		@Override
		protected void invalidated() {
			if (get()) {
				if (dockTitleBar != null) {
					dockTitleBar.setVisible(true);
					dockTitleBar.setManaged(true);
				}
			}

			DockNode.this.pseudoClassStateChanged(DOCKED_PSEUDO_CLASS, get());
		}

		@Override
		public String getName() {
			return "docked";
		}
	};

	public final boolean isDocked() {
		return dockedProperty.get();
	}

	public final BooleanProperty maximizedProperty() {
		return maximizedProperty;
	}

	public final boolean isMaximized() {
		return maximizedProperty.get();
	}

	public final boolean isDecorated() {
		return stageStyle != StageStyle.TRANSPARENT && stageStyle != StageStyle.UNDECORATED;
	}

	/**
	 * Boolean property maintaining whether this node is currently tabbed.
	 *
	 * @defaultValue false
	 */
	public final BooleanProperty tabbedProperty() {
		return tabbedProperty;
	}

	private BooleanProperty tabbedProperty = new SimpleBooleanProperty(false) {
		@Override
		protected void invalidated() {

			if (getChildren() != null) {
				if (get()) {
					getChildren().remove(dockTitleBar);
				} else {
					getChildren().add(0, dockTitleBar);
				}
			}
		}

		@Override
		public String getName() {
			return "tabbed";
		}
	};

	public final boolean isTabbed() {
		return tabbedProperty.get();
	}

	/**
	 * Boolean property maintaining whether this node is currently closed.
	 */
	public final BooleanProperty closedProperty() {
		return closedProperty;
	}

	private BooleanProperty closedProperty = new SimpleBooleanProperty(false) {
		@Override
		protected void invalidated() {
		}

		@Override
		public String getName() {
			return "closed";
		}
	};

	public final boolean isClosed() {
		return closedProperty.get();
	}

	private DockPos lastDockPos;

	public DockPos getLastDockPos() {
		return lastDockPos;
	}

	private Node lastDockSibling;

	public Node getLastDockSibling() {
		return lastDockSibling;
	}

	private double floatingWidth = 500.0;
	private double floatingHeight = 500.0;

	public DockNode setFloatingWidth(double width) {
		this.floatingWidth = width;
		if (stage != null) {
			stage.setWidth(width);
		}
		return this;
	}

	public DockNode setFloatingHeight(double height) {
		this.floatingHeight = height;
		if (stage != null) {
			stage.setHeight(height);
		}
		return this;
	}

	public DockNode setFloatingSize(double width, double height) {
		if (stage != null) {
			stage.setWidth(width);
			stage.setHeight(height);
		}
		return this;
	}

	/**
	 * Dock this node into a dock pane.
	 * 
	 * @param dockPane
	 *            The dock pane to dock this node into.
	 * @param dockPos
	 *            The docking position relative to the sibling of the dock pane.
	 * @param sibling
	 *            The sibling node to dock this node relative to.
	 * @return
	 */
	public DockNode dock(DockPane dockPane, DockPos dockPos, Node sibling) {
		dockImpl(dockPane);
		dockPane.dock(this, dockPos, sibling);
		this.lastDockPos = dockPos;
		this.lastDockSibling = sibling;
		return this;
	}

	/**
	 * Dock this node into a dock pane.
	 * 
	 * @param dockPane
	 *            The dock pane to dock this node into.
	 * @param dockPos
	 *            The docking position relative to the sibling of the dock pane.
	 * @return
	 */
	public DockNode dock(DockPane dockPane, DockPos dockPos) {
		dockImpl(dockPane);
		dockPane.dock(this, dockPos);
		this.lastDockPos = dockPos;
		return this;
	}

	public DockNode dock(DockPane dockPane) {
		Node sibling;
		DockPos position;
		Node firstChild = getFirstChild();
		if (firstChild != null) {
			sibling = firstChild;
			position = DockPos.CENTER;
		} else {
			sibling = dockPane.getRoot();
			position = DockPos.RIGHT;
		}
		return dock(dockPane, position, sibling);
	}

	private final void dockImpl(DockPane dockPane) {
		if (isFloating()) {
			setFloating(false);
		}
		this.dockPane = dockPane;
		this.dockedProperty.set(true);
		this.closedProperty.set(false);
	}

	protected void setDockPane(DockPane pane) {
		dockPane = pane;
		this.dockedProperty.set(true);
	}

	/**
	 * Detach this node from its previous dock pane if it was previously docked.
	 */
	public void undock() {
		if (dockPane != null) {
			dockPane.undock(this);
		}
		this.dockedProperty.set(false);
		this.tabbedProperty.set(false);
	}

	/**
	 * Close this dock node by setting it to not floating and making sure it is
	 * detached from any dock pane.
	 */
	public void close() {
		if (isFloating()) {
			setFloating(false);
		} else if (isDocked()) {
			undock();
		}
		this.closedProperty.set(true);
		if (isRemoveOnClose()) {
			dockPane.remove(this);
		}
	}

	public DockNodeTab getNodeTab() {
		return dockNodeTab;
	}

	private DockNodeTab dockNodeTab;

	public void setNodeTab(DockNodeTab nodeTab) {
		this.dockNodeTab = nodeTab;
	}

	public void focus() {
		if (tabbedProperty().get())
			dockNodeTab.select();
	}

	/**
	 * The last position of the mouse that was within the minimum layout bounds.
	 */
	private Point2D sizeLast;
	/**
	 * Whether we are currently resizing in a given direction.
	 */
	private boolean sizeWest = false, sizeEast = false, sizeNorth = false, sizeSouth = false;

	/**
	 * Gets whether the mouse is currently in this dock node's resize zone.
	 * 
	 * @return Whether the mouse is currently in this dock node's resize zone.
	 */
	public boolean isMouseResizeZone() {
		return sizeWest || sizeEast || sizeNorth || sizeSouth;
	}

	@Override
	public void handle(MouseEvent event) {
		Cursor cursor = Cursor.DEFAULT;

		// TODO: use escape to cancel resize/drag operation like visual studio
		if (!this.isFloating() || !this.isStageResizable()) {
			return;
		}

		if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
			sizeLast = new Point2D(event.getScreenX(), event.getScreenY());
		} else if (event.getEventType() == MouseEvent.MOUSE_MOVED) {
			Insets insets = borderPane.getPadding();
			int tolerance = 6;

			sizeWest = event.getX() < insets.getLeft() + tolerance;
			sizeEast = event.getX() > borderPane.getWidth() - insets.getRight() - tolerance;
			sizeNorth = event.getY() < insets.getTop() + tolerance;
			sizeSouth = event.getY() > borderPane.getHeight() - insets.getBottom() - tolerance;

			if (sizeWest) {
				if (sizeNorth) {
					cursor = Cursor.NW_RESIZE;
				} else if (sizeSouth) {
					cursor = Cursor.SW_RESIZE;
				} else {
					cursor = Cursor.W_RESIZE;
				}
			} else if (sizeEast) {
				if (sizeNorth) {
					cursor = Cursor.NE_RESIZE;
				} else if (sizeSouth) {
					cursor = Cursor.SE_RESIZE;
				} else {
					cursor = Cursor.E_RESIZE;
				}
			} else if (sizeNorth) {
				cursor = Cursor.N_RESIZE;
			} else if (sizeSouth) {
				cursor = Cursor.S_RESIZE;
			}

			this.getScene().setCursor(cursor);
		} else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED && this.isMouseResizeZone()) {
			Point2D sizeCurrent = new Point2D(event.getScreenX(), event.getScreenY());
			Point2D sizeDelta = sizeCurrent.subtract(sizeLast);

			double newX = stage.getX(), newY = stage.getY(), newWidth = stage.getWidth(), newHeight = stage.getHeight();

			if (sizeNorth) {
				newHeight -= sizeDelta.getY();
				newY += sizeDelta.getY();
			} else if (sizeSouth) {
				newHeight += sizeDelta.getY();
			}

			if (sizeWest) {
				newWidth -= sizeDelta.getX();
				newX += sizeDelta.getX();
			} else if (sizeEast) {
				newWidth += sizeDelta.getX();
			}

			// TODO: find a way to do this synchronously and eliminate the
			// flickering of moving the stage
			// around, also file a bug report for this feature if a work around
			// can not be found this
			// primarily occurs when dragging north/west but it also appears in
			// native windows and Visual
			// Studio, so not that big of a concern.
			// Bug report filed:
			// https://bugs.openjdk.java.net/browse/JDK-8133332
			double currentX = sizeLast.getX(), currentY = sizeLast.getY();
			if (newWidth >= stage.getMinWidth()) {
				stage.setX(newX);
				stage.setWidth(newWidth);
				currentX = sizeCurrent.getX();
			}

			if (newHeight >= stage.getMinHeight()) {
				stage.setY(newY);
				stage.setHeight(newHeight);
				currentY = sizeCurrent.getY();
			}
			sizeLast = new Point2D(currentX, currentY);
			// we do not want the title bar getting these events
			// while we are actively resizing
			if (sizeNorth || sizeSouth || sizeWest || sizeEast) {
				event.consume();
			}
		}
	}

	public DockNode dockBack() {

		setMaximized(false);
		DockPos position = lastDockPos;
		Node sibling = lastDockSibling;

		if (sibling == null || (sibling instanceof DockNode
				&& (((DockNode) sibling).isFloating() || ((DockNode) sibling).isClosed()))) {
			Node firstChild = getFirstChild();
			if (firstChild != null) {
				sibling = firstChild;
				position = DockPos.CENTER;
			} else {
				sibling = dockPane.getRoot();
				position = DockPos.RIGHT;
			}
		}

		if (sibling != null) {
			dock(dockPane, position, sibling);
		} else {
			dock(dockPane, DockPos.RIGHT);
		}

		if (sibling == lastDockSibling && parent != null && parent.getType() == Type.SplitPane) {
			ContentSplitPane splitPane = (ContentSplitPane) parent;
			splitPane.resetDividerPositions();
		}

		return this;
	}

	private Node getFirstChild() {
		Node root = dockPane.getRoot();

		if (root instanceof ContentSplitPane) {
			List<Node> children = ((ContentSplitPane) root).getChildrenList();
			if (children.size() == 1) {
				Node child = children.get(0);
				if (child instanceof ContentTabPane) {
					return ((ContentTabPane) child).getChildrenList().get(0);
				} else if (child instanceof DockNode) {
					return child;
				}
			}
		}
		return null;
	}

	public DockNode addOnCloseHandler(Runnable onClose) {
		if (onClose != null) {
			this.closedProperty.addListener((o, ov, nv) -> {
				if (nv) {
					onClose.run();
				}
			});
		}
		return this;
	}

	public DockNode replaceWith(DockNode mainNode) {
		if (!isClosed()) {
			if (isFloating()) {
				mainNode.setFloating(true);
				mainNode.setFloatingWidth(this.stage.getWidth());
				mainNode.setFloatingHeight(this.stage.getHeight());
				mainNode.stage.setX(this.stage.getX());
				mainNode.stage.setY(this.stage.getY());
			} else {
				mainNode.dock(dockPane, DockPos.CENTER, this);
			}
			this.close();
			mainNode.closedProperty().setValue(false);
			((Stage) dockPane.getScene().getWindow()).toFront();
		}
		return this;
	}

	public DockNode addMenuItem(MenuItem... menuItems) {
		dockTitleBar.addMenuItem(menuItems);
		return this;
	}

	public DockNode addMenuItem(String title, EventHandler<ActionEvent> event) {
		MenuItem menuItem = new MenuItem(title);
		menuItem.setOnAction(event);
		return addMenuItem(menuItem);
	}

	private BooleanProperty ignoreStoreProperty = new SimpleBooleanProperty(false);

	public BooleanProperty ignoreStoreProperty() {
		return ignoreStoreProperty;
	}

	public boolean isIgnoreStore() {
		return ignoreStoreProperty.get();
	}

	public DockNode setIgnoreStore(boolean ignoreStore) {
		ignoreStoreProperty.set(ignoreStore);
		return this;
	}

	private BooleanProperty removeOnCloseProperty = new SimpleBooleanProperty(false);

	public BooleanProperty removeOnCloseProperty() {
		return removeOnCloseProperty;
	}

	public boolean isRemoveOnClose() {
		return removeOnCloseProperty.get();
	}

	public DockNode setRemoveOnClose(boolean removeOnClose) {
		removeOnCloseProperty.set(removeOnClose);
		return this;
	}

	private boolean titleRenamed;

	public boolean isTitleRenamed() {
		return titleRenamed;
	}

	public DockNode setOnRenameAction(EventHandler<ActionEvent> renameAE) {
		this.dockTitleBar.setOnRenameAction(renameAE);
		return this;
	}

	public void rename() {
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Zmiana nazwy panel");
		dialog.setHeaderText("Podaj nową nazwę");
		dialog.setContentText("Nazwa");
		dialog.showAndWait().ifPresent(e -> {
			if (e.length() > 0) {
				rename(e);
			}
		});
	}

	public void rename(String title) {
		setTitle(title);
		titleRenamed = true;
	}

	public DockNode setAlwaysOnTop(boolean alwaysOnTop) {
		dockTitleBar.setAlwaysOnTop(alwaysOnTop);
		return this;
	}

	public DockNode setBackButtonToottip(String tooltip) {
		dockTitleBar.setBackButtonToottip(tooltip);
		return this;
	}

	public DockNode setListButtonToottip(String tooltip) {
		dockTitleBar.setListButtonToottip(tooltip);
		return this;
	}

	public DockNode setRenameButtonToottip(String tooltip) {
		dockTitleBar.setRenameButtonToottip(tooltip);
		return this;
	}

	public DockNode setPinButtonTooltip(String tooltip) {
		dockTitleBar.setPinButtonTooltip(tooltip);
		return this;
	}
}
