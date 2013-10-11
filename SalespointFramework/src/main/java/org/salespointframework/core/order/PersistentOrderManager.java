package org.salespointframework.core.order;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.joda.time.DateTime;
import org.salespointframework.core.catalog.ProductIdentifier;
import org.salespointframework.core.inventory.Inventory;
import org.salespointframework.core.inventory.InventoryItem;
import org.salespointframework.core.order.OrderCompletionResult.OrderCompletionStatus;
import org.salespointframework.core.quantity.Quantity;
import org.salespointframework.core.user.UserIdentifier;
import org.salespointframework.util.Iterables;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 
 * @author Thomas Dedek
 * @author Paul Henke
 * 
 */
@Component
@Transactional
class PersistentOrderManager implements OrderManager
{

	private final Inventory inventory;
	private final TransactionTemplate txTemplate;

	@PersistenceContext
	private EntityManager em;
	
	/**
	 * creates a new PersistentOrderManager
	 */
	@Autowired
	public PersistentOrderManager(Inventory inventory, PlatformTransactionManager txManager) {
		this.inventory = Objects.requireNonNull(inventory, "Inventory must not be null!");
		this.txTemplate = new TransactionTemplate(txManager);
	}
	
	@Override
	public void add(Order order)
	{
		Objects.requireNonNull(order, "order must be not null");
		em.persist(order);
	}
	
	/**
	 * Adds multiple {@link Order}s to this PersistentOrderManager
	 * 
	 * @param orders
	 *            an {@link Iterable} of {@link Order}s to be added
	 * 
	 * @throws NullPointerException if orders is null
	 */
	public void addAll(Iterable<? extends Order> orders) {
		Objects.requireNonNull(orders, "orders must not be null");
		for(Order order : orders) {
			em.persist(order);
		}
	}
	

	@Override
	public final <T extends Order> T get(Class<T> clazz, OrderIdentifier orderIdentifier)
	{
		Objects.requireNonNull(orderIdentifier, "orderIdentifier must not be null");
		return em.find(clazz, orderIdentifier);
	}

	@Override
	public final boolean contains(OrderIdentifier orderIdentifier)
	{
		Objects.requireNonNull(orderIdentifier, "orderIdentifier must not be null");
		return em.find(Order.class, orderIdentifier) != null;
	}

	@Override
	public final <T extends Order> Iterable<T> find(Class<T> clazz, DateTime from, DateTime to)
	{
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(clazz);
		Root<T> entry = cq.from(clazz);
		cq.where(cb.between(entry.get(Order_.dateCreated), from.toDate(), to.toDate()));
		TypedQuery<T> tq = em.createQuery(cq);

		return Iterables.of(tq.getResultList());
	}

	@Override
	public final <T extends Order> Iterable<T> find(Class<T> clazz, OrderStatus orderStatus)
	{
		Objects.requireNonNull(clazz, "clazz must not be null");
		Objects.requireNonNull(orderStatus, "orderStatus must not be null");

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(clazz);
		Root<T> entry = cq.from(clazz);
		cq.where(cb.equal(entry.get(Order_.orderStatus), orderStatus));
		TypedQuery<T> tq = em.createQuery(cq);

		return Iterables.of(tq.getResultList());
	}

	@Override
	public final <T extends Order> Iterable<T> find(Class<T> clazz, UserIdentifier userIdentifier)
	{
		Objects.requireNonNull(clazz, "clazz must not be null");
		Objects.requireNonNull(userIdentifier, "userIdentifier must not be null");

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(clazz);
		Root<T> entry = cq.from(clazz);
		cq.where(cb.equal(entry.get(Order_.userIdentifier), userIdentifier));
		TypedQuery<T> tq = em.createQuery(cq);

		return Iterables.of(tq.getResultList());
	}

	@Override
	public final <T extends Order> Iterable<T> find(Class<T> clazz, UserIdentifier userIdentifier, DateTime from, DateTime to)
	{
		Objects.requireNonNull(userIdentifier, "userIdentifier must not be null");
		Objects.requireNonNull(from, "from must not be null");
		Objects.requireNonNull(to, "to must not be null");

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(clazz);
		Root<T> entry = cq.from(clazz);
		Predicate p1 = cb.equal(entry.get(Order_.userIdentifier), userIdentifier);
		Predicate p2 = cb.between(entry.get(Order_.dateCreated), from.toDate(), to.toDate());
		cq.where(p1, p2);
		TypedQuery<T> tq = em.createQuery(cq);

		return Iterables.of(tq.getResultList());
	}

	/**
	 * Updates and persists an existing {@link Order} to the PersistentOrderManager and the Database
	 * @param order the {@link Order} to be updated
	 * @throws NullPointerException if order is null
	 */
	@Override
	public final void update(Order order)
	{
		Objects.requireNonNull(order, "order must not be null");
		em.merge(order);
	}
	

	@Override
	public OrderCompletionResult completeOrder(final Order order) {

		final Map<InventoryItem, Quantity> goodItems = new HashMap<>();
		final Map<InventoryItem, Quantity> badItems = new HashMap<>();

		Iterable<OrderLine> lineItems = order.getOrderLines();
		
		for (OrderLine orderline : lineItems) {
			ProductIdentifier productIdentifier = orderline
					.getProductIdentifier();
			InventoryItem inventoryItem = inventory
					.getByProductIdentifier(InventoryItem.class,
							productIdentifier);

			// TODO was machen wenn nicht im Inventar
			if (inventoryItem == null) {
				System.out.println("No InventoryItem with given ProductIndentifier found in PersistentInventory. Have you initialized your PersistentInventory? Do you need to re-stock your Inventory?");
				break;
			}
			if (!inventoryItem.getQuantity().subtract(orderline.getQuantity())
					.isNegative()) {
				goodItems.put(inventoryItem, orderline.getQuantity());
			} else {
				badItems.put(inventoryItem, orderline.getQuantity());
			}
		}
		
		return txTemplate.execute(new TransactionCallback<OrderCompletionResult>() {
			
			@Override
			public OrderCompletionResult doInTransaction(TransactionStatus status) {

				if (goodItems.size() != order.getNumberOfLineItems()) {
					
					status.setRollbackOnly();
					
					System.out.println("Number of items requested by the OrderLine is greater than the number available in the Inventory. Please re-stock.");
					return new InternalOrderCompletionResult(
							OrderCompletionStatus.FAILED);
				}
				
				System.out.println("Number of items requested by the OrderLine removed from the Inventory.");
				
				boolean failed = false;
				
				for (InventoryItem inventoryItem : goodItems.keySet()) {
					Quantity quantity = goodItems.get(inventoryItem);
					inventoryItem.decreaseQuantity(quantity);
					if (inventoryItem.getQuantity().isNegative()) {
						failed = true;
						break;
					}
				}

				// TODO DRY IT
				if (failed) {
					
					status.setRollbackOnly();
					return new InternalOrderCompletionResult(
							OrderCompletionStatus.FAILED);
				}
				
				order.complete();
				return new InternalOrderCompletionResult(
						OrderCompletionStatus.SUCCESSFUL);
			}
		});
	}
	
	private final class InternalOrderCompletionResult implements OrderCompletionResult {
		private final OrderCompletionStatus status;

		public InternalOrderCompletionResult(OrderCompletionStatus status) {
			this.status = status;
		}

		@Override
		public OrderCompletionStatus getStatus() {
			return status;
		}
	}
}
