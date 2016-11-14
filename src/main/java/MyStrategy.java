import model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static model.SkillType.*;

public final class MyStrategy implements Strategy {
    private static final double WAYPOINT_RADIUS = 100.0D;

    private static final double LOW_HP_FACTOR = 0.30D;

    private static final int TARGET_POSITION_IN_RATING = 0;

    /**
     * Ключевые точки для каждой линии, позволяющие упростить управление перемещением волшебника.
     * <p>
     * Если всё хорошо, двигаемся к следующей точке и атакуем противников.
     * Если осталось мало жизненной энергии, отступаем к предыдущей точке.
     */
    private final Map<LaneType, Point2D[]> waypointsByLane = new EnumMap<>(LaneType.class);

    private final Map<LaneType, Point2D[]> waypointsToBonus = new EnumMap<>(LaneType.class);

    private final Point2D center = new Point2D(2000, 2000);

    private final List<SkillType> skillTypesToLearn = new ArrayList<>();

    private static final int MISSILE_BRANCH = 0;
    private static final int FROST_BOLT_BRANCH = 1;
    private static final int STAFF_AND_FIREBALL_BRANCH = 2;
    private static final int HASTE_BRANCH = 3;
    private static final int SHIELD_BRANCH = 4;

    private final SkillType[][] skillBranches = {
            //todo как результат нет задержки на выполнение действий MISSILE
        { RANGE_BONUS_PASSIVE_1, RANGE_BONUS_AURA_1, RANGE_BONUS_PASSIVE_2, RANGE_BONUS_AURA_2, ADVANCED_MAGIC_MISSILE },
        { MAGICAL_DAMAGE_BONUS_PASSIVE_1, MAGICAL_DAMAGE_BONUS_AURA_1, MAGICAL_DAMAGE_BONUS_PASSIVE_2, MAGICAL_DAMAGE_BONUS_AURA_2, FROST_BOLT },
        { STAFF_DAMAGE_BONUS_PASSIVE_1, STAFF_DAMAGE_BONUS_AURA_1, STAFF_DAMAGE_BONUS_PASSIVE_2, STAFF_DAMAGE_BONUS_AURA_2, FIREBALL },
        { MOVEMENT_BONUS_FACTOR_PASSIVE_1, MOVEMENT_BONUS_FACTOR_AURA_1, MOVEMENT_BONUS_FACTOR_PASSIVE_2, MOVEMENT_BONUS_FACTOR_AURA_2, HASTE },
        { MAGICAL_DAMAGE_ABSORPTION_PASSIVE_1, MAGICAL_DAMAGE_ABSORPTION_AURA_1, MAGICAL_DAMAGE_ABSORPTION_PASSIVE_2, MAGICAL_DAMAGE_ABSORPTION_AURA_2, SHIELD }
    };

    private ActionType favoriteActionType = ActionType.FROST_BOLT;

    private Integer mySkillBranch = FROST_BOLT_BRANCH;

    private Integer mySecondSkillBranch = null;

    private Boolean damager = true;

    private Integer skillLevel = 0;

    private MovingState movingState = MovingState.STABLE_PATH;

    private boolean bonusIsChecked = false;

    private int lastTickCheck = 0;


    private boolean firstInit = true;

    private Random random;

    private LaneType myLane;
    private Point2D[] waypoints;

    private Wizard self;
    private World world;
    private Game game;
    private Move move;

    private DecisionTree decisionTree;

    private int weightOfAggressiveness;

    private Faction ownFaction;
    private long ownId;

    private enum MovingState { STABLE_PATH, TO_BONUS }

    private Map<LaneType, Point2D> bonusesByLane = new EnumMap<>(LaneType.class);

    private Point2D previousPosition = new Point2D(0,0);

    private int stayingTime = 0;


    /**
     * Основной метод стратегии, осуществляющий управление волшебником.
     * Вызывается каждый тик для каждого волшебника.
     *
     * @param self  Волшебник, которым данный метод будет осуществлять управление.
     * @param world Текущее состояние мира.
     * @param game  Различные игровые константы.
     * @param move  Результатом работы метода является изменение полей данного объекта.
     */
    @Override
    public void move(Wizard self, World world, Game game, Move move) {
        initializeTick(self, world, game, move);
        initializeStrategy(self, game);

        processMessages();

        learnSkills();

        // Если осталось мало жизненной энергии, отступаем к предыдущей ключевой точке на линии.
        if (self.getLife() < self.getMaxLife() * LOW_HP_FACTOR) {
            goTo(getPreviousWaypoint());
            return;
        }
        if(manyNear(world.getMinions())) {
            goTo(getPreviousWaypoint());
            return;
        }

        if(movingState == MovingState.STABLE_PATH) {
//todo ожидание миньёнов у башни
            //todo не пытаться отступать внутри квадрата 150х150
            List<LivingUnit> sortedByWeakFactorNearstEnemies = getSortedByWeakFactorNearstEnemies();
            LivingUnit nearestTarget = sortedByWeakFactorNearstEnemies.isEmpty() ? null : sortedByWeakFactorNearstEnemies.get(0);
            if((self.getX() <= 600 && self.getY() <= 600 && (nearestTarget == null || nearestTarget.getX() > 900)) ||
                    (self.getX() >= game.getMapSize() - 600 && self.getY() >= game.getMapSize() - 600 && (nearestTarget == null || nearestTarget.getY() <= 3500))) {
                if((world.getTickIndex() + 100) / 2500 > lastTickCheck) {
                    movingState = MovingState.TO_BONUS;
                    bonusIsChecked = false;
                    lastTickCheck++;
                    goTo(getNextWaypointToBonus());
                    return;
                }
            }
            if(nearestTarget != null) {
                move.setStrafeSpeed(random.nextBoolean() ? game.getWizardStrafeSpeed() : -game.getWizardStrafeSpeed());
            }


            // Если видим противника ... //todo не только ближайший, но и с меньшим здоровьем
            boolean alliesOwnTerritory = superiorityOfAllies() > 0.75;
            if (nearestTarget != null && alliesOwnTerritory) {
                double distance = self.getDistanceTo(nearestTarget);

                if (distance <= self.getCastRange()) {

                    // ... и он в пределах досягаемости наших заклинаний, ...

                    double angle = self.getAngleTo(nearestTarget);

                    if(self.getLife() / self.getMaxLife() > 0.6) {
                        if(nearestTarget.getLife() - game.getMagicMissileDirectDamage() < game.getMagicMissileDirectDamage()) {
                            if(distance > self.getCastRange() / 2) {
                                //подходим ближе, чтобы смочь добить
                                goTo(getNextWaypoint());
                                return;
                            }
                        }
                    }

                    // ... то поворачиваемся к цели.
                    move.setTurn(angle);

                    // Если цель перед нами, ...
                    if (StrictMath.abs(angle) < game.getStaffSector() / 2.0D) {
                        // ... то атакуем.
                        move.setCastAngle(angle);
                        move.setMinCastDistance(distance - nearestTarget.getRadius() + game.getMagicMissileRadius());

                        if (game.isSkillsEnabled()) {
                            int fireballRemainingCooldown = self.getRemainingCooldownTicksByAction()[ActionType.FIREBALL.ordinal()];
                            int maxCooldown = Math.max(fireballRemainingCooldown, self.getRemainingActionCooldownTicks());
                            if (self.getLevel() >= 16 && maxCooldown == 0) {
                                move.setAction(ActionType.FIREBALL);
                                return;
                            }
                            int frostBoltRemainingCooldown = self.getRemainingCooldownTicksByAction()[ActionType.FROST_BOLT.ordinal()];
                            maxCooldown = Math.max(frostBoltRemainingCooldown, self.getRemainingActionCooldownTicks());
                            if (((damager && self.getLevel() >= 6) || (!damager && self.getLevel() >= 11)) && maxCooldown == 0) {
                                move.setAction(ActionType.FROST_BOLT);
                                return;
                            }
                        }
                        move.setAction(ActionType.MAGIC_MISSILE);
                    }
                    return;
                }
            }

            if(!alliesOwnTerritory) {
                goTo(getPreviousWaypoint());
            } else {
                goTo(getNextWaypoint());
            }
        } else if(movingState == MovingState.TO_BONUS) {
            Point2D bonus = bonusesByLane.get(myLane);
            if(self.getDistanceTo(bonus.x, bonus.y) <= game.getWizardVisionRange()) {
                boolean bonusExists = false;
                for (Bonus currentBonus : world.getBonuses()) {
                    if(currentBonus.getX() == bonus.x && currentBonus.getY() == bonus.y) {
                        bonusExists = true;
                        break;
                    }
                }
                if(bonusExists) {
                    goTo(getNextWaypointToBonus());
                } else {
                    //todo need to attack
                    goTo(getPreviousWaypointToBonus());
                }
                bonusIsChecked = true;
            } else {
                if(bonusIsChecked) {
                    movingState = MovingState.STABLE_PATH;
                } else {
                    goTo(getNextWaypointToBonus());
                }

            }
        }
        Point2D currentPosition = new Point2D(self.getX(), self.getY());
        if(previousPosition.equals(currentPosition)) {
            if(stayingTime > 10 && stayingTime < 20) {
                move.setStrafeSpeed(game.getWizardStrafeSpeed());
            } else if(stayingTime >= 20 && stayingTime < 30) {
                move.setStrafeSpeed(-game.getWizardStrafeSpeed());
            } else if(stayingTime >= 30 && stayingTime < 40) {
                move.setSpeed(game.getWizardBackwardSpeed());
                move.setStrafeSpeed(game.getWizardStrafeSpeed());
            } else {
                move.setSpeed(game.getWizardBackwardSpeed());
                move.setStrafeSpeed(-game.getWizardStrafeSpeed());
            }
            stayingTime++;
        } else {
            stayingTime = 0;
        }
        previousPosition = currentPosition;
    }

    private boolean manyNear(Minion[] minions) {
        int nearAmount = 0;
        for (Minion minion : minions) {
            if(minion.getFaction() == self.getFaction()) continue;
            if(MinionType.ORC_WOODCUTTER == minion.getType() && minion.getDistanceTo(self) <= game.getOrcWoodcutterAttackRange() * 2) {
                nearAmount++;
            }
        }
        return nearAmount >= 2;
    }

    private void processMessages() {
        //todo предусмотреть target противника
        if(self.isMaster()) {
            List<LaneType> laneTypes = new ArrayList<>(Arrays.asList(LaneType.TOP, LaneType.MIDDLE, LaneType.BOTTOM));
            laneTypes.remove(myLane);
            for (int i = 0; i < 4; i++) {
                laneTypes.addAll(Arrays.asList(LaneType.TOP, LaneType.MIDDLE, LaneType.BOTTOM));
            }
            Map<LaneType, List<Integer>> skillTypesByLanes = createSkillTypesByLane();

            skillTypesByLanes.get(myLane).add(mySkillBranch);

            Message[] messages = Arrays.stream(world.getPlayers())
                    .filter(p -> !p.isMe())
                    .map(p -> {
                        LaneType currentLaneType;
                        if(laneTypes.size() > 0) {
                            currentLaneType = laneTypes.get(0);
                            laneTypes.remove(0);
                        } else {
                            currentLaneType = LaneType.values()[random.nextInt(3)];
                        }
                        List<Integer> skillTypesOnCurrentLane = skillTypesByLanes.get(myLane);
                        Integer notUsedSkillType = null;
                        for (int i = 0; i < 5; i++) {
                            if(!skillTypesOnCurrentLane.contains(i)) {
                                notUsedSkillType = i;
                                break;
                            }
                        }
                        if(notUsedSkillType == null) {
                            notUsedSkillType = random.nextInt(5);
                        }

                        return new Message(currentLaneType, skillBranches[notUsedSkillType][4], new byte[0]);
                    }).toArray(Message[]::new);
            move.setMessages(messages);


        } else {
            Message[] messages = self.getMessages();
            int myMessageIndex = ((Long) self.getOwnerPlayerId()).intValue();
            if(myMessageIndex >= messages.length) {
                System.err.println("Incorrect message index");
                return;
            }
            Message messageForMe = messages[myMessageIndex];
            if(messageForMe.getLane() != null) {
                myLane = messageForMe.getLane();
                System.out.println("Accepted lane from message");
            }
            switch (messageForMe.getSkillToLearn()) {
                case ADVANCED_MAGIC_MISSILE:
                    mySkillBranch = MISSILE_BRANCH;
                    favoriteActionType = ActionType.MAGIC_MISSILE;
                    damager = true;
                    break;
                case FROST_BOLT:
                    mySkillBranch = FROST_BOLT_BRANCH;
                    favoriteActionType = ActionType.FROST_BOLT;
                    damager = true;
                    break;
                case FIREBALL:
                    mySkillBranch = STAFF_AND_FIREBALL_BRANCH;
                    favoriteActionType = ActionType.FIREBALL;
                    damager = true;
                    break;
                case HASTE:
                    mySkillBranch = HASTE_BRANCH;
                    favoriteActionType = ActionType.HASTE;
                    damager = false;
                    break;
                case SHIELD:
                    mySkillBranch = SHIELD_BRANCH;
                    favoriteActionType = ActionType.SHIELD;
                    damager = false;
                    break;
                default:
                    mySkillBranch = FROST_BOLT_BRANCH;
                    favoriteActionType = ActionType.FROST_BOLT;
                    damager = true;
                    break;
            }
        }
    }

    private Map<LaneType, List<Integer>> createSkillTypesByLane() {
        Map<LaneType, List<Integer>> skillTypesByLane = new HashMap<>();
        skillTypesByLane.put(LaneType.TOP, new ArrayList<>());
        skillTypesByLane.put(LaneType.BOTTOM, new ArrayList<>());
        skillTypesByLane.put(LaneType.MIDDLE, new ArrayList<>());
        return skillTypesByLane;
    }

    // TODO: 12.11.2016 вызывать во время wizard.remainingActionCooldownTicks > 0
    // и если перспективное действие - перемещение
    public Point2D getPerspectivePosition() {
        // TODO: 12.11.2016 учесть позиции противников
        // возможность спрятаться за деревом с учетом скорости перемещения и будет ли это иметь смысл
        // посмотреть на окружающие бонусы
        // важность бонуса защиты при малом количестве здоровья - больше
        // если расстояние от противника до бонуса меньше и сокращается в течении n тиков в зависимости от типа бонуса отступать и применять действия
        return null;
    }

    // TODO: 12.11.2016 если достаточно здоровья, количество атакующих противников * их способность атаковать в худшем случае
    // TODO: 12.11.2016 в сумме дает возможность добить и убежать, кинуть зов о помощи при принятии решения продолжать
    public void staffHit() {
        move.setAction(ActionType.STAFF);
    }

    //todo приоритет зависит от минимума разницы урона и оставшегося здоровья противника
    public void missilShot() {
        move.setAction(ActionType.MAGIC_MISSILE);
    }

    //todo бросаем, если couldown 0 и мана есть если ключевой уничтожаемый персонж имеет большее количество здоровья или менее 30%
    //todo основная задача - заморозить более сильного соперника или не дать убежать противнику без хп
    public void frostBoltShot() {
        move.setAction(ActionType.FROST_BOLT);
    }

    ////todo если couldown 0 и мана есть и расстояние между двумя соперниками 80% от радиуса фаербола, обязательно один волшебник
    public void fireballShot() {
        move.setMaxCastDistance(0);//todo расстояние до центра между группой противников
        move.setMinCastDistance(move.getMaxCastDistance()*0.8);
        move.setAction(ActionType.FIREBALL);
    }

    //todo ускорить союзника, если маны останется на 100% (ледяной болт) - скорость регенерации*общий кулдаун для ледяного болта, если его здоровье 15% и он среди 5 ближайших союзников к ближайшему противнику, если у самого более 80% здоровья
    //todo если ускоренный союзник с учетом скорости и времени до конца регенерации маны успевает покинуть поле боя, то брать 100% для огненного болта, если ближайшее время его запуска не превышает время на покидание зоны бд союзника
    public void doHaste(int targetId) {

    }


    public void doShield(int targetId) {

    }

    /**
     *
     * @return -1 нет бонусов
     */
//    public int distanceToBonus() {
//
//    }

    //todo EMPOWER усиление атаки, если количество союзников более 150% противников, то нужно
    //todo HASTE если противников больше на 150% и рядом нет волшебников союзников, то используем для отсупления
    //todo SHIELD аналогично HASTE
//    public boolean bonusIsNecessary() {
//
//    }

//    public boolean waitMinios() {
//        //todo если стоим вблизи башши, но миньоны отстали - ждем
//    }

    public void learnSkills() {
        if(damager) {
            mySecondSkillBranch = SHIELD_BRANCH;
        } else {
            mySecondSkillBranch = FROST_BOLT_BRANCH;
        }
        switch (self.getLevel()) {
            case 2:
                move.setSkillToLearn(skillBranches[mySkillBranch][0]);
                break;
            case 3:
                move.setSkillToLearn(skillBranches[mySkillBranch][1]);
                break;
            case 4:
                move.setSkillToLearn(skillBranches[mySkillBranch][2]);
                break;
            case 5:
                move.setSkillToLearn(skillBranches[mySkillBranch][3]);
                break;
            case 6:
                move.setSkillToLearn(skillBranches[mySkillBranch][4]);
                break;
            case 7:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][0]);
                break;
            case 8:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][1]);
                break;
            case 9:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][2]);
                break;
            case 10:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][3]);
                break;
            case 11:
                move.setSkillToLearn(skillBranches[mySecondSkillBranch][4]);
                break;
            case 12:
                move.setSkillToLearn(skillBranches[STAFF_AND_FIREBALL_BRANCH][0]);
                break;
            case 13:
                move.setSkillToLearn(skillBranches[STAFF_AND_FIREBALL_BRANCH][1]);
                break;
            case 14:
                move.setSkillToLearn(skillBranches[STAFF_AND_FIREBALL_BRANCH][2]);
                break;
            case 15:
                move.setSkillToLearn(skillBranches[STAFF_AND_FIREBALL_BRANCH][3]);
                break;
            case 16:
                move.setSkillToLearn(skillBranches[STAFF_AND_FIREBALL_BRANCH][4]);
                break;
        }

    }

    public double hastendMultiplier() {
        return 1.0 + game.getHastenedRotationBonusFactor();
    }

    /**
     * Инциализируем стратегию.
     * <p>
     * Для этих целей обычно можно использовать конструктор, однако в данном случае мы хотим инициализировать генератор
     * случайных чисел значением, полученным от симулятора игры.
     */
    private void initializeStrategy(Wizard self, Game game) {
        if(firstInit) {
            firstInit = false;
            ownFaction = self.getFaction();
            ownId = self.getOwnerPlayerId();
            decisionTree = new DecisionTree();
        }
        if (random == null) {
            random = new Random(game.getRandomSeed());

            double mapSize = game.getMapSize();

            waypointsByLane.put(LaneType.MIDDLE, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    random.nextBoolean()
                            ? new Point2D(600.0D, mapSize - 200.0D)
                            : new Point2D(200.0D, mapSize - 600.0D),
                    new Point2D(800.0D, mapSize - 800.0D),
                    new Point2D(mapSize - 600.0D, 600.0D)
            });

            waypointsByLane.put(LaneType.TOP, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(100.0D, mapSize - 400.0D),
                    new Point2D(200.0D, mapSize - 800.0D),
                    new Point2D(200.0D, mapSize * 0.75D),
                    new Point2D(200.0D, mapSize * 0.5D),
                    new Point2D(200.0D, mapSize * 0.25D),
                    new Point2D(200.0D, 200.0D),
                    new Point2D(mapSize * 0.25D, 200.0D),
                    new Point2D(mapSize * 0.5D, 200.0D),
                    new Point2D(mapSize * 0.75D, 200.0D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            });

            waypointsByLane.put(LaneType.BOTTOM, new Point2D[]{
                    new Point2D(100.0D, mapSize - 100.0D),
                    new Point2D(400.0D, mapSize - 100.0D),
                    new Point2D(800.0D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.25D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.5D, mapSize - 200.0D),
                    new Point2D(mapSize * 0.75D, mapSize - 200.0D),
                    new Point2D(mapSize - 200.0D, mapSize - 200.0D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.75D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.5D),
                    new Point2D(mapSize - 200.0D, mapSize * 0.25D),
                    new Point2D(mapSize - 200.0D, 200.0D)
            });

            waypointsToBonus.put(LaneType.TOP, new Point2D[] {
                    new Point2D(200.0D, 200.0D),
                    new Point2D(400.0D, 400.0D),
                    new Point2D(600.0D, 600.0D),
                    new Point2D(800.0D, 800.0D),
                    new Point2D(1000.0D, 1000.0D),
                    new Point2D(1200.0D, 1200.0D)
            });

            waypointsToBonus.put(LaneType.BOTTOM, new Point2D[] {
                    new Point2D(mapSize - 200.0D, mapSize - 200.0D),
                    new Point2D(mapSize - 400.0D, mapSize - 400.0D),
                    new Point2D(mapSize - 600.0D, mapSize - 600.0D),
                    new Point2D(mapSize - 800.0D, mapSize - 800.0D),
                    new Point2D(mapSize - 1000.0D, mapSize - 1000.0D),
                    new Point2D(mapSize - 1200.0D, mapSize - 1200.0D)
            });

            switch ((int) self.getId()) {
                case 1:
                case 2:
                case 6:
                case 7:
                    myLane = LaneType.TOP;
                    break;
                case 3:
                case 8:
                    myLane = LaneType.MIDDLE;
                    break;
                case 4:
                case 5:
                case 9:
                case 10:
                    myLane = LaneType.BOTTOM;
                    break;
                default:
            }

            waypoints = waypointsByLane.get(myLane);

            bonusesByLane.put(LaneType.TOP, new Point2D(1200, 1200));
            bonusesByLane.put(LaneType.MIDDLE, new Point2D(1200, 1200));
            bonusesByLane.put(LaneType.BOTTOM, new Point2D(2800, 2800));

            // Наша стратегия исходит из предположения, что заданные нами ключевые точки упорядочены по убыванию
            // дальности до последней ключевой точки. Сейчас проверка этого факта отключена, однако вы можете
            // написать свою проверку, если решите изменить координаты ключевых точек.

            /*Point2D lastWaypoint = waypoints[waypoints.length - 1];

            Preconditions.checkState(ArrayUtils.isSorted(waypoints, (waypointA, waypointB) -> Double.compare(
                    waypointB.getDistanceTo(lastWaypoint), waypointA.getDistanceTo(lastWaypoint)
            )));*/
        }

        List<Player> playersSortedByScore = Arrays.asList(world.getPlayers());
        Collections.sort(playersSortedByScore, (p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));
        int ownPositionInRating = IntStream.range(0, playersSortedByScore.size())
                .filter(num -> playersSortedByScore.get(num).isMe())
                .findFirst()
                .getAsInt();
        weightOfAggressiveness = TARGET_POSITION_IN_RATING - ownPositionInRating;
    }

    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    private void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
    }

    /**
     * Данный метод предполагает, что все ключевые точки на линии упорядочены по уменьшению дистанции до последней
     * ключевой точки. Перебирая их по порядку, находим первую попавшуюся точку, которая находится ближе к последней
     * точке на линии, чем волшебник. Это и будет следующей ключевой точкой.
     * <p>
     * Дополнительно проверяем, не находится ли волшебник достаточно близко к какой-либо из ключевых точек. Если это
     * так, то мы сразу возвращаем следующую ключевую точку.
     */
    private Point2D getNextWaypoint() {
        int lastWaypointIndex = waypoints.length - 1;
        Point2D lastWaypoint = waypoints[lastWaypointIndex];

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex + 1];
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }

    private Point2D getNextWaypointToBonus() {
        Point2D[] waypoints = waypointsToBonus.get(myLane);
        int lastWaypointIndex = waypoints.length - 1;
        Point2D lastWaypoint = waypoints[lastWaypointIndex];

        for (int waypointIndex = 0; waypointIndex < lastWaypointIndex; ++waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex + 1];
            }

            if (lastWaypoint.getDistanceTo(waypoint) < lastWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return lastWaypoint;
    }

    /**
     * Действие данного метода абсолютно идентично действию метода {@code getNextWaypoint}, если перевернуть массив
     * {@code waypoints}.
     */
    private Point2D getPreviousWaypoint() {
        Point2D firstWaypoint = waypoints[0];

        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex - 1];
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }

    private Point2D getPreviousWaypointToBonus() {
        Point2D[] waypoints = waypointsToBonus.get(myLane);
        Point2D firstWaypoint = waypoints[0];

        for (int waypointIndex = waypoints.length - 1; waypointIndex > 0; --waypointIndex) {
            Point2D waypoint = waypoints[waypointIndex];

            if (waypoint.getDistanceTo(self) <= WAYPOINT_RADIUS) {
                return waypoints[waypointIndex - 1];
            }

            if (firstWaypoint.getDistanceTo(waypoint) < firstWaypoint.getDistanceTo(self)) {
                return waypoint;
            }
        }

        return firstWaypoint;
    }

    /**
     * Простейший способ перемещения волшебника.
     */
    private void goTo(Point2D point) {
        double angle = self.getAngleTo(point.getX(), point.getY());

        move.setTurn(angle);

        if (StrictMath.abs(angle) < game.getStaffSector() / 4.0D) {
            move.setSpeed(game.getWizardForwardSpeed());
        }
    }

    /**
     * Находим ближайшую цель для атаки, независимо от её типа и других характеристик.
     */
    private LivingUnit getNearestTarget() {
        List<LivingUnit> targets = getLivingUnits();

        LivingUnit nearestTarget = null;
        double nearestTargetDistance = Double.MAX_VALUE;

        for (LivingUnit target : targets) {
            if (target.getFaction() == Faction.NEUTRAL || target.getFaction() == self.getFaction()) {
                continue;
            }

            double distance = self.getDistanceTo(target);

            if (distance < nearestTargetDistance) {
                nearestTarget = target;
                nearestTargetDistance = distance;
            }
        }
        return nearestTarget;
    }

    private double superiorityOfAllies() {
        List<LivingUnit> livingUnits = getLivingUnits();
        double allies = 0;
        double enemies = 0;
        for (LivingUnit livingUnit : livingUnits) {
            if(livingUnit.getDistanceTo(self) > self.getVisionRange()) {
                continue;
            }
            if(livingUnit.getFaction() == self.getFaction()) {
                allies++;
            } else if(livingUnit.getFaction() != Faction.NEUTRAL) {
                enemies++;
            }
        }
        if(enemies == 0) {
            return 10000;
        }
        return allies / enemies;
    }

    private List<LivingUnit> getLivingUnits() {
        List<LivingUnit> targets = new ArrayList<>();
        targets.addAll(Arrays.asList(world.getBuildings()));
        targets.addAll(Arrays.asList(world.getWizards()));
        targets.addAll(Arrays.asList(world.getMinions()));
        return targets;
    }

    private List<LivingUnit> getSortedByWeakFactorNearstEnemies() {
        List<LivingUnit> targets = new ArrayList<>();
        List<Building> buildings = Arrays.asList(world.getBuildings());
        targets.addAll(buildings);
        Map<LivingUnit, Integer> weights = new HashMap<>();
        for (Building building : buildings) {
            weights.put(building, 1);
        }
        List<Wizard> wizards = Arrays.asList(world.getWizards());
        targets.addAll(wizards);
        for (Wizard wizard : wizards) {
            weights.put(wizard, 2);
        }
        List<Minion> minions = Arrays.asList(world.getMinions());
        targets.addAll(minions);
        for (Minion minion : minions) {
            weights.put(minion, 3);
        }
        return targets.stream().filter(target -> {
            return target.getFaction() != Faction.NEUTRAL && target.getFaction() != self.getFaction() && self.getDistanceTo(target) <= self.getCastRange();
        }).sorted((target1, target2) -> {
            int weightsCompare = Integer.compare(weights.get(target1), weights.get(target2));
            if(weightsCompare == 0) {
                int lifeCompare = Integer.compare(target1.getLife(), target2.getLife());
                if(lifeCompare == 0) {
                    return Double.compare(self.getDistanceTo(target1), self.getDistanceTo(target2));
                }
                return lifeCompare;
            }
            return weightsCompare;
        }).collect(Collectors.toList());
    }

    /**
     * Вспомогательный класс для хранения позиций на карте.
     */
    private static final class Point2D {
        private final double x;
        private final double y;

        private Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public double getDistanceTo(double x, double y) {
            return StrictMath.hypot(this.x - x, this.y - y);
        }

        public double getDistanceTo(Point2D point) {
            return getDistanceTo(point.x, point.y);
        }

        public double getDistanceTo(Unit unit) {
            return getDistanceTo(unit.getX(), unit.getY());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Point2D point2D = (Point2D) o;

            if (Double.compare(point2D.x, x) != 0) return false;
            return Double.compare(point2D.y, y) == 0;

        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(x);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(y);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

    private static final class DecisionTree {

        private List<SelfNode> selfNodes = new ArrayList<>();

        private Objective objective = new Objective();

        public DecisionTree() {
            init();
        }

        private void init() {
            for (ActionType actionType : ActionType.values()) {
                selfNodes.add(new SelfNode(actionType));
            }
        }

        public ActionType decideActionType(World originalWorld, Wizard originalSelf, Move originalMove) {
            Map<SelfNode, Double> estimations = selfNodes.stream().collect(Collectors.toMap(n -> n, node -> node.estimate(originalWorld, originalSelf, originalMove)));
            Optional<SelfNode> bestNode = selfNodes.stream().max((n1, n2) -> estimations.get(n1).compareTo(estimations.get(n2)));
            if(bestNode.isPresent()) {
                return bestNode.get().actionType;
            } else {
                return ActionType.MAGIC_MISSILE;
            }
        }

        private final class SelfNode extends AbstractNode {

            ActionType actionType;

            public SelfNode(ActionType actionType) {
                this.actionType = actionType;
            }

        }

        private final class EnemyNode extends AbstractNode {

        }

        private class AbstractNode {
            private List<? extends AbstractNode> nodes = new ArrayList<>();

            public double estimate(World world, Wizard self, Move move) {
                return objective.estimate(world, self, move);
            }
        }


    }

    private static class Objective {

        public double estimate(World world, Wizard self, Move move) {
            return estimateScore(world) + estimateMana(self);
        }

        private int estimateScore(World world) {
            return world.getMyPlayer().getScore() - 200;
        }

        private int estimateMana(Wizard self) {
            return self.getMana() * (1 / (1 + self.getRemainingActionCooldownTicks() /* докинуть кулдаун скила из move... */));
        }

    }
    private static World updateAttributes(World world, int score) {
        Player[] players = world.getPlayers();
        int myPlayerIndex = myPlayerIndex(players);
        players[myPlayerIndex] = copy(players[myPlayerIndex], score);

        return copy(world, players);
    }

    private static World copy(World world, Player[] players) {
        return new World(world.getTickIndex(), world.getTickCount(), world.getWidth(), world.getHeight(), players, world.getWizards(), world.getMinions(), world.getProjectiles(), world.getBonuses(), world.getBuildings(), world.getTrees());
    }

    private static Player copy(Player player, int score) {
        return new Player(player.getId(), player.isMe(), player.getName(), player.isStrategyCrashed(), score, player.getFaction());
    }

    private static Move copy(Move move, ActionType actionType) {
        Move copy = new Move();
        copy.setAction(actionType);
        return copy;
    }

    private static int myPlayerIndex(Player[] players) {
        for (int i = 0; i < players.length; i++) {
            if(players[i].isMe()) {
                return i;
            }
        }
        return -1;
    }

}
