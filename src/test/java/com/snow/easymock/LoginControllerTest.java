package com.snow.easymock;

import com.snow.login.LoginController;
import com.snow.login.LoginDao;
import com.snow.login.LoginService;
import com.snow.login.UserForm;
import org.easymock.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class LoginControllerTest {

    @Mock
    private LoginDao loginDao;

    @Mock
    private LoginService loginService;

    @TestSubject
    private LoginController loginController = new LoginController();

    @Test
    public void assertThatNoMethodHasBeenCalled() {
        EasyMock.replay(loginService);
        loginController.login(null);
        EasyMock.verify(loginService);
    }

    @Test
    public void assertTwoMethodsHaveBeenCalled() {
        UserForm userForm = new UserForm();
        userForm.setUsername("foo");
        EasyMock.expect(loginService.login(userForm)).andReturn(true);
        loginService.setCurrentUser("foo");
        EasyMock.replay(loginService);

        String login = loginController.login(userForm);

        Assert.assertEquals("SUCCESS", login);
        EasyMock.verify(loginService);
    }

    @Test
    public void assertOnlyOneMethodHasBeenCalled() {
        UserForm userForm = new UserForm();
        userForm.setUsername("foo");
        EasyMock.expect(loginService.login(userForm)).andReturn(false);
        EasyMock.replay(loginService);

        String login = loginController.login(userForm);

        Assert.assertEquals("FAIL", login);
        EasyMock.verify(loginService);
    }

    @Test
    public void mockExceptionThrowing() {
        UserForm userForm = new UserForm();
        EasyMock.expect(loginService.login(userForm)).andThrow(new IllegalArgumentException());
        EasyMock.replay(loginService);

        String login = loginController.login(userForm);

        Assert.assertEquals("ERROR", login);
        EasyMock.verify(loginService);
    }

    @Test
    public void mockAnObjectToPassAround() {
        UserForm userForm = EasyMock.mock(UserForm.class);
        EasyMock.expect(userForm.getUsername()).andReturn("foo");
        EasyMock.expect(loginService.login(userForm)).andReturn(true);
        loginService.setCurrentUser("foo");
        EasyMock.replay(userForm);
        EasyMock.replay(loginService);

        String login = loginController.login(userForm);

        Assert.assertEquals("SUCCESS", login);
        EasyMock.verify(userForm);
        EasyMock.verify(loginService);
    }

    @Test
    public void argumentMatching() {
        UserForm userForm = new UserForm();
        userForm.setUsername("foo");
        // default matcher
        EasyMock.expect(loginService.login(EasyMock.isA(UserForm.class))).andReturn(true);
        // complex matcher
        loginService.setCurrentUser(specificArgumentMatching("foo"));
        EasyMock.replay(loginService);

        String login = loginController.login(userForm);

        Assert.assertEquals("OK", login);
        EasyMock.verify(loginService);
    }

    private static String specificArgumentMatching(String expected) {
        EasyMock.reportMatcher(new IArgumentMatcher() {
            @Override
            public boolean matches(Object argument) {
                return argument instanceof String
                        && ((String) argument).startsWith(expected);
            }

            @Override
            public void appendTo(StringBuffer buffer) {
                //NOOP
            }
        });
        return null;
    }

    /**
     * Partial mocking also gets a little more complicated with EasyMock,
     * as you need to define which methods will be mocked when creating the mock.
     *
     * This is done with EasyMock.partialMockBuilder(Class.class).addMockedMethod(“methodName”).createMock().
     * Once this is done, you can use the mock as any other non-partial mock.
     */
    @Test
    public void partialMocking() {
        UserForm userForm = new UserForm();
        userForm.setUsername("foo");
        // use partial mock
        LoginService loginServicePartial = EasyMock.partialMockBuilder(LoginService.class)
                .addMockedMethod("setCurrentUser").createMock();
        loginServicePartial.setCurrentUser("foo");
        // let service's login use implementation so let's mock DAO call
        EasyMock.expect(loginDao.login(userForm)).andReturn(1);

        loginServicePartial.setLoginDao(loginDao);
        loginController.loginService = loginServicePartial;

        EasyMock.replay(loginDao);
        EasyMock.replay(loginServicePartial);

        String login = loginController.login(userForm);

        Assert.assertEquals("SUCCESS", login);
        // verify mocked call
        EasyMock.verify(loginServicePartial);
        EasyMock.verify(loginDao);
    }

}
