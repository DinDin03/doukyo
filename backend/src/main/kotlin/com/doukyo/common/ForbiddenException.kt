package com.doukyo.common

// Signed in, but not allowed: you aren't a member of that household. Kept apart
// from UnauthorizedException because the app signs you out on UNAUTHORIZED, and
// a stale request for a household you just left must not do that.
class ForbiddenException(message: String) : RuntimeException(message)
