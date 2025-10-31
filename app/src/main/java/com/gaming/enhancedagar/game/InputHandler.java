package com.gaming.enhancedagar.game;

import android.view.MotionEvent;
import android.view.View;
import android.graphics.PointF;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador avanzado de inputs para el juego Enhanced Agar
 * Soporta touch, multi-touch, gestures, inputs virtuales y calibración automática
 */
public class InputHandler implements View.OnTouchListener {
    
    // Estados del input
    public enum InputState {
        IDLE,
        TOUCHING,
        DRAGGING,
        PINCHING,
        SWIPING
    }
    
    // Tipos de gestures reconocidos
    public enum GestureType {
        TAP,
        LONG_PRESS,
        SWIPE_UP,
        SWIPE_DOWN,
        SWIPE_LEFT,
        SWIPE_RIGHT,
        PINCH_IN,
        PINCH_OUT,
        ROTATE
    }
    
    // Interface para callbacks de eventos
    public interface InputListener {
        void onTouchDown(PointF position);
        void onTouchMove(PointF position);
        void onTouchUp(PointF position);
        void onGestureDetected(GestureType gesture, PointF origin, PointF end);
        void onVirtualInput(int virtualButtonId, boolean pressed);
        void onCalibrationComplete(float scaleFactor, float offsetX, float offsetY);
    }
    
    // Input virtual (botón en pantalla)
    public static class VirtualInput {
        public final int id;
        public final RectF bounds;
        public final String label;
        public boolean pressed;
        public boolean visible;
        
        public VirtualInput(int id, float left, float top, float right, float bottom, String label) {
            this.id = id;
            this.bounds = new RectF(left, top, right, bottom);
            this.label = label;
            this.pressed = false;
            this.visible = true;
        }
        
        public boolean contains(float x, float y) {
            return bounds.contains(x, y);
        }
        
        public void setBounds(float left, float top, float right, float bottom) {
            bounds.set(left, top, right, bottom);
        }
    }
    
    // Datos de un touch
    private static class TouchData {
        public int id;
        public PointF position;
        public PointF startPosition;
        public long startTime;
        public boolean isActive;
        public PointF lastPosition;
        
        public TouchData(int id, float x, float y) {
            this.id = id;
            this.position = new PointF(x, y);
            this.startPosition = new PointF(x, y);
            this.lastPosition = new PointF(x, y);
            this.startTime = System.currentTimeMillis();
            this.isActive = true;
        }
        
        public float getDistanceFromStart() {
            float dx = position.x - startPosition.x;
            float dy = position.y - startPosition.y;
            return (float) Math.sqrt(dx * dx + dy * dy);
        }
        
        public float getVelocity() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - startTime;
            if (timeDiff <= 0) return 0;
            
            float distance = getDistanceFromStart();
            return distance / (timeDiff / 1000.0f);
        }
    }
    
    // Parámetros de calibración
    private static class CalibrationData {
        public float screenWidth;
        public float screenHeight;
        public float referenceWidth = 1080;
        public float referenceHeight = 1920;
        public float scaleFactor;
        public float offsetX;
        public float offsetY;
        public boolean calibrated;
        
        public CalibrationData(float width, float height) {
            this.screenWidth = width;
            this.screenHeight = height;
            this.scaleFactor = Math.min(width / referenceWidth, height / referenceHeight);
            this.offsetX = (width - referenceWidth * scaleFactor) / 2;
            this.offsetY = (height - referenceHeight * scaleFactor) / 2;
            this.calibrated = true;
        }
    }
    
    // Estado interno
    private InputState currentState = InputState.IDLE;
    private List<TouchData> activeTouches = new ArrayList<>();
    private Map<Integer, TouchData> touchMap = new HashMap<>();
    private List<VirtualInput> virtualInputs = new ArrayList<>();
    private InputListener listener;
    
    // Parámetros de gestos
    private static final float TAP_THRESHOLD = 20; // píxeles
    private static final long TAP_TIME_THRESHOLD = 200; // milisegundos
    private static final float SWIPE_THRESHOLD = 100; // píxeles
    private static final float PINCH_THRESHOLD = 50; // píxeles
    
    // Datos para gestos complejos
    private TouchData firstTouch;
    private TouchData secondTouch;
    private float initialPinchDistance;
    private float initialPinchAngle;
    
    // Calibración
    private CalibrationData calibration;
    
    // Constructor
    public InputHandler(InputListener listener) {
        this.listener = listener;
        initializeDefaultVirtualInputs();
    }
    
    /**
     * Inicializa inputs virtuales por defecto
     */
    private void initializeDefaultVirtualInputs() {
        // Botón de pause (esquina superior derecha)
        virtualInputs.add(new VirtualInput(1, 
            920, 20, 1060, 100, "PAUSE"));
        
        // Botón de settings (esquina superior izquierda)
        virtualInputs.add(new VirtualInput(2, 
            20, 20, 160, 100, "SETTINGS"));
        
        // Botón de restart (centro inferior)
        virtualInputs.add(new VirtualInput(3, 
            480, 1720, 600, 1840, "RESTART"));
    }
    
    /**
     * Calibra el input handler para el dispositivo actual
     */
    public void calibrate(float screenWidth, float screenHeight) {
        calibration = new CalibrationData(screenWidth, screenHeight);
        
        // Ajustar posición de botones virtuales según calibración
        adjustVirtualInputsForCalibration();
        
        if (listener != null) {
            listener.onCalibrationComplete(calibration.scaleFactor, 
                calibration.offsetX, calibration.offsetY);
        }
    }
    
    /**
     * Ajusta los botones virtuales según la calibración
     */
    private void adjustVirtualInputsForCalibration() {
        if (calibration == null) return;
        
        for (VirtualInput input : virtualInputs) {
            float left = input.bounds.left * calibration.scaleFactor + calibration.offsetX;
            float top = input.bounds.top * calibration.scaleFactor + calibration.offsetY;
            float right = input.bounds.right * calibration.scaleFactor + calibration.offsetX;
            float bottom = input.bounds.bottom * calibration.scaleFactor + calibration.offsetY;
            input.setBounds(left, top, right, bottom);
        }
    }
    
    /**
     * Añade un nuevo input virtual
     */
    public void addVirtualInput(VirtualInput virtualInput) {
        if (calibration != null) {
            float left = virtualInput.bounds.left * calibration.scaleFactor + calibration.offsetX;
            float top = virtualInput.bounds.top * calibration.scaleFactor + calibration.offsetY;
            float right = virtualInput.bounds.right * calibration.scaleFactor + calibration.offsetX;
            float bottom = virtualInput.bounds.bottom * calibration.scaleFactor + calibration.offsetY;
            virtualInput.setBounds(left, top, right, bottom);
        }
        virtualInputs.add(virtualInput);
    }
    
    /**
     * Remueve un input virtual
     */
    public void removeVirtualInput(int id) {
        virtualInputs.removeIf(input -> input.id == id);
    }
    
    /**
     * Maneja eventos de touch
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        int pointerCount = event.getPointerCount();
        
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return handleActionDown(event);
                
            case MotionEvent.ACTION_POINTER_DOWN:
                return handleActionPointerDown(event);
                
            case MotionEvent.ACTION_MOVE:
                return handleActionMove(event);
                
            case MotionEvent.ACTION_UP:
                return handleActionUp(event);
                
            case MotionEvent.ACTION_POINTER_UP:
                return handleActionPointerUp(event);
                
            case MotionEvent.ACTION_CANCEL:
                return handleActionCancel(event);
        }
        
        return true;
    }
    
    /**
     * Maneja ACTION_DOWN (primer toque)
     */
    private boolean handleActionDown(MotionEvent event) {
        int pointerId = event.getPointerId(0);
        float x = event.getX();
        float y = event.getY();
        
        // Verificar si tocó un input virtual
        VirtualInput virtualInput = getVirtualInputAt(x, y);
        if (virtualInput != null) {
            virtualInput.pressed = true;
            if (listener != null) {
                listener.onVirtualInput(virtualInput.id, true);
            }
            return true;
        }
        
        // Crear nuevo touch data
        TouchData touch = new TouchData(pointerId, x, y);
        activeTouches.add(touch);
        touchMap.put(pointerId, touch);
        
        firstTouch = touch;
        currentState = InputState.TOUCHING;
        
        if (listener != null) {
            listener.onTouchDown(new PointF(x, y));
        }
        
        return true;
    }
    
    /**
     * Maneja ACTION_POINTER_DOWN (toques adicionales)
     */
    private boolean handleActionPointerDown(MotionEvent event) {
        if (activeTouches.size() >= 2) return true;
        
        int pointerId = event.getPointerId(event.getActionIndex());
        float x = event.getX(event.getActionIndex());
        float y = event.getY(event.getActionIndex());
        
        TouchData touch = new TouchData(pointerId, x, y);
        activeTouches.add(touch);
        touchMap.put(pointerId, touch);
        
        if (activeTouches.size() == 2) {
            secondTouch = touch;
            initializePinchDetection();
            currentState = InputState.PINCHING;
        }
        
        return true;
    }
    
    /**
     * Maneja ACTION_MOVE (movimiento)
     */
    private boolean handleActionMove(MotionEvent event) {
        int pointerCount = event.getPointerCount();
        
        for (int i = 0; i < pointerCount; i++) {
            int pointerId = event.getPointerId(i);
            TouchData touch = touchMap.get(pointerId);
            
            if (touch != null && touch.isActive) {
                float x = event.getX(i);
                float y = event.getY(i);
                
                touch.lastPosition.set(touch.position.x, touch.position.y);
                touch.position.set(x, y);
                
                // Verificar si sigue siendo un input virtual
                VirtualInput virtualInput = getVirtualInputAt(x, y);
                if (virtualInput != null) {
                    virtualInput.pressed = true;
                } else {
                    // Liberar cualquier input virtual que no esté siendo presionado
                    releaseUnusedVirtualInputs(x, y);
                }
            }
        }
        
        // Actualizar estado basado en el movimiento
        updateMovementState();
        
        // Actualizar gestos
        if (currentState == InputState.PINCHING) {
            updatePinchGesture();
        } else if (currentState == InputState.DRAGGING) {
            updateSwipeGesture();
        }
        
        // Notificar movimiento
        if (listener != null && firstTouch != null) {
            listener.onTouchMove(new PointF(firstTouch.position.x, firstTouch.position.y));
        }
        
        return true;
    }
    
    /**
     * Maneja ACTION_UP (fin de toque)
     */
    private boolean handleActionUp(MotionEvent event) {
        int pointerId = event.getPointerId(0);
        TouchData touch = touchMap.get(pointerId);
        
        if (touch != null) {
            // Verificar gesture final
            detectFinalGesture(touch);
            
            // Limpiar estado
            removeTouch(pointerId);
            
            // Notificar touch up
            if (listener != null) {
                listener.onTouchUp(new PointF(touch.position.x, touch.position.y));
            }
        }
        
        return true;
    }
    
    /**
     * Maneja ACTION_POINTER_UP (fin de toque adicional)
     */
    private boolean handleActionPointerUp(MotionEvent event) {
        int pointerId = event.getPointerId(event.getActionIndex());
        removeTouch(pointerId);
        
        if (activeTouches.size() < 2) {
            currentState = InputState.TOUCHING;
        }
        
        return true;
    }
    
    /**
     * Maneja ACTION_CANCEL
     */
    private boolean handleActionCancel(MotionEvent event) {
        clearAllTouches();
        currentState = InputState.IDLE;
        return true;
    }
    
    /**
     * Inicializa la detección de pinch
     */
    private void initializePinchDetection() {
        if (firstTouch != null && secondTouch != null) {
            initialPinchDistance = calculateDistance(firstTouch.position, secondTouch.position);
            initialPinchAngle = calculateAngle(firstTouch.position, secondTouch.position);
        }
    }
    
    /**
     * Actualiza el gesture de pinch
     */
    private void updatePinchGesture() {
        if (firstTouch != null && secondTouch != null) {
            float currentDistance = calculateDistance(firstTouch.position, secondTouch.position);
            float distanceDiff = currentDistance - initialPinchDistance;
            
            if (Math.abs(distanceDiff) > PINCH_THRESHOLD) {
                GestureType gesture = distanceDiff > 0 ? GestureType.PINCH_OUT : GestureType.PINCH_IN;
                if (listener != null) {
                    PointF center = getMidpoint(firstTouch.position, secondTouch.position);
                    listener.onGestureDetected(gesture, center, center);
                }
                initialPinchDistance = currentDistance;
            }
        }
    }
    
    /**
     * Actualiza el estado basado en el movimiento
     */
    private void updateMovementState() {
        if (firstTouch != null) {
            float distance = firstTouch.getDistanceFromStart();
            
            if (currentState == InputState.TOUCHING && distance > TAP_THRESHOLD) {
                currentState = InputState.DRAGGING;
            }
        }
    }
    
    /**
     * Actualiza el gesture de swipe
     */
    private void updateSwipeGesture() {
        if (firstTouch != null && activeTouches.size() == 1) {
            float distance = firstTouch.getDistanceFromStart();
            float velocity = firstTouch.getVelocity();
            
            if (distance > SWIPE_THRESHOLD && velocity > 200) {
                PointF direction = calculateSwipeDirection(firstTouch.startPosition, firstTouch.position);
                GestureType gesture = getSwipeGestureFromDirection(direction);
                
                if (listener != null) {
                    listener.onGestureDetected(gesture, firstTouch.startPosition, firstTouch.position);
                }
            }
        }
    }
    
    /**
     * Detecta gesture final al soltar
     */
    private void detectFinalGesture(TouchData touch) {
        long duration = System.currentTimeMillis() - touch.startTime;
        float distance = touch.getDistanceFromStart();
        
        if (duration < TAP_TIME_THRESHOLD && distance < TAP_THRESHOLD) {
            if (listener != null) {
                listener.onGestureDetected(GestureType.TAP, touch.position, touch.position);
            }
        } else if (duration > 1000 && distance < TAP_THRESHOLD) {
            if (listener != null) {
                listener.onGestureDetected(GestureType.LONG_PRESS, touch.position, touch.position);
            }
        }
    }
    
    /**
     * Obtiene input virtual en una posición
     */
    private VirtualInput getVirtualInputAt(float x, float y) {
        for (VirtualInput input : virtualInputs) {
            if (input.visible && input.contains(x, y)) {
                return input;
            }
        }
        return null;
    }
    
    /**
     * Libera inputs virtuales no utilizados
     */
    private void releaseUnusedVirtualInputs(float currentX, float currentY) {
        for (VirtualInput input : virtualInputs) {
            if (input.pressed && !input.contains(currentX, currentY)) {
                input.pressed = false;
                if (listener != null) {
                    listener.onVirtualInput(input.id, false);
                }
            }
        }
    }
    
    /**
     * Remueve un touch
     */
    private void removeTouch(int pointerId) {
        TouchData touch = touchMap.remove(pointerId);
        if (touch != null) {
            activeTouches.remove(touch);
        }
        
        // Actualizar referencias
        if (firstTouch != null && firstTouch.id == pointerId) {
            firstTouch = activeTouches.isEmpty() ? null : activeTouches.get(0);
        }
        if (secondTouch != null && secondTouch.id == pointerId) {
            secondTouch = activeTouches.size() > 1 ? activeTouches.get(1) : null;
        }
        
        if (activeTouches.isEmpty()) {
            currentState = InputState.IDLE;
        }
    }
    
    /**
     * Limpia todos los touches
     */
    private void clearAllTouches() {
        activeTouches.clear();
        touchMap.clear();
        firstTouch = null;
        secondTouch = null;
        
        // Liberar todos los inputs virtuales
        for (VirtualInput input : virtualInputs) {
            if (input.pressed) {
                input.pressed = false;
                if (listener != null) {
                    listener.onVirtualInput(input.id, false);
                }
            }
        }
    }
    
    /**
     * Calcula distancia entre dos puntos
     */
    private float calculateDistance(PointF a, PointF b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    /**
     * Calcula ángulo entre dos puntos
     */
    private float calculateAngle(PointF a, PointF b) {
        return (float) Math.atan2(b.y - a.y, b.x - a.x) * 180 / (float) Math.PI;
    }
    
    /**
     * Obtiene el punto medio entre dos puntos
     */
    private PointF getMidpoint(PointF a, PointF b) {
        return new PointF((a.x + b.x) / 2, (a.y + b.y) / 2);
    }
    
    /**
     * Calcula dirección de swipe
     */
    private PointF calculateSwipeDirection(PointF start, PointF end) {
        return new PointF(end.x - start.x, end.y - start.y);
    }
    
    /**
     * Obtiene tipo de gesture de swipe basado en dirección
     */
    private GestureType getSwipeGestureFromDirection(PointF direction) {
        float angle = (float) Math.atan2(direction.y, direction.x) * 180 / (float) Math.PI;
        
        if (angle >= -45 && angle <= 45) {
            return GestureType.SWIPE_RIGHT;
        } else if (angle > 45 && angle <= 135) {
            return GestureType.SWIPE_DOWN;
        } else if (angle > 135 || angle <= -135) {
            return GestureType.SWIPE_LEFT;
        } else {
            return GestureType.SWIPE_UP;
        }
    }
    
    // Getters públicos
    
    public InputState getCurrentState() {
        return currentState;
    }
    
    public List<TouchData> getActiveTouches() {
        return new ArrayList<>(activeTouches);
    }
    
    public List<VirtualInput> getVirtualInputs() {
        return new ArrayList<>(virtualInputs);
    }
    
    public boolean isTouchActive() {
        return !activeTouches.isEmpty();
    }
    
    public boolean isPinching() {
        return currentState == InputState.PINCHING;
    }
    
    public PointF getPrimaryTouchPosition() {
        return firstTouch != null ? new PointF(firstTouch.position.x, firstTouch.position.y) : null;
    }
    
    public float getPinchScale() {
        if (firstTouch != null && secondTouch != null && initialPinchDistance > 0) {
            float currentDistance = calculateDistance(firstTouch.position, secondTouch.position);
            return currentDistance / initialPinchDistance;
        }
        return 1.0f;
    }
    
    /**
     * Fuerza la liberación de todos los inputs
     */
    public void forceRelease() {
        clearAllTouches();
        currentState = InputState.IDLE;
    }
    
    /**
     * Configura la sensibilidad de gestos
     */
    public void setGestureSensitivity(float tapThreshold, float swipeThreshold, float pinchThreshold) {
        // Nota: En una implementación completa, estos valores se guardarían en variables de instancia
        // y se usarían en lugar de las constantes definidas al inicio de la clase
    }
    
    /**
     * Obtiene información de debug sobre el estado actual
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Estado: ").append(currentState).append("\n");
        sb.append("Touches activos: ").append(activeTouches.size()).append("\n");
        sb.append("Inputs virtuales activos: ");
        
        int pressedCount = 0;
        for (VirtualInput input : virtualInputs) {
            if (input.pressed) pressedCount++;
        }
        sb.append(pressedCount).append("\n");
        
        if (firstTouch != null) {
            sb.append("Touch principal: (").append(firstTouch.position.x).append(", ")
              .append(firstTouch.position.y).append(")\n");
        }
        
        return sb.toString();
    }
}