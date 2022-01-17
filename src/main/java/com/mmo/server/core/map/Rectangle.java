package com.mmo.server.core.map;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class Rectangle {

    private final Vertex topLeftVertex;
    private final Vertex topRightVertex;
    private final Vertex bottomLeftVertex;
    private final Vertex bottomRightVertex;

    @Builder
    private Rectangle(
            @NonNull Vertex topLeftVertex,
            @NonNull Vertex topRightVertex,
            @NonNull Vertex bottomLeftVertex,
            @NonNull Vertex bottomRightVertex) {

        this.topLeftVertex = topLeftVertex;
        this.topRightVertex = topRightVertex;
        this.bottomLeftVertex = bottomLeftVertex;
        this.bottomRightVertex = bottomRightVertex;

        validate();
    }

    public boolean intersects(Vertex vertex) {
        return intersects(vertex.getX(), vertex.getY());
    }

    public boolean intersects(float x, float y) {
        boolean validBottomLeft = x >= bottomLeftVertex.getX()
                && y >= bottomLeftVertex.getY();

        boolean validBottomRight = x <= bottomRightVertex.getX()
                && y >= bottomRightVertex.getY();

        boolean validTopLeft = x >= topLeftVertex.getX()
                && y <= topLeftVertex.getY();

        boolean validTopRight = x <= topRightVertex.getX()
                && y <= topRightVertex.getY();

        return validBottomLeft && validBottomRight && validTopLeft && validTopRight;
    }

    private void validate() throws InvalidRectangleException {
        Set<Vertex> vertices = new LinkedHashSet<>(
                List.of(bottomLeftVertex, bottomRightVertex, topLeftVertex, topRightVertex));

        Set<Float> distances = new LinkedHashSet<>();

        for (Vertex first : vertices) {
            for (Vertex second : vertices) {
                if (!first.equals(second)) {
                    distances.add(first.getDistance(second));
                }
            }
        }

        if (distances.size() > 3)
            throw new InvalidRectangleException("Distance more than 3");

        List<Float> sortedDistances = new ArrayList<>(distances);

        if (distances.size() == 2) {
            if (2 * sortedDistances.get(0) != sortedDistances.get(1)) {
                throw new InvalidRectangleException("Line seqments does not form a square");
            }

            return;
        }

        if (sortedDistances.get(0) + sortedDistances.get(1) != sortedDistances.get(2)) {
            throw new InvalidRectangleException("Distance of sides should satisfy pythagorean theorem");
        }
    }
}
