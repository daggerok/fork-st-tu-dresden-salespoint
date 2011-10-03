package test.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.salespointframework.core.database.Database;
import org.salespointframework.core.user.PersistentUserManager;
import org.salespointframework.core.user.UserCapability;
import org.salespointframework.core.user.UserIdentifier;

/**
 * Test if some Date is or is not in the Database Use of the test: 1. ensure
 * that in persistence.xml method is on DROP AND CREATE TABLES 2. run
 * UsermangerTest, must be successful 3. change methode persistence.xml to only
 * CREATE or (none) 4. run this Test
 * 
 * @author Christopher Bellmann
 * 
 */
@SuppressWarnings("javadoc")
public class UsermangerPersistenceTest {

	private static final PersistentUserManager userManager = new PersistentUserManager(); ;

	@BeforeClass
	public static void setUp() {
		Database.INSTANCE.initializeEntityManagerFactory("SalespointFramework");
	}

	@Test
	public void testHasEmployeeE3AndHasPW() {
		UserIdentifier ui3 = new UserIdentifier();
		Employee e3 = new Employee(ui3, "lala");

		userManager.add(e3);

		assertEquals(
				userManager.get(Employee.class, e3.getIdentifier()), e3);
		assertEquals(
				userManager.get(Employee.class, e3.getIdentifier())
						.verifyPassword("lala"), true);
	}

	@Test
	public void testE3HasCapability() {
		UserIdentifier ui3 = new UserIdentifier();
		Employee e3 = new Employee(ui3, "lala");
		UserCapability capa2 = new UserCapability(
				"MustBeInDataBaseAfterTesting");


		userManager.add(e3);
		e3.addCapability(capa2);

		assertTrue(e3.hasCapability(capa2));

	}

	@Test
	public void testE3HasNOTCapability() {
		UserIdentifier ui3 = new UserIdentifier();
		Employee e3 = new Employee(ui3, "lala");
		UserCapability capa = new UserCapability("CrazyTestCapabilityAgain");

		userManager.add(e3);

		assertFalse(e3.hasCapability(capa));
	}

}
