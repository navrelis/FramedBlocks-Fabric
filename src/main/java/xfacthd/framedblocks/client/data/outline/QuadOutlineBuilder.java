package xfacthd.framedblocks.client.data.outline;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Derives the outline lines of a polyhedron from the planar quads which make up its surface.
 * Edges shared by two quads of the same plane (or by two coplanar quads facing opposite ways) are dropped, collinear
 * segments are merged and segments which lie completely within a longer segment are removed.
 */
final class QuadOutlineBuilder
{
    private static final float QUANTIZATION = 64F;
    private static final float EPSILON = 1.0E-5F;
    private static final float PARALLEL_EPSILON = 1.0E-4F;

    /**
     * @param quads The quads, given as 12 floats each (four consecutive x, y, z positions)
     * @return The outline as a packed array of lines (six floats per line: x1, y1, z1, x2, y2, z2)
     */
    static float[] build(List<float[]> quads)
    {
        // Collect all unique quads with their vertex positions and quad normal (assumes all quads are planar)
        Set<Quad> uniqueQuads = new HashSet<>();
        for (float[] quad : quads)
        {
            if (quad.length != 12)
            {
                throw new IllegalArgumentException("Invalid quad data");
            }
            Quad parsed = Quad.of(quad);
            if (parsed != null)
            {
                uniqueQuads.add(parsed);
            }
        }

        // Collect all edges grouped by the quad normal they originate from
        Map<Integer, Map<Line, Integer>> linesByNormal = new HashMap<>();
        for (Quad quad : uniqueQuads)
        {
            Map<Line, Integer> lines = linesByNormal.computeIfAbsent(quad.normal, $ -> new HashMap<>());
            appendEdge(quad.v0, quad.v1, lines);
            appendEdge(quad.v2, quad.v3, lines);
            appendEdge(quad.v0, quad.v3, lines);
            appendEdge(quad.v1, quad.v2, lines);
        }

        // Collect all unique lines, eliminating identical lines which originate from multiple quads of identical or opposing normals
        Set<Line> uniqueLines = new HashSet<>();
        for (Map.Entry<Integer, Map<Line, Integer>> entry : linesByNormal.entrySet())
        {
            int normal = entry.getKey();
            Map<Line, Integer> oppEntry = linesByNormal.getOrDefault(oppositeNormal(normal), Map.of());

            // Merge parallel line segments which share a point into one line
            Map<Line, Integer> lines = entry.getValue();
            tryMergeSegments(
                    lines.keySet(),
                    line -> lines.merge(line, 1, Integer::sum),
                    line -> lines.computeIfPresent(line, (key, value) -> value > 1 ? value - 1 : null),
                    QuadOutlineBuilder::checkNonOverlapping
            );

            // Collect lines which appear exactly once on this normal and are not present on the opposing normal
            Set<Line> uniqueLinesOnNormal = new HashSet<>();
            for (Map.Entry<Line, Integer> line : lines.entrySet())
            {
                if (line.getValue() == 1 && !oppEntry.containsKey(line.getKey()))
                {
                    uniqueLinesOnNormal.add(line.getKey());
                }
            }
            // "XOR" partially overlapping line segments
            tryMergeSegments(uniqueLinesOnNormal, uniqueLinesOnNormal::add, uniqueLinesOnNormal::remove, QuadOutlineBuilder::checkOneContainsTwo);
            uniqueLines.addAll(uniqueLinesOnNormal);
        }

        // Merge parallel line segments which share a point into one line
        tryMergeSegments(uniqueLines, uniqueLines::add, uniqueLines::remove, QuadOutlineBuilder::checkNonOverlapping);

        // Remove lines which overlap another longer line
        removeOverlappingSegments(uniqueLines);

        // Pack unique lines into float array
        float[] packedLines = new float[uniqueLines.size() * 6];
        int idx = 0;
        for (Line line : uniqueLines)
        {
            packedLines[idx    ] = line.x1;
            packedLines[idx + 1] = line.y1;
            packedLines[idx + 2] = line.z1;
            packedLines[idx + 3] = line.x2;
            packedLines[idx + 4] = line.y2;
            packedLines[idx + 5] = line.z2;
            idx += 6;
        }
        return packedLines;
    }

    private static void appendEdge(Vector3f v1, Vector3f v2, Map<Line, Integer> lines)
    {
        Line line = new Line(v1.x, v1.y, v1.z, v2.x, v2.y, v2.z);
        if (line.isNonZero())
        {
            lines.merge(line, 1, Integer::sum);
        }
    }

    private static void tryMergeSegments(Set<Line> lines, Consumer<Line> adder, Consumer<Line> remover, OverlapCheck check)
    {
        boolean merged;
        do
        {
            merged = false;
            outer:
            for (Line lineOne : new ArrayList<>(lines))
            {
                for (Line lineTwo : new ArrayList<>(lines))
                {
                    if (lineOne == lineTwo) continue;

                    int shared = lineOne.getSharedPointWith(lineTwo);
                    if (shared == -1) continue;

                    if (!lineOne.isParallelTo(lineTwo)) continue;
                    if (!check.test(lineOne, lineTwo, shared)) continue;

                    remover.accept(lineOne);
                    remover.accept(lineTwo);
                    adder.accept(lineOne.merge(lineTwo, shared));
                    merged = true;
                    break outer;
                }
            }
        }
        while (merged);
    }

    private static void removeOverlappingSegments(Set<Line> lines)
    {
        boolean removed;
        do
        {
            removed = false;
            outer:
            for (Line lineOne : new ArrayList<>(lines))
            {
                for (Line lineTwo : new ArrayList<>(lines))
                {
                    if (lineOne == lineTwo) continue;
                    if (!lineOne.isParallelTo(lineTwo)) continue;
                    if (!lineOne.contains(lineTwo.x1, lineTwo.y1, lineTwo.z1)) continue;
                    if (!lineOne.contains(lineTwo.x2, lineTwo.y2, lineTwo.z2)) continue;

                    lines.remove(lineTwo);
                    removed = true;
                    break outer;
                }
            }
        }
        while (removed);
    }

    private static boolean checkNonOverlapping(Line lineOne, Line lineTwo, int shared)
    {
        return !lineOne.containsPointOf(lineTwo, shared, false) && !lineTwo.containsPointOf(lineOne, shared, true);
    }

    private static boolean checkOneContainsTwo(Line lineOne, Line lineTwo, int shared)
    {
        return lineOne.containsPointOf(lineTwo, shared, false);
    }

    private static int packNormal(float x, float y, float z)
    {
        return (Math.round(x * 127F) & 0xFF) | ((Math.round(y * 127F) & 0xFF) << 8) | ((Math.round(z * 127F) & 0xFF) << 16);
    }

    private static int oppositeNormal(int packed)
    {
        int x = (byte) packed;
        int y = (byte) (packed >> 8);
        int z = (byte) (packed >> 16);
        return packNormal(-x / 127F, -y / 127F, -z / 127F);
    }

    private static float quantize(float value)
    {
        return Math.round(value * QUANTIZATION) / QUANTIZATION;
    }

    private record Quad(Vector3f v0, Vector3f v1, Vector3f v2, Vector3f v3, int normal)
    {
        static Quad of(float[] data)
        {
            Vector3f v0 = new Vector3f(quantize(data[0]), quantize(data[1]), quantize(data[2]));
            Vector3f v1 = new Vector3f(quantize(data[3]), quantize(data[4]), quantize(data[5]));
            Vector3f v2 = new Vector3f(quantize(data[6]), quantize(data[7]), quantize(data[8]));
            Vector3f v3 = new Vector3f(quantize(data[9]), quantize(data[10]), quantize(data[11]));

            // Newell's method, robust against degenerate (triangular) quads
            Vector3f[] verts = { v0, v1, v2, v3 };
            float nx = 0F;
            float ny = 0F;
            float nz = 0F;
            for (int i = 0; i < 4; i++)
            {
                Vector3f cur = verts[i];
                Vector3f next = verts[(i + 1) % 4];
                nx += (cur.y - next.y) * (cur.z + next.z);
                ny += (cur.z - next.z) * (cur.x + next.x);
                nz += (cur.x - next.x) * (cur.y + next.y);
            }
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < EPSILON)
            {
                // Zero-area quad, does not contribute to the surface
                return null;
            }
            return new Quad(v0, v1, v2, v3, packNormal(nx / len, ny / len, nz / len));
        }
    }

    private record Line(float x1, float y1, float z1, float x2, float y2, float z2)
    {
        Line
        {
            x1 = quantize(x1);
            y1 = quantize(y1);
            z1 = quantize(z1);
            x2 = quantize(x2);
            y2 = quantize(y2);
            z2 = quantize(z2);

            if (compare(x1, y1, z1, x2, y2, z2) > 0)
            {
                float tx = x1;
                float ty = y1;
                float tz = z1;
                x1 = x2;
                y1 = y2;
                z1 = z2;
                x2 = tx;
                y2 = ty;
                z2 = tz;
            }
        }

        private static int compare(float x1, float y1, float z1, float x2, float y2, float z2)
        {
            int compX = Float.compare(x1, x2);
            if (compX != 0) return compX;
            int compY = Float.compare(y1, y2);
            if (compY != 0) return compY;
            return Float.compare(z1, z2);
        }

        boolean isNonZero()
        {
            return x1 != x2 || y1 != y2 || z1 != z2;
        }

        int getSharedPointWith(Line other)
        {
            if (other.x1 == x1 && other.y1 == y1 && other.z1 == z1) return 0x11;
            if (other.x2 == x2 && other.y2 == y2 && other.z2 == z2) return 0x22;
            if (other.x1 == x2 && other.y1 == y2 && other.z1 == z2) return 0x21;
            if (other.x2 == x1 && other.y2 == y1 && other.z2 == z1) return 0x12;
            return -1;
        }

        boolean contains(float x, float y, float z)
        {
            Vector3f tmp = new Vector3f();
            float d1 = tmp.set(x1, y1, z1).distance(x, y, z);
            float d2 = tmp.set(x2, y2, z2).distance(x, y, z);
            float d3 = tmp.set(x1, y1, z1).distance(x2, y2, z2);
            return Math.abs(d1 + d2 - d3) < EPSILON;
        }

        boolean containsPointOf(Line other, int shared, boolean otherFirst)
        {
            if (otherFirst)
            {
                shared >>>= 4;
            }
            shared &= 0xF;
            return switch (shared)
            {
                case 0x1 -> contains(other.x2, other.y2, other.z2);
                case 0x2 -> contains(other.x1, other.y1, other.z1);
                default -> throw new IllegalArgumentException("Invalid shared point: " + shared);
            };
        }

        boolean isParallelTo(Line other)
        {
            Vector3f diffOne = new Vector3f(x2 - x1, y2 - y1, z2 - z1);
            Vector3f diffTwo = new Vector3f(other.x2 - other.x1, other.y2 - other.y1, other.z2 - other.z1);
            float lenOne = diffOne.length();
            float lenTwo = diffTwo.length();
            if (lenOne == 0F || lenTwo == 0F)
            {
                return false;
            }
            return diffOne.cross(diffTwo).length() / (lenOne * lenTwo) < PARALLEL_EPSILON;
        }

        Line merge(Line other, int shared)
        {
            return switch (shared)
            {
                case 0x11 -> new Line(x2, y2, z2, other.x2, other.y2, other.z2);
                case 0x22 -> new Line(x1, y1, z1, other.x1, other.y1, other.z1);
                case 0x21 -> new Line(x1, y1, z1, other.x2, other.y2, other.z2);
                case 0x12 -> new Line(x2, y2, z2, other.x1, other.y1, other.z1);
                default -> throw new IllegalArgumentException("Invalid shared point: " + shared);
            };
        }
    }

    private interface OverlapCheck
    {
        boolean test(Line lineOne, Line lineTwo, int shared);
    }

    private QuadOutlineBuilder() { }
}
