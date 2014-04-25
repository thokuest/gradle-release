package release

/**
 * Provides an easy way to access release configuration properties.
 *
 * @author thokuest, <thokuest@outlook.com>
 */
class ReleaseProps {
    static String username() {
        return System.properties.'username';
    }

    static String password() {
        return System.properties.'password';
    }
}
