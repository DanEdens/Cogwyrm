package com.cogwyrm.app.utils

sealed class CogwyrmError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ValidationError(message: String, cause: Throwable? = null) : CogwyrmError(message, cause)
    class ConnectionError(message: String, cause: Throwable? = null) : CogwyrmError(message, cause)
    class ConfigurationError(message: String, cause: Throwable? = null) : CogwyrmError(message, cause)
    class NetworkError(message: String, cause: Throwable? = null) : CogwyrmError(message, cause)
    class UnknownError(message: String, cause: Throwable? = null) : CogwyrmError(message, cause)
}
