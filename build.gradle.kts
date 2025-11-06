// Minimal root build.gradle.kts — repositories must be defined in settings.gradle.kts
// Keeps only a clean task to avoid repository mutation errors.

import org.gradle.api.tasks.Delete

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
