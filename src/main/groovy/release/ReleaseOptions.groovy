package release

/**
 * Provides an easy way to access release configuration properties defined on
 * the command line.
 *
 * @author thokuest
 */
class ReleaseOptions {
    static String username() {
        return System.properties.'username'
    }

    static String password() {
        return System.properties.'password'
    }

    static boolean nonInteractive() {
        return System.properties.containsKey("nonInteractive") && !"false".equalsIgnoreCase(System.properties.'nonInteractive');
    }

    static String releaseVersion() {
        return System.properties.'releaseVersion'
    }

    static String developmentVersion() {
        return System.properties.'developmentVersion'
    }

    static String branchVersion() {
        return System.properties.'branchVersion'
    }

    static String branchName() {
        return System.properties.'branchName'
    }
}