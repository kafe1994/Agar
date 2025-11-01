package com.gaming.enhancedagar.entities;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import java.util.List;
import java.util.ArrayList;

/**
 * Clase base para todas las entidades del juego Enhanced Agar
 * Maneja propiedades comunes como posición, tamaño, velocidad y estado
 * Incluye sistema de colisiones, gestión de memoria y métodos de utilidad
 */
public abstract class Entity {
    protected static final String TAG = "Entity";
    
    // Propiedades básicas de posición y movimiento
    protected float x, y;
    protected float width, height;
    protected float velocityX, velocityY;
    protected float maxSpeed;
    protected float acceleration;
    protected boolean isAlive;
    protected boolean isActive;
    protected boolean isVisible;
    
    // Propiedades físicas
    protected float mass;
    
    // Propiedades visuales
    protected int color;
    protected Paint paint;
    protected RectF bounds;
    
    // Sistema de eventos
    protected List<EntityListener> listeners;
    
    // Gestión de entidades activas
    private static List<Entity> activeEntities = new ArrayList<>();
    private static long nextId = 1;
    protected long id;
    
    /**
     * Constructor básico para entidades
     */
    public Entity(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.velocityX = 0;
        this.velocityY = 0;
        this.maxSpeed = 5.0f;
        this.acceleration = 0.5f;
        this.isAlive = true;
        this.isActive = true;
        this.isVisible = true;
        this.color = 0xFF808080; // Color gris por defecto
        this.mass = width * height; // Masa proporcional al área
        this.bounds = new RectF();
        this.listeners = new ArrayList<>();
        this.id = nextId++;
        
        initializePaint();
        updateBounds();
        
        // Registrar en lista de entidades activas
        activeEntities.add(this);
    }
    
    /**
     * Inicializa el objeto Paint para renderizado
     */
    protected void initializePaint() {
        paint = new Paint();
        paint.setColor(color);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
    }
    
    /**
     * Actualiza los límites de la entidad
     */
    protected void updateBounds() {
        bounds.set(x - width/2, y - height/2, x + width/2, y + height/2);
    }
    
    /**
     * Actualiza la lógica de la entidad (método abstracto)
     * @param deltaTime tiempo transcurrido en milisegundos
     */
    public abstract void update(float deltaTime);
    
    /**
     * Renderiza la entidad en el canvas (método abstracto)
     * @param canvas Canvas donde renderizar
     */
    public abstract void draw(Canvas canvas);
    
    /**
     * Actualiza la posición básica de la entidad
     * @param deltaTime tiempo transcurrido
     */
    protected void updateBasicPosition(float deltaTime) {
        if (!isActive) return;
        
        // Actualizar posición basada en velocidad
        x += velocityX * deltaTime;
        y += velocityY * deltaTime;
        
        updateBounds();
    }
    
    /**
     * Verifica si esta entidad colisiona con otra (detección rectangular)
     * @param other otra entidad
     * @return true si colisionan
     */
    public boolean collidesWith(Entity other) {
        if (!isActive || !other.isActive) return false;
        return RectF.intersects(this.bounds, other.bounds);
    }
    
    /**
     * Verifica colisión usando detección circular (más precisa para entidades redondas)
     * @param other otra entidad
     * @return true si colisionan
     */
    public boolean collidesWithCircle(Entity other) {
        if (!isActive || !other.isActive) return false;
        
        float radius1 = Math.max(width, height) / 2;
        float radius2 = Math.max(other.width, other.height) / 2;
        
        float dx = x - other.x;
        float dy = y - other.y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        return distance < (radius1 + radius2);
    }
    
    /**
     * Verifica colisión con rectángulo
     * @param left límite izquierdo
     * @param top límite superior
     * @param right límite derecho
     * @param bottom límite inferior
     * @return true si colisionan
     */
    public boolean collidesWithRect(float left, float top, float right, float bottom) {
        if (!isActive) return false;
        
        RectF rect = new RectF(left, top, right, bottom);
        return RectF.intersects(this.bounds, rect);
    }
    
    /**
     * Maneja la colisión con otra entidad
     * @param other entidad con la que colisiona
     */
    public void onCollision(Entity other) {
        notifyListeners("collision", other);
    }
    
    /**
     * Obtiene la distancia a otra entidad
     * @param other otra entidad
     * @return distancia euclidiana
     */
    public float getDistanceTo(Entity other) {
        float dx = other.x - this.x;
        float dy = other.y - this.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Normaliza la velocidad para mantener la velocidad máxima
     */
    protected void normalizeVelocity() {
        float speed = (float) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
        if (speed > maxSpeed) {
            velocityX = (velocityX / speed) * maxSpeed;
            velocityY = (velocityY / speed) * maxSpeed;
        }
    }
    
    // MÉTODOS DE UTILIDAD
    
    /**
     * Calcula el ángulo hacia otra entidad
     * @param other otra entidad
     * @return ángulo en radianes
     */
    public float getAngleTo(Entity other) {
        return (float) Math.atan2(other.y - y, other.x - x);
    }
    
    /**
     * Mueve la entidad hacia un objetivo
     * @param targetX coordenada X objetivo
     * @param targetY coordenada Y objetivo
     * @param speed velocidad de movimiento
     */
    public void moveTowards(float targetX, float targetY, float speed) {
        float dx = targetX - x;
        float dy = targetY - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (distance > 0.1f) {
            this.velocityX = (dx / distance) * speed;
            this.velocityY = (dy / distance) * speed;
        }
    }
    
    /**
     * Aplica fuerza a la entidad
     * @param forceX fuerza en X
     * @param forceY fuerza en Y
     */
    public void applyForce(float forceX, float forceY) {
        if (!isActive) return;
        
        velocityX += (forceX / mass) * acceleration;
        velocityY += (forceY / mass) * acceleration;
        normalizeVelocity();
    }
    
    /**
     * Verifica si la entidad está dentro de los límites de pantalla
     * @param screenWidth ancho de pantalla
     * @param screenHeight alto de pantalla
     * @return true si está dentro de los límites
     */
    public boolean isInBounds(float screenWidth, float screenHeight) {
        return x >= 0 && x <= screenWidth && y >= 0 && y <= screenHeight;
    }
    
    /**
     * Hace que la entidad rebote en los límites de pantalla
     * @param screenWidth ancho de pantalla
     * @param screenHeight alto de pantalla
     */
    public void bounceInBounds(float screenWidth, float screenHeight) {
        float radius = Math.max(width, height) / 2;
        
        if (x - radius < 0) {
            x = radius;
            velocityX = Math.abs(velocityX);
        } else if (x + radius > screenWidth) {
            x = screenWidth - radius;
            velocityX = -Math.abs(velocityX);
        }
        
        if (y - radius < 0) {
            y = radius;
            velocityY = Math.abs(velocityY);
        } else if (y + radius > screenHeight) {
            y = screenHeight - radius;
            velocityY = -Math.abs(velocityY);
        }
    }
    
    /**
     * Aplica fricción a la velocidad
     * @param frictionFactor factor de fricción (0.0 - 1.0)
     */
    public void applyFriction(float frictionFactor) {
        velocityX *= frictionFactor;
        velocityY *= frictionFactor;
    }
    
    /**
     * Cambia el tamaño de la entidad y actualiza la masa
     * @param newWidth nuevo ancho
     * @param newHeight nuevo alto
     */
    public void setSize(float newWidth, float newHeight) {
        this.width = Math.max(newWidth, 1.0f);
        this.height = Math.max(newHeight, 1.0f);
        this.mass = this.width * this.height;
        updateBounds();
    }
    
    /**
     * Aumenta el tamaño (útil para comer comida)
     * @param amountToAdd cantidad a añadir al tamaño
     */
    public void increaseSize(float amountToAdd) {
        setSize(width + amountToAdd, height + amountToAdd);
    }
    
    /**
     * Obtiene el factor de crecimiento basado en la masa
     * @return factor de crecimiento
     */
    public float getGrowthFactor() {
        return mass > 0 ? mass / (width * height) : 1.0f;
    }
    
    /**
     * Añade un listener para eventos de la entidad
     * @param listener listener a añadir
     */
    public void addListener(EntityListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remueve un listener
     * @param listener listener a remover
     */
    public void removeListener(EntityListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifica a todos los listeners sobre un evento
     * @param eventType tipo de evento
     * @param data datos del evento
     */
    protected void notifyListeners(String eventType, Object data) {
        for (EntityListener listener : listeners) {
            try {
                listener.onEntityEvent(this, eventType, data);
            } catch (Exception e) {
                Log.e(TAG, "Error notificando listener", e);
            }
        }
    }
    
    /**
     * Mueve la entidad a una posición específica
     * @param newX nueva coordenada X
     * @param newY nueva coordenada Y
     */
    public void setPosition(float newX, float newY) {
        float oldX = this.x;
        float oldY = this.y;
        
        this.x = newX;
        this.y = newY;
        updateBounds();
        
        notifyListeners("positionChanged", new float[]{oldX, oldY, newX, newY});
    }
    
    /**
     * Establece la velocidad de la entidad
     * @param velX velocidad en X
     * @param velY velocidad en Y
     */
    public void setVelocity(float velX, float velY) {
        this.velocityX = velX;
        this.velocityY = velY;
        normalizeVelocity();
    }
    
    /**
     * Mata/desactiva la entidad
     */
    public void die() {
        isAlive = false;
        isActive = false;
        notifyListeners("death", null);
    }
    
    /**
     * Revive la entidad
     */
    public void revive() {
        isAlive = true;
        isActive = true;
        notifyListeners("revive", null);
    }
    
    /**
     * Activa o desactiva la entidad
     * @param active estado activo
     */
    public void setActive(boolean active) {
        boolean wasActive = this.isActive;
        this.isActive = active;
        
        if (wasActive != active) {
            notifyListeners("activeStateChanged", active);
        }
        
        if (!active) {
            removeFromActiveList();
        }
    }
    
    /**
     * Hace visible o invisible la entidad
     * @param visible estado visible
     */
    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }
    
    /**
     * Libera recursos y remove de listas activas
     */
    public void dispose() {
        setActive(false);
        removeFromActiveList();
        listeners.clear();
    }
    
    /**
     * Remueve de la lista de entidades activas
     */
    private void removeFromActiveList() {
        activeEntities.remove(this);
    }
    
    /**
     * Limpia todas las entidades inactivas
     */
    public static void cleanupInactiveEntities() {
        activeEntities.removeIf(entity -> !entity.isActive);
    }
    
    /**
     * Obtiene todas las entidades activas
     * @return lista de entidades activas
     */
    public static List<Entity> getActiveEntities() {
        return new ArrayList<>(activeEntities);
    }
    
    /**
     * Limpia todas las entidades (usar al cerrar el juego)
     */
    public static void disposeAllEntities() {
        for (Entity entity : activeEntities) {
            entity.dispose();
        }
        activeEntities.clear();
    }
    
    // Getters y setters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getVelocityX() { return velocityX; }
    public float getVelocityY() { return velocityY; }
    public float getMaxSpeed() { return maxSpeed; }
    public float getAcceleration() { return acceleration; }
    public float getMass() { return mass; }
    public boolean isAlive() { return isAlive; }
    public boolean isActive() { return isActive; }
    public boolean isVisible() { return isVisible; }
    public int getColor() { return color; }
    public Paint getPaint() { return paint; }
    public RectF getBounds() { return bounds; }
    public long getId() { return id; }
    
    public void setX(float x) { this.x = x; updateBounds(); }
    public void setY(float y) { this.y = y; updateBounds(); }
    public void setWidth(float width) { 
        this.width = width; 
        this.mass = this.width * this.height;
        updateBounds(); 
    }
    public void setHeight(float height) { 
        this.height = height; 
        this.mass = this.width * this.height;
        updateBounds(); 
    }
    public void setMaxSpeed(float maxSpeed) { this.maxSpeed = maxSpeed; }
    public void setAcceleration(float acceleration) { this.acceleration = acceleration; }
    public void setColor(int color) { this.color = color; paint.setColor(color); }
    
    /**
     * Método de dibujo base para entidades circulares
     * Puede ser usado por clases derivadas
     */
    protected void drawCircle(Canvas canvas) {
        if (!isVisible) return;
        
        float radius = Math.max(width, height) / 2;
        canvas.drawCircle(x, y, radius, paint);
    }
    
    /**
     * Interface para listeners de eventos de entidad
     */
    public interface EntityListener {
        void onEntityEvent(Entity entity, String eventType, Object data);
    }
}