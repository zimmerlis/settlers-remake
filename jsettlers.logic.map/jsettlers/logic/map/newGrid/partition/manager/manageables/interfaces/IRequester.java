package jsettlers.logic.map.newGrid.partition.manager.manageables.interfaces;

/**
 * Superinterface for requesters.
 * 
 * @author Andreas Eberle
 * 
 */
interface IRequester {
	/**
	 * Indicates if the request from this requester is still active or has been canceled.
	 * 
	 * @return true if the request is still active<br>
	 *         false if the request has been canceled.
	 */
	boolean isDiggerRequestActive();
}
