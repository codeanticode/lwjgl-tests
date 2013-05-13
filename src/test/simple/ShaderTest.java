package test.simple;

import java.io.File;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import test.utils.ShaderUtils;

public class ShaderTest {
  static public int width  = 500;
  static public int height = 290;

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
  
  public void start() {
    try {
      Display.setDisplayMode(new DisplayMode(width, height));
      Display.create();
    } catch (LWJGLException e) {
      e.printStackTrace();
      System.exit(0);
    }

    millisInit = System.currentTimeMillis();
    frameCount = 1;
    setup();

    while (!Display.isCloseRequested()) {
      draw();
      Display.update();
    }

    Display.destroy();
  }
  
  private void setup() {
    String vertSource = ""; 
    try {
      vertSource = ShaderUtils.loadShaderSource(ShaderTest.class.getResource("shaders" + File.separator + "landscape.vp"));
    } catch (Exception e) {
      e.printStackTrace();
    }    
    
    String fragSource = ""; 
    try {
      fragSource = ShaderUtils.loadShaderSource(ShaderTest.class.getResource("shaders" + File.separator + "landscape.fp"));
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
  }
  
  public static void main(String[] argv) {
    ShaderTest shaderTest = new ShaderTest();
    shaderTest.start();
  }
}