package com.letsplay.exception

import java.lang.RuntimeException

class ConnectionException(val code: Int, override val message: String) : RuntimeException()