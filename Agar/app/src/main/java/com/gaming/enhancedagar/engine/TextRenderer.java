package com.gaming.enhancedagar.engine;

import android.graphics.*;
import android.text.TextPaint;
import android.util.LruCache;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.UnderlineSpan;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Sistema avanzado de renderizado de texto con cache inteligente y efectos visuales
 * Optimizado para rendimiento en juegos Android con soporte para auto-scaling y emojis
 */
public class TextRenderer {
    
    // Cache principal para texto renderizado
    private final LruCache<String, Bitmap> textCache;
    private final LruCache<String, TextMetrics> metricsCache;
    
    // Cache para configuraciones de texto
    private final ConcurrentHashMap<String, TextPaint> paintCache;
    
    // Sistema de auto-scaling basado en densidad
    private float density;
    private float scaleFactor;
    
    // Configuración de rendimiento
    private static final int MAX_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MAX_TEXT_CACHE_SIZE = 1000;
    private static final float OUTLINE_WIDTH_MULTIPLIER = 0.1f;
    private static final float SHADOW_OFFSET_MULTIPLIER = 0.05f;
    
    // Patrones para reconocimiento de emojis y caracteres especiales
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
        "[\\u2190-\\u21FF\\u2300-\\u27BF\\uD83C-\\uDBFF\\uDC00-\\uDFFF]"
    );
    
    // Lista de tooltips activos
    private final List<Tooltip> activeTooltips;
    private final List<Label> activeLabels;
    
    /**
     * Enumeración para tipos de efectos de texto
     */
    public enum TextEffect {
        NONE,
        OUTLINE,
        SHADOW,
        GLOW,
        OUTLINE_AND_SHADOW
    }
    
    /**
     * Configuración de fuentes
     */
    public enum FontStyle {
        REGULAR(Typeface.NORMAL),
        BOLD(Typeface.BOLD),
        ITALIC(Typeface.ITALIC),
        BOLD_ITALIC(Typeface.BOLD_ITALIC);
        
        final int style;
        
        FontStyle(int style) {
            this.style = style;
        }
        
        public int getStyle() {
            return style;
        }
    }
    
    /**
     * Métricas de texto calculadas
     */
    private static class TextMetrics {
        public float width;
        public float height;
        public float ascent;
        public float descent;
        public float leading;
        
        public TextMetrics(float width, float height, float ascent, float descent, float leading) {
            this.width = width;
            this.height = height;
            this.ascent = ascent;
            this.descent = descent;
            this.leading = leading;
        }
    }
    
    /**
     * Configuración de renderizado de texto
     */
    public static class TextConfig {
        public float size;
        public Typeface typeface;
        public int color;
        public TextEffect effect;
        public int effectColor;
        public float effectStrength;
        public int backgroundColor;
        public float padding;
        public boolean antiAlias;
        public int maxLines;
        public float lineSpacing;
        public boolean wrap;
        
        public TextConfig() {
            this.size = 14 * getScaleFactor();
            this.typeface = Typeface.DEFAULT;
            this.color = Color.WHITE;
            this.effect = TextEffect.NONE;
            this.effectColor = Color.BLACK;
            this.effectStrength = 1.0f;
            this.backgroundColor = Color.TRANSPARENT;
            this.padding = 4 * getScaleFactor();
            this.antiAlias = true;
            this.maxLines = 1;
            this.lineSpacing = 1.0f;
            this.wrap = true;
        }
        
        public TextConfig(float size, int color, FontStyle style) {
            this();
            this.size = size;
            this.color = color;
            this.typeface = Typeface.create(Typeface.DEFAULT, style.getStyle());
        }
    }
    
    /**
     * Configuración de tooltip
     */
    public static class TooltipConfig {
        public float fontSize;
        public int backgroundColor;
        public int textColor;
        public int borderColor;
        public float borderWidth;
        public float padding;
        public float cornerRadius;
        public int shadowColor;
        public float shadowOffset;
        
        public TooltipConfig() {
            this.fontSize = 12 * getScaleFactor();
            this.backgroundColor = Color.argb(230, 0, 0, 0);
            this.textColor = Color.WHITE;
            this.borderColor = Color.argb(100, 255, 255, 255);
            this.borderWidth = 1 * getScaleFactor();
            this.padding = 8 * getScaleFactor();
            this.cornerRadius = 8 * getScaleFactor();
            this.shadowColor = Color.argb(128, 0, 0, 0);
            this.shadowOffset = 2 * getScaleFactor();
        }
    }
    
    /**
     * Clase interna para tooltips
     */
    private static class Tooltip {
        public String text;
        public float x, y;
        public float width, height;
        public TooltipConfig config;
        public long showTime;
        public long duration;
        public boolean isVisible;
        
        public Tooltip(String text, float x, float y, TooltipConfig config) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.config = config;
            this.duration = 3000; // 3 segundos por defecto
            this.isVisible = true;
            this.showTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Clase interna para labels
     */
    private static class Label {
        public String text;
        public float x, y;
        public TextConfig config;
        public boolean isVisible;
        public long showTime;
        public long duration;
        public boolean autoHide;
        
        public Label(String text, float x, float y, TextConfig config) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.config = config;
            this.isVisible = true;
            this.showTime = 0;
            this.duration = 0;
            this.autoHide = false;
        }
    }
    
    /**
     * Constructor
     */
    public TextRenderer(Context context) {
        this.density = context.getResources().getDisplayMetrics().density;
        this.scaleFactor = density / 1.5f; // Factor de escala base
        
        // Inicializar caches
        this.textCache = new LruCache<String, Bitmap>(MAX_TEXT_CACHE_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (oldValue != null && !oldValue.isRecycled()) {
                    oldValue.recycle();
                }
            }
        };
        
        this.metricsCache = new LruCache<String, TextMetrics>(2000);
        this.paintCache = new ConcurrentHashMap<>();
        this.activeTooltips = new ArrayList<>();
        this.activeLabels = new ArrayList<>();
        
        // Precargar fuentes comunes
        preloadCommonFonts();
    }
    
    /**
     * Establece el factor de escala para auto-scaling
     */
    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor = scaleFactor;
        clearCaches(); // Limpiar cache al cambiar escala
    }
    
    /**
     * Obtiene el factor de escala actual
     */
    public static float getScaleFactor() {
        return 1.0f; // Se sobreescribe en el constructor
    }
    
    /**
     * Precarga fuentes comunes para mejorar rendimiento
     */
    private void preloadCommonFonts() {
        String[] commonFonts = {"system", "bold", "serif", "sans-serif"};
        for (String font : commonFonts) {
            String key = font + "_" + FontStyle.REGULAR.getStyle();
            if (!paintCache.containsKey(key)) {
                TextPaint paint = createTextPaint(14 * scaleFactor, Color.WHITE, 
                    Typeface.create(font, FontStyle.REGULAR.getStyle()), TextEffect.NONE);
                paintCache.put(key, paint);
            }
        }
    }
    
    /**
     * Crea un objeto TextPaint configurado
     */
    private TextPaint createTextPaint(float size, int color, Typeface typeface, TextEffect effect) {
        String paintKey = size + "_" + color + "_" + typeface + "_" + effect;
        
        if (paintCache.containsKey(paintKey)) {
            return paintCache.get(paintKey);
        }
        
        TextPaint paint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(size);
        paint.setColor(color);
        paint.setTypeface(typeface);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        
        // Configurar efectos
        switch (effect) {
            case OUTLINE:
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(size * OUTLINE_WIDTH_MULTIPLIER);
                paint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                break;
            case SHADOW:
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(size * SHADOW_OFFSET_MULTIPLIER, 
                                  size * SHADOW_OFFSET_MULTIPLIER, 
                                  size * SHADOW_OFFSET_MULTIPLIER, 
                                  Color.BLACK);
                break;
            case GLOW:
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(size * 0.1f, 0, 0, color);
                break;
            case OUTLINE_AND_SHADOW:
                paint.setStyle(Paint.Style.STROKE_AND_FILL);
                paint.setStrokeWidth(size * OUTLINE_WIDTH_MULTIPLIER);
                paint.setShadowLayer(size * SHADOW_OFFSET_MULTIPLIER, 
                                  size * SHADOW_OFFSET_MULTIPLIER, 
                                  size * SHADOW_OFFSET_MULTIPLIER, 
                                  Color.BLACK);
                break;
            default:
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                break;
        }
        
        paintCache.put(paintKey, paint);
        return paint;
    }
    
    /**
     * Renderiza texto simple con cache
     */
    public Bitmap renderText(String text, TextConfig config) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        String cacheKey = generateCacheKey(text, config);
        Bitmap cached = textCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        // Calcular dimensiones del texto
        TextMetrics metrics = calculateTextMetrics(text, config);
        int width = (int) Math.ceil(metrics.width + config.padding * 2);
        int height = (int) Math.ceil(metrics.height + config.padding * 2);
        
        if (width <= 0 || height <= 0) {
            return null;
        }
        
        // Crear bitmap para renderizado
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        // Rellenar fondo si es necesario
        if (config.backgroundColor != Color.TRANSPARENT) {
            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(config.backgroundColor);
            backgroundPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(0, 0, width, height, backgroundPaint);
        }
        
        // Configurar paint para el texto
        TextPaint textPaint = createTextPaint(config.size, config.color, 
                                            config.typeface, config.effect);
        
        // Renderizar texto con efectos
        renderTextWithEffects(canvas, text, textPaint, config, metrics);
        
        // Cachear resultado
        textCache.put(cacheKey, result);
        return result;
    }
    
    /**
     * Renderiza texto con efectos especiales
     */
    private void renderTextWithEffects(Canvas canvas, String text, TextPaint paint, 
                                     TextConfig config, TextMetrics metrics) {
        float x = config.padding;
        float y = config.padding + Math.abs(metrics.ascent);
        
        // Renderizar outline si está activado
        if (config.effect == TextEffect.OUTLINE || config.effect == TextEffect.OUTLINE_AND_SHADOW) {
            Paint outlinePaint = new Paint(paint);
            outlinePaint.setStyle(Paint.Style.STROKE);
            outlinePaint.setColor(config.effectColor);
            outlinePaint.setStrokeWidth(config.size * OUTLINE_WIDTH_MULTIPLIER);
            canvas.drawText(text, x, y, outlinePaint);
        }
        
        // Renderizar sombra si está activada
        if (config.effect == TextEffect.SHADOW || config.effect == TextEffect.OUTLINE_AND_SHADOW) {
            Paint shadowPaint = new Paint(paint);
            shadowPaint.setColor(config.effectColor);
            shadowPaint.setShadowLayer(config.size * SHADOW_OFFSET_MULTIPLIER, 
                                     config.size * SHADOW_OFFSET_MULTIPLIER, 
                                     config.size * SHADOW_OFFSET_MULTIPLIER, 
                                     config.effectColor);
            canvas.drawText(text, x, y, shadowPaint);
        }
        
        // Renderizar glow si está activado
        if (config.effect == TextEffect.GLOW) {
            Paint glowPaint = new Paint(paint);
            glowPaint.setShadowLayer(config.size * 0.1f, 0, 0, config.effectColor);
            canvas.drawText(text, x, y, glowPaint);
        }
        
        // Renderizar texto principal
        canvas.drawText(text, x, y, paint);
    }
    
    /**
     * Calcula las métricas del texto para el renderizado
     */
    private TextMetrics calculateTextMetrics(String text, TextConfig config) {
        String metricsKey = text + "_" + config.size + "_" + config.typeface;
        TextMetrics cached = metricsCache.get(metricsKey);
        if (cached != null) {
            return cached;
        }
        
        TextPaint paint = createTextPaint(config.size, config.color, config.typeface, TextEffect.NONE);
        
        // Medir texto
        Paint.FontMetrics fm = paint.getFontMetrics();
        float width = paint.measureText(text);
        float height = fm.bottom - fm.top;
        float ascent = fm.ascent;
        float descent = fm.descent;
        float leading = fm.leading;
        
        // Ajustar altura si hay múltiples líneas
        if (config.maxLines > 1) {
            String[] lines = text.split("\n");
            height = (fm.bottom - fm.top) * lines.length * config.lineSpacing;
        }
        
        TextMetrics metrics = new TextMetrics(width, height, ascent, descent, leading);
        metricsCache.put(metricsKey, metrics);
        return metrics;
    }
    
    /**
     * Renderiza texto con soporte para emojis y caracteres especiales
     */
    public Bitmap renderTextWithEmojis(String text, TextConfig config) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        
        // Detectar emojis y caracteres especiales
        boolean hasEmojis = EMOJI_PATTERN.matcher(text).find();
        
        if (!hasEmojis) {
            // Renderizado normal si no hay emojis
            return renderText(text, config);
        }
        
        // Renderizado especial para emojis
        return renderTextWithSpannable(text, config);
    }
    
    /**
     * Renderiza texto con SpannableString para soporte avanzado de formato
     */
    private Bitmap renderTextWithSpannable(String text, TextConfig config) {
        SpannableString spannable = new SpannableString(text);
        
        // Aplicar formatos especiales a emojis y caracteres especiales
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (EMOJI_PATTERN.matcher(String.valueOf(c)).find()) {
                // Aplicar color especial para emojis
                spannable.setSpan(new ForegroundColorSpan(Color.YELLOW), i, i + 1, 
                                Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
        
        // Crear bitmap para renderizado
        TextMetrics metrics = calculateTextMetrics(text, config);
        int width = (int) Math.ceil(metrics.width + config.padding * 2);
        int height = (int) Math.ceil(metrics.height + config.padding * 2);
        
        if (width <= 0 || height <= 0) {
            return null;
        }
        
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        // Usar StaticLayout para mejor renderizado de texto complejo
        android.text.StaticLayout staticLayout = new android.text.StaticLayout(
            spannable, 
            createTextPaint(config.size, config.color, config.typeface, config.effect),
            width - (int)(config.padding * 2),
            android.text.Layout.Alignment.ALIGN_NORMAL,
            config.lineSpacing,
            0,
            false
        );
        
        // Dibujar fondo si es necesario
        if (config.backgroundColor != Color.TRANSPARENT) {
            Paint backgroundPaint = new Paint();
            backgroundPaint.setColor(config.backgroundColor);
            canvas.drawRect(0, 0, width, height, backgroundPaint);
        }
        
        // Dibujar texto
        canvas.translate(config.padding, config.padding);
        staticLayout.draw(canvas);
        
        return result;
    }
    
    /**
     * Crea un tooltip con texto
     */
    public void showTooltip(String text, float x, float y, TooltipConfig config) {
        Tooltip tooltip = new Tooltip(text, x, y, config);
        activeTooltips.add(tooltip);
    }
    
    /**
     * Crea un tooltip simple
     */
    public void showTooltip(String text, float x, float y) {
        showTooltip(text, x, y, new TooltipConfig());
    }
    
    /**
     * Renderiza todos los tooltips activos
     */
    public void renderTooltips(Canvas canvas) {
        long currentTime = System.currentTimeMillis();
        activeTooltips.removeIf(tooltip -> {
            // Verificar si el tooltip debe ocultarse
            if (currentTime - tooltip.showTime > tooltip.duration) {
                tooltip.isVisible = false;
                return true;
            }
            
            if (!tooltip.isVisible) {
                return true;
            }
            
            // Renderizar tooltip
            renderTooltip(canvas, tooltip);
            return false;
        });
    }
    
    /**
     * Renderiza un tooltip individual
     */
    private void renderTooltip(Canvas canvas, Tooltip tooltip) {
        // Crear configuración de texto para el tooltip
        TextConfig textConfig = new TextConfig();
        textConfig.size = tooltip.config.fontSize;
        textConfig.color = tooltip.config.textColor;
        textConfig.backgroundColor = Color.TRANSPARENT;
        
        // Renderizar texto
        Bitmap textBitmap = renderText(tooltip.text, textConfig);
        if (textBitmap == null) return;
        
        float width = textBitmap.getWidth() + tooltip.config.padding * 2;
        float height = textBitmap.getHeight() + tooltip.config.padding * 2;
        
        // Ajustar posición para evitar salir de pantalla
        float adjustedX = Math.max(tooltip.config.padding, 
                                 Math.min(tooltip.x, canvas.getWidth() - width - tooltip.config.padding));
        float adjustedY = Math.max(tooltip.config.padding, 
                                 Math.min(tooltip.y - height, canvas.getHeight() - height - tooltip.config.padding));
        
        // Crear path con esquinas redondeadas
        Path roundedRectPath = new Path();
        roundedRectPath.addRoundRect(
            adjustedX, adjustedY, adjustedX + width, adjustedY + height,
            new float[]{tooltip.config.cornerRadius, tooltip.config.cornerRadius,
                       tooltip.config.cornerRadius, tooltip.config.cornerRadius,
                       tooltip.config.cornerRadius, tooltip.config.cornerRadius,
                       tooltip.config.cornerRadius, tooltip.config.cornerRadius},
            Path.Direction.CW
        );
        
        // Dibujar sombra
        if (tooltip.config.shadowColor != Color.TRANSPARENT) {
            Paint shadowPaint = new Paint();
            shadowPaint.setColor(tooltip.config.shadowColor);
            shadowPaint.setStyle(Paint.Style.FILL);
            shadowPaint.setShadowLayer(tooltip.config.shadowOffset, 
                                     tooltip.config.shadowOffset, 
                                     tooltip.config.shadowOffset, 
                                     tooltip.config.shadowColor);
            canvas.drawPath(roundedRectPath, shadowPaint);
        }
        
        // Dibujar fondo del tooltip
        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(tooltip.config.backgroundColor);
        backgroundPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(roundedRectPath, backgroundPaint);
        
        // Dibujar borde
        if (tooltip.config.borderWidth > 0) {
            Paint borderPaint = new Paint();
            borderPaint.setColor(tooltip.config.borderColor);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(tooltip.config.borderWidth);
            canvas.drawPath(roundedRectPath, borderPaint);
        }
        
        // Dibujar texto
        canvas.drawBitmap(textBitmap, 
                         adjustedX + tooltip.config.padding, 
                         adjustedY + tooltip.config.padding, 
                         null);
    }
    
    /**
     * Crea un label de texto
     */
    public Label createLabel(String text, float x, float y, TextConfig config) {
        Label label = new Label(text, x, y, config);
        activeLabels.add(label);
        return label;
    }
    
    /**
     * Renderiza todos los labels activos
     */
    public void renderLabels(Canvas canvas) {
        long currentTime = System.currentTimeMillis();
        activeLabels.removeIf(label -> {
            // Verificar si el label debe ocultarse
            if (label.autoHide && currentTime - label.showTime > label.duration) {
                label.isVisible = false;
                return true;
            }
            
            if (!label.isVisible) {
                return true;
            }
            
            // Renderizar label
            renderLabel(canvas, label);
            return false;
        });
    }
    
    /**
     * Renderiza un label individual
     */
    private void renderLabel(Canvas canvas, Label label) {
        Bitmap textBitmap = renderText(label.text, label.config);
        if (textBitmap != null) {
            canvas.drawBitmap(textBitmap, label.x, label.y, null);
        }
    }
    
    /**
     * Actualiza la posición de un label
     */
    public void updateLabelPosition(Label label, float x, float y) {
        label.x = x;
        label.y = y;
    }
    
    /**
     * Actualiza el texto de un label
     */
    public void updateLabelText(Label label, String newText) {
        label.text = newText;
        // Invalidar cache relacionado
        String oldCacheKey = generateCacheKey(label.text, label.config);
        textCache.remove(oldCacheKey);
    }
    
    /**
     * Oculta un label después de un tiempo específico
     */
    public void hideLabelAfter(Label label, long milliseconds) {
        label.autoHide = true;
        label.duration = milliseconds;
        label.showTime = System.currentTimeMillis();
    }
    
    /**
     * Genera una clave única para el cache
     */
    private String generateCacheKey(String text, TextConfig config) {
        return text + "_" + config.size + "_" + config.color + "_" + 
               config.typeface.hashCode() + "_" + config.effect + "_" + 
               config.effectColor + "_" + config.backgroundColor + "_" + 
               config.padding + "_" + scaleFactor;
    }
    
    /**
     * Limpia todos los caches
     */
    public void clearCaches() {
        textCache.evictAll();
        metricsCache.evictAll();
        paintCache.clear();
        preloadCommonFonts();
    }
    
    /**
     * Obtiene estadísticas del cache
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("textCacheSize", textCache.size());
        stats.put("textCacheMaxSize", textCache.maxSize());
        stats.put("metricsCacheSize", metricsCache.size());
        stats.put("metricsCacheMaxSize", metricsCache.maxSize());
        stats.put("paintCacheSize", paintCache.size());
        stats.put("activeTooltips", activeTooltips.size());
        stats.put("activeLabels", activeLabels.size());
        return stats;
    }
    
    /**
     * Libera recursos del TextRenderer
     */
    public void destroy() {
        // Reciclar bitmaps en cache
        Map<String, Bitmap> snapshot = textCache.snapshot();
        for (Bitmap bitmap : snapshot.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        
        // Limpiar caches
        clearCaches();
        
        // Limpiar listas
        activeTooltips.clear();
        activeLabels.clear();
    }
}