package is.yaks.test;

import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;

import is.yaks.*;

public class SelectorTest {

    private void testSelector(String s, String expPath, String expPredicate, String expFragment) {
        try {
            Selector sel = new Selector(s);
            Assert.assertEquals("Selector for "+s+" has unexpected path: "+sel.getPath(),
                expPath, sel.getPath());
            Assert.assertEquals("Selector for "+s+" has unexpected predicate: "+sel.getPredicate(),
                expPredicate, sel.getPredicate());
            Assert.assertEquals("Selector for "+s+" has unexpected fragment: "+sel.getFragment(),
            expFragment, sel.getFragment());
        } catch (Throwable e) {
            e.printStackTrace();
            Assert.fail(e.toString());
        }

    }

    @Test
    public final void testSelectors() {
        testSelector("/a/b/c", "/a/b/c", "", "");
        testSelector("/a/b/c?xyz", "/a/b/c", "xyz", "");
        testSelector("/a/b/c#xyz", "/a/b/c", "", "xyz");
        testSelector("/a/b/c?ghi?xyz", "/a/b/c", "ghi?xyz", "");
        testSelector("/a/b/c#ghi#xyz", "/a/b/c", "", "ghi#xyz");
        testSelector("/a/b/c?ghi#xyz", "/a/b/c", "ghi", "xyz");
        testSelector("/a/b/c#ghi?xyz", "/a/b/c", "", "ghi?xyz");
        testSelector("/*/b/**", "/*/b/**", "", "");
    } 

}
