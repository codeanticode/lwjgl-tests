package test.applet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

public class ShaderUtils {
  
  public static int createShader(int shaderType, String source) {
    int shader = GL20.glCreateShader(shaderType);
    if (shader != 0) {
      GL20.glShaderSource(shader, source);
      GL20.glCompileShader(shader);
      if (!compiled(shader)) {
        System.err.println("Could not compile shader " + shaderType + ":");
        System.err.println(getShaderInfoLog(shader));
        GL20.glDeleteShader(shader);
        shader = 0;
      }
    }
    return shader;
  }
  
  public static int createProgram(int vertexShader, int fragmentShader) {
    int program = GL20.glCreateProgram();
    if (program != 0) {
      GL20.glAttachShader(program, vertexShader);
      GL20.glAttachShader(program, fragmentShader);
      GL20.glLinkProgram(program);
      if (!linked(program)) {
        System.err.println("Could not link program: ");
        System.err.println(getProgramInfoLog(program));
        GL20.glDeleteProgram(program);
        program = 0;
      }
    }
    return program;
  }
  
  private static boolean compiled(int shader) {
    IntBuffer intBuffer = BufferUtils.createIntBuffer(16);
    GL20.glGetShader(shader, GL20.GL_COMPILE_STATUS, intBuffer);
    return intBuffer.get(0) == 0 ? false : true;
  }  
  
  private static boolean linked(int program) {
    IntBuffer intBuffer = BufferUtils.createIntBuffer(16);
    GL20.glGetProgram(program, GL20.GL_LINK_STATUS, intBuffer);
    return intBuffer.get(0) == 0 ? false : true;
  }  
  
  private static String getShaderInfoLog(int shader) {
    int len = GL20.glGetShaderi(shader, GL20.GL_INFO_LOG_LENGTH);
    return GL20.glGetShaderInfoLog(shader, len);
  }
  
  private static String getProgramInfoLog(int prog) {
    int len = GL20.glGetProgrami(prog, GL20.GL_INFO_LOG_LENGTH);
    return GL20.glGetProgramInfoLog(prog, len);
  }  

  public static String loadShaderSource(String name) throws 
  FileNotFoundException, IOException, URISyntaxException {
    URL url = ShaderUtils.class.getResource("shaders" + File.separator + name);
    File file = new File(url.toURI());
    BufferedReader br = new BufferedReader(new FileReader(file));
    String source = "";
    String line = br.readLine();
    while(line != null) {
      source += line;
      source += "\n";
      line = br.readLine();
    }      
    br.close();
    return source;
  }
}