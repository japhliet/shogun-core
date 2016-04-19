package de.terrestris.shogun2.service;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import de.terrestris.shogun2.dao.GenericHibernateDao;
import de.terrestris.shogun2.dao.PermissionCollectionDao;
import de.terrestris.shogun2.model.SecuredPersistentObject;
import de.terrestris.shogun2.model.User;
import de.terrestris.shogun2.model.UserGroup;
import de.terrestris.shogun2.model.security.Permission;
import de.terrestris.shogun2.model.security.PermissionCollection;

/**
 * Abstract (parent) test for the
 * {@link AbstractSecuredPersistentObjectServiceTest}.
 *
 * @author Nils Bühner
 *
 */
public abstract class AbstractSecuredPersistentObjectServiceTest<E extends SecuredPersistentObject, D extends GenericHibernateDao<E, Integer>, S extends AbstractSecuredPersistentObjectService<E, D>>
	extends AbstractCrudServiceTest<E, D, S> {

	/**
	 * The service for the {@link PermissionCollection} that will be mocked and
	 * injected to the crud service.
	 */
	protected PermissionCollectionService<PermissionCollection, PermissionCollectionDao<PermissionCollection>> permissionCollectionService;

	@SuppressWarnings("unchecked")
	@Before
	@Override
	public void setUp() {

		// mock permission collection service
		final Class<PermissionCollectionService<PermissionCollection, PermissionCollectionDao<PermissionCollection>>> permissionCollectionServiceClass =
				(Class<PermissionCollectionService<PermissionCollection, PermissionCollectionDao<PermissionCollection>>>)
				new PermissionCollectionService<PermissionCollection, PermissionCollectionDao<PermissionCollection>>().getClass();

		// see here why we are mocking this way:
		// http://stackoverflow.com/a/24302622
		this.permissionCollectionService = mock(permissionCollectionServiceClass);

		// call parent/super, which will init mocks
		super.setUp();

	}

	/**
	 *
	 */
	@Test
	public void addUserPermissions_shouldDoNothingWhenPassedEntityIsNull() {

		Permission permissions = Permission.ADMIN;
		User user = new User("Dummy", "Dummy", "dummy");

		crudService.addUserPermissions(null, user , permissions);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(any(getCrudService().getEntityClass()));

		assertTrue(implToTest.getUserPermissions().keySet().isEmpty());
	}

	/**
	 *
	 */
	@Test
	public void addUserPermissions_shouldDoNothingWhenNoPermissionsHaveBeenPassed() {

		User user = new User("Dummy", "Dummy", "dummy");

		crudService.addUserPermissions(implToTest, user);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		assertTrue(implToTest.getUserPermissions().keySet().isEmpty());
	}

	/**
	 *
	 */
	@Test
	public void addUserPermissions_shouldCreateNewPermissionCollectionWithOneElement() {

		final Permission adminPermission = Permission.ADMIN;
		PermissionCollection permissionCollection = new PermissionCollection();
		permissionCollection.getPermissions().add(adminPermission);

		User user = new User("Dummy", "Dummy", "dummy");

		// be sure that no user permissions are set
		assertTrue(implToTest.getUserPermissions().keySet().isEmpty());

		// mock
		doReturn(permissionCollection).when(permissionCollectionService).saveOrUpdate(any(PermissionCollection.class));
		doNothing().when(dao).saveOrUpdate(implToTest);

		// invoke method to test
		crudService.addUserPermissions(implToTest, user, adminPermission);

		// be sure that the permission collection as well as the entity have been saved
		verify(permissionCollectionService, times(1)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(1)).saveOrUpdate(implToTest);

		// assert that we have permission for exactly one user
		assertEquals(1, implToTest.getUserPermissions().keySet().size());

		// assert that we have set the correct number of permissions
		assertEquals(permissionCollection.getPermissions().size(), implToTest.getUserPermissions().get(user).getPermissions().size());
	}

	/**
	 *
	 */
	@Test
	public void addUserPermissions_shouldCreateNewPermissionCollectionWithMultipleElements() {

		final Permission readPermission = Permission.READ;
		final Permission writePermission = Permission.WRITE;
		final Permission deletePermission = Permission.DELETE;

		PermissionCollection permissionCollection = new PermissionCollection();
		permissionCollection.getPermissions().add(readPermission);
		permissionCollection.getPermissions().add(writePermission);
		permissionCollection.getPermissions().add(deletePermission);

		User user = new User("Dummy", "Dummy", "dummy");

		// be sure that no user permissions are set
		assertTrue(implToTest.getUserPermissions().keySet().isEmpty());

		// mock
		doReturn(permissionCollection).when(permissionCollectionService).saveOrUpdate(any(PermissionCollection.class));
		doNothing().when(dao).saveOrUpdate(implToTest);

		// invoke method to test
		crudService.addUserPermissions(implToTest, user, readPermission, writePermission, deletePermission);

		// be sure that the permission collection as well as the entity have been saved
		verify(permissionCollectionService, times(1)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(1)).saveOrUpdate(implToTest);

		// assert that we have exactly one permission set
		assertEquals(1, implToTest.getUserPermissions().keySet().size());

		// assert that we have set the correct number of permissions
		assertEquals(permissionCollection.getPermissions().size(), implToTest.getUserPermissions().get(user).getPermissions().size());
	}

	/**
	 *
	 */
	@Test
	public void addUserPermissions_shouldAddPermissionToExistingPermissionCollection() {

		final Permission existingPermission = Permission.READ;
		final Permission newPermission = Permission.WRITE;

		PermissionCollection existingPermissionCollection = new PermissionCollection();
		PermissionCollection newPermissionCollection = new PermissionCollection();

		existingPermissionCollection.getPermissions().add(existingPermission);

		newPermissionCollection.getPermissions().add(existingPermission);
		newPermissionCollection.getPermissions().add(newPermission);

		User user = new User("Dummy", "Dummy", "dummy");

		Map<User, PermissionCollection> existingUserPermissionsMap = new HashMap<User, PermissionCollection>();
		existingUserPermissionsMap.put(user, existingPermissionCollection);

		// set existing permissions
		implToTest.setUserPermissions(existingUserPermissionsMap);

		// be sure that our user permission is set
		assertEquals(1, implToTest.getUserPermissions().keySet().size());
		assertEquals(existingPermissionCollection.getPermissions().size(), implToTest.getUserPermissions().get(user).getPermissions().size());

		// mock
		doReturn(newPermissionCollection).when(permissionCollectionService).saveOrUpdate(any(PermissionCollection.class));

		// invoke method to test
		crudService.addUserPermissions(implToTest, user, newPermission);

		// be sure that the permission collection, BUT NOT the entity have been saved
		verify(permissionCollectionService, times(1)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		// assert that we have permission for exactly one user
		assertEquals(1, implToTest.getUserPermissions().keySet().size());

		// assert that we have set the correct number of permissions
		assertEquals(existingPermissionCollection.getPermissions().size(), implToTest.getUserPermissions().get(user).getPermissions().size());
	}

	/**
	 *
	 */
	@Test
	public void removeUserPermissions_shouldDoNothingWhenPassedEntityIsNull() {

		Permission permissions = Permission.ADMIN;
		User user = new User("Dummy", "Dummy", "dummy");

		crudService.removeUserPermissions(null, user , permissions);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(any(getCrudService().getEntityClass()));

		assertTrue(implToTest.getUserPermissions().keySet().isEmpty());
	}

	/**
	 *
	 */
	@Test
	public void removeUserPermissions_shouldDoNothingWhenNoPermissionsHaveBeenPassed() {

		User user = new User("Dummy", "Dummy", "dummy");

		crudService.removeUserPermissions(implToTest, user);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		assertTrue(implToTest.getUserPermissions().keySet().isEmpty());
	}

	/**
	 *
	 */
	@Test
	public void removeUserPermissions_shouldDoNothingWhenNoPermissionsExist() {

		final Permission writePermission = Permission.WRITE;

		User user = new User("Dummy", "Dummy", "dummy");

		crudService.removeUserPermissions(implToTest, user, writePermission);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		assertEquals(0, implToTest.getUserPermissions().keySet().size());
	}

	/**
	 *
	 */
	@Test
	public void removeUserPermissions_shouldRemoveExistingPermission() {

		final Permission readPermission = Permission.READ;
		final Permission writePermission = Permission.WRITE;

		PermissionCollection existingPermissionCollection = new PermissionCollection();

		existingPermissionCollection.getPermissions().add(readPermission);
		existingPermissionCollection.getPermissions().add(writePermission);

		User user = new User("Dummy", "Dummy", "dummy");

		Map<User, PermissionCollection> existingUserPermissionsMap = new HashMap<User, PermissionCollection>();
		existingUserPermissionsMap.put(user, existingPermissionCollection);

		implToTest.setUserPermissions(existingUserPermissionsMap);

		crudService.removeUserPermissions(implToTest, user, writePermission);

		// be sure that the permission collection has been updated
		verify(permissionCollectionService, times(1)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		assertEquals(1, implToTest.getUserPermissions().keySet().size());
	}

	/**
	 *
	 */
	@Test
	public void addGroupPermissions_shouldDoNothingWhenPassedEntityIsNull() {

		Permission permissions = Permission.ADMIN;
		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		crudService.addGroupPermissions(null, userGroup , permissions);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(any(getCrudService().getEntityClass()));

		assertTrue(implToTest.getGroupPermissions().keySet().isEmpty());
	}

	/**
	 *
	 */
	@Test
	public void addGroupPermissions_shouldDoNothingWhenNoPermissionsHaveBeenPassed() {

		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		crudService.addGroupPermissions(implToTest, userGroup);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		assertTrue(implToTest.getGroupPermissions().keySet().isEmpty());
	}

	/**
	 *
	 */
	@Test
	public void addGroupPermissions_shouldCreateNewPermissionCollectionWithOneElement() {

		final Permission adminPermission = Permission.ADMIN;
		PermissionCollection permissionCollection = new PermissionCollection();
		permissionCollection.getPermissions().add(adminPermission);

		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		// be sure that no user permissions are set
		assertTrue(implToTest.getGroupPermissions().keySet().isEmpty());

		// mock
		doReturn(permissionCollection).when(permissionCollectionService).saveOrUpdate(any(PermissionCollection.class));
		doNothing().when(dao).saveOrUpdate(implToTest);

		// invoke method to test
		crudService.addGroupPermissions(implToTest, userGroup, adminPermission);

		// be sure that the permission collection as well as the entity have been saved
		verify(permissionCollectionService, times(1)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(1)).saveOrUpdate(implToTest);

		// assert that we have permission for exactly one user
		assertEquals(1, implToTest.getGroupPermissions().keySet().size());

		// assert that we have set the correct number of permissions
		assertEquals(permissionCollection.getPermissions().size(), implToTest.getGroupPermissions().get(userGroup).getPermissions().size());
	}

	/**
	 *
	 */
	@Test
	public void addGroupPermissions_shouldCreateNewPermissionCollectionWithMultipleElements() {

		final Permission readPermission = Permission.READ;
		final Permission writePermission = Permission.WRITE;
		final Permission deletePermission = Permission.DELETE;

		PermissionCollection permissionCollection = new PermissionCollection();
		permissionCollection.getPermissions().add(readPermission);
		permissionCollection.getPermissions().add(writePermission);
		permissionCollection.getPermissions().add(deletePermission);

		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		// be sure that no user permissions are set
		assertTrue(implToTest.getGroupPermissions().keySet().isEmpty());

		// mock
		doReturn(permissionCollection).when(permissionCollectionService).saveOrUpdate(any(PermissionCollection.class));
		doNothing().when(dao).saveOrUpdate(implToTest);

		// invoke method to test
		crudService.addGroupPermissions(implToTest, userGroup, readPermission, writePermission, deletePermission);

		// be sure that the permission collection as well as the entity have been saved
		verify(permissionCollectionService, times(1)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(1)).saveOrUpdate(implToTest);

		// assert that we have exactly one permission set
		assertEquals(1, implToTest.getGroupPermissions().keySet().size());

		// assert that we have set the correct number of permissions
		assertEquals(permissionCollection.getPermissions().size(), implToTest.getGroupPermissions().get(userGroup).getPermissions().size());
	}

	/**
	 *
	 */
	@Test
	public void addGroupPermissions_shouldAddPermissionToExistingPermissionCollection() {

		final Permission existingPermission = Permission.READ;
		final Permission newPermission = Permission.WRITE;

		PermissionCollection existingPermissionCollection = new PermissionCollection();
		PermissionCollection newPermissionCollection = new PermissionCollection();

		existingPermissionCollection.getPermissions().add(existingPermission);

		newPermissionCollection.getPermissions().add(existingPermission);
		newPermissionCollection.getPermissions().add(newPermission);

		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		Map<UserGroup, PermissionCollection> existingGroupPermissionsMap = new HashMap<UserGroup, PermissionCollection>();
		existingGroupPermissionsMap.put(userGroup, existingPermissionCollection);

		// set existing permissions
		implToTest.setGroupPermissions(existingGroupPermissionsMap);

		// be sure that our user permission is set
		assertEquals(1, implToTest.getGroupPermissions().keySet().size());
		assertEquals(existingPermissionCollection.getPermissions().size(), implToTest.getGroupPermissions().get(userGroup).getPermissions().size());

		// mock
		doReturn(newPermissionCollection).when(permissionCollectionService).saveOrUpdate(any(PermissionCollection.class));

		// invoke method to test
		crudService.addGroupPermissions(implToTest, userGroup, newPermission);

		// be sure that the permission collection, BUT NOT the entity have been saved
		verify(permissionCollectionService, times(1)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		// assert that we have permission for exactly one user
		assertEquals(1, implToTest.getGroupPermissions().keySet().size());

		// assert that we have set the correct number of permissions
		assertEquals(existingPermissionCollection.getPermissions().size(), implToTest.getGroupPermissions().get(userGroup).getPermissions().size());
	}

	/**
	 *
	 */
	@Test
	public void removeGroupPermissions_shouldDoNothingWhenPassedEntityIsNull() {

		Permission permissions = Permission.ADMIN;

		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		crudService.removeGroupPermissions(null, userGroup , permissions);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(any(getCrudService().getEntityClass()));

		assertTrue(implToTest.getGroupPermissions().keySet().isEmpty());
	}

	/**
	 *
	 */
	@Test
	public void removeGroupPermissions_shouldDoNothingWhenNoPermissionsHaveBeenPassed() {

		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		crudService.removeGroupPermissions(implToTest, userGroup);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		assertTrue(implToTest.getGroupPermissions().keySet().isEmpty());
	}

	/**
	 *
	 */
	@Test
	public void removeGroupPermissions_shouldDoNothingWhenNoPermissionsExist() {

		final Permission writePermission = Permission.WRITE;

		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		crudService.removeGroupPermissions(implToTest, userGroup, writePermission);

		// be sure that nothing happened
		verify(permissionCollectionService, times(0)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		assertEquals(0, implToTest.getGroupPermissions().keySet().size());
	}

	/**
	 *
	 */
	@Test
	public void removeGroupPermissions_shouldRemoveExistingPermission() {

		final Permission readPermission = Permission.READ;
		final Permission writePermission = Permission.WRITE;

		PermissionCollection existingPermissionCollection = new PermissionCollection();

		existingPermissionCollection.getPermissions().add(readPermission);
		existingPermissionCollection.getPermissions().add(writePermission);

		UserGroup userGroup = new UserGroup();
		userGroup.setName("test");

		Map<UserGroup, PermissionCollection> existingGroupPermissionsMap = new HashMap<UserGroup, PermissionCollection>();
		existingGroupPermissionsMap.put(userGroup, existingPermissionCollection);

		implToTest.setGroupPermissions(existingGroupPermissionsMap);

		crudService.removeGroupPermissions(implToTest, userGroup, writePermission);

		// be sure that the permission collection has been updated
		verify(permissionCollectionService, times(1)).saveOrUpdate(any(PermissionCollection.class));
		verify(dao, times(0)).saveOrUpdate(implToTest);

		assertEquals(1, implToTest.getGroupPermissions().keySet().size());
	}

}