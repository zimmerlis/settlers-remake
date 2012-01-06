package jsettlers.logic.map.newGrid;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Random;

import jsettlers.common.Color;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.map.IGraphicsBackgroundListener;
import jsettlers.common.map.IGraphicsGrid;
import jsettlers.common.map.shapes.FreeMapArea;
import jsettlers.common.map.shapes.IMapArea;
import jsettlers.common.map.shapes.MapNeighboursArea;
import jsettlers.common.map.shapes.MapShapeFilter;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.mapobject.IMapObject;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IMovable;
import jsettlers.common.player.IPlayerable;
import jsettlers.common.position.ISPosition2D;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.input.IGuiInputGrid;
import jsettlers.logic.algorithms.borders.BordersThread;
import jsettlers.logic.algorithms.borders.IBordersThreadGrid;
import jsettlers.logic.algorithms.construction.ConstructMarksThread;
import jsettlers.logic.algorithms.construction.IConstructionMarkableMap;
import jsettlers.logic.algorithms.fogofwar.FogOfWar;
import jsettlers.logic.algorithms.fogofwar.IFogOfWarGrid;
import jsettlers.logic.algorithms.landmarks.ILandmarksThreadMap;
import jsettlers.logic.algorithms.landmarks.LandmarksCorrectingThread;
import jsettlers.logic.algorithms.path.IPathCalculateable;
import jsettlers.logic.algorithms.path.area.IInAreaFinderMap;
import jsettlers.logic.algorithms.path.area.InAreaFinder;
import jsettlers.logic.algorithms.path.astar.HexAStar;
import jsettlers.logic.algorithms.path.astar.IAStarPathMap;
import jsettlers.logic.algorithms.path.dijkstra.DijkstraAlgorithm;
import jsettlers.logic.algorithms.path.dijkstra.IDijkstraPathMap;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.buildings.military.Barrack;
import jsettlers.logic.buildings.military.IOccupyableBuilding;
import jsettlers.logic.buildings.workers.WorkerBuilding;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.map.newGrid.flags.FlagsGrid;
import jsettlers.logic.map.newGrid.landscape.EResourceType;
import jsettlers.logic.map.newGrid.landscape.LandscapeGrid;
import jsettlers.logic.map.newGrid.movable.IHexMovable;
import jsettlers.logic.map.newGrid.movable.MovableGrid;
import jsettlers.logic.map.newGrid.objects.AbstractHexMapObject;
import jsettlers.logic.map.newGrid.objects.IMapObjectsManagerGrid;
import jsettlers.logic.map.newGrid.objects.MapObjectsManager;
import jsettlers.logic.map.newGrid.objects.ObjectsGrid;
import jsettlers.logic.map.newGrid.partition.IPartitionableGrid;
import jsettlers.logic.map.newGrid.partition.PartitionsGrid;
import jsettlers.logic.map.newGrid.partition.manager.manageables.IManageableBearer;
import jsettlers.logic.map.newGrid.partition.manager.manageables.IManageableBricklayer;
import jsettlers.logic.map.newGrid.partition.manager.manageables.IManageableDigger;
import jsettlers.logic.map.newGrid.partition.manager.manageables.IManageableWorker;
import jsettlers.logic.map.newGrid.partition.manager.manageables.interfaces.IDiggerRequester;
import jsettlers.logic.map.newGrid.partition.manager.manageables.interfaces.IMaterialRequester;
import jsettlers.logic.map.random.RandomMapEvaluator;
import jsettlers.logic.map.random.RandomMapFile;
import jsettlers.logic.map.random.grid.BuildingObject;
import jsettlers.logic.map.random.grid.MapGrid;
import jsettlers.logic.map.random.grid.MapObject;
import jsettlers.logic.map.random.grid.MapStoneObject;
import jsettlers.logic.map.random.grid.MapTreeObject;
import jsettlers.logic.map.random.grid.MovableObject;
import jsettlers.logic.map.random.grid.StackObject;
import jsettlers.logic.movable.IMovableGrid;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.stack.IRequestsStackGrid;
import random.RandomSingleton;

/**
 * This is the main grid offering an interface for interacting with the grid.
 * 
 * @author Andreas Eberle
 * 
 */
public class MainGrid implements Serializable {
	private static final long serialVersionUID = 3824511313693431423L;

	final short width;
	final short height;

	final LandscapeGrid landscapeGrid;
	final ObjectsGrid objectsGrid;
	final PartitionsGrid partitionsGrid;
	final MovableGrid movableGrid;
	final FlagsGrid flagsGrid;
	transient Color[][] debugColors;

	final MovablePathfinderGrid movablePathfinderGrid;
	final MapObjectsManager mapObjectsManager;
	final BuildingsGrid buildingsGrid;
	final FogOfWar fogOfWar;

	transient IGraphicsGrid graphicsGrid;
	transient LandmarksCorrectingThread landmarksCorrectionThread;
	transient ConstructionMarksGrid constructionMarksGrid;
	transient ConstructMarksThread constructionMarksCalculator;
	transient BordersThread bordersThread;
	transient IGuiInputGrid guiInputGrid;

	public MainGrid(short width, short height) {
		this.width = width;
		this.height = height;

		this.movablePathfinderGrid = new MovablePathfinderGrid();
		this.mapObjectsManager = new MapObjectsManager(new MapObjectsManagerGrid());

		this.landscapeGrid = new LandscapeGrid(width, height);
		this.objectsGrid = new ObjectsGrid(width, height);
		this.movableGrid = new MovableGrid(width, height);
		this.flagsGrid = new FlagsGrid(width, height);
		this.partitionsGrid = new PartitionsGrid(width, height, new PartitionableGrid());

		this.buildingsGrid = new BuildingsGrid();
		this.fogOfWar = new FogOfWar(width, height); // TODO @Andreas implement new interface for fog of war

		initAdditionalGrids();
	}

	private void initAdditionalGrids() {
		this.graphicsGrid = new GraphicsGrid();
		this.landmarksCorrectionThread = new LandmarksCorrectingThread(new LandmarksGrid());
		this.constructionMarksGrid = new ConstructionMarksGrid();
		this.constructionMarksCalculator = new ConstructMarksThread(constructionMarksGrid, (byte) 0);
		this.bordersThread = new BordersThread(new BordersThreadGrid());
		this.guiInputGrid = new GUIInputGrid();

		this.debugColors = new Color[width][height];

		this.fogOfWar.startThread(new FogOfWarGrid());

		this.partitionsGrid.initPartitionsAlgorithm(movablePathfinderGrid.aStar);
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		initAdditionalGrids();
	}

	private MainGrid(MapGrid mapGrid) {
		this((short) mapGrid.getWidth(), (short) mapGrid.getHeight());

		for (short y = 0; y < height; y++) {
			for (short x = 0; x < width; x++) {
				landscapeGrid.setLandscapeTypeAt(x, y, mapGrid.getLandscape(x, y));
				landscapeGrid.setHeightAt(x, y, mapGrid.getLandscapeHeight(x, y));

				switch (mapGrid.getLandscape(x, y)) {
				case MOUNTAIN:
					landscapeGrid.setResourceAt(x, y, EResourceType.values()[RandomSingleton.getInt(0, 2)], (byte) RandomSingleton.getInt(-100, 127));
					break;
				case WATER:
					landscapeGrid.setResourceAt(x, y, EResourceType.FISH, (byte) RandomSingleton.getInt(-100, 127));
					break;
				}
			}
		}

		// tow passes, we might need the base grid tiles to add blocking, ... status
		for (short y = 0; y < height; y++) {
			for (short x = 0; x < width; x++) {
				MapObject object = mapGrid.getMapObject(x, y);
				if (object != null) {
					addMapObject(x, y, object);
				}
			}
		}

		System.out.println("grid filled");
	}

	public static MainGrid createForDebugging() {
		MainGrid grid = new MainGrid((short) 200, (short) 100);

		for (short x = 0; x < grid.width; x++) {
			for (short y = 0; y < grid.height; y++) {
				grid.landscapeGrid.setLandscapeTypeAt(x, y, ELandscapeType.GRASS);
				grid.landscapeGrid.setHeightAt(x, y, (byte) 0);
			}
		}

		Building tower = Building.getBuilding(EBuildingType.TOWER, (byte) 0);
		tower.appearAt(grid.buildingsGrid, new ShortPoint2D(55, 50));

		tower = Building.getBuilding(EBuildingType.TOWER, (byte) 0);
		tower.appearAt(grid.buildingsGrid, new ShortPoint2D(145, 50));

		grid.placeStack(new ShortPoint2D(30, 50), EMaterialType.PLANK, 8);
		grid.placeStack(new ShortPoint2D(32, 50), EMaterialType.PLANK, 8);
		grid.placeStack(new ShortPoint2D(34, 50), EMaterialType.PLANK, 8);
		grid.placeStack(new ShortPoint2D(36, 50), EMaterialType.PLANK, 8);
		grid.placeStack(new ShortPoint2D(30, 40), EMaterialType.STONE, 8);
		grid.placeStack(new ShortPoint2D(32, 40), EMaterialType.STONE, 8);
		grid.placeStack(new ShortPoint2D(34, 40), EMaterialType.STONE, 8);
		grid.placeStack(new ShortPoint2D(36, 40), EMaterialType.STONE, 8);
		grid.placeStack(new ShortPoint2D(34, 30), EMaterialType.HAMMER, 1);
		grid.placeStack(new ShortPoint2D(36, 30), EMaterialType.BLADE, 1);

		grid.placeStack(new ShortPoint2D(38, 30), EMaterialType.AXE, 1);
		grid.placeStack(new ShortPoint2D(40, 30), EMaterialType.SAW, 1);

		for (int i = 0; i < 10; i++) {
			grid.createNewMovableAt(new ShortPoint2D(60 + 2 * i, 50), EMovableType.BEARER, (byte) 0);
		}
		grid.createNewMovableAt(new ShortPoint2D(50, 50), EMovableType.PIONEER, (byte) 0);

		grid.createNewMovableAt(new ShortPoint2D(60, 60), EMovableType.SWORDSMAN_L3, (byte) 0);

		return grid;
	}

	@Deprecated
	public static MainGrid create(String filename, byte players, Random random) {
		RandomMapFile file = RandomMapFile.getByName(filename);
		RandomMapEvaluator evaluator = new RandomMapEvaluator(file.getInstructions(), players);
		evaluator.createMap(random);
		MapGrid mapGrid = evaluator.getGrid();

		return new MainGrid(mapGrid);
	}

	public static MainGrid create(MapGrid mapGrid) {
		return new MainGrid(mapGrid);
	}

	private void addMapObject(short x, short y, MapObject object) {
		ISPosition2D pos = new ShortPoint2D(x, y);

		if (object instanceof MapTreeObject) {
			if (isInBounds(x, y) && movablePathfinderGrid.isTreePlantable(x, y)) {
				mapObjectsManager.plantAdultTree(pos);
			}
		} else if (object instanceof MapStoneObject) {
			mapObjectsManager.addStone(pos, ((MapStoneObject) object).getCapacity());
		} else if (object instanceof StackObject) {
			placeStack(pos, ((StackObject) object).getType(), ((StackObject) object).getCount());
		} else if (object instanceof BuildingObject) {
			Building building = Building.getBuilding(((BuildingObject) object).getType(), ((BuildingObject) object).getPlayer());
			building.appearAt(buildingsGrid, pos);
			if (building instanceof IOccupyableBuilding) {
				Movable soldier = createNewMovableAt(((IOccupyableBuilding) building).getDoor(), EMovableType.SWORDSMAN_L1, building.getPlayer());
				soldier.setOccupyableBuilding((IOccupyableBuilding) building);
			}
		} else if (object instanceof MovableObject) {
			createNewMovableAt(pos, ((MovableObject) object).getType(), ((MovableObject) object).getPlayer());
		}
	}

	private void placeStack(ISPosition2D pos, EMaterialType materialType, int count) {
		for (int i = 0; i < count; i++) {
			movablePathfinderGrid.pushMaterial(pos, materialType, true);
		}
	}

	public IGraphicsGrid getGraphicsGrid() {
		return graphicsGrid;
	}

	public IGuiInputGrid getGuiInputGrid() {
		return guiInputGrid;
	}

	protected final boolean isInBounds(short x, short y) {
		return x >= 0 && x < width && y >= 0 && y < height;
	}

	private void changePlayerAt(ISPosition2D position, byte player) {
		partitionsGrid.changePlayerAt(position.getX(), position.getY(), player);
		bordersThread.checkPosition(position);
		landmarksCorrectionThread.addLandmarkedPosition(position);
	}

	public final Movable createNewMovableAt(ISPosition2D pos, EMovableType type, byte player) {
		Movable movable = new Movable(movablePathfinderGrid, pos, type, player);
		buildingsGrid.placeNewMovable(pos, movable);
		return movable;
	}

	protected final boolean isLandscapeBlocking(short x, short y) {
		ELandscapeType landscapeType = landscapeGrid.getLandscapeTypeAt(x, y);
		return landscapeType == ELandscapeType.WATER || landscapeType == ELandscapeType.SNOW;
	}

	class PathfinderGrid implements IAStarPathMap, IDijkstraPathMap, IInAreaFinderMap, Serializable {
		private static final long serialVersionUID = -2775530442375843213L;

		@Override
		public boolean isBlocked(IPathCalculateable requester, short x, short y) {
			return flagsGrid.isBlocked(x, y) || isLandscapeBlocking(x, y)
					|| (requester.needsPlayersGround() && requester.getPlayer() != partitionsGrid.getPlayerAt(x, y));
		}

		@Override
		public float getHeuristicCost(short sx, short sy, short tx, short ty) {
			float dx = (short) Math.abs(sx - tx);
			float dy = (short) Math.abs(sy - ty);

			return (dx + dy) * Constants.TILE_HEURISTIC_DIST;
		}

		@Override
		public final float getCost(short sx, short sy, short tx, short ty) {
			return Constants.TILE_PATHFINDER_COST * (flagsGrid.isProtected(sx, sy) ? 3.5f : 1);
		}

		@Override
		public final void markAsOpen(short x, short y) {
			debugColors[x][y] = Color.BLUE;
		}

		@Override
		public final void markAsClosed(short x, short y) {
			debugColors[x][y] = Color.RED;
		}

		@Override
		public final void setDijkstraSearched(short x, short y) {
			markAsOpen(x, y);
		}

		@Override
		public final boolean fitsSearchType(short x, short y, ESearchType searchType, IPathCalculateable pathCalculable) {
			switch (searchType) {

			case FOREIGN_GROUND:
				return !flagsGrid.isBlocked(x, y) && !hasSamePlayer(x, y, pathCalculable) && !isMarked(x, y)
						&& !partitionsGrid.isEnforcedByTower(x, y);

			case CUTTABLE_TREE:
				return isInBounds((short) (x - 1), (short) (y - 1))
						&& objectsGrid.hasCuttableObject((short) (x - 1), (short) (y - 1), EMapObjectType.TREE_ADULT)
						&& hasSamePlayer((short) (x - 1), (short) (y - 1), pathCalculable) && !isMarked(x, y);

			case PLANTABLE_TREE:
				return y < height - 1 && isTreePlantable(x, (short) (y + 1)) && !hasProtectedNeighbor(x, (short) (y + 1))
						&& hasSamePlayer(x, (short) (y + 1), pathCalculable) && !isMarked(x, y);

			case PLANTABLE_CORN:
				return isCornPlantable(x, y) && hasSamePlayer(x, y, pathCalculable) && !isMarked(x, y) && !flagsGrid.isProtected(x, y);

			case CUTTABLE_CORN:
				return isCornCuttable(x, y) && hasSamePlayer(x, y, pathCalculable) && !isMarked(x, y);

			case CUTTABLE_STONE:
				return y < height + 1 && x < width - 1 && objectsGrid.hasCuttableObject((short) (x - 1), (short) (y + 1), EMapObjectType.STONE)
						&& hasSamePlayer(x, y, pathCalculable) && !isMarked(x, y);

			case ENEMY: {
				IMovable movable = movableGrid.getMovableAt(x, y);
				return movable != null && movable.getPlayer() != pathCalculable.getPlayer();
			}

			case RIVER:
				return isRiver(x, y) && hasSamePlayer(x, y, pathCalculable) && !isMarked(x, y);

			case FISHABLE:
				return hasSamePlayer(x, y, pathCalculable) && hasNeighbourLandscape(x, y, ELandscapeType.WATER);

			case NON_BLOCKED_OR_PROTECTED:
				return !(flagsGrid.isProtected(x, y) || flagsGrid.isBlocked(x, y) || isLandscapeBlocking(x, y))
						&& (!pathCalculable.needsPlayersGround() || hasSamePlayer(x, y, pathCalculable)) && movableGrid.getMovableAt(x, y) == null;

			case SOLDIER_BOWMAN:
				return isSoldierAt(x, y, searchType, pathCalculable.getPlayer());
			case SOLDIER_SWORDSMAN:
				return isSoldierAt(x, y, searchType, pathCalculable.getPlayer());
			case SOLDIER_PIKEMAN:
				return isSoldierAt(x, y, searchType, pathCalculable.getPlayer());

			case MOUNTAIN:
				return isInBounds(x, y) && !flagsGrid.isMarked(x, y) && canAddRessourceSign(x, y);

			case FOREIGN_MATERIAL:
				return isInBounds(x, y) && !hasSamePlayer(x, y, pathCalculable) && mapObjectsManager.hasStealableMaterial(x, y);

			default:
				System.err.println("can't handle search type in fitsSearchType(): " + searchType);
				return false;
			}
		}

		protected final boolean canAddRessourceSign(short x, short y) {
			return x % 2 == 0
					&& y % 2 == 0
					&& landscapeGrid.getLandscapeTypeAt(x, y) == ELandscapeType.MOUNTAIN
					&& !(objectsGrid.hasMapObjectType(x, y, EMapObjectType.FOUND_COAL)
							|| objectsGrid.hasMapObjectType(x, y, EMapObjectType.FOUND_IRON) || objectsGrid.hasMapObjectType(x, y,
							EMapObjectType.FOUND_GOLD));
		}

		private final boolean isSoldierAt(short x, short y, ESearchType searchType, byte player) {
			IHexMovable movable = movableGrid.getMovableAt(x, y);
			if (movable == null) {
				return false;
			} else {
				if (movable.getPlayer() == player && movable.canOccupyBuilding()) {
					EMovableType type = movable.getMovableType();
					switch (searchType) {
					case SOLDIER_BOWMAN:
						return type == EMovableType.BOWMAN_L1 || type == EMovableType.BOWMAN_L2 || type == EMovableType.BOWMAN_L3;
					case SOLDIER_SWORDSMAN:
						return type == EMovableType.SWORDSMAN_L1 || type == EMovableType.SWORDSMAN_L2 || type == EMovableType.SWORDSMAN_L3;
					case SOLDIER_PIKEMAN:
						return type == EMovableType.PIKEMAN_L1 || type == EMovableType.PIKEMAN_L2 || type == EMovableType.PIKEMAN_L3;
					default:
						return false;
					}
				} else {
					return false;
				}
			}
		}

		private final boolean isMarked(short x, short y) {
			return flagsGrid.isMarked(x, y);
		}

		private final boolean hasProtectedNeighbor(short x, short y) {
			for (EDirection currDir : EDirection.valuesCached()) {
				if (flagsGrid.isProtected(currDir.getNextTileX(x), currDir.getNextTileY(y)))
					return true;
			}
			return false;
		}

		private final boolean hasNeighbourLandscape(short x, short y, ELandscapeType landscape) {
			for (ISPosition2D pos : new MapNeighboursArea(new ShortPoint2D(x, y))) {
				short currX = pos.getX();
				short currY = pos.getY();
				if (isInBounds(currX, currY) && landscapeGrid.getLandscapeTypeAt(currX, currY) == landscape) {
					return true;
				}
			}
			return false;
		}

		private final boolean hasSamePlayer(short x, short y, IPathCalculateable requester) {
			return partitionsGrid.getPlayerAt(x, y) == requester.getPlayer();
		}

		private final boolean isRiver(short x, short y) {
			ELandscapeType type = landscapeGrid.getLandscapeTypeAt(x, y);
			return type == ELandscapeType.RIVER1 || type == ELandscapeType.RIVER2 || type == ELandscapeType.RIVER3 || type == ELandscapeType.RIVER4;
		}

		final boolean isTreePlantable(short x, short y) {
			return landscapeGrid.getLandscapeTypeAt(x, y) == ELandscapeType.GRASS && !flagsGrid.isBlocked(x, y) && !hasBlockedNeighbor(x, y);
		}

		private final boolean hasBlockedNeighbor(short x, short y) {
			for (EDirection currDir : EDirection.values()) {
				short currX = currDir.getNextTileX(x);
				short currY = currDir.getNextTileY(y);
				if (!isInBounds(currX, currY) || flagsGrid.isBlocked(currX, currY)) {
					return true;
				}
			}

			return false;
		}

		private final boolean isCornPlantable(short x, short y) {
			ELandscapeType landscapeType = landscapeGrid.getLandscapeTypeAt(x, y);
			return (landscapeType == ELandscapeType.GRASS || landscapeType == ELandscapeType.EARTH) && !flagsGrid.isProtected(x, y)
					&& !hasProtectedNeighbor(x, y) && !objectsGrid.hasMapObjectType(x, y, EMapObjectType.CORN_GROWING)
					&& !objectsGrid.hasMapObjectType(x, y, EMapObjectType.CORN_ADULT)
					&& !objectsGrid.hasNeighborObjectType(x, y, EMapObjectType.CORN_ADULT)
					&& !objectsGrid.hasNeighborObjectType(x, y, EMapObjectType.CORN_GROWING);
		}

		private final boolean isCornCuttable(short x, short y) {
			return objectsGrid.hasCuttableObject(x, y, EMapObjectType.CORN_ADULT);
		}

	}

	final class GraphicsGrid implements IGraphicsGrid {
		@Override
		public final short getHeight() {
			return height;
		}

		@Override
		public final short getWidth() {
			return width;
		}

		@Override
		public final IMovable getMovableAt(int x, int y) {
			return movableGrid.getMovableAt((short) x, (short) y);
		}

		@Override
		public final IMapObject getMapObjectsAt(int x, int y) {
			return objectsGrid.getObjectsAt((short) x, (short) y);
		}

		@Override
		public final byte getHeightAt(int x, int y) {
			return landscapeGrid.getHeightAt((short) x, (short) y);
		}

		@Override
		public final ELandscapeType getLandscapeTypeAt(int x, int y) {
			return landscapeGrid.getLandscapeTypeAt((short) x, (short) y);
		}

		@Override
		public final Color getDebugColorAt(int x, int y) {
			// short value = (short) (partitionsGrid.getPartitionAt((short) x, (short) y) + 1);
			// return new Color((value % 3) * 0.33f, ((value / 3) % 3) * 0.33f, ((value / 9) % 3) * 0.33f, 1);

			// return debugColors[x][y];

			return flagsGrid.isBlocked((short) x, (short) y) ? new Color(0, 0, 0, 1) : (flagsGrid.isProtected((short) x, (short) y) ? new Color(0, 0,
					1, 1) : (flagsGrid.isMarked((short) x, (short) y) ? new Color(0, 1, 0, 1) : null));
		}

		@Override
		public final boolean isBorder(int x, int y) {
			return partitionsGrid.isBorderAt((short) x, (short) y);
		}

		@Override
		public final byte getPlayerAt(int x, int y) {
			return partitionsGrid.getPlayerAt((short) x, (short) y);
		}

		@Override
		public final byte getVisibleStatus(int x, int y) {
			return fogOfWar.getVisibleStatus(x, y);
		}

		@Override
		public final boolean isFogOfWarVisible(int x, int y) {
			return fogOfWar.isVisible(x, y);
		}

		@Override
		public final void setBackgroundListener(IGraphicsBackgroundListener backgroundListener) {
			landscapeGrid.setBackgroundListener(backgroundListener);
		}

	}

	class MapObjectsManagerGrid implements IMapObjectsManagerGrid {
		private static final long serialVersionUID = 6223899915568781576L;

		@Override
		public void setLandscape(short x, short y, ELandscapeType landscapeType) {
			landscapeGrid.setLandscapeTypeAt(x, y, landscapeType);
		}

		@Override
		public void setBlocked(short x, short y, boolean blocked) {
			flagsGrid.setBlockedAndProtected(x, y, blocked);
		}

		@Override
		public AbstractHexMapObject removeMapObjectType(short x, short y, EMapObjectType mapObjectType) {
			return objectsGrid.removeMapObjectType(x, y, mapObjectType);
		}

		@Override
		public boolean removeMapObject(short x, short y, AbstractHexMapObject mapObject) {
			return objectsGrid.removeMapObject(x, y, mapObject);
		}

		@Override
		public boolean isBlocked(short x, short y) {
			return flagsGrid.isBlocked(x, y);
		}

		@Override
		public AbstractHexMapObject getMapObject(short x, short y, EMapObjectType mapObjectType) {
			return objectsGrid.getMapObjectAt(x, y, mapObjectType);
		}

		@Override
		public void addMapObject(short x, short y, AbstractHexMapObject mapObject) {
			objectsGrid.addMapObjectAt(x, y, mapObject);
		}

		@Override
		public short getWidth() {
			return width;
		}

		@Override
		public short getHeight() {
			return height;
		}

		@Override
		public boolean isInBounds(short x, short y) {
			return MainGrid.this.isInBounds(x, y);
		}

		@Override
		public void setProtected(short x, short y, boolean protect) {
			flagsGrid.setProtected(x, y, protect);
		}

	}

	class LandmarksGrid implements ILandmarksThreadMap {
		@Override
		public boolean isBlocked(short x, short y) {
			ELandscapeType landscape = landscapeGrid.getLandscapeTypeAt(x, y);
			return flagsGrid.isBlocked(x, y) || landscape == ELandscapeType.WATER || landscape == ELandscapeType.SNOW;
		}

		@Override
		public boolean isInBounds(short x, short y) {
			return MainGrid.this.isInBounds(x, y);
		}

		@Override
		public short getPartitionAt(short x, short y) {
			return partitionsGrid.getPartitionAt(x, y);
		}

		@Override
		public void setPartitionAndPlayerAt(short x, short y, short partition) {
			partitionsGrid.setPartitionAndPlayerAt(x, y, partition);
			bordersThread.checkPosition(new ShortPoint2D(x, y));

			AbstractHexMapObject building = objectsGrid.getMapObjectAt(x, y, EMapObjectType.BUILDING);
			if (building != null && ((IPlayerable) building).getPlayer() != partitionsGrid.getPlayerAt(x, y)) {
				((Building) building).kill();
			}
		}
	}

	class ConstructionMarksGrid implements IConstructionMarkableMap {
		@Override
		public void setConstructMarking(ISPosition2D pos, byte value) {
			mapObjectsManager.setConstructionMarking(pos, value);
		}

		@Override
		public short getWidth() {
			return width;
		}

		@Override
		public short getHeight() {
			return height;
		}

		@Override
		public byte getHeightAt(short x, short y) {
			return landscapeGrid.getHeightAt(x, y);
		}

		@Override
		public boolean canConstructAt(short x, short y, EBuildingType type, byte player) {
			ELandscapeType[] landscapes = type.getGroundtypes();
			for (RelativePoint curr : type.getProtectedTiles()) {
				short currX = curr.calculateX(x);
				short currY = curr.calculateY(y);
				if (!MainGrid.this.isInBounds(currX, currY) || flagsGrid.isProtected(currX, currY)
						|| partitionsGrid.getPlayerAt(currX, currY) != player || !isAllowedLandscape(currX, currY, landscapes)) {
					return false;
				}
			}
			return true;
		}

		private boolean isAllowedLandscape(short x, short y, ELandscapeType[] landscapes) {
			ELandscapeType landscapeAt = landscapeGrid.getLandscapeTypeAt(x, y);
			for (byte i = 0; i < landscapes.length; i++) {
				if (landscapeAt == landscapes[i]) {
					return true;
				}
			}
			return false;
		}
	}

	class MovablePathfinderGrid extends PathfinderGrid implements IMovableGrid, Serializable {
		private static final long serialVersionUID = 4006228724969442801L;

		transient HexAStar aStar;
		transient DijkstraAlgorithm dijkstra;
		transient private InAreaFinder inAreaFinder;

		public MovablePathfinderGrid() {
			initPathfinders();
		}

		private final void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
			ois.defaultReadObject();
			initPathfinders();
		}

		private final void initPathfinders() {
			aStar = new HexAStar(this, width, height);
			dijkstra = new DijkstraAlgorithm(this, aStar, width, height);
			inAreaFinder = new InAreaFinder(this, width, height);
		}

		@Override
		public final void movableLeft(ISPosition2D position, IHexMovable movable) {
			movableGrid.movableLeft(position, movable);
		}

		@Override
		public final void movableEntered(ISPosition2D position, IHexMovable movable) {
			movableGrid.movableEntered(position, movable);
		}

		@Override
		public final MapObjectsManager getMapObjectsManager() {
			return mapObjectsManager;
		}

		@Override
		public final IHexMovable getMovable(ISPosition2D position) {
			return movableGrid.getMovableAt(position.getX(), position.getY());
		}

		@Override
		public final boolean isBlocked(short x, short y) {
			return flagsGrid.isBlocked(x, y) || isLandscapeBlocking(x, y);
		}

		@Override
		public final boolean canPush(ISPosition2D position) {
			return mapObjectsManager.canPush(position);
		}

		@Override
		public final boolean pushMaterial(ISPosition2D position, EMaterialType materialType, boolean offer) {
			if (mapObjectsManager.pushMaterial(position.getX(), position.getY(), materialType)) {
				if (offer) {
					partitionsGrid.pushMaterial(position, materialType);
				}
				return true;
			} else
				return false;
		}

		@Override
		public final boolean canPop(ISPosition2D position, EMaterialType material) {
			return mapObjectsManager.canPop(position.getX(), position.getY(), material);
		}

		@Override
		public final boolean popMaterial(ISPosition2D position, EMaterialType materialType) {
			if (mapObjectsManager.popMaterial(position.getX(), position.getY(), materialType)) {
				return true;
			} else
				return false;
		}

		@Override
		public final ELandscapeType getLandscapeTypeAt(ISPosition2D position) {
			return landscapeGrid.getLandscapeTypeAt(position.getX(), position.getY());
		}

		@Override
		public final byte getHeightAt(ISPosition2D position) {
			return landscapeGrid.getHeightAt(position.getX(), position.getY());
		}

		@Override
		public final void changeHeightAt(ISPosition2D position, byte delta) {
			landscapeGrid.changeHeightAt(position.getX(), position.getY(), delta);
		}

		@Override
		public final void setMarked(ISPosition2D position, boolean marked) {
			flagsGrid.setMarked(position.getX(), position.getY(), marked);
		}

		@Override
		public final boolean isMarked(ISPosition2D position) {
			return flagsGrid.isMarked(position.getX(), position.getY());
		}

		@Override
		public final boolean isInBounds(ISPosition2D position) {
			final short x = position.getX();
			final short y = position.getY();
			return 0 <= x && x < width && 0 <= y && y < height;
		}

		@Override
		public final byte getPlayerAt(ISPosition2D position) {
			return partitionsGrid.getPlayerAt(position);
		}

		@Override
		public final void changePlayerAt(ISPosition2D position, byte player) {
			MainGrid.this.changePlayerAt(position, player);
		}

		@Override
		public final boolean executeSearchType(ISPosition2D position, ESearchType searchType) {
			return mapObjectsManager.executeSearchType(position, searchType);
		}

		@Override
		public final HexAStar getAStar() {
			return aStar;
		}

		@Override
		public final DijkstraAlgorithm getDijkstra() {
			return dijkstra;
		}

		@Override
		public final InAreaFinder getInAreaFinder() {
			return inAreaFinder;
		}

		@Override
		public final boolean fitsSearchType(ISPosition2D position, ESearchType searchType, IPathCalculateable pathCalculateable) {
			return super.fitsSearchType(position.getX(), position.getY(), searchType, pathCalculateable);
		}

		@Override
		public final void addJobless(IManageableBearer manageable) {
			partitionsGrid.addJobless(manageable);
		}

		@Override
		public final void addJobless(IManageableWorker worker) {
			partitionsGrid.addJobless(worker);

		}

		@Override
		public final void addJobless(IManageableBricklayer bricklayer) {
			partitionsGrid.addJobless(bricklayer);
		}

		@Override
		public final void addJobless(IManageableDigger digger) {
			partitionsGrid.addJobless(digger);
		}

		@Override
		public final void changeLandscapeAt(ISPosition2D pos, ELandscapeType type) {
			landscapeGrid.setLandscapeTypeAt(pos.getX(), pos.getY(), type);
		}

		@Override
		public final void placeSmoke(ISPosition2D pos, boolean place) {
			if (place) {
				mapObjectsManager.addSimpleMapObject(pos, EMapObjectType.SMOKE, false, (byte) 0);
			} else {
				mapObjectsManager.removeMapObjectType(pos.getX(), pos.getY(), EMapObjectType.SMOKE);
			}
		}

		@Override
		public final boolean isProtected(short x, short y) {
			return flagsGrid.isProtected(x, y);
		}

		@Override
		public final void placePig(ISPosition2D pos, boolean place) {
			mapObjectsManager.placePig(pos, place);
		}

		@Override
		public final boolean isPigThere(ISPosition2D pos) {
			return mapObjectsManager.isPigThere(pos);
		}

		@Override
		public final boolean isPigAdult(ISPosition2D pos) {
			return mapObjectsManager.isPigAdult(pos);
		}

		@Override
		public final boolean isEnforcedByTower(ISPosition2D pos) {
			return partitionsGrid.isEnforcedByTower(pos.getX(), pos.getY());
		}

		@Override
		public final boolean isAllowedForMovable(short x, short y, IPathCalculateable pathCalculatable) {
			return MainGrid.this.isInBounds(x, y) && !isBlocked(x, y) && !isLandscapeBlocking(x, y)
					&& (!pathCalculatable.needsPlayersGround() || pathCalculatable.getPlayer() == partitionsGrid.getPlayerAt(x, y));
		}

		@Override
		public final EResourceType getResourceTypeAt(short x, short y) {
			return landscapeGrid.getResourceTypeAt(x, y);
		}

		@Override
		public final byte getResourceAmountAt(short x, short y) {
			return landscapeGrid.getResourceAmountAt(x, y);
		}

		@Override
		public final boolean canAddRessourceSign(ISPosition2D pos) {
			return super.canAddRessourceSign(pos.getX(), pos.getY());
		}

		@Override
		public EMaterialType getMaterialTypeAt(ISPosition2D pos) {
			return mapObjectsManager.getMaterialTypeAt(pos.getX(), pos.getY());
		}

		@Override
		public EMaterialType stealMaterialAt(ISPosition2D pos) {
			EMaterialType materialType = mapObjectsManager.stealMaterialAt(pos.getX(), pos.getY());
			if (materialType != null) {
				partitionsGrid.removeOfferAt(pos, materialType);
			}
			return materialType;
		}

	}

	class BordersThreadGrid implements IBordersThreadGrid {
		@Override
		public final byte getPlayer(short x, short y) {
			return partitionsGrid.getPlayerAt(x, y);
		}

		@Override
		public final void setBorder(short x, short y, boolean isBorder) {
			partitionsGrid.setBorderAt(x, y, isBorder);
		}

		@Override
		public final boolean isInBounds(short x, short y) {
			return MainGrid.this.isInBounds(x, y);
		}
	}

	class BuildingsGrid implements IBuildingsGrid, Serializable {
		private static final long serialVersionUID = -5567034251907577276L;

		private final RequestStackGrid requestStackGrid = new RequestStackGrid();

		@Override
		public final byte getHeightAt(ISPosition2D position) {
			return landscapeGrid.getHeightAt(position.getX(), position.getY());
		}

		@Override
		public final void pushMaterialsTo(ISPosition2D position, EMaterialType type, byte numberOf) {
			for (int i = 0; i < numberOf; i++) {
				movablePathfinderGrid.pushMaterial(position, type, true);
			}
		}

		@Override
		public final boolean setBuilding(ISPosition2D position, Building newBuilding) {
			if (MainGrid.this.isInBounds(position.getX(), position.getY())) {
				FreeMapArea area = new FreeMapArea(position, newBuilding.getBuildingType().getProtectedTiles());

				if (canConstructAt(area)) {
					setProtectedState(area, true);
					mapObjectsManager.addBuildingTo(position, newBuilding);
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}

		private final void setProtectedState(FreeMapArea area, boolean setProtected) {
			for (ISPosition2D curr : area) {
				if (MainGrid.this.isInBounds(curr.getX(), curr.getY()))
					flagsGrid.setProtected(curr.getX(), curr.getY(), setProtected);
			}
		}

		private final boolean canConstructAt(FreeMapArea area) {
			boolean isFree = true;

			for (ISPosition2D curr : area) {
				short x = curr.getX();
				short y = curr.getY();
				if (!isInBounds(x, y) || flagsGrid.isProtected(x, y) || flagsGrid.isBlocked(x, y)) {
					isFree = false;
				}
			}
			return isFree;
		}

		@Override
		public final void removeBuildingAt(ISPosition2D pos) {
			IBuilding building = (IBuilding) objectsGrid.getMapObjectAt(pos.getX(), pos.getY(), EMapObjectType.BUILDING);
			mapObjectsManager.removeMapObjectType(pos.getX(), pos.getY(), EMapObjectType.BUILDING);

			FreeMapArea area = new FreeMapArea(pos, building.getBuildingType().getProtectedTiles());
			for (ISPosition2D curr : area) {
				if (isInBounds(curr.getX(), curr.getY())) {
					flagsGrid.setBlockedAndProtected(curr.getX(), curr.getY(), false);
				}
			}
		}

		@Override
		public final void occupyArea(MapShapeFilter toBeOccupied, ISPosition2D occupiersPosition, byte player) {
			List<ISPosition2D> occupiedPositions = partitionsGrid.occupyArea(toBeOccupied, occupiersPosition, player);
			bordersThread.checkPositions(occupiedPositions);
			landmarksCorrectionThread.addLandmarkedPositions(occupiedPositions);
		}

		@Override
		public final void freeOccupiedArea(MapShapeFilter occupied, ISPosition2D pos) {
			List<ISPosition2D> totallyFreed = partitionsGrid.freeOccupiedArea(occupied, pos);
			if (!totallyFreed.isEmpty()) {
				// FIXME check for towers that would already occupy this location
			}
		}

		@Override
		public final void setBlocked(FreeMapArea area, boolean blocked) {
			for (ISPosition2D curr : area) {
				if (MainGrid.this.isInBounds(curr.getX(), curr.getY()))
					flagsGrid.setBlockedAndProtected(curr.getX(), curr.getY(), blocked);
			}
		}

		@Override
		public final short getWidth() {
			return width;
		}

		@Override
		public final short getHeight() {
			return height;
		}

		@Override
		public final IHexMovable getMovable(ISPosition2D position) {
			return movableGrid.getMovableAt(position.getX(), position.getY());
		}

		@Override
		public final void placeNewMovable(ISPosition2D position, IHexMovable movable) {
			movableGrid.movableEntered(position, movable);
		}

		@Override
		public final MapObjectsManager getMapObjectsManager() {
			return mapObjectsManager;
		}

		@Override
		public final IMovableGrid getMovableGrid() {
			return movablePathfinderGrid;
		}

		@Override
		public final void requestDiggers(IDiggerRequester requester, byte amount) {
			partitionsGrid.requestDiggers(requester, amount);
		}

		@Override
		public final void requestBricklayer(Building building, ISPosition2D bricklayerTargetPos, EDirection direction) {
			partitionsGrid.requestBricklayer(building, bricklayerTargetPos, direction);
		}

		@Override
		public final IRequestsStackGrid getRequestStackGrid() {
			return requestStackGrid;
		}

		@Override
		public final void requestBuildingWorker(EMovableType workerType, WorkerBuilding workerBuilding) {
			partitionsGrid.requestBuildingWorker(workerType, workerBuilding);
		}

		@Override
		public final void requestSoilderable(Barrack barrack) {
			partitionsGrid.requestSoilderable(barrack);
		}

		@Override
		public final DijkstraAlgorithm getDijkstra() {
			return movablePathfinderGrid.dijkstra;
		}

		private class RequestStackGrid implements IRequestsStackGrid, Serializable {
			private static final long serialVersionUID = 1278397366408051067L;

			@Override
			public final void request(IMaterialRequester requester, EMaterialType materialType, byte priority) {
				partitionsGrid.request(requester, materialType, priority);
			}

			@Override
			public final boolean hasMaterial(ISPosition2D position, EMaterialType materialType) {
				return mapObjectsManager.canPop(position.getX(), position.getY(), materialType);
			}

			@Override
			public final void popMaterial(ISPosition2D position, EMaterialType materialType) {
				mapObjectsManager.popMaterial(position.getX(), position.getY(), materialType);
			}

			@Override
			public final byte getStackSize(ISPosition2D position, EMaterialType materialType) {
				return mapObjectsManager.getStackSize(position.getX(), position.getY(), materialType);
			}

			@Override
			public final void releaseRequestsAt(ISPosition2D position, EMaterialType materialType) {
				partitionsGrid.releaseRequestsAt(position, materialType);

				byte stackSize = mapObjectsManager.getStackSize(position.getX(), position.getY(), materialType);
				for (byte i = 0; i < stackSize; i++) {
					partitionsGrid.pushMaterial(position, materialType);
				}
			}
		}

	}

	class GUIInputGrid implements IGuiInputGrid {
		@Override
		public final IHexMovable getMovable(short x, short y) {
			return movableGrid.getMovableAt(x, y);
		}

		@Override
		public final short getWidth() {
			return width;
		}

		@Override
		public final short getHeight() {
			return height;
		}

		@Override
		public final IBuilding getBuildingAt(short x, short y) {
			return (IBuilding) objectsGrid.getMapObjectAt(x, y, EMapObjectType.BUILDING);
		}

		@Override
		public final boolean isInBounds(ISPosition2D position) {
			return MainGrid.this.isInBounds(position.getX(), position.getY());
		}

		@Override
		public final IBuildingsGrid getBuildingsGrid() {
			return buildingsGrid;
		}

		@Override
		public final byte getPlayerAt(ISPosition2D position) {
			return partitionsGrid.getPlayerAt(position);
		}

		@Override
		public final void setBuildingType(EBuildingType buildingType) {
			constructionMarksCalculator.setBuildingType(buildingType);
		}

		@Override
		public final void setScreen(IMapArea screenArea) {
			constructionMarksCalculator.setScreen(screenArea);
		}

		@Override
		public final void resetDebugColors() {
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					debugColors[x][y] = null;
				}
			}
		}

		@Override
		public final ISPosition2D getConstructablePositionAround(ISPosition2D pos, EBuildingType type) {
			byte player = partitionsGrid.getPlayerAt(pos);
			if (constructionMarksGrid.canConstructAt(pos.getX(), pos.getY(), type, player)) {
				return pos;
			} else {
				for (ISPosition2D neighbour : new MapNeighboursArea(pos)) {
					if (constructionMarksGrid.canConstructAt(neighbour.getX(), neighbour.getY(), type, player)) {
						return neighbour;
					}
				}
				return null;
			}
		}

		@Override
		public final void save() throws FileNotFoundException, IOException, InterruptedException {
			GameSerializer serializer = new GameSerializer();
			serializer.save(MainGrid.this);
		}
	}

	class PartitionableGrid implements IPartitionableGrid, Serializable {
		private static final long serialVersionUID = 5631266851555264047L;

		@Override
		public final boolean isBlocked(short x, short y) {
			return flagsGrid.isBlocked(x, y) || isLandscapeBlocking(x, y);
		}

		@Override
		public final void changedPartitionAt(short x, short y) {
			landmarksCorrectionThread.addLandmarkedPosition(new ShortPoint2D(x, y));
		}

		@Override
		public final void setDebugColor(final short x, final short y, Color color) {
			debugColors[x][y] = color;
		}

	}

	class FogOfWarGrid implements IFogOfWarGrid {
		@Override
		public final IMovable getMovableAt(int x, int y) {
			return movableGrid.getMovableAt((short) x, (short) y);
		}

		@Override
		public final IMapObject getMapObjectsAt(int x, int y) {
			return objectsGrid.getObjectsAt((short) x, (short) y);
		}
	}

}
