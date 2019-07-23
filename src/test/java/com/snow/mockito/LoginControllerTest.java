package com.snow.mockito;

import com.snow.login.LoginController;
import com.snow.login.LoginDao;
import com.snow.login.LoginService;
import com.snow.login.UserForm;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

/**
 * 该测绘的目标就是测试LoginController中的login方法。
 * 该login方法中做了三件事：
 * 1. 如果输入的UserForm是null，则不用调用login，和setCurrentUser了。
 * 2. 如果UserForm是合法的，则先调用LoginService的login方法，该方法可能抛异常，可以返回boolean型
 * 3. 如果LoginService的login方法没有抛异常，则会在返回true的情况下调用setCurrentUser方法
 * 总之就是想尽一切办法做个假的LoginService来保证LoginController是可以测的。
 *
 * 为此测试上面的login方法，我们设计了如下的测试用例
 * 1. 传入非法的UserForm，使得LoginService的任何一个方法都不调用
 * a case in which no calls to the mocked service will be done.
 * 2. 传入合法的UserForm，只允许LoginService的一个方法被调用
 * a case in which only one method will be called.
 * 3. 传入合法且有效的UserForm，保证LoginService的所有的方法都被调用
 * a case in which all methods will be called.
 * 4. 传入合法且合适的UserForm，使得LoginService的方法抛出异常
 * a case in which exception throwing will be tested.
 */
public class LoginControllerTest {


    /**
     * 加上@Mock表示要全面的做个假的LoginDao, 再使用该类，其中的方法也不会被真正调用。
     */
    @Mock
    private LoginDao loginDao;

    /**
     * 加上@Spy表示该类会被部分mock，也就是说这个类是可以部分被信任的。其中的部分方法是会被真正调用的。
     */
    @Spy
    @InjectMocks
    private LoginService spiedLoginService;

    @Mock
    private LoginService loginService;

    /**
     * @InjectMocks表示它所修饰的对象的所有注入元素都是假的，而它自己却是真的。
     */
    @InjectMocks
    private LoginController loginController;

    @Before
    public void setup() {
        loginController = new LoginController();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void assertThatNoMethodHasBeenCalled() {
        String returnValue = loginController.login(null);
        System.out.println(returnValue);

        //LoginController的login方法的输入一旦是非法的，则根本无法走到调用LoginService方法的地方。
        //因此，该处验证一下那个假冒的loginService根本就没有被调用过就可以了。
        Mockito.verifyZeroInteractions(loginService);
    }

    @Test
    public void assertTwoMethodsHaveBeenCalled() {
        UserForm userForm = new UserForm();
        userForm.setUsername("foo");
        //先构建了一个合法的输入，然后mock了一下loginService的login方法的输出，确保其输出是合法有效的。
        Mockito.when(loginService.login(userForm)).thenReturn(true);

        //使得loginController的login方法被真实调用，只是当该方法中需要调用loginService.login方法时，直接用假的返回值代替。
        String login = loginController.login(userForm);

        //验证controller的返回值是否符合预期
        Assert.assertEquals("SUCCESS", login);

        //验证假的loginService中的两个方法是不是都被调用到了。
        //请注意，这里的两个方法都是假的，所有需要mock.verify来验证。如果是真实调用到了，直接用assert验证就可以了。
        Mockito.verify(loginService).login(userForm);
        Mockito.verify(loginService).setCurrentUser("foo");
    }

    @Test
    public void assertOnlyOneMethodHasBeenCalled() {
        UserForm userForm = new UserForm();
        userForm.setUsername("foo");
        Mockito.when(loginService.login(userForm)).thenReturn(false);

        String login = loginController.login(userForm);

        Assert.assertEquals("FAIL", login);

        Mockito.verify(loginService).login(userForm);
        //验证除了上面那个假的方法之外，是不是其他方法都没有被调用到。
        Mockito.verifyNoMoreInteractions(loginService);
    }

    @Test
    public void mockExceptionThrowin() {
        UserForm userForm = new UserForm();
        Mockito.when(loginService.login(userForm)).thenThrow(IllegalArgumentException.class);

        String login = loginController.login(userForm);

        Assert.assertEquals("ERROR", login);
        Mockito.verify(loginService).login(userForm);
        Mockito.verifyNoMoreInteractions(loginService);
        //Mockito.verifyZeroInteractions(loginService);
    }

    /**
     * 这是演示直接mock一个对象的方式，这个对象可以不是以@Mock的注解的方式来创建
     */
    @Test
    public void mockAnObjectToPassAround() {
        UserForm userForm = Mockito.when(Mockito.mock(UserForm.class).getUsername())
                .thenReturn("foo").getMock();
        Mockito.when(loginService.login(userForm)).thenReturn(true);

        String login = loginController.login(userForm);

        Assert.assertEquals("SUCCESS", login);
        Mockito.verify(loginService).login(userForm);
        Mockito.verify(loginService).setCurrentUser("foo");
    }

    @Test
    public void argumentMatching() {
        UserForm userForm = new UserForm();
        userForm.setUsername("foo");
        // default matcher
        Mockito.when(loginService.login(Mockito.any(UserForm.class))).thenReturn(true);

        String login = loginController.login(userForm);

        Assert.assertEquals("SUCCESS", login);
        Mockito.verify(loginService).login(userForm);
        // complex matcher
        Mockito.verify(loginService).setCurrentUser(ArgumentMatchers.argThat(
                argument -> argument.startsWith("foo")
        ));
    }

    @Test
    public void partialMocking() {
        // 通过显性的赋值直接把loginController所依赖的loginService放进去
        //这个loginService不像上面的那个是完全不可信任的，这个是可以部分信任的
        loginController.loginService = spiedLoginService;
        UserForm userForm = new UserForm();
        userForm.setUsername("foo");
        // let service's login use implementation so let's mock DAO call
        Mockito.when(loginDao.login(userForm)).thenReturn(1);

        String login = loginController.login(userForm);

        Assert.assertEquals("SUCCESS", login);
        // verify mocked call
        Mockito.verify(spiedLoginService).setCurrentUser("foo");
    }
}
