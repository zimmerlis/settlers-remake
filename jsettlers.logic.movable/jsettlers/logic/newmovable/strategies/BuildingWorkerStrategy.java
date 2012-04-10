package jsettlers.logic.newmovable.strategies;

import jsettlers.common.buildings.jobs.EBuildingJobType;
import jsettlers.common.buildings.jobs.IBuildingJob;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EAction;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.MillBuilding;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.map.newGrid.partition.manager.manageables.IManageableWorker;
import jsettlers.logic.map.newGrid.partition.manager.manageables.interfaces.IWorkerRequestBuilding;
import jsettlers.logic.newmovable.NewMovable;
import jsettlers.logic.newmovable.NewMovableStrategy;
import random.RandomSingleton;

public class BuildingWorkerStrategy extends NewMovableStrategy implements IManageableWorker {
	private static final long serialVersionUID = 5949318243804026519L;

	private final EMovableType movableType;

	private IBuildingJob currentJob = null;
	private IWorkerRequestBuilding building;

	private boolean done;
	private short delayCtr;

	private EMaterialType poppedMaterial;

	public BuildingWorkerStrategy(NewMovable movable, EMovableType movableType) {
		super(movable);
		this.movableType = movableType;
		reportJobless();
	}

	@Override
	protected void action() {
		if (currentJob == null)
			return;

		switch (currentJob.getType()) {
		case GO_TO:
			gotoAction();
			break;

		case IS_PRODUCTIVE:
			if (isProductive()) {
				jobFinished();
			} else {
				jobFailed();
			}
			break;

		case WAIT:
			waitSeconds();
			break;

		case WALK:
			if (super.forceGoInDirection(currentJob.getDirection())) {
				jobFinished();
			} else {
				jobFailed();
			}
			break;

		case SHOW:
			ShortPoint2D pos = getCurrentJobPos();
			super.setPos(pos);
			super.setVisible(true);
			jobFinished();
			break;

		case HIDE:
			super.setVisible(false);
			jobFinished();
			break;

		case SET_MATERIAL:
			super.setMaterial(currentJob.getMaterial());
			jobFinished();
			break;

		case TAKE:
			takeAction();
			break;
		case REMOTETAKE:
			if (this.building.popMaterial(getCurrentJobPos(), currentJob.getMaterial())) {
				jobFinished();
			} else {
				jobFailed();
			}
			break;

		case DROP:
			dropAction(super.getMaterial());
			break;
		case DROP_POPPED:
			dropAction(poppedMaterial);
			break;

		case PRE_SEARCH:
			preSearchPathAction(true);
			break;

		case PRE_SEARCH_IN_AREA:
			preSearchPathAction(false);
			break;

		case FOLLOW_SEARCHED:
			super.followPresearchedPath();
			break;

		case LOOK_AT_SEARCHED:
			lookAtSearched();
			break;
		case LOOK_AT:
			super.lookInDirection(currentJob.getDirection());
			jobFinished();
			break;

		case EXECUTE:
			executeAction();
			break;

		case PLAY_ACTION1:
			super.playAction(EAction.ACTION1, currentJob.getTime());
			jobFinished();
			break;
		case PLAY_ACTION2:
			super.playAction(EAction.ACTION2, currentJob.getTime());
			jobFinished();
			break;

		case AVAILABLE:
			if (super.getStrategyGrid().canPop(getCurrentJobPos(), currentJob.getMaterial())) {
				jobFinished();
			} else {
				jobFailed();
			}
			break;

		case NOT_FULL:
			if (super.getStrategyGrid().canPushMaterial(getCurrentJobPos())) {
				jobFinished();
			} else {
				jobFailed();
			}
			break;

		case SMOKE_ON:
		case SMOKE_OFF: {
			super.getStrategyGrid().placeSmoke(getCurrentJobPos(), currentJob.getType() == EBuildingJobType.SMOKE_ON);
			jobFinished();
			break;
		}

		case START_WORKING:
		case STOP_WORKING:
			if (building instanceof MillBuilding) {
				((MillBuilding) building).setRotating(currentJob.getType() == EBuildingJobType.START_WORKING);
			}
			jobFinished();
			break;

		case PIG_IS_ADULT:
			if (super.getStrategyGrid().isPigAdult(getCurrentJobPos())) {
				jobFinished();
			} else {
				jobFailed();
			}
			break;

		case PIG_IS_THERE:
			if (super.getStrategyGrid().hasPigAt(getCurrentJobPos())) {
				jobFinished();
			} else {
				jobFailed();
			}
			break;

		case PIG_PLACE:
		case PIG_REMOVE:
			placeOrRemovePigAction();
			break;

		case POP_TOOL:
			popToolRequestAction();
			break;

		}
	}

	private void placeOrRemovePigAction() {
		ShortPoint2D pos = getCurrentJobPos();
		super.getStrategyGrid().placePigAt(pos, currentJob.getType() == EBuildingJobType.PIG_PLACE);
		jobFinished();
	}

	private void popToolRequestAction() {
		ShortPoint2D pos = building.getDoor();
		poppedMaterial = super.getStrategyGrid().popToolProductionRequest(pos);
		if (poppedMaterial != null) {
			jobFinished();
		} else {
			jobFailed();
		}
	}

	private void executeAction() {
		if (super.getStrategyGrid().executeSearchType(super.getPos(), currentJob.getSearchType())) {
			jobFinished();
		} else {
			jobFailed();
		}
	}

	private void takeAction() {
		if (this.building.popMaterial(super.getPos(), currentJob.getMaterial())) {
			super.playAction(EAction.TAKE, Constants.MOVABLE_TAKE_DROP_DURATION);
			jobFinished();
		} else {
			jobFailed();
		}

	}

	private void dropAction(EMaterialType materialType) {
		if (!done) {
			super.playAction(EAction.DROP, Constants.MOVABLE_TAKE_DROP_DURATION);
		} else {
			super.getStrategyGrid().dropMaterial(super.getPos(), materialType);
		}
	}

	/**
	 * 
	 * @param dijkstra
	 *            if true, dijkstra algorithm is used<br>
	 *            if false, in area finder is used.
	 */
	private void preSearchPathAction(boolean dijkstra) {
		boolean pathFound = super.preSearchPath(dijkstra, building.getWorkAreaCenterX(), building.getWorkAreaCenterY(), building.getBuildingType()
				.getWorkradius(), currentJob.getSearchType());
		if (pathFound) {
			jobFinished();
		} else {
			jobFailed();
		}
	}

	private void waitSeconds() {
		if (!done) {
			done = true;
			delayCtr = (short) (currentJob.getTime() * Constants.MOVABLE_INTERRUPTS_PER_SECOND);
		} else {
			delayCtr--;
			if (delayCtr <= 0) {
				jobFinished();
			}
		}
	}

	private boolean isProductive() {
		switch (building.getBuildingType()) {
		case FISHER:
			// TODO: look into the water, not at the sand.
			return hasProductiveResources(super.getPos(), EResourceType.FISH);
		case COALMINE:
			return hasProductiveResources(building.getDoor(), EResourceType.COAL);
		case IRONMINE:
			return hasProductiveResources(building.getDoor(), EResourceType.IRON);
		case GOLDMINE:
			return hasProductiveResources(building.getDoor(), EResourceType.GOLD);
		}
		return false;
	}

	private boolean hasProductiveResources(ShortPoint2D pos, EResourceType type) {
		float amount = super.getStrategyGrid().getResourceAmountAround(pos.getX(), pos.getY(), type);
		return RandomSingleton.get().nextFloat() < amount;
	}

	private void gotoAction() {
		if (!done) {
			this.done = true;
			if (!super.goToPos(getCurrentJobPos())) {
				jobFailed();
			}
		} else {
			jobFinished(); // start next action
		}
	}

	private void jobFinished() {
		this.currentJob = this.currentJob.getNextSucessJob();
		done = false;
	}

	private void jobFailed() {
		this.currentJob = this.currentJob.getNextFailJob();
		done = false;
	}

	private ShortPoint2D getCurrentJobPos() {
		return building.calculateRealPoint(currentJob.getDx(), currentJob.getDy());
	}

	private void lookAtSearched() {
		EDirection direction = super.getStrategyGrid().getDirectionOfSearched(super.getPos(), currentJob.getSearchType());
		if (direction != null) {
			super.lookInDirection(direction);
			jobFinished();
		} else {
			jobFailed();
		}
	}

	@Override
	public EMovableType getMovableType() {
		return movableType;
	}

	@Override
	public void setWorkerJob(IWorkerRequestBuilding building) {
		this.building = building;
		this.currentJob = building.getBuildingType().getStartJob();
	}

	@Override
	public void buildingDestroyed() {
		reportJobless();
	}

	private void reportJobless() {
		super.getStrategyGrid().addJoblessWorker(this);
	}

}
