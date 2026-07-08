// Единая палитра оформления — светлая тема «premium fitness».
// Раньше цветовые константы дублировались в каждом файле; теперь один источник.
// Контраст акцентов проверен валидатором для светлого фона (scripts/validate_palette.js).

// ── Нейтрали ─────────────────────────────────────────────────────────────────
export const C_BG      = '#F6F7F9'; // холст приложения (прохладный off-white)
export const C_SURFACE = '#FFFFFF'; // карточка
export const C_SURF2   = '#F3F5F8'; // вложенная плитка / зебра таблиц
export const C_BORDER  = '#E6E8EE'; // hairline-граница
export const C_TEXT    = '#101828'; // основной текст
export const C_MUTED   = '#667085'; // приглушённый текст

// ── Акценты по видам спорта ──────────────────────────────────────────────────
export const C_BIKE = '#0E9F6E'; // велосипед
export const C_SWIM = '#2563EB'; // плавание
export const C_RUN  = '#D97706'; // бег
export const C_WALK = '#64748B'; // ходьба / прочее
export const C_RED  = '#EF4444'; // ошибки

// ── Тени и оверлеи ───────────────────────────────────────────────────────────
export const CARD_SHADOW       = '0 1px 2px rgba(16,24,40,0.04), 0 1px 3px rgba(16,24,40,0.06)';
export const CARD_SHADOW_HOVER = '0 6px 16px rgba(16,24,40,0.10), 0 2px 6px rgba(16,24,40,0.06)';
export const OVERLAY_BG        = 'rgba(16,24,40,0.45)';
