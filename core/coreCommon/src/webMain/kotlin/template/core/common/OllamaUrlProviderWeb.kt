package template.core.common

class OllamaUrlProviderWeb : OllamaUrlProvider {
    override fun getBaseUrl(): String = "http://localhost:11434"
}
