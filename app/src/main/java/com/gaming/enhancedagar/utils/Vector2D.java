package com.gaming.enhancedagar.utils;

import android.graphics.PointF;

/**
 * Vector 2D para cálculos de movimiento y física
 */
public class Vector2D {
    public float x;
    public float y;

    // Constructores
    public Vector2D() {
        this(0, 0);
    }

    public Vector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(Vector2D other) {
        this(other.x, other.y);
    }

    public Vector2D(PointF point) {
        this(point.x, point.y);
    }

    // Operaciones vectoriales
    public Vector2D add(Vector2D other) {
        return new Vector2D(x + other.x, y + other.y);
    }

    public Vector2D subtract(Vector2D other) {
        return new Vector2D(x - other.x, y - other.y);
    }

    public Vector2D multiply(float scalar) {
        return new Vector2D(x * scalar, y * scalar);
    }

    public Vector2D divide(float scalar) {
        if (scalar != 0) {
            return new Vector2D(x / scalar, y / scalar);
        }
        return new Vector2D(0, 0);
    }

    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }

    public float magnitudeSquared() {
        return x * x + y * y;
    }

    public Vector2D normalize() {
        float mag = magnitude();
        if (mag > 0) {
            return divide(mag);
        }
        return new Vector2D(0, 0);
    }

    public Vector2D setMagnitude(float magnitude) {
        Vector2D normalized = normalize();
        return normalized.multiply(magnitude);
    }

    public float distance(Vector2D other) {
        return (float) Math.sqrt(Math.pow(other.x - x, 2) + Math.pow(other.y - y, 2));
    }

    public float distanceSquared(Vector2D other) {
        float dx = other.x - x;
        float dy = other.y - y;
        return dx * dx + dy * dy;
    }

    public float dot(Vector2D other) {
        return x * other.x + y * other.y;
    }

    public float angle(Vector2D other) {
        float dot = dot(other);
        float mag1 = magnitude();
        float mag2 = other.magnitude();
        
        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }
        
        return (float) Math.acos(dot / (mag1 * mag2));
    }

    public Vector2D rotate(float angle) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        return new Vector2D(x * cos - y * sin, x * sin + y * cos);
    }

    public Vector2D limit(float max) {
        if (magnitudeSquared() > max * max) {
            return setMagnitude(max);
        }
        return this;
    }

    public static Vector2D zero() {
        return new Vector2D(0, 0);
    }

    public static Vector2D random() {
        return new Vector2D((float) Math.random() * 2 - 1, (float) Math.random() * 2 - 1);
    }

    public static Vector2D fromAngle(float angle) {
        return new Vector2D((float) Math.cos(angle), (float) Math.sin(angle));
    }

    // Métodos para compatibilidad con Android
    public PointF toPointF() {
        return new PointF(x, y);
    }

    // Override methods
    @Override
    public Vector2D clone() {
        return new Vector2D(x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Vector2D vector2D = (Vector2D) obj;
        return Float.compare(vector2D.x, x) == 0 &&
               Float.compare(vector2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(x) ^ Float.hashCode(y);
    }

    @Override
    public String toString() {
        return "Vector2D{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}