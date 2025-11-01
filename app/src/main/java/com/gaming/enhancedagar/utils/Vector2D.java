package com.gaming.enhancedagar.utils;

/**
 * Clase de vectores 2D para cálculos matemáticos y físicos
 * Optimizada para operaciones frecuentes en el juego
 */
public class Vector2D {
    public float x, y;
    
    public Vector2D() {
        this(0, 0);
    }
    
    public Vector2D(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public Vector2D set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    public Vector2D add(Vector2D v) {
        x += v.x;
        y += v.y;
        return this;
    }
    
    public Vector2D sub(Vector2D v) {
        x -= v.x;
        y -= v.y;
        return this;
    }
    
    public Vector2D mul(float scalar) {
        x *= scalar;
        y *= scalar;
        return this;
    }
    
    public Vector2D div(float scalar) {
        if (scalar != 0) {
            x /= scalar;
            y /= scalar;
        }
        return this;
    }
    
    public float magnitude() {
        return (float) Math.sqrt(x * x + y * y);
    }
    
    public Vector2D normalize() {
        float mag = magnitude();
        if (mag > 0) {
            x /= mag;
            y /= mag;
        }
        return this;
    }
    
    public float dot(Vector2D v) {
        return x * v.x + y * v.y;
    }
    
    public float cross(Vector2D v) {
        return x * v.y - y * v.x;
    }
    
    public float distance(Vector2D v) {
        float dx = x - v.x;
        float dy = y - v.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    public float distanceSq(Vector2D v) {
        float dx = x - v.x;
        float dy = y - v.y;
        return dx * dx + dy * dy;
    }
    
    public Vector2D copy() {
        return new Vector2D(x, y);
    }
    
    public Vector2D limit(float max) {
        float mag = magnitude();
        if (mag > max) {
            normalize();
            mul(max);
        }
        return this;
    }
    
    public static Vector2D fromAngle(float angle) {
        return new Vector2D((float) Math.cos(angle), (float) Math.sin(angle));
    }
    
    public float heading() {
        return (float) Math.atan2(y, x);
    }
    
    @Override
    public String toString() {
        return "Vector2D(" + x + ", " + y + ")";
    }
}
