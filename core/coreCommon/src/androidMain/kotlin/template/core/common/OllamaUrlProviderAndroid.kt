package template.core.common

class OllamaUrlProviderAndroid : OllamaUrlProvider {
    override fun getBaseUrl(): String = "http://10.0.2.2:11434" // "http://127.0.0.1:11434"
}
