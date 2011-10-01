package org.salespointframework.core.user;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.salespointframework.core.database.Database;
import org.salespointframework.util.ArgumentNullException;
import org.salespointframework.util.Iterables;
import org.salespointframework.util.Objects;

/**
 * @author Christopher Bellmann
 * @author Paul Henke
 * @author Hannes Weissbach
 */

public class PersistentUserManager implements UserManager<PersistentUser>
{
	private final EntityManagerFactory emf = Database.INSTANCE.getEntityManagerFactory();

	@Override
	public void add(PersistentUser user)
	{
		Objects.requireNonNull(user, "user");

		EntityManager em = emf.createEntityManager();
		if (em.find(PersistentUser.class, user.getIdentifier()) != null)
		{
			throw new DuplicateUserException(user.getIdentifier());
		}
		em.persist(user);
		beginCommit(em);
	}

	@Override
	public boolean remove(UserIdentifier userIdentifer)
	{
		Objects.requireNonNull(userIdentifer, "userIdentifer");

		// If user is logged on, log him off.
		for (Map.Entry<Object, PersistentUser> entry : userTokenMap.entrySet())
		{
			if (entry.getValue().getIdentifier().equals(userIdentifer))
			{
				Object token = entry.getKey();
				this.logOff(token);
				break;
			}
		}

		EntityManager em = emf.createEntityManager();
		Object user = em.find(PersistentUser.class, userIdentifer);
		if (user != null)
		{
			em.remove(user);
			beginCommit(em);
			return true;
		} else
		{
			return false;
		}
	}

	@Override
	public boolean contains(UserIdentifier userIdentifier)
	{
		Objects.requireNonNull(userIdentifier, "userIdentifier");

		EntityManager em = emf.createEntityManager();
		return em.find(PersistentUser.class, userIdentifier) != null;
	}

	@Override
	public <T extends PersistentUser> Iterable<T> find(Class<T> clazz)
	{
		Objects.requireNonNull(clazz, "clazz");

		EntityManager em = emf.createEntityManager();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> q = cb.createQuery(clazz);
		TypedQuery<T> tq = em.createQuery(q.where(q.from(clazz).type().in(clazz)));

		return Iterables.from(tq.getResultList());
	}

	@Override
	public <T extends PersistentUser> T get(Class<T> clazz, UserIdentifier userIdentifier)
	{
		Objects.requireNonNull(clazz, "clazz");
		Objects.requireNonNull(userIdentifier, "userIdentifier");

		EntityManager em = emf.createEntityManager();
		return em.find(clazz, userIdentifier);
	}

	/**
	 * Updates and persists an existing {@link PersistentUser} to the PersistentUserManager and the Database
	 * @param user the {@link PersistentUser} to be updated
	 * @throws ArgumentNullException if user is null
	 */
	public void update(PersistentUser user)
	{
		Objects.requireNonNull(user, "user");

		EntityManager em = emf.createEntityManager();
		em.merge(user);
		beginCommit(em);
	}

	private final Map<Object, PersistentUser> userTokenMap = new ConcurrentHashMap<Object, PersistentUser>();

	// TODO naming kinda sucks

	// associates a user with a token
	@Override
	public final boolean logOn(PersistentUser user, Object token)
	{
		Objects.requireNonNull(user, "user");
		Objects.requireNonNull(token, "token");

		PersistentUser temp = this.get(PersistentUser.class, user.getIdentifier());

		if (temp == null)
		{
			return false;
		} else {
			userTokenMap.put(token, user);
			return true;
		}
	}

	@Override
	public final void logOff(Object token)
	{
		Objects.requireNonNull(token, "token");

		userTokenMap.remove(token);
	}

	@Override
	public final <T extends PersistentUser> T getUserByToken(Class<T> clazz, Object token)
	{
		Objects.requireNonNull(clazz, "clazz");
		Objects.requireNonNull(token, "token");

		PersistentUser user = userTokenMap.get(token);
		if (clazz.isInstance(user))
		{
			return clazz.cast(user);
		} else
		{
			throw new ClassCastException();
		}
	}

	private void beginCommit(EntityManager entityManager)
	{
		entityManager.getTransaction().begin();
		entityManager.getTransaction().commit();
	}
}
