import groovy.json.JsonSlurper
def file = new File("app/build.gradle")
def text = file.text
def newText = text.replaceAll(/def getConfigValue = \{ String id ->[\s\S]*?def generateConfigId =/, """def getConfigValue = { String id ->
    try {
        def configFile = file("\\\${rootProject.projectDir.path}/app/src/main/assets/config/config.json")
        def manualFile = file("\\\${rootProject.projectDir.path}/app/src/main/assets/config/config_manual.json")
        
        if (configFile.exists()) {
            def json = new groovy.json.JsonSlurper().parseText(configFile.text)
            if (json.containsKey(id)) return json[id].toString()
        }
        if (manualFile.exists()) {
            def manualJson = new groovy.json.JsonSlurper().parseText(manualFile.text)
            if (manualJson.containsKey(id)) return manualJson[id].toString()
        }
        
        throw new GradleException("Configuration '\$id' not found in config.json files")
    } catch (Exception e) {
        throw new GradleException("Error reading configuration '\$id': \${e.message}", e)
    }
}

/**
 * Generates a unique app identifier from conf_name and conf_core.
 * Converts both strings to lowercase, removes special characters,
 * and combines them with an underscore.
 * 
 * Example: "Sonic The Hedgehog" + "picodrive" -> "sonic_the_hedgehog_picodrive"
 */
def generateConfigId =""")
file.write(newText)
