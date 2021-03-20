package mysh.cluster;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * IClusterUserTest
 *
 * @since 2017/12/03
 */
public class IClusterUserTest {
    @Test
    public void split() {
        List<List<Integer>> s3 = IClusterUser.split(Arrays.asList(1, 2, 3, 4), 3);
        Assertions.assertEquals(3, s3.size());

        List<List<Integer>> s5 = IClusterUser.split(Arrays.asList(1, 2, 3, 4), 5);
        Assertions.assertEquals(4, s5.size());
    }
}