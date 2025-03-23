package com.iodesystems.selenium.el

data class Either(
  private val left: IEl? = null,
  private val right: IEl? = null,
) : IEl by left ?: right!! {
  fun <T> map(fn: (IEl?, IEl?) -> T): T? {
    return fn(left, right)
  }

  fun <T> left(fn: IEl.(IEl) -> T): T? {
    return if (left == null) null
    else fn.invoke(left, left)
  }

  fun <T> right(fn: IEl.(IEl) -> T): T? {
    return if (right == null) null
    else fn.invoke(right, right)
  }
}
