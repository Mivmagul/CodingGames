package com.mivmagul.codingame.contest.ice.and.fire;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;


class Player {
    private Scanner in = new Scanner(System.in);

    private int /*mineAmount, buildingAmount, unitAmount, */gold, income, opponentGold, opponentIncome;
    private Map<Position, Cell> cells = new HashMap<>();
//    private List<Mine> mines = new ArrayList<>();
//    private List<Building> buildings = new ArrayList<>();
//    private List<Unit> units = new ArrayList<>();

    public void run() {
        initParams();

        while (true) {
            updateParams();

            // + find enemyHQ
            // + try to kill enemyHQ in 1 step
            // + find enemies
            // - try to kill enemies on boarding
            // - try to train max lvl unit on boarding
            // - train farmers
            // - find owned units
            // - move 1 max unit towards enemyHQ
            // - move farmers

            StringBuilder result = new StringBuilder();

            Cell enemyHQ = getEnemyHQ();
            Stream<Cell> cellsNearEnemyHQ = getBoardingCells(enemyHQ);
            Stream<Cell> ownedUnitsNearEnemyHQ = filterCells(cellsNearEnemyHQ, Arrays.asList(
                    Cell::hasUnit,
                    Cell::isOwned
            ));

            if (!isEmpty(ownedUnitsNearEnemyHQ)) {
                result.append(doMoveAny(ownedUnitsNearEnemyHQ, enemyHQ.getPosition()));
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
                    Stream<Cell> ownedUnits = filterCells(boardingCells, Arrays.asList(
                            Cell::hasUnit,
                            Cell::isOwned
                    ));
                    Stream<Cell> strongUnits = filterCells(ownedUnits, Arrays.asList(
                            cell -> cell.getUnit().isStronger(enemy.getUnit())
                    ));
                    if (!isEmpty(strongUnits)) {
                        result.append(doMoveAny(strongUnits, enemy.getPosition()));
                    } else {
                        // TODO: 5/20/2019
                    }
                }
            }

            System.out.println(result.toString());
        }
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
                .map(Map.Entry::getValue);
    }

    private Stream<Cell> getCellsValuesStream() {
        return cells.values().stream();
    }

    private String doMove(int id, Position position) {
        return doMove(id, position.getX(), position.getY());
    }

    private String doMoveAll(Stream<Cell> stream, Position position) {
        StringBuilder resultString = new StringBuilder();
        List<Integer> unitIds = stream
                .map(Cell::getUnit)
                .filter(Objects::nonNull)
                .map(Unit::getUnitId)
                .collect(Collectors.toList());
        for (Integer id : unitIds) {
            resultString.append(doMove(id, position));
        }
        return resultString.toString();
    }

    private String doMoveAny(Stream<Cell> stream, Position position) {
        int unitId = stream
                .map(Cell::getUnit)
                .filter(Objects::nonNull)
                .map(Unit::getUnitId)
                .findFirst()
                .get();
        return doMove(unitId, position);
    }

    private String doTrain(int level, Position position) {
        return doTrain(level, position.getX(), position.getY());
    }

    private String doTrain(int level, Set<Position> positions) {
        StringBuilder resultString = new StringBuilder();
        for (Position position : positions) {
            resultString.append(doTrain(level, position));
        }
        return resultString.toString();
    }

    private String doMove(int id, int x, int y) {
        return "MOVE " + id + " " + x + " " + y + ";";
    }

    private String doTrain(int level, int x, int y) {
        int cost = UnitAbilities.getValueOf(level).getCost();
        if (cost <= gold) {
            gold -= cost;
            return "TRAIN " + level + " " + x + " " + y + ";";
        }
        System.err.println("cannot train unit lvl = " + level + ", x = " + x + ", y = " + y);
        return "";
    }

    private String doWait() {
        return "WAIT";
    }

    private void initParams() {
        int mineAmount = in.nextInt();
//        mines = new ArrayList<>(mineAmount);
        for (int index = 0; index < mineAmount; index++) {
//            mines.add(new Mine(in.nextInt(), in.nextInt()));
        }
    }

    private void updateParams() {
        gold = in.nextInt();
        income = in.nextInt();
        opponentGold = in.nextInt();
        opponentIncome = in.nextInt();

        cells.clear();
        for (int i = 0; i < 12; i++) {
            String line = in.next();
//            System.err.println(line);
            for (int j = 0; j < line.length(); j++) {
                Cell cell = new Cell(CellType.getValueOf(line.charAt(j)), i, j);
//                cells.add(cell);
                cells.put(cell.getPosition(), cell);
            }
        }

        int buildingAmount = in.nextInt();
//        buildings = new ArrayList<>(buildingAmount);
        for (int i = 0; i < buildingAmount; i++) {
            Building building = new Building(Owner.getValueOf(in.nextInt()), BuildingType.getValueOf(in.nextInt()));
            Cell cell = cells.get(new Position(in.nextInt(), in.nextInt()));
            cell.setBuilding(building);
            building.setCell(cell); // TODO: 19.05.2019 ??

//            buildings.add(new Building(
//                    Owner.getValueOf(in.nextInt()),
//                    BuildingType.getValueOf(in.nextInt()),
//                    in.nextInt(),
//                    in.nextInt()
//            ));
        }

        int unitAmount = in.nextInt();
//        units = new ArrayList<>(unitAmount);
        for (int i = 0; i < unitAmount; i++) {
            Unit unit = new Unit(Owner.getValueOf(in.nextInt()), in.nextInt(), in.nextInt());
            Cell cell = cells.get(new Position(in.nextInt(), in.nextInt()));
            cell.setUnit(unit);
            unit.setCell(cell); // TODO: 19.05.2019 ??
//            System.err.println(unit);

//            units.add(new Unit(
//                    Owner.getValueOf(in.nextInt()),
//                    in.nextInt(),
//                    in.nextInt(),
//                    in.nextInt(),
//                    in.nextInt()
//            ));
        }
    }

    public static void main(String[] args) {
        Player player = new Player();
        player.run();
    }
}

class Cell {
    Position position;
    CellType cellType;
    Building building;
    Unit unit;

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

//    public boolean isNearByTo(Position position) { // TODO: 17.05.2019 diagonal?
//        return position.getDistance(getPosition()) == 1;
//    }


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

    public boolean isStronger(Unit opponent) {
        return UnitAbilities.getValueOf(level).getCanKill() >= opponent.getLevel();
    }

    @Override
    public String toString() {
        return "Unit{" +
                "owner=" + owner +
                ", unitId=" + unitId +
                ", level=" + level +
                ", cell=" + cell +
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

//    public int getDistance(Position position) { // TODO: 17.05.2019 diagonal?
//        return (int) Math.sqrt(Math.pow(x - position.getX(), 2) + Math.pow(y - position.getY(), 2));
//    }

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

enum UnitAbilities {
    _1(1, 10, 1, 0, BuildingType.HQ),
    _2(2, 20, 4, 1, BuildingType.HQ),
    _3(3, 30, 20, 3, BuildingType.HQ);

    private int level, cost, upkeep, canKill;
    private BuildingType canDestroy;

    UnitAbilities(int level, int cost, int upkeep, int canKill, BuildingType canDestroy) {
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

    public static UnitAbilities getValueOf(int level) {
        for (UnitAbilities item : values()) {
            if (item.getLevel() == level) {
                return item;
            }
        }
        throw new RuntimeException(); // TODO: 19.05.2019 ???
    }
}