import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.awt.Point;
import java.util.LinkedList;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;
import static org.lwjgl.opengl.GL11.*;

/**
 * class DisplayLevel
 * <p>
 * The window data and layout for the window of the active game, with one level displayed.
 *
 * @author Stanley S.
 * @version 1.9
 */
public class DisplayLevel extends DisplayableWindow
{
  /**
   * The currently displayed level.
   */
  private Level lvl;
  /**
   * The level before the current drag.
   * <p>
   * Used for displaying the background haze of each path.
   */
  private Level old;
  /**
   * The layer that is displayed on the main display. (The z-axis, 0 being the top.)
   */
  private int layer;
  /**
   * The path of points of which the user has dragged.
   */
  private LinkedList<Point3I> dragPath;

  /**
   * The fade-in effect when opening the level for the first time, and fade-out effect upon winning the level.
   */
  private DisplayTransitionHelper fade = new DisplayTransitionHelper(0, 60, 60, -60, InterpolationType.SINUSOID);

  // The sizes and locations of all the things that are going to be displayed.
  protected static final int displayLocations_WindowWidth = 800;
  protected static final int displayLocations_WindowHeight = 600;
  protected static final int displayLocations_WindowCenterX = displayLocations_WindowWidth / 2;
  protected static final int displayLocations_WindowCenterY = displayLocations_WindowHeight / 2;

  protected static final int displayLocations_MainWindowWidth = 550;
  protected static final int displayLocations_MainWindowCenterX = displayLocations_WindowWidth - (displayLocations_MainWindowWidth / 2);
  protected static final int displayLocations_MainWindowCenterY = displayLocations_WindowHeight / 2;

  protected static final int displayLocations_GameBufferRight = 50;
  protected static final int displayLocations_GameSize = 400;
  protected static final int displayLocations_GameBufferTop = 50;
  protected static final int displayLocations_GameCenterX = displayLocations_WindowWidth - displayLocations_GameBufferRight - (displayLocations_GameSize / 2);
  protected static final int displayLocations_GameCenterY = displayLocations_GameBufferTop + (displayLocations_GameSize / 2);
  protected static final int displayLocations_GameLeft = displayLocations_GameCenterX - (displayLocations_GameSize / 2);
  protected static final int displayLocations_GameRight = displayLocations_WindowWidth - displayLocations_GameBufferRight;
  protected static final int displayLocations_GameTop = displayLocations_GameBufferTop;
  protected static final int displayLocations_GameBottom = displayLocations_GameTop + displayLocations_GameSize;

  protected static final int displayLocations_LeftBarWidth = displayLocations_WindowWidth - displayLocations_MainWindowWidth;
  protected static final int displayLocations_LeftBarCenterX = displayLocations_LeftBarWidth / 2;
  protected static final int displayLocations_LeftBarCenterY = displayLocations_WindowHeight / 2;

  protected static final int displayLocations_LeftBar_LevelsBufferLeft = 65;
  protected static final int displayLocations_LeftBar_LevelsSize = 120;
  protected static final int displayLocations_LeftBar_LevelsBuffer = 30;
  protected static final int displayLocations_LeftBar_LevelsBufferedSize = displayLocations_LeftBar_LevelsSize + displayLocations_LeftBar_LevelsBuffer;
  protected static final int displayLocations_LeftBar_LevelsCenterX = displayLocations_LeftBar_LevelsBufferLeft + (displayLocations_LeftBar_LevelsSize / 2);
  protected static final int displayLocations_LeftBar_LevelsCenterY = displayLocations_WindowHeight / 2;

  /**
   * Constructor for DisplayLevel.
   * <p>
   * Creates a new displayable window with the given level, starting on the top level.
   *
   * @param level The level data.
   */
  public DisplayLevel(Level level)
  {
    lvl = level;
    layer = 0;
    old = lvl.clone();
  }

  /**
   * When a user left-clicks on the level selector, change to that level.
   * <p>
   * If they left-click on the main board, start a dragpath.
   *
   * @param clickType Enumeration of which mouse button was
   *                  used to do the click. See {@link org.lwjgl.glfw.GLFW#GLFW_MOUSE_BUTTON_1}
   *                  through {@link org.lwjgl.glfw.GLFW#GLFW_MOUSE_BUTTON_8}.
   * @param location  The point on the screen that was clicked.
   */
  public void doClick(int clickType, Point location)
  {
    if (clickType != GLFW_MOUSE_BUTTON_1)
      return;

    if (Driver.DEBUG) System.out.println("Click registered at: [" + location.getX() + "," + location.getY() + "].");

    layer = onAnotherLevel(location);

    Point3I cell = getSquare(location);
    if (cell != null)
    {
      Path path = lvl.getPath(cell);
      if (path != null)
      {
        LinkedList<Point3I> flow = lvl.getFlowPath(path.getColor());
        if (flow != null && flow.contains(cell) && path.getType() != PathType.START)
        {
          dragPath = flow;
          doDrag(location);
        }
        else
        {
          dragPath = new LinkedList<>();
          dragPath.add(getSquare(location));
        }
      }
    }
  }

  /**
   * When the user clicks and holds, then moves the mouse,
   * this will be called. Warning: This is asynchronous, and
   * can happen at any time, EVEN WHEN OTHER METHODS ARE
   * INPROGRESS.
   *
   * @param location The point on the screen where the mouse was moved to.
   */
  public void doDrag(Point location)
  {
    if (dragPath == null)
      return;

    Point3I cell = getSquare(location);

    if (cell != null && !dragPath.getLast().equals(cell))
    {
      while (dragPath.contains(cell))
        dragPath.removeLast();
      dragPath.add(cell);
      if (Driver.DEBUG)
      {
        System.out.print("The drag path is: [");
        for (Point3I p : dragPath)
          System.out.print(p.toString() + ",");
        System.out.println("].");
      }
    }
  }

  /**
   * When the user clicks, then lets go, this will be
   * called. Warning: This is asynchronous, and can happen
   * at any time, EVEN WHEN OTHER METHODS ARE INPROGRESS.
   *
   * @param clickType Enumeration of which mouse button was used to do the click. See {@link GLFW#GLFW_MOUSE_BUTTON_1} through {@link GLFW#GLFW_MOUSE_BUTTON_8}
   * @param location  The point on the screen where the mouse is, upon being released.
   */
  public void doRelease(int clickType, Point location)
  {
    if (dragPath != null)
    {
      makeDragPermanent();
      dragPath = null;
      old = lvl;
    }

    if (old.checkWin())
    {
      if (Driver.DEBUG) System.out.println("U WON. NICE.");
      fade.setIncreasing();
    }
  }

  /**
   * When the user attempts to scroll, this will be
   * called. Warning: This is asynchronous, and can happen
   * at any time, EVEN WHEN OTHER METHODS ARE INPROGRESS.
   *
   * @param directionIsUp This will be {@code true}, if the scroll is upward.
   * @param location      The point on the screen where the mouse is, when it is scrolled.
   */
  public void doScroll(boolean directionIsUp, Point location)
  {
    if (Driver.DEBUG)
      System.out.println("An " + (directionIsUp ? "upward" : "downward") + " scroll has been registered!");
    Point3I loc = getSquare(location);
    Point3I adj = null;
    if (loc != null)
      adj = loc.add(0, 0, directionIsUp ? -1 : 1);
    if (dragPath == null || (getSquare(location) != null && (lvl.isDrawable(adj) || (lvl.getPath(adj) != null && lvl.getPath(adj).getColor() == lvl.getPath(loc).getColor()))))
    {
      if (Driver.DEBUG) System.out.println("In here!");
      if (directionIsUp && layer > 0)
        layer--;
      if (!directionIsUp && layer + 1 < lvl.size())
        layer++;
    }

    // If the mouse is currently held down, then we are drawing a path to a new layer.
    if (dragPath != null)
      doDrag(location);
  }

  /**
   * Abstract method to be implemented by subclasses to do the actual rendering of the window.
   *
   * @param window The GLFWwindow pointer that holds the window's data. Needed to do any displaying.
   */
  public void display(long window)
  {
    if (fade.get() == 60 && fade.isIncreasing())
      Display.setDisplay(new DisplaySelect(Level.easy(), Level.medium(), Level.hard()));
    fade.tick();

    // Line between left and right sections
    Display.setColor3(new Color(191, 191, 191));
    Display.drawRectangleOr(displayLocations_LeftBarWidth, displayLocations_WindowCenterY, 2, displayLocations_WindowHeight, true);

    // Update paths for top layer. This is the largest waste of CPU I could come up with. All that cloneing.
    if (dragPath != null)
    {
      lvl = old.clone();
      LinkedList<Point3I> cpy = (LinkedList<Point3I>) dragPath.clone();
      makeDragPermanent();
      dragPath = cpy;
    }

    // Left bar and layers within
    for (int i = layer; i > layer - lvl.size(); i--)
    {
      Display.setColor3(new Color(0, 0, 95));
      Display.drawRectangleOr(displayLocations_LeftBar_LevelsCenterX, displayLocations_LeftBar_LevelsCenterY - displayLocations_LeftBar_LevelsBufferedSize * i, displayLocations_LeftBar_LevelsSize, displayLocations_LeftBar_LevelsSize, true);
      drawLayerOr(layer - i, displayLocations_LeftBar_LevelsCenterX, displayLocations_LeftBar_LevelsCenterY - displayLocations_LeftBar_LevelsBufferedSize * i, displayLocations_LeftBar_LevelsSize);
    }

    // Display main window
    Display.setColor3(new Color(0, 0, 95));
    Display.drawRectangleOr(displayLocations_GameCenterX, displayLocations_GameCenterY, displayLocations_GameSize, displayLocations_GameSize, true);
    drawLayerOr(layer, displayLocations_GameCenterX, displayLocations_GameCenterY, displayLocations_GameSize);

    // Fade in/out
    Display.enableTransparency();
    Display.setColor4(0, 0, 31, (int) (255 * (fade.get() / 60D)));
    Display.drawRectangleOr(400, 300, 800, 600, true);
    Display.disableTransparency();
  }



  // **************** HELPER/ALMOSTSTATIC METHODS **************** // TODO



  /**
   * If the mouse is over another level on the left, return the id of that level.
   * Otherwise, return the current level's id.
   *
   * @param clickLocation The mouse's current position (probably over a level).
   * @return The level that the mouse is over, or if it isn't over a level, the current one.
   */
  private int onAnotherLevel(Point clickLocation)
  {
    final int quantity = lvl.size();
    final int yMid = displayLocations_WindowHeight / 2;

    int x = (int) clickLocation.getX();
    int y = (int) clickLocation.getY();
    if (x >= displayLocations_LeftBar_LevelsBufferLeft && x <= displayLocations_LeftBar_LevelsBufferLeft + displayLocations_LeftBar_LevelsSize)
    {
      int offset = yMid + displayLocations_LeftBar_LevelsSize / 2 + quantity * (displayLocations_LeftBar_LevelsSize + displayLocations_LeftBar_LevelsBuffer) - y;
      if (offset % (displayLocations_LeftBar_LevelsSize + displayLocations_LeftBar_LevelsBuffer) <= displayLocations_LeftBar_LevelsSize)
      {
        int ret = layer - (offset / (displayLocations_LeftBar_LevelsSize + displayLocations_LeftBar_LevelsBuffer) - quantity);
        if (ret < lvl.size() && ret >= 0)
        {
          if (Driver.DEBUG) System.out.println("Now on layer: " + ret);
          return ret;
        }
      }
    }
    return layer;
  }

  /**
   * If the mouse is over the main board or not.
   *
   * @param clickLocation The mouse's current position (probably over the main board).
   * @return whether the mouse is over the main board or not.
   */
  private boolean onMainBoard(Point clickLocation)
  {
    int x = (int) clickLocation.getX();
    int y = (int) clickLocation.getY();
    return ((x >= displayLocations_GameLeft && x <= displayLocations_GameRight) && (y >= displayLocations_GameTop && y <= displayLocations_GameBottom));
  }

  /**
   * The square/cell that the mouse is over, or null if it isn't over a square.
   *
   * @param clickLocation The mouse's current position (probably over the main board).
   * @return the {@link Point3I} that the mouse is over, or null if it isn't over a square.
   */
  private Point3I getSquare(Point clickLocation)
  {
    if (!onMainBoard(clickLocation))
      return null;
    int cubeSize = lvl.size();
    int x = (int) clickLocation.getX() - displayLocations_GameLeft;
    int y = (int) clickLocation.getY() - displayLocations_GameTop;

    return new Point3I(cubeSize * x / displayLocations_GameSize, cubeSize * y / displayLocations_GameSize, layer);
  }



  // **************** METHODS THAT DO STUFF **************** // TODO



  /**
   * Takes the drag path and applies it to the current level.
   */
  private void makeDragPermanent()
  {
    if (dragPath == null || dragPath.size() == 0 || lvl.getPath(dragPath.getFirst()) == null)
      throw new IllegalArgumentException("No path to make permanent!");
    if (dragPath.size() == 1)
      lvl.clearColor(lvl.getPath(dragPath.getFirst()).getColor());

    // Find the color of the path that is being drawn.
    PathColor clr = lvl.getPath(dragPath.getFirst()).getColor();
    // Clear those paths, so we can redraw them where they need to go.
    lvl.clearColor(clr);

    if (lvl.getPath(dragPath.getFirst()).getType() != PathType.START)
      throw new IllegalArgumentException("First path is not a START.");

    // Make sure all elements are next to each other and legal.
    for (int i = 1; i < dragPath.size(); i++)
    {
      if (!lvl.validLocation(dragPath.get(i)))
      {
        dragPath.remove(i--);
      }
      Point3I here = dragPath.get(i);
      boolean nextTo = false;
      here = dragPath.get(i - 1).add(-here.getX(), -here.getY(), -here.getZ());
      for (PathDirection pdir : PathDirection.DIRECTIONS)
        if (
            pdir.move(new Point3I()).equals(here)) nextTo = true;
      if (!nextTo)
        dragPath.remove(i--);
    }

    // For each location in the dragpath, try to add the new path, and stop when reaching a point that cannot be passed.
    // i=0 is the {@code Start}ing point. Always.
    for (int i = 1; i < dragPath.size(); i++)
    {
      Point3I dragPathi = dragPath.get(i);
      Point3I dragPathiPrev = dragPath.get(i - 1);
      // Remove any paths that this path crosses paths with.
      Path pathToReplace = lvl.getPath(dragPathi);
      if (pathToReplace != null)
      {

        // Remove any elements in the cutoffflow after this point.
        LinkedList<Point3I> flow = lvl.getFlowPath(pathToReplace.getColor());
        if (flow != null && flow.contains(dragPathi))
        {
          while (!flow.getFirst().equals(dragPathi))
            flow.removeFirst();
          flow.removeFirst();

          for (Point3I pt : flow)
            if (lvl.isDrawable(pt))
              lvl.deletePath(pt);
        }

        // The one before it should no longer point to the one that isn't there.
        Path previousCutOff = lvl.getPath(lvl.getPreviousInFlow(dragPathi));
        if (previousCutOff != null)
          previousCutOff.setDirection(null);
        if (lvl.isDrawable(dragPathi))
          lvl.deletePath(dragPathi);
      }

      // if it is a start, and this color, connect to it.
      if (!lvl.isDrawable(dragPathi))
      {
        if (lvl.getPath(dragPathi).getColor() == clr)
        {
          lvl.getPath(dragPathiPrev).setDirection(PathDirection.getDirection(dragPathiPrev, dragPathi));
          lvl.getPath(dragPathi).setDirection(null);
        }
        i = dragPath.size();
      }
      else
      {
        lvl.setPath(dragPathi, clr, null);
        lvl.getPath(dragPathiPrev).setDirection(PathDirection.getDirection(dragPathiPrev, dragPathi));
      }
    }

  }

  /**
   * Given a position and size, draw a layer on the display.
   *
   * @param layer The layer of the {@link Cube} that is being drawn.
   * @param xPos  The X position of the center of the level to be drawn.
   * @param yPos  The Y position of the center of the level to be drawn.
   * @param width The width in pixels of the size of the level to be drawn.
   */
  private void drawLayerOr(int layer, double xPos, double yPos, double width)
  {
    xPos -= width / 2D;
    yPos -= width / 2D;
    for (int x = 0; x < lvl.size(); x++)
      for (int y = 0; y < lvl.size(); y++)
      {
        if (old.getPath(x, y, layer) != null)
        {
          Display.enableTransparency();
          Display.setColor4(old.getPath(x, y, layer).getColor().toColor(), 63);
          Display.drawRectangleOr((int) (xPos + (x + .5) * width / lvl.size()), (int) (yPos + (y + .5) * width / lvl.size()), (int) (width / lvl.size()), (int) (width / lvl.size()), true);
          Display.disableTransparency();
        }
        drawPath(new Point3I(x, y, layer), xPos + x * width / lvl.size(), yPos + y * width / lvl.size(), width / lvl.size());
      }

    drawGrid(xPos, yPos, width);
  }

  /**
   * Draws the background grid of a level at a given location with a given size.
   *
   * @param xPos  The X position of the center of the grid to be drawn.
   * @param yPos  The Y position of the center of the grid to be drawn.
   * @param width The width in pixels of the size of the grid to be drawn.
   */
  private void drawGrid(double xPos, double yPos, double width)
  {
    int size = lvl.size();

    Display.setColor3(new Color(191, 191, 191));
    glBegin(GL_LINE_LOOP);
    Display.doPointOr(xPos, yPos);
    Display.doPointOr(xPos + width, yPos);
    Display.doPointOr(xPos + width, yPos + width);
    Display.doPointOr(xPos, yPos + width);
    glEnd();

    glBegin(GL_LINES);
    for (int r = 1; r < size; r++)
    {
      Display.doPointOr(xPos, yPos + r * width / size);
      Display.doPointOr(xPos + width, yPos + r * width / size);
    }
    for (int c = 1; c < size; c++)
    {
      Display.doPointOr(xPos + c * width / size, yPos);
      Display.doPointOr(xPos + c * width / size, yPos + width);
    }
    glEnd();
  }

  /**
   * Draws a specified path at a specific location and size.
   *
   * @param pt    The location of the path to be drawn.
   * @param xPos  The X position of the center of the path to be drawn.
   * @param yPos  The Y position of the center of the path to be drawn.
   * @param width The width in pixels of the size of the path to be drawn.
   */
  private void drawPath(Point3I pt, double xPos, double yPos, double width)
  {
    xPos += width / 2D;
    yPos += width / 2D;
    Path path = lvl.getPath(pt);
    if (path != null)
    {

      Display.setColor3(path.getColor().toColor());

      if (path.getType() == PathType.START)
        Display.doCircle(xPos, yPos, width / 3D, true);
      if (path.getType() == PathType.PATH)
        Display.doCircle(xPos, yPos, width / 4D, true);

      PathDirection nextDirection = path.getDirection();
      if (nextDirection != null)
      {
        switch (nextDirection)
        {
          case UP:
            //Display.drawRectangleOr(xPos,yPos - width/2, width/2, width, true);
            glBegin(GL_POLYGON);
            Display.doPointOr(xPos - width / 4D, yPos - width);
            Display.doPointOr(xPos - width / 4D, yPos);
            Display.doPointOr(xPos + width / 4D, yPos);
            Display.doPointOr(xPos + width / 4D, yPos - width);
            glEnd();
            break;
          case DOWN:
            glBegin(GL_POLYGON);
            Display.doPointOr(xPos - width / 4D, yPos);
            Display.doPointOr(xPos - width / 4D, yPos + width);
            Display.doPointOr(xPos + width / 4D, yPos + width);
            Display.doPointOr(xPos + width / 4D, yPos);
            glEnd();
            break;
          case LEFT:
            glBegin(GL_POLYGON);
            Display.doPointOr(xPos - width, yPos - width / 4D);
            Display.doPointOr(xPos, yPos - width / 4D);
            Display.doPointOr(xPos, yPos + width / 4D);
            Display.doPointOr(xPos - width, yPos + width / 4D);
            glEnd();
            break;
          case RIGHT:
            glBegin(GL_POLYGON);
            Display.doPointOr(xPos, yPos - width / 4D);
            Display.doPointOr(xPos + width, yPos - width / 4D);
            Display.doPointOr(xPos + width, yPos + width / 4D);
            Display.doPointOr(xPos, yPos + width / 4D);
            glEnd();
            break;
          default:
        }

        Display.setColor3(path.getColor().toColor().darker());
        switch (nextDirection)
        {
          case IN:
            glBegin(GL_TRIANGLES);
            Display.doPointOr(xPos - width / 5D, yPos + width / 4D);
            Display.doPointOr(xPos + width / 5D, yPos + width / 4D);
            Display.doPointOr(xPos, yPos + width / 2D);
            glEnd();
            break;
          case OUT:
            glBegin(GL_TRIANGLES);
            Display.doPointOr(xPos - width / 5D, yPos - width / 4D);
            Display.doPointOr(xPos + width / 5D, yPos - width / 4D);
            Display.doPointOr(xPos, yPos - width / 2D);
            glEnd();
            break;
        }
      }
    }

    // The ones above.
    path = lvl.getPath(PathDirection.IN.move(pt));
    if (path != null)
    {
      Display.setColor3(path.getColor().toColor().darker());
      if (lvl.getPath(pt) != null && path.getColor() == lvl.getPath(pt).getColor() && path.getDirection() == PathDirection.OUT)
      {
        glBegin(GL_TRIANGLES);
        Display.doPointOr(xPos - width / 5D, yPos + width / 4D);
        Display.doPointOr(xPos + width / 5D, yPos + width / 4D);
        Display.doPointOr(xPos, yPos + width / 2D);
        glEnd();
      }
      else
        Display.doCircle(xPos, yPos + width / 3D, width / 16D, true);
    }

    // The ones below
    path = lvl.getPath(PathDirection.OUT.move(pt));
    if (path != null)
    {
      Display.setColor3(path.getColor().toColor().darker());
      if (lvl.getPath(pt) != null && path.getColor() == lvl.getPath(pt).getColor() && path.getDirection() == PathDirection.IN)
      {
        glBegin(GL_TRIANGLES);
        Display.doPointOr(xPos - width / 5D, yPos - width / 4D);
        Display.doPointOr(xPos + width / 5D, yPos - width / 4D);
        Display.doPointOr(xPos, yPos - width / 2D);
        glEnd();
      }
      else
        Display.doCircle(xPos, yPos - width / 3D, width / 16D, true);
    }
  }
}
