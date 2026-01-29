/*
 * Copyright 2024 Google LLC
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
package com.google.ar.core.examples.java.common.rendering;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.Matrix;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders lines between points in 3D space.
 */
public class LineRenderer {
  private static final String TAG = LineRenderer.class.getSimpleName();

  // Shader names.
  private static final String VERTEX_SHADER_NAME = "shaders/line.vert";
  private static final String FRAGMENT_SHADER_NAME = "shaders/line.frag";

  private static final int COORDS_PER_VERTEX = 3;
  private static final int BYTES_PER_FLOAT = 4;

  private int lineProgram;
  private int arrowProgram;
  private int linePositionAttribute;
  private int arrowPositionAttribute;
  private int lineColorUniform;
  private int arrowColorUniform;
  private int lineMvpMatrixUniform;
  private int arrowMvpMatrixUniform;

  private FloatBuffer lineVertexBuffer;
  private FloatBuffer arrowVertexBuffer;
  private int lineVertexCount = 0;
  private int arrowVertexCount = 0;

  // Line color (RGBA)
  private float[] lineColor = {0.55f, 0.25f, 0.75f, 0.60f}; // More opaque purple to match reference
  private float[] arrowColor = {1.0f, 1.0f, 1.0f, 0.95f}; // White arrows with slight transparency
  private float lineWidthMeters = 0.15f; // Line width in world space (meters) - thick like reference image
  private float arrowSpacing = 0.4f; // Distance between arrows in meters

  /**
   * Allocates and initializes OpenGL resources needed by the line renderer. Must be called on the
   * OpenGL thread, typically in {@link android.opengl.GLSurfaceView.Renderer#onSurfaceCreated}.
   *
   * @param context Needed to access shader source.
   */
  public void createOnGlThread(Context context) throws IOException {
    // Create line shader program
    int vertexShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_NAME);
    int fragmentShader =
        ShaderUtil.loadGLShader(TAG, context, GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_NAME);

    lineProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(lineProgram, vertexShader);
    GLES20.glAttachShader(lineProgram, fragmentShader);
    GLES20.glLinkProgram(lineProgram);

    ShaderUtil.checkGLError(TAG, "Line program creation");

    linePositionAttribute = GLES20.glGetAttribLocation(lineProgram, "a_Position");
    lineColorUniform = GLES20.glGetUniformLocation(lineProgram, "u_Color");
    lineMvpMatrixUniform = GLES20.glGetUniformLocation(lineProgram, "u_MvpMatrix");

    // Create arrow shader program (reuse same shaders)
    arrowProgram = GLES20.glCreateProgram();
    GLES20.glAttachShader(arrowProgram, vertexShader);
    GLES20.glAttachShader(arrowProgram, fragmentShader);
    GLES20.glLinkProgram(arrowProgram);

    ShaderUtil.checkGLError(TAG, "Arrow program creation");

    arrowPositionAttribute = GLES20.glGetAttribLocation(arrowProgram, "a_Position");
    arrowColorUniform = GLES20.glGetUniformLocation(arrowProgram, "u_Color");
    arrowMvpMatrixUniform = GLES20.glGetUniformLocation(arrowProgram, "u_MvpMatrix");

    ShaderUtil.checkGLError(TAG, "Program parameters");
  }

  /**
   * Updates the line vertices and generates arrow decorations.
   *
   * @param points List of 3D points (each point has x, y, z coordinates)
   */
  public void updateLines(List<float[]> points) {
    if (points == null || points.size() < 2) {
      lineVertexCount = 0;
      arrowVertexCount = 0;
      return;
    }

    List<Float> lineVertices = new ArrayList<>();
    List<Float> arrowVertices = new ArrayList<>();

    for (int i = 0; i < points.size() - 1; i++) {
      float[] p1 = points.get(i);
      float[] p2 = points.get(i + 1);

      // Generate thick line as a quad (two triangles)
      generateThickLineSegment(p1, p2, lineVertices);

      // Generate arrows along this segment
      generateArrowsAlongSegment(p1, p2, arrowVertices);
    }

    // Allocate line buffer
    lineVertexCount = lineVertices.size() / COORDS_PER_VERTEX;
    ByteBuffer lineBb = ByteBuffer.allocateDirect(lineVertices.size() * BYTES_PER_FLOAT);
    lineBb.order(ByteOrder.nativeOrder());
    lineVertexBuffer = lineBb.asFloatBuffer();
    for (Float f : lineVertices) {
      lineVertexBuffer.put(f);
    }
    lineVertexBuffer.position(0);

    // Allocate arrow buffer
    arrowVertexCount = arrowVertices.size() / COORDS_PER_VERTEX;
    if (arrowVertexCount > 0) {
      ByteBuffer arrowBb = ByteBuffer.allocateDirect(arrowVertices.size() * BYTES_PER_FLOAT);
      arrowBb.order(ByteOrder.nativeOrder());
      arrowVertexBuffer = arrowBb.asFloatBuffer();
      for (Float f : arrowVertices) {
        arrowVertexBuffer.put(f);
      }
      arrowVertexBuffer.position(0);
    }
  }

  /**
   * Generate a thick line segment as a rectangular quad (6 vertices for 2 triangles).
   */
  private void generateThickLineSegment(float[] p1, float[] p2, List<Float> vertices) {
    // Calculate direction vector
    float dx = p2[0] - p1[0];
    float dy = p2[1] - p1[1];
    float dz = p2[2] - p1[2];
    float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

    if (length < 0.001f) return;

    // Normalize direction
    dx /= length;
    dy /= length;
    dz /= length;

    // Calculate perpendicular vector (cross product with up vector)
    float[] up = {0, 1, 0};
    float perpX = dy * up[2] - dz * up[1];
    float perpY = dz * up[0] - dx * up[2];
    float perpZ = dx * up[1] - dy * up[0];

    float perpLen = (float) Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
    if (perpLen < 0.001f) {
      // Direction is parallel to up, use a different perpendicular
      up[0] = 1; up[1] = 0; up[2] = 0;
      perpX = dy * up[2] - dz * up[1];
      perpY = dz * up[0] - dx * up[2];
      perpZ = dx * up[1] - dy * up[0];
      perpLen = (float) Math.sqrt(perpX * perpX + perpY * perpY + perpZ * perpZ);
    }

    // Normalize perpendicular
    perpX /= perpLen;
    perpY /= perpLen;
    perpZ /= perpLen;

    // Scale by half width
    float halfWidth = lineWidthMeters / 2.0f;
    perpX *= halfWidth;
    perpY *= halfWidth;
    perpZ *= halfWidth;

    // Calculate 4 corners of the quad
    float p1x1 = p1[0] - perpX;
    float p1y1 = p1[1] - perpY;
    float p1z1 = p1[2] - perpZ;

    float p1x2 = p1[0] + perpX;
    float p1y2 = p1[1] + perpY;
    float p1z2 = p1[2] + perpZ;

    float p2x1 = p2[0] - perpX;
    float p2y1 = p2[1] - perpY;
    float p2z1 = p2[2] - perpZ;

    float p2x2 = p2[0] + perpX;
    float p2y2 = p2[1] + perpY;
    float p2z2 = p2[2] + perpZ;

    // First triangle (p1x1, p1x2, p2x1)
    vertices.add(p1x1); vertices.add(p1y1); vertices.add(p1z1);
    vertices.add(p1x2); vertices.add(p1y2); vertices.add(p1z2);
    vertices.add(p2x1); vertices.add(p2y1); vertices.add(p2z1);

    // Second triangle (p1x2, p2x2, p2x1)
    vertices.add(p1x2); vertices.add(p1y2); vertices.add(p1z2);
    vertices.add(p2x2); vertices.add(p2y2); vertices.add(p2z2);
    vertices.add(p2x1); vertices.add(p2y1); vertices.add(p2z1);
  }

  /**
   * Generate chevron arrows along a line segment as thick geometry.
   */
  private void generateArrowsAlongSegment(float[] p1, float[] p2, List<Float> arrowVertices) {
    // Calculate segment direction and length
    float dx = p2[0] - p1[0];
    float dy = p2[1] - p1[1];
    float dz = p2[2] - p1[2];
    float segmentLength = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

    if (segmentLength < 0.01f) return;

    // Normalize direction
    dx /= segmentLength;
    dy /= segmentLength;
    dz /= segmentLength;

    // Calculate perpendicular vectors for chevron
    float[] up = {0, 1, 0}; // Assume up is Y-axis
    float[] right = new float[3];

    // right = direction × up
    right[0] = dy * up[2] - dz * up[1];
    right[1] = dz * up[0] - dx * up[2];
    right[2] = dx * up[1] - dy * up[0];

    float rightLen = (float) Math.sqrt(right[0] * right[0] + right[1] * right[1] + right[2] * right[2]);
    if (rightLen > 0.01f) {
      right[0] /= rightLen;
      right[1] /= rightLen;
      right[2] /= rightLen;
    } else {
      // If parallel, use different perpendicular
      up[0] = 1; up[1] = 0; up[2] = 0;
      right[0] = dy * up[2] - dz * up[1];
      right[1] = dz * up[0] - dx * up[2];
      right[2] = dx * up[1] - dy * up[0];
      rightLen = (float) Math.sqrt(right[0] * right[0] + right[1] * right[1] + right[2] * right[2]);
      if (rightLen > 0.01f) {
        right[0] /= rightLen;
        right[1] /= rightLen;
        right[2] /= rightLen;
      }
    }

    // Generate arrows at regular intervals - matching reference image density
    float arrowWidth = 0.10f;  // Width of chevron arms (slightly narrower)
    float arrowLength = 0.18f;  // Length of chevron arms (slightly longer)
    float arrowThickness = 0.06f; // Thickness of arrow lines (thicker for more solid appearance)

    int numArrows = Math.max(1, (int) (segmentLength / arrowSpacing));
    for (int i = 1; i <= numArrows; i++) {
      float t = (float) i / (numArrows + 1);

      // Center point of arrow
      float cx = p1[0] + dx * segmentLength * t;
      float cy = p1[1] + dy * segmentLength * t;
      float cz = p1[2] + dz * segmentLength * t;

      // Create more triangular chevron shape
      // Tip of chevron (pointing forward)
      float tipX = cx + dx * arrowLength * 0.5f;
      float tipY = cy + dy * arrowLength * 0.5f;
      float tipZ = cz + dz * arrowLength * 0.5f;

      // Left arm back point
      float leftBackX = cx - dx * arrowLength * 0.5f - right[0] * arrowWidth;
      float leftBackY = cy - dy * arrowLength * 0.5f - right[1] * arrowWidth;
      float leftBackZ = cz - dz * arrowLength * 0.5f - right[2] * arrowWidth;

      // Right arm back point
      float rightBackX = cx - dx * arrowLength * 0.5f + right[0] * arrowWidth;
      float rightBackY = cy - dy * arrowLength * 0.5f + right[1] * arrowWidth;
      float rightBackZ = cz - dz * arrowLength * 0.5f + right[2] * arrowWidth;

      // Generate thick left arm as a quad
      generateThickArrowArm(leftBackX, leftBackY, leftBackZ, tipX, tipY, tipZ,
                           dx, dy, dz, right, arrowThickness, arrowVertices);

      // Generate thick right arm as a quad
      generateThickArrowArm(rightBackX, rightBackY, rightBackZ, tipX, tipY, tipZ,
                           dx, dy, dz, right, arrowThickness, arrowVertices);
    }
  }

  /**
   * Generate a thick arrow arm as a quad.
   */
  private void generateThickArrowArm(float x1, float y1, float z1, float x2, float y2, float z2,
                                     float dirX, float dirY, float dirZ, float[] right,
                                     float thickness, List<Float> vertices) {
    // Calculate perpendicular for thickness
    float dx = x2 - x1;
    float dy = y2 - y1;
    float dz = z2 - z1;
    float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    if (len < 0.001f) return;

    dx /= len;
    dy /= len;
    dz /= len;

    // Use right vector for thickness
    float thickX = right[0] * thickness / 2.0f;
    float thickY = right[1] * thickness / 2.0f;
    float thickZ = right[2] * thickness / 2.0f;

    // Four corners of the arm quad
    float x1a = x1 - thickX;
    float y1a = y1 - thickY;
    float z1a = z1 - thickZ;

    float x1b = x1 + thickX;
    float y1b = y1 + thickY;
    float z1b = z1 + thickZ;

    float x2a = x2 - thickX;
    float y2a = y2 - thickY;
    float z2a = z2 - thickZ;

    float x2b = x2 + thickX;
    float y2b = y2 + thickY;
    float z2b = z2 + thickZ;

    // First triangle
    vertices.add(x1a); vertices.add(y1a); vertices.add(z1a);
    vertices.add(x1b); vertices.add(y1b); vertices.add(z1b);
    vertices.add(x2a); vertices.add(y2a); vertices.add(z2a);

    // Second triangle
    vertices.add(x1b); vertices.add(y1b); vertices.add(z1b);
    vertices.add(x2b); vertices.add(y2b); vertices.add(z2b);
    vertices.add(x2a); vertices.add(y2a); vertices.add(z2a);
  }

  /**
   * Set the color of the lines.
   *
   * @param r Red component (0.0 to 1.0)
   * @param g Green component (0.0 to 1.0)
   * @param b Blue component (0.0 to 1.0)
   * @param a Alpha component (0.0 to 1.0)
   */
  public void setLineColor(float r, float g, float b, float a) {
    lineColor[0] = r;
    lineColor[1] = g;
    lineColor[2] = b;
    lineColor[3] = a;
  }

  /**
   * Set the width of the lines in meters (world space).
   *
   * @param widthMeters Line width in meters
   */
  public void setLineWidth(float widthMeters) {
    this.lineWidthMeters = widthMeters;
  }

  /**
   * Draws the lines and arrows.
   *
   * @param cameraView Camera view matrix.
   * @param cameraPerspective Camera projection matrix.
   */
  public void draw(float[] cameraView, float[] cameraPerspective) {
    if (lineVertexBuffer == null || lineVertexCount == 0) {
      return;
    }

    float[] mvpMatrix = new float[16];
    Matrix.multiplyMM(mvpMatrix, 0, cameraPerspective, 0, cameraView, 0);

    ShaderUtil.checkGLError(TAG, "Before draw");

    // Enable alpha blending for translucency
    GLES20.glEnable(GLES20.GL_BLEND);
    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

    // Disable depth writing to prevent z-fighting, but keep depth testing
    GLES20.glDepthMask(false);

    // Draw the purple line as triangles (quads)
    GLES20.glUseProgram(lineProgram);

    GLES20.glEnableVertexAttribArray(linePositionAttribute);
    GLES20.glVertexAttribPointer(
        linePositionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, lineVertexBuffer);

    GLES20.glUniformMatrix4fv(lineMvpMatrixUniform, 1, false, mvpMatrix, 0);
    GLES20.glUniform4fv(lineColorUniform, 1, lineColor, 0);

    GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, lineVertexCount);
    GLES20.glDisableVertexAttribArray(linePositionAttribute);

    // Draw the white arrows as triangles
    if (arrowVertexBuffer != null && arrowVertexCount > 0) {
      GLES20.glUseProgram(arrowProgram);

      GLES20.glEnableVertexAttribArray(arrowPositionAttribute);
      GLES20.glVertexAttribPointer(
          arrowPositionAttribute, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, arrowVertexBuffer);

      GLES20.glUniformMatrix4fv(arrowMvpMatrixUniform, 1, false, mvpMatrix, 0);
      GLES20.glUniform4fv(arrowColorUniform, 1, arrowColor, 0);

      GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, arrowVertexCount);
      GLES20.glDisableVertexAttribArray(arrowPositionAttribute);
    }

    // Re-enable depth writing
    GLES20.glDepthMask(true);

    // Disable blending
    GLES20.glDisable(GLES20.GL_BLEND);

    ShaderUtil.checkGLError(TAG, "Draw");
  }
}
