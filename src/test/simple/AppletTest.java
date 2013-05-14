package test.simple;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import test.utils.ShaderUtils;

@SuppressWarnings("serial")
public class AppletTest extends Applet {
  static public int width  = 500;
  static public int height = 290;

  private Canvas canvas;  
  private Thread thread;
  private boolean running = false;  
  
  private int vertShader;
  private int fragShader;
  private int shaderProg;
  private int resLoc;
  private int timeLoc;
  private int vertLoc;
  private FloatBuffer vertices;  
  
  private long millisInit;
  private int frameCount;
 
  private int fcount, lastm;  
  private int fint = 1;  
  private float frameRate;    
  
  public void startLWJGL() {
    thread = new Thread() {
      public void run() {
        running = true;
        millisInit = System.currentTimeMillis();
        frameCount = 1;
        try {
          Display.setParent(canvas);
          Display.create();
          setup();
        } catch (LWJGLException e) {
          e.printStackTrace();
          return;
        }
        drawLoop();
      }
    };
    thread.start();
  }
  
  /**
   * Tell game loop to stop running, after which the LWJGL Display will 
   * be destoryed. The main thread will wait for the Display.destroy().
   */
  private void stopLWJGL() {
    running = false;
    try {
      thread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void start() { }

  public void stop() { }
  
  /**
   * Applet Destroy method will remove the canvas, 
   * before canvas is destroyed it will notify stopLWJGL()
   * to stop the main game loop and to destroy the Display
   */
  public void destroy() {
    remove(canvas);
    super.destroy();
  }
  
  public void init() {
    setLayout(new BorderLayout());
    try {
      canvas = new Canvas() {
        public final void addNotify() {
          super.addNotify();
          startLWJGL();
        }
        public final void removeNotify() {
          stopLWJGL();
          super.removeNotify();
        }
      };
      setSize(width, height);
      canvas.setSize(getWidth(),getHeight());
      add(canvas);
      canvas.setFocusable(true);
      canvas.requestFocus();
      canvas.setIgnoreRepaint(true);
      setVisible(true);
    } catch (Exception e) {
      System.err.println(e);
      throw new RuntimeException("Unable to create display");
    }
  }

  private void setup() {
    String vertSource = ""; 
    try {
      vertSource = ShaderUtils.loadShaderSource(AppletTest.class.getResource("shaders" + File.separator + "landscape.vp"));
    } catch (Exception e) {
      e.printStackTrace();
    }    
    
    String fragSource = ""; 
    try {
      fragSource = ShaderUtils.loadShaderSource(AppletTest.class.getResource("shaders" + File.separator + "landscape.fp"));
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
  }
  
  private void drawLoop() {
    while(running) draw();
    Display.destroy();
  }
  
  private void draw() {
    GL11.glClearColor(0.5f, 0.1f, 0.1f, 1);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

    GL20.glUseProgram(shaderProg);   
    GL20.glUniform3f(resLoc, (float)width, (float)height, 0);
    GL20.glUniform1f(timeLoc, (System.currentTimeMillis() - millisInit) / 1000.0f);    
    GL20.glEnableVertexAttribArray(vertLoc);
    vertices.position(0);
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    GL20.glVertexAttribPointer(vertLoc, 2, false, 0, vertices);
    GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);    
    GL20.glDisableVertexAttribArray(vertLoc);
    GL20.glUseProgram(0);
    
    // Compute current framerate and printout.
    fcount += 1;
    int m = (int) (System.currentTimeMillis() - millisInit);
    if (m - lastm > 1000 * fint) {
      frameRate = (float)(fcount) / fint;
      fcount = 0;
      lastm = m;
    }      
    
    if (frameCount % 60 == 0) {
      System.out.println("frame: " + frameCount +" - fps: " + frameRate);
    }
    
    frameCount++;
    
    Display.sync(60);
    Display.update();    
  }  
  
  public Dimension getPreferredSize() {
    return new Dimension(width, height);
  }  
  
  static public void main(String[] args) {
    Frame f = new Frame("test");
    f.setLayout(new BorderLayout());
    Applet test = new AppletTest();
    f.add(test, BorderLayout.CENTER);
    f.pack();
    f.setVisible(true);
    test.init();
  }
}
