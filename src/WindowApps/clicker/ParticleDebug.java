package WindowApps.clicker;

;

/**
 * Временная диагностика маршрута координат частиц: пишет лог из живого приложения,
 * чтобы не гадать, где теряется точка клика. Удаляется после отладки.
 */
final class ParticleDebug {

    private ParticleDebug() {
    }

    static void log(String message) {
        try (java.io.FileWriter writer = new java.io.FileWriter("particle-debug.log", true)) {
            writer.write(System.currentTimeMillis() + " " + message + System.lineSeparator());
        } catch (Exception ignored) {
            // диагностика не должна ломать приложение
        }
    }
}
