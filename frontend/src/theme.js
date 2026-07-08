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
// Слоёные мягкие тени — дают карточкам объём, не утяжеляя («глубина и стекло»).
export const CARD_SHADOW       = '0 1px 2px rgba(16,24,40,0.04), 0 2px 8px rgba(16,24,40,0.05), 0 12px 28px -14px rgba(16,24,40,0.12)';
export const CARD_SHADOW_HOVER = '0 2px 6px rgba(16,24,40,0.06), 0 10px 24px -6px rgba(16,24,40,0.14), 0 18px 40px -12px rgba(16,24,40,0.12)';
export const OVERLAY_BG        = 'rgba(16,24,40,0.45)';

// ── «Стекло»: холст со свечением + фростед-хедер ─────────────────────────────
// Тёплое зелёное и прохладное синее свечение в верхней части холста.
export const CANVAS_BG   = 'radial-gradient(1100px 520px at 12% -8%, rgba(14,159,110,0.10), transparent 60%), radial-gradient(900px 480px at 100% -6%, rgba(37,99,235,0.08), transparent 55%), #f6f7f9';
export const HEADER_BG   = 'rgba(246,247,249,0.72)'; // полупрозрачный C_BG под backdrop-blur
export const HEADER_BLUR = 'saturate(180%) blur(12px)';
