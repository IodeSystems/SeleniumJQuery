package com.iodesystems.selenium.exceptions

class RetryException(
  message: String, cause: Throwable? = null
) : Exception(message, cause)
