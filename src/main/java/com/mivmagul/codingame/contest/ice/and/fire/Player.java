package com.mivmagul.codingame.contest.ice.and.fire;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class Player {
    public static final int INCOME_PER_ACTIVE_CELL = 1;
    public static final int LEVEL_OF_UNIT_TO_FARM = 1;

    private Scanner in = new Scanner(System.in);
    private StringBuilder resultCommand;

    private int gold, currentIncome, newIncome, opponentGold, opponentIncome;
    private Map<Position, Cell> cells;
//    private List<Mine> mines = new ArrayList<>();

    public void run() {
        initParams();

        while (true) {
            updateParams();

            // + find enemyHQ
            // + try to kill enemyHQ in 1 step
            // + find enemies
            // - try to kill enemies on boarding
            // - try to train max lvl unit on boarding ?? - only on enemy
            // - train farmers
            // - find owned units
            // - move 1 max unit towards enemyHQ
            // - move farmers


            Cell enemyHQ = getEnemyHQ();
            Stream<Cell> cellsNearEnemyHQ = getBoardingCells(enemyHQ);
            Stream<Cell> ownedUnitsNearEnemyHQ = filterCells(cellsNearEnemyHQ, Arrays.asList(
                    Cell::hasUnit,
                    Cell::isOwned
            ));

            if (!isEmpty(ownedUnitsNearEnemyHQ)) {
                doMoveAny(ownedUnitsNearEnemyHQ, enemyHQ.getPosition());
            } else {
                Stream<Cell> enemyUnits = filterCells(getCellsValuesStream(), Arrays.asList(
                        Cell::hasUnit,
                        Cell::isEnemy
                ));
                Map<Cell, Stream<Cell>> enemyUnit2OwnedCells = new HashMap<>();
                for (Cell enemyUnit : enemyUnits.collect(Collectors.toList())) {
                    enemyUnit2OwnedCells.put(enemyUnit, getBoardingCells(enemyUnit));
                }
                for (Map.Entry<Cell, Stream<Cell>> entry : enemyUnit2OwnedCells.entrySet()) {
                    Cell enemy = entry.getKey();
                    Stream<Cell> boardingCells = entry.getValue();
                    Stream<Cell> ownedUnitsNearEnemyUnit = filterCells(boardingCells, Arrays.asList(
                            Cell::hasUnit,
                            Cell::isOwned
                    ));
                    Stream<Cell> strongUnits = filterCells(ownedUnitsNearEnemyUnit, Arrays.asList(
                            cell -> cell.getUnit().isStronger(enemy.getUnit())
                    ));
                    if (!isEmpty(strongUnits)) {
                        doMoveAny(strongUnits, enemy.getPosition());
                    } else {
                        // TODO: 5/20/2019
                    }
                }

                Stream<Cell> ownedCells = filterCells(getCellsValuesStream(), Arrays.asList(
                        Cell::isOwned
                ));

                moveFarmers();
                // TODO: 5/22/2019 not only near HQ
                trainFarmers();
            }

            System.out.println(resultCommand);
        }
    }

    private void trainFarmers() {
        doTrain(LEVEL_OF_UNIT_TO_FARM, getOwnedHQ().getPosition().getBoardingPositions());
    }

    private void moveFarmers() {
        Stream<Cell> ownedUnits = filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasUnit,
                Cell::isOwned
        ));
        Stream<Unit> farmers = ownedUnits
                .map(Cell::getUnit)
                .filter(Unit::wasNotMoved)
                .filter(Unit::hasLowestLevel);
        farmers.forEach(unit -> doMove(unit, getNearestNeutralCell(unit.getCell()).getPosition()));
    }

    private Cell getOwnedHQ() {
        Stream<Cell> cellStream = filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasBuilding,
                Cell::isOwned,
                c -> c.getBuilding().isHQ()
        ));
        return cellStream.findFirst().orElse(null);
    }

    private Cell getEnemyHQ() {
        Stream<Cell> cellStream = filterCells(getCellsValuesStream(), Arrays.asList(
                Cell::hasBuilding,
                Cell::isEnemy,
                c -> c.getBuilding().isHQ()
        ));
        return cellStream.findFirst().orElse(null);
    }

    private Stream<Cell> filterCells(Stream<Cell> stream, List<Predicate<Cell>> predicates) {
        return stream.filter(predicates.stream().reduce(x -> true, Predicate::and));
    }

    private boolean isEmpty(Stream<Cell> stream) {
        return stream.count() == 0;
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
    public Cell getNearestNeutralCell(Cell cell) {
        Supplier<Stream<Cell>> boardingCells = () -> getBoardingCells(cell);
        return boardingCells
                .get()
                .filter(Cell::isNeutral)
                .filter(Cell::wasNotMovedOn)
                .findAny()
                .orElseGet(
                        () -> getNearestNeutralCell(boardingCells
                        .get()
                        .findAny()
                        .orElse(getEnemyHQ())
                        )
                ); // TODO: 5/22/2019 HA-HA getEnemyHQ!!!
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

    private void doMoveAll(Stream<Cell> stream, Position position) {
        getUnitsForMove(stream).forEach(unit -> doMove(unit, position));
    }

    private void doMoveAny(Stream<Cell> stream, Position position) {
        getUnitsForMove(stream).findFirst().ifPresent(unit -> doMove(unit, position));
    }

    private void doMove(Unit unit, Position position) {
        if (unit.wasMoved()) {
            System.err.println("Unit was moved before. id = " + unit.getUnitId() + ", position = " + position);
            return;
        }
        if (cells.get(position).wasMovedOn()) {
            System.err.println("Cell was moved on before. id = " + unit.getUnitId() + ", position = " + position);
            return;
        }
        unit.setWasMoved(true);
        if (cells.get(position).isNeutral()) {
            changeNewIncome(INCOME_PER_ACTIVE_CELL);
        }
        addToResultCommand("MOVE " + unit.getUnitId() + " " + position.getX() + " " + position.getY());
    }

    private void doTrain(int level, Set<Position> positions) {
        positions.forEach(position -> doTrain(level, position));
    }

    private void doTrain(int level, Position position) {
        if (!cells.get(position).isMovable()) {
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
        changeNewIncome(-properties.getUpkeep());
        addToResultCommand("TRAIN " + level + " " + position.getX() + " " + position.getY());
    }

    private void doWait() {
        addToResultCommand("WAIT");
    }

    private void addToResultCommand(String text) {
        resultCommand.append(text).append(";");
    }

    private void initParams() {
        int mineAmount = in.nextInt();
//        mines = new ArrayList<>(mineAmount);
        for (int index = 0; index < mineAmount; index++) {
//            mines.add(new Mine(in.nextInt(), in.nextInt()));
        }
    }

    private void updateParams() {
        resultCommand = new StringBuilder();

        gold = in.nextInt();
        currentIncome = in.nextInt();
        opponentGold = in.nextInt();
        opponentIncome = in.nextInt();

        setNewIncome(currentIncome);

        cells = new HashMap<>();
        for (int i = 0; i < 12; i++) {
            String line = in.next();
            for (int j = 0; j < line.length(); j++) {
                Cell cell = new Cell(CellType.getValueOf(line.charAt(j)), j, i);
                cells.put(cell.getPosition(), cell);
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

    public void setNewIncome(int newIncome) {
        this.newIncome = newIncome;
    }

    public void changeNewIncome(int amount) {
        newIncome += amount;
    }

    public static void main(String[] args) {
        Player player = new Player();
        player.run();
    }
}

class Cell {
    private Position position;
    private CellType cellType;
    private Building building;
    private Unit unit;
    private boolean wasMovedOn = false;

    public Cell(Position position) {
        this.position = position;
    }

    public Cell(int x, int y) {
        this(new Position(x, y));
    }

    public Cell(CellType cellType, int x, int y) {
        this(new Position(x, y));
        this.cellType = cellType;
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
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public boolean wasMovedOn() {
        return wasMovedOn;
    }

    public boolean wasNotMovedOn() {
        return !wasMovedOn();
    }

    public void setWasMovedOn(boolean wasMovedOn) {
        this.wasMovedOn = wasMovedOn;
    }

    public boolean isVoid() {
        return CellType.VOID == cellType;
    }

    public boolean isNeutral() {
        return CellType.NEUTRAL == cellType;
    }

    public boolean isOwned() {
        return CellType.OWNED_A == cellType || CellType.OWNED_NA == cellType;
    }

    public boolean isEnemy() {
        return CellType.ENEMY_A == cellType || CellType.ENEMY_NA == cellType;
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
                ", wasMovedOn=" + wasMovedOn +
                '}';
    }
}

class Mine {
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
    HQ(0);

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
    _1(1, 10, 1, 0, BuildingType.HQ),
    _2(2, 20, 4, 1, BuildingType.HQ),
    _3(3, 30, 20, 3, BuildingType.HQ);

    private int level, cost, upkeep, canKill;
    private BuildingType canDestroy;

    UnitProperties(int level, int cost, int upkeep, int canKill, BuildingType canDestroy) {
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

    public BuildingType getCanDestroy() {
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