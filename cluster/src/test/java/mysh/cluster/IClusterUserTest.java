package mysh.cluster;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

/**
 * IClusterUserTest
 *
 * @author 凯泓(zhixian.zzx@alibaba-inc.com)
 * @since 2017/12/03
 */
public class IClusterUserTest {
    @Test
    public void split() {
        List<List<Integer>> s3 = IClusterUser.split(Arrays.asList(1, 2, 3, 4), 3);
        Assert.assertEquals(3, s3.size());

        List<List<Integer>> s5 = IClusterUser.split(Arrays.asList(1, 2, 3, 4), 5);
        Assert.assertEquals(4, s5.size());
    }
}