package com.mivmagul.codingame.contest.ice.and.fire;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class Player {
    public static final int INCOME_PER_ACTIVE_CELL = 1;
    public static final int LEVEL_OF_UNIT_TO_FARM = 1;
    public static final int LENGTH_OF_PLAYING_FIELD = 12;
    public static final int MINE_INCOME = 4;
    public static final int MINE_FIXED_COST = 20;
    public static final int MINE_INDEX_COST = 4;
    public static final int TOWER_COST = 15;

    private Scanner in = new Scanner(System.in);
    private StringBuilder resultCommand;

    private int gold, currentIncome, newIncome, opponentGold, opponentIncome;
    private Map<Position, Cell> cells = new HashMap<>();

    public void run() {
        initParams();

        while (true) {
            updateParams();

            Cell enemyHQ = getEnemyHQ();
            Stream<Cell> cellsNearEnemyHQ = enemyHQ.getBoardingCells().stream();
            Stream<Cell> ownedUnitsNearEnemyHQ = getOwnedUnitsNearEnemyHQ(cellsNearEnemyHQ);

            List<Cell> ownedUnitsNearEnemyHQList = ownedUnitsNearEnemyHQ.collect(Collectors.toList());
            if (!ownedUnitsNearEnemyHQList.isEmpty()) {
                doMoveAny(ownedUnitsNearEnemyHQList.stream(), enemyHQ.getPosition());
            } else {
                Stream<Cell> enemyUnits = getEnemyUnits();
                Stream<Cell> enemyTowers = getEnemyTowers();
                Stream<Cell> enemyMines = getEnemyMines();

                for (Cell enemyUnit : enemyUnits.collect(Collectors.toList())) {
                    Set<Cell> boardingCells = enemyUnit.getBoardingCells();

                    List<Cell> ownedCellsNearEnemyUnit = boardingCells.stream().filter(Cell::isOwned).collect(Collectors.toList());
                    if (ownedCellsNearEnemyUnit.isEmpty()) {
                        continue;
                    }

                    Stream<Cell> ownedUnitsNearEnemyUnit = filterCells(ownedCellsNearEnemyUnit.stream(), Arrays.asList(
                            Cell::hasUnit
                    ));

                    List<Cell> strongUnits = filterCells(ownedUnitsNearEnemyUnit, Arrays.asList(
                            cell -> cell.getUnit().isStronger(enemyUnit.getUnit())
                    )).collect(Collectors.toList());

                    if (!strongUnits.isEmpty()) {
                        doMoveAny(strongUnits.stream(), enemyUnit.getPosition());
                    } else {
                        // TODO: 5/24/2019 check for gold = level 2 also
                        doTrain(enemyUnit.getUnit().getLevel(), enemyUnit.getPosition());
                    }
                }

                moveFarmers();
                trainFarmers();
            }

            System.out.println(resultCommand);
        }
    }

    private void trainFarmers() {
        doTrain(LEVEL_OF_UNIT_TO_FARM, getNearestEmptyCell(getOwnedHQ()).getPosition());
    }

    private void moveFarmers() {
        Stream<Cell> ownedUnits = getOwnedUnitsNearEnemyHQ(getCellsValuesStream());
        Stream<Unit> farmers = ownedUnits
                .map(Cell::getUnit)
                .filter(Unit::wasNotMoved)
                .filter(Unit::hasLowestLevel);
        farmers.forEach(unit -> doMove(unit, getNearestEmptyCell(unit.getCell()).getPosition()));
    }

    private Stream<Cell> getOwnedUnitsNearEnemyHQ(Stream<Cell> cellsNearEnemyHQ) {
        return filterCells(cellsNearEnemyHQ, Arrays.asList(
                Cell::hasUnit,
                Cell::isOwned
        ));
    }

    private Cell getOwnedHQ() {
        return filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasBuilding,
                Cell::isOwned,
                c -> c.getBuilding().isHQ()
        )).findFirst().orElse(null);
    }

    private Cell getEnemyHQ() {
        return filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasBuilding,
                Cell::isEnemy,
                c -> c.getBuilding().isHQ()
        )).findFirst().orElse(null);
    }

    private Stream<Cell> getEnemyUnits() {
        return filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasUnit,
                Cell::isEnemy
        ));
    }

    private Stream<Cell> getEnemyTowers() {
        return filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasBuilding,
                Cell::isEnemy,
                cell -> cell.getBuilding().isTower()
        ));
    }

    private Stream<Cell> getEnemyMines() {
        return filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasBuilding,
                Cell::isEnemy,
                cell -> cell.getBuilding().isMine()
        ));
    }

    private Stream<Cell> filterCells(Stream<Cell> stream, List<Predicate<Cell>> predicates) {
        return stream.filter(predicates.stream().reduce(x -> true, Predicate::and));
    }

    private Stream<Cell> getBoardingCells(Cell cell) {
        Set<Position> boardingPositions = cell.getPosition().getBoardingPositions();
        return cells
                .entrySet()
                .stream()
                .filter(entry -> boardingPositions.contains(entry.getKey()))
                .filter(entry -> !entry.getValue().isVoid())
                .map(Map.Entry::getValue);
    }
    private Cell getNearestEmptyCell(Cell cell) {
        return getEmptyNotUsedCells(cell.getBoardingCells().stream())
                .findAny()
                .orElseGet(
                        () -> getEmptyNotUsedCells(getCellsValuesStream())
                                .reduce((c1, c2) -> c1.getPosition().getDistance(cell.getPosition()) < c2.getPosition().getDistance(cell.getPosition()) ? c1 : c2)
                                .orElse(getEnemyMines()
                                                .findAny()
                                                .orElse(getEnemyHQ())
                                )
                );
    }

    private Stream<Cell> getEmptyNotUsedCells(Stream<Cell> stream) {
        return filterCells(stream, Arrays.asList(
                Cell::isEmpty,
                Cell::wasNotUsed,
                cell -> cell.isNearBy(Cell::isOwnedActive)
        ));
    }

    private Stream<Unit> getUnitsForMove(Stream<Cell> stream) {
        return stream
                .map(Cell::getUnit)
                .filter(Objects::nonNull)
                .filter(Unit::wasNotMoved);
    }

    private Stream<Cell> getCellsValuesStream() {
        return cells.values().stream();
    }

    private int getNumberOfOwnedMines() {
        return (int) filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasMine,
                Cell::isOwned
        )).count();
    }

    private void doMoveAll(Stream<Cell> stream, Position position) {
        getUnitsForMove(stream).forEach(unit -> doMove(unit, position));
    }

    private void doMoveAny(Stream<Cell> stream, Position position) {
        getUnitsForMove(stream).findFirst().ifPresent(unit -> doMove(unit, position));
    }

    private void doMove(Unit unit, Position position) {
        Cell cell = cells.get(position);
        if (unit.wasMoved()) {
            System.err.println("Unit was moved before. id = " + unit.getUnitId() + ", position = " + position);
            return;
        }

        boolean isUnitNearBy = unit.getCell().getPosition().getBoardingPositions().contains(position);
        if (isUnitNearBy) {
            if (cell.wasUsed()) {
                System.err.println("Cell was moved on before. id = " + unit.getUnitId() + ", position = " + position);
                return;
            }
            if (cell.isNeutral()) {
                increaseNewIncome(INCOME_PER_ACTIVE_CELL);
            }
            Cell previousCell = unit.getCell();
            previousCell.setUnit(null);

            unit.setCell(cell);
            unit.setWasMoved(true);

            cell.setUnit(unit);
            cell.setCellType(CellType.OWNED_A);
            cell.setWasUsed(true);

            addToResultCommand("MOVE " + unit.getUnitId() + " " + position.getX() + " " + position.getY());
        } else {
            // TODO: 5/24/2019 change the logic to understand the cell where unit is going on
            addToResultCommand("MOVE " + unit.getUnitId() + " " + position.getX() + " " + position.getY());
        }
    }

    private void doTrain(int level, Set<Position> positions) {
        positions.forEach(position -> doTrain(level, position));
    }

    private void doTrain(int level, Position position) {
        Cell cell = cells.get(position);
        if (!cell.isMovable()) {
            System.err.println("Cell is not movable. lvl = " + level + ", position = " + position);
            return;
        }
        UnitProperties properties = UnitProperties.getValueOf(level);  // TODO: 5/22/2019 refactoring? not here
        int cost = properties.getCost();
        if (cost > gold) {
            System.err.println("Not enough gold. lvl = " + level + ", position = " + position);
            return;
        }
        gold -= cost;
        increaseNewIncome(-properties.getUpkeep());

        Unit unit = new Unit(Owner.OWNED, position.getX(), position.getY());
        unit.setCell(cell);

        cell.setUnit(unit);
        cell.setCellType(CellType.OWNED_A);
        cell.setWasUsed(true);

        addToResultCommand("TRAIN " + level + " " + position.getX() + " " + position.getY());
    }

    private void doBuild(BuildingType buildingType, Position position) {
        if (buildingType == BuildingType.MINE) {
            int cost = MINE_FIXED_COST + MINE_INDEX_COST * getNumberOfOwnedMines();
            if (cost > gold) {
                System.err.println("Not enough gold. mine, position = " + position);
                return;
            }
            increaseNewIncome(MINE_INCOME);
        } else if (buildingType == BuildingType.TOWER) {
            if (TOWER_COST > gold) {
                System.err.println("Not enough gold. tower, position = " + position);
                return;
            }
        }

        Cell cell = cells.get(position);

        Building building = new Building(Owner.OWNED, buildingType);
        building.setCell(cell);

        cell.setBuilding(building);
        cell.setCellType(CellType.OWNED_A);
        cell.setWasUsed(true);

        addToResultCommand("BUILD " + buildingType.name() + " " + position.getX() + " " + position.getY());
    }

    private void doWait() {
        addToResultCommand("WAIT");
    }

    private void addToResultCommand(String text) {
        resultCommand.append(text).append(";");
    }

    private void initParams() {
        for (int x = 0; x < LENGTH_OF_PLAYING_FIELD; x++) {
            for (int y = 0; y < LENGTH_OF_PLAYING_FIELD; y++) {
                Cell cell = new Cell(x, y);
                cells.put(cell.getPosition(), cell);
            }
        }
        getCellsValuesStream().forEach(
                cell -> cell.setBoardingCells(getBoardingCells(cell).collect(Collectors.toSet()))
        );

        int mineAmount = in.nextInt();
        for (int index = 0; index < mineAmount; index++) {
            cells.get(new Position(in.nextInt(), in.nextInt())).setHasMine(true);
        }
    }

    private void updateParams() {
        clearCells();

        resultCommand = new StringBuilder();

        gold = in.nextInt();
        currentIncome = in.nextInt();
        opponentGold = in.nextInt();
        opponentIncome = in.nextInt();

        setNewIncome(currentIncome);

        for (int i = 0; i < 12; i++) {
            String line = in.next();
            for (int j = 0; j < line.length(); j++) {
                cells.get(new Position(j, i)).setCellType(CellType.getValueOf(line.charAt(j)));
            }
        }

        int buildingAmount = in.nextInt();
        for (int i = 0; i < buildingAmount; i++) {
            Building building = new Building(Owner.getValueOf(in.nextInt()), BuildingType.getValueOf(in.nextInt()));
            Cell cell = cells.get(new Position(in.nextInt(), in.nextInt()));
            cell.setBuilding(building);
            building.setCell(cell); // TODO: 19.05.2019 ??
        }

        int unitAmount = in.nextInt();
        for (int i = 0; i < unitAmount; i++) {
            Unit unit = new Unit(Owner.getValueOf(in.nextInt()), in.nextInt(), in.nextInt());
            Cell cell = cells.get(new Position(in.nextInt(), in.nextInt()));
            cell.setUnit(unit);
            unit.setCell(cell); // TODO: 19.05.2019 ??
        }
    }

    private void clearCells() {
        getCellsValuesStream().forEach(Cell::clearParams);
    }

    public void setNewIncome(int newIncome) {
        this.newIncome = newIncome;
    }

    public void increaseNewIncome(int amount) {
        newIncome += amount;
    }

    public static void main(String[] args) {
        Player player = new Player();
        player.run();
    }
}

class Cell {
    private Position position;
    private boolean hasMine;
    private Set<Cell> boardingCells = new HashSet<>();

    private CellType cellType;
    private Building building;
    private Unit unit;
    private boolean wasUsed;
    private boolean isProtected;

    public Cell(Position position) {
        this.position = position;
    }

    public Cell(int x, int y) {
        this(new Position(x, y));
    }

    public void clearParams() {
        setCellType(null);
        setBuilding(null);
        setUnit(null);
        setWasUsed(false);
        setProtected(false);
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public CellType getCellType() {
        return cellType;
    }

    public void setCellType(CellType cellType) {
        this.cellType = cellType;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
        if (building != null && building.isTower()) {
            getBoardingCells().forEach(cell -> cell.setProtected(true));
        }
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public boolean wasUsed() {
        return wasUsed;
    }

    public boolean wasNotUsed() {
        return !wasUsed();
    }

    public void setWasUsed(boolean wasUsed) {
        this.wasUsed = wasUsed;
    }

    public boolean hasMine() {
        return hasMine;
    }

    public void setHasMine(boolean hasMine) {
        this.hasMine = hasMine;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setProtected(boolean aProtected) {
        isProtected = aProtected;
    }

    public Set<Cell> getBoardingCells() {
        return boardingCells;
    }

    public void setBoardingCells(Set<Cell> boardingCells) {
        this.boardingCells = boardingCells;
    }

    public boolean isNearBy(Predicate<Cell> predicate) {
        return getBoardingCells().stream().anyMatch(predicate);
    }

    public boolean isVoid() {
        return CellType.VOID == cellType;
    }

    public boolean isNeutral() {
        return CellType.NEUTRAL == cellType;
    }

    public boolean isOwnedActive() {
        return CellType.OWNED_A == cellType;
    }

    public boolean isOwned() {
        return isOwnedActive() || CellType.OWNED_NA == cellType;
    }

    public boolean isEnemy() {
        return CellType.ENEMY_A == cellType || CellType.ENEMY_NA == cellType;
    }

    public boolean isEmpty() {
        return isNeutral() || (isEnemy() && !hasBuilding() && !hasUnit());
//        return (isEnemy() || isNeutral()) && !(hasBuilding() || hasUnit());
    }

    public boolean hasBuilding() {
        return building != null;
    }

    public boolean hasUnit() {
        return unit != null;
    }

    public boolean isMovable() {
        return !isVoid() && !(isOwned() && hasBuilding() || isOwned() && hasUnit());
    }

    public boolean isNearByTo(Position position) {
        return position.getDistance(getPosition()) == 1;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cell cell = (Cell) o;
        return position.equals(cell.position);
    }

    @Override
    public int hashCode() {
        return position.hashCode();
    }

    @Override
    public String toString() {
        return "Cell{" +
                "position=" + position +
                ", cellType=" + cellType +
                ", building=" + building +
                ", unit=" + unit +
                ", wasUsed=" + wasUsed +
                '}';
    }
}

class Building {
    private Owner owner;
    private BuildingType buildingType;
    private Cell cell;

    public Building(Owner owner, BuildingType buildingType) {
        this.owner = owner;
        this.buildingType = buildingType;
    }

    public Owner getOwner() {
        return owner;
    }

    public BuildingType getBuildingType() {
        return buildingType;
    }

    public Cell getCell() {
        return cell;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    public boolean isOwned() {
        return owner == Owner.OWNED;
    }

    public boolean isEnemy() {
        return owner == Owner.ENEMY;
    }

    public boolean isHQ() {
        return buildingType == BuildingType.HQ;
    }

    public boolean isMine() {
        return buildingType == BuildingType.MINE;
    }

    public boolean isTower() {
        return buildingType == BuildingType.TOWER;
    }
}

class Unit {
    private Owner owner;
    private int unitId;
    private int level;
    private Cell cell;
    private boolean wasMoved = false;

    public Unit(Owner owner, int unitId, int level) {
        this.owner = owner;
        this.unitId = unitId;
        this.level = level;
    }

    public Owner getOwner() {
        return owner;
    }

    public int getUnitId() {
        return unitId;
    }

    public int getLevel() {
        return level;
    }

    public Cell getCell() {
        return cell;
    }

    public void setCell(Cell cell) {
        this.cell = cell;
    }

    public boolean isOwned() {
        return owner == Owner.OWNED;
    }

    public boolean isEnemy() {
        return owner == Owner.ENEMY;
    }

    public boolean wasMoved() {
        return wasMoved;
    }

    public boolean wasNotMoved() {
        return !wasMoved();
    }

    public void setWasMoved(boolean wasMoved) {
        this.wasMoved = wasMoved;
    }

    public boolean hasLowestLevel() {
        return level == 1;
    }

    public boolean isStronger(Unit opponent) {
        return UnitProperties.getValueOf(level).getCanKill() >= opponent.getLevel();
    }

    @Override
    public String toString() {
        return "Unit{" +
                "owner=" + owner +
                ", unitId=" + unitId +
                ", level=" + level +
                ", position=" + cell.getPosition() +
                '}';
    }
}

class Position {
    private int x, y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDistance(Position position) {
        return Math.abs(x - position.getX()) + Math.abs(y - position.getY());
    }

    public Set<Position> getBoardingPositions() {
        Set<Position> boardingPositions = new HashSet<>();

        if (x > 0) {
            boardingPositions.add(new Position(x - 1, y));
        }
        if (x < 11) {
            boardingPositions.add(new Position(x + 1, y));
        }
        if (y > 0) {
            boardingPositions.add(new Position(x, y - 1));
        }
        if (y < 11) {
            boardingPositions.add(new Position(x, y + 1));
        }

        return boardingPositions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Position position = (Position) o;
        return x == position.x && y == position.y;
    }

    @Override
    public int hashCode() {
        return 31 * x + y;
    }

    @Override
    public String toString() {
        return "[" + x + ":" + y + "]";
    }
}

enum Owner {
    OWNED(0),
    ENEMY(1);

    private int id;

    Owner(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Owner getValueOf(int id) {
        for (Owner item : values()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }
}

enum BuildingType {
    HQ(0),
    MINE(1),
    TOWER(2);

    private int id;

    BuildingType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static BuildingType getValueOf(int id) {
        for (BuildingType item : values()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }
}

enum CellType {
    VOID('#'),
    NEUTRAL('.'),
    OWNED_A('O'),
    OWNED_NA('o'),
    ENEMY_A('X'),
    ENEMY_NA('x');

    private char id;

    CellType(char id) {
        this.id = id;
    }

    public char getId() {
        return id;
    }

    public static CellType getValueOf(char id) {
        for (CellType item : values()) {
            if (item.getId() == id) {
                return item;
            }
        }
        return null;
    }
}

enum UnitProperties {
    _1(1, 10, 1, 0, Arrays.asList(BuildingType.HQ, BuildingType.MINE)),
    _2(2, 20, 4, 1, Arrays.asList(BuildingType.HQ, BuildingType.MINE)),
    _3(3, 30, 20, 3, Arrays.asList(BuildingType.HQ, BuildingType.MINE, BuildingType.TOWER));

    private int level, cost, upkeep, canKill;
    private List<BuildingType> canDestroy;

    UnitProperties(int level, int cost, int upkeep, int canKill, List<BuildingType> canDestroy) {
        this.level = level;
        this.cost = cost;
        this.upkeep = upkeep;
        this.canKill = canKill;
        this.canDestroy = canDestroy;
    }

    public int getLevel() {
        return level;
    }

    public int getCost() {
        return cost;
    }

    public int getUpkeep() {
        return upkeep;
    }

    public int getCanKill() {
        return canKill;
    }

    public List<BuildingType> getCanDestroy() {
        return canDestroy;
    }

    public static int getMaxLevelAvailable() {
        int maxLevel = 0;
        for (UnitProperties item : values()) {
            if (item.getLevel() > maxLevel) {
                maxLevel = item.getLevel();
            }
        }
        return maxLevel;
    }

    public static UnitProperties getValueOf(int level) {
        for (UnitProperties item : values()) {
            if (item.getLevel() == level) {
                return item;
            }
        }
        throw new RuntimeException(); // TODO: 19.05.2019 ???
    }
}