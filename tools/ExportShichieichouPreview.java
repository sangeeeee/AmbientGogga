import com.sange.ambientgogga.client.model.ShichieichouAnimation;
import com.sange.ambientgogga.client.model.ShichieichouPose;
import com.sange.ambientgogga.client.model.ShichieichouAnatomy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Exports the actual Java deformation for the standalone visual inspection page. */
public class ExportShichieichouPreview {
    public static void main(String[] args) throws Exception {
        StringBuilder json = new StringBuilder("{\"rows\":").append(ShichieichouAnimation.ROWS)
                .append(",\"columns\":").append(ShichieichouAnimation.COLUMNS).append(",\"uv\":[");
        for (int row = 0; row < ShichieichouAnimation.ROWS; row++) {
            for (int column = 0; column < ShichieichouAnimation.COLUMNS; column++) {
                if (row != 0 || column != 0) json.append(',');
                float u = ShichieichouAnimation.U_MIN + (ShichieichouAnimation.U_MAX - ShichieichouAnimation.U_MIN)
                        * column / ShichieichouAnimation.SPAN_SEGMENTS;
                json.append(String.format(Locale.ROOT, "%.6f,%.6f", u, ShichieichouAnimation.textureV(row)));
            }
        }
        json.append("],\"anatomy\":[");
        float[] body = ShichieichouAnatomy.MESH.vertices();
        for (int i = 0; i < body.length; i++) {
            if (i != 0) json.append(',');
            json.append(String.format(Locale.ROOT, "%.6f", body[i]));
        }
        json.append("],\"colors\":[");
        int[] colors = ShichieichouAnatomy.MESH.colors();
        for (int i = 0; i < colors.length; i++) {
            if (i != 0) json.append(',');
            json.append(colors[i]);
        }
        json.append("],\"startAge\":180,\"strokeSpeed\":").append(ShichieichouAnimation.STROKE_SPEED)
                .append(",\"frames\":[");
        float[][] wings = new float[2][ShichieichouAnimation.ROWS * ShichieichouAnimation.COLUMNS * 3];
        ShichieichouPose pose = new ShichieichouPose();
        for (int tick = 0; tick < 180; tick++) pose.sample(wings, tick, 0, 0.3F, 0, 0, 0, 0, 0, 0.9F);
        for (int frame = 0; frame < 72; frame++) {
            if (frame != 0) json.append(',');
            json.append('[');
            float age = 180 + (float) (frame * 2 * Math.PI / ShichieichouAnimation.STROKE_SPEED / 72);
            pose.sample(wings, age, 0, 0.3F, 0, 0, 0, 0, 0, 0.9F);
            for (int side = 0; side < 2; side++) {
                float[] positions = wings[side];
                for (int i = 0; i < positions.length; i++) {
                    if (side != 0 || i != 0) json.append(',');
                    json.append(String.format(Locale.ROOT, "%.5f", positions[i]));
                }
            }
            json.append(']');
        }
        json.append("]}");
        Files.writeString(Path.of(args[0]), json);
    }
}
