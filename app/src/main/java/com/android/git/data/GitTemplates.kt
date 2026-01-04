package com.android.git.data

object GitTemplates {

    // قائمة القوالب (الاسم -> المحتوى)
    val templates = mapOf(
        "Android / Kotlin" to """
            # Android
            .gradle/
            .idea/
            build/
            app/build/
            local.properties
            *.iml
            .DS_Store
            captures/
            .externalNativeBuild/
            .cxx/
        """.trimIndent(),

        "Python / AI" to """
            # Python
            __pycache__/
            *.py[cod]
            *${'$'}py.class
            
            # Virtual Env
            venv/
            env/
            .env
            
            # AI / ML
            *.ipynb_checkpoints
            *.pt
            *.pth
            *.h5
            models/
            data/
        """.trimIndent(),

        "Web / Node.js" to """
            # Node
            node_modules/
            npm-debug.log
            yarn-error.log
            
            # Build
            dist/
            build/
            .env
            .DS_Store
        """.trimIndent(),

        "Flutter" to """
            # Flutter
            .dart_tool/
            .idea/
            .pub/
            build/
            .packages
            .flutter-plugins
            .flutter-plugins-dependencies
        """.trimIndent(),

        "Java" to """
            # Java
            *.class
            *.log
            *.jar
            *.war
            .idea/
            *.iml
        """.trimIndent()
    )
}