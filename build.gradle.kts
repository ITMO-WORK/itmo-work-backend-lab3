subprojects {
    apply(plugin = "jacoco")

    tasks.withType<Test>().configureEach {
        useJUnitPlatform {
            val exclude = project.findProperty("excludeTags") as String?
            if (exclude != null) {
                excludeTags(exclude)
            }
        }
        finalizedBy("jacocoTestReport")
    }

    tasks.withType<JacocoReport>().configureEach {
        reports {
            xml.required.set(true)
        }
    }
}