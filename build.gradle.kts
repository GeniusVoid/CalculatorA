// Minimal root build file — no repositories here (settings.gradle handles repos)
import org.gradle.api.tasks.Delete

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
