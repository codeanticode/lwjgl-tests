package test.applet;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.PixelFormat;

import test.utils.ShaderUtils;

@SuppressWarnings("serial")
public class DispApplet extends Applet implements Runnable {
  static public int APPLET_WIDTH  = 500;
  static public int APPLET_HEIGHT = 290;
  static public int TARGET_FPS    = 120;
  static public boolean MANUAL_SYNC = false;
  
  //////////////////////////////////////////////////////////////////////////////
  
  static private Frame frame;
  static private DispApplet applet;
  private Canvas canvas;
  
  private int width;
  private int height;
  private Thread thread;
  
  private boolean doneSetup = false;
  
  private long frameRatePeriod = 1000000000L / TARGET_FPS;
  private long millisOffset;
  private int frameCount;
  private float frameRate;
  
  private int vertShader;
  private int fragShader;
  private int shaderProg;
  private int resLoc;
  private int timeLoc;
  private int vertLoc;
  private FloatBuffer vertices;
  
  private int fcount = 0, lastm = 0;  
  private int fint = 1;      
    
  public void init() {
    setSize(APPLET_WIDTH, APPLET_HEIGHT);
    setPreferredSize(new Dimension(APPLET_WIDTH, APPLET_HEIGHT));
    width = APPLET_WIDTH;
    height = APPLET_HEIGHT;
  }
  
  public void start() {
    thread = new Thread(this, "Animation Thread");
    thread.start();    
  }
  
  public void run() {    
    initGL();
    
    int noDelays = 0;
    // Number of frames with a delay of 0 ms before the
    // animation thread yields to other running threads.
    final int NO_DELAYS_PER_YIELD = 15;
    
    long beforeTime = System.nanoTime();
    long overSleepTime = 0L;
    
    millisOffset = System.currentTimeMillis();
    frameCount = 1;
    while (Thread.currentThread() == thread) {
      requestDraw();
      
      if (frameCount == 1) {
        EventQueue.invokeLater(new Runnable() {
          public void run() {
            requestFocusInWindow();
          }
        });
      }      
      
      if (MANUAL_SYNC) {
        long afterTime = System.nanoTime();
        long timeDiff = afterTime - beforeTime;
        long sleepTime = (frameRatePeriod - timeDiff) - overSleepTime;      
        if (sleepTime > 0) {  // some time left in this cycle
          try {
            Thread.sleep(sleepTime / 1000000L, (int) (sleepTime % 1000000L));
            noDelays = 0;  // Got some sleep, not delaying anymore
          } catch (InterruptedException ex) { }
          overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
        } else {    // sleepTime <= 0; the frame took longer than the period
          overSleepTime = 0L;
          noDelays++;
          if (noDelays > NO_DELAYS_PER_YIELD) {
            Thread.yield();   // give another thread a chance to run
            noDelays = 0;
          }
        }
        beforeTime = System.nanoTime();
      } else {
        Thread.yield();
      }
    }
  }
  
  public void requestDraw() {
    if (doneSetup) {
      draw();
    } else {
      setup();
    }
    
    Display.update();
    if (!MANUAL_SYNC) {
      Display.sync(TARGET_FPS);
    }
  }
  
  private void initGL() {
    canvas = new Canvas();
    canvas.setFocusable(true);
    canvas.requestFocus();
    canvas.setBackground(new Color(0xFFCCCCCC, true));   
    canvas.setBounds(0, 0, applet.width, applet.height);
    
    applet.setLayout(new BorderLayout());
    applet.add(canvas, BorderLayout.CENTER);
    
    try {      
      PixelFormat format = new PixelFormat(32, 8, 24, 8, 1);
      Display.setDisplayMode(new DisplayMode(applet.width, applet.height));
      int argb = 0xFFCCCCCC;
      float r = ((argb >> 16) & 0xff) / 255.0f;
      float g = ((argb >> 8) & 0xff) / 255.0f;
      float b = ((argb) & 0xff) / 255.0f; 
      Display.setInitialBackground(r, g, b); 
      Display.setParent(canvas);      
      Display.create(format);
    } catch (LWJGLException e) {
      e.printStackTrace();
    }
  }
  
  private void setup() {
    if (60 < TARGET_FPS) {
      // Disables vsync
      Display.setVSyncEnabled(false);  
    }
        
    String vertSource = ""; 
    try {
      vertSource = ShaderUtils.loadShaderSource(DispApplet.class.getResource("shaders" + File.separator + "landscape.vp"));
    } catch (Exception e) {
      e.printStackTrace();
    }    
    
    String fragSource = ""; 
    try {
      fragSource = ShaderUtils.loadShaderSource(DispApplet.class.getResource("shaders" + File.separator + "landscape.fp"));
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    vertShader = ShaderUtils.createShader(GL20.GL_VERTEX_SHADER, vertSource);
    fragShader = ShaderUtils.createShader(GL20.GL_FRAGMENT_SHADER, fragSource);
    shaderProg = ShaderUtils.createProgram(vertShader, fragShader);    
    resLoc = GL20.glGetUniformLocation(shaderProg, "iResolution");
    timeLoc = GL20.glGetUniformLocation(shaderProg, "iGlobalTime");
    vertLoc = GL20.glGetAttribLocation(shaderProg, "inVertex");
    
    vertices = BufferUtils.createFloatBuffer(16);
    vertices.put(new float[] { -1.0f, -1.0f, 
                               +1.0f, -1.0f, 
                               -1.0f, +1.0f,
                               +1.0f, +1.0f }); 
    
    doneSetup = true;
  }

  private void draw() {
    GL11.glClearColor(0.5f, 0.1f, 0.1f, 1);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

    GL20.glUseProgram(shaderProg);   
    GL20.glUniform3f(resLoc, (float)width, (float)height, 0);
    GL20.glUniform1f(timeLoc, (System.currentTimeMillis() - millisOffset) / 1000.0f);    
    GL20.glEnableVertexAttribArray(vertLoc);
    vertices.position(0);
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    GL20.glVertexAttribPointer(vertLoc, 2, false, 0, vertices);
    GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);    
    GL20.glDisableVertexAttribArray(vertLoc);
    GL20.glUseProgram(0);
    
    // Compute current framerate and printout.
    frameCount++;      
    fcount += 1;
    int m = (int) (System.currentTimeMillis() - millisOffset);
    if (m - lastm > 1000 * fint) {
      frameRate = (float)(fcount) / fint;
      fcount = 0;
      lastm = m;
    }         
    if (frameCount % TARGET_FPS == 0) {
      System.out.println("FrameCount: " + frameCount + " - " + 
                         "FrameRate: " + frameRate);
    }    
  }  
  
  static public void main(String[] args) {    
    GraphicsEnvironment environment = 
        GraphicsEnvironment.getLocalGraphicsEnvironment();
    GraphicsDevice displayDevice = environment.getDefaultScreenDevice();

    frame = new Frame(displayDevice.getDefaultConfiguration());
    frame.setBackground(new Color(0xCC, 0xCC, 0xCC));
    frame.setTitle("LWJGL Applet");
    
    try {
      Class<?> c = Thread.currentThread().getContextClassLoader().
          loadClass(DispApplet.class.getName());
      applet = (DispApplet) c.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }    
    
    frame.setLayout(null);
    frame.add(applet);
    frame.pack();
    frame.setResizable(false);
    
    applet.init();
    
    Insets insets = frame.getInsets();
    int windowW = applet.width + insets.left + insets.right;
    int windowH = applet.height + insets.top + insets.bottom;
    frame.setSize(windowW, windowH);    
    
    Rectangle screenRect = displayDevice.getDefaultConfiguration().getBounds();    
    frame.setLocation(screenRect.x + (screenRect.width - applet.width) / 2,
        screenRect.y + (screenRect.height - applet.height) / 2);    
    
    int usableWindowH = windowH - insets.top - insets.bottom;
    applet.setBounds((windowW - applet.width)/2,
                     insets.top + (usableWindowH - applet.height)/2,
                     applet.width, applet.height);
    
    // This allows to close the frame.
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });      
    
    frame.setVisible(true);
    applet.start();    
  }
}
