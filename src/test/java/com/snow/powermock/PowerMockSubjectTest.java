package com.snow.powermock;

import com.snow.login.PowerMockSubject;
import com.snow.login.PowerMockSubjectCaller;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PowerMockSubject.class)
public class PowerMockSubjectTest {

    @Test
    public void mockStaticMethod() {
        mockStatic(PowerMockSubject.class);
        when(PowerMockSubject.staticMethod(Mockito.anyString()))
                .thenReturn("12345 static");
        String actual = new PowerMockSubjectCaller()
                .callStaticMethod("12345");
        Assert.assertEquals("12345 static", actual);
    }

    /**
     * 构造函数的作用就是构造一个新的对象
     * 以File为例子 File f = new File（“/asdf/asdf/aa.ext”）
     */
    @Test
    public void mockConstructor() throws Exception {
        File f = mock(File.class);
        whenNew(File.class).withArguments("/asdf/asdf/aa.ext").thenReturn(f);
        when(f.exists()).thenReturn(true);
        when(f.getName()).thenReturn("aa.ext");


        String actual = new PowerMockSubject("", 0)
                .callConstructor("/asdf/asdf/aa.ext");

        Assert.assertEquals("aa.ext", actual);

        verifyNew(File.class).withArguments("/asdf/asdf/aa.ext");
    }

    /**
     * 一般情况下private方法都是类里边其他的public方法调用的。为了规避私有方法对public方法的影响，需要将private方法mock一下。
     */
    @Test
    public void mockPrivateMethod() throws Exception {
        PowerMockSubject pms = spy(new PowerMockSubject("", 0));
        when(pms, "calculateB", -11213).thenReturn(99);

        int actual = pms.callPrivateMethod(-11213);

        Assert.assertEquals(99, actual);

        verifyPrivate(pms).invoke("calculateB", -11213);
    }

}
