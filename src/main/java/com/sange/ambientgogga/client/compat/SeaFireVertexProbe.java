package com.sange.ambientgogga.client.compat;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryStack;

/** Runs a private copy of an existing vertex stage. It never relinks or edits the shader pack's program. */
public final class SeaFireVertexProbe implements AutoCloseable {
    private final int source;
    private int program, vao, input, feedback, output;
    private long fence;
    private final List<Attribute> attributes = new ArrayList<>();
    private final List<UniformCopy> uniforms = new ArrayList<>();
    private int stride;
    private final int points;
    private final ByteBuffer vertices;
    private final FloatBuffer results;
    private final Matrix4f inverse = new Matrix4f();
    private final float[] heights;

    public SeaFireVertexProbe(int source, int points) {
        this.source = source;
        this.points = points;
        this.results = BufferUtils.createFloatBuffer(points * 8);
        this.heights = new float[points];
        try (var stack = MemoryStack.stackPush()) {
            program = GL20.glCreateProgram();
            var count = stack.mallocInt(1);
            var attached = stack.mallocInt(8);
            GL20.glGetAttachedShaders(source, count, attached);
            boolean vertex = false;
            for (int i = 0; i < count.get(0); i++) {
                int shader = attached.get(i);
                if (GL20.glGetShaderi(shader, GL20.GL_SHADER_TYPE) == GL20.GL_VERTEX_SHADER) {
                    GL20.glAttachShader(program, shader);
                    vertex = true;
                }
            }
            if (!vertex) throw new IllegalStateException("Terrain vertex stage unavailable");
            GL30.glTransformFeedbackVaryings(program, new String[]{"gl_Position"}, GL30.GL_INTERLEAVED_ATTRIBS);
            GL20.glLinkProgram(program);
            if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0)
                throw new IllegalStateException("Vertex probe link failed: " + GL20.glGetProgramInfoLog(program));
            // New Iris layouts using uniform/storage blocks need a separate adapter.
            if (GL31.glGetProgrami(program, GL31.GL_ACTIVE_UNIFORM_BLOCKS) != 0)
                throw new IllegalStateException("Uniform-block terrain layout is unsupported");
            if (GL.getCapabilities().OpenGL43) {
                for (int i = 0; i < GL43.glGetProgramInterfacei(program, GL43.GL_SHADER_STORAGE_BLOCK, GL43.GL_ACTIVE_RESOURCES); i++) {
                    var property = stack.ints(GL43.GL_REFERENCED_BY_VERTEX_SHADER);
                    var referenced = stack.mallocInt(1);
                    GL43.glGetProgramResourceiv(program, GL43.GL_SHADER_STORAGE_BLOCK, i, property, null, referenced);
                    if (referenced.get(0) != 0)
                        throw new IllegalStateException("Active vertex storage block is unsupported: "
                                + GL43.glGetProgramResourceName(program, GL43.GL_SHADER_STORAGE_BLOCK, i));
                }
            }
            var size = stack.mallocInt(1);
            var type = stack.mallocInt(1);
            boolean position = false, material = false;
            for (int i = 0; i < GL20.glGetProgrami(program, GL20.GL_ACTIVE_ATTRIBUTES); i++) {
                String name = GL20.glGetActiveAttrib(program, i, size, type);
                if (name.startsWith("gl_")) continue;
                String semantic = name.startsWith("iris_") ? name.substring(5) : name;
                int kind = attributeKind(semantic);
                position |= kind == 0; material |= kind == 1;
                int components = components(type.get(0));
                if (kind < 0 || size.get(0) != 1 || components == 0)
                    throw new IllegalStateException("Unsupported terrain attribute: " + name);
                attributes.add(new Attribute(GL20.glGetAttribLocation(program, name), kind, stride,
                        components, isInteger(type.get(0))));
                stride += components * 4;
            }
            if (!position || !material) throw new IllegalStateException("Terrain shader does not expose vegetation tags");
            for (int i = 0; i < GL20.glGetProgrami(program, GL20.GL_ACTIVE_UNIFORMS); i++) {
                String name = GL20.glGetActiveUniform(program, i, size, type);
                int from = GL20.glGetUniformLocation(source, name);
                if (from < 0) throw new IllegalStateException("Missing source uniform: " + name);
                uniforms.add(new UniformCopy(from, GL20.glGetUniformLocation(program, name), type.get(0), size.get(0), name));
            }
            vao = GL30.glGenVertexArrays(); input = GL15.glGenBuffers();
            feedback = GL40.glGenTransformFeedbacks(); output = GL15.glGenBuffers();
        } catch (RuntimeException error) { close(); throw error; }
        vertices = BufferUtils.createByteBuffer(points * 2 * stride);
    }

    /** The caller owns GL state restoration, including the source program's sampler bindings. */
    public void submit(float[] positions, int material, Matrix4f inverseProjectionView) {
        if (fence != 0) throw new IllegalStateException("Probe already pending");
        inverse.set(inverseProjectionView);
        GL20.glUseProgram(program);
        for (var uniform : uniforms) uniform.copy(source);
        vertices.clear();
        for (int i = 0; i < points; i++) {
            for (int pair = 0; pair < 2; pair++) {
                for (var attr : attributes) {
                    for (int c = 0; c < attr.components; c++) {
                        float value = attributeValue(attr.kind, c, positions, i * 3, pair == 0 ? material : -1);
                        if (attr.integer) vertices.putInt((int) value); else vertices.putFloat(value);
                    }
                }
            }
        }
        vertices.flip();
        GL30.glBindVertexArray(vao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, input);
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STREAM_DRAW);
        for (var attr : attributes) {
            GL20.glEnableVertexAttribArray(attr.location);
            if (attr.integer) GL30.glVertexAttribIPointer(attr.location, attr.components, GL11.GL_INT, stride, attr.offset);
            else GL20.glVertexAttribPointer(attr.location, attr.components, GL11.GL_FLOAT, false, stride, attr.offset);
        }
        GL40.glBindTransformFeedback(GL40.GL_TRANSFORM_FEEDBACK, feedback);
        GL15.glBindBuffer(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, output);
        GL15.glBufferData(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, (long) points * 8 * 4, GL15.GL_STREAM_READ);
        GL30.glBindBufferBase(GL30.GL_TRANSFORM_FEEDBACK_BUFFER, 0, output);
        GL11.glEnable(GL30.GL_RASTERIZER_DISCARD);
        GL30.glBeginTransformFeedback(GL11.GL_POINTS);
        try { GL11.glDrawArrays(GL11.GL_POINTS, 0, points * 2); }
        finally { GL30.glEndTransformFeedback(); }
        fence = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
    }

    /** Zero-time fence polling avoids waiting for the GPU on the render thread. */
    public float[] poll(float strength, float limit) {
        if (fence == 0) return null;
        int status = GL32.glClientWaitSync(fence, 0, 0);
        if (status == GL32.GL_TIMEOUT_EXPIRED) return null;
        if (status == GL32.GL_WAIT_FAILED) throw new IllegalStateException("Vertex probe fence failed");
        GL32.glDeleteSync(fence); fence = 0;
        int old = GL11.glGetInteger(GL31.GL_COPY_READ_BUFFER);
        GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, output);
        results.clear();
        try { GL15.glGetBufferSubData(GL31.GL_COPY_READ_BUFFER, 0, results); }
        finally { GL15.glBindBuffer(GL31.GL_COPY_READ_BUFFER, old); }
        var moving = new Vector4f();
        var still = new Vector4f();
        for (int i = 0; i < points; i++) {
            moving.set(results.get(i * 8), results.get(i * 8 + 1), results.get(i * 8 + 2), results.get(i * 8 + 3)).mul(inverse);
            still.set(results.get(i * 8 + 4), results.get(i * 8 + 5), results.get(i * 8 + 6), results.get(i * 8 + 7)).mul(inverse);
            float delta = moving.y / moving.w - still.y / still.w;
            if (!Float.isFinite(delta) || Math.abs(delta) > 4)
                throw new IllegalStateException("Terrain probe returned invalid displacement");
            // Many plant shaders only bend downward. Reflect that motion above water instead of hiding the particles.
            heights[i] = Math.min(limit, Math.abs(delta) * strength);
        }
        return heights;
    }

    public boolean pending() { return fence != 0; }
    public int source() { return source; }

    private static int attributeKind(String name) {
        return switch (name) {
            case "Position", "vaPosition" -> 0;
            case "mc_Entity", "Entity" -> 1;
            case "Color", "vaColor" -> 2;
            case "UV0", "vaUV0" -> 3;
            case "UV2", "vaUV2" -> 4;
            case "Normal", "vaNormal" -> 5;
            case "mc_midTexCoord" -> 6;
            case "at_tangent" -> 7;
            case "at_midBlock" -> 8;
            default -> -1;
        };
    }

    private static float attributeValue(int kind, int c, float[] pos, int offset, int material) {
        return switch (kind) {
            case 0 -> c < 3 ? pos[offset + c] : 1;
            case 1 -> c == 0 ? material : c == 1 ? -1 : 0;
            case 2 -> 1;
            case 3 -> c == 0 ? 0.5F : 0.25F;
            case 4 -> 240;
            case 5 -> c == 1 ? 1 : 0;
            case 6 -> 0.5F;
            case 7 -> c == 0 || c == 3 ? 1 : 0;
            case 8 -> c == 1 ? -32 : 0;
            default -> 0;
        };
    }

    private static int components(int type) {
        return switch (type) {
            case GL11.GL_FLOAT, GL11.GL_INT -> 1;
            case GL20.GL_FLOAT_VEC2, GL20.GL_INT_VEC2 -> 2;
            case GL20.GL_FLOAT_VEC3, GL20.GL_INT_VEC3 -> 3;
            case GL20.GL_FLOAT_VEC4, GL20.GL_INT_VEC4 -> 4;
            default -> 0;
        };
    }

    private static boolean isInteger(int type) {
        return type == GL11.GL_INT || type == GL20.GL_INT_VEC2 || type == GL20.GL_INT_VEC3 || type == GL20.GL_INT_VEC4;
    }

    private static final class UniformCopy {
        final int from, to, type, size;
        final String name;
        final FloatBuffer floats;
        final java.nio.IntBuffer ints;
        UniformCopy(int from, int to, int type, int size, String name) {
            this.from = from; this.to = to; this.type = type; this.size = size; this.name = name;
            // Shader-pack uniform arrays are uncommon; refuse rather than copying only element zero.
            if (size != 1) throw new IllegalStateException("Uniform arrays are unsupported: " + name);
            int count = switch (type) {
                case GL20.GL_FLOAT_MAT2 -> 4;
                case GL20.GL_FLOAT_MAT3 -> 9;
                case GL20.GL_FLOAT_MAT4 -> 16;
                default -> Math.max(1, components(type));
            };
            floats = BufferUtils.createFloatBuffer(count);
            ints = BufferUtils.createIntBuffer(count);
            if (components(type) == 0 && type != GL20.GL_FLOAT_MAT2 && type != GL20.GL_FLOAT_MAT3
                    && type != GL20.GL_FLOAT_MAT4 && type != GL20.GL_BOOL
                    && type != GL20.GL_SAMPLER_2D && type != GL20.GL_SAMPLER_3D
                    && type != GL20.GL_SAMPLER_2D_SHADOW && type != GL20.GL_SAMPLER_CUBE)
                throw new IllegalStateException("Unsupported vertex uniform: " + name + " (" + type + ")");
        }
        void copy(int source) {
            // Probe positions are camera-relative, independent of the last rendered chunk.
            if (name.equals("iris_ChunkOffset") || name.equals("ChunkOffset")) { GL20.glUniform3f(to, 0, 0, 0); return; }
            switch (type) {
                case GL11.GL_FLOAT, GL20.GL_FLOAT_VEC2, GL20.GL_FLOAT_VEC3, GL20.GL_FLOAT_VEC4,
                        GL20.GL_FLOAT_MAT2, GL20.GL_FLOAT_MAT3, GL20.GL_FLOAT_MAT4 -> {
                    GL20.glGetUniformfv(source, from, floats);
                    switch (type) {
                        case GL11.GL_FLOAT -> GL20.glUniform1fv(to, floats);
                        case GL20.GL_FLOAT_VEC2 -> GL20.glUniform2fv(to, floats);
                        case GL20.GL_FLOAT_VEC3 -> GL20.glUniform3fv(to, floats);
                        case GL20.GL_FLOAT_VEC4 -> GL20.glUniform4fv(to, floats);
                        case GL20.GL_FLOAT_MAT2 -> GL20.glUniformMatrix2fv(to, false, floats);
                        case GL20.GL_FLOAT_MAT3 -> GL20.glUniformMatrix3fv(to, false, floats);
                        default -> GL20.glUniformMatrix4fv(to, false, floats);
                    }
                }
                default -> {
                    GL20.glGetUniformiv(source, from, ints);
                    switch (type) {
                        case GL20.GL_INT_VEC2 -> GL20.glUniform2iv(to, ints);
                        case GL20.GL_INT_VEC3 -> GL20.glUniform3iv(to, ints);
                        case GL20.GL_INT_VEC4 -> GL20.glUniform4iv(to, ints);
                        default -> GL20.glUniform1iv(to, ints);
                    }
                }
            }
        }
    }

    private record Attribute(int location, int kind, int offset, int components, boolean integer) { }

    @Override public void close() {
        if (fence != 0) GL32.glDeleteSync(fence);
        if (program != 0) GL20.glDeleteProgram(program);
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        if (input != 0) GL15.glDeleteBuffers(input);
        if (output != 0) GL15.glDeleteBuffers(output);
        if (feedback != 0) GL40.glDeleteTransformFeedbacks(feedback);
        fence = 0; program = vao = input = output = feedback = 0;
    }
}
