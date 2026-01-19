package template.core.common

class OllamaUrlProviderJvm : OllamaUrlProvider {
    override fun getBaseUrl(): String = "http://localhost:11434"
}
