package tectonicus;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThan;

import org.junit.jupiter.api.Test;

public class VersionTests {
    @Test
    void compareTwoVersions() {
        int test = Version.VERSION_RV.compareTo(Version.VERSIONS_6_TO_8);
        assertThat(test, is(lessThan(0)));
    }
}
