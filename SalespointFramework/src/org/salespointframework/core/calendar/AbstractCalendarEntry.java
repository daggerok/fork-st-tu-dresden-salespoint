package org.salespointframework.core.calendar;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.joda.time.DateTime;
import org.salespointframework.util.Objects;

/**
 * 
 * This is an abstract representation of an calendar entry which provides basic
 * functionality
 * 
 * @author stanley
 * 
 */
@MappedSuperclass
public abstract class AbstractCalendarEntry implements CalendarEntry {

    @Id
    @GeneratedValue
    private long                          calendarEntryIdentifier;

    /**
     * This map stores a relationship between users and their capabilitys
     * relative to this calendar entry.
     */
    @ElementCollection
    protected Map<String, CapabilityList> capabilities = new HashMap<String, CapabilityList>();
    //protected Map<String, ArrayList<CalendarEntryCapability>> capabilities = new HashMap<String, ArrayList<CalendarEntryCapability>>();

    /**
     * Description of this calendar entry. May be empty.
     */
    protected String                      description;

    /**
     * The start date and time for this entry.
     */
    protected long                        startTime;

    /**
     * The end date and time for this entry.
     */
    protected long                        endTime;

    /**
     * Title of this entry.
     */
    protected String                      title;

    /**
     * Represents how often this entry should be repeated. For determining the
     * time between two repetitions, see
     * {@link AbstractCalendarEntry#repeatStep}
     * 
     */
    protected int                         repeatCount;

    /**
     * Represents the time in millis between two repetitions of this entry. For
     * determining how often an entry should be repeated, see
     * {@link AbstractCalendarEntry#repeatCount}
     */
    protected long                        repeatStep;

    /**
     * This contructor should not be used. It only exists for persistence
     * reasons.
     */
    @Deprecated
    public AbstractCalendarEntry() {

    }

    /**
     * Basic contructor with
     * 
     * @param owner
     *            The id of the user, who created this entry.
     * @param title
     *            The title of this entry.
     * @param start
     *            Start time and date.
     * @param end
     *            End time and date.
     * @throws IllegalArgumentException
     *             The {@link IllegalArgumentException} will be thrown, if the
     *             begin is not before the end of this calendar entry or the
     *             title is empty.
     */
    public AbstractCalendarEntry(String owner, String title, DateTime start, DateTime end) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");

        if (start.isAfter(end))
            throw new IllegalArgumentException("An calendar entry cannot end before it starts.");

        if (title.isEmpty())
            throw new IllegalArgumentException("The title cannot be empty.");

        this.title = title;
        this.startTime = start.getMillis();
        this.endTime = end.getMillis();

        description = "";
        repeatCount = 0;
        repeatStep = 0;

        for (CalendarEntryCapability cap : CalendarEntryCapability.values())
            addCapability(owner, cap);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof CalendarEntry)
            return this.equals((CalendarEntry) object);
        return false;
    }

    /**
     * Determines if the given {@link CalendarEntry} is equal to this one or
     * not. Two calendar entries are equal to each other, if their hash code is
     * the same.
     * 
     * @param entry
     *            the entry this one should be compared with
     * @return <code>true</code> if and only if the hashCode of this Object
     *         equals the hashCode of the object given as parameter.
     *         <code>false</code> otherwise.
     */
    public boolean equals(CalendarEntry entry) {
        return (entry != null) && (this.hashCode() == entry.hashCode());
    }

    /**
     * Returns the hash code for this entry. The hash of this object is the hash
     * of its primary key.
     * 
     * @return the hash code for this entry
     */
    @Override
    public int hashCode() {
        return new Long(calendarEntryIdentifier).hashCode();
    }

    /**
     * Returns the description of this entry. The description may be empty but
     * never <code>null</code>.
     * 
     * @return the description of this entry
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Returns the title of this entry. The title cannot be empty.
     * 
     * @return the title of this entry
     */
    @Override
    public String getTitle() {
        return title;
    }

    /**
     * Returns the start time of this entry. The start time is never
     * <code>null</code>
     * 
     * @return the start time of this entry
     */
    @Override
    public DateTime getStart() {
        return new DateTime(startTime);
    }

    /**
     * Returns the end time of this entry. The end time is never
     * <code>null</code>
     * 
     * @return the end time of this entry
     */
    @Override
    public DateTime getEnd() {
        return new DateTime(endTime);
    }

    /**
     * Returns the ID of this entry. The ID is the entry's primary key,
     * generated automatically when the entry has been stored. The ID of an
     * entry that hasn't been persisted yet is undefined.
     * 
     * @return the ID of this entry
     */
    @Override
    public long getID() {
        return calendarEntryIdentifier;
    }

    /**
     * Sets the start time of this entry. The start time must not be
     * <code>null</code> and has to be before the end time.
     * 
     * @param start
     *            the new start time
     * 
     * @throws IllegalArgumentException
     *             if the start is after the end
     * 
     * @see DateTime
     */
    @Override
    public void setStart(DateTime start) {
        Objects.requireNonNull(start, "start");
        if (start.getMillis() >= endTime)
            throw new IllegalArgumentException("An calendar entry cannot start after it ends.");

        this.startTime = start.getMillis();
    }

    /**
     * Sets the end time of this entry. The end time must not be
     * <code>null</code> and has to be after the start time.
     * 
     * @param end
     *            the new end time
     * 
     * @throws IllegalArgumentException
     *             if the start is after the end
     * 
     * @see DateTime
     */
    @Override
    public void setEnd(DateTime end) {
        Objects.requireNonNull(end, "end");
        if (end.getMillis() <= startTime)
            throw new IllegalArgumentException("An calendar entry cannot end before it starts.");

        this.endTime = end.getMillis();
    }

    /**
     * Sets the new title of the entry. The title must not be <code>null</code>
     * nor empty.
     * 
     * @param title
     *            the new title for this entry
     * 
     * @throws IllegalArgumentException
     *             if title is empty
     */
    @Override
    public void setTitle(String title) {
        Objects.requireNonNull(title, "title");
        if (title.isEmpty())
            throw new IllegalArgumentException("The title cannot be empty.");
        this.title = title;
    }

    /**
     * Sets the new description of this entry. The description must not be null
     * but can be an empty string.
     * 
     * @param description
     *            the new description for this entry
     */
    @Override
    public void setDescription(String description) {
        Objects.requireNonNull(description, "description");
        this.description = description;
    }

    /**
     * Return the userID of the owner of this entry. Typically the owner is the
     * creator of an entry but this can change over time. An entry can only have
     * one owner at a time.
     * 
     * @return The userID of the user who is the owner of this entry.
     */
    @Override
    public String getOwner() {
        for (String userId : capabilities.keySet()) {
            if (capabilities.get(userId).contains(CalendarEntryCapability.OWNER)) {
                return userId;
            }
        }
        return null;
    }

    /**
     * Adds a capability for a specific user. Parameters must not be
     * <code>null</code>. If capability <code>OWNER</code> should be added, the
     * old owner looses this capability, because there can be only one owner at
     * a time.
     * 
     * @param user
     *            the userID of the user who should get the new capability.
     * @param capability
     *            the new capability for the given user
     */
    @Override
    public void addCapability(String user, CalendarEntryCapability capability) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(capability, "capability");

        if (capability == CalendarEntryCapability.OWNER) {
            Iterator<String> owner = getUsersByCapability(capability).iterator();
            while (owner.hasNext())
                removeCapability(owner.next(), capability);
        }
            
            

        CapabilityList capList = null;
        //ArrayList<CalendarEntryCapability> capList = null;

        if ((capList = capabilities.get(user)) == null) {
            capList = new CapabilityList();
            capabilities.put(user, capList);
        } else {
            if (capList.contains(capability))
                return;
        }

        capList.add(capability);
    }

    /**
     * Removes a capability from a user. Parameters must not be
     * <code>null</code>. The capability <code>OWNER</code> cannot be removed.
     * 
     * @param user
     *            userID of the user who should loose the capability
     * @param capability
     *            the capability that schould be removed from the user
     * 
     * @throws IllegalArgumentException
     *             if the capability <code>OWNER</code> should be removed.
     */
    @Override
    public void removeCapability(String user, CalendarEntryCapability capability) {
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(capability, "capability");

        if (capability == CalendarEntryCapability.OWNER)
            throw new IllegalArgumentException("Capability 'OWNER' cannot be removed.");

        CapabilityList capList = null;
        //ArrayList<CalendarEntryCapability> capList = null;

        if ((capList = capabilities.get(user)) != null) {
            capList.remove(capability);
            if (capList.isEmpty())
                capabilities.remove(user);
        }
    }

    /**
     * Returns all capabilities the given user has for this entry. The userID
     * must not be <code>null</code>
     * 
     * @param user
     *            the userID of the user whose capabilities should be returned.
     * 
     * @return An {@link Iterable} which contains all capabilities of this user
     *         or null if the user has no capabilities for this entry.
     */
    public Iterable<CalendarEntryCapability> getCapabilitiesByUser(String user) {
        Objects.requireNonNull(user, "user");

        return capabilities.get(user);
    }

    /**
     * Returns all users that have the given capability. The capability must not
     * be <code>null</code>
     * 
     * @param capability
     *            the capability for which all users should be found.
     * 
     * @return An {@link Iterable} which contains all user IDs of users who have
     *         the capability or null if there is no user who has the capability
     *         for this entry.
     */
    public Iterable<String> getUsersByCapability(CalendarEntryCapability capability) {
        Objects.requireNonNull(capability, "capability");

        List<String> users = new LinkedList<String>();

        for (String user : capabilities.keySet()) {
            if (capabilities.get(user).contains(capability))
                users.add(user);
        }

        return users;
    }

}