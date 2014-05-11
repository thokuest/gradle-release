package release.helper

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging


class Validate {
    private static final Logger log = Logging.getLogger(Validate.class)

    static void warnOrFail(def condition, def message) {
        if (condition) {
            fail(message)
        }

        warn(message)
    }

    static void warn(def message) {
        log.warn("!!WARNING!! $message")
    }

    static void warn(Closure c) {
        log.warn("!!WARNING!! ${c()}")
    }

    static void fail(def message) {
        throw new GradleException(message);
    }

    static void fail(Closure c) {
        throw new GradleException(c());
    }

    static void assertTrue(def condition, def message) {
        if (!condition) {
            fail(message)
        }
    }
}
