package demo.action;

import li.annotation.Inject;
import li.test.BaseActionTest;

import org.junit.Before;
import org.junit.Test;

import demo.action.AccountAction;

import static org.junit.Assert.*;

public class AccountActionTest extends BaseActionTest {
	@Inject
	AccountAction accountAction;

	@Before
	public void before() {
		assertNotNull(accountAction);
	}

	@Test
	public void listTest() {
	}
}